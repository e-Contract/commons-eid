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

package test.integ.be.fedict.commons.eid.client;

import java.math.BigInteger;
import java.util.Set;
import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import be.fedict.commons.eid.client.BeIDCardEventsManager;
import be.fedict.commons.eid.client.CardAndTerminalEventsManager;

public class StringUtils {
	public static String atrToString(ATR atr) {
		return String.format("%x", new BigInteger(1, atr.getBytes()));
	}

	public static String getShortTerminalname(String terminalName) {
		StringBuilder shortName = new StringBuilder();
		String[] words = terminalName.split(" ");
		if (words.length > 1) {
			shortName.append(words[0]);
			shortName.append(" ");
			shortName.append(words[1]);
		} else
			shortName.append(terminalName);

		return shortName.toString();
	}

	public static void printTerminalOverviewLine(
			CardAndTerminalEventsManager cardAndTerminalEventsManager) {
		StringBuilder overviewLine = new StringBuilder();

		try {
			for (CardTerminal terminal : cardAndTerminalEventsManager
					.getTerminalsPresent()) {
				overviewLine.append("[");
				overviewLine.append(StringUtils.getShortTerminalname(terminal
						.getName()));
				try {
					if (terminal.isCardPresent())
						overviewLine.append("*");
				} catch (CardException cex) {
					overviewLine.append("!");
				}
				overviewLine.append("] ");
			}
		} catch (CardException e) {
			System.err.println("FAILED TO READ LIST OF TERMINALS");
		}

		System.out.println(overviewLine.toString());
	}

	public static void printTerminalAndCardOverviewLine(
			CardAndTerminalEventsManager cardAndTerminalEventsManager) {
		StringBuilder overviewLine = new StringBuilder();

		try {
			for (CardTerminal terminal : cardAndTerminalEventsManager
					.getTerminalsPresent()) {
				overviewLine.append("[");
				overviewLine.append(terminal.getName());
				try {
					if (terminal.isCardPresent()) {
						overviewLine.append(":");
						Card card = terminal.connect("*");
						overviewLine.append(StringUtils.atrToString(card
								.getATR()));
					}
				} catch (CardException cex) {
					overviewLine.append("!");
				}
				overviewLine.append("] ");
			}
		} catch (CardException e) {
			System.err.println("FAILED TO READ LIST OF TERMINALS");
		}

		System.out.println(overviewLine.toString());
	}

	public static void printTerminalOverviewLine(
			BeIDCardEventsManager cardManager) {
		StringBuilder overviewLine = new StringBuilder();

		for (CardTerminal terminal : cardManager
				.getTerminalsWithBeIDCardsPresent()) {
			overviewLine.append("[");
			overviewLine.append(StringUtils.getShortTerminalname(terminal
					.getName()));
			overviewLine.append("] ");
		}

		System.out.println(overviewLine.toString());
	}

	public static void printTerminalSet(Set<CardTerminal> set) {
		StringBuilder overviewLine = new StringBuilder();

		for (CardTerminal terminal : set) {
			overviewLine.append("[");
			overviewLine.append(terminal.getName());
			overviewLine.append("] ");
		}

		System.out.println(overviewLine.toString());
	}
}
