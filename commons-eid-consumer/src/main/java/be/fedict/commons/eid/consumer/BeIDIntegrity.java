/*
 * Commons eID Project.
 * Copyright (C) 2008-2012 FedICT.
 * Copyright (C) 2009 Frank Cornelis.
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.DigestInfo;

import be.fedict.commons.eid.consumer.tlv.TlvParser;

/**
 * Utility class for various eID related integrity checks.
 * 
 * @author Frank Cornelis
 * 
 */
public class BeIDIntegrity {

	private final static Log LOG = LogFactory.getLog(BeIDIntegrity.class);

	private final CertificateFactory certificateFactory;

	public BeIDIntegrity() {
		try {
			this.certificateFactory = CertificateFactory.getInstance("X.509");
		} catch (CertificateException e) {
			throw new RuntimeException("X.509 algo", e);
		}
	}

	public X509Certificate loadCertificate(byte[] encodedCertificate) {
		X509Certificate certificate;
		try {
			certificate = (X509Certificate) this.certificateFactory
					.generateCertificate(new ByteArrayInputStream(
							encodedCertificate));
		} catch (CertificateException e) {
			throw new RuntimeException(
					"X509 decoding error: " + e.getMessage(), e);
		}
		return certificate;
	}

	public Identity getVerifiedIdentity(byte[] identityFile,
			byte[] identitySignatureFile, X509Certificate rrnCertificate) {
		Identity identity = getVerifiedIdentity(identityFile,
				identitySignatureFile, null, rrnCertificate);
		return identity;
	}

	public Identity getVerifiedIdentity(byte[] identityFile,
			byte[] identitySignatureFile, byte[] photo,
			X509Certificate rrnCertificate) {
		PublicKey publicKey = rrnCertificate.getPublicKey();
		boolean result;
		try {
			result = verifySignature(identitySignatureFile, publicKey,
					identityFile);
		} catch (Exception e) {
			throw new SecurityException(
					"identity signature verification error: " + e.getMessage(),
					e);
		}
		if (false == result) {
			return null;
		}
		Identity identity = TlvParser.parse(identityFile, Identity.class);
		if (null != photo) {
			byte[] expectedPhotoDigest = identity.getPhotoDigest();
			byte[] actualPhotoDigest = digest(photo);
			if (false == Arrays.equals(expectedPhotoDigest, actualPhotoDigest)) {
				throw new SecurityException("photo digest mismatch");
			}
		}
		return identity;
	}

	public Address getVerifiedAddress(byte[] addressFile,
			byte[] identitySignatureFile, byte[] addressSignatureFile,
			X509Certificate rrnCertificate) {
		byte[] trimmedAddressFile = trimRight(addressFile);
		PublicKey publicKey = rrnCertificate.getPublicKey();
		boolean result;
		try {
			result = verifySignature(addressSignatureFile, publicKey,
					trimmedAddressFile, identitySignatureFile);
		} catch (Exception e) {
			throw new SecurityException(
					"address signature verification error: " + e.getMessage(),
					e);
		}
		if (false == result) {
			return null;
		}
		Address address = TlvParser.parse(addressFile, Address.class);
		return address;

	}

	private boolean verifySignature(byte[] signatureData, PublicKey publicKey,
			byte[]... data) throws NoSuchAlgorithmException,
			InvalidKeyException, SignatureException {
		Signature signature;
		signature = Signature.getInstance("SHA1withRSA");
		signature.initVerify(publicKey);
		for (byte[] dataItem : data) {
			signature.update(dataItem);
		}
		boolean result = signature.verify(signatureData);
		return result;
	}

	private byte[] digest(byte[] data) {
		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA1");
		}
		byte[] digestValue = messageDigest.digest(data);
		return digestValue;
	}

	private byte[] trimRight(byte[] addressFile) {
		int idx;
		for (idx = 0; idx < addressFile.length; idx++) {
			if (0 == addressFile[idx]) {
				break;
			}
		}
		byte[] result = new byte[idx];
		System.arraycopy(addressFile, 0, result, 0, idx);
		return result;
	}

	public boolean verifyAuthnSignature(byte[] toBeSigned,
			byte[] signatureValue, X509Certificate authnCertificate) {
		PublicKey publicKey = authnCertificate.getPublicKey();
		boolean result;
		try {
			result = verifySignature(signatureValue, publicKey, toBeSigned);
		} catch (InvalidKeyException e) {
			LOG.warn("invalid key: " + e.getMessage(), e);
			return false;
		} catch (NoSuchAlgorithmException e) {
			LOG.warn("no such algo: " + e.getMessage(), e);
			return false;
		} catch (SignatureException e) {
			LOG.warn("signature error: " + e.getMessage(), e);
			return false;
		}
		return result;
	}

	public boolean verifyNonRepSignature(byte[] expectedDigestValue,
			byte[] signatureValue, X509Certificate certificate) {
		try {
			return __verifyNonRepSignature(expectedDigestValue, signatureValue,
					certificate);
		} catch (InvalidKeyException e) {
			LOG.warn("invalid key: " + e.getMessage(), e);
			return false;
		} catch (NoSuchAlgorithmException e) {
			LOG.warn("no such algo: " + e.getMessage(), e);
			return false;
		} catch (NoSuchPaddingException e) {
			LOG.warn("no such padding: " + e.getMessage(), e);
			return false;
		} catch (BadPaddingException e) {
			LOG.warn("bad padding: " + e.getMessage(), e);
			return false;
		} catch (IOException e) {
			LOG.warn("IO error: " + e.getMessage(), e);
			return false;
		} catch (IllegalBlockSizeException e) {
			LOG.warn("illegal block size: " + e.getMessage(), e);
			return false;
		}
	}

	private boolean __verifyNonRepSignature(byte[] expectedDigestValue,
			byte[] signatureValue, X509Certificate certificate)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, IOException {
		PublicKey publicKey = certificate.getPublicKey();

		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, publicKey);
		byte[] actualSignatureDigestInfoValue = cipher.doFinal(signatureValue);

		ASN1InputStream asnInputStream = new ASN1InputStream(
				actualSignatureDigestInfoValue);
		DigestInfo actualSignatureDigestInfo = new DigestInfo(
				(ASN1Sequence) asnInputStream.readObject());
		asnInputStream.close();

		byte[] actualDigestValue = actualSignatureDigestInfo.getDigest();
		return Arrays.equals(expectedDigestValue, actualDigestValue);
	}
}
