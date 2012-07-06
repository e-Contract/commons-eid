package test.integ.be.fedict.commons.eid.client.cardfactoryproxy;

import java.util.Random;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class ErrorCapableBeIdCard extends SimulatedBeIDCard {
	protected static final ResponseAPDU TOOFAST = new ResponseAPDU(new byte[]{
			0x6c, (byte) 0x00});

	private boolean nextTooFast;
	private boolean nextBitError;
	private boolean nextRandomResponse;
	private boolean nextCardException;
	private int delay;
	private Random random;

	public ErrorCapableBeIdCard(String profile) {
		this(profile, System.currentTimeMillis());
	}

	public ErrorCapableBeIdCard(String profile, long seed) {
		super(profile);
		this.random = new Random(seed);
		this.delay = 0;
	}

	public ErrorCapableBeIdCard introduceTooFastError() {
		this.nextTooFast = true;
		return this;
	}

	public ErrorCapableBeIdCard introduceBitError() {
		this.nextBitError = true;
		return this;
	}

	public ErrorCapableBeIdCard introduceRandomResponse() {
		this.nextRandomResponse = true;
		return this;
	}

	public ErrorCapableBeIdCard introduceCardException() {
		this.nextCardException = true;
		return this;
	}

	public int getDelay() {
		return delay;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}

	@Override
	protected ResponseAPDU transmit(CommandAPDU apdu) throws CardException {
		if (nextCardException) {
			nextCardException = false;
			throw new CardException("Fake CardException Introduced By "
					+ this.getClass().getName());
		}

		if (nextTooFast) {
			nextTooFast = false;
			return TOOFAST;
		}

		if (nextRandomResponse) {
			nextRandomResponse = false;
			byte[] randomAPDU = new byte[16];
			random.nextBytes(randomAPDU);
			return new ResponseAPDU(randomAPDU);
		}

		if (delay > 0) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
			}
		}

		if (nextBitError) {
			ResponseAPDU response = super.transmit(apdu);
			byte[] responseBytes = response.getBytes();

			// flip some bits
			responseBytes[random.nextInt(responseBytes.length)] ^= (byte) random
					.nextInt();

			return new ResponseAPDU(responseBytes);
		}

		return super.transmit(apdu);
	}
}
