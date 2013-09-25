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

package test.integ.be.fedict.commons.eid.client;

import java.util.Locale;

import javax.swing.JOptionPane;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import be.fedict.commons.eid.client.PINPurpose;
import be.fedict.commons.eid.client.spi.BeIDCardUI;
import be.fedict.commons.eid.dialogs.DefaultBeIDCardUI;
import be.fedict.commons.eid.dialogs.Messages;

public class DialogsTest {

	private static final Log LOG = LogFactory.getLog(DialogsTest.class);

	@Test
	public void testDefaultDialogs() throws Exception {
		Messages messages = Messages.getInstance(new Locale("fr"));
		BeIDCardUI beIDCardUI = new DefaultBeIDCardUI(null, messages);

		char[] pin = beIDCardUI
				.obtainPIN(2, PINPurpose.NonRepudiationSignature);
		LOG.debug("PIN: " + new String(pin));
		beIDCardUI.advisePINPadPINEntry(1, PINPurpose.NonRepudiationSignature);

		JOptionPane.showMessageDialog(null, "Waiting...");

		beIDCardUI.advisePINPadPINEntry(-1, PINPurpose.NonRepudiationSignature);

		JOptionPane.showMessageDialog(null, "Waiting...");

		beIDCardUI.advisePINPadOperationEnd();

		beIDCardUI.obtainPIN(-1, PINPurpose.PINTest);
		beIDCardUI.obtainPIN(2, PINPurpose.PINTest);
		beIDCardUI.obtainPIN(1, PINPurpose.PINTest);
		beIDCardUI.obtainPIN(-1, PINPurpose.AuthenticationSignature);
		beIDCardUI.obtainPIN(2, PINPurpose.AuthenticationSignature);
		beIDCardUI.obtainPIN(1, PINPurpose.AuthenticationSignature);
		beIDCardUI.obtainPIN(-1, PINPurpose.NonRepudiationSignature);
		beIDCardUI.obtainPIN(2, PINPurpose.NonRepudiationSignature);
		beIDCardUI.obtainPIN(1, PINPurpose.NonRepudiationSignature);

	}
}
