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

import be.fedict.commons.eid.client.PINPurpose;

/**
 * Implement a BeIDCardUI to interact with the user from a BeIDCard instance.
 * 
 * @author Frank Marien
 *
 */
public interface BeIDCardUI {
	/**
	 * get PIN from the user
	 * @param triesLeft the number of attempts left before the PIN is blocked.
	 * @param type the reason why the PIN code is requested
	 * @return the PIN code.
	 * @throws UserCancelledException thrown in case the user cancels the PIN entry.
	 */
	char[] obtainPIN(int triesLeft, PINPurpose type)
			throws UserCancelledException;

	/**
	 * get Old and New PIN from the user. (pin change)
	 * @param triesLeft the number of attempts left before the PIN is blocked.
	 * @return old and new PIN codes
	 */
	char[][] obtainOldAndNewPIN(int triesLeft);

	/**
	 * get PUK Codes from the user (PIN unblock)
	 * @param triesLeft the number of attempts left before the card is blocked.
	 * @return PUK1 and PUK2 codes
	 */
	char[][] obtainPUKCodes(int triesLeft);

	/**
	 * PIN was changed successfully
	 */
	void advisePINChanged();

	/**
	 * too many tries. PIN now blocked
	 */
	void advisePINBlocked();

	/**
	 * PIN unblock operation was successful.
	 */
	void advisePINUnblocked();

	/**
	 * user can enter PIN on Secure PINPad
	 * @param retriesLeft the number of attempts left before the PIN is blocked.
	 * @param type the reason why the PIN code is requested
	 */
	void advisePINPadPINEntry(int retriesLeft, PINPurpose type);

	/**
	 * user can enter PUK on PINPad
	 * @param retriesLeft the number of attempts left before the card is blocked.
	 */
	void advisePINPadPUKEntry(int retriesLeft);

	/**
	 * user can change PIN (old, new, new-again) on PIN Pad
	 * @param retriesLeft the number of attempts left before the PIN is blocked.
	 */
	void advisePINPadChangePIN(int retriesLeft);

	/**
	 * user can enter old PIN on PINPad
	 * @param retriesLeft the number of attempts left before the PIN is blocked.
	 */
	void advisePINPadOldPINEntry(int retriesLeft);

	/**
	 * user can enter new PIN on PINPad
	 * @param retriesLeft the number of attempts left before the PIN is blocked.
	 */
	void advisePINPadNewPINEntry(int retriesLeft);

	/**
	 * user can enter new PIN on PINPad again
	 * @param retriesLeft the number of attempts left before the PIN is blocked.
	 */
	void advisePINPadNewPINEntryAgain(int retriesLeft);

	/**
	 * one of the above PINPad operation ends
	 */
	void advisePINPadOperationEnd();

	/**
	 * user needs to attend some operation on a secure reader
	 * (more instuctions are available from the reader's own user interface)
	 */
	void adviseSecureReaderOperation();

	/**
	 * operation on secure reader ends
	 */
	void adviseSecureReaderOperationEnd();
}
