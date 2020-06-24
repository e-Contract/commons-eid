/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2009-2020 e-Contract.be BV.
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.UIManager;
import javax.swing.border.Border;

import be.fedict.commons.eid.client.PINPurpose;
import be.fedict.commons.eid.client.spi.BeIDCardUI;
import be.fedict.commons.eid.client.spi.UserCancelledException;
import be.fedict.commons.eid.dialogs.Messages.MESSAGE_ID;

/**
 * Default Implementation of BeIDCardUI Interface
 * 
 * @author Frank Cornelis
 * @author Frank Marien
 * 
 */
public class DefaultBeIDCardUI implements BeIDCardUI {
	public static final int MIN_PIN_SIZE = 4;
	public static final int MAX_PIN_SIZE = 12;
	public static final int PUK_SIZE = 6;
	private static final String OPERATION_CANCELLED = "operation cancelled.";

	// TODO can pinPadFrame and secureReaderTransactionFrame be on-screen at the
	// same time? if not can be one member var and one dispose method
	private Component parentComponent;
	private JFrame pinPadFrame;
	private JFrame secureReaderTransactionFrame;
	private Locale locale;
	private Messages messages;

	public DefaultBeIDCardUI() {
		this(null);
	}

	public DefaultBeIDCardUI(Messages messages) {
		this(null, messages);
	}

	public DefaultBeIDCardUI(final Component parentComponent, Messages messages) {
		if (GraphicsEnvironment.isHeadless()) {
			throw new UnsupportedOperationException(
					"DefaultBeIDCardUI is a GUI and hence requires an interactive GraphicsEnvironment");
		}
		this.parentComponent = parentComponent;

		if (messages != null) {
			this.messages = messages;
		} else {
			this.messages = Messages.getInstance();
		}

	}

	@Override
	public void advisePINBlocked() {
		JOptionPane.showMessageDialog(this.parentComponent,
				this.messages.getMessage(MESSAGE_ID.PIN_BLOCKED),
				"eID card blocked", JOptionPane.ERROR_MESSAGE);
	}

	@Override
	public void advisePINChanged() {
		JOptionPane.showMessageDialog(this.parentComponent,
				this.messages.getMessage(MESSAGE_ID.PIN_CHANGED),
				"eID PIN change", JOptionPane.INFORMATION_MESSAGE);
	}

	@Override
	public void advisePINPadChangePIN(final int retriesLeft) {
		showPINPadFrame(retriesLeft, "eID PIN change",
				this.messages.getMessage(MESSAGE_ID.PIN_PAD_CHANGE));

	}

	@Override
	public void advisePINPadNewPINEntry(final int retriesLeft) {
		showPINPadFrame(retriesLeft, "eID PIN change",
				this.messages.getMessage(MESSAGE_ID.PIN_PAD_MODIFY_NEW));
	}

	@Override
	public void advisePINPadNewPINEntryAgain(final int retriesLeft) {
		showPINPadFrame(retriesLeft, "eID PIN change",
				this.messages.getMessage(MESSAGE_ID.PIN_PAD_MODIFY_NEW_AGAIN));
	}

	@Override
	public void advisePINPadOldPINEntry(final int retriesLeft) {
		showPINPadFrame(retriesLeft, "eID PIN change",
				this.messages.getMessage(MESSAGE_ID.PIN_PAD_MODIFY_OLD));

	}

	@Override
	public void advisePINPadOperationEnd() {
		disposePINPadFrame();
	}

	@Override
	public void advisePINPadPINEntry(final int retriesLeft,
			final PINPurpose purpose, String applicationName) {
		if (null == applicationName) {
			showPINPadFrame(
					retriesLeft,
					"PIN",
					this.messages.getMessage(MESSAGE_ID.PIN_REASON,
							purpose.getType()),
					this.messages.getMessage(MESSAGE_ID.PIN_PAD));
		} else {
			showPINPadFrame(
					retriesLeft,
					"PIN",
					this.messages.getMessage(MESSAGE_ID.PIN_REASON,
							purpose.getType()),
					this.messages.getMessage(MESSAGE_ID.APPLICATION) + ": "
							+ applicationName,
					this.messages.getMessage(MESSAGE_ID.PIN_PAD));
		}
	}

	@Override
	public void advisePINPadPUKEntry(final int retriesLeft) {
		showPINPadFrame(retriesLeft, "eID PIN unblock",
				this.messages.getMessage(MESSAGE_ID.PUK_PAD));

	}

	@Override
	public void advisePINUnblocked() {
		JOptionPane.showMessageDialog(this.parentComponent,
				this.messages.getMessage(MESSAGE_ID.PIN_UNBLOCKED),
				"eID PIN unblock", JOptionPane.INFORMATION_MESSAGE);
	}

	@Override
	public char[][] obtainOldAndNewPIN(final int retriesLeft) {
		final Box mainPanel = Box.createVerticalBox();

		if (-1 != retriesLeft) {
			mainPanel.add(Box.createVerticalStrut(4));
			final Box retriesPanel = createWarningBox(this.messages
					.getMessage(MESSAGE_ID.RETRIES_LEFT) + ": " + retriesLeft);
			mainPanel.add(retriesPanel);
			mainPanel.add(Box.createVerticalStrut(24));
		}

		final JPasswordField oldPinField = new JPasswordField(MAX_PIN_SIZE);
		final Box oldPinPanel = Box.createHorizontalBox();
		final JLabel oldPinLabel = new JLabel(
				this.messages.getMessage(MESSAGE_ID.CURRENT_PIN) + ":");
		oldPinLabel.setLabelFor(oldPinField);
		oldPinPanel.add(oldPinLabel);
		oldPinPanel.add(Box.createHorizontalStrut(5));
		oldPinPanel.add(oldPinField);
		mainPanel.add(oldPinPanel);

		mainPanel.add(Box.createVerticalStrut(5));

		final JPasswordField newPinField = new JPasswordField(MAX_PIN_SIZE);
		final Box newPinPanel = Box.createHorizontalBox();
		final JLabel newPinLabel = new JLabel(
				this.messages.getMessage(MESSAGE_ID.NEW_PIN) + ":");
		newPinLabel.setLabelFor(newPinField);
		newPinPanel.add(newPinLabel);
		newPinPanel.add(Box.createHorizontalStrut(5));
		newPinPanel.add(newPinField);
		mainPanel.add(newPinPanel);

		mainPanel.add(Box.createVerticalStrut(5));

		final JPasswordField new2PinField = new JPasswordField(MAX_PIN_SIZE);
		{
			final Box new2PinPanel = Box.createHorizontalBox();
			final JLabel new2PinLabel = new JLabel(
					this.messages.getMessage(MESSAGE_ID.NEW_PIN) + ":");
			new2PinLabel.setLabelFor(new2PinField);
			new2PinPanel.add(new2PinLabel);
			new2PinPanel.add(Box.createHorizontalStrut(5));
			new2PinPanel.add(new2PinField);
			mainPanel.add(new2PinPanel);
		}

		final int result = JOptionPane.showOptionDialog(this.parentComponent,
				mainPanel, "Change eID PIN", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, null, null);
		if (result != JOptionPane.OK_OPTION) {
			throw new RuntimeException(OPERATION_CANCELLED);
		}
		if (false == Arrays.equals(newPinField.getPassword(),
				new2PinField.getPassword())) {
			throw new RuntimeException("new PINs not equal");
		}
		final char[] oldPin = new char[oldPinField.getPassword().length];
		final char[] newPin = new char[newPinField.getPassword().length];
		System.arraycopy(oldPinField.getPassword(), 0, oldPin, 0,
				oldPinField.getPassword().length);
		System.arraycopy(newPinField.getPassword(), 0, newPin, 0,
				newPinField.getPassword().length);
		Arrays.fill(oldPinField.getPassword(), (char) 0);
		Arrays.fill(newPinField.getPassword(), (char) 0);
		return new char[][]{oldPin, newPin};
	}

	@Override
	public char[] obtainPIN(final int retriesLeft, final PINPurpose reason,
			String applicationName) throws UserCancelledException {
		// main panel
		JPanel mainPanel = new JPanel() {
			private static final long serialVersionUID = 1L;
			private static final int BORDER_SIZE = 20;

			@Override
			public Insets getInsets() {
				return new Insets(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE,
						BORDER_SIZE);
			}
		};
		final BoxLayout boxLayout = new BoxLayout(mainPanel,
				BoxLayout.PAGE_AXIS);
		mainPanel.setLayout(boxLayout);

		final Box reasonPanel = Box.createHorizontalBox();
		final JLabel reasonLabel = new JLabel(this.messages.getMessage(
				MESSAGE_ID.PIN_REASON, reason.getType()));
		reasonPanel.add(reasonLabel);
		reasonPanel.add(Box.createHorizontalGlue());
		mainPanel.add(reasonPanel);
		mainPanel.add(Box.createVerticalStrut(16));

		if (null != applicationName) {
			Box applicationBox = Box.createHorizontalBox();
			JLabel applicationLabel = new JLabel(
					this.messages.getMessage(MESSAGE_ID.APPLICATION) + ": "
							+ applicationName);
			applicationBox.add(applicationLabel);
			applicationBox.add(Box.createHorizontalGlue());
			mainPanel.add(applicationBox);
			mainPanel.add(Box.createVerticalStrut(16));
		}

		if (-1 != retriesLeft) {
			addWarningBox(mainPanel,
					this.messages.getMessage(MESSAGE_ID.RETRIES_LEFT) + ": "
							+ retriesLeft);
		}

		final Box passwordPanel = Box.createHorizontalBox();
		final JLabel promptLabel = new JLabel(
				this.messages.getMessage(MESSAGE_ID.LABEL_PIN) + ": ");
		passwordPanel.add(promptLabel);
		passwordPanel.add(Box.createHorizontalStrut(5));
		final JPasswordField passwordField = new JPasswordField(MAX_PIN_SIZE);
		promptLabel.setLabelFor(passwordField);
		passwordPanel.add(passwordField);
		passwordPanel.setBorder(createGenerousLowerBevelBorder());
		mainPanel.add(passwordPanel);

		// button panel
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT)) {
			private static final long serialVersionUID = 1L;

			@Override
			public Insets getInsets() {
				return new Insets(0, 0, 5, 5);
			}
		};
		final JButton okButton = new JButton(
				this.messages.getMessage(MESSAGE_ID.OK));
		okButton.setEnabled(false);
		buttonPanel.add(okButton);
		final JButton cancelButton = new JButton(
				this.messages.getMessage(MESSAGE_ID.CANCEL));
		buttonPanel.add(cancelButton);

		// dialog box
		final JDialog dialog = new JDialog((Frame) null,
				this.messages.getMessage(MESSAGE_ID.ENTER_PIN), true);
		dialog.setAlwaysOnTop(true);
		dialog.setLayout(new BorderLayout());
		dialog.getContentPane().add(mainPanel, BorderLayout.CENTER);
		dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		final DialogResult dialogResult = new DialogResult();

		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				dialogResult.result = DialogResult.Result.OK;
				dialog.dispose();
			}
		});
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				dialogResult.result = DialogResult.Result.CANCEL;
				dialog.dispose();
			}
		});
		passwordField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				final int pinSize = passwordField.getPassword().length;
				if (MIN_PIN_SIZE <= pinSize && pinSize <= MAX_PIN_SIZE) {
					dialogResult.result = DialogResult.Result.OK;
					dialog.dispose();
				}
			}
		});
		passwordField.addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(final KeyEvent e) {
			}

			@Override
			public void keyReleased(final KeyEvent e) {
				final int pinSize = passwordField.getPassword().length;
				if (MIN_PIN_SIZE <= pinSize && pinSize <= MAX_PIN_SIZE) {
					okButton.setEnabled(true);
				} else {
					okButton.setEnabled(false);
				}
			}

			@Override
			public void keyTyped(final KeyEvent e) {
			}
		});

		dialog.pack();
		dialog.setLocationRelativeTo(this.parentComponent);

		dialog.setVisible(true);
		// setVisible will wait until some button or so has been pressed

		if (dialogResult.result == DialogResult.Result.OK) {
			return passwordField.getPassword();
		}
		throw new UserCancelledException();
	}

	@Override
	public char[][] obtainPUKCodes(final int retriesLeft) {
		final Box mainPanel = Box.createVerticalBox();

		if (-1 != retriesLeft) {
			addWarningBox(mainPanel,
					this.messages.getMessage(MESSAGE_ID.RETRIES_LEFT) + ": "
							+ retriesLeft);
		}

		final JPasswordField puk1Field = new JPasswordField(8);
		final Box puk1Panel = Box.createHorizontalBox();
		final JLabel puk1Label = new JLabel("eID PUK1:");
		puk1Label.setLabelFor(puk1Field);
		puk1Panel.add(puk1Label);
		puk1Panel.add(Box.createHorizontalStrut(5));
		puk1Panel.add(puk1Field);
		mainPanel.add(puk1Panel);

		mainPanel.add(Box.createVerticalStrut(5));

		final JPasswordField puk2Field = new JPasswordField(8);
		final Box puk2Panel = Box.createHorizontalBox();
		final JLabel puk2Label = new JLabel("eID PUK2:");
		puk2Label.setLabelFor(puk2Field);
		puk2Panel.add(puk2Label);
		puk2Panel.add(Box.createHorizontalStrut(5));
		puk2Panel.add(puk2Field);
		mainPanel.add(puk2Panel);

		final int result = JOptionPane.showOptionDialog(this.parentComponent,
				mainPanel, "eID PIN unblock", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, null, null);
		if (result != JOptionPane.OK_OPTION) {
			throw new RuntimeException(OPERATION_CANCELLED);
		}
		if (puk1Field.getPassword().length != PUK_SIZE
				|| puk2Field.getPassword().length != PUK_SIZE) {
			throw new RuntimeException("PUK size incorrect");
		}
		final char[] puk1 = new char[puk1Field.getPassword().length];
		final char[] puk2 = new char[puk2Field.getPassword().length];
		System.arraycopy(puk1Field.getPassword(), 0, puk1, 0,
				puk1Field.getPassword().length);
		System.arraycopy(puk2Field.getPassword(), 0, puk2, 0,
				puk2Field.getPassword().length);
		Arrays.fill(puk1Field.getPassword(), (char) 0);
		Arrays.fill(puk2Field.getPassword(), (char) 0);
		return new char[][]{puk1, puk2};
	}

	@Override
	public void adviseSecureReaderOperation() {
		if (null != this.secureReaderTransactionFrame) {
			disposeSecureReaderFrame();
		}
		this.secureReaderTransactionFrame = new JFrame(
				"Transaction Confirmation");
		this.secureReaderTransactionFrame.setAlwaysOnTop(true);
		JPanel panel = new JPanel() {
			private static final long serialVersionUID = 1L;

			@Override
			public Insets getInsets() {
				return new Insets(10, 30, 10, 30);
			}
		};
		final BoxLayout boxLayout = new BoxLayout(panel, BoxLayout.PAGE_AXIS);
		panel.setLayout(boxLayout);
		panel.add(new JLabel(
				"Check the transaction message on the secure card reader."));

		this.secureReaderTransactionFrame.getContentPane().add(panel);
		this.secureReaderTransactionFrame.pack();

		this.secureReaderTransactionFrame
					.setLocationRelativeTo(this.parentComponent);

		this.secureReaderTransactionFrame.setVisible(true);
	}

	@Override
	public void adviseSecureReaderOperationEnd() {
		disposeSecureReaderFrame();
	}

	/*
	 * **********************************************************************************************************************
	 */

	private Box addWarningBox(final JComponent parent,
			final String warningMessage) {
		parent.add(Box.createVerticalStrut(4));
		final Box retriesPanel = createWarningBox(warningMessage);
		parent.add(retriesPanel);
		parent.add(Box.createVerticalStrut(24));
		return retriesPanel;
	}

	private Box createWarningBox(final String warningText) {
		final Box warningBox = Box.createHorizontalBox();
		final JLabel warningLabel = new JLabel(warningText);
		warningLabel.setForeground(Color.RED);
		final Icon warningIcon = UIManager.getIcon("OptionPane.warningIcon");
		if (warningIcon != null) {
			warningLabel.setIcon(warningIcon);
		}
		warningBox.add(warningLabel);
		warningBox.add(Box.createHorizontalGlue());
		warningBox.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(Color.red, 1),
				BorderFactory.createEmptyBorder(8, 8, 8, 8)));
		return warningBox;
	}

	private Border createGenerousLowerBevelBorder() {
		return BorderFactory.createCompoundBorder(
				BorderFactory.createLoweredBevelBorder(),
				BorderFactory.createEmptyBorder(16, 16, 16, 16));
	}

	private void showPINPadFrame(final int retriesLeft, final String title,
			final String... messages) {
		if (null != this.pinPadFrame) {
			disposePINPadFrame();
		}
		this.pinPadFrame = new JFrame(title);
		this.pinPadFrame.setAlwaysOnTop(true);
		this.pinPadFrame.setResizable(false);
		JPanel panel = new JPanel();
		final BoxLayout boxLayout = new BoxLayout(panel, BoxLayout.PAGE_AXIS);
		panel.setLayout(boxLayout);
		panel.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

		if (messages.length > 0) {
			final JLabel label = new JLabel(messages[0]);
			label.setAlignmentX((float) 0.5);
			panel.add(label);
		}

		if (-1 != retriesLeft) {
			panel.add(Box.createVerticalStrut(24));
			final Box warningBox = this.createWarningBox(this.messages
					.getMessage(MESSAGE_ID.RETRIES_LEFT) + ": " + retriesLeft);
			panel.add(warningBox);
			panel.add(Box.createVerticalStrut(24));
		}

		for (int i = 1; i < messages.length; i++) {
			final JLabel label = new JLabel(messages[i]);
			label.setAlignmentX((float) 0.5);
			panel.add(label);
		}

		this.pinPadFrame.getContentPane().add(panel);
		this.pinPadFrame.pack();

		this.pinPadFrame.setLocationRelativeTo(this.parentComponent);

		this.pinPadFrame.setVisible(true);
	}

	private void disposePINPadFrame() {
		if (null != this.pinPadFrame) {
			this.pinPadFrame.dispose();
			this.pinPadFrame = null;
		}
	}

	/*
	 * 
	 */

	private void disposeSecureReaderFrame() {
		if (null != this.secureReaderTransactionFrame) {
			this.secureReaderTransactionFrame.dispose();
			this.secureReaderTransactionFrame = null;
		}
	}

	/*
	 * 
	 */

	private static class DialogResult {
		enum Result {
			OK, CANCEL
		};

		public Result result = null;
	}

	@Override
	public void setLocale(Locale newLocale) {
		this.locale = newLocale;
		this.messages = Messages.getInstance(newLocale);
	}

	@Override
	public Locale getLocale() {
		return locale;
	}
}
