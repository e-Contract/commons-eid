/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2017 Corilus NV.
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

/**
 * Logger-interface.
 * 
 * @author Frank Cornelis
 * @author Dennis Wagelaar
 */
public interface Logger {

	/**
	 * Error messages receiver.
	 * 
	 * @param message
	 *            the error message.
	 */
	void error(String message);

	/**
	 * Error messages receiver.
	 * 
	 * @param message
	 *            the error message.
	 * @param exception
	 *            the exception to log.
	 */
	void error(String message, Throwable exception);

	/**
	 * Info messages receiver.
	 * 
	 * @param message
	 *            the info message.
	 */
	void info(String message);

	/**
	 * Debug messages receiver.
	 * 
	 * @param message
	 *            the debug message.
	 */
	void debug(String message);
}
