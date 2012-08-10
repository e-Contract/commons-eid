package test.integ.be.fedict.commons.eid.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.client.BeIDFileType;
import be.fedict.commons.eid.client.impl.BeIDDigest;
import be.fedict.commons.eid.consumer.BeIDIntegrity;
import be.fedict.commons.eid.consumer.Identity;

public class BeIDCardsExercise {
	private static final Log LOG = LogFactory.getLog(BeIDCardsExercise.class);
	private BeIDCards beIDCards;

	@Test
	public void testReadFiles() throws Exception {
		BeIDCard beIDCard = getBeIDCard();
		beIDCard.addCardListener(new TestBeIDCardListener());

		LOG.debug("reading identity file");
		byte[] identityFile = beIDCard.readFile(BeIDFileType.Identity);
		LOG.debug("reading identity signature file");
		byte[] identitySignatureFile = beIDCard
				.readFile(BeIDFileType.IdentitySignature);
		LOG.debug("reading RRN certificate file");
		byte[] rrnCertificateFile = beIDCard
				.readFile(BeIDFileType.RRNCertificate);

		CertificateFactory certificateFactory = CertificateFactory
				.getInstance("X.509");
		X509Certificate rrnCertificate = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(
						rrnCertificateFile));

		beIDCard.close();

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		Identity identity = beIDIntegrity.getVerifiedIdentity(identityFile,
				identitySignatureFile, rrnCertificate);

		assertNotNull(identity);
		assertNotNull(identity.getNationalNumber());
	}

	@Test
	public void testAuthnSignature() throws Exception {
		BeIDCard beIDCard = getBeIDCard();

		byte[] toBeSigned = new byte[10];
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(toBeSigned);

		X509Certificate authnCertificate = beIDCard
				.getAuthenticationCertificate();

		byte[] signatureValue;
		try {
			signatureValue = beIDCard.signAuthn(toBeSigned, false);
		} finally {
			beIDCard.close();
		}

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		boolean result = beIDIntegrity.verifyAuthnSignature(toBeSigned,
				signatureValue, authnCertificate);

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
	public void testNonRepSignature() throws Exception {
		byte[] toBeSigned = new byte[10];
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(toBeSigned);
		MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
		byte[] digestValue = messageDigest.digest(toBeSigned);

		BeIDCard beIDCard = getBeIDCard();

		X509Certificate signingCertificate;
		byte[] signatureValue;
		try {
			signatureValue = beIDCard.sign(digestValue, BeIDDigest.SHA_1,
					BeIDFileType.SigningCertificate, false);
			assertNotNull(signatureValue);
			signingCertificate = beIDCard.getSigningCertificate();
		} finally {
			beIDCard.close();
		}

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		boolean result = beIDIntegrity.verifyNonRepSignature(digestValue,
				signatureValue, signingCertificate);
		assertTrue(result);
	}

	private BeIDCard getBeIDCard() {
		if (this.beIDCards == null) {
			this.beIDCards = new BeIDCards(new TestLogger());
		}

		BeIDCard beIDCard = this.beIDCards.getFirstBeIDCard();
		assertNotNull(beIDCard);
		return beIDCard;
	}

}
