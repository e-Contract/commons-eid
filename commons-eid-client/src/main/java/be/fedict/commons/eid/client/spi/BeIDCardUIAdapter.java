/*
 * Commons eID Project.
 * Copyright (C) 2012 FedICT.
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

import be.fedict.commons.eid.client.PINPurpose;

public class BeIDCardUIAdapter implements BeIDCardUI {

	private static final String OPERATION_CANCELLED = "operation cancelled.";

	@Override
	public char[] obtainPIN(final int triesLeft, final PINPurpose type) {
		throw new RuntimeException(OPERATION_CANCELLED);
	}

	@Override
	public char[][] obtainOldAndNewPIN(final int triesLeft) {
		throw new RuntimeException(OPERATION_CANCELLED);
	}

	@Override
	public char[][] obtainPUKCodes(final int triesLeft) {
		throw new RuntimeException(OPERATION_CANCELLED);
	}

	@Override
	public void advisePINChanged() {
	}

	@Override
	public void advisePINBlocked() {
	}

	@Override
	public void advisePINUnblocked() {
	}

	@Override
	public void advisePINPadPINEntry(final int retriesLeft, PINPurpose purpose) {
	}

	@Override
	public void advisePINPadPUKEntry(final int retriesLeft) {
	}

	@Override
	public void advisePINPadChangePIN(final int retriesLeft) {
	}

	@Override
	public void advisePINPadOldPINEntry(final int retriesLeft) {
	}

	@Override
	public void advisePINPadNewPINEntry(final int retriesLeft) {
	}

	@Override
	public void advisePINPadNewPINEntryAgain(final int retriesLeft) {
	}

	@Override
	public void advisePINPadOperationEnd() {
	}

	@Override
	public void adviseSecureReaderOperation() {
	}

	@Override
	public void adviseSecureReaderOperationEnd() {
	}
}
