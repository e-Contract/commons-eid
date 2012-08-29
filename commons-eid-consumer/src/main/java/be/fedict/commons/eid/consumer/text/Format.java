/*
 * eID Middleware Project.
 * Copyright (C) 2010-2012 FedICT.
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

package be.fedict.commons.eid.consumer.text;

/**
 *
 * @author Frank Marien
 */
public class Format {
	// format a national number into YY.MM.DD-S&G.CS
	public static String formatNationalNumber(final String nationalNumber) {
		//YY MM DD S&G CS
		//01 23 45 678 9A

		final StringBuilder formatted = new StringBuilder(nationalNumber
				.substring(0, 2));
		formatted.append('.');
		formatted.append(nationalNumber.substring(2, 4));
		formatted.append('.');
		formatted.append(nationalNumber.substring(4, 6));
		formatted.append('-');
		formatted.append(nationalNumber.substring(6, 9));
		formatted.append('.');
		formatted.append(nationalNumber.substring(9));
		return formatted.toString();
	}

	/*
	 *  format a card number into XXX-YYYYYYYY-ZZ
	 */
	public static String formatCardNumber(final String cardNumber) {
		final StringBuilder formatted = new StringBuilder();

		if (cardNumber.length() == 10 && cardNumber.startsWith("B")) {
			//B 0123456 78
			formatted.append(cardNumber.substring(0, 1));
			formatted.append(' ');
			formatted.append(cardNumber.substring(1, 7));
			formatted.append(' ');
			formatted.append(cardNumber.substring(8));
		} else if (cardNumber.length() == 12) {
			//012-3456789-01
			formatted.append(cardNumber.substring(0, 3));
			formatted.append('-');
			formatted.append(cardNumber.substring(3, 10));
			formatted.append('-');
			formatted.append(cardNumber.substring(10));
		} else {
			formatted.append(cardNumber);
		}

		return formatted.toString();
	}
}
