/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2014-2024 e-Contract.be BV.
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

import java.io.StringWriter;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignContext;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.XMLValidateContext;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.client.spi.UserCancelledException;
import be.fedict.commons.eid.jca.AbstractBeIDPrivateKey;
import be.fedict.commons.eid.jca.BeIDKeyStoreParameter;
import be.fedict.commons.eid.jca.BeIDProvider;
import be.fedict.commons.eid.jca.UserCancelledSignatureException;

import static org.junit.jupiter.api.Assertions.*;

public class JCATest {

	private static final Logger LOGGER = LoggerFactory.getLogger(JCATest.class);

	@BeforeAll
	public static void setup() {
		Security.insertProviderAt(new BeIDProvider(), 1);
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	public void testGenericSignatureCreation() throws Exception {
		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);
		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		String signatureAlgorithm;
		switch (authnPrivateKey.getAlgorithm()) {
		case "RSA":
			signatureAlgorithm = "SHA256withRSA";
			break;
		case "EC":
			signatureAlgorithm = "SHA256withECDSA";
			assertInstanceOf(ECPrivateKey.class, authnPrivateKey);
			break;
		default:
			throw new IllegalStateException("unsupported key algo");
		}
		Signature signature = Signature.getInstance(signatureAlgorithm);
		signature.initSign(authnPrivateKey);
		byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		byte[] signatureValue = signature.sign();

		X509Certificate certificate = (X509Certificate) keyStore.getCertificate("Authentication");
		PublicKey publicKey = certificate.getPublicKey();
		signature.initVerify(publicKey);
		signature.update(toBeSigned);
		boolean result = signature.verify(signatureValue);
		assertTrue(result);
	}

	@Test
	public void testGenericXMLSignatureCreation() throws Exception {
		String javaVersion = System.getProperty("java.version");
		LOGGER.debug("Java version: {}", javaVersion);
		Provider[] providers = Security.getProviders();
		for (Provider provider : providers) {
			LOGGER.debug("JCA provider: {} ({})", provider.getName(), provider.getClass().getName());
		}
		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);
		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		X509Certificate authnCertificate = (X509Certificate) keyStore.getCertificate("Authentication");
		String digestAlgo = "http://www.w3.org/2001/04/xmlenc#sha256";
		String signatureAlgo;
		switch (authnPrivateKey.getAlgorithm()) {
		case "RSA":
			signatureAlgo = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
			break;
		case "EC":
			signatureAlgo = "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256";
			break;
		default:
			throw new IllegalStateException("unsupported key algo");
		}
		LOGGER.debug("signature algorithm: {}", signatureAlgo);

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		Document document = documentBuilder.newDocument();
		Element rootElement = document.createElementNS("urn:test", "Root");
		document.appendChild(rootElement);

		XMLSignContext domSignContext = new DOMSignContext(authnPrivateKey, document.getDocumentElement());
		XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory.getInstance("DOM");
		List<Reference> references = new LinkedList<>();
		List<Transform> transforms = new LinkedList<>();
		transforms.add(xmlSignatureFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
		transforms.add(xmlSignatureFactory.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE,
				(C14NMethodParameterSpec) null));
		Reference reference = xmlSignatureFactory.newReference("",
				xmlSignatureFactory.newDigestMethod(digestAlgo, null), transforms, null, null);
		references.add(reference);

		SignedInfo signedInfo = xmlSignatureFactory.newSignedInfo(
				xmlSignatureFactory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,
						(C14NMethodParameterSpec) null),
				xmlSignatureFactory.newSignatureMethod(signatureAlgo, null), references);

		KeyInfoFactory keyInfoFactory = xmlSignatureFactory.getKeyInfoFactory();
		X509Data x509Data = keyInfoFactory.newX509Data(Collections.singletonList(authnCertificate));
		KeyInfo keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(x509Data));

		XMLSignature xmlSignature = xmlSignatureFactory.newXMLSignature(signedInfo, keyInfo);
		xmlSignature.sign(domSignContext);

		LOGGER.debug("validating XML signature...");
		NodeList signatureNodeList = document.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature");
		Element signatureElement = (Element) signatureNodeList.item(0);
		XMLValidateContext validateContext = new DOMValidateContext(authnCertificate.getPublicKey(), signatureElement);
		xmlSignature = xmlSignatureFactory.unmarshalXMLSignature(validateContext);
		boolean result = xmlSignature.validate(validateContext);
		assertTrue(result);

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		StringWriter stringWriter = new StringWriter();
		transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
		LOGGER.debug("result: {}", stringWriter);
	}

	@Test
	public void testSoftwareXMLSignatureECDSA() throws Exception {
		String javaVersion = System.getProperty("java.version");
		LOGGER.debug("Java version: {}", javaVersion);
		Provider[] providers = Security.getProviders();
		Provider sunECProvider = null;
		for (Provider provider : providers) {
			if (provider.getName().equals("SunEC")) {
				sunECProvider = provider;
			}
			LOGGER.debug("JCA provider: {} ({})", provider.getName(), provider.getClass().getName());
			Set<Map.Entry<Object, Object>> entries = provider.entrySet();
			for (Map.Entry<Object, Object> entry : entries) {
				String algo = (String) entry.getKey();
				if (!algo.contains("Signature")) {
					continue;
				}
				if (!algo.contains("ECDSA")) {
					continue;
				}
				LOGGER.debug("entry type: {}", algo);
			}
		}
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp384r1");
		keyGen.initialize(ecSpec, random);

		KeyPair keyPair = keyGen.generateKeyPair();
		PrivateKey privateKey = keyPair.getPrivate();
		PublicKey publicKey = keyPair.getPublic();
		String digestAlgo = "http://www.w3.org/2001/04/xmlenc#sha256";
		String signatureAlgo = "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256";

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		Document document = documentBuilder.newDocument();
		Element rootElement = document.createElementNS("urn:test", "Root");
		document.appendChild(rootElement);

		XMLSignContext domSignContext = new DOMSignContext(privateKey, document.getDocumentElement());
		// next is to prevent BeIDProvider to high-jack ECDSA
		domSignContext.setProperty("org.jcp.xml.dsig.internal.dom.SignatureProvider", sunECProvider);
		XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory.getInstance("DOM");
		List<Reference> references = new LinkedList<>();
		List<Transform> transforms = new LinkedList<>();
		transforms.add(xmlSignatureFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
		transforms.add(xmlSignatureFactory.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE,
				(C14NMethodParameterSpec) null));
		Reference reference = xmlSignatureFactory.newReference("",
				xmlSignatureFactory.newDigestMethod(digestAlgo, null), transforms, null, null);
		references.add(reference);

		SignedInfo signedInfo = xmlSignatureFactory.newSignedInfo(
				xmlSignatureFactory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,
						(C14NMethodParameterSpec) null),
				xmlSignatureFactory.newSignatureMethod(signatureAlgo, null), references);

		KeyInfoFactory keyInfoFactory = xmlSignatureFactory.getKeyInfoFactory();
		KeyValue keyValue = keyInfoFactory.newKeyValue(publicKey);
		KeyInfo keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(keyValue));

		XMLSignature xmlSignature = xmlSignatureFactory.newXMLSignature(signedInfo, keyInfo);
		xmlSignature.sign(domSignContext);

		LOGGER.debug("validating XML signature...");
		NodeList signatureNodeList = document.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature");
		Element signatureElement = (Element) signatureNodeList.item(0);
		XMLValidateContext validateContext = new DOMValidateContext(publicKey, signatureElement);
		validateContext.setProperty("org.jcp.xml.dsig.internal.dom.SignatureProvider", sunECProvider);
		xmlSignature = xmlSignatureFactory.unmarshalXMLSignature(validateContext);
		boolean result = xmlSignature.validate(validateContext);
		assertTrue(result);

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		StringWriter stringWriter = new StringWriter();
		transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
		LOGGER.debug("result: {}", stringWriter);
	}

	@Test
	public void testSwingParentLocale() throws Exception {
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
		String signatureAlgo;
		if (authnPrivateKey instanceof ECPrivateKey) {
			signatureAlgo = "SHA256withECDSA";
		} else {
			signatureAlgo = "SHA256withRSA";
		}
		final Signature signature = Signature.getInstance(signatureAlgo);
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
		MyFrame myFrame = new MyFrame();

		final KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(myFrame);

		final PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		String signatureAlgo;
		if (authnPrivateKey instanceof ECPrivateKey) {
			signatureAlgo = "SHA256withECDSA";
		} else {
			signatureAlgo = "SHA256withRSA";
		}
		final Signature signature = Signature.getInstance(signatureAlgo);
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
		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);

		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		String signatureAlgo;
		if (authnPrivateKey instanceof ECPrivateKey) {
			signatureAlgo = "SHA256withECDSA";
		} else {
			signatureAlgo = "SHA256withRSA";
		}
		final Signature signature = Signature.getInstance(signatureAlgo);
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
		KeyStore keyStore = KeyStore.getInstance("BeID");
		BeIDKeyStoreParameter keyStoreParameter = new BeIDKeyStoreParameter();
		keyStoreParameter.setAutoRecovery(true);
		keyStoreParameter.setCardReaderStickiness(true);
		keyStore.load(keyStoreParameter);

		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		PublicKey authnPublicKey = keyStore.getCertificate("Authentication").getPublicKey();
		String signatureAlgo;
		if (authnPrivateKey instanceof ECPrivateKey) {
			signatureAlgo = "SHA256withECDSA";
		} else {
			signatureAlgo = "SHA256withRSA";
		}
		final Signature signature = Signature.getInstance(signatureAlgo);
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
		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);

		for (int idx = 0; idx < 100; idx++) {
			assertNotNull(keyStore.getCertificate("Authentication"));
		}
	}

	@Test
	public void testCAAliases() throws Exception {
		// setup
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
		final KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);

		// operate
		assertTrue(keyStore.containsAlias("RRN"));
		Entry entry = keyStore.getEntry("RRN", null);
		assertNotNull(entry);
		assertInstanceOf(TrustedCertificateEntry.class, entry);
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
		verifySignatureAlgorithm("SHA3-256withRSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA3-384withRSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA3-512withRSA", authnPrivateKey, authnCertificate.getPublicKey());
	}

	@Test
	public void testNonRepudiationSignature() throws Exception {
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
		final KeyStore keyStore = KeyStore.getInstance("BeID");
		final BeIDKeyStoreParameter keyStoreParameter = new BeIDKeyStoreParameter();
		final BeIDCard beIDCard = getBeIDCard();
		keyStoreParameter.setBeIDCard(beIDCard);
		keyStoreParameter.setLogoff(true);
		keyStore.load(keyStoreParameter);

		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);

		Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initSign(authnPrivateKey);
		assertInstanceOf(BeIDProvider.class, signature.getProvider());

		JOptionPane.showMessageDialog(null, "Please click Cancel on the next PIN dialog.");

		final byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		try {
			signature.sign();
			fail();
		} catch (UserCancelledSignatureException e) {
			// expected
			assertInstanceOf(UserCancelledException.class, e.getCause());
		}
	}

	@Test
	public void testBeIDSignature() throws Exception {
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
		final KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);
		PrivateKeyEntry privateKeyEntry = (PrivateKeyEntry) keyStore.getEntry("Authentication", null);
		assertNotNull(privateKeyEntry);
		assertInstanceOf(AbstractBeIDPrivateKey.class, privateKeyEntry.getPrivateKey());

		TrustedCertificateEntry caEntry = (TrustedCertificateEntry) keyStore.getEntry("CA", null);
		assertNotNull(caEntry);
		LOGGER.debug("CA entry: {}", ((X509Certificate) caEntry.getTrustedCertificate()).getSubjectX500Principal());

		TrustedCertificateEntry rootEntry = (TrustedCertificateEntry) keyStore.getEntry("Root", null);
		assertNotNull(rootEntry);
		LOGGER.debug("root entry: {}", ((X509Certificate) rootEntry.getTrustedCertificate()).getSubjectX500Principal());
	}

	@Test
	public void testECDSA() throws Exception {
		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);
		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		X509Certificate authnCertificate = (X509Certificate) keyStore.getCertificate("Authentication");

		PrivateKey signPrivateKey = (PrivateKey) keyStore.getKey("Signature", null);
		X509Certificate signCertificate = (X509Certificate) keyStore.getCertificate("Signature");

		verifySignatureAlgorithm("SHA256withECDSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA384withECDSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA512withECDSA", authnPrivateKey, authnCertificate.getPublicKey());

		// verifySignatureAlgorithm("SHA256withECDSA", signPrivateKey,
		// signCertificate.getPublicKey());
		// verifySignatureAlgorithm("SHA384withECDSA", signPrivateKey,
		// signCertificate.getPublicKey());
		// verifySignatureAlgorithm("SHA512withECDSA", signPrivateKey,
		// signCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA3-256withECDSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA3-384withECDSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA3-512withECDSA", authnPrivateKey, authnCertificate.getPublicKey());

		verifySignatureAlgorithm("SHA256withECDSAinP1363Format", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA384withECDSAinP1363Format", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA512withECDSAinP1363Format", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA3-256withECDSAinP1363Format", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA3-384withECDSAinP1363Format", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA3-512withECDSAinP1363Format", authnPrivateKey, authnCertificate.getPublicKey());
	}

	@Test
	public void testECDSA_SHA3() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp384r1");
		keyGen.initialize(ecSpec, random);

		KeyPair keyPair = keyGen.generateKeyPair();
		PrivateKey privateKey = keyPair.getPrivate();
		PublicKey publicKey = keyPair.getPublic();
		LOGGER.debug("public key type: {}", publicKey.getClass().getName());
		ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
		ECParameterSpec ecParameterSpec = ecPublicKey.getParams();
		LOGGER.debug("EC parameter spec: {}", ecParameterSpec);
		LOGGER.debug("public key size: {} bytes", publicKey.getEncoded().length);

		KeyFactory keyFactory = KeyFactory.getInstance("EC");
		EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());
		keyFactory.generatePublic(publicKeySpec);

		Signature signature = Signature.getInstance("SHA3-256withECDSA");
		signature.initSign(privateKey);
		final byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		final byte[] signatureValue = signature.sign();
		LOGGER.debug("signature size: {} bytes", signatureValue.length);
		signature.initVerify(publicKey);
		LOGGER.debug("signature provider: {}", signature.getProvider().getName());
		signature.update(toBeSigned);
		final boolean result = signature.verify(signatureValue);
		assertTrue(result);
	}

	@Test
	public void testNONEwithRSA() throws Exception {
		byte[] data = "hello world".getBytes();
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
		byte[] digest = messageDigest.digest(data);

		ASN1ObjectIdentifier hashAlgoId = NISTObjectIdentifiers.id_sha256;
		DigestInfo digestInfo = new DigestInfo(new AlgorithmIdentifier(hashAlgoId, DERNull.INSTANCE), digest);

		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);
		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		X509Certificate authnCertificate = (X509Certificate) keyStore.getCertificate("Authentication");

		Signature signature = Signature.getInstance("NONEwithRSA");
		signature.initSign(authnPrivateKey);
		signature.update(digestInfo.getEncoded());
		byte[] signatureValue = signature.sign();

		signature = Signature.getInstance("SHA256withRSA");
		signature.initVerify(authnCertificate);
		signature.update(data);
		boolean result = signature.verify(signatureValue);
		assertTrue(result);
	}

	@Test
	public void testNONEwithECDSA() throws Exception {
		byte[] data = "hello world".getBytes();
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
		byte[] digest = messageDigest.digest(data);

		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);
		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		X509Certificate authnCertificate = (X509Certificate) keyStore.getCertificate("Authentication");

		Signature signature = Signature.getInstance("NONEwithECDSA");
		signature.initSign(authnPrivateKey);
		signature.update(digest);
		byte[] signatureValue = signature.sign();

		signature = Signature.getInstance("SHA256withECDSA");
		signature.initVerify(authnCertificate);
		signature.update(data);
		boolean result = signature.verify(signatureValue);
		assertTrue(result);
	}

	@Test
	public void testNONEwithECDSAinP1363Format() throws Exception {
		byte[] data = "hello world".getBytes();
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
		byte[] digest = messageDigest.digest(data);

		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);
		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		X509Certificate authnCertificate = (X509Certificate) keyStore.getCertificate("Authentication");

		Signature signature = Signature.getInstance("NONEwithECDSAinP1363Format");
		signature.initSign(authnPrivateKey);
		signature.update(digest);
		byte[] signatureValue = signature.sign();

		signature = Signature.getInstance("SHA256withECDSAinP1363Format");
		signature.initVerify(authnCertificate);
		signature.update(data);
		boolean result = signature.verify(signatureValue);
		assertTrue(result);
	}

	private void verifySignatureAlgorithm(final String signatureAlgorithm, final PrivateKey privateKey,
			PublicKey publicKey) throws Exception {
		Signature signature = Signature.getInstance(signatureAlgorithm);
		signature.initSign(privateKey);
		assertInstanceOf(BeIDProvider.class, signature.getProvider());

		final byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		final byte[] signatureValue = signature.sign();
		assertNotNull(signatureValue);
		LOGGER.debug("signature size: {} bytes", signatureValue.length);

		signature.initVerify(publicKey);
		LOGGER.debug("signature provider: {}", signature.getProvider().getName());
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
