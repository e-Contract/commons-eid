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

public class BeIDCardEventsManager implements CardEventsListener {
	private final static byte[] ATR_PATTERN = new byte[]{0x3b, (byte) 0x98,
			0x00, 0x40, 0x00, (byte) 0x00, 0x00, 0x00, 0x01, 0x01, (byte) 0xad,
			0x13, 0x10};
	private final static byte[] ATR_MASK = new byte[]{(byte) 0xff, (byte) 0xff,
			0x00, (byte) 0xff, 0x00, 0x00, 0x00, 0x00, (byte) 0xff,
			(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xf0};

	private CardAndTerminalEventsManager cardAndTerminalEventsManager;
	private boolean terminalManagerIsPrivate;
	private Map<CardTerminal, BELPICCard> terminalsAndCards;
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
		this.terminalsAndCards = new HashMap<CardTerminal, BELPICCard>();
		this.cardAndTerminalEventsManager = cardAndTerminalEventsManager;
		this.cardAndTerminalEventsManager.addCardListener(this);
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

	public BeIDCardEventsManager stop() {
		if (terminalManagerIsPrivate)
			cardAndTerminalEventsManager.stop();
		return this;
	}

	public Map<CardTerminal, BELPICCard> getTerminalsAndBeIDCardsPresent() {
		updateTerminalsAndCards();
		Map<CardTerminal, BELPICCard> copyOfTerminalsAndCards;
		synchronized (terminalsAndCards) {
			copyOfTerminalsAndCards = new HashMap<CardTerminal, BELPICCard>(
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

	public Set<BELPICCard> getBeIDCardsPresent() {
		updateTerminalsAndCards();
		Set<BELPICCard> cards;
		synchronized (terminalsAndCards) {
			cards = new HashSet<BELPICCard>(terminalsAndCards.values());
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

	public BELPICCard getFirstBeIDCard() {
		Set<BELPICCard> cards = getBeIDCardsPresent();
		Iterator<BELPICCard> cardsIterator = cards.iterator();
		if (!cardsIterator.hasNext())
			return null;
		return cardsIterator.next();
	}

	public Map.Entry<CardTerminal, BELPICCard> getFirstBeIDTerminalAndCard() {
		Map<CardTerminal, BELPICCard> terminalsAndCards = getTerminalsAndBeIDCardsPresent();
		Iterator<Map.Entry<CardTerminal, BELPICCard>> terminalsAndCardsIterator = terminalsAndCards
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

		Map<CardTerminal, BELPICCard> newTerminalsAndCards = new HashMap<CardTerminal, BELPICCard>();

		try {
			for (CardTerminal terminal : cardAndTerminalEventsManager
					.getTerminalsWithCards()) {
				try {
					Card card = terminal.connect("*");
					if (card != null && matchesEidAtr(card.getATR()))
						newTerminalsAndCards.put(terminal, new BELPICCard(card,
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

	@Override
	public void cardInserted(CardTerminal cardTerminal, Card card) {
		if (card != null && matchesEidAtr(card.getATR())) {
			BELPICCard beIDCard = new BELPICCard(card, logger);

			synchronized (terminalsAndCards) {
				terminalsAndCards.put(cardTerminal, beIDCard);
			}

			synchronized (beIdListeners) {
				for (BeIDCardEventsListener listener : beIdListeners)
					listener.eIDCardInserted(cardTerminal, beIDCard);
			}
		} else {
			synchronized (otherCardListeners) {
				for (CardEventsListener listener : otherCardListeners)
					listener.cardInserted(cardTerminal, card);
			}
		}
	}

	@Override
	public void cardRemoved(CardTerminal cardTerminal) {
		BELPICCard beIDCard = terminalsAndCards.get(cardTerminal);
		if (beIDCard != null) {
			synchronized (terminalsAndCards) {
				terminalsAndCards.remove(cardTerminal);
			}

			synchronized (beIdListeners) {
				for (BeIDCardEventsListener listener : beIdListeners)
					listener.eIDCardRemoved(cardTerminal, beIDCard);
			}
		} else {
			synchronized (otherCardListeners) {
				for (CardEventsListener listener : otherCardListeners)
					listener.cardRemoved(cardTerminal);
			}
		}
	}
}
