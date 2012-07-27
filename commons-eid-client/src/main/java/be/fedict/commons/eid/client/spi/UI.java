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

public interface UI {
	// get PIN from the user
	char[] obtainPIN(int triesLeft);

	// get Old and New PIN from the user
	char[][] obtainOldAndNewPIN(int triesLeft);

	// get PUK Codes from the user
	char[][] obtainPUKCodes(int triesLeft);

	// PIN changed successfully
	void advisePINChanged();

	// too many tries. PIN now blocked
	void advisePINBlocked();

	// PIN unblock operation successful.
	void advisePINUnblocked();

	// user can enter PIN on PINPad
	void advisePINPadPINEntry(int retriesLeft);

	// user can enter PUK on PINPad
	void advisePINPadPUKEntry(int retriesLeft);

	// user can change PIN (old, new, new-again) on PIN Pad
	void advisePINPadChangePIN(int retriesLeft);

	// user can enter old PIN on PINPad
	void advisePINPadOldPINEntry(int retriesLeft);

	// user can enter new PIN on PINPad
	void advisePINPadNewPINEntry(int retriesLeft);

	// user can enter new PIN on PINPad again
	void advisePINPadNewPINEntryAgain(int retriesLeft);

	// one of the above PINPad operation ends
	void advisePINPadOperationEnd();

	// user needs to attend some operation on a secure reader
	void adviseSecureReaderOperation();

	// operation on secure reader ends
	void adviseSecureReaderOperationEnd();
}
