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

import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.SignatureSpi;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ProxySignature extends SignatureSpi {

	private static final Log LOG = LogFactory.getLog(ProxySignature.class);

	private final MessageDigest messageDigest;

	private final static Map<String, String> digestAlgos;

	private ProxyPrivateKey privateKey;

	static {
		digestAlgos = new HashMap<String, String>();
		digestAlgos.put("SHA1withRSA", "SHA-1");
		digestAlgos.put("SHA256withRSA", "SHA-256");
	}

	public ProxySignature(String algorithm) throws NoSuchAlgorithmException {
		LOG.debug("constructor: " + algorithm);
		String digestAlgo = digestAlgos.get(algorithm);
		LOG.debug("digest algo: " + digestAlgo);
		this.messageDigest = MessageDigest.getInstance(digestAlgo);
	}

	@Override
	protected void engineInitVerify(PublicKey publicKey)
			throws InvalidKeyException {
		LOG.debug("engineInitVerify");
	}

	@Override
	protected void engineInitSign(PrivateKey privateKey)
			throws InvalidKeyException {
		LOG.debug("engineInitSign");
		if (false == privateKey instanceof ProxyPrivateKey) {
			throw new InvalidKeyException("ProxyPrivateKey expected");
		}
		this.privateKey = (ProxyPrivateKey) privateKey;
		this.messageDigest.reset();
	}

	@Override
	protected void engineUpdate(byte b) throws SignatureException {
		LOG.debug("engineUpdate");
		this.messageDigest.update(b);
	}

	@Override
	protected void engineUpdate(byte[] b, int off, int len)
			throws SignatureException {
		LOG.debug("engineUpdate");
		this.messageDigest.update(b, off, len);
	}

	@Override
	protected byte[] engineSign() throws SignatureException {
		LOG.debug("engineSign");
		byte[] digestValue = this.messageDigest.digest();
		byte[] signatureValue;
		try {
			signatureValue = this.privateKey.sign(digestValue,
					this.messageDigest.getAlgorithm());
		} catch (InterruptedException e) {
			throw new SignatureException("interrupted on sign");
		}
		return signatureValue;
	}

	@Override
	protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
		LOG.debug("engineVerify");
		return false;
	}

	@Override
	@Deprecated
	protected void engineSetParameter(String param, Object value)
			throws InvalidParameterException {
	}

	@Override
	@Deprecated
	protected Object engineGetParameter(String param)
			throws InvalidParameterException {
		return null;
	}
}
