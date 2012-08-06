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

import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignatureSpi;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BeIDSignature extends SignatureSpi {

	private static final Log LOG = LogFactory.getLog(BeIDSignature.class);

	private final static Map<String, String> digestAlgos;

	private final MessageDigest messageDigest;

	private BeIDPrivateKey privateKey;

	private Signature verifySignature;

	private final String signatureAlgorithm;

	static {
		digestAlgos = new HashMap<String, String>();
		digestAlgos.put("SHA1withRSA", "SHA-1");
		digestAlgos.put("SHA256withRSA", "SHA-256");
		digestAlgos.put("SHA384withRSA", "SHA-384");
		digestAlgos.put("SHA512withRSA", "SHA-512");
	}

	BeIDSignature(String signatureAlgorithm) throws NoSuchAlgorithmException {
		LOG.debug("constructor: " + signatureAlgorithm);
		this.signatureAlgorithm = signatureAlgorithm;
		String digestAlgo = digestAlgos.get(signatureAlgorithm);
		if (null == digestAlgo) {
			throw new NoSuchAlgorithmException(signatureAlgorithm);
		}
		this.messageDigest = MessageDigest.getInstance(digestAlgo);
	}

	@Override
	protected void engineInitVerify(PublicKey publicKey)
			throws InvalidKeyException {
		LOG.debug("engineInitVerify");
		if (null == this.verifySignature) {
			try {
				this.verifySignature = Signature
						.getInstance(this.signatureAlgorithm);
			} catch (NoSuchAlgorithmException e) {
				throw new InvalidKeyException(
						"no such algo: " + e.getMessage(), e);
			}
		}
		this.verifySignature.initVerify(publicKey);
	}

	@Override
	protected void engineInitSign(PrivateKey privateKey)
			throws InvalidKeyException {
		LOG.debug("engineInitSign");
		if (false == privateKey instanceof BeIDPrivateKey) {
			throw new InvalidKeyException();
		}
		this.privateKey = (BeIDPrivateKey) privateKey;
		this.messageDigest.reset();
	}

	@Override
	protected void engineUpdate(byte b) throws SignatureException {
		this.messageDigest.update(b);
		if (null != this.verifySignature) {
			this.verifySignature.update(b);
		}
	}

	@Override
	protected void engineUpdate(byte[] b, int off, int len)
			throws SignatureException {
		this.messageDigest.update(b, off, len);
		if (null != this.verifySignature) {
			this.verifySignature.update(b, off, len);
		}
	}

	@Override
	protected byte[] engineSign() throws SignatureException {
		LOG.debug("engineSign");
		byte[] digestValue = this.messageDigest.digest();
		return this.privateKey.sign(digestValue,
				this.messageDigest.getAlgorithm());
	}

	@Override
	protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
		LOG.debug("engineVerify");
		if (null == this.verifySignature) {
			throw new SignatureException("initVerify required");
		}
		boolean result = this.verifySignature.verify(sigBytes);
		return result;
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
