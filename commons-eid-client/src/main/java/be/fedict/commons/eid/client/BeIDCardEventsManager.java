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

public class BeIDCardEventsManager {
	private final static byte[] ATR_PATTERN = new byte[]{0x3b, (byte) 0x98,
			0x00, 0x40, 0x00, (byte) 0x00, 0x00, 0x00, 0x01, 0x01, (byte) 0xad,
			0x13, 0x10};
	private final static byte[] ATR_MASK = new byte[]{(byte) 0xff, (byte) 0xff,
			0x00, (byte) 0xff, 0x00, 0x00, 0x00, 0x00, (byte) 0xff,
			(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xf0};

	private CardAndTerminalEventsManager cardAndTerminalEventsManager;
	private boolean terminalManagerIsPrivate;
	private Map<CardTerminal, BeIDCard> terminalsAndCards;
	private Set<BeIDCardEventsListener> beIdListeners;
	private Set<CardEventsListener> otherCardListeners;
	private final Logger logger;

	/*
	 * a BeIDCardEventsManager with a default (void) logger and a private
	 * CardAndTerminalEventsManager that is automatically started and stopped
	 * with the BeIDCardEventsManager
	 */
	public BeIDCardEventsManager() {
		this(new VoidLogger());
	}

	/*
	 * a BeIDCardEventsManager logging to logger, and a private Terminalmanager
	 * that is automatically started and stopped with the BeIDCardEventsManager
	 */
	public BeIDCardEventsManager(Logger logger) {
		this(logger, new CardAndTerminalEventsManager());
		this.terminalManagerIsPrivate = true;
	}

	/*
	 * a BeIDCardEventsManager with a default (void) logger, caller supplies a
	 * CardAndTerminalEventsManager. note: caller is responsible for start()in
	 * the supplied CardAndTerminalEventsManager, it will not be automatically
	 * started!
	 */
	public BeIDCardEventsManager(
			CardAndTerminalEventsManager cardAndTerminalEventsManager) {
		this(new VoidLogger(), cardAndTerminalEventsManager);
	}

	/*
	 * a BeIDCardEventsManager logging to logger, caller supplies a
	 * CardAndTerminalEventsManager. note: caller is responsible for start()in
	 * the supplied CardAndTerminalEventsManager, it will not be automatically
	 * started!
	 */
	public BeIDCardEventsManager(Logger logger,
			CardAndTerminalEventsManager cardAndTerminalEventsManager) {
		this.logger = logger;
		this.beIdListeners = new HashSet<BeIDCardEventsListener>();
		this.otherCardListeners = new HashSet<CardEventsListener>();
		this.terminalsAndCards = new HashMap<CardTerminal, BeIDCard>();
		this.cardAndTerminalEventsManager = cardAndTerminalEventsManager;
		this.cardAndTerminalEventsManager
				.addCardListener(new CardEventsListener() {
					@Override
					public void cardInserted(CardTerminal cardTerminal,
							Card card) {
						if (card != null && matchesEidAtr(card.getATR())) {
							BeIDCard beIDCard = new BeIDCard(card,
									BeIDCardEventsManager.this.logger);

							synchronized (BeIDCardEventsManager.this.terminalsAndCards) {
								BeIDCardEventsManager.this.terminalsAndCards
										.put(cardTerminal, beIDCard);
							}

							Set<BeIDCardEventsListener> copyOfListeners = null;

							synchronized (BeIDCardEventsManager.this.beIdListeners) {
								copyOfListeners = new HashSet<BeIDCardEventsListener>(
										BeIDCardEventsManager.this.beIdListeners);
							}

							for (BeIDCardEventsListener listener : copyOfListeners) {
								try {
									listener.eIDCardInserted(cardTerminal,
											beIDCard);
								} catch (Throwable thrownInListener) {
									BeIDCardEventsManager.this.logger
											.error("Exception thrown in BeIDCardEventsListener.eIDCardInserted:"
													+ thrownInListener
															.getMessage());
								}
							}
						} else {
							Set<CardEventsListener> copyOfListeners = null;

							synchronized (BeIDCardEventsManager.this.otherCardListeners) {
								copyOfListeners = new HashSet<CardEventsListener>(
										BeIDCardEventsManager.this.otherCardListeners);
							}

							for (CardEventsListener listener : copyOfListeners) {
								try {
									listener.cardInserted(cardTerminal, card);
								} catch (Throwable thrownInListener) {
									BeIDCardEventsManager.this.logger
											.error("Exception thrown in CardEventsListener.cardInserted:"
													+ thrownInListener
															.getMessage());
								}
							}
						}
					}

					@Override
					public void cardRemoved(CardTerminal cardTerminal) {
						BeIDCard beIDCard = BeIDCardEventsManager.this.terminalsAndCards
								.get(cardTerminal);
						if (beIDCard != null) {
							synchronized (BeIDCardEventsManager.this.terminalsAndCards) {
								BeIDCardEventsManager.this.terminalsAndCards
										.remove(cardTerminal);
							}

							Set<BeIDCardEventsListener> copyOfListeners = null;

							synchronized (BeIDCardEventsManager.this.beIdListeners) {
								copyOfListeners = new HashSet<BeIDCardEventsListener>(
										BeIDCardEventsManager.this.beIdListeners);
							}

							for (BeIDCardEventsListener listener : copyOfListeners) {
								try {
									listener.eIDCardRemoved(cardTerminal,
											beIDCard);
								} catch (Throwable thrownInListener) {
									BeIDCardEventsManager.this.logger
											.error("Exception thrown in BeIDCardEventsListener.eIDCardRemoved:"
													+ thrownInListener
															.getMessage());
								}

							}
						} else {
							Set<CardEventsListener> copyOfListeners = null;

							synchronized (BeIDCardEventsManager.this.otherCardListeners) {
								copyOfListeners = new HashSet<CardEventsListener>(
										BeIDCardEventsManager.this.otherCardListeners);
							}

							for (CardEventsListener listener : copyOfListeners) {
								try {
									listener.cardRemoved(cardTerminal);
								} catch (Throwable thrownInListener) {
									BeIDCardEventsManager.this.logger
											.error("Exception thrown in CardEventsListener.cardRemoved:"
													+ thrownInListener
															.getMessage());
								}
							}
						}

					}
				});
		updateTerminalsAndCards();
	}

	public BeIDCardEventsManager start() {
		if (terminalManagerIsPrivate)
			cardAndTerminalEventsManager.start();
		return this;
	}

	// add a BeIDCardEventsListener to be notified of BeID cards being inserted
	public BeIDCardEventsManager addBeIDCardEventListener(
			BeIDCardEventsListener listener) {
		synchronized (beIdListeners) {
			beIdListeners.add(listener);
		}
		return this;
	}

	// remove a BeIDCardEventsListener
	public BeIDCardEventsManager removeBeIDCardListener(
			BeIDCardEventsListener listener) {
		synchronized (beIdListeners) {
			beIdListeners.remove(listener);
		}
		return this;
	}

	// add a CardEventsListener to get notified of other cards being
	// inserted/removed
	public BeIDCardEventsManager addOtherCardEventListener(
			CardEventsListener listener) {
		synchronized (otherCardListeners) {
			otherCardListeners.add(listener);
		}
		return this;
	}

	// remove a CardEventsListener
	public BeIDCardEventsManager removeOtherCardEventListener(
			BeIDCardEventsListener listener) {
		synchronized (otherCardListeners) {
			otherCardListeners.remove(listener);
		}
		return this;
	}

	public BeIDCardEventsManager stop() throws InterruptedException {
		if (terminalManagerIsPrivate)
			cardAndTerminalEventsManager.stop();
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
		// if our CardAndTerminalEventsManager is running, terminalsAndCards
		// will get updated
		// asynchronously, don't replace it here
		if (cardAndTerminalEventsManager.isRunning())
			return;

		Map<CardTerminal, BeIDCard> newTerminalsAndCards = new HashMap<CardTerminal, BeIDCard>();

		try {
			for (CardTerminal terminal : cardAndTerminalEventsManager
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
