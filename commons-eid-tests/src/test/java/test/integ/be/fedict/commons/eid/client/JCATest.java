/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2014-2020 e-Contract.be BV.
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Enumeration;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.client.spi.UserCancelledException;
import be.fedict.commons.eid.jca.BeIDKeyStoreParameter;
import be.fedict.commons.eid.jca.BeIDPrivateKey;
import be.fedict.commons.eid.jca.BeIDProvider;
import be.fedict.commons.eid.jca.UserCancelledSignatureException;

public class JCATest {

	private static final Logger LOGGER = LoggerFactory.getLogger(JCATest.class);

	@Test
	public void testSwingParentLocale() throws Exception {
		Security.addProvider(new BeIDProvider());

		final JFrame frame = new JFrame("Test Parent frame");
		frame.setSize(200, 200);
		frame.setLocation(300, 300);
		frame.setVisible(true);

		final KeyStore keyStore = KeyStore.getInstance("BeID");
		final BeIDKeyStoreParameter keyStoreParameter = new BeIDKeyStoreParameter();
		keyStoreParameter.setLogoff(true);
		keyStoreParameter.setParentComponent(frame);
		keyStoreParameter.setLocale(new Locale("nl"));
		keyStore.load(keyStoreParameter);

		final PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		final Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initSign(authnPrivateKey);

		final byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		signature.sign();
	}

	private static class MyFrame extends JFrame implements KeyStore.LoadStoreParameter {

		private static final long serialVersionUID = 1L;

		public MyFrame() {
			super("Test frame 2");
			setSize(200, 200);
			setLocation(300, 300);
			setVisible(true);
		}

		@Override
		public ProtectionParameter getProtectionParameter() {
			return null;
		}
	}

	@Test
	public void testSwingParent2() throws Exception {
		Security.addProvider(new BeIDProvider());

		MyFrame myFrame = new MyFrame();

		final KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(myFrame);

		final PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		final Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initSign(authnPrivateKey);

		final byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		byte[] signatureValue = signature.sign();

		Certificate[] certificateChain = keyStore.getCertificateChain("Authentication");
		signature.initVerify(certificateChain[0]);
		signature.update(toBeSigned);
		assertTrue(signature.verify(signatureValue));
	}

	@Test
	public void testRecoveryAfterRemoval() throws Exception {
		Security.addProvider(new BeIDProvider());

		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);

		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		final Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initSign(authnPrivateKey);

		final byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		signature.sign();

		JOptionPane.showMessageDialog(null, "Please remove/insert eID card...");

		keyStore.load(null); // reload the keystore.
		authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		signature.initSign(authnPrivateKey);
		signature.update(toBeSigned);
		signature.sign();
	}

	/**
	 * Integration test for automatic recovery of a {@link PrivateKey} instance.
	 * <p/>
	 * Automatic recovery should work on the same eID card.
	 * <p/>
	 * When inserting another eID card however, the automatic recovery should fail.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAutoRecovery() throws Exception {
		Security.addProvider(new BeIDProvider());

		KeyStore keyStore = KeyStore.getInstance("BeID");
		BeIDKeyStoreParameter keyStoreParameter = new BeIDKeyStoreParameter();
		keyStoreParameter.setAutoRecovery(true);
		keyStoreParameter.setCardReaderStickiness(true);
		keyStore.load(keyStoreParameter);

		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		PublicKey authnPublicKey = keyStore.getCertificate("Authentication").getPublicKey();
		final Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initSign(authnPrivateKey);

		final byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		byte[] signatureValue = signature.sign();

		signature.initVerify(authnPublicKey);
		signature.update(toBeSigned);
		assertTrue(signature.verify(signatureValue));

		JOptionPane.showMessageDialog(null, "Please remove/insert eID card...");

		signature.initSign(authnPrivateKey);
		signature.update(toBeSigned);
		signatureValue = signature.sign();

		signature.initVerify(authnPublicKey);
		signature.update(toBeSigned);
		assertTrue(signature.verify(signatureValue));
	}

	@Test
	public void testGetCertificateCaching() throws Exception {
		Security.addProvider(new BeIDProvider());

		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);

		for (int idx = 0; idx < 100; idx++) {
			assertNotNull(keyStore.getCertificate("Authentication"));
		}
	}

	@Test
	public void testCAAliases() throws Exception {
		// setup
		Security.addProvider(new BeIDProvider());
		final KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);

		// operate
		X509Certificate citizenCACertificate = (X509Certificate) keyStore.getCertificate("CA");
		X509Certificate rootCACertificate = (X509Certificate) keyStore.getCertificate("Root");
		X509Certificate rrnCertificate = (X509Certificate) keyStore.getCertificate("RRN");

		// verify
		assertNotNull(citizenCACertificate);
		LOGGER.debug("citizen CA: {}", citizenCACertificate.getSubjectX500Principal());
		assertNotNull(rootCACertificate);
		LOGGER.debug("root CA: {}", rootCACertificate.getSubjectX500Principal());
		assertNotNull(rrnCertificate);
		assertTrue(rrnCertificate.getSubjectX500Principal().toString().contains("RRN"));
	}

	@Test
	public void testRRNCertificate() throws Exception {
		// setup
		Security.addProvider(new BeIDProvider());
		final KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);

		// operate
		assertTrue(keyStore.containsAlias("RRN"));
		Entry entry = keyStore.getEntry("RRN", null);
		assertNotNull(entry);
		assertTrue(entry instanceof TrustedCertificateEntry);
		TrustedCertificateEntry trustedCertificateEntry = (TrustedCertificateEntry) entry;
		assertNotNull(trustedCertificateEntry.getTrustedCertificate());
		assertTrue(((X509Certificate) trustedCertificateEntry.getTrustedCertificate()).getSubjectX500Principal()
				.toString().contains("RRN"));
		assertNotNull(keyStore.getCertificate("RRN"));
		Certificate[] certificateChain = keyStore.getCertificateChain("RRN");
		assertNotNull(certificateChain);
		assertEquals(2, certificateChain.length);
		LOGGER.debug("RRN subject: {}", ((X509Certificate) certificateChain[0]).getSubjectX500Principal());
		LOGGER.debug("RRN issuer: {}", ((X509Certificate) certificateChain[0]).getIssuerX500Principal());
		LOGGER.debug("root subject: {}", ((X509Certificate) certificateChain[1]).getSubjectX500Principal());
		LOGGER.debug("root issuer: {}", ((X509Certificate) certificateChain[1]).getIssuerX500Principal());
	}

	@Test
	public void testAuthenticationSignatures() throws Exception {
		Security.addProvider(new BeIDProvider());
		Security.addProvider(new BouncyCastleProvider());
		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);
		X509Certificate authnCertificate = (X509Certificate) keyStore.getCertificate("Authentication");
		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);

		verifySignatureAlgorithm("SHA1withRSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA224withRSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA256withRSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA384withRSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA512withRSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("RIPEMD128withRSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("RIPEMD160withRSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("RIPEMD256withRSA", authnPrivateKey, authnCertificate.getPublicKey());
	}

	@Test
	public void testNonRepudiationSignature() throws Exception {
		Security.addProvider(new BeIDProvider());
		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);
		PrivateKey signPrivateKey = (PrivateKey) keyStore.getKey("Signature", null);
		Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initSign(signPrivateKey);
		byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		byte[] signatureValue = signature.sign();
		assertNotNull(signatureValue);

		Certificate[] signCertificateChain = keyStore.getCertificateChain("Signature");
		assertNotNull(signCertificateChain);
	}

	@Test
	public void testNonRepudiationSignaturePPDU() throws Exception {
		Security.addProvider(new BeIDProvider());

		BeIDKeyStoreParameter beIDKeyStoreParameter = new BeIDKeyStoreParameter();
		beIDKeyStoreParameter.addPPDUName("digipass 870");
		beIDKeyStoreParameter.addPPDUName("digipass 875");
		beIDKeyStoreParameter.addPPDUName("digipass 920");
		beIDKeyStoreParameter.setLocale(new Locale("nl"));

		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(beIDKeyStoreParameter);
		PrivateKey signPrivateKey = (PrivateKey) keyStore.getKey("Signature", null);
		Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initSign(signPrivateKey);
		byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		byte[] signatureValue = signature.sign();
		assertNotNull(signatureValue);

		Certificate[] signCertificateChain = keyStore.getCertificateChain("Signature");
		assertNotNull(signCertificateChain);
	}

	@Test
	public void testLocale() throws Exception {
		Security.addProvider(new BeIDProvider());

		KeyStore keyStore = KeyStore.getInstance("BeID");
		BeIDKeyStoreParameter beIDKeyStoreParameter = new BeIDKeyStoreParameter();
		beIDKeyStoreParameter.setLocale(Locale.FRENCH);
		beIDKeyStoreParameter.setLogger(new TestLogger());
		keyStore.load(beIDKeyStoreParameter);

		PrivateKey privateKey = (PrivateKey) keyStore.getKey("Signature", null);

		Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initSign(privateKey);

		byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		signature.sign();
	}

	@Test
	public void testCancelOperation() throws Exception {
		Security.addProvider(new BeIDProvider());

		final KeyStore keyStore = KeyStore.getInstance("BeID");
		final BeIDKeyStoreParameter keyStoreParameter = new BeIDKeyStoreParameter();
		final BeIDCard beIDCard = getBeIDCard();
		keyStoreParameter.setBeIDCard(beIDCard);
		keyStoreParameter.setLogoff(true);
		keyStore.load(keyStoreParameter);

		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);

		Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initSign(authnPrivateKey);
		assertTrue(signature.getProvider() instanceof BeIDProvider);

		JOptionPane.showMessageDialog(null, "Please click Cancel on the next PIN dialog.");

		final byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		try {
			signature.sign();
			fail();
		} catch (UserCancelledSignatureException e) {
			// expected
			assertTrue(e.getCause() instanceof UserCancelledException);
		}
	}

	@Test
	public void testBeIDSignature() throws Exception {
		Security.addProvider(new BeIDProvider());

		final KeyStore keyStore = KeyStore.getInstance("BeID");
		final BeIDKeyStoreParameter keyStoreParameter = new BeIDKeyStoreParameter();
		final BeIDCard beIDCard = getBeIDCard();
		keyStoreParameter.setBeIDCard(beIDCard);
		keyStoreParameter.setLogoff(true);
		keyStore.load(keyStoreParameter);

		final Enumeration<String> aliases = keyStore.aliases();
		while (aliases.hasMoreElements()) {
			final String alias = aliases.nextElement();
			LOGGER.debug("alias: {}", alias);
		}

		assertEquals(2, keyStore.size());

		assertTrue(keyStore.containsAlias("Signature"));
		assertTrue(keyStore.containsAlias("Authentication"));
		assertNotNull(keyStore.getCreationDate("Signature"));
		assertNotNull(keyStore.getCreationDate("Authentication"));

		assertTrue(keyStore.isKeyEntry("Signature"));
		final X509Certificate signCertificate = (X509Certificate) keyStore.getCertificate("Signature");
		assertNotNull(signCertificate);

		assertTrue(keyStore.isKeyEntry("Authentication"));
		final X509Certificate authnCertificate = (X509Certificate) keyStore.getCertificate("Authentication");
		assertNotNull(authnCertificate);

		assertNotNull(keyStore.getCertificateChain("Signature"));
		assertNotNull(keyStore.getCertificateChain("Authentication"));

		assertTrue(keyStore.isKeyEntry("Authentication"));
		final PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		assertNotNull(authnPrivateKey);

		assertTrue(keyStore.isKeyEntry("Signature"));
		final PrivateKey signPrivateKey = (PrivateKey) keyStore.getKey("Signature", null);
		assertNotNull(signPrivateKey);

		verifySignatureAlgorithm("SHA1withRSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA256withRSA", signPrivateKey, signCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA384withRSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA512withRSA", authnPrivateKey, authnCertificate.getPublicKey());

		Security.addProvider(new BouncyCastleProvider());

		verifySignatureAlgorithm("SHA1withRSAandMGF1", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA256withRSAandMGF1", authnPrivateKey, authnCertificate.getPublicKey());
	}

	@Test
	public void testPSSPrefix() throws Exception {
		Security.addProvider(new BeIDProvider());
		Security.addProvider(new BouncyCastleProvider());
		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);
		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		X509Certificate authnCertificate = (X509Certificate) keyStore.getCertificate("Authentication");
		PublicKey authnPublicKey = authnCertificate.getPublicKey();

		Signature signature = Signature.getInstance("SHA1withRSAandMGF1");
		signature.initSign(authnPrivateKey);

		byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		byte[] signatureValue = signature.sign();

		signature.initVerify(authnPublicKey);
		signature.update(toBeSigned);
		boolean result = signature.verify(signatureValue);
		assertTrue(result);

		RSAPublicKey rsaPublicKey = (RSAPublicKey) authnPublicKey;
		BigInteger signatureValueBigInteger = new BigInteger(signatureValue);
		BigInteger messageBigInteger = signatureValueBigInteger.modPow(rsaPublicKey.getPublicExponent(),
				rsaPublicKey.getModulus());
		String paddedMessage = new String(Hex.encodeHex(messageBigInteger.toByteArray()));
		LOGGER.debug("padded message: {}", paddedMessage);
		assertTrue(paddedMessage.endsWith("bc"));
	}

	@Test
	public void testPSS256() throws Exception {
		Security.addProvider(new BeIDProvider());
		Security.addProvider(new BouncyCastleProvider());
		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);
		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		X509Certificate authnCertificate = (X509Certificate) keyStore.getCertificate("Authentication");
		PublicKey authnPublicKey = authnCertificate.getPublicKey();

		Signature signature = Signature.getInstance("SHA256withRSAandMGF1");
		signature.initSign(authnPrivateKey);

		byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		byte[] signatureValue = signature.sign();

		signature.initVerify(authnPublicKey);
		signature.update(toBeSigned);
		boolean result = signature.verify(signatureValue);
		assertTrue(result);
	}

	@Test
	public void testAuthenticationWithApplicationName() throws Exception {
		Security.addProvider(new BeIDProvider());
		Security.addProvider(new BouncyCastleProvider());
		BeIDKeyStoreParameter keyStoreParameter = new BeIDKeyStoreParameter();
		keyStoreParameter.setApplicationName("Commons eID Integration Test");
		keyStoreParameter.setLogoff(true);
		keyStoreParameter.setAllowFailingLogoff(true);
		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(keyStoreParameter);
		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		X509Certificate authnCertificate = (X509Certificate) keyStore.getCertificate("Authentication");
		PublicKey authnPublicKey = authnCertificate.getPublicKey();

		Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initSign(authnPrivateKey);

		byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		byte[] signatureValue = signature.sign();

		signature.initVerify(authnPublicKey);
		signature.update(toBeSigned);
		boolean result = signature.verify(signatureValue);
		assertTrue(result);
	}

	@Test
	public void testSoftwareRSAKeyWrapping() throws Exception {
		final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		final KeyPair keyPair = keyPairGenerator.generateKeyPair();

		final KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
		final SecretKey secretKey = keyGenerator.generateKey();
		LOGGER.debug("secret key algo: {}", secretKey.getAlgorithm());

		final Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.WRAP_MODE, keyPair.getPublic());
		LOGGER.debug("cipher security provider: {}", cipher.getProvider().getName());
		LOGGER.debug("cipher type: {}", cipher.getClass().getName());
		final byte[] wrappedKey = cipher.wrap(secretKey);

		cipher.init(Cipher.UNWRAP_MODE, keyPair.getPrivate());
		final Key resultKey = cipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);

		assertArrayEquals(secretKey.getEncoded(), resultKey.getEncoded());

	}

	@Test
	public void testAutoFindCard() throws Exception {
		Security.addProvider(new BeIDProvider());

		final KeyStore keyStore = KeyStore.getInstance("BeID");
		BeIDKeyStoreParameter beIDKeyStoreParameter = new BeIDKeyStoreParameter();
		beIDKeyStoreParameter.setLocale(new Locale("fr"));
		keyStore.load(beIDKeyStoreParameter);

		final Enumeration<String> aliases = keyStore.aliases();
		assertNotNull(aliases);
		while (aliases.hasMoreElements()) {
			final String alias = aliases.nextElement();
			LOGGER.debug("alias: {}", alias);
		}

		final X509Certificate authnCertificate = (X509Certificate) keyStore.getCertificate("Authentication");
		assertNotNull(authnCertificate);
	}

	@Test
	public void testGetEntry() throws Exception {
		Security.addProvider(new BeIDProvider());

		final KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);
		PrivateKeyEntry privateKeyEntry = (PrivateKeyEntry) keyStore.getEntry("Authentication", null);
		assertNotNull(privateKeyEntry);
		assertTrue(privateKeyEntry.getPrivateKey() instanceof BeIDPrivateKey);

		TrustedCertificateEntry caEntry = (TrustedCertificateEntry) keyStore.getEntry("CA", null);
		assertNotNull(caEntry);
		LOGGER.debug("CA entry: {}", ((X509Certificate) caEntry.getTrustedCertificate()).getSubjectX500Principal());

		TrustedCertificateEntry rootEntry = (TrustedCertificateEntry) keyStore.getEntry("Root", null);
		assertNotNull(rootEntry);
		LOGGER.debug("root entry: {}", ((X509Certificate) rootEntry.getTrustedCertificate()).getSubjectX500Principal());
	}

	@Test
	public void testECDSA() throws Exception {
		Security.addProvider(new BeIDProvider());
		Security.addProvider(new BouncyCastleProvider());
		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);
		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		X509Certificate authnCertificate = (X509Certificate) keyStore.getCertificate("Authentication");

		PrivateKey signPrivateKey = (PrivateKey) keyStore.getKey("Signature", null);
		X509Certificate signCertificate = (X509Certificate) keyStore.getCertificate("Signature");

		verifySignatureAlgorithm("SHA256withECDSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA384withECDSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA512withECDSA", authnPrivateKey, authnCertificate.getPublicKey());

		verifySignatureAlgorithm("SHA256withECDSA", signPrivateKey, signCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA384withECDSA", signPrivateKey, signCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA512withECDSA", signPrivateKey, signCertificate.getPublicKey());
	}

	private void verifySignatureAlgorithm(final String signatureAlgorithm, final PrivateKey privateKey,
			final PublicKey publicKey) throws Exception {
		Signature signature = Signature.getInstance(signatureAlgorithm);
		signature.initSign(privateKey);
		assertTrue(signature.getProvider() instanceof BeIDProvider);

		final byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		final byte[] signatureValue = signature.sign();
		assertNotNull(signatureValue);

		signature.initVerify(publicKey);
		signature.update(toBeSigned);
		final boolean beIDResult = signature.verify(signatureValue);
		assertTrue(beIDResult);

		signature = Signature.getInstance(signatureAlgorithm);
		signature.initVerify(publicKey);
		signature.update(toBeSigned);
		final boolean result = signature.verify(signatureValue);
		assertTrue(result);

		if (publicKey instanceof RSAPublicKey) {
			RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
			BigInteger signatureValueBigInteger = new BigInteger(signatureValue);
			BigInteger messageBigInteger = signatureValueBigInteger.modPow(rsaPublicKey.getPublicExponent(),
					rsaPublicKey.getModulus());
			LOGGER.debug("Padded DigestInfo: {}", new String(Hex.encodeHex(messageBigInteger.toByteArray())));
			assertEquals("RSA", privateKey.getAlgorithm());
		} else {
			assertEquals("EC", privateKey.getAlgorithm());
		}
	}

	private BeIDCard getBeIDCard() throws Exception {
		final TestLogger logger = new TestLogger();
		final BeIDCards beIDCards = new BeIDCards(logger);
		final BeIDCard beIDCard = beIDCards.getOneBeIDCard();
		assertNotNull(beIDCard);
		return beIDCard;
	}
}
