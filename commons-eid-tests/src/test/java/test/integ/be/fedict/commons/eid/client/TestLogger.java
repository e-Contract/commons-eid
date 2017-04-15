/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2017 Corilus NV.
 * Copyright (C) 2017 e-Contract.be BVBA.
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

package test.integ.be.fedict.commons.eid.client;

import org.slf4j.LoggerFactory;

import be.fedict.commons.eid.client.spi.Logger;

public class TestLogger implements Logger {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TestLogger.class);

	@Override
	public void error(final String message) {
		LOGGER.error(message);
	}

	@Override
	public void error(final String message, final Throwable exception) {
		LOGGER.error(message, exception);
	}

	@Override
	public void info(final String message) {
		LOGGER.info(message);
	}

	@Override
	public void debug(final String message) {
		LOGGER.debug(message);
	}
}
