/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2014 e-Contract.be BVBA.
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

package be.fedict.commons.eid.client.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import be.fedict.commons.eid.client.spi.Logger;

/**
 * CCID I/O according to the USB Smart card CCID 1.1 specifications. FrankM
 * added PPDU support
 * 
 * @author Frank Cornelis
 * @author Frank Marien
 * 
 */
public class CCID {
	public static final int GET_FEATURES = 0x42000D48;
	public static final int GET_FEATURES_MICROSOFT = 0x31 << 16 | 3400 << 2;
	public static final int MIN_PIN_SIZE = 4;
	public static final int MAX_PIN_SIZE = 12;

	public static final String DUTCH_LANGUAGE = "nl";
	public static final String FRENCH_LANGUAGE = Locale.FRENCH.getLanguage();
	public static final String GERMAN_LANGUAGE = Locale.GERMAN.getLanguage();
	public static final int DUTCH_LANGUAGE_CODE = 0x13;
	public static final int FRENCH_LANGUAGE_CODE = 0x0c;
	public static final int GERMAN_LANGUAGE_CODE = 0x07;
	public static final int ENGLISH_LANGUAGE_CODE = 0x09;

	public static boolean riskPPDU = false;
	public static Set<String> ppduExceptions = null;

	private final Logger logger;
	private final Card card;
	private final EnumMap<FEATURE, Integer> features;
	private boolean usesPPDU;

	public enum FEATURE {
		VERIFY_PIN_START(0x01), VERIFY_PIN_FINISH(0x02), VERIFY_PIN_DIRECT(0x06), MODIFY_PIN_START(
				0x03), MODIFY_PIN_FINISH(0x04), MODIFY_PIN_DIRECT(0x07), GET_KEY_PRESSED(
				0x05), EID_PIN_PAD_READER(0x80);

		private final byte tag;

		FEATURE(final int tag) {
			this.tag = (byte) tag;
		}

		public byte getTag() {
			return this.tag;
		}
	}

	public enum INS {
		VERIFY_PIN(0x20), MODIFY_PIN(0x24), VERIFY_PUK(0x2C);

		private final int ins;

		INS(final int ins) {
			this.ins = ins;
		}

		int getIns() {
			return this.ins;
		}
	}

	/*
	 * **********************************************************************************************************
	 */

	public static void riskPPDU(boolean riskPPDU) {
		CCID.riskPPDU = riskPPDU;
	}

	public static void setPPDUExceptions(Collection<String> ppduExceptions) {
		CCID.ppduExceptions = new HashSet<String>(ppduExceptions);
	}

	public static void addPPDUException(String terminalName) {
		if (CCID.ppduExceptions == null)
			CCID.ppduExceptions = new HashSet<String>();
		CCID.ppduExceptions.add(terminalName);
	}

	public static void removePPDUException(String terminalName) {
		if (CCID.ppduExceptions == null)
			return;
		CCID.ppduExceptions.remove(terminalName);
	}

	private static boolean riskPPDUForCardTerminal(String name) {
		if (CCID.ppduExceptions == null)
			return CCID.riskPPDU;
		return CCID.riskPPDU
				? (!CCID.ppduExceptions.contains(name))
				: (CCID.ppduExceptions.contains(name));
	}

	public boolean usesPPDU() {
		return usesPPDU;
	}

	/*
	 * **********************************************************************************************************
	 */

	public CCID(final Card card, final CardTerminal cardTerminal,
			final Logger logger) {
		this.card = card;
		this.logger = logger;
		this.features = new EnumMap<FEATURE, Integer>(FEATURE.class);
		this.usesPPDU = false;

		final boolean onMSWindows = (System.getProperty("os.name") != null && System
				.getProperty("os.name").startsWith("Windows"));

		this.logger
				.debug("Getting CCID FEATURES using standard control command");
		try {
			getFeaturesUsingControlChannel(card, onMSWindows);
		} catch (final CardException cexInNormal) {
			this.logger
					.debug("GET_FEATURES over standard control command failed: "
							+ cexInNormal.getMessage());
			if (onMSWindows && riskPPDUForCardTerminal(cardTerminal.getName())) {
				this.logger
						.debug("Attempting To get CCID FEATURES using Pseudo-APDU Fallback Strategy");
				try {
					getFeaturesUsingPPDU(card);
				} catch (CardException cexInPseudo) {
					this.logger
							.error("Pseudo-APDU Fallback strategy failed as well: "
									+ cexInPseudo.getMessage());
				}
			} else {
				this.logger
						.debug("Not risking PPDU Fallback strategy for CardTerminal ["
								+ cardTerminal.getName() + "] on this platform");
			}
		}
	}

	private void getFeaturesUsingControlChannel(final Card card,
			final boolean onMSWindows) throws CardException {
		byte[] featureBytes = card.transmitControlCommand(onMSWindows
				? GET_FEATURES_MICROSOFT
				: GET_FEATURES, new byte[0]);
		this.logger.debug("CCID FEATURES found using standard control command");
		for (FEATURE feature : FEATURE.values()) {
			Integer featureCode = findFeatureTLV(feature.getTag(), featureBytes);
			this.features.put(feature, featureCode);
			if (featureCode != null) {
				this.logger.debug("FEATURE " + feature.name() + " = "
						+ Integer.toHexString(featureCode));
			}
		}
	}

	private void getFeaturesUsingPPDU(final Card card) throws CardException {
		ResponseAPDU responseAPDU = card.getBasicChannel().transmit(
				new CommandAPDU((byte) 0xff, (byte) 0xc2, 0x01, 0x00,
						new byte[]{}, 32));
		this.logger.debug("PPDU response: "
				+ Integer.toHexString(responseAPDU.getSW()));
		if (responseAPDU.getSW() == 0x9000) {
			byte[] featureBytes = responseAPDU.getData();
			this.logger
					.debug("CCID FEATURES found using Pseudo-APDU Fallback Strategy");
			for (FEATURE feature : FEATURE.values()) {
				Integer featureCode = findFeaturePPDU(feature.getTag(),
						featureBytes);
				this.features.put(feature, featureCode);
				if (featureCode != null) {
					this.logger.debug("FEATURE " + feature.name() + " = "
							+ Integer.toHexString(featureCode));
				}
			}
			this.usesPPDU = true;
		} else {
			this.logger.error("CCID Features via PPDU Not Supported");
		}
	}

	public boolean hasFeature(final FEATURE feature) {
		return getFeature(feature) != null;
	}

	public Integer getFeature(final FEATURE feature) {
		return this.features.get(feature);
	}

	/*
	 * **********************************************************************************************************
	 */

	private Integer findFeatureTLV(final byte featureTag, final byte[] features) {
		int idx = 0;
		while (idx < features.length) {
			final byte tag = features[idx];
			idx += 2;
			if (featureTag == tag) {
				int feature = 0;
				for (int count = 0; count < 3; count++) {
					feature |= features[idx] & 0xff;
					idx++;
					feature <<= 8;
				}
				feature |= features[idx] & 0xff;
				return feature;
			}
			idx += 4;
		}
		return null;
	}

	private Integer findFeaturePPDU(final byte featureTag, final byte[] features) {
		for (int idx = 0; idx < features.length; idx++) {
			final byte tag = features[idx];
			if (featureTag == tag)
				return new Integer(tag);
		}
		return null;
	}

	/*
	 * ***********************************************************************************************************************
	 */

	protected byte[] transmitPPDUCommand(final int controlCode,
			final byte[] command) throws CardException {
		ResponseAPDU responseAPDU = card.getBasicChannel().transmit(
				new CommandAPDU((byte) 0xff, (byte) 0xc2, 0x01, controlCode,
						command));
		if (responseAPDU.getSW() != 0x9000)
			throw new CardException("PPDU Command Failed: ResponseAPDU="
					+ responseAPDU.getSW());

		if (responseAPDU.getData().length == 0)
			return responseAPDU.getBytes();
		else
			return responseAPDU.getData();
	}

	protected byte[] transmitControlCommand(final int controlCode,
			final byte[] command) throws CardException {
		if (usesPPDU())
			return transmitPPDUCommand(controlCode, command);
		return this.card.transmitControlCommand(controlCode, command);
	}

	public void waitForOK() throws CardException, InterruptedException {
		// wait for key pressed
		loop : while (true) {
			final byte[] getKeyPressedResult = transmitControlCommand(
					this.getFeature(FEATURE.GET_KEY_PRESSED), new byte[0]);
			final byte key = getKeyPressedResult[0];
			switch (key) {
				case 0x00 :
					this.logger.debug("waiting for CCID...");
					Thread.sleep(200);
					break;

				case 0x2b :
					this.logger.debug("PIN digit");
					break;

				case 0x0a :
					this.logger.debug("erase PIN digit");
					break;

				case 0x0d :
					this.logger.debug("user confirmed");
					break loop;

				case 0x1b :
					this.logger.debug("user canceled");
					// XXX: need to send the PIN finish ioctl?
					throw new SecurityException("canceled by user");

				case 0x40 :
					// happens in case of a reader timeout
					this.logger.debug("PIN abort");
					break loop;

				default :
					this.logger.debug("CCID get key pressed result: " + key
							+ " hex: " + Integer.toHexString(key));
			}
		}
	}

	/*
	 * *** static utilities ****
	 */

	public final byte getLanguageId(final Locale locale) {

		final String language = locale.getLanguage();

		if (DUTCH_LANGUAGE.equals(language)) {
			return DUTCH_LANGUAGE_CODE;
		}

		if (FRENCH_LANGUAGE.equals(language)) {
			return FRENCH_LANGUAGE_CODE;
		}

		if (GERMAN_LANGUAGE.equals(language)) {
			return GERMAN_LANGUAGE_CODE;
		}

		return ENGLISH_LANGUAGE_CODE; // ENGLISH
	}

	public byte[] createPINVerificationDataStructure(final Locale locale,
			final INS ins) throws IOException {
		final ByteArrayOutputStream verifyCommand = new ByteArrayOutputStream();
		verifyCommand.write(30); // bTimeOut
		verifyCommand.write(30); // bTimeOut2
		verifyCommand.write(0x80 | 0x08 | 0x00 | 0x01); // bmFormatString
		/*
		 * bmFormatString. bit 7: 1 = system units are bytes
		 * 
		 * bit 6-3: 1 = PIN position in APDU command after Lc, so just after the
		 * 0x20 | pinSize.
		 * 
		 * bit 2: 0 = left justify data
		 * 
		 * bit 1-0: 1 = BCD
		 */
		verifyCommand.write(0x47); // bmPINBlockString
		/*
		 * bmPINBlockString
		 * 
		 * bit 7-4: 4 = PIN length
		 * 
		 * bit 3-0: 7 = PIN block size (7 times 0xff)
		 */
		verifyCommand.write(0x04); // bmPINLengthFormat
		/*
		 * bmPINLengthFormat. weird... the values do not make any sense to me.
		 * 
		 * bit 7-5: 0 = RFU
		 * 
		 * bit 4: 0 = system units are bits
		 * 
		 * bit 3-0: 4 = PIN length position in APDU
		 */
		verifyCommand
				.write(new byte[]{(byte) MAX_PIN_SIZE, (byte) MIN_PIN_SIZE}); // wPINMaxExtraDigit
		/*
		 * first byte = maximum PIN size in digit
		 * 
		 * second byte = minimum PIN size in digit.
		 */
		verifyCommand.write(0x02); // bEntryValidationCondition
		/*
		 * 0x02 = validation key pressed. So the user must press the green
		 * button on his pinpad.
		 */
		verifyCommand.write(0x01); // bNumberMessage
		/*
		 * 0x01 = message with index in bMsgIndex
		 */
		verifyCommand.write(new byte[]{this.getLanguageId(locale), 0x04}); // wLangId
		/*
		 * 0x04 = default sub-language
		 */
		verifyCommand.write(0x00); // bMsgIndex
		/*
		 * 0x00 = PIN insertion prompt
		 */
		verifyCommand.write(new byte[]{0x00, 0x00, 0x00}); // bTeoPrologue
		/*
		 * bTeoPrologue : only significant for T=1 protocol.
		 */
		final byte[] verifyApdu = new byte[]{
				0x00, // CLA
				(byte) ins.getIns(), // INS
				0x00, // P1
				0x01, // P2
				0x08, // Lc = 8 bytes in command data
				(byte) 0x20, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,};
		verifyCommand.write(verifyApdu.length & 0xff); // ulDataLength[0]
		verifyCommand.write(0x00); // ulDataLength[1]
		verifyCommand.write(0x00); // ulDataLength[2]
		verifyCommand.write(0x00); // ulDataLength[3]
		verifyCommand.write(verifyApdu); // abData
		final byte[] verifyCommandData = verifyCommand.toByteArray();
		return verifyCommandData;
	}

	public byte[] createPINModificationDataStructure(final Locale locale,
			final INS ins) throws IOException {
		final ByteArrayOutputStream modifyCommand = new ByteArrayOutputStream();
		modifyCommand.write(30); // bTimeOut
		modifyCommand.write(30); // bTimeOut2
		modifyCommand.write(0x80 | 0x08 | 0x00 | 0x01); // bmFormatString
		/*
		 * bmFormatString. bit 7: 1 = system units are bytes
		 * 
		 * bit 6-3: 1 = PIN position in APDU command after Lc, so just after the
		 * 0x20 | pinSize.
		 * 
		 * bit 2: 0 = left justify data
		 * 
		 * bit 1-0: 1 = BCD
		 */

		modifyCommand.write(0x47); // bmPINBlockString
		/*
		 * bmPINBlockString
		 * 
		 * bit 7-4: 4 = PIN length
		 * 
		 * bit 3-0: 7 = PIN block size (7 times 0xff)
		 */

		modifyCommand.write(0x04); // bmPINLengthFormat
		/*
		 * bmPINLengthFormat. weird... the values do not make any sense to me.
		 * 
		 * bit 7-5: 0 = RFU
		 * 
		 * bit 4: 0 = system units are bits
		 * 
		 * bit 3-0: 4 = PIN length position in APDU
		 */

		modifyCommand.write(0x00); // bInsertionOffsetOld
		/*
		 * bInsertionOffsetOld: Insertion position offset in bytes for the
		 * current PIN
		 */

		modifyCommand.write(0x8); // bInsertionOffsetNew
		/*
		 * bInsertionOffsetNew: Insertion position offset in bytes for the new
		 * PIN
		 */

		modifyCommand
				.write(new byte[]{(byte) MAX_PIN_SIZE, (byte) MIN_PIN_SIZE}); // wPINMaxExtraDigit
		/*
		 * first byte = maximum PIN size in digit
		 * 
		 * second byte = minimum PIN size in digit.
		 */

		modifyCommand.write(0x03); // bConfirmPIN
		/*
		 * bConfirmPIN: Flags governing need for confirmation of new PIN
		 */

		modifyCommand.write(0x02); // bEntryValidationCondition
		/*
		 * 0x02 = validation key pressed. So the user must press the green
		 * button on his pinpad.
		 */

		modifyCommand.write(0x03); // bNumberMessage
		/*
		 * 0x03 = message with index in bMsgIndex
		 */

		modifyCommand.write(new byte[]{this.getLanguageId(locale), 0x04}); // wLangId
		/*
		 * 0x04 = default sub-language
		 */

		modifyCommand.write(0x00); // bMsgIndex1
		/*
		 * 0x00 = PIN insertion prompt
		 */

		modifyCommand.write(0x01); // bMsgIndex2
		/*
		 * 0x01 = new PIN prompt
		 */

		modifyCommand.write(0x02); // bMsgIndex3
		/*
		 * 0x02 = new PIN again prompt
		 */

		modifyCommand.write(new byte[]{0x00, 0x00, 0x00}); // bTeoPrologue
		/*
		 * bTeoPrologue : only significant for T=1 protocol.
		 */

		final byte[] modifyApdu = new byte[]{
				0x00, // CLA
				(byte) ins.getIns(), // INS
				0x00, // P1
				0x01, // P2
				0x10, // Lc = 16 bytes in command data
				(byte) 0x20, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0x20, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,};
		modifyCommand.write(modifyApdu.length & 0xff); // ulDataLength[0]
		modifyCommand.write(0x00); // ulDataLength[1]
		modifyCommand.write(0x00); // ulDataLength[2]
		modifyCommand.write(0x00); // ulDataLength[3]
		modifyCommand.write(modifyApdu); // abData
		final byte[] modifyCommandData = modifyCommand.toByteArray();
		return modifyCommandData;
	}

}
