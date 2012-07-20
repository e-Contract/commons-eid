package be.fedict.commons.eid.client;

public interface Dialogs {
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
}
