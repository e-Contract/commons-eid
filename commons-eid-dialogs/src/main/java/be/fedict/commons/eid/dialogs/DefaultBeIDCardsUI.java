/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2009 e-Contract.be BVBA.
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

package be.fedict.commons.eid.dialogs;

import java.awt.Component;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.util.Collection;
import java.util.Locale;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.CancelledException;
import be.fedict.commons.eid.client.OutOfCardsException;
import be.fedict.commons.eid.client.impl.LocaleManager;
import be.fedict.commons.eid.client.spi.BeIDCardsUI;

/**
 * Default Implementation of BeIDCardsUI Interface
 * 
 * @author Frank Marien
 * 
 */
public class DefaultBeIDCardsUI implements BeIDCardsUI {
	private Component parentComponent;
	private Messages messages;
	private JFrame adviseFrame;
	private BeIDSelector selectionDialog;
	private Locale locale;

	public DefaultBeIDCardsUI() {
		this(null);
	}

	public DefaultBeIDCardsUI(final Component parentComponent) {
		this(parentComponent, null);
	}

	public DefaultBeIDCardsUI(final Component parentComponent,
			final Messages messages) {
		this.parentComponent = parentComponent;
		this.messages = messages;
		if (GraphicsEnvironment.isHeadless()) {
			throw new UnsupportedOperationException(
					"DefaultBeIDCardsUI is a GUI and cannot run in a headless environment");
		}

		if (messages != null) {
			this.messages = messages;
			setLocale(messages.getLocale());
		} else {
			this.messages = Messages.getInstance(getLocale());
		}
	}

	@Override
	public void adviseCardTerminalRequired() {
		showAdvise(
				this.messages.getMessage(Messages.MESSAGE_ID.CONNECT_READER),
				this.messages.getMessage(Messages.MESSAGE_ID.CONNECT_READER));
	}

	@Override
	public void adviseBeIDCardRequired() {
		showAdvise(
				this.messages
						.getMessage(Messages.MESSAGE_ID.INSERT_CARD_QUESTION),
				this.messages
						.getMessage(Messages.MESSAGE_ID.INSERT_CARD_QUESTION));
	}

	@Override
	public void adviseBeIDCardRemovalRequired() {
		showAdvise(this.messages.getMessage(Messages.MESSAGE_ID.REMOVE_CARD),
				this.messages.getMessage(Messages.MESSAGE_ID.REMOVE_CARD));

	}

	@Override
	public BeIDCard selectBeIDCard(final Collection<BeIDCard> availableCards)
			throws CancelledException, OutOfCardsException {
		try {
			this.selectionDialog = new BeIDSelector(this.parentComponent,
					"Select eID card", availableCards);
			return this.selectionDialog.choose();
		} finally {
			this.selectionDialog = null;
		}
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

	private void showAdvise(final String title, final String message) {
		if (null != this.adviseFrame) {
			this.adviseEnd();
		}

		this.adviseFrame = new JFrame(title);
		this.adviseFrame.setAlwaysOnTop(true);
		this.adviseFrame.setResizable(false);
		JPanel panel = new JPanel() {
			private static final long serialVersionUID = 1L;

			@Override
			public Insets getInsets() {
				return new Insets(10, 30, 10, 30);
			}
		};
		final BoxLayout boxLayout = new BoxLayout(panel, BoxLayout.PAGE_AXIS);
		panel.setLayout(boxLayout);

		panel.add(new JLabel(message));
		this.adviseFrame.getContentPane().add(panel);
		this.adviseFrame.pack();

		if (this.parentComponent != null) {
			this.adviseFrame.setLocationRelativeTo(this.parentComponent);
		} else {
			GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment
					.getLocalGraphicsEnvironment();
			GraphicsDevice graphicsDevice = graphicsEnvironment
					.getDefaultScreenDevice();
			DisplayMode displayMode = graphicsDevice.getDisplayMode();
			int screenWidth = displayMode.getWidth();
			int screenHeight = displayMode.getHeight();
			int dialogWidth = this.adviseFrame.getWidth();
			int dialogHeight = this.adviseFrame.getHeight();
			this.adviseFrame.setLocation((screenWidth - dialogWidth) / 2,
					(screenHeight - dialogHeight) / 2);
		}

		this.adviseFrame.setVisible(true);
	}

	@Override
	public void eIDCardInsertedDuringSelection(final BeIDCard card) {
		if (this.selectionDialog != null) {
			this.selectionDialog.addEIDCard(card);
		}
	}

	@Override
	public void eIDCardRemovedDuringSelection(final BeIDCard card) {
		if (this.selectionDialog != null) {
			this.selectionDialog.removeEIDCard(card);
		}
	}

	@Override
	public void setLocale(Locale newLocale) {
		this.locale = newLocale;
		this.messages = Messages.getInstance(newLocale);

	}

	@Override
	public Locale getLocale() {
		if (this.locale != null) {
			return this.locale;
		}
		return LocaleManager.getLocale();
	}

}
