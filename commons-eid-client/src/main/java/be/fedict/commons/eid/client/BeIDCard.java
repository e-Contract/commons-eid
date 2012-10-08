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

package be.fedict.commons.eid.client;

import java.awt.GraphicsEnvironment;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import be.fedict.commons.eid.client.event.BeIDCardListener;
import be.fedict.commons.eid.client.impl.BeIDDigest;
import be.fedict.commons.eid.client.impl.CCID;
import be.fedict.commons.eid.client.impl.VoidLogger;
import be.fedict.commons.eid.client.spi.BeIDCardUI;
import be.fedict.commons.eid.client.spi.Logger;

public class BeIDCard {
	private static final String UI_MISSING_LOG_MESSAGE = "No BeIDCardUI set and can't load DefaultBeIDCardUI";
	private static final String UI_DEFAULT_REQUIRES_HEAD = "No BeIDCardUI set and DefaultBeIDCardUI requires a graphical environment";
	private static final String DEFAULT_UI_IMPLEMENTATION = "be.fedict.eid.commons.dialogs.DefaultBeIDCardUI";

	private static final byte[] BELPIC_AID = new byte[] { (byte) 0xA0, 0x00,
			0x00, 0x01, 0x77, 0x50, 0x4B, 0x43, 0x53, 0x2D, 0x31, 0x35 };
	private static final byte[] APPLET_AID = new byte[] { (byte) 0xA0, 0x00,
			0x00, 0x00, 0x30, 0x29, 0x05, 0x70, 0x00, (byte) 0xAD, 0x13, 0x10,
			0x01, 0x01, (byte) 0xFF };
	private static final int BLOCK_SIZE = 0xff;

	private final CardChannel cardChannel;
	private final List<BeIDCardListener> cardListeners;
	private final CertificateFactory certificateFactory;

	private final Card card;
	private final Logger logger;

	private CCID ccid;
	private BeIDCardUI ui;
	private Locale locale;

	public BeIDCard(final Card card, final Logger logger) {
		this.card = card;
		this.cardChannel = card.getBasicChannel();
		if (null == logger) {
			throw new IllegalArgumentException("logger expected");
		}
		this.logger = logger;
		this.cardListeners = new LinkedList<BeIDCardListener>();
		try {
			this.certificateFactory = CertificateFactory.getInstance("X.509");
		} catch (final CertificateException e) {
			throw new RuntimeException("X.509 algo", e);
		}
	}

	public BeIDCard(final Card card) {
		this(card, new VoidLogger());
	}

	public BeIDCard(final CardTerminal cardTerminal, final Logger logger)
			throws CardException {
		this(cardTerminal.connect("T=0"), logger);
	}

	public BeIDCard(final CardTerminal cardTerminal) throws CardException {
		this(cardTerminal.connect("T=0"));
	}

	/**
	 * Gettings certificates for a given fileType.
	 * 
	 * @param fileType
	 * @return
	 * @throws CertificateException
	 * @throws CardException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public X509Certificate getCertificate(final FileType fileType)
			throws CertificateException, CardException, IOException,
			InterruptedException {
		return (X509Certificate) this.certificateFactory
				.generateCertificate(new ByteArrayInputStream(
						readFile(fileType)));
	}

	/**
	 * Gives back the X509 authentication certificate.
	 * 
	 * @return
	 * @throws CardException
	 * @throws IOException
	 * @throws CertificateException
	 * @throws InterruptedException
	 */
	public X509Certificate getAuthenticationCertificate() throws CardException,
			IOException, CertificateException, InterruptedException {
		return this.getCertificate(FileType.AuthentificationCertificate);
	}

	/**
	 * Gives back the X509 non-repudiation certificate.
	 * 
	 * @return
	 * @throws CardException
	 * @throws IOException
	 * @throws CertificateException
	 * @throws InterruptedException
	 */
	public X509Certificate getSigningCertificate() throws CardException,
			IOException, CertificateException, InterruptedException {
		return this.getCertificate(FileType.NonRepudiationCertificate);
	}

	/**
	 * Gives back the citizen CA certificate.
	 * 
	 * @return
	 * @throws CardException
	 * @throws IOException
	 * @throws CertificateException
	 * @throws InterruptedException
	 */
	public X509Certificate getCACertificate() throws CardException,
			IOException, CertificateException, InterruptedException {
		return this.getCertificate(FileType.CACertificate);
	}

	/**
	 * Gives back the national registration certificate.
	 * 
	 * @return
	 * @throws CardException
	 * @throws IOException
	 * @throws CertificateException
	 * @throws InterruptedException
	 */
	public X509Certificate getRRNCertificate() throws CardException,
			IOException, CertificateException, InterruptedException {
		return this.getCertificate(FileType.RRNCertificate);
	}

	/**
	 * Gives back the entire certificate chain for a given file type. Of course,
	 * only file types corresponding with a certificate are accepted.
	 * 
	 * @param fileType
	 * @return
	 * @throws CertificateException
	 * @throws CardException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public List<X509Certificate> getCertificateChain(final FileType fileType)
			throws CertificateException, CardException, IOException,
			InterruptedException {
		final CertificateFactory certificateFactory = CertificateFactory
				.getInstance("X.509");
		final List<X509Certificate> chain = new LinkedList<X509Certificate>();
		chain.add((X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(
						readFile(fileType))));
		if (fileType.chainIncludesCitizenCA()) {
			chain.add((X509Certificate) certificateFactory
					.generateCertificate(new ByteArrayInputStream(
							readFile(FileType.CACertificate))));
		}
		chain.add((X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(
						readFile(FileType.RootCertificate))));
		return chain;
	}

	/**
	 * Gives back the X509 authentication certificate chain.
	 * 
	 * @return
	 * @throws CardException
	 * @throws IOException
	 * @throws CertificateException
	 * @throws InterruptedException
	 */
	public List<X509Certificate> getAuthenticationCertificateChain()
			throws CardException, IOException, CertificateException,
			InterruptedException {
		return this.getCertificateChain(FileType.AuthentificationCertificate);
	}

	/**
	 * Gives back the X509 non-repudiation certificate chain.
	 * 
	 * @return
	 * @throws CardException
	 * @throws IOException
	 * @throws CertificateException
	 * @throws InterruptedException
	 */
	public List<X509Certificate> getSigningCertificateChain()
			throws CardException, IOException, CertificateException,
			InterruptedException {
		return this.getCertificateChain(FileType.NonRepudiationCertificate);
	}

	/**
	 * Gives back the Citizen CA X509 certificate chain.
	 * 
	 * @return
	 * @throws CardException
	 * @throws IOException
	 * @throws CertificateException
	 * @throws InterruptedException
	 */
	public List<X509Certificate> getCACertificateChain() throws CardException,
			IOException, CertificateException, InterruptedException {
		return this.getCertificateChain(FileType.CACertificate);
	}

	/**
	 * Gives back the national registration X509 certificate chain.
	 * 
	 * @return
	 * @throws CardException
	 * @throws IOException
	 * @throws CertificateException
	 * @throws InterruptedException
	 */
	public List<X509Certificate> getRRNCertificateChain() throws CardException,
			IOException, CertificateException, InterruptedException {
		return this.getCertificateChain(FileType.RRNCertificate);
	}

	/**
	 * Sign a given digest value.
	 * 
	 * @param digestValue
	 *            the digest value to be signed.
	 * @param digestAlgo
	 *            the algorithm used to calculate the given digest value.
	 * @param fileType
	 *            the certificate's file type.
	 * @param requireSecureReader
	 *            <code>true</code> if a secure pinpad reader is required.
	 * @return
	 * @throws CardException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public byte[] sign(final byte[] digestValue, final BeIDDigest digestAlgo,
			final FileType fileType, final boolean requireSecureReader)
			throws CardException, IOException, InterruptedException {
		if (!fileType.isCertificateUserCanSignWith()) {
			throw new IllegalArgumentException(
					"Not a certificate that can be used for signing: "
							+ fileType.name());
		}

		if (getCCID().hasFeature(CCID.FEATURE.EID_PIN_PAD_READER)) {
			this.logger.debug("eID-aware secure PIN pad reader detected");
		}

		if (requireSecureReader
				&& (!getCCID().hasFeature(CCID.FEATURE.VERIFY_PIN_DIRECT))
				&& (getCCID().hasFeature(CCID.FEATURE.VERIFY_PIN_START))) {
			throw new SecurityException("not a secure reader");
		}

		notifySigningBegin(fileType);

		try {
			// select the key
			this.logger.debug("selecting key...");

			ResponseAPDU responseApdu = transmitCommand(
					BeIDCommandAPDU.SELECT_ALGORITHM_AND_PRIVATE_KEY,
					new byte[] { (byte) 0x04, // length
							// of
							// following
							// data
							(byte) 0x80, digestAlgo.getAlgorithmReference(), // algorithm
							// reference
							(byte) 0x84, fileType.getKeyId() }); // private key
			// reference

			if (0x9000 != responseApdu.getSW()) {
				throw new ResponseAPDUException(
						"SET (select algorithm and private key) error",
						responseApdu);
			}

			if (FileType.NonRepudiationCertificate.getKeyId() == fileType
					.getKeyId()) {
				this.logger
						.debug("non-repudiation key detected, immediate PIN verify");
				verifyPin(PINPurpose.NonRepudiationSignature);
			}

			final ByteArrayOutputStream digestInfo = new ByteArrayOutputStream();
			digestInfo.write(digestAlgo.getPrefix(digestValue.length));
			digestInfo.write(digestValue);

			this.logger.debug("computing digital signature...");
			responseApdu = transmitCommand(
					BeIDCommandAPDU.COMPUTE_DIGITAL_SIGNATURE,
					digestInfo.toByteArray());
			if (0x9000 == responseApdu.getSW()) {
				/*
				 * OK, we could use the card PIN caching feature.
				 * 
				 * Notice that the card PIN caching also works when first doing
				 * an authentication after a non-repudiation signature.
				 */
				return responseApdu.getData();
			}
			if (0x6982 != responseApdu.getSW()) {
				this.logger.debug("SW: "
						+ Integer.toHexString(responseApdu.getSW()));
				throw new ResponseAPDUException(
						"compute digital signature error", responseApdu);
			}
			/*
			 * 0x6982 = Security status not satisfied, so we do a PIN
			 * verification before retrying.
			 */
			this.logger.debug("PIN verification required...");
			verifyPin(PINPurpose.fromFileType(fileType));

			this.logger
					.debug("computing digital signature (attempt #2 after PIN verification)...");
			responseApdu = transmitCommand(
					BeIDCommandAPDU.COMPUTE_DIGITAL_SIGNATURE,
					digestInfo.toByteArray());
			if (0x9000 != responseApdu.getSW()) {
				throw new ResponseAPDUException(
						"compute digital signature error", responseApdu);
			}

			return responseApdu.getData();
		} finally {
			notifySigningEnd(fileType);
		}
	}

	/**
	 * Create an authentication signature.
	 * 
	 * @param toBeSigned
	 * @param requireSecureReader
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws CardException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public byte[] signAuthn(final byte[] toBeSigned,
			final boolean requireSecureReader) throws NoSuchAlgorithmException,
			CardException, IOException, InterruptedException {
		final MessageDigest messageDigest = BeIDDigest.SHA_1
				.getMessageDigestInstance();
		final byte[] digest = messageDigest.digest(toBeSigned);
		return this.sign(digest, BeIDDigest.SHA_1,
				FileType.AuthentificationCertificate, requireSecureReader);
	}

	/**
	 * Verifying PIN Code (without other actions, for testing PIN), using the
	 * most secure method available. Note that this still has the side effect of
	 * loading a successfully tests PIN into the PIN cache, so that unless the
	 * card is removed, a subsequent authentication attempt will not request the
	 * PIN, but proceed with the PIN given here.
	 * 
	 * @throws IOException
	 * @throws CardException
	 * @throws InterruptedException
	 */
	public void verifyPin() throws IOException, CardException,
			InterruptedException {
		this.verifyPin(PINPurpose.PINTest);
	}

	/**
	 * Change PIN code. This method will attempt to change PIN using the most
	 * secure method available. if requiresSecureReader is true, this will throw
	 * a SecurityException if no SPR is available, otherwise, this will default
	 * to changing the PIN via the UI
	 * 
	 * @param requireSecureReader
	 * @throws Exception
	 */
	public void changePin(final boolean requireSecureReader) throws Exception {
		if (requireSecureReader
				&& (!getCCID().hasFeature(CCID.FEATURE.MODIFY_PIN_DIRECT))
				&& (!getCCID().hasFeature(CCID.FEATURE.MODIFY_PIN_START))) {
			throw new SecurityException("not a secure reader");
		}

		int retriesLeft = -1;
		ResponseAPDU responseApdu;
		do {
			if (getCCID().hasFeature(CCID.FEATURE.MODIFY_PIN_START)) {
				this.logger.debug("using modify pin start/finish...");
				responseApdu = changePINViaCCIDStartFinish(retriesLeft);
			} else if (getCCID().hasFeature(CCID.FEATURE.MODIFY_PIN_DIRECT)) {
				this.logger.debug("could use direct PIN modify here...");
				responseApdu = changePINViaCCIDDirect(retriesLeft);
			} else {
				responseApdu = changePINViaUI(retriesLeft);
			}

			if (0x9000 != responseApdu.getSW()) {
				this.logger.debug("CHANGE PIN error");
				this.logger.debug("SW: "
						+ Integer.toHexString(responseApdu.getSW()));
				if (0x6983 == responseApdu.getSW()) {
					getUI().advisePINBlocked();
					throw new ResponseAPDUException("eID card blocked!",
							responseApdu);
				}
				if (0x63 != responseApdu.getSW1()) {
					this.logger.debug("PIN change error. Card blocked?");
					throw new ResponseAPDUException("PIN Change Error",
							responseApdu);
				}
				retriesLeft = responseApdu.getSW2() & 0xf;
				this.logger.debug("retries left: " + retriesLeft);
			}
		} while (0x9000 != responseApdu.getSW());
		getUI().advisePINChanged();
	}

	/**
	 * Gives back random data generated by the eID card itself.
	 * 
	 * @param size
	 *            the size of the requested random data.
	 * @return
	 * @throws CardException
	 */
	public byte[] getChallenge(final int size) throws CardException {
		final ResponseAPDU responseApdu = transmitCommand(
				BeIDCommandAPDU.GET_CHALLENGE, new byte[] {}, 0, 0, size);
		if (0x9000 != responseApdu.getSW()) {
			this.logger.debug("get challenge failure: "
					+ Integer.toHexString(responseApdu.getSW()));
			throw new ResponseAPDUException("get challenge failure: "
					+ Integer.toHexString(responseApdu.getSW()), responseApdu);
		}
		if (size != responseApdu.getData().length) {
			throw new RuntimeException("challenge size incorrect: "
					+ responseApdu.getData().length);
		}
		return responseApdu.getData();
	}

	/**
	 * Create a text message transaction signature. The FedICT eID aware secure
	 * pinpad readers can visualize such type of text message transactions on
	 * their hardware display.
	 * 
	 * @param transactionMessage
	 *            the transaction message to be signed.
	 * @param requireSecureReader
	 * @return
	 * @throws CardException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public byte[] signTransactionMessage(final String transactionMessage,
			final boolean requireSecureReader) throws CardException,
			IOException, InterruptedException {
		if (getCCID().hasFeature(CCID.FEATURE.EID_PIN_PAD_READER)) {
			getUI().adviseSecureReaderOperation();
		}

		byte[] signature;
		try {
			signature = this.sign(transactionMessage.getBytes(),
					BeIDDigest.PLAIN_TEXT,
					FileType.AuthentificationCertificate, requireSecureReader);
		} finally {
			if (getCCID().hasFeature(CCID.FEATURE.EID_PIN_PAD_READER)) {
				getUI().adviseSecureReaderOperationEnd();
			}
		}
		return signature;
	}

	/**
	 * Gives back the ATR of the eID card.
	 * 
	 * @return
	 */
	public ATR getATR() {
		return this.card.getATR();
	}

	/**
	 * Unblocking PIN using PUKs. This will choose the most secure method
	 * available to unblock a blocked PIN. If requireSecureReader is true, will
	 * throw SecurityException if an SPR is not available
	 * 
	 * @param requireSecureReader
	 * @throws Exception
	 */
	public void unblockPin(final boolean requireSecureReader) throws Exception {
		if (requireSecureReader
				&& (!getCCID().hasFeature(CCID.FEATURE.VERIFY_PIN_DIRECT))) {
			throw new SecurityException("not a secure reader");
		}

		ResponseAPDU responseApdu;
		int retriesLeft = -1;
		do {
			if (getCCID().hasFeature(CCID.FEATURE.VERIFY_PIN_DIRECT)) {
				this.logger.debug("could use direct PIN verify here...");
				responseApdu = unblockPINViaCCIDVerifyPINDirectOfPUK(retriesLeft);
			} else {
				responseApdu = unblockPINViaUI(retriesLeft);
			}

			if (0x9000 != responseApdu.getSW()) {
				this.logger.debug("PIN unblock error");
				this.logger.debug("SW: "
						+ Integer.toHexString(responseApdu.getSW()));
				if (0x6983 == responseApdu.getSW()) {
					getUI().advisePINBlocked();
					throw new ResponseAPDUException("eID card blocked!",
							responseApdu);
				}
				if (0x63 != responseApdu.getSW1()) {
					this.logger.debug("PIN unblock error.");
					throw new ResponseAPDUException("PIN unblock error",
							responseApdu);
				}
				retriesLeft = responseApdu.getSW2() & 0xf;
				this.logger.debug("retries left: " + retriesLeft);
			}
		} while (0x9000 != responseApdu.getSW());
		getUI().advisePINUnblocked();
	}

	// ===========================================================================================================
	// private implementations
	// ===========================================================================================================

	/*
	 * Verify PIN code for purpose "purpose" This method will attempt to verify
	 * PIN using the most secure method available. If that method turns out to
	 * be the UI, will pass purpose to the UI.
	 */

	private void verifyPin(final PINPurpose purpose) throws IOException,
			CardException, InterruptedException {
		ResponseAPDU responseApdu;
		int retriesLeft = -1;
		do {
			if (getCCID().hasFeature(CCID.FEATURE.VERIFY_PIN_DIRECT)) {
				responseApdu = verifyPINViaCCIDDirect(retriesLeft);
			} else if (getCCID().hasFeature(CCID.FEATURE.VERIFY_PIN_START)) {
				responseApdu = verifyPINViaCCIDStartFinish(retriesLeft);
			} else {
				responseApdu = verifyPINViaUI(retriesLeft, purpose);
			}

			if (0x9000 != responseApdu.getSW()) {
				this.logger.debug("VERIFY_PIN error");
				this.logger.debug("SW: "
						+ Integer.toHexString(responseApdu.getSW()));
				if (0x6983 == responseApdu.getSW()) {
					getUI().advisePINBlocked();
					throw new ResponseAPDUException("eID card blocked!",
							responseApdu);
				}
				if (0x63 != responseApdu.getSW1()) {
					this.logger.debug("PIN verification error.");
					throw new ResponseAPDUException("PIN Verification Error",
							responseApdu);
				}
				retriesLeft = responseApdu.getSW2() & 0xf;
				this.logger.debug("retries left: " + retriesLeft);
			}
		} while (0x9000 != responseApdu.getSW());
	}

	/*
	 * Verify PIN code using CCID Direct PIN Verify sequence.
	 */

	private ResponseAPDU verifyPINViaCCIDDirect(final int retriesLeft)
			throws IOException, CardException {
		this.logger.debug("direct PIN verification...");
		getUI().advisePINPadPINEntry(retriesLeft);
		byte[] result;
		try {
			result = transmitCCIDControl(
					CCID.FEATURE.VERIFY_PIN_DIRECT,
					getCCID().createPINVerificationDataStructure(getLocale(),
							CCID.INS.VERIFY_PIN));
		} finally {
			getUI().advisePINPadOperationEnd();
		}
		final ResponseAPDU responseApdu = new ResponseAPDU(result);
		if (0x6401 == responseApdu.getSW()) {
			this.logger.debug("canceled by user");
			final SecurityException securityException = new SecurityException(
					"canceled by user");
			securityException
					.initCause(new ResponseAPDUException(responseApdu));
			throw securityException;
		} else if (0x6400 == responseApdu.getSW()) {
			this.logger.debug("PIN pad timeout");
		}
		return responseApdu;
	}

	/*
	 * Verify PIN code using CCID Start/Finish sequence.
	 */

	private ResponseAPDU verifyPINViaCCIDStartFinish(final int retriesLeft)
			throws IOException, CardException, InterruptedException {
		this.logger.debug("CCID verify PIN start/end sequence...");

		getUI().advisePINPadPINEntry(retriesLeft);

		try {
			transmitCCIDControl(
					CCID.FEATURE.GET_KEY_PRESSED,
					getCCID().createPINVerificationDataStructure(getLocale(),
							CCID.INS.VERIFY_PIN));
			getCCID().waitForOK();
		} finally {
			getUI().advisePINPadOperationEnd();
		}

		return new ResponseAPDU(
				transmitCCIDControl(CCID.FEATURE.VERIFY_PIN_FINISH));
	}

	/*
	 * Verify PIN code by obtaining it from the current UI
	 */

	private ResponseAPDU verifyPINViaUI(final int retriesLeft,
			final PINPurpose purpose) throws CardException {
		final char[] pin = getUI().obtainPIN(retriesLeft, purpose);
		final byte[] verifyData = new byte[] { (byte) (0x20 | pin.length),
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
		for (int idx = 0; idx < pin.length; idx += 2) {
			final char digit1 = pin[idx];
			final char digit2;
			if (idx + 1 < pin.length) {
				digit2 = pin[idx + 1];
			} else {
				digit2 = '0' + 0xf;
			}
			final byte value = (byte) (byte) ((digit1 - '0' << 4) + (digit2 - '0'));
			verifyData[idx / 2 + 1] = value;
		}
		Arrays.fill(pin, (char) 0); // minimize exposure

		this.logger.debug("verifying PIN...");
		try {
			return transmitCommand(BeIDCommandAPDU.VERIFY_PIN, verifyData);
		} finally {
			Arrays.fill(verifyData, (byte) 0); // minimize exposure
		}
	}

	/*
	 * Modify PIN code using CCID Direct PIN Modify sequence.
	 */

	private ResponseAPDU changePINViaCCIDDirect(final int retriesLeft)
			throws IOException, CardException {
		this.logger.debug("direct PIN modification...");
		getUI().advisePINPadChangePIN(retriesLeft);
		byte[] result;

		try {
			result = transmitCCIDControl(
					CCID.FEATURE.MODIFY_PIN_DIRECT,
					getCCID().createPINModificationDataStructure(getLocale(),
							CCID.INS.MODIFY_PIN));
		} finally {
			getUI().advisePINPadOperationEnd();
		}

		final ResponseAPDU responseApdu = new ResponseAPDU(result);
		if (0x6402 == responseApdu.getSW()) {
			this.logger.debug("PINs differ");
		} else if (0x6401 == responseApdu.getSW()) {
			this.logger.debug("canceled by user");
			final SecurityException securityException = new SecurityException(
					"canceled by user");
			securityException
					.initCause(new ResponseAPDUException(responseApdu));
			throw securityException;
		} else if (0x6400 == responseApdu.getSW()) {
			this.logger.debug("PIN pad timeout");
		}

		return responseApdu;
	}

	/*
	 * Modify PIN code using CCID Modify PIN Start sequence
	 */

	private ResponseAPDU changePINViaCCIDStartFinish(final int retriesLeft)
			throws IOException, CardException, InterruptedException {
		transmitCCIDControl(
				CCID.FEATURE.MODIFY_PIN_START,
				getCCID().createPINModificationDataStructure(getLocale(),
						CCID.INS.MODIFY_PIN));

		try {
			this.logger.debug("enter old PIN...");
			getUI().advisePINPadOldPINEntry(retriesLeft);
			getCCID().waitForOK();
			getUI().advisePINPadOperationEnd();

			this.logger.debug("enter new PIN...");
			getUI().advisePINPadNewPINEntry(retriesLeft);
			getCCID().waitForOK();
			getUI().advisePINPadOperationEnd();

			this.logger.debug("enter new PIN again...");
			getUI().advisePINPadNewPINEntryAgain(retriesLeft);
			getCCID().waitForOK();
		} finally {
			getUI().advisePINPadOperationEnd();
		}

		return new ResponseAPDU(
				transmitCCIDControl(CCID.FEATURE.MODIFY_PIN_FINISH));
	}

	/*
	 * Modify PIN via the UI
	 */

	private ResponseAPDU changePINViaUI(final int retriesLeft)
			throws CardException {
		final char[][] pins = getUI().obtainOldAndNewPIN(retriesLeft);
		final char[] oldPin = pins[0];
		final char[] newPin = pins[1];

		final byte[] changePinData = new byte[] {
				(byte) (0x20 | oldPin.length), (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) (0x20 | newPin.length), (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF };

		for (int idx = 0; idx < oldPin.length; idx += 2) {
			final char digit1 = oldPin[idx];
			final char digit2;
			if (idx + 1 < oldPin.length) {
				digit2 = oldPin[idx + 1];
			} else {
				digit2 = '0' + 0xf;
			}
			final byte value = (byte) (byte) ((digit1 - '0' << 4) + (digit2 - '0'));
			changePinData[idx / 2 + 1] = value;
		}
		Arrays.fill(oldPin, (char) 0); // minimize exposure

		for (int idx = 0; idx < newPin.length; idx += 2) {
			final char digit1 = newPin[idx];
			final char digit2;
			if (idx + 1 < newPin.length) {
				digit2 = newPin[idx + 1];
			} else {
				digit2 = '0' + 0xf;
			}
			final byte value = (byte) (byte) ((digit1 - '0' << 4) + (digit2 - '0'));
			changePinData[(idx / 2 + 1) + 8] = value;
		}
		Arrays.fill(newPin, (char) 0); // minimize exposure

		try {
			return transmitCommand(BeIDCommandAPDU.CHANGE_PIN, changePinData);
		} finally {
			Arrays.fill(changePinData, (byte) 0);
		}
	}

	/*
	 * Unblock PIN using CCID Verify PIN Direct sequence on the PUK
	 */

	private ResponseAPDU unblockPINViaCCIDVerifyPINDirectOfPUK(
			final int retriesLeft) throws IOException, CardException {
		this.logger.debug("direct PUK verification...");
		getUI().advisePINPadPUKEntry(retriesLeft);
		byte[] result;
		try {
			result = transmitCCIDControl(
					CCID.FEATURE.VERIFY_PIN_DIRECT,
					getCCID().createPINVerificationDataStructure(getLocale(),
							CCID.INS.VERIFY_PUK));
		} finally {
			getUI().advisePINPadOperationEnd();
		}

		final ResponseAPDU responseApdu = new ResponseAPDU(result);
		if (0x6401 == responseApdu.getSW()) {
			this.logger.debug("canceled by user");
			final SecurityException securityException = new SecurityException(
					"canceled by user");
			securityException
					.initCause(new ResponseAPDUException(responseApdu));
			throw securityException;
		} else if (0x6400 == responseApdu.getSW()) {
			this.logger.debug("PIN pad timeout");
		}
		return responseApdu;
	}

	/*
	 * Unblock the PIN by obtaining PUK codes from the UI and calling RESET_PIN
	 * on the card.
	 */

	private ResponseAPDU unblockPINViaUI(final int retriesLeft)
			throws CardException {
		final char[][] puks = getUI().obtainPUKCodes(retriesLeft);
		final char[] puk1 = puks[0];
		final char[] puk2 = puks[1];

		final char[] fullPuk = new char[puks.length];
		System.arraycopy(puk2, 0, fullPuk, 0, puk2.length);
		Arrays.fill(puk2, (char) 0);
		System.arraycopy(puk1, 0, fullPuk, puk2.length, puk1.length);
		Arrays.fill(puk1, (char) 0);

		final byte[] unblockPinData = new byte[] {
				(byte) (0x20 | ((byte) (puks.length))), (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF };

		for (int idx = 0; idx < fullPuk.length; idx += 2) {
			final char digit1 = fullPuk[idx];
			final char digit2 = fullPuk[idx + 1];
			final byte value = (byte) (byte) ((digit1 - '0' << 4) + (digit2 - '0'));
			unblockPinData[idx / 2 + 1] = value;
		}
		Arrays.fill(fullPuk, (char) 0); // minimize exposure

		try {
			return transmitCommand(BeIDCommandAPDU.RESET_PIN, unblockPinData);
		} finally {
			Arrays.fill(unblockPinData, (byte) 0);
		}
	}

	public Locale getLocale() {
		if (this.locale != null) {
			return this.locale;
		}
		return Locale.getDefault();
	}

	public void setLocale(final Locale locale) {
		this.locale = locale;
	}

	public BeIDCard addCardListener(final BeIDCardListener beIDCardListener) {
		synchronized (this.cardListeners) {
			this.cardListeners.add(beIDCardListener);
		}

		return this;
	}

	public BeIDCard removeCardListener(final BeIDCardListener beIDCardListener) {
		synchronized (this.cardListeners) {
			this.cardListeners.remove(beIDCardListener);
		}

		return this;
	}

	public BeIDCard selectApplet() throws CardException {
		ResponseAPDU responseApdu;

		responseApdu = transmitCommand(BeIDCommandAPDU.SELECT_APPLET_0,
				BELPIC_AID);
		if (0x9000 != responseApdu.getSW()) {
			this.logger.error("error selecting BELPIC");
			this.logger.debug("status word: "
					+ Integer.toHexString(responseApdu.getSW()));
			/*
			 * Try to select the Applet.
			 */
			try {
				responseApdu = transmitCommand(BeIDCommandAPDU.SELECT_APPLET_1,
						APPLET_AID);
			} catch (final CardException e) {
				this.logger.error("error selecting Applet");
				return this;
			}
			if (0x9000 != responseApdu.getSW()) {
				this.logger.error("could not select applet");
			} else {
				this.logger
						.debug("BELPIC JavaCard applet selected by APPLET_AID");
			}
		} else {
			this.logger.debug("BELPIC JavaCard applet selected by BELPIC_AID");
		}

		return this;
	}

	// --------------------------------------------------------------------------------------------------------------------------------

	public BeIDCard beginExclusive() throws CardException {
		this.logger.debug("---begin exclusive---");
		this.card.beginExclusive();
		return this;
	}

	public BeIDCard endExclusive() throws CardException {
		this.logger.debug("---end exclusive---");
		this.card.endExclusive();
		return this;
	}

	// --------------------------------------------------------------------------------------------------------------------------------

	public byte[] readBinary(final FileType fileType, final int estimatedMaxSize)
			throws CardException, IOException, InterruptedException {
		int offset = 0;
		this.logger.debug("read binary");
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] data;
		do {
			if (Thread.currentThread().isInterrupted()) {
				this.logger.debug("interrupted in readBinary");
				throw new InterruptedException();
			}

			notifyReadProgress(fileType, offset, estimatedMaxSize);
			final ResponseAPDU responseApdu = transmitCommand(
					BeIDCommandAPDU.READ_BINARY, offset >> 8, offset & 0xFF,
					BLOCK_SIZE);
			final int sw = responseApdu.getSW();
			if (0x6B00 == sw) {
				/*
				 * Wrong parameters (offset outside the EF) End of file reached.
				 * Can happen in case the file size is a multiple of 0xff bytes.
				 */
				break;
			}

			if (0x9000 != sw) {
				final IOException ioEx = new IOException(
						"BeIDCommandAPDU response error: "
								+ responseApdu.getSW());
				ioEx.initCause(new ResponseAPDUException(responseApdu));
				throw ioEx;
			}

			data = responseApdu.getData();
			baos.write(data);
			offset += data.length;
		} while (BLOCK_SIZE == data.length);
		notifyReadProgress(fileType, offset, offset);
		return baos.toByteArray();
	}

	public BeIDCard selectFile(final byte[] fileId) throws CardException,
			FileNotFoundException {
		this.logger.debug("selecting file");
		final ResponseAPDU responseApdu = transmitCommand(
				BeIDCommandAPDU.SELECT_FILE, fileId);
		if (0x9000 != responseApdu.getSW()) {
			final FileNotFoundException fnfEx = new FileNotFoundException(
					"wrong status word after selecting file: "
							+ Integer.toHexString(responseApdu.getSW()));
			fnfEx.initCause(new ResponseAPDUException(responseApdu));
			throw fnfEx;
		}

		try {
			// SCARD_E_SHARING_VIOLATION fix
			Thread.sleep(20);
		} catch (final InterruptedException e) {
			throw new RuntimeException("sleep error: " + e.getMessage());
		}

		return this;
	}

	public byte[] readFile(final FileType fileType) throws CardException,
			IOException, InterruptedException {
		this.beginExclusive();

		try {
			this.selectFile(fileType.getFileId());
			return this.readBinary(fileType, fileType.getEstimatedMaxSize());
		} finally {
			this.endExclusive();
		}
	}

	public BeIDCard close() {
		this.logger.debug("closing eID card");

		try {
			this.card.disconnect(true);
		} catch (final CardException e) {
			this.logger.error("could not disconnect the card: "
					+ e.getMessage());
		}

		return this;
	}

	public void logoff() throws Exception {
		final CommandAPDU logoffApdu = new CommandAPDU(0x80, 0xE6, 0x00, 0x00);
		this.logger.debug("logoff...");
		final ResponseAPDU responseApdu = transmit(logoffApdu);
		if (0x9000 != responseApdu.getSW()) {
			throw new RuntimeException("logoff failed");
		}
	}

	// ----------------------------------------------------------------------------------------------------------------------------------

	protected byte[] transmitCCIDControl(final CCID.FEATURE feature)
			throws CardException {
		return transmitControlCommand(getCCID().getFeature(feature),
				new byte[0]);
	}

	protected byte[] transmitCCIDControl(final CCID.FEATURE feature,
			final byte[] command) throws CardException {
		return transmitControlCommand(getCCID().getFeature(feature), command);
	}

	protected byte[] transmitControlCommand(final int controlCode,
			final byte[] command) throws CardException {
		return this.card.transmitControlCommand(controlCode, command);
	}

	protected ResponseAPDU transmitCommand(final BeIDCommandAPDU apdu,
			final int p1, final int p2, final int le) throws CardException {
		return transmit(new CommandAPDU(apdu.getCla(), apdu.getIns(), p1, p2,
				le));
	}

	protected ResponseAPDU transmitCommand(final BeIDCommandAPDU apdu,
			final byte[] data) throws CardException {
		return transmit(new CommandAPDU(apdu.getCla(), apdu.getIns(),
				apdu.getP1(), apdu.getP2(), data));
	}

	protected ResponseAPDU transmitCommand(final BeIDCommandAPDU apdu,
			final byte[] data, final int dataOffset, final int dataLength,
			final int ne) throws CardException {
		return transmit(new CommandAPDU(apdu.getCla(), apdu.getIns(),
				apdu.getP1(), apdu.getP2(), data, dataOffset, dataLength, ne));
	}

	private ResponseAPDU transmit(final CommandAPDU commandApdu)
			throws CardException {
		ResponseAPDU responseApdu = this.cardChannel.transmit(commandApdu);
		if (0x6c == responseApdu.getSW1()) {
			/*
			 * A minimum delay of 10 msec between the answer ?????????6C
			 * xx????????? and the next BeIDCommandAPDU is mandatory for eID
			 * v1.0 and v1.1 cards.
			 */
			this.logger.debug("sleeping...");
			try {
				Thread.sleep(10);
			} catch (final InterruptedException e) {
				throw new RuntimeException("cannot sleep");
			}
			responseApdu = this.cardChannel.transmit(commandApdu);
		}
		return responseApdu;
	}

	// ----------------------------------------------------------------------------------------------------------------------------------

	private void notifyReadProgress(final FileType fileType, final int offset,
			int estimatedMaxOffset) {
		if (offset > estimatedMaxOffset) {
			estimatedMaxOffset = offset;
		}

		synchronized (this.cardListeners) {
			for (BeIDCardListener listener : this.cardListeners) {
				try {
					listener.notifyReadProgress(fileType, offset,
							estimatedMaxOffset);
				} catch (final Exception ex) {
					this.logger
							.debug("Exception Thrown In BeIDCardListener.notifyReadProgress():"
									+ ex.getMessage());
				}
			}
		}
	}

	private void notifySigningBegin(final FileType keyType) {
		synchronized (this.cardListeners) {
			for (BeIDCardListener listener : this.cardListeners) {
				try {
					listener.notifySigningBegin(keyType);
				} catch (final Exception ex) {
					this.logger
							.debug("Exception Thrown In BeIDCardListener.notifySigningBegin():"
									+ ex.getMessage());
				}
			}
		}
	}

	private void notifySigningEnd(final FileType keyType) {
		synchronized (this.cardListeners) {
			for (BeIDCardListener listener : this.cardListeners) {
				try {
					listener.notifySigningEnd(keyType);
				} catch (final Exception ex) {
					this.logger
							.debug("Exception Thrown In BeIDCardListener.notifySigningBegin():"
									+ ex.getMessage());
				}
			}
		}
	}

	// ----------------------------------------------------------------------------------------------------------------------------------

	private CCID getCCID() {
		if (this.ccid == null) {
			this.ccid = new CCID(this.card);
		}
		return this.ccid;
	}

	private BeIDCardUI getUI() {
		if (this.ui == null) {
			if (GraphicsEnvironment.isHeadless()) {
				this.logger.error(UI_DEFAULT_REQUIRES_HEAD);
				throw new UnsupportedOperationException(
						UI_DEFAULT_REQUIRES_HEAD);
			}

			try {
				final ClassLoader classLoader = BeIDCard.class.getClassLoader();
				final Class<?> uiClass = classLoader
						.loadClass(DEFAULT_UI_IMPLEMENTATION);
				this.ui = (BeIDCardUI) uiClass.newInstance();
			} catch (final Exception e) {
				this.logger.error(UI_MISSING_LOG_MESSAGE);
				throw new UnsupportedOperationException(UI_MISSING_LOG_MESSAGE,
						e);
			}
		}

		return this.ui;
	}

	public void setUI(final BeIDCardUI userInterface) {
		this.ui = userInterface;
	}

	/*
	 * BeIDCommandAPDU encapsulates values sent in CommandAPDU's, to make these
	 * more readable in BeIDCard.
	 */

	private enum BeIDCommandAPDU {
		SELECT_APPLET_0(0x00, 0xA4, 0x04, 0x0C),

		SELECT_APPLET_1(0x00, 0xA4, 0x04, 0x0C),

		SELECT_FILE(0x00, 0xA4, 0x08, 0x0C),

		READ_BINARY(0x00, 0xB0),

		VERIFY_PIN(0x00, 0x20, 0x00, 0x01),

		CHANGE_PIN(0x00, 0x24, 0x00, 0x01), // 0x0024=change
		// reference
		// change
		SELECT_ALGORITHM_AND_PRIVATE_KEY(0x00, 0x22, 0x41, 0xB6), // ISO 7816-8
		// SET
		// COMMAND
		// (select
		// algorithm and
		// key for
		// signature)

		COMPUTE_DIGITAL_SIGNATURE(0x00, 0x2A, 0x9E, 0x9A), // ISO 7816-8 COMPUTE
		// DIGITAL SIGNATURE
		// COMMAND
		RESET_PIN(0x00, 0x2C, 0x00, 0x01),

		GET_CHALLENGE(0x00, 0x84, 0x00, 0x00);

		private final int cla;
		private final int ins;
		private final int p1;
		private final int p2;

		private BeIDCommandAPDU(final int cla, final int ins, final int p1,
				final int p2) {
			this.cla = cla;
			this.ins = ins;
			this.p1 = p1;
			this.p2 = p2;
		}

		private BeIDCommandAPDU(final int cla, final int ins) {
			this.cla = cla;
			this.ins = ins;
			this.p1 = -1;
			this.p2 = -1;
		}

		public int getCla() {
			return this.cla;
		}

		public int getIns() {
			return this.ins;
		}

		public int getP1() {
			return this.p1;
		}

		public int getP2() {
			return this.p2;
		}
	}
}
