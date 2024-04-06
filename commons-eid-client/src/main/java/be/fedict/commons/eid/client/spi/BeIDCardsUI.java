/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
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

package be.fedict.commons.eid.client.spi;

import java.util.Collection;
import java.util.Locale;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.client.CancelledException;
import be.fedict.commons.eid.client.OutOfCardsException;

/**
 * implement a BeIDCardsUI to interact with the user with an instance of
 * {@link BeIDCards}.
 * 
 * @author Frank Marien
 * 
 */
public interface BeIDCardsUI {
	/**
	 * set Locale for subsequent operations. Implementations MUST ensure that after
	 * this call, any of the obtainXXX and adviseXXX methods for the same instance
	 * respect the locale set here. Implementations MAY choose to update any
	 * interface elements already facing the user at time of call, but this is not
	 * required.
	 * 
	 * @param newLocale
	 */
	void setLocale(Locale newLocale);

	/**
	 * get the Locale currently set.
	 * 
	 * @return the current Locale for this UI
	 */
	Locale getLocale();

	/**
	 * The user needs to connect a Card Terminal, since there are none
	 */
	void adviseCardTerminalRequired();

	/**
	 * The user needs to insert a BeID Card. There are card terminals, but none
	 * currently holds a BeID card.
	 * 
	 * @throws CancelledException
	 */
	void adviseBeIDCardRequired() throws CancelledException;

	/**
	 * The user needs to remove a BeID Card for security reasons.
	 */
	void adviseBeIDCardRemovalRequired();

	/**
	 * No more user actions are required, at this point.
	 */
	void adviseEnd();

	/**
	 * user has multiple eID Cards inserted and needs to choose exactly one. throws
	 * CancelledException if user cancels throws OutOfCardsException if all cards
	 * removed before selection could be made.
	 * 
	 * @param availableCards
	 * @return
	 * @throws be.fedict.commons.eid.client.CancelledException
	 * @throws be.fedict.commons.eid.client.OutOfCardsException
	 */
	BeIDCard selectBeIDCard(Collection<BeIDCard> availableCards) throws CancelledException, OutOfCardsException;

	/**
	 * user added a BeID card while selectBeIDCard() was blocking. An implementation
	 * should update the list of cards, if possible.
	 * 
	 * @param card the card just inserted.
	 */
	void eIDCardInsertedDuringSelection(BeIDCard card);

	/**
	 * user removed a BeID card while selectBeIDCard() was blocking. An
	 * implementation should update the list of cards, if possible.
	 * 
	 * @param card the card just removed.
	 */
	void eIDCardRemovedDuringSelection(BeIDCard card);
}
