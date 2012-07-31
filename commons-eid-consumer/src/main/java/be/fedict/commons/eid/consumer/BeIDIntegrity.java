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

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import be.fedict.commons.eid.consumer.tlv.TlvParser;

/**
 * Utility class for various eID related integrity checks.
 * 
 * @author Frank Cornelis
 * 
 */
public class BeIDIntegrity {

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
}
