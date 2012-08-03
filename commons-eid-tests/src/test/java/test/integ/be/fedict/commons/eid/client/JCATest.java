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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.util.Enumeration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import be.fedict.commons.eid.consumer.jca.ProxyPrivateKey;
import be.fedict.commons.eid.consumer.jca.ProxyProvider;

public class JCATest {

	private static final Log LOG = LogFactory.getLog(JCATest.class);

	@Test
	public void testKeyStore() throws Exception {
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
}
