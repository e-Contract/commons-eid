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
import javax.smartcardio.CardTerminal;
import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCardManager;
import be.fedict.commons.eid.client.event.BeIDCardEventsListener;

public class BeIDCardEventsExamples {

	/*
	 * get information about BeID cards being inserted and removed, while doing something else:
	 */
	public BeIDCardEventsExamples demonstrate_basic_asynchronous_usage()
			throws InterruptedException {
		//-------------------------------------------------------------------------------------------------------
		// instantiate a BeIDCardManager with default settings (no logging, private CardAndTerminalManager)
		//-------------------------------------------------------------------------------------------------------
		BeIDCardManager beIDCardManager = new BeIDCardManager();

		//-------------------------------------------------------------------------------------------------------	
		// register a BeIDCardManagerListener
		//-------------------------------------------------------------------------------------------------------
		beIDCardManager.addBeIDCardEventListener(new BeIDCardEventsListener() {
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
		});

		System.out
				.println("First, you'll see Inserted events for BeID Cards that were already inserted");

		//-------------------------------------------------------------------------------------------------------
		// start the BeIDCardManager instance.
		//-------------------------------------------------------------------------------------------------------
		beIDCardManager.start();

		//-------------------------------------------------------------------------------------------------------
		// the main thread goes off and does other things (for this example, just loop and sleep)
		//-------------------------------------------------------------------------------------------------------
		for (;;)
			Thread.sleep(2000);
	}

	//-------------------------------------------------------------------------------------------------------

	public static void main(String[] args) throws InterruptedException {
		BeIDCardEventsExamples examples = new BeIDCardEventsExamples();
		examples.demonstrate_basic_asynchronous_usage();
	}
}
