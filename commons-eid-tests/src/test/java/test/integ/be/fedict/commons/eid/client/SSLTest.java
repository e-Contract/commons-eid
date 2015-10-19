/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2014-2015 e-Contract.be BVBA.
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
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Locale;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.misc.MiscObjectIdentifiers;
import org.bouncycastle.asn1.misc.NetscapeCertType;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.joda.time.DateTime;
import org.junit.Test;

import be.fedict.commons.eid.jca.BeIDManagerFactoryParameters;
import be.fedict.commons.eid.jca.BeIDProvider;
import org.bouncycastle.asn1.x509.Extension;

public class SSLTest {

	private static final Log LOG = LogFactory.getLog(SSLTest.class);

	@Test
	public void testTestEIDBelgiumBe() throws Exception {
		Security.addProvider(new BeIDProvider());

		SSLContext sslContext = SSLContext.getInstance("TLS");
		KeyManagerFactory keyManagerFactory = KeyManagerFactory
				.getInstance("BeID");

		keyManagerFactory.init(null);
		SecureRandom secureRandom = new SecureRandom();
		sslContext.init(keyManagerFactory.getKeyManagers(),
				new TrustManager[]{new ClientTestX509TrustManager()},
				secureRandom);
		SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
		SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(
				"test.eid.belgium.be", 443);
		LOG.debug("socket created");
		SSLSession sslSession = sslSocket.getSession();
		Certificate[] peerCertificates = sslSession.getPeerCertificates();
		for (Certificate peerCertificate : peerCertificates) {
			LOG.debug("peer certificate: "
					+ ((X509Certificate) peerCertificate)
							.getSubjectX500Principal());
		}
	}

	@Test
	public void testMutualSSL() throws Exception {

		Security.addProvider(new BeIDProvider());

		final KeyPair serverKeyPair = generateKeyPair();
		final PrivateKey serverPrivateKey = serverKeyPair.getPrivate();
		final DateTime notBefore = new DateTime();
		final DateTime notAfter = notBefore.plusDays(1);
		final X509Certificate serverCertificate = generateCACertificate(
				serverKeyPair, "CN=Test", notBefore, notAfter);

		final KeyManager keyManager = new ServerTestX509KeyManager(
				serverPrivateKey, serverCertificate);
		final TrustManager trustManager = new ServerTestX509TrustManager();
		final SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(new KeyManager[]{keyManager},
				new TrustManager[]{trustManager}, new SecureRandom());

		final SSLServerSocketFactory sslServerSocketFactory = sslContext
				.getServerSocketFactory();

		final int serverPort = 8443;
		final SSLServerSocket sslServerSocket = (SSLServerSocket) sslServerSocketFactory
				.createServerSocket(serverPort);

		sslServerSocket.setNeedClientAuth(true);

		final TestRunnable testRunnable = new TestRunnable(serverPort);
		final Thread thread = new Thread(testRunnable);
		thread.start();

		SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
		LOG.debug("server accepted");
		InputStream inputStream = sslSocket.getInputStream();
		int result = inputStream.read();
		LOG.debug("result: " + result);
		assertEquals(12, result);
		SSLSession sslSession = sslSocket.getSession();
		sslSession.invalidate();
		sslSocket = (SSLSocket) sslServerSocket.accept();
		inputStream = sslSocket.getInputStream();
		result = inputStream.read();
		LOG.debug("result: " + result);
		assertEquals(34, result);
	}

	private static final class TestRunnable implements Runnable {

		private static final Log LOG = LogFactory.getLog(TestRunnable.class);

		private final int serverPort;

		public TestRunnable(final int serverPort) {
			this.serverPort = serverPort;
		}

		@Override
		public void run() {
			try {
				mutualSSLConnection();
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}

		private void mutualSSLConnection() throws Exception {
			Thread.sleep(1000);

			final JFrame frame = new JFrame("Mutual SSL test");
			frame.setSize(200, 200);
			frame.setLocation(300, 300);
			frame.setVisible(true);

			final SSLContext sslContext = SSLContext.getInstance("TLS");
			final KeyManagerFactory keyManagerFactory = KeyManagerFactory
					.getInstance("BeID");
			final BeIDManagerFactoryParameters spec = new BeIDManagerFactoryParameters();
			spec.setLocale(Locale.FRENCH);
			spec.setParentComponent(frame);
			spec.setAutoRecovery(true);
			spec.setCardReaderStickiness(true);

			keyManagerFactory.init(spec);
			// SecureRandom secureRandom = SecureRandom.getInstance("BeID");
			SecureRandom secureRandom = new SecureRandom();
			sslContext.init(keyManagerFactory.getKeyManagers(),
					new TrustManager[]{new ClientTestX509TrustManager()},
					secureRandom);
			final SSLSocketFactory sslSocketFactory = sslContext
					.getSocketFactory();
			SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(
					"localhost", this.serverPort);
			LOG.debug("socket created");
			OutputStream outputStream = sslSocket.getOutputStream();
			outputStream.write(12);
			SSLSession sslSession = sslSocket.getSession();
			sslSession.invalidate();
			JOptionPane.showMessageDialog(null, "Please remove eID card...");
			sslSocket.close();
			sslSocket = (SSLSocket) sslSocketFactory.createSocket("localhost",
					this.serverPort);
			outputStream = sslSocket.getOutputStream();
			outputStream.write(34);
		}
	}

	private static final class ClientTestX509TrustManager
			implements
				X509TrustManager {

		private static final Log LOG = LogFactory
				.getLog(ClientTestX509TrustManager.class);

		@Override
		public void checkClientTrusted(final X509Certificate[] chain,
				final String authType) throws CertificateException {
			LOG.debug("checkClientTrusted");
		}

		@Override
		public void checkServerTrusted(final X509Certificate[] chain,
				final String authType) throws CertificateException {
			LOG.debug("checkServerTrusted: " + authType);
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			LOG.debug("getAcceptedIssuers");
			return null;
		}

	}

	private static final class ServerTestX509TrustManager
			implements
				X509TrustManager {

		private static final Log LOG = LogFactory
				.getLog(ServerTestX509TrustManager.class);

		@Override
		public void checkClientTrusted(final X509Certificate[] chain,
				final String authType) throws CertificateException {
			LOG.debug("checkClientTrusted");
			LOG.debug("subject: " + chain[0].getSubjectX500Principal());
		}

		@Override
		public void checkServerTrusted(final X509Certificate[] chain,
				final String authType) throws CertificateException {
			LOG.debug("checkServerTrusted");
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			LOG.debug("getAcceptedIssuers");
			return new X509Certificate[]{};
		}

	}

	private static final class ServerTestX509KeyManager
			implements
				X509KeyManager {
		private static final Log LOG = LogFactory
				.getLog(ServerTestX509KeyManager.class);

		private final PrivateKey serverPrivateKey;

		private final X509Certificate serverCertificate;

		public ServerTestX509KeyManager(final PrivateKey serverPrivateKey,
				final X509Certificate serverCertificate) {
			this.serverPrivateKey = serverPrivateKey;
			this.serverCertificate = serverCertificate;
		}

		@Override
		public String chooseClientAlias(final String[] keyType,
				final Principal[] issuers, final Socket socket) {
			LOG.debug("chooseClientAlias");
			return null;
		}

		@Override
		public String chooseServerAlias(final String keyType,
				final Principal[] issuers, final Socket socket) {
			LOG.debug("chooseServerAlias: " + keyType);
			if (keyType.equals("RSA")) {
				return "test-server";
			}
			return null;
		}

		@Override
		public X509Certificate[] getCertificateChain(final String alias) {
			LOG.debug("getCertificateChain: " + alias);
			if (false == alias.equals("test-server")) {
				return null;
			}
			return new X509Certificate[]{this.serverCertificate};
		}

		@Override
		public String[] getClientAliases(final String keyType,
				final Principal[] issuers) {
			LOG.debug("getClientAliases");
			return null;
		}

		@Override
		public PrivateKey getPrivateKey(final String alias) {
			LOG.debug("getPrivateKey: " + alias);
			if (false == alias.equals("test-server")) {
				return null;
			}
			return this.serverPrivateKey;
		}

		@Override
		public String[] getServerAliases(final String keyType,
				final Principal[] issuers) {
			LOG.debug("getServerAliases");
			return null;
		}
	}

	private static KeyPair generateKeyPair() throws Exception {
		final KeyPairGenerator keyPairGenerator = KeyPairGenerator
				.getInstance("RSA");
		final SecureRandom random = new SecureRandom();
		keyPairGenerator.initialize(new RSAKeyGenParameterSpec(1024,
				RSAKeyGenParameterSpec.F4), random);
		final KeyPair keyPair = keyPairGenerator.generateKeyPair();
		return keyPair;
	}

	private X509Certificate generateCACertificate(final KeyPair keyPair,
			final String subject, final DateTime notBefore,
			final DateTime notAfter) throws Exception {
		LOG.debug("generate CA certificate: " + subject);

		final X500Name issuer = new X500Name(subject);
		final X500Name subjectX500Name = new X500Name(subject);

		final SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo
				.getInstance(keyPair.getPublic().getEncoded());

		final SecureRandom secureRandom = new SecureRandom();
		final byte[] serialValue = new byte[8];
		secureRandom.nextBytes(serialValue);
		final BigInteger serial = new BigInteger(serialValue);

		final X509v3CertificateBuilder x509v3CertificateBuilder = new X509v3CertificateBuilder(
				issuer, serial, notBefore.toDate(), notAfter.toDate(),
				subjectX500Name, publicKeyInfo);

		try {
			final JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();
			x509v3CertificateBuilder.addExtension(
					Extension.subjectKeyIdentifier, false, extensionUtils
							.createSubjectKeyIdentifier(keyPair.getPublic()));
			x509v3CertificateBuilder.addExtension(
					Extension.authorityKeyIdentifier, false, extensionUtils
							.createAuthorityKeyIdentifier(keyPair.getPublic()));

			x509v3CertificateBuilder.addExtension(
					MiscObjectIdentifiers.netscapeCertType, false,
					new NetscapeCertType(NetscapeCertType.sslCA
							| NetscapeCertType.smimeCA
							| NetscapeCertType.objectSigningCA));

			x509v3CertificateBuilder.addExtension(Extension.keyUsage, true,
					new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));

			x509v3CertificateBuilder.addExtension(Extension.basicConstraints,
					true, new BasicConstraints(2147483647));

		} catch (final Exception e) {
			throw new RuntimeException(e);
		}

		final AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder()
				.find("SHA1withRSA");
		final AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder()
				.find(sigAlgId);
		AsymmetricKeyParameter asymmetricKeyParameter;
		try {
			asymmetricKeyParameter = PrivateKeyFactory.createKey(keyPair
					.getPrivate().getEncoded());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		ContentSigner contentSigner;
		try {
			contentSigner = new BcRSAContentSignerBuilder(sigAlgId, digAlgId)
					.build(asymmetricKeyParameter);
		} catch (final OperatorCreationException e) {
			throw new RuntimeException(e);
		}
		final X509CertificateHolder x509CertificateHolder = x509v3CertificateBuilder
				.build(contentSigner);

		byte[] encodedCertificate;
		try {
			encodedCertificate = x509CertificateHolder.getEncoded();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		CertificateFactory certificateFactory;
		try {
			certificateFactory = CertificateFactory.getInstance("X.509");
		} catch (final CertificateException e) {
			throw new RuntimeException(e);
		}
		X509Certificate certificate;
		try {
			certificate = (X509Certificate) certificateFactory
					.generateCertificate(new ByteArrayInputStream(
							encodedCertificate));
		} catch (final CertificateException e) {
			throw new RuntimeException(e);
		}
		return certificate;
	}

	@Test
	public void testKeyManagerFactory() throws Exception {
		Security.addProvider(new BeIDProvider());
		final KeyManagerFactory keyManagerFactory = KeyManagerFactory
				.getInstance("BeID");
		assertNotNull(keyManagerFactory);

		final String algo = keyManagerFactory.getAlgorithm();
		LOG.debug("key manager factory algo: " + algo);
		assertEquals("BeID", algo);

		final KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
		assertNotNull(keyManagers);
	}
}
