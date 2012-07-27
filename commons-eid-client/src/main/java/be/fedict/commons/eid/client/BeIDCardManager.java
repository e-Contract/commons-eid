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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import be.fedict.commons.eid.client.event.BeIDCardEventsListener;
import be.fedict.commons.eid.client.event.CardEventsListener;
import be.fedict.commons.eid.client.impl.VoidLogger;
import be.fedict.commons.eid.client.spi.Logger;

public class BeIDCardManager {
	private final static byte[] ATR_PATTERN = new byte[]{0x3b, (byte) 0x98,
			0x00, 0x40, 0x00, (byte) 0x00, 0x00, 0x00, 0x01, 0x01, (byte) 0xad,
			0x13, 0x10};
	private final static byte[] ATR_MASK = new byte[]{(byte) 0xff, (byte) 0xff,
			0x00, (byte) 0xff, 0x00, 0x00, 0x00, 0x00, (byte) 0xff,
			(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xf0};

	private CardAndTerminalManager cardAndTerminalManager;
	private boolean terminalManagerIsPrivate;
	private Map<CardTerminal, BeIDCard> terminalsAndCards;
	private Set<BeIDCardEventsListener> beIdListeners;
	private Set<CardEventsListener> otherCardListeners;
	private final Logger logger;

	/*
	 * a BeIDCardManager with a default (void) logger and a private
	 * CardAndTerminalManager that is automatically started and stopped
	 * with the BeIDCardManager
	 */
	public BeIDCardManager() {
		this(new VoidLogger());
	}

	/*
	 * a BeIDCardManager logging to logger, and a private Terminalmanager
	 * that is automatically started and stopped with the BeIDCardManager
	 */
	public BeIDCardManager(Logger logger) {
		this(logger, new CardAndTerminalManager());
		this.terminalManagerIsPrivate = true;
	}

	/*
	 * a BeIDCardManager with a default (void) logger, caller supplies a
	 * CardAndTerminalManager. note: caller is responsible for start()in
	 * the supplied CardAndTerminalManager, it will not be automatically
	 * started!
	 */
	public BeIDCardManager(CardAndTerminalManager cardAndTerminalManager) {
		this(new VoidLogger(), cardAndTerminalManager);
	}

	/*
	 * a BeIDCardManager logging to logger, caller supplies a
	 * CardAndTerminalManager. note: caller is responsible for start()in
	 * the supplied CardAndTerminalManager, it will not be automatically
	 * started!
	 */
	public BeIDCardManager(Logger logger,
			CardAndTerminalManager cardAndTerminalManager) {
		this.logger = logger;
		this.beIdListeners = new HashSet<BeIDCardEventsListener>();
		this.otherCardListeners = new HashSet<CardEventsListener>();
		this.terminalsAndCards = new HashMap<CardTerminal, BeIDCard>();
		this.cardAndTerminalManager = cardAndTerminalManager;
		this.cardAndTerminalManager.addCardListener(new CardEventsListener() {
			@Override
			public void cardInserted(CardTerminal cardTerminal, Card card) {
				if (card != null && matchesEidAtr(card.getATR())) {
					BeIDCard beIDCard = new BeIDCard(card,
							BeIDCardManager.this.logger);

					synchronized (BeIDCardManager.this.terminalsAndCards) {
						BeIDCardManager.this.terminalsAndCards.put(
								cardTerminal, beIDCard);
					}

					Set<BeIDCardEventsListener> copyOfListeners = null;

					synchronized (BeIDCardManager.this.beIdListeners) {
						copyOfListeners = new HashSet<BeIDCardEventsListener>(
								BeIDCardManager.this.beIdListeners);
					}

					for (BeIDCardEventsListener listener : copyOfListeners) {
						try {
							listener.eIDCardInserted(cardTerminal, beIDCard);
						} catch (Throwable thrownInListener) {
							BeIDCardManager.this.logger
									.error("Exception thrown in BeIDCardEventsListener.eIDCardInserted:"
											+ thrownInListener.getMessage());
						}
					}
				} else {
					Set<CardEventsListener> copyOfListeners = null;

					synchronized (BeIDCardManager.this.otherCardListeners) {
						copyOfListeners = new HashSet<CardEventsListener>(
								BeIDCardManager.this.otherCardListeners);
					}

					for (CardEventsListener listener : copyOfListeners) {
						try {
							listener.cardInserted(cardTerminal, card);
						} catch (Throwable thrownInListener) {
							BeIDCardManager.this.logger
									.error("Exception thrown in CardEventsListener.cardInserted:"
											+ thrownInListener.getMessage());
						}
					}
				}
			}

			@Override
			public void cardRemoved(CardTerminal cardTerminal) {
				BeIDCard beIDCard = BeIDCardManager.this.terminalsAndCards
						.get(cardTerminal);
				if (beIDCard != null) {
					synchronized (BeIDCardManager.this.terminalsAndCards) {
						BeIDCardManager.this.terminalsAndCards
								.remove(cardTerminal);
					}

					Set<BeIDCardEventsListener> copyOfListeners = null;

					synchronized (BeIDCardManager.this.beIdListeners) {
						copyOfListeners = new HashSet<BeIDCardEventsListener>(
								BeIDCardManager.this.beIdListeners);
					}

					for (BeIDCardEventsListener listener : copyOfListeners) {
						try {
							listener.eIDCardRemoved(cardTerminal, beIDCard);
						} catch (Throwable thrownInListener) {
							BeIDCardManager.this.logger
									.error("Exception thrown in BeIDCardEventsListener.eIDCardRemoved:"
											+ thrownInListener.getMessage());
						}

					}
				} else {
					Set<CardEventsListener> copyOfListeners = null;

					synchronized (BeIDCardManager.this.otherCardListeners) {
						copyOfListeners = new HashSet<CardEventsListener>(
								BeIDCardManager.this.otherCardListeners);
					}

					for (CardEventsListener listener : copyOfListeners) {
						try {
							listener.cardRemoved(cardTerminal);
						} catch (Throwable thrownInListener) {
							BeIDCardManager.this.logger
									.error("Exception thrown in CardEventsListener.cardRemoved:"
											+ thrownInListener.getMessage());
						}
					}
				}

			}
		});
		updateTerminalsAndCards();
	}

	public BeIDCardManager start() {
		if (terminalManagerIsPrivate)
			cardAndTerminalManager.start();
		return this;
	}

	// add a BeIDCardEventsListener to be notified of BeID cards being inserted
	public BeIDCardManager addBeIDCardEventListener(
			BeIDCardEventsListener listener) {
		synchronized (beIdListeners) {
			beIdListeners.add(listener);
		}
		return this;
	}

	// remove a BeIDCardEventsListener
	public BeIDCardManager removeBeIDCardListener(
			BeIDCardEventsListener listener) {
		synchronized (beIdListeners) {
			beIdListeners.remove(listener);
		}
		return this;
	}

	// add a CardEventsListener to get notified of other cards being
	// inserted/removed
	public BeIDCardManager addOtherCardEventListener(CardEventsListener listener) {
		synchronized (otherCardListeners) {
			otherCardListeners.add(listener);
		}
		return this;
	}

	// remove a CardEventsListener
	public BeIDCardManager removeOtherCardEventListener(
			BeIDCardEventsListener listener) {
		synchronized (otherCardListeners) {
			otherCardListeners.remove(listener);
		}
		return this;
	}

	public BeIDCardManager stop() throws InterruptedException {
		if (terminalManagerIsPrivate)
			cardAndTerminalManager.stop();
		return this;
	}

	public Map<CardTerminal, BeIDCard> getTerminalsAndBeIDCardsPresent() {
		updateTerminalsAndCards();
		Map<CardTerminal, BeIDCard> copyOfTerminalsAndCards;
		synchronized (terminalsAndCards) {
			copyOfTerminalsAndCards = new HashMap<CardTerminal, BeIDCard>(
					terminalsAndCards);
		}
		return copyOfTerminalsAndCards;
	}

	public Set<CardTerminal> getTerminalsWithBeIDCardsPresent() {
		updateTerminalsAndCards();
		Set<CardTerminal> terminals;
		synchronized (terminalsAndCards) {
			terminals = new HashSet<CardTerminal>(terminalsAndCards.keySet());
		}
		return terminals;
	}

	public Set<BeIDCard> getBeIDCardsPresent() {
		updateTerminalsAndCards();
		Set<BeIDCard> cards;
		synchronized (terminalsAndCards) {
			cards = new HashSet<BeIDCard>(terminalsAndCards.values());
		}
		return cards;
	}

	public CardTerminal getFirstBeIDCardTerminal() {
		Set<CardTerminal> terminalsWithCards = getTerminalsWithBeIDCardsPresent();
		Iterator<CardTerminal> terminalsWithCardsIterator = terminalsWithCards
				.iterator();
		if (!terminalsWithCardsIterator.hasNext())
			return null;
		return terminalsWithCardsIterator.next();
	}

	public BeIDCard getFirstBeIDCard() {
		Set<BeIDCard> cards = getBeIDCardsPresent();
		Iterator<BeIDCard> cardsIterator = cards.iterator();
		if (!cardsIterator.hasNext())
			return null;
		return cardsIterator.next();
	}

	public Map.Entry<CardTerminal, BeIDCard> getFirstBeIDTerminalAndCard() {
		Map<CardTerminal, BeIDCard> terminalsAndCards = getTerminalsAndBeIDCardsPresent();
		Iterator<Map.Entry<CardTerminal, BeIDCard>> terminalsAndCardsIterator = terminalsAndCards
				.entrySet().iterator();
		if (!terminalsAndCardsIterator.hasNext())
			return null;
		return terminalsAndCardsIterator.next();
	}

	private void updateTerminalsAndCards() {
		// if our CardAndTerminalManager is running, terminalsAndCards
		// will get updated
		// asynchronously, don't replace it here
		if (cardAndTerminalManager.isRunning())
			return;

		Map<CardTerminal, BeIDCard> newTerminalsAndCards = new HashMap<CardTerminal, BeIDCard>();

		try {
			for (CardTerminal terminal : cardAndTerminalManager
					.getTerminalsWithCards()) {
				try {
					Card card = terminal.connect("*");
					if (card != null && matchesEidAtr(card.getATR()))
						newTerminalsAndCards.put(terminal, new BeIDCard(card,
								logger));
				} catch (CardException cex) {
					logger.error("Can't Connect to Card in Terminal "
							+ terminal.getName());
				}
			}
		} catch (CardException cex) {
			logger.error("Can't Obtain List Of Terminals With Cards");
		}

		terminalsAndCards = newTerminalsAndCards;
	}

	private boolean matchesEidAtr(ATR atr) {
		byte[] atrBytes = atr.getBytes();
		if (atrBytes.length != ATR_PATTERN.length)
			return false;
		for (int idx = 0; idx < atrBytes.length; idx++)
			atrBytes[idx] &= ATR_MASK[idx];
		if (Arrays.equals(atrBytes, ATR_PATTERN))
			return true;
		return false;
	}
}
