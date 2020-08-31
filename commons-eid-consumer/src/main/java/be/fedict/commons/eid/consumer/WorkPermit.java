/*
 * Commons eID Project.
 * Copyright (C) 2018-2020 e-Contract.be BV.
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

package be.fedict.commons.eid.consumer;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public enum WorkPermit implements Serializable {

	JOB_MARKET_UNLIMITED("7"),

	JOB_MARKET_LIMITED("8"),

	JOB_MARKET_NONE("9"),

	SEASONAL_WORKER("A");

	private final String key;

	private WorkPermit(String key) {
		this.key = key;
	}

	public String getKey() {
		return this.key;
	}

	private static final Map<String, WorkPermit> WORK_PERMITS;

	static {
		WORK_PERMITS = new HashMap<>();
		for (WorkPermit workPermit : WorkPermit.values()) {
			String key = workPermit.key;
			WORK_PERMITS.put(key, workPermit);
		}
	}

	public static WorkPermit toWorkPermit(String key) {
		return WORK_PERMITS.get(key);
	}
}
