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
import java.util.Locale;

import be.fedict.commons.eid.client.BeIDCard;

/**
 * An adapter implementing empty or simple implementations of the BeIDCardsUI interface.
 * Intended to be extended by a useful class without having to implement unused methods.
 * For example, in an embedded application where only one card reader is possible,
 * only adviseBeIDCardRequired() and adviseEnd() would ever be called.
 * @author Frank Marien
 *
 */
public class BeIDCardsIUAdapter implements BeIDCardsUI {

	@Override
	public void setLocale(Locale newLocale) {
	}

	@Override
	public void adviseCardTerminalRequired() {
	}

	@Override
	public void adviseBeIDCardRequired() {
	}

	@Override
	public void adviseBeIDCardRemovalRequired() {
	}

	@Override
	public BeIDCard selectBeIDCard(final Collection<BeIDCard> availableCards) {
		return availableCards.iterator().next();
	}

	@Override
	public void eIDCardInsertedDuringSelection(final BeIDCard card) {
	}

	@Override
	public void eIDCardRemovedDuringSelection(final BeIDCard card) {
	}

	@Override
	public void adviseEnd() {
	}
}
