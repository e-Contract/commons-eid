/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2017-2020 e-Contract.be BV.
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

import java.io.ByteArrayOutputStream;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * eID based JCA {@link Signature} implementation. Supports the following
 * signature algorithms:
 * <ul>
 * <li><code>SHA1withRSA</code></li>
 * <li><code>SHA224withRSA</code></li>
 * <li><code>SHA256withRSA</code></li>
 * <li><code>SHA384withRSA</code></li>
 * <li><code>SHA512withRSA</code></li>
 * <li><code>NONEwithRSA</code>, used for mutual TLS authentication.</li>
 * <li><code>RIPEMD128withRSA</code></li>
 * <li><code>RIPEMD160withRSA</code></li>
 * <li><code>RIPEMD256withRSA</code></li>
 * <li><code>SHA1withRSAandMGF1</code>, supported by recent eID cards.</li>
 * <li><code>SHA256withRSAandMGF1</code>, supported by recent eID cards.</li>
 * <li><code>SHA256withECDSA</code>, supported by eID version 1.8 cards.</li>
 * <li><code>SHA384withECDSA</code>, supported by eID version 1.8 cards.</li>
 * <li><code>SHA512withECDSA</code>, supported by eID version 1.8 cards.</li>
 * <li><code>SHA3-256withECDSA</code>, supported by eID version 1.8 cards.</li>
 * <li><code>SHA3-384withECDSA</code>, supported by eID version 1.8 cards.</li>
 * <li><code>SHA3-512withECDSA</code>, supported by eID version 1.8 cards.</li>
 * <li><code>NONEwithECDSA</code>, supported by eID version 1.8 cards, used for
 * mutual TLS authentication.</li>
 * </ul>
 *
 * Some of the more exotic digest algorithms like SHA-224, RIPEMDxxx, and SHA3
 * will require an additional security provider like BouncyCastle.
 * 
 * @author Frank Cornelis
 * 
 */
public class BeIDSignature extends SignatureSpi {

	private static final Logger LOGGER = LoggerFactory.getLogger(BeIDSignature.class);

	private final static Map<String, String> digestAlgos;

	private final MessageDigest messageDigest;

	private BeIDPrivateKey privateKey;

	private Signature verifySignature;

	private final String signatureAlgorithm;

	private final ByteArrayOutputStream precomputedDigestOutputStream;

	static {
		digestAlgos = new HashMap<>();
		digestAlgos.put("SHA1withRSA", "SHA-1");
		digestAlgos.put("SHA224withRSA", "SHA-224");
		digestAlgos.put("SHA256withRSA", "SHA-256");
		digestAlgos.put("SHA384withRSA", "SHA-384");
		digestAlgos.put("SHA512withRSA", "SHA-512");
		digestAlgos.put("NONEwithRSA", null);
		digestAlgos.put("RIPEMD128withRSA", "RIPEMD128");
		digestAlgos.put("RIPEMD160withRSA", "RIPEMD160");
		digestAlgos.put("RIPEMD256withRSA", "RIPEMD256");
		digestAlgos.put("SHA1withRSAandMGF1", "SHA-1");
		digestAlgos.put("SHA256withRSAandMGF1", "SHA-256");
		digestAlgos.put("SHA256withECDSA", "SHA-256");
		digestAlgos.put("SHA384withECDSA", "SHA-384");
		digestAlgos.put("SHA512withECDSA", "SHA-512");
		digestAlgos.put("SHA3-256withECDSA", "SHA3-256");
		digestAlgos.put("SHA3-384withECDSA", "SHA3-384");
		digestAlgos.put("SHA3-512withECDSA", "SHA3-512");
		digestAlgos.put("NONEwithECDSA", null);
	}

	BeIDSignature(final String signatureAlgorithm) throws NoSuchAlgorithmException {
		LOGGER.debug("constructor: {}", signatureAlgorithm);
		this.signatureAlgorithm = signatureAlgorithm;
		if (false == digestAlgos.containsKey(signatureAlgorithm)) {
			LOGGER.error("no such algo: {}", signatureAlgorithm);
			throw new NoSuchAlgorithmException(signatureAlgorithm);
		}
		final String digestAlgo = digestAlgos.get(signatureAlgorithm);
		if (null != digestAlgo) {
			this.messageDigest = MessageDigest.getInstance(digestAlgo);
			this.precomputedDigestOutputStream = null;
		} else {
			LOGGER.debug("NONE message digest");
			this.messageDigest = null;
			this.precomputedDigestOutputStream = new ByteArrayOutputStream();
		}
	}

	@Override
	protected void engineInitVerify(final PublicKey publicKey) throws InvalidKeyException {
		LOGGER.debug("engineInitVerify");
		if (null == this.verifySignature) {
			try {
				this.verifySignature = Signature.getInstance(this.signatureAlgorithm);
			} catch (final NoSuchAlgorithmException nsaex) {
				throw new InvalidKeyException("no such algo: " + nsaex.getMessage(), nsaex);
			}
		}
		this.verifySignature.initVerify(publicKey);
	}

	@Override
	protected void engineInitSign(final PrivateKey privateKey) throws InvalidKeyException {
		LOGGER.debug("engineInitSign");
		if (false == privateKey instanceof BeIDPrivateKey) {
			throw new InvalidKeyException();
		}
		this.privateKey = (BeIDPrivateKey) privateKey;
		if (null != this.messageDigest) {
			this.messageDigest.reset();
		}
	}

	@Override
	protected void engineUpdate(final byte b) throws SignatureException {
		this.messageDigest.update(b);
		if (null != this.verifySignature) {
			this.verifySignature.update(b);
		}
	}

	@Override
	protected void engineUpdate(final byte[] b, final int off, final int len) throws SignatureException {
		if (null != this.messageDigest) {
			this.messageDigest.update(b, off, len);
		}
		if (null != this.precomputedDigestOutputStream) {
			this.precomputedDigestOutputStream.write(b, off, len);
		}
		if (null != this.verifySignature) {
			this.verifySignature.update(b, off, len);
		}
	}

	@Override
	protected byte[] engineSign() throws SignatureException {
		LOGGER.debug("engineSign");
		final byte[] digestValue;
		String digestAlgo;
		if (null != this.messageDigest) {
			digestValue = this.messageDigest.digest();
			digestAlgo = this.messageDigest.getAlgorithm();
			if (this.signatureAlgorithm.endsWith("andMGF1")) {
				digestAlgo += "-PSS";
			} else if (this.signatureAlgorithm.contains("ECDSA")) {
				digestAlgo += "-ECDSA";
			}
		} else if (null != this.precomputedDigestOutputStream) {
			digestValue = this.precomputedDigestOutputStream.toByteArray();
			if (this.signatureAlgorithm.contains("ECDSA")) {
				digestAlgo = "NONE-ECDSA";
			} else {
				// RSA
				digestAlgo = "NONE";
			}
		} else {
			throw new SignatureException();
		}
		return this.privateKey.sign(digestValue, digestAlgo);
	}

	@Override
	protected boolean engineVerify(final byte[] sigBytes) throws SignatureException {
		LOGGER.debug("engineVerify");
		if (null == this.verifySignature) {
			throw new SignatureException("initVerify required");
		}
		final boolean result = this.verifySignature.verify(sigBytes);
		return result;
	}

	@Override
	@Deprecated
	protected void engineSetParameter(final String param, final Object value) throws InvalidParameterException {
	}

	@Override
	@Deprecated
	protected Object engineGetParameter(final String param) throws InvalidParameterException {
		return null;
	}
}
