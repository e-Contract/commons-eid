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
import be.fedict.commons.eid.client.BeIDCardEventsListener;
import be.fedict.commons.eid.client.BeIDCardEventsManager;
import be.fedict.commons.eid.client.CardAndTerminalEventsManager;
import be.fedict.commons.eid.client.CardEventsListener;
import be.fedict.commons.eid.client.CardTerminalEventsListener;

/*
 * mixed asynchronous detection of CardTerminals, BeID and non-BeID cards,
 * using a BeIDCardEventsManager with your own CardAndTerminalEventsManager
 */
public class MixedDetectionExamples
		implements
			BeIDCardEventsListener,
			CardEventsListener,
			CardTerminalEventsListener {
	private void demonstrate() throws InterruptedException {
		//-------------------------------------------------------------------------------------------------------
		// instantiate a CardAndTerminalEventsManager with default settings (no logging, default timeout)
		//-------------------------------------------------------------------------------------------------------
		CardAndTerminalEventsManager cardAndTerminalEventsManager = new CardAndTerminalEventsManager();

		//-------------------------------------------------------------------------------------------------------
		// instantiate a BeIDCardManager, pass it our CardAndTerminalEventsManager to use
		//-------------------------------------------------------------------------------------------------------
		BeIDCardEventsManager beIDCardManager = new BeIDCardEventsManager(
				cardAndTerminalEventsManager);

		//-------------------------------------------------------------------------------------------------------	
		// register ourselves as BeIDCardEventsListener to get BeID card insert and remove events
		//-------------------------------------------------------------------------------------------------------
		beIDCardManager.addBeIDCardEventListener(this);

		//-------------------------------------------------------------------------------------------------------	
		// register ourselves as CardEventsListener to the BeIDCardEventsManager, to get events of *other* cards
		// being inserted/removed (if we would register ourselves to the CardAndTerminalEventsManager
		// for this, we would get 2 events when a BeID was inserted, one for the BeID, one for the Card by itself,
		// because CardAndTerminalEventsManager cannot distinguish between them, and BeIDCardEventsManager can)
		//-------------------------------------------------------------------------------------------------------
		beIDCardManager.addOtherCardEventListener(this);
		//  ^^^^^^^^^^^^^^^  // see above	

		//-------------------------------------------------------------------------------------------------------	
		// register ourselves as CardTerminalEventsListener to get CardTerminal attach and detach events
		//-------------------------------------------------------------------------------------------------------
		cardAndTerminalEventsManager.addCardTerminalListener(this);

		//-------------------------------------------------------------------------------------------------------
		// start the BeIDCardManager instance
		//-------------------------------------------------------------------------------------------------------
		beIDCardManager.start();

		//-------------------------------------------------------------------------------------------------------
		// start the CardAndTerminalEventsManager
		//-------------------------------------------------------------------------------------------------------
		cardAndTerminalEventsManager.start();

		//-------------------------------------------------------------------------------------------------------
		// the main thread goes off and does other things (for this example, just loop and sleep)
		//-------------------------------------------------------------------------------------------------------
		System.out
				.println("Now.. attach, detach terminals, insert cards of all kinds..");
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

	//------------------------------------------------------------------------------------------------------------
	// these respond to non-BeID cards being inserted and removed
	// (because we registered with a BeIDCardEventsManager, not a CardAndTerminalEventsManager)
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

	//-------------------------------------------------------------------------------------------------------

	public static void main(String[] args) throws InterruptedException {
		new MixedDetectionExamples().demonstrate();
	}
}
