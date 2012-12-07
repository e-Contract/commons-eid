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

/**
 * Interface to subscribe to a CardAndTerminalManager
 * 
 * @author Frank Marien
 * 
 */

package be.fedict.commons.eid.client.event;
import javax.smartcardio.Card;
import javax.smartcardio.CardTerminal;

/**
 * The CardEventsListener represents events delivered by a {@link CardAndTerminalManager}.
 * Register one or more instances of a class implementing CardEventsListener to
 * respond to any type of smartcards being inserted and removed.
 * @author Frank Marien
 */
public interface CardEventsListener {
	void cardEventsInitialized();
	void cardInserted(CardTerminal cardTerminal, Card card);
	void cardRemoved(CardTerminal cardTerminal);
}
