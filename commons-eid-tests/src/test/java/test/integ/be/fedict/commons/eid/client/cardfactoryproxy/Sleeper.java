package test.integ.be.fedict.commons.eid.client.cardfactoryproxy;

public class Sleeper {
	private boolean isAwoken;

	public synchronized void sleepUntilAwakened(long timeout) {
		while (!isAwoken)
			try {
				this.wait(timeout);
			} catch (InterruptedException e) {
			} // intentionally empty
		isAwoken = false;
	}

	public synchronized void awaken() {
		isAwoken = true;
		this.notify();
	}
}
