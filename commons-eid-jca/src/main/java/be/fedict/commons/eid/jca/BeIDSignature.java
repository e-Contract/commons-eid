/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2017-2024 e-Contract.be BV.
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

import be.fedict.commons.eid.client.impl.BeIDDigest;

/**
 * eID based JCA {@link Signature} implementation. Supports the following
 * signature algorithms:
 * <ul>
 * <li><code>SHA1withRSA</code></li>
 * <li><code>SHA224withRSA</code></li>
 * <li><code>SHA256withRSA</code></li>
 * <li><code>SHA384withRSA</code></li>
 * <li><code>SHA512withRSA</code></li>
 * <li><code>SHA3-256withRSA</code></li>
 * <li><code>SHA3-384withRSA</code></li>
 * <li><code>SHA3-512withRSA</code></li>
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
 * <li><code>SHA256withECDSAinP1363Format</code>, supported by eID version 1.8
 * cards.</li>
 * <li><code>SHA384withECDSAinP1363Format</code>, supported by eID version 1.8
 * cards.</li>
 * <li><code>SHA512withECDSAinP1363Format</code>, supported by eID version 1.8
 * cards.</li>
 * <li><code>SHA3-256withECDSAinP1363Format</code>, supported by eID version 1.8
 * cards.</li>
 * <li><code>SHA3-384withECDSAinP1363Format</code>, supported by eID version 1.8
 * cards.</li>
 * <li><code>SHA3-512withECDSAinP1363Format</code>, supported by eID version 1.8
 * cards.</li>
 * <li><code>NONEwithECDSA</code>, supported by eID version 1.8 cards, used for
 * mutual TLS authentication.</li>
 * </ul>
 *
 * Some of the more exotic digest algorithms like SHA-224, RIPEMDxxx, and SHA3
 * might require an additional security provider like BouncyCastle.
 * 
 * @author Frank Cornelis
 * 
 */
public class BeIDSignature extends SignatureSpi {

	private static final Logger LOGGER = LoggerFactory.getLogger(BeIDSignature.class);

	private final static Map<String, BeIDDigest> digestAlgos;

	private final MessageDigest messageDigest;

	private AbstractBeIDPrivateKey privateKey;

	private Signature verifySignature;

	private final String signatureAlgorithm;

	private final ByteArrayOutputStream precomputedDigestOutputStream;

	static {
		digestAlgos = new HashMap<>();
		digestAlgos.put("SHA1withRSA", BeIDDigest.SHA_1);
		digestAlgos.put("SHA224withRSA", BeIDDigest.SHA_224);
		digestAlgos.put("SHA256withRSA", BeIDDigest.SHA_256);
		digestAlgos.put("SHA384withRSA", BeIDDigest.SHA_384);
		digestAlgos.put("SHA512withRSA", BeIDDigest.SHA_512);
		digestAlgos.put("NONEwithRSA", BeIDDigest.NONE);
		digestAlgos.put("RIPEMD128withRSA", BeIDDigest.RIPEMD_128);
		digestAlgos.put("RIPEMD160withRSA", BeIDDigest.RIPEMD_160);
		digestAlgos.put("RIPEMD256withRSA", BeIDDigest.RIPEMD_256);
		digestAlgos.put("SHA1withRSAandMGF1", BeIDDigest.SHA_1_PSS);
		digestAlgos.put("SHA256withRSAandMGF1", BeIDDigest.SHA_256_PSS);
		digestAlgos.put("SHA256withECDSA", BeIDDigest.ECDSA_SHA_2_256);
		digestAlgos.put("SHA384withECDSA", BeIDDigest.ECDSA_SHA_2_384);
		digestAlgos.put("SHA512withECDSA", BeIDDigest.ECDSA_SHA_2_512);
		digestAlgos.put("SHA3-256withECDSA", BeIDDigest.ECDSA_SHA_3_256);
		digestAlgos.put("SHA3-384withECDSA", BeIDDigest.ECDSA_SHA_3_384);
		digestAlgos.put("SHA3-512withECDSA", BeIDDigest.ECDSA_SHA_3_512);
		digestAlgos.put("SHA256withECDSAinP1363Format", BeIDDigest.ECDSA_SHA_2_256_P1363);
		digestAlgos.put("SHA384withECDSAinP1363Format", BeIDDigest.ECDSA_SHA_2_384_P1363);
		digestAlgos.put("SHA512withECDSAinP1363Format", BeIDDigest.ECDSA_SHA_2_512_P1363);
		digestAlgos.put("SHA3-256withECDSAinP1363Format", BeIDDigest.ECDSA_SHA_3_256_P1363);
		digestAlgos.put("SHA3-384withECDSAinP1363Format", BeIDDigest.ECDSA_SHA_3_384_P1363);
		digestAlgos.put("SHA3-512withECDSAinP1363Format", BeIDDigest.ECDSA_SHA_3_512_P1363);
		digestAlgos.put("NONEwithECDSA", BeIDDigest.ECDSA_NONE);
		digestAlgos.put("NONEwithECDSAinP1363Format", BeIDDigest.ECDSA_NONE_P1363);
		digestAlgos.put("SHA3-256withRSA", BeIDDigest.SHA3_256);
		digestAlgos.put("SHA3-384withRSA", BeIDDigest.SHA3_384);
		digestAlgos.put("SHA3-512withRSA", BeIDDigest.SHA3_512);
	}

	BeIDSignature(final String signatureAlgorithm) throws NoSuchAlgorithmException {
		LOGGER.debug("constructor: {}", signatureAlgorithm);
		this.signatureAlgorithm = signatureAlgorithm;
		if (!digestAlgos.containsKey(signatureAlgorithm)) {
			LOGGER.error("no such algo: {}", signatureAlgorithm);
			throw new NoSuchAlgorithmException(signatureAlgorithm);
		}
		final String digestAlgo = digestAlgos.get(signatureAlgorithm).getAlgorithm();
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
		LOGGER.debug("engineInitVerify: {}", publicKey.getClass().getName());
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
		if (!(privateKey instanceof AbstractBeIDPrivateKey)) {
			throw new InvalidKeyException();
		}
		this.privateKey = (AbstractBeIDPrivateKey) privateKey;
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
		if (null != this.messageDigest) {
			digestValue = this.messageDigest.digest();
		} else if (null != this.precomputedDigestOutputStream) {
			digestValue = this.precomputedDigestOutputStream.toByteArray();
		} else {
			throw new SignatureException();
		}
		BeIDDigest beidDigest = digestAlgos.get(this.signatureAlgorithm);
		return this.privateKey.sign(digestValue, beidDigest);
	}

	@Override
	protected boolean engineVerify(final byte[] sigBytes) throws SignatureException {
		LOGGER.debug("engineVerify");
		if (null == this.verifySignature) {
			throw new SignatureException("initVerify required");
		}
		return this.verifySignature.verify(sigBytes);
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
