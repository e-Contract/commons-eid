/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
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

package be.fedict.commons.eid.client;

import javax.smartcardio.ResponseAPDU;

/**
 * A ResponseAPDUException encapsulates a ResponseAPDU that lead to the
 * exception, making it available to the catching code.
 * @author Frank Marien
 *
 */
public class ResponseAPDUException extends RuntimeException {
	private static final long serialVersionUID = -3573705690889181394L;
	private final ResponseAPDU apdu;

	public ResponseAPDUException(final ResponseAPDU apdu) {
		super();
		this.apdu = apdu;
	}

	public ResponseAPDUException(final String message, final ResponseAPDU apdu) {
		super(message + " [" + Integer.toHexString(apdu.getSW()) + "]");
		this.apdu = apdu;
	}

	public ResponseAPDU getApdu() {
		return this.apdu;
	}
}
