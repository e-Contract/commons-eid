/*
 * Commons eID Project.
 * Copyright (C) 2008-2012 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */

package be.fedict.commons.eid.consumer.jca;

import java.io.Serializable;
import java.security.PrivateKey;
import java.util.concurrent.Semaphore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of {@link PrivateKey} for proxy signatures.
 * 
 * @author Frank Cornelis
 * 
 */
public class ProxyPrivateKey implements PrivateKey {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory.getLog(ProxyPrivateKey.class);

	private DigestInfo digestInfo;

	private byte[] signatureValue;

	private final Semaphore consumerSemaphore;
	private final Semaphore producerSemaphore;

	/**
	 * Default constructor.
	 */
	public ProxyPrivateKey() {
		LOG.debug("constructor");
		this.consumerSemaphore = new Semaphore(0);
		this.producerSemaphore = new Semaphore(0);
	}

	@Override
	public String getAlgorithm() {
		LOG.debug("getAlgorithm");
		return "RSA";
	}

	@Override
	public String getFormat() {
		LOG.debug("getFormat");
		return null;
	}

	@Override
	public byte[] getEncoded() {
		LOG.debug("getEncoded");
		return null;
	}

	/**
	 * Gives back the digest info as injected by the JCA consumer.
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	public DigestInfo getDigestInfo() throws InterruptedException {
		this.producerSemaphore.acquire();
		return this.digestInfo;
	}

	/**
	 * Sets the signature value. The signature value should be calculated based
	 * on the previously acquired digest info.
	 * 
	 * @param signatureValue
	 *            the PKCS#1 signature value.
	 * @see #getDigestInfo()
	 */
	public void setSignatureValue(final byte[] signatureValue) {
		this.signatureValue = signatureValue;
		this.consumerSemaphore.release();
	}

	byte[] sign(final byte[] digestValue, final String digestAlgorithm)
			throws InterruptedException {
		this.digestInfo = new DigestInfo(digestValue, digestAlgorithm);
		this.producerSemaphore.release();
		this.consumerSemaphore.acquire();
		return this.signatureValue;
	}

	/**
	 * A digest info data-structure.
	 * 
	 * @author Frank Cornelis
	 * 
	 */
	public static final class DigestInfo implements Serializable {
		private static final long serialVersionUID = 1L;

		private final byte[] digestValue;
		private final String digestAlgorithm;

		/**
		 * Main constructor.
		 * 
		 * @param digestValue
		 * @param digestAlgorithm
		 */
		public DigestInfo(final byte[] digestValue, final String digestAlgorithm) {
			this.digestValue = digestValue;
			this.digestAlgorithm = digestAlgorithm;
		}

		/**
		 * Gives back the digest value.
		 * 
		 * @return
		 */
		public byte[] getDigestValue() {
			return this.digestValue;
		}

		/**
		 * Gives back the digest algorithm.
		 * 
		 * @return
		 */
		public String getDigestAlgorithm() {
			return this.digestAlgorithm;
		}
	}
}
