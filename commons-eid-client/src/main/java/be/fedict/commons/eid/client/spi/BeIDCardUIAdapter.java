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
	public void advisePINPadPINEntry(final int retriesLeft) {
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
