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

package be.fedict.commons.eid.client.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import javax.smartcardio.Card;
import javax.smartcardio.CardException;

public class CCID {
	public static final byte FEATURE_VERIFY_PIN_START_TAG = 0x01;
	public static final byte FEATURE_VERIFY_PIN_FINISH_TAG = 0x02;
	public static final byte FEATURE_MODIFY_PIN_START_TAG = 0x03;
	public static final byte FEATURE_MODIFY_PIN_FINISH_TAG = 0x04;
	public static final byte FEATURE_GET_KEY_PRESSED_TAG = 0x05;
	public static final byte FEATURE_VERIFY_PIN_DIRECT_TAG = 0x06;
	public static final byte FEATURE_MODIFY_PIN_DIRECT_TAG = 0x07;
	public static final byte FEATURE_EID_PIN_PAD_READER_TAG = (byte) 0x80;

	public static final int GET_FEATURES = 0x42000D48;
	public static final int GET_FEATURES_MICROSOFT = (0x31 << 16 | (3400) << 2);

	public static final int MIN_PIN_SIZE = 4;
	public static final int MAX_PIN_SIZE = 12;

	public static Integer getFeature(Card card, byte featureTag)
			throws CardException {
		byte[] features = readFeatures(card);
		if (features == null || features.length == 0)
			return null;
		return findFeature(featureTag, features);
	}

	private static byte[] readFeatures(Card card) throws CardException {
		String osName = System.getProperty("os.name");
		return card.transmitControlCommand(osName.startsWith("Windows")
				? GET_FEATURES_MICROSOFT
				: GET_FEATURES, new byte[0]);
	}

	private static Integer findFeature(byte featureTag, byte[] features) {
		int idx = 0;
		while (idx < features.length) {
			byte tag = features[idx];
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

	public static void waitForOK(Card card, int getKeyPressedFeature)
			throws CardException, InterruptedException {
		// wait for key pressed
		loop : while (true) {
			byte[] getKeyPressedResult = card.transmitControlCommand(
					getKeyPressedFeature, new byte[0]);
			byte key = getKeyPressedResult[0];
			switch (key) {
				case 0x00 :
					// this.view.addDetailMessage("waiting for CCID...");
					Thread.sleep(200);
					break;
				case 0x2b :
					// this.view.addDetailMessage("PIN digit");
					break;
				case 0x0a :
					// this.view.addDetailMessage("erase PIN digit");
					break;
				case 0x0d :
					// this.view.addDetailMessage("user confirmed");
					break loop;
				case 0x1b :
					// this.view.addDetailMessage("user canceled");
					// XXX: need to send the PIN finish ioctl?
					throw new SecurityException("canceled by user");
				case 0x40 :
					// happens in case of a reader timeout
					// this.view.addDetailMessage("PIN abort");
					break loop;
				default :
					// this.view.addDetailMessage("CCID get key pressed result: "
					// + key + " hex: " + Integer.toHexString(key));
			}
		}
	}

	public static byte getLanguageId(Locale locale) {
		/*
		 * USB language Ids
		 */
		if (Locale.FRENCH.equals(locale)) {
			return 0x0c;
		}
		if (Locale.GERMAN.equals(locale)) {
			return 0x07;
		}
		String language = locale.getLanguage();
		if ("nl".equals(language)) {
			return 0x13;
		}
		return 0x09; // ENGLISH
	}

	public static byte[] createPINVerificationDataStructure(Locale locale,
			int apduIns) throws IOException {
		ByteArrayOutputStream verifyCommand = new ByteArrayOutputStream();
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
		verifyCommand.write(new byte[]{getLanguageId(locale), 0x04}); // wLangId
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
		byte[] verifyApdu = new byte[]{
				0x00, // CLA
				(byte) apduIns, // INS
				0x00, // P1
				0x01, // P2
				0x08, // Lc = 8 bytes in command data
				(byte) 0x20, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
		verifyCommand.write(verifyApdu.length & 0xff); // ulDataLength[0]
		verifyCommand.write(0x00); // ulDataLength[1]
		verifyCommand.write(0x00); // ulDataLength[2]
		verifyCommand.write(0x00); // ulDataLength[3]
		verifyCommand.write(verifyApdu); // abData
		byte[] verifyCommandData = verifyCommand.toByteArray();
		return verifyCommandData;
	}

	public static byte[] createPINModificationDataStructure(Locale locale,
			int apduIns) throws IOException {
		ByteArrayOutputStream modifyCommand = new ByteArrayOutputStream();
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

		modifyCommand.write(new byte[]{getLanguageId(locale), 0x04}); // wLangId
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

		byte[] modifyApdu = new byte[]{
				0x00, // CLA
				(byte) apduIns, // INS
				0x00, // P1
				0x01, // P2
				0x10, // Lc = 16 bytes in command data
				(byte) 0x20, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0x20, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
		modifyCommand.write(modifyApdu.length & 0xff); // ulDataLength[0]
		modifyCommand.write(0x00); // ulDataLength[1]
		modifyCommand.write(0x00); // ulDataLength[2]
		modifyCommand.write(0x00); // ulDataLength[3]
		modifyCommand.write(modifyApdu); // abData
		byte[] modifyCommandData = modifyCommand.toByteArray();
		return modifyCommandData;
	}

}
