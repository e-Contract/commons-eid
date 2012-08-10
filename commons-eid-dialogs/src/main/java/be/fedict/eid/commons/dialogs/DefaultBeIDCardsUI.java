/*
 * Commons eID Project.
 * Copyright (C) 2008-2012 FedICT.
 * Copyright (C) 2009 Frank Cornelis.
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

package be.fedict.eid.commons.dialogs;

import java.awt.Component;
import java.awt.Insets;
import java.util.Locale;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import be.fedict.commons.eid.client.spi.BeIDCardsUI;

/**
 * Default Implementation of BeIDCardUI Interface
 * 
 * @author Frank Marien
 * 
 */
public class DefaultBeIDCardsUI implements BeIDCardsUI {
	private Component parentComponent;
	private Messages messages;
	private JFrame adviseFrame;

	public DefaultBeIDCardsUI() {
		this(null, new Messages(Locale.getDefault()));
	}

	public DefaultBeIDCardsUI(Component parentComponent) {
		this(parentComponent, new Messages(Locale.getDefault()));
	}

	public DefaultBeIDCardsUI(Component parentComponent, Messages messages) {
		this.parentComponent = parentComponent;
		this.messages = messages;
	}

	@Override
	public void adviseCardTerminalRequired() {
		showAdvise(
				this.messages.getMessage(Messages.MESSAGE_ID.CONNECT_READER),
				this.messages.getMessage(Messages.MESSAGE_ID.CONNECT_READER));
	}

	@Override
	public void adviseBeIDCardRequired() {
		showAdvise(this.messages
				.getMessage(Messages.MESSAGE_ID.INSERT_CARD_QUESTION),
				this.messages
						.getMessage(Messages.MESSAGE_ID.INSERT_CARD_QUESTION));
	}

	@Override
	public void adviseEnd() {
		if (null != this.adviseFrame) {
			this.adviseFrame.dispose();
			this.adviseFrame = null;
		}
	}

	/*
	 * **********************************************************************************************************************
	 */

	private void showAdvise(String title, String message) {
		if (null != this.adviseFrame) {
			adviseEnd();
		}

		this.adviseFrame = new JFrame(title);
		JPanel panel = new JPanel() {
			private static final long serialVersionUID = 1L;

			@Override
			public Insets getInsets() {
				return new Insets(10, 30, 10, 30);
			}
		};
		BoxLayout boxLayout = new BoxLayout(panel, BoxLayout.PAGE_AXIS);
		panel.setLayout(boxLayout);

		panel.add(new JLabel(message));
		this.adviseFrame.getContentPane().add(panel);
		this.adviseFrame.pack();

		if (this.parentComponent != null) {
			this.adviseFrame.setLocationRelativeTo(this.parentComponent);
		}
		this.adviseFrame.setVisible(true);
	}
}
