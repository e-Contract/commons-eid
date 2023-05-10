/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2017-2023 e-Contract.be BV.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcECContentSignerBuilder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
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

	@Test
	public void testIntegrityV17EC() throws Exception {
		// setup
		byte[] identityFile = IOUtils
				.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-eid-v17-ec/identity.tlv"));
		byte[] identitySignatureFile = IOUtils
				.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-eid-v17-ec/identity-signature.der"));
		byte[] rrnCertFile = IOUtils
				.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-eid-v17-ec/rrn-cert.der"));
		byte[] photoFile = IOUtils
				.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-eid-v17-ec/photo.jpg"));
		byte[] addressFile = IOUtils
				.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-eid-v17-ec/address.tlv"));
		byte[] addressSignatureFile = IOUtils
				.toByteArray(BeIDIntegrityTest.class.getResourceAsStream("/test-eid-v17-ec/address-signature.der"));
		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();

		// operate
		X509Certificate rrnCert = beIDIntegrity.loadCertificate(rrnCertFile);
		Identity identity = beIDIntegrity.getVerifiedIdentity(identityFile, identitySignatureFile, photoFile, rrnCert);

		Address address = beIDIntegrity.getVerifiedAddress(addressFile, identitySignatureFile, addressSignatureFile,
				rrnCert);

		// verify
		assertNotNull(identity);
		LOGGER.debug("name: {}", identity.getFirstName());

		assertNotNull(address);
		LOGGER.debug("street: {}", address.getStreetAndNumber());
	}

	@Test
	public void testIntegrity() throws Exception {
		KeyPair rrnKeyPair = generateKeyPair(1024, "RSA");
		PublicKey rrnPublicKey = rrnKeyPair.getPublic();
		PrivateKey rrnPrivateKey = rrnKeyPair.getPrivate();
		X509Certificate nrnCertificate = generateSelfSignedCertificate(rrnPublicKey, "CN=Test RRN", rrnPrivateKey,
				"SHA256withRSA");

		byte[] photoFile = "eID photo".getBytes();

		ByteArrayOutputStream identityOutputStream = new ByteArrayOutputStream();
		identityOutputStream.write(new byte[] { 6, 4, '1', '2', '3', '4' }); // national register number tag
		identityOutputStream.write(17); // photo digest tag
		identityOutputStream.write(256 / 8); // sha256 size
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
		messageDigest.update(photoFile);
		byte[] photoDigest = messageDigest.digest();
		identityOutputStream.write(photoDigest);
		byte[] identityFile = identityOutputStream.toByteArray();

		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initSign(rrnPrivateKey);
		signature.update(identityFile);
		byte[] identitySignatureFile = signature.sign();

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		Identity identity = beIDIntegrity.getVerifiedIdentity(identityFile, identitySignatureFile, photoFile,
				nrnCertificate);
		assertNotNull(identity);
		assertEquals("1234", identity.nationalNumber);
	}

	@Test
	public void testEmptyCardEUStartDate() throws Exception {
		KeyPair rrnKeyPair = generateKeyPair(1024, "RSA");
		PublicKey rrnPublicKey = rrnKeyPair.getPublic();
		PrivateKey rrnPrivateKey = rrnKeyPair.getPrivate();
		X509Certificate nrnCertificate = generateSelfSignedCertificate(rrnPublicKey, "CN=Test RRN", rrnPrivateKey,
				"SHA256withRSA");

		byte[] photoFile = "eID photo".getBytes();

		ByteArrayOutputStream identityOutputStream = new ByteArrayOutputStream();
		identityOutputStream.write(new byte[] { 6, 4, '1', '2', '3', '4', // national register number tag
				31, 0 // empty cardEUStartDate
		});
		identityOutputStream.write(17); // photo digest tag
		identityOutputStream.write(256 / 8); // sha256 size
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
		messageDigest.update(photoFile);
		byte[] photoDigest = messageDigest.digest();
		identityOutputStream.write(photoDigest);
		byte[] identityFile = identityOutputStream.toByteArray();

		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initSign(rrnPrivateKey);
		signature.update(identityFile);
		byte[] identitySignatureFile = signature.sign();

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		Identity identity = beIDIntegrity.getVerifiedIdentity(identityFile, identitySignatureFile, photoFile,
				nrnCertificate);
		assertNotNull(identity);
		assertEquals("1234", identity.nationalNumber);
	}

	@Test
	public void testIntegritySHA1() throws Exception {
		KeyPair rrnKeyPair = generateKeyPair(1024, "RSA");
		PublicKey rrnPublicKey = rrnKeyPair.getPublic();
		PrivateKey rrnPrivateKey = rrnKeyPair.getPrivate();
		X509Certificate nrnCertificate = generateSelfSignedCertificate(rrnPublicKey, "CN=Test RRN", rrnPrivateKey,
				"SHA1withRSA");

		byte[] photoFile = "eID photo".getBytes();

		ByteArrayOutputStream identityOutputStream = new ByteArrayOutputStream();
		identityOutputStream.write(new byte[] { 6, 4, '1', '2', '3', '4' }); // national register number tag
		identityOutputStream.write(17); // photo digest tag
		identityOutputStream.write(160 / 8); // sha1 size
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
		messageDigest.update(photoFile);
		byte[] photoDigest = messageDigest.digest();
		identityOutputStream.write(photoDigest);
		byte[] identityFile = identityOutputStream.toByteArray();

		Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initSign(rrnPrivateKey);
		signature.update(identityFile);
		byte[] identitySignatureFile = signature.sign();

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		Identity identity = beIDIntegrity.getVerifiedIdentity(identityFile, identitySignatureFile, photoFile,
				nrnCertificate);
		assertNotNull(identity);
		assertEquals("1234", identity.nationalNumber);
	}

	@Test
	public void testIntegrityEC() throws Exception {
		KeyPair rrnKeyPair = generateKeyPair(0, "EC");
		PublicKey rrnPublicKey = rrnKeyPair.getPublic();
		PrivateKey rrnPrivateKey = rrnKeyPair.getPrivate();
		X509Certificate nrnCertificate = generateSelfSignedCertificate(rrnPublicKey, "CN=Test RRN", rrnPrivateKey,
				"SHA384withECDSA");

		byte[] photoFile = "eID photo".getBytes();

		ByteArrayOutputStream identityOutputStream = new ByteArrayOutputStream();
		identityOutputStream.write(new byte[] { 6, 4, '1', '2', '3', '4' }); // national register number tag
		identityOutputStream.write(17); // photo digest tag
		identityOutputStream.write(384 / 8); // sha384 size
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-384");
		messageDigest.update(photoFile);
		byte[] photoDigest = messageDigest.digest();
		identityOutputStream.write(photoDigest);
		byte[] identityFile = identityOutputStream.toByteArray();

		Signature signature = Signature.getInstance("SHA384withECDSA");
		signature.initSign(rrnPrivateKey);
		signature.update(identityFile);
		byte[] identitySignatureFile = signature.sign();

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		Identity identity = beIDIntegrity.getVerifiedIdentity(identityFile, identitySignatureFile, photoFile,
				nrnCertificate);
		assertNotNull(identity);
		assertEquals("1234", identity.nationalNumber);
	}

	private KeyPair generateKeyPair(int keySize, String keyAlgorithm) throws Exception {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(keyAlgorithm);
		SecureRandom random = new SecureRandom();
		if ("RSA".equals(keyAlgorithm)) {
			keyPairGenerator.initialize(new RSAKeyGenParameterSpec(keySize, RSAKeyGenParameterSpec.F4), random);
		} else {
			keyPairGenerator.initialize(new ECGenParameterSpec("secp384r1"));
		}
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		return keyPair;
	}

	private X509Certificate generateSelfSignedCertificate(PublicKey publicKey, String subjectDn,

			PrivateKey privateKey, String signatureAlgorithm)
			throws IOException, InvalidKeyException, IllegalStateException, NoSuchAlgorithmException,
			SignatureException, CertificateException, OperatorCreationException {
		LocalDateTime notBefore = LocalDateTime.now();
		LocalDateTime notAfter = notBefore.plusYears(1);
		X500Name issuerName = new X500Name(subjectDn);
		X500Name subjectName = new X500Name(subjectDn);
		BigInteger serial = new BigInteger(128, new SecureRandom());
		SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
		X509v3CertificateBuilder x509v3CertificateBuilder = new X509v3CertificateBuilder(issuerName, serial,
				Date.from(notBefore.atZone(ZoneId.systemDefault()).toInstant()),
				Date.from(notAfter.atZone(ZoneId.systemDefault()).toInstant()), subjectName, publicKeyInfo);

		JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();

		x509v3CertificateBuilder.addExtension(Extension.subjectKeyIdentifier, false,
				extensionUtils.createSubjectKeyIdentifier(publicKey));

		AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(signatureAlgorithm);
		AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
		AsymmetricKeyParameter asymmetricKeyParameter = PrivateKeyFactory.createKey(privateKey.getEncoded());

		ContentSigner contentSigner;
		if (signatureAlgorithm.contains("RSA")) {
			contentSigner = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(asymmetricKeyParameter);
		} else {
			contentSigner = new BcECContentSignerBuilder(sigAlgId, digAlgId).build(asymmetricKeyParameter);
		}

		X509CertificateHolder x509CertificateHolder = x509v3CertificateBuilder.build(contentSigner);

		byte[] encodedCertificate = x509CertificateHolder.getEncoded();

		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		X509Certificate certificate = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(encodedCertificate));
		return certificate;
	}
}
