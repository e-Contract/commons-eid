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

package be.fedict.commons.eid.jca;

import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.client.impl.BeIDDigest;

/**
 * eID based JCA private key. Should not be used directly, but via the
 * {@link BeIDKeyStore}.
 * 
 * @see BeIDKeyStore
 * @author Frank Cornelis
 * 
 */
public class BeIDPrivateKey implements PrivateKey {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory.getLog(BeIDPrivateKey.class);

	private final FileType certificateFileType;

	private final BeIDCard beIDCard;

	private final boolean logoff;

	private final static Map<String, BeIDDigest> beIDDigests;

	static {
		beIDDigests = new HashMap<String, BeIDDigest>();
		beIDDigests.put("SHA-1", BeIDDigest.SHA_1);
		beIDDigests.put("SHA-256", BeIDDigest.SHA_256);
		beIDDigests.put("SHA-384", BeIDDigest.SHA_384);
		beIDDigests.put("SHA-512", BeIDDigest.SHA_512);
		beIDDigests.put("NONE", BeIDDigest.NONE);
		beIDDigests.put("SHA-1-PSS", BeIDDigest.SHA_1_PSS);
		beIDDigests.put("SHA-256-PSS", BeIDDigest.SHA_256_PSS);
	}

	public BeIDPrivateKey(final FileType certificateFileType,
			final BeIDCard beIDCard, final boolean logoff) {
		LOG.debug("constructor: " + certificateFileType);
		this.certificateFileType = certificateFileType;
		this.beIDCard = beIDCard;
		this.logoff = logoff;
	}

	@Override
	public String getAlgorithm() {
		return "RSA";
	}

	@Override
	public String getFormat() {
		return null;
	}

	@Override
	public byte[] getEncoded() {
		return null;
	}

	byte[] sign(final byte[] digestValue, final String digestAlgo)
			throws SignatureException {
		final BeIDDigest beIDDigest = beIDDigests.get(digestAlgo);
		if (null == beIDDigest) {
			throw new SignatureException("unsupported algo: " + digestAlgo);
		}
		byte[] signatureValue;
		try {
			signatureValue = this.beIDCard.sign(digestValue, beIDDigest,
					this.certificateFileType, false);
			if (this.logoff) {
				this.beIDCard.logoff();
			}
		} catch (final Exception ex) {
			throw new SignatureException(ex);
		}
		return signatureValue;
	}
}
