/*
 * Commons eID Project.
 * Copyright (C) 2012-2013 FedICT.
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

/**
 * a PINPurpose encapsulates the different reasons why the user's PIN code may be
 * requested: an authentication signature, a non-repudiation signature, or a user-requested 
 * test of their PIN code.
 * @author Frank Marien
 */
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

	/**
	 * Determine the likely reason for a PIN request by checking the certificate chain
	 * involved.
	 * @param fileType the File on the BeID that is involved in the operation
	 * @return the PIN Purpose associated with this type of file
	 */
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
