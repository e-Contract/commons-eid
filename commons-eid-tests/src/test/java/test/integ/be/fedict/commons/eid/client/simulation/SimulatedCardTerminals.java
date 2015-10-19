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

package test.integ.be.fedict.commons.eid.client.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;

public class SimulatedCardTerminals extends CardTerminals {
	private final Set<SimulatedCardTerminal> terminals;

	public SimulatedCardTerminals() {
		this.terminals = new HashSet<SimulatedCardTerminal>();
	}

	public synchronized SimulatedCardTerminals attachCardTerminal(
			final SimulatedCardTerminal terminal) {
		terminal.setTerminals(this);
		this.terminals.add(terminal);
		notifyAll();
		return this;
	}

	public synchronized SimulatedCardTerminals detachCardTerminal(
			final SimulatedCardTerminal terminal) {
		terminal.setTerminals(null);
		this.terminals.remove(terminal);
		notifyAll();
		return this;
	}

	public synchronized SimulatedCardTerminals propagateCardEvent() {
		notifyAll();
		return this;
	}

	@Override
	public synchronized List<CardTerminal> list(final State state)
			throws CardException {
		switch (state) {
			case ALL :
				return Collections
						.unmodifiableList(new ArrayList<CardTerminal>(
								this.terminals));

			case CARD_PRESENT : {
				final ArrayList<CardTerminal> presentList = new ArrayList<CardTerminal>();
				for (CardTerminal terminal : this.terminals) {
					if (terminal.isCardPresent()) {
						presentList.add(terminal);
					}
				}
				return Collections.unmodifiableList(presentList);
			}

			case CARD_ABSENT : {
				final ArrayList<CardTerminal> absentList = new ArrayList<CardTerminal>();
				for (CardTerminal terminal : this.terminals) {
					if (!terminal.isCardPresent()) {
						absentList.add(terminal);
					}
				}
				return Collections.unmodifiableList(absentList);
			}

			default :
				throw new CardException(
						"list with CARD_INSERTION or CARD_REMOVAL not supported in SimulatedCardTerminals");

		}
	}

	@Override
	public synchronized boolean waitForChange(final long timeout)
			throws CardException {
		try {
			wait(timeout);
		} catch (final InterruptedException iex) {
			return false;
		}
		return true;
	}
}