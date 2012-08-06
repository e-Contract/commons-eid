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

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCardManager;
import be.fedict.commons.eid.client.spi.UI;
import be.fedict.commons.eid.consumer.jca.ProxyPrivateKey;
import be.fedict.commons.eid.consumer.jca.ProxyProvider;
import be.fedict.commons.eid.jca.BeIDKeyStoreParameter;
import be.fedict.commons.eid.jca.BeIDProvider;
import be.fedict.eid.commons.dialogs.DefaultDialogs;

public class JCATest {

	private static final Log LOG = LogFactory.getLog(JCATest.class);

	@Test
	public void testProxySignature() throws Exception {
		Security.addProvider(new ProxyProvider());

		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		KeyPair keyPair = keyPairGenerator.generateKeyPair();

		KeyStore keyStore = KeyStore.getInstance("ProxyBeID");
		keyStore.load(null);
		Enumeration<String> aliases = keyStore.aliases();
		while (aliases.hasMoreElements()) {
			String alias = aliases.nextElement();
			LOG.debug("alias: " + alias);
			assertTrue(keyStore.isKeyEntry(alias));
			assertFalse(keyStore.isCertificateEntry(alias));
		}

		assertTrue(keyStore.containsAlias("Signature"));
		assertNotNull(keyStore.getCreationDate("Signature"));

		Key key = keyStore.getKey("Signature", null);
		assertNotNull(key);
		LOG.debug("key type: " + key.getClass().getName());
		final ProxyPrivateKey privateKey = (ProxyPrivateKey) key;

		{
			Signature signature = Signature.getInstance("SHA1withRSA");
			signature.initSign(privateKey);
			assertTrue(signature.getProvider() instanceof ProxyProvider);
		}
		{
			Signature signature = Signature.getInstance("SHA1withRSA");
			signature.initSign(keyPair.getPrivate());
			LOG.debug("signature provider: "
					+ signature.getProvider().getName());
			assertEquals("SunRsaSign", signature.getProvider().getName());
		}

		FutureTask<String> signTask = new FutureTask<String>(
				new Callable<String>() {
					@Override
					public String call() throws Exception {
						Signature signature = Signature
								.getInstance("SHA256withRSA");
						signature.initSign(privateKey);

						byte[] toBeSigned = "hello world".getBytes();
						signature.update(toBeSigned);
						byte[] signatureValue = signature.sign();
						LOG.debug("received signature value");
						assertNotNull(signatureValue);
						return "signature result";
					}

				});
		ExecutorService executor = Executors.newFixedThreadPool(1);
		executor.execute(signTask);

		Thread.sleep(1000);
		ProxyPrivateKey.DigestInfo digestInfo = privateKey.getDigestInfo();
		LOG.debug("received digest value");

		assertNotNull(digestInfo);
		byte[] signatureValue = new byte[20];
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(signatureValue);
		privateKey.setSignatureValue(signatureValue);
		String signResult = signTask.get();
		assertNotNull(signResult);
	}

	@Test
	public void testBeIDSignature() throws Exception {
		Security.addProvider(new BeIDProvider());

		KeyStore keyStore = KeyStore.getInstance("BeID");
		BeIDKeyStoreParameter keyStoreParameter = new BeIDKeyStoreParameter();
		BeIDCard beIDCard = getBeIDCard();
		keyStoreParameter.setBeIDCard(beIDCard);
		keyStoreParameter.setLogoff(true);
		keyStore.load(keyStoreParameter);

		Enumeration<String> aliases = keyStore.aliases();
		while (aliases.hasMoreElements()) {
			String alias = aliases.nextElement();
			LOG.debug("alias: " + alias);
		}

		assertEquals(2, keyStore.size());

		assertTrue(keyStore.containsAlias("Signature"));
		assertTrue(keyStore.containsAlias("Authentication"));
		assertNotNull(keyStore.getCreationDate("Signature"));
		assertNotNull(keyStore.getCreationDate("Authentication"));

		assertTrue(keyStore.isCertificateEntry("Signature"));
		X509Certificate signCertificate = (X509Certificate) keyStore
				.getCertificate("Signature");
		assertNotNull(signCertificate);

		assertTrue(keyStore.isCertificateEntry("Authentication"));
		X509Certificate authnCertificate = (X509Certificate) keyStore
				.getCertificate("Authentication");
		assertNotNull(authnCertificate);

		assertNotNull(keyStore.getCertificateChain("Signature"));
		assertNotNull(keyStore.getCertificateChain("Authentication"));

		assertTrue(keyStore.isKeyEntry("Authentication"));
		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey(
				"Authentication", null);
		assertNotNull(authnPrivateKey);

		assertTrue(keyStore.isKeyEntry("Signature"));
		PrivateKey signPrivateKey = (PrivateKey) keyStore.getKey("Signature",
				null);
		assertNotNull(signPrivateKey);

		verifySignatureAlgorithm("SHA1withRSA", authnPrivateKey,
				authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA256withRSA", signPrivateKey,
				signCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA384withRSA", authnPrivateKey,
				authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA512withRSA", authnPrivateKey,
				authnCertificate.getPublicKey());
	}

	@Test
	public void testSoftwareRSAKeyWrapping() throws Exception {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		KeyPair keyPair = keyPairGenerator.generateKeyPair();

		KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
		SecretKey secretKey = keyGenerator.generateKey();
		LOG.debug("secret key algo: " + secretKey.getAlgorithm());

		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.WRAP_MODE, keyPair.getPublic());
		LOG.debug("cipher security provider: " + cipher.getProvider().getName());
		LOG.debug("cipher type: " + cipher.getClass().getName());
		byte[] wrappedKey = cipher.wrap(secretKey);

		cipher.init(Cipher.UNWRAP_MODE, keyPair.getPrivate());
		Key resultKey = cipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);

		assertArrayEquals(secretKey.getEncoded(), resultKey.getEncoded());

	}

	@Test
	public void testAutoFindCard() throws Exception {
		Security.addProvider(new BeIDProvider());

		KeyStore keyStore = KeyStore.getInstance("BeID");
		// keyStore.load(null);
		keyStore.load(null, null);

		Enumeration<String> aliases = keyStore.aliases();
		assertNotNull(aliases);
		while (aliases.hasMoreElements()) {
			String alias = aliases.nextElement();
			LOG.debug("alias: " + alias);
		}

		X509Certificate authnCertificate = (X509Certificate) keyStore
				.getCertificate("Authentication");
		assertNotNull(authnCertificate);
	}

	private void verifySignatureAlgorithm(String signatureAlgorithm,
			PrivateKey privateKey, PublicKey publicKey) throws Exception {
		Signature signature = Signature.getInstance(signatureAlgorithm);
		signature.initSign(privateKey);
		assertTrue(signature.getProvider() instanceof BeIDProvider);

		byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		byte[] signatureValue = signature.sign();
		assertNotNull(signatureValue);

		signature.initVerify(publicKey);
		signature.update(toBeSigned);
		boolean beIDResult = signature.verify(signatureValue);
		assertTrue(beIDResult);

		signature = Signature.getInstance(signatureAlgorithm);
		signature.initVerify(publicKey);
		signature.update(toBeSigned);
		boolean result = signature.verify(signatureValue);
		assertTrue(result);
	}

	private BeIDCard getBeIDCard() throws Exception {
		TestLogger logger = new TestLogger();
		BeIDCardManager beIDCardManager = new BeIDCardManager(logger);
		BeIDCard beIDCard = beIDCardManager.getFirstBeIDCard();
		assertNotNull(beIDCard);

		UI userInterface = new DefaultDialogs();
		beIDCard.setUserInterface(userInterface);
		return beIDCard;
	}
}
