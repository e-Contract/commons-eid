/*
 * Commons eID Project.
 * Copyright (C) 2012 FedICT.
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

public enum PINPurpose {
	PINTest("test"), AuthenticationSignature("authentication"), NonRepudiationSignature(
			"nonrepudiation");

	private final String type;

	private PINPurpose(final String type) {
		this.type = type;
	}

	public String getType() {
		return this.type;
	}

	public static PINPurpose fromFileType(final FileType fileType) {
		switch (fileType) {
			case AuthentificationCertificate :
				return AuthenticationSignature;
			case NonRepudiationCertificate :
				return NonRepudiationSignature;
			default :
				return PINTest;
		}
	}
}
