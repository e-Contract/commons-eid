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

import java.util.Map;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCardManager;
import be.fedict.commons.eid.client.TerminalManager;


public class StringUtils {
	static final String HEXDIGITS = "0123456789ABCDEF";

	public static String byteArrayToHexString(byte[] bytes) {
		if (bytes == null)
			return null;
		StringBuilder hexBuilder = new StringBuilder(2 * bytes.length);
		for (byte thisbyte : bytes)
			hexBuilder.append(HEXDIGITS.charAt((thisbyte & 0xf0) >> 4)).append(
					HEXDIGITS.charAt((thisbyte & 0x0f)));
		return hexBuilder.toString();
	}
	
	public static String getShortTerminalname(String terminalName)
	{
		StringBuilder shortName=new StringBuilder();
		String[] words=terminalName.split(" ");
		if(words.length>1)
		{
			shortName.append(words[0]);
			shortName.append(" ");
			shortName.append(words[1]);
		}
		else
			shortName.append(terminalName);
		
		return shortName.toString();
	}

	public static void printTerminalOverviewLine(TerminalManager terminalManager)
	{
		StringBuilder overviewLine=new StringBuilder();
		
		try 
		{
			for(CardTerminal terminal : terminalManager.getTerminalsPresent())
			{
				overviewLine.append("[");
				overviewLine.append(StringUtils.getShortTerminalname(terminal.getName()));
				try
				{
					if(terminal.isCardPresent())
						overviewLine.append("*");
				}
				catch(CardException cex)
				{
					overviewLine.append("!");
				}
				overviewLine.append("] ");
			}
		} 
		catch (CardException e)
		{
			System.err.println("FAILED TO READ LIST OF TERMINALS");
		}
		
		System.out.println(overviewLine.toString());
	}
	
	public static void printTerminalOverviewLine(BeIDCardManager cardManager)
	{
		StringBuilder overviewLine=new StringBuilder();
		
		for(Map.Entry<CardTerminal,BeIDCard> TnC : cardManager.getTerminalsWithBeIDCards().entrySet())
		{
			CardTerminal cardTerminal=TnC.getKey();
			BeIDCard	 beIDCard	 =TnC.getValue();
			
			overviewLine.append("[");
			overviewLine.append(StringUtils.getShortTerminalname(cardTerminal.getName()));
			overviewLine.append("] ");
		}
		
		System.out.println(overviewLine.toString());
	}
}
