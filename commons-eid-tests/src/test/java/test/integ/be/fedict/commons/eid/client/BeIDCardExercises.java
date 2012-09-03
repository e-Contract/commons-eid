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
import be.fedict.commons.eid.client.BeIDCardsException;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.client.event.BeIDCardListener;
import be.fedict.commons.eid.client.impl.BeIDDigest;
import be.fedict.commons.eid.consumer.BeIDIntegrity;
import be.fedict.commons.eid.consumer.Identity;

public class BeIDCardExercises {
	private static final Log LOG = LogFactory.getLog(BeIDCardExercises.class);
	private BeIDCards beIDCards;

	@Test
	public void testReadFiles() throws Exception {
		final BeIDCard beIDCard = getBeIDCard();
		beIDCard.addCardListener(new TestBeIDCardListener());

		LOG.debug("reading identity file");
		final byte[] identityFile = beIDCard.readFile(FileType.Identity);
		LOG.debug("reading identity signature file");
		final byte[] identitySignatureFile = beIDCard
				.readFile(FileType.IdentitySignature);
		LOG.debug("reading RRN certificate file");
		final byte[] rrnCertificateFile = beIDCard
				.readFile(FileType.RRNCertificate);

		final CertificateFactory certificateFactory = CertificateFactory
				.getInstance("X.509");
		final X509Certificate rrnCertificate = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(
						rrnCertificateFile));

		beIDCard.close();

		final BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		final Identity identity = beIDIntegrity.getVerifiedIdentity(
				identityFile, identitySignatureFile, rrnCertificate);

		assertNotNull(identity);
		assertNotNull(identity.getNationalNumber());
	}

	@Test
	public void testAuthnSignature() throws Exception {
		final BeIDCard beIDCard = getBeIDCard();

		final byte[] toBeSigned = new byte[10];
		final SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(toBeSigned);

		final X509Certificate authnCertificate = beIDCard
				.getAuthenticationCertificate();

		byte[] signatureValue;
		try {
			signatureValue = beIDCard.signAuthn(toBeSigned, false);
		} finally {
			beIDCard.close();
		}

		final BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		final boolean result = beIDIntegrity.verifyAuthnSignature(toBeSigned,
				signatureValue, authnCertificate);

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
			signatureValue = beIDCard.sign(digestValue, BeIDDigest.SHA_1,
					FileType.NonRepudiationCertificate, false);
			assertNotNull(signatureValue);
			signingCertificate = beIDCard.getSigningCertificate();
		} finally {
			beIDCard.close();
		}

		final BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		final boolean result = beIDIntegrity.verifyNonRepSignature(digestValue,
				signatureValue, signingCertificate);
		assertTrue(result);
	}

	// @Test
	// public void testUnblockPIN() throws Exception
	// {
	// BeIDCard beIDCard = getBeIDCard();
	// beIDCard.unblockPin(true);
	// }

	private BeIDCard getBeIDCard() {
		this.beIDCards = new BeIDCards(new TestLogger());
		BeIDCard beIDCard = null;
		try {
			beIDCard = this.beIDCards.getOneBeIDCard();
			assertNotNull(beIDCard);

			beIDCard.addCardListener(new BeIDCardListener() {
				@Override
				public void notifyReadProgress(final FileType fileType,
						final int offset, final int estimatedMaxSize) {
					LOG.debug("read progress of " + fileType.name() + ":"
							+ offset + " of " + estimatedMaxSize);
				}

				@Override
				public void notifySigningBegin(final FileType keyType) {
					LOG.debug("signing with "
							+ (keyType == FileType.AuthentificationCertificate
									? "authentication"
									: "non-repudiation") + " key has begun");
				}

				@Override
				public void notifySigningEnd(final FileType keyType) {
					LOG.debug("signing with "
							+ (keyType == FileType.AuthentificationCertificate
									? "authentication"
									: "non-repudiation") + " key has ended");
				}
			});
		} catch (final BeIDCardsException bcex) {
			System.err.println(bcex.getMessage());
		}

		return beIDCard;
	}
}
