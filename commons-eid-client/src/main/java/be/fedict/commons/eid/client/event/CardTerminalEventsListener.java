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

package be.fedict.commons.eid.client.event;

import javax.smartcardio.CardTerminal;

/**
 * The CardTerminalEventsListener represents events delivered by a {@link CardAndTerminalManager}.
 * Register one or more instances of a class implementing CardTerminalEventsListener to
 * respond to any type of Card Terminals being attached and detached.
 * @author Frank Marien
 */
public interface CardTerminalEventsListener {
	void terminalEventsInitialized();
	void terminalAttached(CardTerminal cardTerminal);
	void terminalDetached(CardTerminal cardTerminal);
}
