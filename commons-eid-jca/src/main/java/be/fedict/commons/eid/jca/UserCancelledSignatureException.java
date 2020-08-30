/*
 * Commons eID Project.
 * Copyright (C) 2015-2020 e-Contract.be BV.
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
package be.fedict.commons.eid.jca;

import java.security.SignatureException;

/**
 * Thrown in case the user cancelled the eID operation.
 * 
 * @author Frank Cornelis
 */
public class UserCancelledSignatureException extends SignatureException {

	private static final long serialVersionUID = 1L;

	public UserCancelledSignatureException(Exception e) {
		super(e);
	}
}
