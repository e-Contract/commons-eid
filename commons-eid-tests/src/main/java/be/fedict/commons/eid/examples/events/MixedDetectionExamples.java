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

import java.math.BigInteger;
import javax.smartcardio.Card;
import javax.smartcardio.CardTerminal;
import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCardManager;
import be.fedict.commons.eid.client.CardAndTerminalManager;
import be.fedict.commons.eid.client.event.BeIDCardEventsListener;
import be.fedict.commons.eid.client.event.CardEventsListener;
import be.fedict.commons.eid.client.event.CardTerminalEventsListener;

/*
 * mixed asynchronous detection of CardTerminals, BeID and non-BeID cards,
 * using a BeIDCardManager with your own CardAndTerminalManager
 */
public class MixedDetectionExamples
		implements
			BeIDCardEventsListener,
			CardEventsListener,
			CardTerminalEventsListener {
	private void demonstrate() throws InterruptedException {
		//-------------------------------------------------------------------------------------------------------
		// instantiate a CardAndTerminalManager with default settings (no logging, default timeout)
		//-------------------------------------------------------------------------------------------------------
		CardAndTerminalManager cardAndTerminalManager = new CardAndTerminalManager();

		//-------------------------------------------------------------------------------------------------------
		// instantiate a BeIDCardManager, pass it our CardAndTerminalManager to use
		//-------------------------------------------------------------------------------------------------------
		BeIDCardManager beIDCardManager = new BeIDCardManager(
				cardAndTerminalManager);

		//-------------------------------------------------------------------------------------------------------	
		// register ourselves as BeIDCardEventsListener to get BeID card insert and remove events
		//-------------------------------------------------------------------------------------------------------
		beIDCardManager.addBeIDCardEventListener(this);

		//-------------------------------------------------------------------------------------------------------	
		// register ourselves as CardEventsListener to the BeIDCardManager, to get events of *other* cards
		// being inserted/removed (if we would register ourselves to the CardAndTerminalManager
		// for this, we would get 2 events when a BeID was inserted, one for the BeID, one for the Card by itself,
		// because CardAndTerminalManager cannot distinguish between them, and BeIDCardManager can)
		//-------------------------------------------------------------------------------------------------------
		beIDCardManager.addOtherCardEventListener(this);
		//  ^^^^^^^^^^^^^^^  // see above	

		//-------------------------------------------------------------------------------------------------------	
		// register ourselves as CardTerminalEventsListener to get CardTerminal attach and detach events
		//-------------------------------------------------------------------------------------------------------
		cardAndTerminalManager.addCardTerminalListener(this);

		System.out
				.println("First, you'll see events for terminals and Cards that were already present");

		//-------------------------------------------------------------------------------------------------------
		// start the BeIDCardManager instance
		//-------------------------------------------------------------------------------------------------------
		beIDCardManager.start();

		//-------------------------------------------------------------------------------------------------------
		// start the CardAndTerminalManager
		//-------------------------------------------------------------------------------------------------------
		cardAndTerminalManager.start();

		//-------------------------------------------------------------------------------------------------------
		// the main thread goes off and does other things (for this example, just loop and sleep)
		//-------------------------------------------------------------------------------------------------------
		for (;;)
			Thread.sleep(2000);
	}

	//------------------------------------------------------------------------------------------------------------

	//------------------------------------------------------------------------------------------------------------
	// these respond to CardTerminals being attached and detached
	//------------------------------------------------------------------------------------------------------------

	@Override
	public void terminalAttached(CardTerminal cardTerminal) {
		System.out.println("CardTerminal [" + cardTerminal.getName()
				+ "] attached\n");
	}

	@Override
	public void terminalDetached(CardTerminal cardTerminal) {
		System.out.println("CardTerminal [" + cardTerminal.getName()
				+ "] detached\n");
	}

	@Override
	public void terminalException(Throwable throwable) {
		System.out.println("Exception [" + throwable.getLocalizedMessage()
				+ "]\n");
	}

	@Override
	public void terminalEventsInitialized() {
		System.out
				.println("From now on you'll see terminals being Attached/Detached");
	}

	//------------------------------------------------------------------------------------------------------------
	// these respond to BeID cards being inserted and removed
	//------------------------------------------------------------------------------------------------------------

	@Override
	public void eIDCardRemoved(CardTerminal cardTerminal, BeIDCard card) {
		System.out.println("BeID Card Removed From Card Termimal ["
				+ cardTerminal.getName() + "]\n");
	}

	@Override
	public void eIDCardInserted(CardTerminal cardTerminal, BeIDCard card) {
		System.out.println("BeID Card Inserted Into Card Termimal ["
				+ cardTerminal.getName() + "]\n");
	}

	@Override
	public void eIDCardEventsInitialized() {
		System.out
				.println("From now on you'll see BeID Cards being Inserted/Removed");
	}

	//------------------------------------------------------------------------------------------------------------
	// these respond to non-BeID cards being inserted and removed
	// (because we registered with a BeIDCardManager, not a CardAndTerminalManager)
	//------------------------------------------------------------------------------------------------------------

	@Override
	public void cardInserted(CardTerminal cardTerminal, Card card) {
		if (card != null)
			System.out.println("Other Card ["
					+ String.format("%x", new BigInteger(1, card.getATR()
							.getBytes())) + "] Inserted Into Terminal ["
					+ cardTerminal.getName() + "]");
		else
			System.out.println("Other Card Inserted Into Terminal ["
					+ cardTerminal.getName() + "] but failed to connect()");
	}

	@Override
	public void cardRemoved(CardTerminal cardTerminal) {
		System.out.println("Other Card Removed From [" + cardTerminal.getName()
				+ "]");
	}

	@Override
	public void cardEventsInitialized() {
		System.out
				.println("From now on you'll see Non-BeID Cards being Inserted/Removed");
	}

	//-------------------------------------------------------------------------------------------------------

	public static void main(String[] args) throws InterruptedException {
		new MixedDetectionExamples().demonstrate();
	}

}
