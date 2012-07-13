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
	private static final int BLOCK_SIZE = 0xff;
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

	//--------------------------------------------------------------------------------------------------------------------------------

	public BeIDCard addCardListener(BeIDCardListener beIDCardListener) {
		synchronized (this.cardListeners) {
			this.cardListeners.add(beIDCardListener);
		}

		return this;
	}

	public BeIDCard removeCardListener(BeIDCardListener beIDCardListener) {
		synchronized (this.cardListeners) {
			this.cardListeners.remove(beIDCardListener);
		}

		return this;
	}

	//--------------------------------------------------------------------------------------------------------------------------------

	public byte[] readBinary(int estimatedMaxSize) throws CardException,
			IOException {
		int offset = 0;
		this.logger.debug("read binary");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] data;
		do {
			notifyReadProgress(offset, estimatedMaxSize);
			ResponseAPDU responseApdu = transmitCommand(
					BeIDCommandAPDU.READ_BINARY, offset >> 8, offset & 0xFF,
					BLOCK_SIZE);
			int sw = responseApdu.getSW();
			if (0x6B00 == sw) {
				/*
				 * Wrong parameters (offset outside the EF) End of file reached.
				 * Can happen in case the file size is a multiple of 0xff bytes.
				 */
				break;
			}

			if (0x9000 != sw) {
				IOException ioEx = new IOException(
						"BeIDCommandAPDU response error: "
								+ responseApdu.getSW());
				ioEx.initCause(new ResponseAPDUException(responseApdu));
				throw ioEx;
			}

			data = responseApdu.getData();
			baos.write(data);
			offset += data.length;
		} while (BLOCK_SIZE == data.length);
		notifyReadProgress(offset, offset);
		return baos.toByteArray();
	}

	public BeIDCard selectFile(byte[] fileId) throws CardException,
			FileNotFoundException {
		this.logger.debug("selecting file");
		ResponseAPDU responseApdu = transmitCommand(
				BeIDCommandAPDU.SELECT_FILE, fileId);
		if (0x9000 != responseApdu.getSW()) {
			FileNotFoundException fnfEx = new FileNotFoundException(
					"wrong status word after selecting file: "
							+ Integer.toHexString(responseApdu.getSW()));
			fnfEx.initCause(new ResponseAPDUException(responseApdu));
			throw fnfEx;
		}

		try {
			// SCARD_E_SHARING_VIOLATION fix
			Thread.sleep(20);
		} catch (InterruptedException e) {
			throw new RuntimeException("sleep error: " + e.getMessage());
		}

		return this;
	}

	public byte[] readFile(BeIDFileType beIDFile) throws CardException,
			IOException {
		selectFile(beIDFile.getFileId());
		return readBinary(beIDFile.getEstimatedMaxSize());
	}

	public BeIDCard close() {
		this.logger.debug("closing eID card");

		try {
			this.card.disconnect(true);
		} catch (CardException e) {
			this.logger.error("could not disconnect the card: "
					+ e.getMessage());
		}

		return this;
	}

	public Card getCard() {
		return card;
	}

	// ----------------------------------------------------------------------------------------------------------------------------------

	private ResponseAPDU transmitCommand(BeIDCommandAPDU apdu, int p1, int p2,
			int le) throws CardException {
		return transmit(new CommandAPDU(apdu.getCla(), apdu.getIns(), p1, p2,
				le));
	}

	private ResponseAPDU transmitCommand(BeIDCommandAPDU apdu, byte[] data)
			throws CardException {
		return transmit(new CommandAPDU(apdu.getCla(), apdu.getIns(), apdu
				.getP1(), apdu.getP2(), data));
	}

	private ResponseAPDU transmit(CommandAPDU commandApdu) throws CardException {
		ResponseAPDU responseApdu = this.cardChannel.transmit(commandApdu);
		if (0x6c == responseApdu.getSW1()) {
			/*
			 * A minimum delay of 10 msec between the answer ?????????6C
			 * xx????????? and the next BeIDCommandAPDU is mandatory for eID v1.0 and v1.1
			 * cards.
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

	// ----------------------------------------------------------------------------------------------------------------------------------

	private void notifyReadProgress(int offset, int estimatedMaxOffset) {
		if (offset > estimatedMaxOffset) {
			estimatedMaxOffset = offset;
		}

		synchronized (this.cardListeners) {
			for (BeIDCardListener listener : this.cardListeners) {
				listener.notifyReadProgress(offset, estimatedMaxOffset);
			}
		}
	}
}
