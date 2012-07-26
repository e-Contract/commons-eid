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

package be.fedict.commons.eid.examples.events;

import java.util.Set;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import be.fedict.commons.eid.client.CardAndTerminalEventsManager;
import be.fedict.commons.eid.client.event.CardTerminalEventsListener;

public class TerminalEventsExamples {
	/*
	 * get information about CardTerminals currently attached, from the current thread:
	 */
	public void cardterminals_basic_synchronous() {
		//-------------------------------------------------------------------------------------------------------
		// instantiate a CardAndTerminalEventsManager with default settings (no logging, default timeout)
		//-------------------------------------------------------------------------------------------------------
		CardAndTerminalEventsManager cardAndTerminalEventsManager = new CardAndTerminalEventsManager();

		//-------------------------------------------------------------------------------------------------------
		// ask it for all CardTerminals that are currently attached
		//-------------------------------------------------------------------------------------------------------
		Set<CardTerminal> terminalsAttached = null;

		try {
			terminalsAttached = cardAndTerminalEventsManager
					.getTerminalsPresent();
		} catch (CardException cex) {
			System.out.println("Oops! Failed to get list of CardTerminals:"
					+ cex.getLocalizedMessage());
		}

		//-------------------------------------------------------------------------------------------------------
		// either say there are none, or if there are, list them
		//-------------------------------------------------------------------------------------------------------
		if (terminalsAttached == null || terminalsAttached.isEmpty()) {
			System.out.println("No CardTerminals Found");
		} else {
			System.out.println("Terminals Attached:");

			for (CardTerminal terminal : terminalsAttached) {
				System.out.println("-" + terminal.getName());
			}
		}
	}

	/*
	 * get information about CardTerminals being attached and detached, while doing something else:
	 */
	public TerminalEventsExamples cardterminals_basic_asynchronous()
			throws InterruptedException {
		//-------------------------------------------------------------------------------------------------------
		// instantiate a CardAndTerminalEventsManager with default settings (no logging, default timeout)
		//-------------------------------------------------------------------------------------------------------
		CardAndTerminalEventsManager cardAndTerminalEventsManager = new CardAndTerminalEventsManager();

		//-------------------------------------------------------------------------------------------------------	
		// register a CardTerminalEventsListener
		//-------------------------------------------------------------------------------------------------------
		cardAndTerminalEventsManager
				.addCardTerminalListener(new CardTerminalEventsListener() {
					@Override
					public void terminalException(Throwable throwable) {
						System.out.println("Exception ["
								+ throwable.getLocalizedMessage() + "]\n");
					}

					@Override
					public void terminalDetached(CardTerminal cardTerminal) {
						System.out.println("CardTerminal ["
								+ cardTerminal.getName() + "] detached\n");
					}

					@Override
					public void terminalAttached(CardTerminal cardTerminal) {
						System.out.println("CardTerminal ["
								+ cardTerminal.getName() + "] attached\n");
					}
				});

		//-------------------------------------------------------------------------------------------------------
		// start the CardAndTerminalEventsManager instance running as a daemon thread.
		//-------------------------------------------------------------------------------------------------------
		cardAndTerminalEventsManager.start();

		//-------------------------------------------------------------------------------------------------------
		// the main thread goes off and does other things (for this example, just loop and sleep)
		//-------------------------------------------------------------------------------------------------------
		System.err.println("Now.. attach and detach some cardterminals..");
		for (;;)
			Thread.sleep(2000);
	}

	//-------------------------------------------------------------------------------------------------------

	public static void main(String[] args) throws InterruptedException {
		TerminalEventsExamples examples = new TerminalEventsExamples();
		examples.cardterminals_basic_synchronous();
		examples.cardterminals_basic_asynchronous();
	}
}
