/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2024 e-Contract.be BV.
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

import java.math.BigInteger;
import java.util.Set;
import javax.smartcardio.ATR;
import javax.smartcardio.CardTerminal;

public class StringUtils {
	public static String atrToString(final ATR atr) {
		return String.format("%x", new BigInteger(1, atr.getBytes()));
	}

	public static String getShortTerminalname(final String terminalName) {
		final StringBuilder shortName = new StringBuilder();
		final String[] words = terminalName.split(" ");
		if (words.length > 1) {
			shortName.append(words[0]);
			shortName.append(" ");
			shortName.append(words[1]);
		} else {
			shortName.append(terminalName);
		}

		return shortName.toString();
	}

	public static void printTerminalSet(final Set<CardTerminal> set) {
		final StringBuilder overviewLine = new StringBuilder();

		for (CardTerminal terminal : set) {
			overviewLine.append("[");
			overviewLine.append(terminal.getName());
			overviewLine.append("] ");
		}

		System.out.println(overviewLine);
	}
}
