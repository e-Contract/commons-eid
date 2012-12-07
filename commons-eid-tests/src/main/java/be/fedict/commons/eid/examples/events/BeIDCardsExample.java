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

import java.io.IOException;
import java.util.Set;
import javax.smartcardio.CardException;
import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.client.BeIDCardsException;
import be.fedict.commons.eid.client.CancelledException;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.consumer.Identity;
import be.fedict.commons.eid.consumer.tlv.TlvParser;

public class BeIDCardsExample {
	/*
	 * get information about BeID cards inserted, from the current thread:
	 */
	public void demonstrate() throws InterruptedException {
		// -------------------------------------------------------------------------------------------------------
		// instantiate a BeIDCardManager with default settings (no logging)
		// -------------------------------------------------------------------------------------------------------
		final BeIDCards beIDCards = new BeIDCards();

		// -------------------------------------------------------------------------------------------------------
		// ask it for all CardTerminals that currently contain BeID cards (which
		// may be none at all)
		// -------------------------------------------------------------------------------------------------------
		final Set<BeIDCard> cards = beIDCards.getAllBeIDCards();
		System.out.println(cards.size() + " BeID Cards found");
		// -------------------------------------------------------------------------------------------------------
		// ask it for one BeID Card. This may block and interact with the user.
		// -------------------------------------------------------------------------------------------------------

		try {
			final BeIDCard card = beIDCards.getOneBeIDCard();

			// -------------------------------------------------------------------------------------------------------
			// read identity file, decode it and print something containing card holder's first name
			// -------------------------------------------------------------------------------------------------------
			try {
				final byte[] idData = card.readFile(FileType.Identity);
				final Identity id = TlvParser.parse(idData, Identity.class);
				System.out.println(id.firstName + "'s card");
			} catch (final CardException cex) {
				// TODO Auto-generated catch block
				cex.printStackTrace();
			} catch (final IOException iox) {
				// TODO Auto-generated catch block
				iox.printStackTrace();
			}

			// -------------------------------------------------------------------------------------------------------
			// wait for removal of the card we've just read.
			// -------------------------------------------------------------------------------------------------------
			System.out.println("Please remove the card now.");
			beIDCards.waitUntilCardRemoved(card);
			System.out.println("Thank you.");
		} catch (final CancelledException cex) {
			System.out.println("Cancelled By User");
		}

	}

	public static void main(final String[] args) throws InterruptedException,
			BeIDCardsException {
		final BeIDCardsExample examples = new BeIDCardsExample();
		examples.demonstrate();
	}

}
