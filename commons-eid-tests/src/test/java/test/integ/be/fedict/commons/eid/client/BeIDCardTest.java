/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2014-2017 e-Contract.be BVBA.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
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

	@Test
	public void testReadFiles() throws Exception {
		final BeIDCard beIDCard = getBeIDCard();
		beIDCard.addCardListener(new TestBeIDCardListener());

		LOGGER.debug("reading identity file");
		final byte[] identityFile = beIDCard.readFile(FileType.Identity);
		LOGGER.debug("reading identity signature file");
		final byte[] identitySignatureFile = beIDCard.readFile(FileType.IdentitySignature);
		LOGGER.debug("reading RRN certificate file");
		final byte[] rrnCertificateFile = beIDCard.readFile(FileType.RRNCertificate);
		LOGGER.debug("reading auth certificate file");
		beIDCard.readFile(FileType.AuthentificationCertificate);
		LOGGER.debug("reading sign certificate file");
		beIDCard.readFile(FileType.NonRepudiationCertificate);
		LOGGER.debug("reading root certificate file");
		beIDCard.readFile(FileType.RootCertificate);
		LOGGER.debug("reading CA certificate file");
		beIDCard.readFile(FileType.CACertificate);
		LOGGER.debug("reading Photo file");
		final byte[] photoFile = beIDCard.readFile(FileType.Photo);
                File photoFileFile = File.createTempFile("eid-photo-", ".jpg");
                FileUtils.writeByteArrayToFile(photoFileFile, photoFile);

		final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		final X509Certificate rrnCertificate = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(rrnCertificateFile));

		beIDCard.close();

		final BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		final Identity identity = beIDIntegrity.getVerifiedIdentity(identityFile, identitySignatureFile, photoFile,
				rrnCertificate);

		assertNotNull(identity);
		assertNotNull(identity.getNationalNumber());
                LOGGER.debug("eID photo file: {}", photoFileFile.getAbsoluteFile());
	}

	@Test
	public void testAddressFileValidation() throws Exception {
		final BeIDCard beIDCard = getBeIDCard();
		beIDCard.addCardListener(new TestBeIDCardListener());

		LOGGER.debug("reading address file");
		final byte[] addressFile = beIDCard.readFile(FileType.Address);
		LOGGER.debug("reading address signature file");
		final byte[] addressSignatureFile = beIDCard.readFile(FileType.AddressSignature);
		LOGGER.debug("reading identity signature file");
		final byte[] identitySignatureFile = beIDCard.readFile(FileType.IdentitySignature);
		LOGGER.debug("reading RRN certificate file");
		final byte[] rrnCertificateFile = beIDCard.readFile(FileType.RRNCertificate);

		final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		final X509Certificate rrnCertificate = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(rrnCertificateFile));

		beIDCard.close();

		final BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		final Address address = beIDIntegrity.getVerifiedAddress(addressFile, identitySignatureFile,
				addressSignatureFile, rrnCertificate);

		assertNotNull(address);
		assertNotNull(address.getMunicipality());
	}

	@Test
	public void testSaveAddressFile() throws Exception {
		final BeIDCard beIDCard = getBeIDCard();
		beIDCard.addCardListener(new TestBeIDCardListener());

		LOGGER.debug("reading address file");
		final byte[] addressFile = beIDCard.readFile(FileType.Address);

		File tmpFile = File.createTempFile("address-", ".tlv");
		FileUtils.writeByteArrayToFile(tmpFile, addressFile);
		LOGGER.debug("tmp address file: {}", tmpFile.getAbsolutePath());
	}

	@Test
	public void testAuthnSignature() throws Exception {
		final BeIDCard beIDCard = getBeIDCard();

		final byte[] toBeSigned = new byte[10];
		final SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(toBeSigned);

		final X509Certificate authnCertificate = beIDCard.getAuthenticationCertificate();

		byte[] signatureValue;
		try {
			signatureValue = beIDCard.signAuthn(toBeSigned, false);
		} finally {
			beIDCard.close();
		}

		final BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		final boolean result = beIDIntegrity.verifyAuthnSignature(toBeSigned, signatureValue, authnCertificate);

		assertTrue(result);
	}

	@Test
	public void testRRNCertificate() throws Exception {
		final BeIDCard beIDCard = getBeIDCard();

		final X509Certificate rrnCertificate = beIDCard.getRRNCertificate();

		assertNotNull(rrnCertificate);
		LOGGER.debug("RRN certificate: {}", rrnCertificate);
	}
        
        @Test
	public void testAuthCertificate() throws Exception {
		final BeIDCard beIDCard = getBeIDCard();

		final X509Certificate authnCertificate = beIDCard.getAuthenticationCertificate();

		assertNotNull(authnCertificate);
		LOGGER.debug("authentication certificate: {}", authnCertificate);
	}

	@Test
	public void testGetCardData() throws Exception {
		final BeIDCard beIDCard = getBeIDCard();

		final byte[] cardDataFile = beIDCard.getCardData();

		assertNotNull(cardDataFile);
		LOGGER.debug("card data file size: {}", cardDataFile.length);
		LOGGER.debug("card data file: {}", Hex.toHexString(cardDataFile));
		CardData cardData = ByteArrayParser.parse(cardDataFile, CardData.class);
		LOGGER.debug("PKCS#1 1.5 supported: {}", cardData.isRSASSAPKCS115Supported());
		LOGGER.debug("PSS supported: {}", cardData.isRSASSAPSSSupported());
		LOGGER.debug("PKCS#1 support: {}", Integer.toHexString(cardData.getPkcs1Support()));
	}

	@Test
	public void testPSSSignature() throws Exception {
		final BeIDCard beIDCard = getBeIDCard();

		final byte[] toBeSigned = new byte[10];
		final SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(toBeSigned);

		final X509Certificate authnCertificate = beIDCard.getAuthenticationCertificate();

		final MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
		final byte[] digestValue = messageDigest.digest(toBeSigned);

		byte[] signatureValue;
		try {
			signatureValue = beIDCard.sign(digestValue, BeIDDigest.SHA_1_PSS, FileType.AuthentificationCertificate,
					false);
		} finally {
			beIDCard.close();
		}

		Security.addProvider(new BouncyCastleProvider());

		final BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		final boolean result = beIDIntegrity.verifySignature("SHA1withRSAandMGF1", signatureValue,
				authnCertificate.getPublicKey(), toBeSigned);

		assertTrue(result);
	}

	@Test
	public void testPSSSignatureSHA256() throws Exception {
		final BeIDCard beIDCard = getBeIDCard();

		final byte[] toBeSigned = new byte[10];
		final SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(toBeSigned);

		final X509Certificate authnCertificate = beIDCard.getAuthenticationCertificate();

		final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
		final byte[] digestValue = messageDigest.digest(toBeSigned);

		byte[] signatureValue;
		try {
			signatureValue = beIDCard.sign(digestValue, BeIDDigest.SHA_256_PSS, FileType.AuthentificationCertificate,
					false);
		} finally {
			beIDCard.close();
		}

		Security.addProvider(new BouncyCastleProvider());

		final BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		final boolean result = beIDIntegrity.verifySignature("SHA256withRSAandMGF1", signatureValue,
				authnCertificate.getPublicKey(), toBeSigned);

		assertTrue(result);
	}

	@Test
	public void testChangePIN() throws Exception {
		final BeIDCard beIDCard = getBeIDCard();

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
		final byte[] toBeSigned = new byte[10];
		final SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(toBeSigned);
		final MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
		final byte[] digestValue = messageDigest.digest(toBeSigned);

		final BeIDCard beIDCard = getBeIDCard();

		X509Certificate signingCertificate;
		byte[] signatureValue;
		try {
			signatureValue = beIDCard.sign(digestValue, BeIDDigest.SHA_1, FileType.NonRepudiationCertificate, false);
			assertNotNull(signatureValue);
			signingCertificate = beIDCard.getSigningCertificate();
		} finally {
			beIDCard.close();
		}

		final BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		final boolean result = beIDIntegrity.verifyNonRepSignature(digestValue, signatureValue, signingCertificate);
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
		} catch (final BeIDCardsException bcex) {
			System.err.println(bcex.getMessage());
		}

		return beIDCard;
	}
}
