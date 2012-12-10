/*
 * Commons eID Project.
 * Copyright (C) 2008-2012 FedICT.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Enumeration;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;

import sun.security.pkcs11.SunPKCS11;
import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.consumer.jca.ProxyPrivateKey;
import be.fedict.commons.eid.consumer.jca.ProxyProvider;
import be.fedict.commons.eid.jca.BeIDKeyStoreParameter;
import be.fedict.commons.eid.jca.BeIDPrivateKey;
import be.fedict.commons.eid.jca.BeIDProvider;

public class JCATest {

	private static final Log LOG = LogFactory.getLog(JCATest.class);

	@Test
	public void testProxySignature() throws Exception {
		Security.addProvider(new ProxyProvider());

		final KeyPairGenerator keyPairGenerator = KeyPairGenerator
				.getInstance("RSA");
		final KeyPair keyPair = keyPairGenerator.generateKeyPair();

		final KeyStore keyStore = KeyStore.getInstance("ProxyBeID");
		keyStore.load(null);
		final Enumeration<String> aliases = keyStore.aliases();
		while (aliases.hasMoreElements()) {
			final String alias = aliases.nextElement();
			LOG.debug("alias: " + alias);
			assertTrue(keyStore.isKeyEntry(alias));
			assertFalse(keyStore.isCertificateEntry(alias));
		}

		assertTrue(keyStore.containsAlias("Signature"));
		assertNotNull(keyStore.getCreationDate("Signature"));

		final Key key = keyStore.getKey("Signature", null);
		assertNotNull(key);
		LOG.debug("key type: " + key.getClass().getName());
		final ProxyPrivateKey privateKey = (ProxyPrivateKey) key;

		{
			final Signature signature = Signature.getInstance("SHA1withRSA");
			signature.initSign(privateKey);
			assertTrue(signature.getProvider() instanceof ProxyProvider);
		}
		{
			final Signature signature = Signature.getInstance("SHA1withRSA");
			signature.initSign(keyPair.getPrivate());
			LOG.debug("signature provider: "
					+ signature.getProvider().getName());
			assertEquals("SunRsaSign", signature.getProvider().getName());
		}

		FutureTask<String> signTask = new FutureTask<String>(
				new Callable<String>() {
					@Override
					public String call() throws Exception {
						final Signature signature = Signature
								.getInstance("SHA256withRSA");
						signature.initSign(privateKey);

						final byte[] toBeSigned = "hello world".getBytes();
						signature.update(toBeSigned);
						final byte[] signatureValue = signature.sign();
						LOG.debug("received signature value");
						assertNotNull(signatureValue);
						return "signature result";
					}

				});
		final ExecutorService executor = Executors.newFixedThreadPool(1);
		executor.execute(signTask);

		Thread.sleep(1000);
		final ProxyPrivateKey.DigestInfo digestInfo = privateKey
				.getDigestInfo();
		LOG.debug("received digest value");

		assertNotNull(digestInfo);
		final byte[] signatureValue = new byte[20];
		final SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(signatureValue);
		privateKey.setSignatureValue(signatureValue);
		final String signResult = signTask.get();
		assertNotNull(signResult);
	}

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

		final PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey(
				"Authentication", null);
		final Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initSign(authnPrivateKey);

		final byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		signature.sign();
	}

	private static class MyFrame extends JFrame
			implements
				KeyStore.LoadStoreParameter {

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

		final PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey(
				"Authentication", null);
		final Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initSign(authnPrivateKey);

		final byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		byte[] signatureValue = signature.sign();

		Certificate[] certificateChain = keyStore
				.getCertificateChain("Authentication");
		signature.initVerify(certificateChain[0]);
		signature.update(toBeSigned);
		assertTrue(signature.verify(signatureValue));
	}

	@Test
	public void testRecoveryAfterRemoval() throws Exception {
		Security.addProvider(new BeIDProvider());

		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);

		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey(
				"Authentication", null);
		final Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initSign(authnPrivateKey);

		final byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		byte[] signatureValue = signature.sign();

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
	 * When inserting another eID card however, the automatic recovery should
	 * fail.
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

		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey(
				"Authentication", null);
		PublicKey authnPublicKey = keyStore.getCertificate("Authentication")
				.getPublicKey();
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
		X509Certificate citizenCACertificate = (X509Certificate) keyStore
				.getCertificate("CA");
		X509Certificate rootCACertificate = (X509Certificate) keyStore
				.getCertificate("Root");

		// verify
		assertNotNull(citizenCACertificate);
		LOG.debug("citizen CA: "
				+ citizenCACertificate.getSubjectX500Principal());
		assertNotNull(rootCACertificate);
		LOG.debug("root CA: " + rootCACertificate.getSubjectX500Principal());
	}

	@Test
	public void testAuthenticationSignatures() throws Exception {
		Security.addProvider(new BeIDProvider());
		Security.addProvider(new BouncyCastleProvider());
		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);
		X509Certificate authnCertificate = (X509Certificate) keyStore
				.getCertificate("Authentication");
		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey(
				"Authentication", null);

		verifySignatureAlgorithm("SHA1withRSA", authnPrivateKey,
				authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA224withRSA", authnPrivateKey,
				authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA256withRSA", authnPrivateKey,
				authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA384withRSA", authnPrivateKey,
				authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA512withRSA", authnPrivateKey,
				authnCertificate.getPublicKey());
		verifySignatureAlgorithm("RIPEMD128withRSA", authnPrivateKey,
				authnCertificate.getPublicKey());
		verifySignatureAlgorithm("RIPEMD160withRSA", authnPrivateKey,
				authnCertificate.getPublicKey());
		verifySignatureAlgorithm("RIPEMD256withRSA", authnPrivateKey,
				authnCertificate.getPublicKey());
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
			LOG.debug("alias: " + alias);
		}

		assertEquals(2, keyStore.size());

		assertTrue(keyStore.containsAlias("Signature"));
		assertTrue(keyStore.containsAlias("Authentication"));
		assertNotNull(keyStore.getCreationDate("Signature"));
		assertNotNull(keyStore.getCreationDate("Authentication"));

		assertTrue(keyStore.isCertificateEntry("Signature"));
		final X509Certificate signCertificate = (X509Certificate) keyStore
				.getCertificate("Signature");
		assertNotNull(signCertificate);

		assertTrue(keyStore.isCertificateEntry("Authentication"));
		final X509Certificate authnCertificate = (X509Certificate) keyStore
				.getCertificate("Authentication");
		assertNotNull(authnCertificate);

		assertNotNull(keyStore.getCertificateChain("Signature"));
		assertNotNull(keyStore.getCertificateChain("Authentication"));

		assertTrue(keyStore.isKeyEntry("Authentication"));
		final PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey(
				"Authentication", null);
		assertNotNull(authnPrivateKey);

		assertTrue(keyStore.isKeyEntry("Signature"));
		final PrivateKey signPrivateKey = (PrivateKey) keyStore.getKey(
				"Signature", null);
		assertNotNull(signPrivateKey);

		verifySignatureAlgorithm("SHA1withRSA", authnPrivateKey,
				authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA256withRSA", signPrivateKey,
				signCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA384withRSA", authnPrivateKey,
				authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA512withRSA", authnPrivateKey,
				authnCertificate.getPublicKey());

		Security.addProvider(new BouncyCastleProvider());

		verifySignatureAlgorithm("SHA1withRSAandMGF1", authnPrivateKey,
				authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA256withRSAandMGF1", authnPrivateKey,
				authnCertificate.getPublicKey());
	}

	@Test
	public void testPSSPrefix() throws Exception {
		Security.addProvider(new BeIDProvider());
		Security.addProvider(new BouncyCastleProvider());
		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);
		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey(
				"Authentication", null);
		X509Certificate authnCertificate = (X509Certificate) keyStore
				.getCertificate("Authentication");
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
		BigInteger messageBigInteger = signatureValueBigInteger.modPow(
				rsaPublicKey.getPublicExponent(), rsaPublicKey.getModulus());
		String paddedMessage = new String(Hex.encodeHex(messageBigInteger
				.toByteArray()));
		LOG.debug("padded message: " + paddedMessage);
		assertTrue(paddedMessage.endsWith("bc"));
	}

	@Test
	public void testSoftwareRSAKeyWrapping() throws Exception {
		final KeyPairGenerator keyPairGenerator = KeyPairGenerator
				.getInstance("RSA");
		final KeyPair keyPair = keyPairGenerator.generateKeyPair();

		final KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
		final SecretKey secretKey = keyGenerator.generateKey();
		LOG.debug("secret key algo: " + secretKey.getAlgorithm());

		final Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.WRAP_MODE, keyPair.getPublic());
		LOG
				.debug("cipher security provider: "
						+ cipher.getProvider().getName());
		LOG.debug("cipher type: " + cipher.getClass().getName());
		final byte[] wrappedKey = cipher.wrap(secretKey);

		cipher.init(Cipher.UNWRAP_MODE, keyPair.getPrivate());
		final Key resultKey = cipher.unwrap(wrappedKey, "AES",
				Cipher.SECRET_KEY);

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
			LOG.debug("alias: " + alias);
		}

		final X509Certificate authnCertificate = (X509Certificate) keyStore
				.getCertificate("Authentication");
		assertNotNull(authnCertificate);
	}

	@Test
	public void testGetEntry() throws Exception {
		Security.addProvider(new BeIDProvider());

		final KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);
		PrivateKeyEntry privateKeyEntry = (PrivateKeyEntry) keyStore.getEntry(
				"Authentication", null);
		assertNotNull(privateKeyEntry);
		assertTrue(privateKeyEntry.getPrivateKey() instanceof BeIDPrivateKey);

		TrustedCertificateEntry caEntry = (TrustedCertificateEntry) keyStore
				.getEntry("CA", null);
		assertNotNull(caEntry);
		LOG.debug("CA entry: "
				+ ((X509Certificate) caEntry.getTrustedCertificate())
						.getSubjectX500Principal());

		TrustedCertificateEntry rootEntry = (TrustedCertificateEntry) keyStore
				.getEntry("Root", null);
		assertNotNull(rootEntry);
		LOG.debug("root entry: "
				+ ((X509Certificate) rootEntry.getTrustedCertificate())
						.getSubjectX500Principal());
	}

	@Test
	public void testPKCS1ViaPKCS11VersusCommonsEID() throws Exception {
		byte[] toBeSigned = "hello world".getBytes();

		{
			Security.addProvider(new BeIDProvider());
			KeyStore keyStore = KeyStore.getInstance("BeID");
			keyStore.load(null);
			PrivateKeyEntry privateKeyEntry = (PrivateKeyEntry) keyStore
					.getEntry("Authentication", null);
			PrivateKey privateKey = privateKeyEntry.getPrivateKey();
			RSAPublicKey publicKey = (RSAPublicKey) keyStore.getCertificate(
					"Authentication").getPublicKey();
			Signature signature = Signature.getInstance("SHA1withRSA");
			signature.initSign(privateKey);
			signature.update(toBeSigned);
			byte[] signatureValue = signature.sign();
			BigInteger signatureValueBigInteger = new BigInteger(signatureValue);
			BigInteger messageBigInteger = signatureValueBigInteger.modPow(
					publicKey.getPublicExponent(), publicKey.getModulus());
			LOG
					.debug("original message via Commons eID: "
							+ new String(Hex.encodeHex(messageBigInteger
									.toByteArray())));
		}

		{
			File tmpConfigFile = File.createTempFile("pkcs11-", "conf");
			tmpConfigFile.deleteOnExit();
			PrintWriter configWriter = new PrintWriter(new FileOutputStream(
					tmpConfigFile), true);
			configWriter.println("name=SmartCard");
			configWriter.println("library=/usr/lib/libbeidpkcs11.so.0");
			configWriter.println("slotListIndex=1");

			SunPKCS11 provider = new SunPKCS11(tmpConfigFile.getAbsolutePath());
			Security.addProvider(provider);
			KeyStore keyStore = KeyStore.getInstance("PKCS11", provider);
			keyStore.load(null, null);
			Enumeration<String> aliases = keyStore.aliases();
			while (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				LOG.debug("PKCS#11 alias: " + alias);
				LOG.debug("is certificate entry: "
						+ keyStore.isCertificateEntry(alias));
				LOG.debug("is key entry: " + keyStore.isKeyEntry(alias));
			}
			PrivateKeyEntry privateKeyEntry = (PrivateKeyEntry) keyStore
					.getEntry("Authentication", null);
			PrivateKey privateKey = privateKeyEntry.getPrivateKey();
			Signature signature = Signature.getInstance("SHA1withRSA");
			signature.initSign(privateKey);
			signature.update(toBeSigned);
			byte[] signatureValue = signature.sign();

			X509Certificate certificate = (X509Certificate) privateKeyEntry
					.getCertificate();
			RSAPublicKey publicKey = (RSAPublicKey) certificate.getPublicKey();
			BigInteger signatureValueBigInteger = new BigInteger(signatureValue);
			BigInteger messageBigInteger = signatureValueBigInteger.modPow(
					publicKey.getPublicExponent(), publicKey.getModulus());
			LOG
					.debug("original message via PKCS11:      "
							+ new String(Hex.encodeHex(messageBigInteger
									.toByteArray())));
		}
	}

	private void verifySignatureAlgorithm(final String signatureAlgorithm,
			final PrivateKey privateKey, final PublicKey publicKey)
			throws Exception {
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

		RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
		BigInteger signatureValueBigInteger = new BigInteger(signatureValue);
		BigInteger messageBigInteger = signatureValueBigInteger.modPow(
				rsaPublicKey.getPublicExponent(), rsaPublicKey.getModulus());
		LOG.debug("Padded DigestInfo: "
				+ new String(Hex.encodeHex(messageBigInteger.toByteArray())));
	}

	private BeIDCard getBeIDCard() throws Exception {
		final TestLogger logger = new TestLogger();
		final BeIDCards beIDCards = new BeIDCards(logger);
		final BeIDCard beIDCard = beIDCards.getOneBeIDCard();
		assertNotNull(beIDCard);
		return beIDCard;
	}
}
