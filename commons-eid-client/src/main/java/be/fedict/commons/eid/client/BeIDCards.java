package be.fedict.commons.eid.client;

import java.awt.GraphicsEnvironment;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.smartcardio.Card;
import javax.smartcardio.CardTerminal;
import be.fedict.commons.eid.client.event.BeIDCardEventsListener;
import be.fedict.commons.eid.client.event.CardEventsListener;
import be.fedict.commons.eid.client.impl.VoidLogger;
import be.fedict.commons.eid.client.spi.BeIDCardsUI;
import be.fedict.commons.eid.client.spi.Logger;
import be.fedict.commons.eid.client.spi.Sleeper;

public class BeIDCards {
	private static final String UI_MISSING_LOG_MESSAGE = "No BeIDCardsUI set and can't load DefaultBeIDCardsUI";
	private static final String UI_DEFAULT_REQUIRES_HEAD = "No BeIDCardsUI set and DefaultBeIDCardsUI requires a graphical environment";
	private static final String DEFAULT_UI_IMPLEMENTATION = "be.fedict.eid.commons.dialogs.DefaultBeIDCardsUI";

	private final Logger logger;
	private BeIDCardManager cardManager;
	private boolean cardManagerIsPrivate;
	private Map<CardTerminal, BeIDCard> beIDTerminalsAndCards;
	private Map<CardTerminal, Card> otherTerminalsAndCards;
	private Sleeper beIDSleeper, otherCardSleeper;
	private BeIDCardsUI ui;

	/*
	 * a BeIDCards with a default (void) logger and a private BeIDCardManager
	 * that is automatically started and stopped.
	 */
	public BeIDCards() {
		this(new VoidLogger());
	}

	/*
	 * a BeIDCards logging to logger, and a private BeIDCardManager that is
	 * automatically started and stopped.
	 */
	public BeIDCards(Logger logger) {
		this(logger, new BeIDCardManager());
		this.cardManagerIsPrivate = true;
		this.cardManager.start();
	}

	/*
	 * a BeIDCards with a default (void) logger, caller supplies a
	 * BeIDCardManager. note: caller is responsible for start()ing the supplied
	 * BeIDCardManager
	 */
	public BeIDCards(BeIDCardManager cardManager) {
		this(new VoidLogger(), cardManager);
	}

	/*
	 * a BeIDCards logging to logger, caller supplies a BeIDCardManager. note:
	 * caller is responsible for start()ing the supplied BeIDCardManager
	 */
	public BeIDCards(Logger logger, BeIDCardManager cardManager) {
		this.logger = logger;
		this.cardManager = cardManager;
		this.beIDSleeper = new Sleeper();
		this.beIDTerminalsAndCards = new HashMap<CardTerminal, BeIDCard>();

		this.cardManager.addBeIDCardEventListener(new BeIDCardEventsListener() {
			@Override
			public void eIDCardInserted(CardTerminal cardTerminal, BeIDCard card) {
				BeIDCards.this.logger.debug("eID Card Insertion Reported");
				synchronized (BeIDCards.this.beIDTerminalsAndCards) {
					BeIDCards.this.beIDTerminalsAndCards
							.put(cardTerminal, card);
					beIDSleeper.awaken();
				}
			}

			@Override
			public void eIDCardRemoved(CardTerminal cardTerminal, BeIDCard card) {
				BeIDCards.this.logger.debug("eID Card Removal Reported");
				synchronized (BeIDCards.this.beIDTerminalsAndCards) {
					BeIDCards.this.beIDTerminalsAndCards.remove(cardTerminal);
					beIDSleeper.awaken();
				}
			}
		});

		this.cardManager.addOtherCardEventListener(new CardEventsListener() {
			@Override
			public void cardInserted(CardTerminal cardTerminal, Card card) {
				BeIDCards.this.logger.debug("Other Card Insertion Reported");
				synchronized (BeIDCards.this.otherTerminalsAndCards) {
					BeIDCards.this.otherTerminalsAndCards.put(cardTerminal,
							card);
					otherCardSleeper.awaken();
				}
			}

			@Override
			public void cardRemoved(CardTerminal cardTerminal) {
				BeIDCards.this.logger.debug("Other Card Removal Reported");
				synchronized (BeIDCards.this.otherTerminalsAndCards) {
					BeIDCards.this.otherTerminalsAndCards.remove(cardTerminal);
					otherCardSleeper.awaken();
				}
			}
		});
	}

	public boolean hasBeIDCards() {
		boolean has;
		synchronized (this.beIDTerminalsAndCards) {
			has = (!beIDTerminalsAndCards.isEmpty());
		}
		return has;
	}

	public Set<BeIDCard> getBeIDCards() {
		waitForAtLeastOneBeIDCard();
		HashSet<BeIDCard> cards;
		synchronized (this.beIDTerminalsAndCards) {
			cards = new HashSet<BeIDCard>(this.beIDTerminalsAndCards.values());
		}
		return cards;
	}

	public BeIDCard getFirstBeIDCard() {
		waitForAtLeastOneBeIDCard();
		Set<BeIDCard> cards = getBeIDCards();
		return cards.iterator().next();
	}

	public BeIDCard waitForSingleBeIDCard() throws MultipleBeIDCardsException {
		waitForAtLeastOneBeIDCard();

		HashSet<BeIDCard> cards;
		synchronized (this.beIDTerminalsAndCards) {
			cards = new HashSet<BeIDCard>(this.beIDTerminalsAndCards.values());
		}

		if (cards.size() > 1)
			throw new MultipleBeIDCardsException();

		return cards.iterator().next();
	}

	/*
	 * call close() if you no longer need this BeIDCards instance. this stops a
	 * private cardManager, if present
	 */
	public BeIDCards close() throws InterruptedException {
		if (this.cardManagerIsPrivate)
			this.cardManager.stop();
		return this;
	}

	// ----------------------------------------------------------------------------------------------------------------------------------

	private BeIDCardsUI getUI() {
		if (this.ui == null) {
			if (GraphicsEnvironment.isHeadless()) {
				logger.error(UI_DEFAULT_REQUIRES_HEAD);
				throw new UnsupportedOperationException(
						UI_DEFAULT_REQUIRES_HEAD);
			}

			try {
				ClassLoader classLoader = BeIDCard.class.getClassLoader();
				Class<?> uiClass = classLoader
						.loadClass(DEFAULT_UI_IMPLEMENTATION);
				this.ui = (BeIDCardsUI) uiClass.newInstance();
			} catch (Exception e) {
				logger.error(UI_MISSING_LOG_MESSAGE);
				throw new UnsupportedOperationException(UI_MISSING_LOG_MESSAGE,
						e);
			}
		}

		return this.ui;
	}

	public void setUI(BeIDCardsUI ui) {
		this.ui = ui;
	}

	/*
	 * Private, supporting methods
	 * **********************************************
	 */

	private void waitForAtLeastOneBeIDCard() {
		if (!hasBeIDCards()) {
			try {
				getUI().adviseBeIDCardRequired();
				while (!hasBeIDCards()) {
					beIDSleeper.sleepUntilAwakened();
				}
			} finally {
				getUI().adviseEnd();
			}
		}
	}
}
