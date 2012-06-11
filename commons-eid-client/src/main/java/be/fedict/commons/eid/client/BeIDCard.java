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

	private final Card card;

	private final CardChannel cardChannel;

	private final Logger logger;

	public BeIDCard(Card card, Logger logger) {
		this.card = card;
		this.cardChannel = card.getBasicChannel();
		if (null == logger) {
			throw new IllegalArgumentException("logger expected");
		}
		this.logger = logger;
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
