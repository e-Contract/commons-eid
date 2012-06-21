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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;

/**
 * The card manager takes care of detection of (eID) smart cards.
 * 
 * @author Frank Cornelis
 * 
 */
public class CardManager {

	private final static byte[] ATR_PATTERN = new byte[] { 0x3b, (byte) 0x98,
			0x00, 0x40, 0x00, (byte) 0x00, 0x00, 0x00, 0x01, 0x01, (byte) 0xad,
			0x13, 0x10 };

	private final static byte[] ATR_MASK = new byte[] { (byte) 0xff,
			(byte) 0xff, 0x00, (byte) 0xff, 0x00, 0x00, 0x00, 0x00,
			(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xf0 };

	private final TerminalFactory terminalFactory;

	private final CardTerminals cardTerminals;

	private final Logger logger;

	public CardManager(Logger logger) {
		LibJ2PCSCGNULinuxFix.fixNativeLibrary();
		this.terminalFactory = TerminalFactory.getDefault();
		this.cardTerminals = this.terminalFactory.terminals();
		this.logger = logger;
	}

	public CardManager() {
		this(new VoidLogger());
	}

	public CardTerminal findFirstBeIDCardTerminal() throws CardException {
		List<CardTerminal> cardTerminalList;
		try {
			cardTerminalList = this.cardTerminals.list();
		} catch (CardException e) {
			this.logger.debug("card terminals list error: " + e.getMessage());
			this.logger.debug("no card readers connected?");
			Throwable cause = e.getCause();
			if (null != cause) {
				/*
				 * Windows can give us a sun.security.smartcardio.PCSCException
				 * SCARD_E_NO_READERS_AVAILABLE when no card readers are
				 * connected to the system.
				 */
				this.logger.debug("cause: " + cause.getMessage());
				this.logger.debug("cause type: " + cause.getClass().getName());
				if ("SCARD_E_NO_READERS_AVAILABLE".equals(cause.getMessage())) {
					/*
					 * Windows platform.
					 */
					this.logger.debug("no reader available");
				}
			}
			return null;
		}
		Set<CardTerminal> eIDCardTerminals = new HashSet<CardTerminal>();
		for (CardTerminal cardTerminal : cardTerminalList) {
			this.logger.debug("Scanning card terminal: "
					+ cardTerminal.getName());
			if (cardTerminal.isCardPresent()) {
				Card card;
				try {
					/*
					 * eToken is not using T=0 apparently, hence the need for an
					 * explicit CardException catch
					 */
					card = cardTerminal.connect("T=0");
				} catch (CardException e) {
					this.logger.debug("could not connect to card: "
							+ e.getMessage());
					continue;
				}
				ATR atr = card.getATR();
				if (matchesEidAtr(atr)) {
					eIDCardTerminals.add(cardTerminal);
				} else {
					byte[] atrBytes = atr.getBytes();
					StringBuffer atrStringBuffer = new StringBuffer();
					for (byte atrByte : atrBytes) {
						atrStringBuffer.append(Integer
								.toHexString(atrByte & 0xff));
					}
					this.logger.debug("not a supported eID card. ATR= "
							+ atrStringBuffer);
				}
				card.disconnect(true);
			}
		}
		if (eIDCardTerminals.isEmpty()) {
			return null;
		}
		if (eIDCardTerminals.size() > 1) {
			this.logger.debug("multiple eID cards present");
		}
		CardTerminal eIDCardTerminal = eIDCardTerminals.iterator().next();
		this.logger.debug("eID card terminal: " + eIDCardTerminal.getName());
		return eIDCardTerminal;
	}

	private boolean matchesEidAtr(ATR atr) {
		byte[] atrBytes = atr.getBytes();
		if (atrBytes.length != ATR_PATTERN.length) {
			return false;
		}
		for (int idx = 0; idx < atrBytes.length; idx++) {
			atrBytes[idx] &= ATR_MASK[idx];
		}
		if (Arrays.equals(atrBytes, ATR_PATTERN)) {
			return true;
		}
		return false;
	}
}
