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

package be.fedict.commons.eid.client.impl;

import be.fedict.commons.eid.client.spi.BeIDCardUI;

public class MissingUI implements BeIDCardUI {

	@Override
	public char[] obtainPIN(int triesLeft) {
		throw new UnsupportedOperationException();
	}

	@Override
	public char[][] obtainOldAndNewPIN(int triesLeft) {
		throw new UnsupportedOperationException();
	}

	@Override
	public char[][] obtainPUKCodes(int triesLeft) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void advisePINChanged() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void advisePINBlocked() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void advisePINUnblocked() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void advisePINPadPINEntry(int retriesLeft) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void advisePINPadPUKEntry(int retriesLeft) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void advisePINPadChangePIN(int retriesLeft) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void advisePINPadOldPINEntry(int retriesLeft) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void advisePINPadNewPINEntry(int retriesLeft) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void advisePINPadNewPINEntryAgain(int retriesLeft) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void advisePINPadOperationEnd() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void adviseSecureReaderOperation() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void adviseSecureReaderOperationEnd() {
		throw new UnsupportedOperationException();
	}
}
