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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.ResponseAPDU;

import be.fedict.commons.eid.client.spi.Dialogs;
import be.fedict.commons.eid.client.spi.Logger;

public class BeIDCard extends BELPICCard {
	private Dialogs dialogs;
	private Locale locale;

	public BeIDCard(Card card, Logger logger) {
		super(card, logger);
	}

	public BeIDCard(Card card) {
		super(card);
	}

	public BeIDCard(CardTerminal cardTerminal, Logger logger)
			throws CardException {
		super(cardTerminal, logger);
	}

	public BeIDCard(CardTerminal cardTerminal) throws CardException {
		super(cardTerminal);
	}

	/*
	 * Getting Certificates (by BeIDFileType)
	 */

	public X509Certificate getCertificate(BeIDFileType fileType)
			throws CertificateException, CardException, IOException {
		CertificateFactory certificateFactory = CertificateFactory
				.getInstance("X.509");
		return (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(
						readFile(fileType)));
	}

	/*
	 * Getting Certificates(convenience methods)
	 */

	public X509Certificate getAuthenticationCertificate() throws CardException,
			IOException, CertificateException {
		return getCertificate(BeIDFileType.AuthentificationCertificate);
	}

	public X509Certificate getSigningCertificate() throws CardException,
			IOException, CertificateException {
		return getCertificate(BeIDFileType.SigningCertificate);
	}

	public X509Certificate getCACertificate() throws CardException,
			IOException, CertificateException {
		return getCertificate(BeIDFileType.CACertificate);
	}

	public X509Certificate getRRNCertificate() throws CardException,
			IOException, CertificateException {
		return getCertificate(BeIDFileType.RRNCertificate);
	}

	/*
	 * Getting Certificate Chains(by BeIDFileType of Leaf Certificate)
	 */

	public List<X509Certificate> getCertificateChain(BeIDFileType fileType)
			throws CertificateException, CardException, IOException {
		CertificateFactory certificateFactory = CertificateFactory
				.getInstance("X.509");
		List<X509Certificate> chain = new LinkedList<X509Certificate>();
		chain.add((X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(
						readFile(fileType))));
		if (fileType.chainIncludesCitizenCA())
			chain.add((X509Certificate) certificateFactory
					.generateCertificate(new ByteArrayInputStream(
							readFile(BeIDFileType.CACertificate))));
		chain.add((X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(
						readFile(BeIDFileType.RootCertificate))));
		return chain;
	}

	/*
	 * Getting Certificate Chains (convenience methods)
	 */

	public List<X509Certificate> getAuthenticationCertificateChain()
			throws CardException, IOException, CertificateException {
		return getCertificateChain(BeIDFileType.AuthentificationCertificate);
	}

	public List<X509Certificate> getSigningCertificateChain()
			throws CardException, IOException, CertificateException {
		return getCertificateChain(BeIDFileType.SigningCertificate);
	}

	public List<X509Certificate> getCACertificateChain() throws CardException,
			IOException, CertificateException {
		return getCertificateChain(BeIDFileType.CACertificate);
	}

	public List<X509Certificate> getRRNCertificateChain() throws CardException,
			IOException, CertificateException {
		return getCertificateChain(BeIDFileType.RRNCertificate);
	}

	/*
	 * Signing data
	 */

	public byte[] sign(byte[] digestValue, String digestAlgo, byte keyId,
			boolean requireSecureReader) throws CardException, IOException,
			InterruptedException {
		Integer directPinVerifyFeature = CCID.getFeature(getCard(),
				CCID.FEATURE_VERIFY_PIN_DIRECT_TAG);
		Integer verifyPinStartFeature = CCID.getFeature(getCard(),
				CCID.FEATURE_VERIFY_PIN_START_TAG);
		Integer eIDPINPadReaderFeature = CCID.getFeature(getCard(),
				CCID.FEATURE_EID_PIN_PAD_READER_TAG);

		if (null != eIDPINPadReaderFeature) {
			getLogger().debug("eID-aware secure PIN pad reader detected");
		}

		if (requireSecureReader && null == directPinVerifyFeature
				&& null == verifyPinStartFeature) {
			throw new SecurityException("not a secure reader");
		}

		Digest digest = Digest.byName(digestAlgo);

		// select the key
		getLogger().debug("selecting key...");

		ResponseAPDU responseApdu = transmitCommand(
				BeIDCommandAPDU.SELECT_ALGORITHM_AND_PRIVATE_KEY, new byte[]{
						(byte) 0x04, // length
						// of
						// following
						// data
						(byte) 0x80, digest.getAlgorithmReference(), // algorithm reference
						(byte) 0x84, keyId}); // private key reference

		if (0x9000 != responseApdu.getSW()) {
			throw new ResponseAPDUException(
					"SET (select algorithm and private key) error",
					responseApdu);
		}

		if (BeIDFileType.SigningCertificate.getKeyId() == keyId) {
			getLogger().debug(
					"non-repudiation key detected, immediate PIN verify");
			verifyPin(directPinVerifyFeature, verifyPinStartFeature);
		}

		ByteArrayOutputStream digestInfo = new ByteArrayOutputStream();
		digestInfo.write(digest.getPrefix(digestValue.length));
		digestInfo.write(digestValue);

		getLogger().debug("computing digital signature...");
		responseApdu = transmitCommand(
				BeIDCommandAPDU.COMPUTE_DIGITAL_SIGNATURE, digestInfo
						.toByteArray());
		if (0x9000 == responseApdu.getSW()) {
			/*
			 * OK, we could use the card PIN caching feature.
			 * 
			 * Notice that the card PIN caching also works when first doing an
			 * authentication after a non-repudiation signature.
			 */
			return responseApdu.getData();
		}
		if (0x6982 != responseApdu.getSW()) {
			getLogger().debug(
					"SW: " + Integer.toHexString(responseApdu.getSW()));
			throw new ResponseAPDUException("compute digital signature error",
					responseApdu);
		}
		/*
		 * 0x6982 = Security status not satisfied, so we do a PIN verification
		 * before retrying.
		 */
		getLogger().debug("PIN verification required...");
		verifyPin(directPinVerifyFeature, verifyPinStartFeature);

		getLogger()
				.debug(
						"computing digital signature (attempt #2 after PIN verification)...");
		responseApdu = transmitCommand(
				BeIDCommandAPDU.COMPUTE_DIGITAL_SIGNATURE, digestInfo
						.toByteArray());
		if (0x9000 != responseApdu.getSW()) {
			throw new ResponseAPDUException("compute digital signature error",
					responseApdu);
		}

		return responseApdu.getData();
	}

	/*
	 * sign SHA-1 With Authentication key convenience method
	 */

	public byte[] signAuthn(byte[] toBeSigned, boolean requireSecureReader)
			throws NoSuchAlgorithmException, CardException, IOException,
			InterruptedException {
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
		byte[] digest = messageDigest.digest(toBeSigned);
		return sign(digest, "SHA-1", BeIDFileType.AuthentificationCertificate
				.getKeyId(), requireSecureReader);
	}

	/*
	 * Verifying PIN Code
	 */

	public void verifyPin() throws IOException, CardException,
			InterruptedException {
		Integer directPinVerifyFeature = CCID.getFeature(getCard(),
				CCID.FEATURE_VERIFY_PIN_DIRECT_TAG);
		Integer verifyPinStartFeature = CCID.getFeature(getCard(),
				CCID.FEATURE_VERIFY_PIN_START_TAG);
		verifyPin(directPinVerifyFeature, verifyPinStartFeature);
	}

	/*
	 * Changing PIN Code
	 */

	public void changePin(boolean requireSecureReader) throws Exception {
		Integer directPinModifyFeature = CCID.getFeature(getCard(),
				CCID.FEATURE_MODIFY_PIN_DIRECT_TAG);
		Integer modifyPinStartFeature = CCID.getFeature(getCard(),
				CCID.FEATURE_MODIFY_PIN_START_TAG);

		if (requireSecureReader && null == directPinModifyFeature
				&& null == modifyPinStartFeature) {
			throw new SecurityException("not a secure reader");
		}

		int retriesLeft = -1;
		ResponseAPDU responseApdu;
		do {
			if (null != modifyPinStartFeature) {
				getLogger().debug("using modify pin start/finish...");
				responseApdu = doChangePinStartFinish(retriesLeft,
						modifyPinStartFeature);
			} else if (null != directPinModifyFeature) {
				getLogger().debug("could use direct PIN modify here...");
				responseApdu = doChangePinDirect(retriesLeft,
						directPinModifyFeature);
			} else {
				responseApdu = doChangePin(retriesLeft);
			}

			if (0x9000 != responseApdu.getSW()) {
				getLogger().debug("CHANGE PIN error");
				getLogger().debug(
						"SW: " + Integer.toHexString(responseApdu.getSW()));
				if (0x6983 == responseApdu.getSW()) {
					this.dialogs.advisePINBlocked();
					throw new ResponseAPDUException("eID card blocked!",
							responseApdu);
				}
				if (0x63 != responseApdu.getSW1()) {
					getLogger().debug("PIN change error. Card blocked?");
					throw new ResponseAPDUException("PIN Change Error",
							responseApdu);
				}
				retriesLeft = responseApdu.getSW2() & 0xf;
				getLogger().debug("retries left: " + retriesLeft);
			}
		} while (0x9000 != responseApdu.getSW());
		this.dialogs.advisePINChanged();
	}

	/*
	 * various techniques and modes of verifying PIN codes. Called by
	 * verifyPin() as appropriate
	 */

	private void verifyPin(Integer directPinVerifyFeature,
			Integer verifyPinStartFeature) throws IOException, CardException,
			InterruptedException {
		ResponseAPDU responseApdu;
		int retriesLeft = -1;
		do {
			if (null != directPinVerifyFeature) {
				responseApdu = verifyPinDirect(retriesLeft,
						directPinVerifyFeature);
			} else if (null != verifyPinStartFeature) {
				responseApdu = verifyPin(retriesLeft, verifyPinStartFeature);
			} else {
				responseApdu = verifyPin(retriesLeft);
			}
			if (0x9000 != responseApdu.getSW()) {
				getLogger().debug("VERIFY_PIN error");
				getLogger().debug(
						"SW: " + Integer.toHexString(responseApdu.getSW()));
				if (0x6983 == responseApdu.getSW()) {
					this.dialogs.advisePINBlocked();
					throw new ResponseAPDUException("eID card blocked!",
							responseApdu);
				}
				if (0x63 != responseApdu.getSW1()) {
					getLogger().debug("PIN verification error.");
					throw new ResponseAPDUException("PIN Verification Error",
							responseApdu);
				}
				retriesLeft = responseApdu.getSW2() & 0xf;
				getLogger().debug("retries left: " + retriesLeft);
			}
		} while (0x9000 != responseApdu.getSW());
	}

	// ---------------------------------------------------------------------------------------------------------------------------------

	private ResponseAPDU verifyPin(int retriesLeft,
			Integer verifyPinStartFeature) throws IOException, CardException,
			InterruptedException {
		getLogger().debug("CCID verify PIN start/end sequence...");
		byte[] verifyCommandData = CCID.createPINVerificationDataStructure(
				getLocale(), 0x20);
		this.dialogs.advisePINPadPINEntry(retriesLeft);
		try {
			int getKeyPressedFeature = CCID.getFeature(getCard(),
					CCID.FEATURE_GET_KEY_PRESSED_TAG);
			transmitControlCommand(verifyPinStartFeature, verifyCommandData);
			CCID.waitForOK(getCard(), getKeyPressedFeature);
		} finally {
			this.dialogs.advisePINPadOperationEnd();
		}
		int verifyPinFinishIoctl = CCID.getFeature(getCard(),
				CCID.FEATURE_VERIFY_PIN_FINISH_TAG);
		byte[] verifyPinFinishResult = transmitControlCommand(
				verifyPinFinishIoctl, new byte[0]);
		ResponseAPDU responseApdu = new ResponseAPDU(verifyPinFinishResult);
		return responseApdu;
	}

	// ----------------------------------------------------------------------------------------------------------------------------------

	private ResponseAPDU verifyPinDirect(int retriesLeft,
			Integer directPinVerifyFeature) throws IOException, CardException {
		getLogger().debug("direct PIN verification...");
		byte[] verifyCommandData = CCID.createPINVerificationDataStructure(
				getLocale(), 0x20);
		this.dialogs.advisePINPadPINEntry(retriesLeft);
		byte[] result;
		try {
			result = transmitControlCommand(directPinVerifyFeature,
					verifyCommandData);
		} finally {
			this.dialogs.advisePINPadOperationEnd();
		}
		ResponseAPDU responseApdu = new ResponseAPDU(result);
		if (0x6401 == responseApdu.getSW()) {
			getLogger().debug("canceled by user");
			SecurityException securityException = new SecurityException(
					"canceled by user");
			securityException
					.initCause(new ResponseAPDUException(responseApdu));
			throw securityException;
		} else if (0x6400 == responseApdu.getSW()) {
			getLogger().debug("PIN pad timeout");
		}
		return responseApdu;
	}

	private ResponseAPDU verifyPin(int retriesLeft) throws CardException {
		char[] pin = this.dialogs.obtainPIN(retriesLeft);
		byte[] verifyData = new byte[]{(byte) (0x20 | pin.length), (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF};
		for (int idx = 0; idx < pin.length; idx += 2) {
			char digit1 = pin[idx];
			char digit2;
			if (idx + 1 < pin.length) {
				digit2 = pin[idx + 1];
			} else {
				digit2 = '0' + 0xf;
			}
			byte value = (byte) (byte) ((digit1 - '0' << 4) + (digit2 - '0'));
			verifyData[idx / 2 + 1] = value;
		}
		Arrays.fill(pin, (char) 0); // minimize exposure

		getLogger().debug("verifying PIN...");
		try {
			return transmitCommand(BeIDCommandAPDU.VERIFY_PIN, verifyData);
		} finally {
			Arrays.fill(verifyData, (byte) 0); // minimize exposure
		}
	}

	/*
	 * various techniques and modes of changing PIN codes. Called by
	 * changePin(boolean requireSecureReader) as appropriate
	 */

	private ResponseAPDU doChangePinStartFinish(int retriesLeft,
			Integer modifyPinStartFeature) throws IOException, CardException,
			InterruptedException {
		byte[] modifyCommandData = CCID.createPINModificationDataStructure(
				getLocale(), 0x24);
		transmitControlCommand(modifyPinStartFeature, modifyCommandData);
		int getKeyPressedFeature = CCID.getFeature(getCard(),
				CCID.FEATURE_GET_KEY_PRESSED_TAG);

		try {
			getLogger().debug("enter old PIN...");
			this.dialogs.advisePINPadOldPINEntry(retriesLeft);
			CCID.waitForOK(getCard(), getKeyPressedFeature);
			this.dialogs.advisePINPadOperationEnd();

			this.dialogs.advisePINPadNewPINEntry(retriesLeft);
			getLogger().debug("enter new PIN...");
			CCID.waitForOK(getCard(), getKeyPressedFeature);
			this.dialogs.advisePINPadOperationEnd();

			this.dialogs.advisePINPadNewPINEntryAgain(retriesLeft);
			getLogger().debug("enter new PIN again...");
			CCID.waitForOK(getCard(), getKeyPressedFeature);
		} finally {
			this.dialogs.advisePINPadOperationEnd();
		}

		int modifyPinFinishIoctl = CCID.getFeature(getCard(),
				CCID.FEATURE_MODIFY_PIN_FINISH_TAG);
		byte[] modifyPinFinishResult = transmitControlCommand(
				modifyPinFinishIoctl, new byte[0]);
		return new ResponseAPDU(modifyPinFinishResult);
	}

	private ResponseAPDU doChangePinDirect(int retriesLeft,
			Integer directPinModifyFeature) throws IOException, CardException {
		getLogger().debug("direct PIN modification...");
		byte[] modifyCommandData = CCID.createPINModificationDataStructure(
				getLocale(), 0x24);
		this.dialogs.advisePINPadChangePIN(retriesLeft);
		byte[] result;
		try {
			result = transmitControlCommand(directPinModifyFeature,
					modifyCommandData);
		} finally {
			this.dialogs.advisePINPadOperationEnd();
		}
		ResponseAPDU responseApdu = new ResponseAPDU(result);
		if (0x6402 == responseApdu.getSW()) {
			getLogger().debug("PINs differ");
		} else if (0x6401 == responseApdu.getSW()) {
			getLogger().debug("canceled by user");
			SecurityException securityException = new SecurityException(
					"canceled by user");
			securityException
					.initCause(new ResponseAPDUException(responseApdu));
			throw securityException;
		} else if (0x6400 == responseApdu.getSW()) {
			getLogger().debug("PIN pad timeout");
		}
		return responseApdu;
	}

	private ResponseAPDU doChangePin(int retriesLeft) throws CardException {
		char[][] pins = this.dialogs.obtainOldAndNewPIN(retriesLeft);
		char[] oldPin = pins[0];
		char[] newPin = pins[1];

		byte[] changePinData = new byte[]{(byte) (0x20 | oldPin.length),
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) (0x20 | newPin.length), (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

		for (int idx = 0; idx < oldPin.length; idx += 2) {
			char digit1 = oldPin[idx];
			char digit2;
			if (idx + 1 < oldPin.length) {
				digit2 = oldPin[idx + 1];
			} else {
				digit2 = '0' + 0xf;
			}
			byte value = (byte) (byte) ((digit1 - '0' << 4) + (digit2 - '0'));
			changePinData[idx / 2 + 1] = value;
		}
		Arrays.fill(oldPin, (char) 0); // minimize exposure

		for (int idx = 0; idx < newPin.length; idx += 2) {
			char digit1 = newPin[idx];
			char digit2;
			if (idx + 1 < newPin.length) {
				digit2 = newPin[idx + 1];
			} else {
				digit2 = '0' + 0xf;
			}
			byte value = (byte) (byte) ((digit1 - '0' << 4) + (digit2 - '0'));
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
	 * Unblocking PIN using PUKs
	 */

	public void unblockPin(boolean requireSecureReader) throws Exception {
		Integer directPinVerifyFeature = CCID.getFeature(getCard(),
				CCID.FEATURE_VERIFY_PIN_DIRECT_TAG);

		if (requireSecureReader && null == directPinVerifyFeature) {
			throw new SecurityException("not a secure reader");
		}

		ResponseAPDU responseApdu;
		int retriesLeft = -1;
		do {
			if (null != directPinVerifyFeature) {
				getLogger().debug("could use direct PIN verify here...");
				responseApdu = verifyPukDirect(retriesLeft,
						directPinVerifyFeature);
			} else {
				responseApdu = doUnblockPin(retriesLeft);
			}

			if (0x9000 != responseApdu.getSW()) {
				getLogger().debug("PIN unblock error");
				getLogger().debug(
						"SW: " + Integer.toHexString(responseApdu.getSW()));
				if (0x6983 == responseApdu.getSW()) {
					this.dialogs.advisePINBlocked();
					throw new ResponseAPDUException("eID card blocked!",
							responseApdu);
				}
				if (0x63 != responseApdu.getSW1()) {
					getLogger().debug("PIN unblock error.");
					throw new ResponseAPDUException("PIN unblock error",
							responseApdu);
				}
				retriesLeft = responseApdu.getSW2() & 0xf;
				getLogger().debug("retries left: " + retriesLeft);
			}
		} while (0x9000 != responseApdu.getSW());
		this.dialogs.advisePINUnblocked();
	}

	private ResponseAPDU doUnblockPin(int retriesLeft) throws CardException {
		char[][] puks = this.dialogs.obtainPUKCodes(retriesLeft);
		char[] puk1 = puks[0];
		char[] puk2 = puks[1];

		char[] fullPuk = new char[puks.length];
		System.arraycopy(puk2, 0, fullPuk, 0, puk2.length);
		Arrays.fill(puk2, (char) 0);
		System.arraycopy(puk1, 0, fullPuk, puk2.length, puk1.length);
		Arrays.fill(puk1, (char) 0);

		byte[] unblockPinData = new byte[]{
				(byte) (0x20 | ((byte) (puks.length))), (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF};

		for (int idx = 0; idx < fullPuk.length; idx += 2) {
			char digit1 = fullPuk[idx];
			char digit2 = fullPuk[idx + 1];
			byte value = (byte) (byte) ((digit1 - '0' << 4) + (digit2 - '0'));
			unblockPinData[idx / 2 + 1] = value;
		}
		Arrays.fill(fullPuk, (char) 0); // minimize exposure

		try {
			return transmitCommand(BeIDCommandAPDU.RESET_PIN, unblockPinData);
		} finally {
			Arrays.fill(unblockPinData, (byte) 0);
		}
	}

	private ResponseAPDU verifyPukDirect(int retriesLeft,
			Integer directPinVerifyFeature) throws IOException, CardException {
		getLogger().debug("direct PUK verification...");
		byte[] verifyCommandData = CCID.createPINVerificationDataStructure(
				getLocale(), 0x2C);
		this.dialogs.advisePINPadPUKEntry(retriesLeft);
		byte[] result;
		try {
			result = transmitControlCommand(directPinVerifyFeature,
					verifyCommandData);
		} finally {
			this.dialogs.advisePINPadOperationEnd();
		}
		ResponseAPDU responseApdu = new ResponseAPDU(result);
		if (0x6401 == responseApdu.getSW()) {
			getLogger().debug("canceled by user");
			SecurityException securityException = new SecurityException(
					"canceled by user");
			securityException
					.initCause(new ResponseAPDUException(responseApdu));
			throw securityException;
		} else if (0x6400 == responseApdu.getSW()) {
			getLogger().debug("PIN pad timeout");
		}
		return responseApdu;
	}

	public Locale getLocale() {
		if (this.locale != null)
			return this.locale;
		return Locale.getDefault();
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}
}
