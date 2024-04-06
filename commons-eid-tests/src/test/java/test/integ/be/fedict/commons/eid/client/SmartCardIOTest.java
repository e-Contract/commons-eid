/*
 * Commons eID Project.
 * Copyright (C) 2020 e-Contract.be BV.
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

import java.security.Provider;
import java.security.Security;
import java.util.List;

import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmartCardIOTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(SmartCardIOTest.class);

	@Test
	public void testProviders() throws Exception {
		Provider[] providers = Security.getProviders();
		for (Provider provider : providers) {
			LOGGER.debug("provider: {}", provider.getName());
			for (Object key : provider.keySet()) {
				LOGGER.debug("\t{}", key);
			}
		}
	}

	@Test
	public void testSmartCardIO() throws Exception {
		LOGGER.debug("OS: {}", System.getProperty("os.name"));
		// macOS Big Sur 11.0.1 work-around
		System.setProperty("sun.security.smartcardio.library",
				"/System/Library/Frameworks/PCSC.framework/Versions/Current/PCSC");
		TerminalFactory terminalFactory = TerminalFactory.getDefault();
		LOGGER.debug("terminal factory type: {}", terminalFactory.getType());
		LOGGER.debug("terminal factory provider: {}", terminalFactory.getProvider().getName());
		CardTerminals terminals = terminalFactory.terminals();
		List<CardTerminal> cardTerminalList = terminals.list();
		LOGGER.debug("number of card terminals: {}", cardTerminalList.size());
	}
}
