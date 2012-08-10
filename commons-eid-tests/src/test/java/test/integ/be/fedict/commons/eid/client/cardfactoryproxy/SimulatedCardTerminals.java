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

package test.integ.be.fedict.commons.eid.client.cardfactoryproxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import be.fedict.commons.eid.client.spi.Sleeper;

public class SimulatedCardTerminals extends CardTerminals {
	private Set<SimulatedCardTerminal> terminals;
	private Sleeper sleeper;

	public SimulatedCardTerminals() {
		terminals = new HashSet<SimulatedCardTerminal>();
		sleeper = new Sleeper();
	}

	public synchronized SimulatedCardTerminals attachCardTerminal(
			SimulatedCardTerminal terminal) {
		terminal.setTerminals(this);
		terminals.add(terminal);
		sleeper.awaken();
		return this;
	}

	public synchronized SimulatedCardTerminals detachCardTerminal(
			SimulatedCardTerminal terminal) {
		terminal.setTerminals(null);
		terminals.remove(terminal);
		sleeper.awaken();
		return this;
	}

	public SimulatedCardTerminals propagateCardEvent() {
		sleeper.awaken();
		return this;
	}

	@Override
	public synchronized List<CardTerminal> list(State state)
			throws CardException {
		switch (state) {
			case ALL :
				return Collections
						.unmodifiableList(new ArrayList<CardTerminal>(terminals));

			case CARD_PRESENT : {
				ArrayList<CardTerminal> presentList = new ArrayList<CardTerminal>();
				for (CardTerminal terminal : terminals) {
					if (terminal.isCardPresent())
						presentList.add(terminal);
				}
				return Collections.unmodifiableList(presentList);
			}

			case CARD_ABSENT : {
				ArrayList<CardTerminal> absentList = new ArrayList<CardTerminal>();
				for (CardTerminal terminal : terminals) {
					if (!terminal.isCardPresent())
						absentList.add(terminal);
				}
				return Collections.unmodifiableList(absentList);
			}

			default :
				throw new CardException(
						"list with CARD_INSERTION or CARD_REMOVAL not supported in SimulatedCardTerminals");

		}
	}

	@Override
	public boolean waitForChange(long timeout) throws CardException {
		sleeper.sleepUntilAwakened(timeout);
		return true;
	}
}