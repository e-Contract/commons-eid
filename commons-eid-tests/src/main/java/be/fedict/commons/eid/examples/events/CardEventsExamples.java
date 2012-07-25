package be.fedict.commons.eid.examples.events;

import java.math.BigInteger;
import java.util.Set;
import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import be.fedict.commons.eid.client.CardAndTerminalEventsManager;
import be.fedict.commons.eid.client.CardEventsListener;

public class CardEventsExamples {
	/*
	 * get information about Smart Cards currently inserted, from the current thread:
	 */
	public void cardterminals_basic_synchronous() {
		//-------------------------------------------------------------------------------------------------------
		// instantiate a CardAndTerminalEventsManager with default settings (no logging, default timeout)
		//-------------------------------------------------------------------------------------------------------
		CardAndTerminalEventsManager cardAndTerminalEventsManager = new CardAndTerminalEventsManager();

		//-------------------------------------------------------------------------------------------------------
		// ask it for all CardTerminals that are currently attached and that have cards inserted
		//-------------------------------------------------------------------------------------------------------
		Set<CardTerminal> terminalsWithCards = null;

		try {
			terminalsWithCards = cardAndTerminalEventsManager
					.getTerminalsWithCards();
		} catch (CardException cex) {
			System.out
					.println("Oops! Failed to get list of CardTerminals With Cards:"
							+ cex.getLocalizedMessage());
		}

		//-------------------------------------------------------------------------------------------------------
		// either say there are none, or if there are, list them
		//-------------------------------------------------------------------------------------------------------
		if (terminalsWithCards == null || terminalsWithCards.isEmpty()) {
			System.out.println("No CardTerminals With Cards Found");
		} else {
			System.out.println("Terminals With Cards Inserted:");

			for (CardTerminal terminal : terminalsWithCards) {
				System.out.println("-" + terminal.getName());
			}
		}
	}

	/*
	 * get information about Cards being inserted and removed, while doing something else:
	 */
	public CardEventsExamples cardterminals_basic_asynchronous()
			throws InterruptedException {
		//-------------------------------------------------------------------------------------------------------
		// instantiate a CardAndTerminalEventsManager with default settings (no logging, default timeout)
		//-------------------------------------------------------------------------------------------------------
		CardAndTerminalEventsManager cardAndTerminalEventsManager = new CardAndTerminalEventsManager();

		//-------------------------------------------------------------------------------------------------------	
		// register a CardEventsListener
		//-------------------------------------------------------------------------------------------------------
		cardAndTerminalEventsManager.addCardListener(new CardEventsListener() {
			@Override
			public void cardInserted(CardTerminal cardTerminal, Card card) {
				if (card != null)
					System.err.println("Card ["
							+ String.format("%x", new BigInteger(1, card
									.getATR().getBytes()))
							+ "] Inserted Into Terminal ["
							+ cardTerminal.getName() + "]");
				else
					System.err.println("Card present but failed to connect()");
			}

			@Override
			public void cardRemoved(CardTerminal cardTerminal) {
				System.err.println("Card Removed From ["
						+ cardTerminal.getName() + "]");
			}
		});

		//-------------------------------------------------------------------------------------------------------
		// start the CardAndTerminalEventsManager instance running as a daemon thread.
		//-------------------------------------------------------------------------------------------------------
		cardAndTerminalEventsManager.start();

		//-------------------------------------------------------------------------------------------------------
		// the main thread goes off and does other things (for this example, just loop and sleep)
		//-------------------------------------------------------------------------------------------------------
		System.err.println("Now.. insert and remove some cards");
		for (;;)
			Thread.sleep(2000);
	}

	//-------------------------------------------------------------------------------------------------------

	public static void main(String[] args) throws InterruptedException {
		CardEventsExamples examples = new CardEventsExamples();
		examples.cardterminals_basic_synchronous();
		examples.cardterminals_basic_asynchronous();
	}
}
