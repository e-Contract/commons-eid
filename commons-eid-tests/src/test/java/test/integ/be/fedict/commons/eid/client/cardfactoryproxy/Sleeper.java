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
