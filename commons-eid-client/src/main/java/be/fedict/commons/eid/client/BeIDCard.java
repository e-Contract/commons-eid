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

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

/**
 * Low-level eID methods.
 * 
 * @author Frank Cornelis
 * 
 */
public class BeIDCard {

	public static final byte[] ADDRESS_FILE_ID = new byte[] { 0x3F, 0x00,
			(byte) 0xDF, 0x01, 0x40, 0x33 };

	public static final byte[] ADDRESS_SIGN_FILE_ID = new byte[] { 0x3F, 0x00,
			(byte) 0xDF, 0x01, 0x40, 0x34 };

	public static final byte[] PHOTO_FILE_ID = new byte[] { 0x3F, 0x00,
			(byte) 0xDF, 0x01, 0x40, 0x35 };

	public static final byte[] AUTHN_CERT_FILE_ID = new byte[] { 0x3F, 0x00,
			(byte) 0xDF, 0x00, 0x50, 0x38 };

	public static final byte[] SIGN_CERT_FILE_ID = new byte[] { 0x3F, 0x00,
			(byte) 0xDF, 0x00, 0x50, 0x39 };

	public static final byte[] CA_CERT_FILE_ID = new byte[] { 0x3F, 0x00,
			(byte) 0xDF, 0x00, 0x50, 0x3A };

	public static final byte[] ROOT_CERT_FILE_ID = new byte[] { 0x3F, 0x00,
			(byte) 0xDF, 0x00, 0x50, 0x3B };

	private final Card card;

	private final CardChannel cardChannel;

	private final Logger logger;

	private final List<BeIDCardListener> cardListeners;

	public BeIDCard(Card card, Logger logger) {
		this.card = card;
		this.cardChannel = card.getBasicChannel();
		if (null == logger) {
			throw new IllegalArgumentException("logger expected");
		}
		this.logger = logger;
		this.cardListeners = new LinkedList<BeIDCardListener>();
	}

	public BeIDCard(Card card) {
		this(card, new VoidLogger());
	}

	public BeIDCard(CardTerminal cardTerminal, Logger logger)
			throws CardException {
		this(cardTerminal.connect("T=0"), logger);
	}

	public BeIDCard(CardTerminal cardTerminal) throws CardException {
		this(cardTerminal.connect("T=0"));
	}

	public synchronized void addCardListener(BeIDCardListener beIDCardListener) {
		this.cardListeners.add(beIDCardListener);
	}

	public synchronized void removeCardListener(
			BeIDCardListener beIDCardListener) {
		this.cardListeners.remove(beIDCardListener);
	}

	private ResponseAPDU transmit(CommandAPDU commandApdu) throws CardException {
		ResponseAPDU responseApdu = this.cardChannel.transmit(commandApdu);
		if (0x6c == responseApdu.getSW1()) {
			/*
			 * A minimum delay of 10 msec between the answer ‘6C xx’ and the
			 * next APDU is mandatory for eID v1.0 and v1.1 cards.
			 */
			this.logger.debug("sleeping...");
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				throw new RuntimeException("cannot sleep");
			}
			responseApdu = this.cardChannel.transmit(commandApdu);
		}
		return responseApdu;
	}

	private static final int BLOCK_SIZE = 0xff;

	private byte[] readBinary(int estimatedMaxSize) throws CardException,
			IOException {
		int offset = 0;
		this.logger.debug("read binary");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] data;
		do {
			notifyReadProgress(offset, estimatedMaxSize);
			CommandAPDU readBinaryApdu = new CommandAPDU(0x00, 0xB0,
					offset >> 8, offset & 0xFF, BLOCK_SIZE);
			ResponseAPDU responseApdu = transmit(readBinaryApdu);
			int sw = responseApdu.getSW();
			if (0x6B00 == sw) {
				/*
				 * Wrong parameters (offset outside the EF) End of file reached.
				 * Can happen in case the file size is a multiple of 0xff bytes.
				 */
				break;
			}
			if (0x9000 != sw) {
				throw new IOException("APDU response error: "
						+ responseApdu.getSW());
			}

			data = responseApdu.getData();
			baos.write(data);
			offset += data.length;
		} while (BLOCK_SIZE == data.length);
		notifyReadProgress(offset, offset);
		return baos.toByteArray();
	}

	private void notifyReadProgress(int offset, int estimatedMaxOffset) {
		if (offset > estimatedMaxOffset) {
			estimatedMaxOffset = offset;
		}
		List<BeIDCardListener> listeners;
		synchronized (this) {
			listeners = new LinkedList<BeIDCardListener>(this.cardListeners);
		}
		for (BeIDCardListener listener : listeners) {
			listener.notifyReadProgress(offset, estimatedMaxOffset);
		}
	}

	private void selectFile(byte[] fileId) throws CardException,
			FileNotFoundException {
		this.logger.debug("selecting file");
		CommandAPDU selectFileApdu = new CommandAPDU(0x00, 0xA4, 0x08, 0x0C,
				fileId);
		ResponseAPDU responseApdu = transmit(selectFileApdu);
		if (0x9000 != responseApdu.getSW()) {
			throw new FileNotFoundException(
					"wrong status word after selecting file: "
							+ Integer.toHexString(responseApdu.getSW()));
		}
		try {
			// SCARD_E_SHARING_VIOLATION fix
			Thread.sleep(20);
		} catch (InterruptedException e) {
			throw new RuntimeException("sleep error: " + e.getMessage());
		}
	}

	public byte[] readFile(BeIDFileType beIDFile) throws CardException,
			IOException {
		byte[] fileId = beIDFile.getFileId();
		int estimatedMaxSize = beIDFile.getEstimatedMaxSize();
		selectFile(fileId);
		byte[] data = readBinary(estimatedMaxSize);
		return data;
	}

	public void close() {
		this.logger.debug("closing eID card");
		try {
			this.card.disconnect(true);
		} catch (CardException e) {
			this.logger.error("could not disconnect the card: "
					+ e.getMessage());
		}
	}
}
