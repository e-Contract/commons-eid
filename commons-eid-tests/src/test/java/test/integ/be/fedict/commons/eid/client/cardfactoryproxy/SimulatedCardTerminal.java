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

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;

public class SimulatedCardTerminal extends CardTerminal {
	private String name;
	private SimulatedCard card;
	private SimulatedCardTerminals terminals;
	private Sleeper sleeper;

	// ---------------------------------------------------------

	public SimulatedCardTerminal(String name) {
		super();
		this.name = name;
		sleeper = new Sleeper();
	}

	public void insertCard(SimulatedCard card) {
		if (this.card != null)
			throw new RuntimeException(
					"Can't Insert 2 Cards in one Card Reader");
		this.card = card;
		sleeper.awaken();
		if (terminals != null)
			terminals.propagateCardEvent();
	}

	public void removeCard() {
		if (this.card == null)
			throw new RuntimeException("Can't Remove Card From Empty Reader");
		this.card = null;
		sleeper.awaken();
		if (terminals != null)
			terminals.propagateCardEvent();
	}

	// -----------------------------------------------------------

	@Override
	public Card connect(String protocol) throws CardException {
		if (!isCardPresent())
			throw new CardException("No Card Present");
		return card;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isCardPresent() throws CardException {
		return card != null;
	}

	@Override
	public boolean waitForCardAbsent(long timeout) throws CardException {
		return waitForCardState(false, timeout);
	}

	@Override
	public boolean waitForCardPresent(long timeout) throws CardException {
		return waitForCardState(true, timeout);
	}

	private boolean waitForCardState(boolean state, long timeout)
			throws CardException {
		if (isCardPresent() == state)
			return true;
		sleeper.sleepUntilAwakened(timeout);
		return true;
	}

	public void setTerminals(SimulatedCardTerminals terminals) {
		this.terminals = terminals;
	}
}
