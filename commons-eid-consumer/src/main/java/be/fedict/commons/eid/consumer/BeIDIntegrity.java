/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2009-2024 e-Contract.be BV.
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

package be.fedict.commons.eid.consumer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.fedict.commons.eid.consumer.tlv.TlvParser;

/**
 * Utility class for various eID related integrity checks.
 * 
 * @author Frank Cornelis
 * 
 */
public class BeIDIntegrity {

	private final static Logger LOGGER = LoggerFactory.getLogger(BeIDIntegrity.class);

	private final CertificateFactory certificateFactory;
	private final KeyFactory keyFactory;

	/**
	 * Default constructor.
	 */
	public BeIDIntegrity() {
		try {
			this.certificateFactory = CertificateFactory.getInstance("X.509");
			this.keyFactory = KeyFactory.getInstance("EC");
		} catch (final CertificateException | NoSuchAlgorithmException cex) {
			throw new RuntimeException("algo", cex);
		}
	}

	/**
	 * Loads a DER-encoded X509 certificate from a byte array.
	 * 
	 * @param encodedCertificate
	 * @return
	 */
	public X509Certificate loadCertificate(final byte[] encodedCertificate) {
		X509Certificate certificate;
		try {
			certificate = (X509Certificate) this.certificateFactory
					.generateCertificate(new ByteArrayInputStream(encodedCertificate));
		} catch (final CertificateException cex) {
			throw new RuntimeException("X509 decoding error: " + cex.getMessage(), cex);
		}
		return certificate;
	}

	/**
	 * Gives back a parsed identity file after integrity verification.
	 * 
	 * @param identityFile
	 * @param identitySignatureFile
	 * @param rrnCertificate
	 * @return
	 */
	public Identity getVerifiedIdentity(final byte[] identityFile, final byte[] identitySignatureFile,
			final X509Certificate rrnCertificate) {
		return this.getVerifiedIdentity(identityFile, identitySignatureFile, null, rrnCertificate);
	}

	/**
	 * Gives back a parsed identity file after integrity verification including the
	 * eID photo.
	 * 
	 * @param identityFile
	 * @param identitySignatureFile
	 * @param photo
	 * @param rrnCertificate
	 * @return
	 */
	public Identity getVerifiedIdentity(final byte[] identityFile, final byte[] identitySignatureFile,
			final byte[] photo, final X509Certificate rrnCertificate) {
		final PublicKey publicKey = rrnCertificate.getPublicKey();
		boolean result;
		try {
			result = verifySignature(rrnCertificate.getSigAlgName(), identitySignatureFile, publicKey, identityFile);
		} catch (final InvalidKeyException | NoSuchAlgorithmException | SignatureException ex) {
			throw new SecurityException("identity signature verification error: " + ex.getMessage(), ex);
		}
		if (!result) {
			throw new SecurityException("signature integrity error");
		}
		final Identity identity = TlvParser.parse(identityFile, Identity.class);
		if (null != photo) {
			final byte[] expectedPhotoDigest = identity.getPhotoDigest();
			final byte[] actualPhotoDigest = digest(getDigestAlgo(expectedPhotoDigest.length), photo);
			if (!Arrays.equals(expectedPhotoDigest, actualPhotoDigest)) {
				throw new SecurityException("photo digest mismatch");
			}
		}
		return identity;
	}

	/**
	 * Gives back a parsed identity file after integrity verification including the
	 * eID photo. This method will also try to validation a card authentication
	 * signature.
	 * 
	 * @param identityFile
	 * @param identitySignatureFile
	 * @param photo
	 * @param challenge
	 * @param cardSignatureValue
	 * @param basicPublicKeyFile
	 * @param rrnCertificate
	 * @return
	 */
	public Identity getVerifiedIdentity(final byte[] identityFile, final byte[] identitySignatureFile,
			final byte[] photo, final byte[] challenge, final byte[] cardSignatureValue,
			final byte[] basicPublicKeyFile, final X509Certificate rrnCertificate) {
		Identity identity = getVerifiedIdentity(identityFile, identitySignatureFile, photo, rrnCertificate);

		boolean result;
		try {
			EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(basicPublicKeyFile);
			PublicKey basicPublicKey = this.keyFactory.generatePublic(publicKeySpec);
			Signature signature = Signature.getInstance("SHA384withECDSA");
			signature.initVerify(basicPublicKey);
			signature.update(challenge);
			result = signature.verify(cardSignatureValue);
		} catch (Exception e) {
			throw new SecurityException("card basic signature incorrect");
		}
		if (!result) {
			throw new SecurityException("card basic signature incorrect");
		}

		byte[] expectedBasicPublicKeyDigest = identity.getBasicPublicKeyDigest();
		if (null == expectedBasicPublicKeyDigest) {
			throw new SecurityException("missing basic public key digest");
		}
		byte[] actualBasicPublicKeyDigest = digest(getDigestAlgo(expectedBasicPublicKeyDigest.length),
				basicPublicKeyFile);
		if (!Arrays.equals(expectedBasicPublicKeyDigest, actualBasicPublicKeyDigest)) {
			throw new SecurityException("basic public key digest mismatch");
		}
		return identity;
	}

	/**
	 * Gives back a parsed address file after integrity verification.
	 * 
	 * @param addressFile
	 * @param identitySignatureFile
	 * @param addressSignatureFile
	 * @param rrnCertificate
	 * @return
	 */
	public Address getVerifiedAddress(final byte[] addressFile, final byte[] identitySignatureFile,
			final byte[] addressSignatureFile, final X509Certificate rrnCertificate) {
		final byte[] trimmedAddressFile = trimRight(addressFile);
		final PublicKey publicKey = rrnCertificate.getPublicKey();
		boolean result;
		try {
			result = verifySignature(rrnCertificate.getSigAlgName(), addressSignatureFile, publicKey,
					trimmedAddressFile, identitySignatureFile);
		} catch (final InvalidKeyException | NoSuchAlgorithmException | SignatureException ex) {
			throw new SecurityException("address signature verification error: " + ex.getMessage(), ex);
		}
		if (!result) {
			throw new SecurityException("address integrity error");
		}
		return TlvParser.parse(addressFile, Address.class);

	}

	/**
	 * Verifies a SHA1withRSA or SHA256withECDSA signature.
	 * 
	 * @param signatureData
	 * @param publicKey
	 * @param data
	 * @return
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws SignatureException
	 */
	public boolean verifySignature(final byte[] signatureData, final PublicKey publicKey, final byte[]... data)
			throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {
		LOGGER.debug("public key algorithm: {}", publicKey.getAlgorithm());
		String signatureAlgo;
		if ("EC".equals(publicKey.getAlgorithm())) {
			signatureAlgo = "SHA256withECDSA";
		} else {
			signatureAlgo = "SHA1withRSA";
		}
		return this.verifySignature(signatureAlgo, signatureData, publicKey, data);
	}

	/**
	 * Verifies a signature.
	 * 
	 * @param signatureAlgo
	 * @param signatureData
	 * @param publicKey
	 * @param data
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws SignatureException
	 */
	public boolean verifySignature(final String signatureAlgo, byte[] signatureData, final PublicKey publicKey,
			final byte[]... data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		Signature signature;
		signature = Signature.getInstance(signatureAlgo);
		signature.initVerify(publicKey);
		for (byte[] dataItem : data) {
			signature.update(dataItem);
		}
		if (null == signatureData) {
			throw new SignatureException("missing signature data");
		}
		if (signatureAlgo.contains("ECDSA")) {
			// fix for RRN signatures
			signatureData = fixECDSASignature(signatureData);
		}
		return signature.verify(signatureData);
	}

	private byte[] fixECDSASignature(byte[] signature) {
		int derSize = signature[1];
		if (signature.length > derSize + 2) {
			LOGGER.debug("signature too long: {} bytes", signature.length - derSize - 2);
			byte[] fixedSignature = new byte[derSize + 2];
			System.arraycopy(signature, 0, fixedSignature, 0, derSize + 2);
			return fixedSignature;
		}
		return signature;
	}

	private byte[] digest(final String algoName, final byte[] data) {
		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance(algoName);
		} catch (final NoSuchAlgorithmException nsaex) {
			throw new RuntimeException(algoName);
		}
		return messageDigest.digest(data);
	}

	private byte[] trimRight(final byte[] addressFile) {
		int idx;
		for (idx = 0; idx < addressFile.length; idx++) {
			if (0 == addressFile[idx]) {
				break;
			}
		}
		final byte[] result = new byte[idx];
		System.arraycopy(addressFile, 0, result, 0, idx);
		return result;
	}

	/**
	 * Verifies an authentication signature.
	 * 
	 * @param toBeSigned
	 * @param signatureValue
	 * @param authnCertificate
	 * @return
	 */
	public boolean verifyAuthnSignature(final byte[] toBeSigned, byte[] signatureValue,
			final X509Certificate authnCertificate) {
		final PublicKey publicKey = authnCertificate.getPublicKey();
		boolean result;
		try {
			result = this.verifySignature(signatureValue, publicKey, toBeSigned);
		} catch (final InvalidKeyException ikex) {
			LOGGER.warn("invalid key: " + ikex.getMessage(), ikex);
			return false;
		} catch (final NoSuchAlgorithmException nsaex) {
			LOGGER.warn("no such algo: " + nsaex.getMessage(), nsaex);
			return false;
		} catch (final SignatureException sigex) {
			LOGGER.warn("signature error: " + sigex.getMessage(), sigex);
			return false;
		}
		return result;
	}

	/**
	 * Verifies a non-repudiation signature.
	 * 
	 * @param expectedDigestValue
	 * @param signatureValue
	 * @param certificate
	 * @return
	 */
	public boolean verifyNonRepSignature(final byte[] expectedDigestValue, final byte[] signatureValue,
			final X509Certificate certificate) {
		final PublicKey publicKey = certificate.getPublicKey();
		return verifyNonRepSignature(expectedDigestValue, signatureValue, publicKey);
	}

	/**
	 * Verifies a non-repudiation signature.
	 * 
	 * @param expectedDigestValue
	 * @param signatureValue
	 * @param publicKey
	 * @return
	 */
	public boolean verifyNonRepSignature(final byte[] expectedDigestValue, final byte[] signatureValue,
			final PublicKey publicKey) {
		try {
			return __verifyNonRepSignature(expectedDigestValue, signatureValue, publicKey);
		} catch (final InvalidKeyException ikex) {
			LOGGER.warn("invalid key: " + ikex.getMessage(), ikex);
			return false;
		} catch (final NoSuchAlgorithmException nsaex) {
			LOGGER.warn("no such algo: " + nsaex.getMessage(), nsaex);
			return false;
		} catch (final NoSuchPaddingException nspex) {
			LOGGER.warn("no such padding: " + nspex.getMessage(), nspex);
			return false;
		} catch (final BadPaddingException bpex) {
			LOGGER.warn("bad padding: " + bpex.getMessage(), bpex);
			return false;
		} catch (final IOException ioex) {
			LOGGER.warn("IO error: " + ioex.getMessage(), ioex);
			return false;
		} catch (final IllegalBlockSizeException ibex) {
			LOGGER.warn("illegal block size: " + ibex.getMessage(), ibex);
			return false;
		} catch (NoSuchProviderException e) {
			LOGGER.warn("no such provider: " + e.getMessage(), e);
			return false;
		} catch (SignatureException e) {
			LOGGER.warn("signature error: " + e.getMessage(), e);
			return false;
		}
	}

	private boolean __verifyNonRepSignature(final byte[] expectedDigestValue, final byte[] signatureValue,
			final PublicKey publicKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException, IOException, NoSuchProviderException, SignatureException {
		switch (publicKey.getAlgorithm()) {
		case "RSA":
			return __verifyNonRepSignatureRSA(expectedDigestValue, signatureValue, publicKey);
		case "EC":
			return __verifyNonRepSignatureEC(expectedDigestValue, signatureValue, publicKey);
		default:
			throw new IllegalArgumentException("unsupported key algo: " + publicKey.getAlgorithm());
		}
	}

	private boolean __verifyNonRepSignatureRSA(final byte[] expectedDigestValue, final byte[] signatureValue,
			final PublicKey publicKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException, IOException {
		final Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, publicKey);
		final byte[] actualSignatureDigestInfoValue = cipher.doFinal(signatureValue);

		final DigestInfo actualSignatureDigestInfo;
		try (ASN1InputStream asnInputStream = new ASN1InputStream(actualSignatureDigestInfoValue)) {
			actualSignatureDigestInfo = new DigestInfo((ASN1Sequence) asnInputStream.readObject());
		}

		final byte[] actualDigestValue = actualSignatureDigestInfo.getDigest();
		return Arrays.equals(expectedDigestValue, actualDigestValue);
	}

	private boolean __verifyNonRepSignatureEC(final byte[] expectedDigestValue, final byte[] signatureValue,
			final PublicKey publicKey) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException,
			SignatureException, IOException {
		Signature signature = Signature.getInstance("NONEwithECDSA", BouncyCastleProvider.PROVIDER_NAME);
		signature.initVerify(publicKey);
		signature.update(expectedDigestValue);
		return signature.verify(signatureValue);
	}

	private String getDigestAlgo(final int hashSize) throws SecurityException {
		switch (hashSize) {
		case 20:
			return "SHA-1";
		case 28:
			return "SHA-224";
		case 32:
			return "SHA-256";
		case 48:
			return "SHA-384";
		case 64:
			return "SHA-512";
		}
		throw new SecurityException("Failed to find guess algorithm for hash size of " + hashSize + " bytes");
	}
}
