/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2014-2023 e-Contract.be BV.
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
package test.integ.be.fedict.commons.eid.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.client.BeIDCardsException;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.client.event.BeIDCardListener;
import be.fedict.commons.eid.client.impl.BeIDDigest;
import be.fedict.commons.eid.consumer.Address;
import be.fedict.commons.eid.consumer.BeIDIntegrity;
import be.fedict.commons.eid.consumer.CardData;
import be.fedict.commons.eid.consumer.Identity;
import be.fedict.commons.eid.consumer.tlv.ByteArrayParser;

public class BeIDCardTest {

	protected static final Logger LOGGER = LoggerFactory.getLogger(BeIDCardTest.class);

	protected BeIDCards beIDCards;

	@BeforeAll
	public static void setup() throws Exception {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	public void testReadFiles() throws Exception {
		BeIDCard beIDCard = getBeIDCard();
		beIDCard.addCardListener(new TestBeIDCardListener());

		LOGGER.debug("reading identity file");
		byte[] identityFile = beIDCard.readFile(FileType.Identity);
		LOGGER.debug("reading identity signature file");
		byte[] identitySignatureFile = beIDCard.readFile(FileType.IdentitySignature);
		LOGGER.debug("reading RRN certificate file");
		byte[] rrnCertificateFile = beIDCard.readFile(FileType.RRNCertificate);
		LOGGER.debug("reading auth certificate file");
		byte[] authnCertFile = beIDCard.readFile(FileType.AuthentificationCertificate);
		LOGGER.debug("reading sign certificate file");
		byte[] signCertFile = beIDCard.readFile(FileType.NonRepudiationCertificate);
		LOGGER.debug("reading root certificate file");
		byte[] rootCertFile = beIDCard.readFile(FileType.RootCertificate);
		LOGGER.debug("reading CA certificate file");
		byte[] caCertFile = beIDCard.readFile(FileType.CACertificate);
		LOGGER.debug("reading Photo file");
		byte[] photoFile = beIDCard.readFile(FileType.Photo);
		File photoFileFile = File.createTempFile("eid-photo-", ".jpg");
		FileUtils.writeByteArrayToFile(photoFileFile, photoFile);

		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		X509Certificate rrnCertificate = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(rrnCertificateFile));
		X509Certificate authnCert = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(authnCertFile));
		X509Certificate signCert = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(signCertFile));
		X509Certificate rootCert = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(rootCertFile));
		X509Certificate caCert = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(caCertFile));
		LOGGER.debug("RRN cert: {}", rrnCertificate);
		LOGGER.debug("auth cert: {}", authnCert);
		LOGGER.debug("sign cert: {}", signCert);
		LOGGER.debug("root cert: {}", rootCert);
		LOGGER.debug("ca cert: {}", caCert);

		beIDCard.close();

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		Identity identity = beIDIntegrity.getVerifiedIdentity(identityFile, identitySignatureFile, photoFile,
				rrnCertificate);

		assertNotNull(identity);
		assertNotNull(identity.getNationalNumber());
		LOGGER.debug("eID photo file: {}", photoFileFile.getAbsoluteFile());
	}

	@Test
	public void testBasicPublic() throws Exception {
		BeIDCard beIDCard = getBeIDCard();
		beIDCard.addCardListener(new TestBeIDCardListener());

		byte[] toBeSigned = "hello world".getBytes();
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-384");
		byte[] digestValue = messageDigest.digest(toBeSigned);

		byte[] signatureValue = beIDCard.internalAuthenticate(digestValue);
		LOGGER.debug("signature size: {} bytes", signatureValue.length);

		ECPublicKey basicPublicKey = beIDCard.getBasicPublicKey();

		Signature signature = Signature.getInstance("SHA384withECDSA");
		signature.initVerify(basicPublicKey);
		signature.update(toBeSigned);
		boolean result = signature.verify(signatureValue);
		assertTrue(result);
	}

	@Test
	public void testReadFilesWithCardAuthentication() throws Exception {
		BeIDCard beIDCard = getBeIDCard();
		beIDCard.addCardListener(new TestBeIDCardListener());

		LOGGER.debug("reading identity file");
		byte[] identityFile = beIDCard.readFile(FileType.Identity);
		LOGGER.debug("reading identity signature file");
		byte[] identitySignatureFile = beIDCard.readFile(FileType.IdentitySignature);
		LOGGER.debug("reading RRN certificate file");
		byte[] rrnCertificateFile = beIDCard.readFile(FileType.RRNCertificate);
		LOGGER.debug("reading auth certificate file");
		beIDCard.readFile(FileType.AuthentificationCertificate);
		LOGGER.debug("reading sign certificate file");
		beIDCard.readFile(FileType.NonRepudiationCertificate);
		LOGGER.debug("reading root certificate file");
		beIDCard.readFile(FileType.RootCertificate);
		LOGGER.debug("reading CA certificate file");
		beIDCard.readFile(FileType.CACertificate);
		LOGGER.debug("reading Photo file");
		byte[] photoFile = beIDCard.readFile(FileType.Photo);
		File photoFileFile = File.createTempFile("eid-photo-", ".jpg");
		FileUtils.writeByteArrayToFile(photoFileFile, photoFile);
		byte[] basicPublicKeyFile = beIDCard.readFile(FileType.BasicPublic);

		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		X509Certificate rrnCertificate = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(rrnCertificateFile));

		byte[] toBeSigned = "nonce||challenge".getBytes();
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-384");
		messageDigest.update(toBeSigned);
		byte[] digestValue = messageDigest.digest();

		byte[] signatureValue = beIDCard.internalAuthenticate(digestValue);

		beIDCard.close();

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		Identity identity = beIDIntegrity.getVerifiedIdentity(identityFile, identitySignatureFile, photoFile,
				toBeSigned, signatureValue, basicPublicKeyFile, rrnCertificate);

		assertNotNull(identity);
		assertNotNull(identity.getNationalNumber());
		LOGGER.debug("eID photo file: {}", photoFileFile.getAbsoluteFile());
	}

	@Test
	public void testAddressFileValidation() throws Exception {
		BeIDCard beIDCard = getBeIDCard();
		beIDCard.addCardListener(new TestBeIDCardListener());

		LOGGER.debug("reading address file");
		byte[] addressFile = beIDCard.readFile(FileType.Address);
		LOGGER.debug("reading address signature file");
		byte[] addressSignatureFile = beIDCard.readFile(FileType.AddressSignature);
		LOGGER.debug("reading identity signature file");
		byte[] identitySignatureFile = beIDCard.readFile(FileType.IdentitySignature);
		LOGGER.debug("reading RRN certificate file");
		byte[] rrnCertificateFile = beIDCard.readFile(FileType.RRNCertificate);

		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		X509Certificate rrnCertificate = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(rrnCertificateFile));
		LOGGER.debug("RRN cert: {}", rrnCertificate);

		beIDCard.close();

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		Address address = beIDIntegrity.getVerifiedAddress(addressFile, identitySignatureFile, addressSignatureFile,
				rrnCertificate);

		assertNotNull(address);
		assertNotNull(address.getMunicipality());
	}

	@Test
	public void testSaveAddressFile() throws Exception {
		BeIDCard beIDCard = getBeIDCard();
		beIDCard.addCardListener(new TestBeIDCardListener());

		LOGGER.debug("reading address file");
		byte[] addressFile = beIDCard.readFile(FileType.Address);

		File tmpFile = File.createTempFile("address-", ".tlv");
		FileUtils.writeByteArrayToFile(tmpFile, addressFile);
		LOGGER.debug("tmp address file: {}", tmpFile.getAbsolutePath());
	}

	@Test
	public void testSaveIdentityFiles() throws Exception {
		BeIDCard beIDCard = getBeIDCard();
		beIDCard.addCardListener(new TestBeIDCardListener());

		byte[] addressData = beIDCard.readFile(FileType.Address);
		File tmpAddressFile = File.createTempFile("address-", ".tlv");
		FileUtils.writeByteArrayToFile(tmpAddressFile, addressData);

		byte[] identityData = beIDCard.readFile(FileType.Identity);
		File tmpIdentityFile = File.createTempFile("identity-", ".tlv");
		FileUtils.writeByteArrayToFile(tmpIdentityFile, identityData);

		byte[] identitySignatureData = beIDCard.readFile(FileType.IdentitySignature);
		File tmpIdentitySignatureFile = File.createTempFile("identity-signature-", ".der");
		FileUtils.writeByteArrayToFile(tmpIdentitySignatureFile, identitySignatureData);

		byte[] photoData = beIDCard.readFile(FileType.Photo);
		File tmpPhotoFile = File.createTempFile("photo-", ".jpg");
		FileUtils.writeByteArrayToFile(tmpPhotoFile, photoData);

		byte[] addressSignatureData = beIDCard.readFile(FileType.AddressSignature);
		File tmpAddressSignatureFile = File.createTempFile("address-signature-", ".der");
		FileUtils.writeByteArrayToFile(tmpAddressSignatureFile, addressSignatureData);

		byte[] rrnCertificateData = beIDCard.readFile(FileType.RRNCertificate);
		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		X509Certificate rrnCertificate = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(rrnCertificateData));
		byte[] rrnCertificateDataClean = rrnCertificate.getEncoded();
		File tmpCertificateFile = File.createTempFile("rrn-cert-", ".der");
		FileUtils.writeByteArrayToFile(tmpCertificateFile, rrnCertificateDataClean);

		LOGGER.debug("tmp identity file: {}", tmpIdentityFile.getAbsolutePath());
		LOGGER.debug("tmp address file: {}", tmpAddressFile.getAbsolutePath());
		LOGGER.debug("tmp identity signature file: {}", tmpIdentitySignatureFile.getAbsolutePath());
		LOGGER.debug("tmp photo file: {}", tmpPhotoFile.getAbsolutePath());
		LOGGER.debug("tmp address signature file: {}", tmpAddressSignatureFile.getAbsolutePath());
		LOGGER.debug("tmp RRN certificate file: {}", tmpCertificateFile.getAbsolutePath());
	}

	@Test
	public void testAuthnSignature() throws Exception {
		BeIDCard beIDCard = getBeIDCard();

		byte[] toBeSigned = new byte[10];
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(toBeSigned);

		X509Certificate authnCertificate = beIDCard.getAuthenticationCertificate();
		LOGGER.debug("certificate: {}", authnCertificate);

		byte[] signatureValue;
		try {
			signatureValue = beIDCard.signAuthn(toBeSigned, false);
		} finally {
			beIDCard.close();
		}

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		boolean result = beIDIntegrity.verifyAuthnSignature(toBeSigned, signatureValue, authnCertificate);

		assertTrue(result);
	}

	@Test
	public void testRRNCertificate() throws Exception {
		BeIDCard beIDCard = getBeIDCard();

		X509Certificate rrnCertificate = beIDCard.getRRNCertificate();

		assertNotNull(rrnCertificate);
		LOGGER.debug("RRN certificate: {}", rrnCertificate);
	}

	@Test
	public void testAuthCertificate() throws Exception {
		BeIDCard beIDCard = getBeIDCard();

		X509Certificate authnCertificate = beIDCard.getAuthenticationCertificate();

		assertNotNull(authnCertificate);
		LOGGER.debug("authentication certificate: {}", authnCertificate);
	}

	@Test
	public void testGetCardData() throws Exception {
		BeIDCard beIDCard = getBeIDCard();

		byte[] cardDataFile = beIDCard.getCardData();

		assertNotNull(cardDataFile);
		LOGGER.debug("card data file size: {}", cardDataFile.length);
		LOGGER.debug("card data file: {}", Hex.toHexString(cardDataFile));
		CardData cardData = ByteArrayParser.parse(cardDataFile, CardData.class);
		LOGGER.debug("PKCS#1 1.5 supported: {}", cardData.isRSASSAPKCS115Supported());
		LOGGER.debug("PSS supported: {}", cardData.isRSASSAPSSSupported());
		LOGGER.debug("PKCS#1 support: {}", Integer.toHexString(cardData.getPkcs1Support()));
		LOGGER.debug("applet version: {}", cardData.getApplicationVersion());
		LOGGER.debug("global OS version: {}", cardData.getGlobalOSVersion());
		LOGGER.debug("applet life cycle: {}", cardData.getApplicationLifeCycle());
		LOGGER.debug("remaining attempts authn PIN: {}", cardData.getAuthPinRemainingAttempts());
	}

	@Test
	public void testPSSSignature() throws Exception {
		BeIDCard beIDCard = getBeIDCard();

		byte[] toBeSigned = new byte[10];
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(toBeSigned);

		X509Certificate authnCertificate = beIDCard.getAuthenticationCertificate();

		MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
		byte[] digestValue = messageDigest.digest(toBeSigned);

		byte[] signatureValue;
		try {
			signatureValue = beIDCard.sign(digestValue, BeIDDigest.SHA_1_PSS, FileType.AuthentificationCertificate,
					false);
		} finally {
			beIDCard.close();
		}

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		boolean result = beIDIntegrity.verifySignature("SHA1withRSAandMGF1", signatureValue,
				authnCertificate.getPublicKey(), toBeSigned);

		assertTrue(result);
	}

	@Test
	public void testPSSSignatureSHA256() throws Exception {
		BeIDCard beIDCard = getBeIDCard();

		byte[] toBeSigned = new byte[10];
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(toBeSigned);

		X509Certificate authnCertificate = beIDCard.getAuthenticationCertificate();

		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
		byte[] digestValue = messageDigest.digest(toBeSigned);

		byte[] signatureValue;
		try {
			signatureValue = beIDCard.sign(digestValue, BeIDDigest.SHA_256_PSS, FileType.AuthentificationCertificate,
					false);
		} finally {
			beIDCard.close();
		}

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		boolean result = beIDIntegrity.verifySignature("SHA256withRSAandMGF1", signatureValue,
				authnCertificate.getPublicKey(), toBeSigned);

		assertTrue(result);
	}

	@Test
	public void testChangePIN() throws Exception {
		BeIDCard beIDCard = getBeIDCard();

		try {
			beIDCard.changePin(false);
		} finally {
			beIDCard.close();
		}
	}

	@Test
	public void testUnblockPIN() throws Exception {
		BeIDCard beIDCard = getBeIDCard();
		try {
			beIDCard.unblockPin(false);
		} finally {
			beIDCard.close();
		}
	}

	@Test
	public void testNonRepSignature() throws Exception {
		byte[] toBeSigned = new byte[10];
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(toBeSigned);
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
		byte[] digestValue = messageDigest.digest(toBeSigned);

		BeIDCard beIDCard = getBeIDCard();

		BeIDDigest beidDigest;
		if (beIDCard.isEC()) {
			beidDigest = BeIDDigest.ECDSA_SHA_2_256;
		} else {
			beidDigest = BeIDDigest.SHA_256;
		}

		X509Certificate signingCertificate;
		byte[] signatureValue;
		try {
			signatureValue = beIDCard.sign(digestValue, beidDigest, FileType.NonRepudiationCertificate, false);
			assertNotNull(signatureValue);
			signingCertificate = beIDCard.getSigningCertificate();
		} finally {
			beIDCard.close();
		}

		LOGGER.debug("signature size: {} bytes", signatureValue.length);
		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		boolean result = beIDIntegrity.verifyNonRepSignature(digestValue, signatureValue, signingCertificate);
		assertTrue(result);
	}

	@Test
	public void testECDSA_AuthenticationSignature() throws Exception {
		byte[] toBeSigned = new byte[10];
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(toBeSigned);
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
		byte[] digestValue = messageDigest.digest(toBeSigned);

		BeIDCard beIDCard = getBeIDCard();

		X509Certificate certificate;
		byte[] signatureValue;
		try {
			signatureValue = beIDCard.sign(digestValue, BeIDDigest.ECDSA_SHA_2_256,
					FileType.AuthentificationCertificate, false);
			assertNotNull(signatureValue);
			certificate = beIDCard.getAuthenticationCertificate();
		} finally {
			beIDCard.close();
		}

		LOGGER.debug("signature size: {} bytes", signatureValue.length);
		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		boolean result = beIDIntegrity.verifyAuthnSignature(toBeSigned, signatureValue, certificate);
		assertTrue(result);
	}

	protected BeIDCard getBeIDCard() {
		this.beIDCards = new BeIDCards(new TestLogger());
		BeIDCard beIDCard = null;
		try {
			beIDCard = this.beIDCards.getOneBeIDCard();
			assertNotNull(beIDCard);

			beIDCard.addCardListener(new BeIDCardListener() {
				@Override
				public void notifyReadProgress(final FileType fileType, final int offset, final int estimatedMaxSize) {
					LOGGER.debug("read progress of {}: {} of {}", fileType.name(), offset, estimatedMaxSize);
				}

				@Override
				public void notifySigningBegin(final FileType keyType) {
					LOGGER.debug("signing with {} key has begun",
							(keyType == FileType.AuthentificationCertificate ? "authentication" : "non-repudiation"));
				}

				@Override
				public void notifySigningEnd(final FileType keyType) {
					LOGGER.debug("signing with {} key has ended",
							(keyType == FileType.AuthentificationCertificate ? "authentication" : "non-repudiation"));
				}
			});
		} catch (BeIDCardsException bcex) {
			System.err.println(bcex.getMessage());
		}

		return beIDCard;
	}
}
