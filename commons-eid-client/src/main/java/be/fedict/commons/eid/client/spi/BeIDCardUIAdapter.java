package be.fedict.commons.eid.client.spi;

import be.fedict.commons.eid.client.PINPurpose;

public class BeIDCardUIAdapter implements BeIDCardUI {

	private static final String OPERATION_CANCELLED = "operation cancelled.";

	@Override
	public char[] obtainPIN(int triesLeft, PINPurpose type) {
		throw new RuntimeException(OPERATION_CANCELLED);
	}

	@Override
	public char[][] obtainOldAndNewPIN(int triesLeft) {
		throw new RuntimeException(OPERATION_CANCELLED);
	}

	@Override
	public char[][] obtainPUKCodes(int triesLeft) {
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
	public void advisePINPadPINEntry(int retriesLeft) {
	}

	@Override
	public void advisePINPadPUKEntry(int retriesLeft) {
	}

	@Override
	public void advisePINPadChangePIN(int retriesLeft) {
	}

	@Override
	public void advisePINPadOldPINEntry(int retriesLeft) {
	}

	@Override
	public void advisePINPadNewPINEntry(int retriesLeft) {
	}

	@Override
	public void advisePINPadNewPINEntryAgain(int retriesLeft) {
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
