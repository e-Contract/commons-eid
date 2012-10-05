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

package be.fedict.commons.eid.jca;

import java.security.KeyStore;
import java.security.KeyStore.ProtectionParameter;

import be.fedict.commons.eid.client.BeIDCard;

/**
 * An eID specific {@link KeyStore} parameter.
 * 
 * @see KeyStore
 * @author Frank Cornelis
 * 
 */
public class BeIDKeyStoreParameter implements KeyStore.LoadStoreParameter {

	private BeIDCard beIDCard;

	private boolean logoff;

	@Override
	public ProtectionParameter getProtectionParameter() {
		return null;
	}

	/**
	 * Sets the {@link BeIDCard} to be used by the corresponding
	 * {@link KeyStore}.
	 * 
	 * @param beIDCard
	 */
	public void setBeIDCard(final BeIDCard beIDCard) {
		this.beIDCard = beIDCard;
	}

	public BeIDCard getBeIDCard() {
		return this.beIDCard;
	}

	/**
	 * Set to <code>true</code> if you want an eID logoff to be issued after
	 * each PIN entry.
	 * 
	 * @param logoff
	 */
	public void setLogoff(final boolean logoff) {
		this.logoff = logoff;
	}

	public boolean getLogoff() {
		return this.logoff;
	}
}
