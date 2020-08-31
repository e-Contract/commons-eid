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

package test.unit.be.fedict.commons.eid.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.fedict.commons.eid.consumer.Address;
import be.fedict.commons.eid.consumer.BeIDIntegrity;
import be.fedict.commons.eid.consumer.Identity;

public class BeIDIntegrityTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(BeIDIntegrityTest.class);

	@BeforeAll
	public static void setup() throws Exception {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	public void testIdentityIntegrity() throws Exception {
		// setup
		byte[] identityFile = IOUtils.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-identity.tlv"));
		byte[] identitySignatureFile = IOUtils
				.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-identity-sign.der"));
		byte[] rrnCertFile = IOUtils.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-rrn-cert.der"));
		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();

		// operate
		X509Certificate rrnCert = beIDIntegrity.loadCertificate(rrnCertFile);
		Identity identity = beIDIntegrity.getVerifiedIdentity(identityFile, identitySignatureFile, rrnCert);

		// verify
		assertNotNull(identity);
		LOGGER.debug("name: {}", identity.getFirstName());
		assertEquals("Alice Geldigekaart", identity.getFirstName());
	}

	@Test
	public void testIdentityIntegrityCorruption() throws Exception {
		// setup
		byte[] identityFile = IOUtils.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-identity.tlv"));
		byte[] identitySignatureFile = IOUtils
				.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-identity-sign.der"));
		byte[] rrnCertFile = IOUtils.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-rrn-cert.der"));
		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();

		// setup: corrupt identity file
		identityFile[0] = 4;

		// operate
		X509Certificate rrnCert = beIDIntegrity.loadCertificate(rrnCertFile);

		// operate & verify
		try {
			beIDIntegrity.getVerifiedIdentity(identityFile, identitySignatureFile, rrnCert);
			fail();
		} catch (SecurityException e) {
			// expected
		}
	}

	@Test
	public void testPhotoIntegrity() throws Exception {
		// setup
		byte[] identityFile = IOUtils.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-identity.tlv"));
		byte[] identitySignatureFile = IOUtils
				.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-identity-sign.der"));
		byte[] rrnCertFile = IOUtils.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-rrn-cert.der"));
		byte[] photoData = IOUtils.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-photo.jpg"));
		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();

		// operate
		X509Certificate rrnCert = beIDIntegrity.loadCertificate(rrnCertFile);
		Identity identity = beIDIntegrity.getVerifiedIdentity(identityFile, identitySignatureFile, photoData, rrnCert);

		// verify
		assertNotNull(identity);
		LOGGER.debug("name: {}", identity.getFirstName());
		assertEquals("Alice Geldigekaart", identity.getFirstName());
	}

	@Test
	public void testPhotoIntegrityCorruption() throws Exception {
		// setup
		byte[] identityFile = IOUtils.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-identity.tlv"));
		byte[] identitySignatureFile = IOUtils
				.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-identity-sign.der"));
		byte[] rrnCertFile = IOUtils.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-rrn-cert.der"));
		byte[] photoData = IOUtils.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-photo.jpg"));
		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();

		// setup: corrupt photo
		photoData[0] = 0;

		// operate
		X509Certificate rrnCert = beIDIntegrity.loadCertificate(rrnCertFile);

		// operate & verify
		try {
			beIDIntegrity.getVerifiedIdentity(identityFile, identitySignatureFile, photoData, rrnCert);
			fail();
		} catch (SecurityException e) {
			// expected
		}
	}

	@Test
	public void testAddressIntegrity() throws Exception {
		// setup
		byte[] identitySignatureFile = IOUtils
				.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-identity-sign.der"));
		byte[] rrnCertFile = IOUtils.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-rrn-cert.der"));
		byte[] addressFile = IOUtils.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-address.tlv"));
		byte[] addressSignatureFile = IOUtils
				.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-address-sign.der"));

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();

		// operate
		X509Certificate rrnCert = beIDIntegrity.loadCertificate(rrnCertFile);
		Address address = beIDIntegrity.getVerifiedAddress(addressFile, identitySignatureFile, addressSignatureFile,
				rrnCert);

		// verify
		assertNotNull(address);
		LOGGER.debug("ZIP: {}", address.getZip());
		assertEquals("2000", address.getZip());
	}

	@Test
	public void testAddressIntegrityCorruption() throws Exception {
		// setup
		byte[] identitySignatureFile = IOUtils
				.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-identity-sign.der"));
		byte[] rrnCertFile = IOUtils.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-rrn-cert.der"));
		byte[] addressFile = IOUtils.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-address.tlv"));
		byte[] addressSignatureFile = IOUtils
				.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-address-sign.der"));

		// setup: corrupt address
		addressFile[0] = 123;

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();

		// operate
		X509Certificate rrnCert = beIDIntegrity.loadCertificate(rrnCertFile);

		// operate & verify
		try {
			beIDIntegrity.getVerifiedAddress(addressFile, identitySignatureFile, addressSignatureFile, rrnCert);
			fail();
		} catch (SecurityException e) {
			// expected
		}
	}

	@Test
	public void testVerifyNonRepSignatureRSA() throws Exception {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		PrivateKey privateKey = keyPair.getPrivate();
		PublicKey publicKey = keyPair.getPublic();

		byte[] data = "hello world".getBytes();
		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initSign(privateKey);
		signature.update(data);
		byte[] signatureValue = signature.sign();

		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
		byte[] digestValue = messageDigest.digest(data);

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		boolean result = beIDIntegrity.verifyNonRepSignature(digestValue, signatureValue, publicKey);
		assertTrue(result);
	}

	@Test
	public void testVerifyNonRepSignatureEC() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp384r1");
		keyGen.initialize(ecSpec, random);

		KeyPair keyPair = keyGen.generateKeyPair();
		PrivateKey privateKey = keyPair.getPrivate();
		PublicKey publicKey = keyPair.getPublic();

		Signature signature = Signature.getInstance("SHA256withECDSA");

		byte[] data = "hello world".getBytes();
		signature.initSign(privateKey);
		signature.update(data);
		byte[] signatureValue = signature.sign();

		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
		byte[] digestValue = messageDigest.digest(data);

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		boolean result = beIDIntegrity.verifyNonRepSignature(digestValue, signatureValue, publicKey);
		assertTrue(result);
	}
}
