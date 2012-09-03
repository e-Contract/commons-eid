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

package be.fedict.commons.eid.client.spi;

import java.util.Collection;
import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.OutOfCardsException;
import be.fedict.commons.eid.client.CancelledException;

public interface BeIDCardsUI {
	// user needs to connect a Card Terminal
	void adviseCardTerminalRequired();

	// user needs to insert a BeID Card
	void adviseBeIDCardRequired();

	// user no longer needs to take action
	void adviseEnd();

	// user has multiple eID Cards inserted and needs to choose exactly one
	// throws CancelledException if user cancels
	// throws OutOfCardsException if all cards removed before selection could me made
	BeIDCard selectBeIDCard(Collection<BeIDCard> availableCards)
			throws CancelledException, OutOfCardsException;

	// user added a BeID card while selectBeIDCard() was blocking
	void eIDCardInsertedDuringSelection(BeIDCard card);

	// user removed a BeID card while selectBeIDCard() was blocking
	void eIDCardRemovedDuringSelection(BeIDCard card);
}
