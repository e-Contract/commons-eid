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
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import be.fedict.commons.eid.client.PINPurpose;
import be.fedict.commons.eid.client.spi.BeIDCardUI;
import be.fedict.eid.commons.dialogs.Messages.MESSAGE_ID;

/**
 * Default Implementation of BeIDCardUI Interface
 * 
 * @author Frank Cornelis
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
	private Messages messages;
	private JFrame pinPadFrame;
	private JFrame secureReaderTransactionFrame;

	public DefaultBeIDCardUI() {
		this(null, new Messages(Locale.getDefault()));
	}

	public DefaultBeIDCardUI(final Component parentComponent) {
		this(parentComponent, new Messages(Locale.getDefault()));
	}

	public DefaultBeIDCardUI(final Component parentComponent,
			final Messages messages) {
		this.parentComponent = parentComponent;
		this.messages = messages;
		if (GraphicsEnvironment.isHeadless()) {
			throw new UnsupportedOperationException(
					"DefaultBeIDCardUI is a GUI and hence requires an interactive GraphicsEnvironment");
		}
	}

	@Override
	public void advisePINBlocked() {
		JOptionPane.showMessageDialog(this.parentComponent, this.messages
				.getMessage(MESSAGE_ID.PIN_BLOCKED), "eID card blocked",
				JOptionPane.ERROR_MESSAGE);
	}

	@Override
	public void advisePINChanged() {
		JOptionPane.showMessageDialog(this.parentComponent, this.messages
				.getMessage(MESSAGE_ID.PIN_CHANGED), "eID PIN change",
				JOptionPane.INFORMATION_MESSAGE);
	}

	@Override
	public void advisePINPadChangePIN(final int retriesLeft) {
		showPINPadFrame(retriesLeft, "eID PIN change", this.messages
				.getMessage(MESSAGE_ID.PIN_PAD_CHANGE));

	}

	@Override
	public void advisePINPadNewPINEntry(final int retriesLeft) {
		showPINPadFrame(retriesLeft, "eID PIN change", this.messages
				.getMessage(MESSAGE_ID.PIN_PAD_MODIFY_NEW));
	}

	@Override
	public void advisePINPadNewPINEntryAgain(final int retriesLeft) {
		showPINPadFrame(retriesLeft, "eID PIN change", this.messages
				.getMessage(MESSAGE_ID.PIN_PAD_MODIFY_NEW_AGAIN));
	}

	@Override
	public void advisePINPadOldPINEntry(final int retriesLeft) {
		showPINPadFrame(retriesLeft, "eID PIN change", this.messages
				.getMessage(MESSAGE_ID.PIN_PAD_MODIFY_OLD));

	}

	@Override
	public void advisePINPadOperationEnd() {
		disposePINPadFrame();
	}

	@Override
	public void advisePINPadPINEntry(final int retriesLeft) {
		showPINPadFrame(retriesLeft, "PIN", this.messages
				.getMessage(MESSAGE_ID.PIN_PAD));
	}

	@Override
	public void advisePINPadPUKEntry(final int retriesLeft) {
		showPINPadFrame(retriesLeft, "eID PIN unblock", this.messages
				.getMessage(MESSAGE_ID.PUK_PAD));

	}

	@Override
	public void advisePINUnblocked() {
		JOptionPane.showMessageDialog(this.parentComponent, this.messages
				.getMessage(MESSAGE_ID.PIN_UNBLOCKED), "eID PIN unblock",
				JOptionPane.INFORMATION_MESSAGE);
	}

	@Override
	public char[][] obtainOldAndNewPIN(final int retriesLeft) {
		final Box mainPanel = Box.createVerticalBox();

		if (-1 != retriesLeft) {
			final Box retriesPanel = Box.createHorizontalBox();
			final JLabel retriesLabel = new JLabel(this.messages
					.getMessage(MESSAGE_ID.RETRIES_LEFT)
					+ ": " + retriesLeft);
			retriesLabel.setForeground(Color.RED);
			retriesPanel.add(retriesLabel);
			retriesPanel.add(Box.createHorizontalGlue());
			mainPanel.add(retriesPanel);
			mainPanel.add(Box.createVerticalStrut(5));
		}

		final JPasswordField oldPinField = new JPasswordField(MAX_PIN_SIZE);
		final Box oldPinPanel = Box.createHorizontalBox();
		final JLabel oldPinLabel = new JLabel(this.messages
				.getMessage(MESSAGE_ID.CURRENT_PIN)
				+ ":");
		oldPinLabel.setLabelFor(oldPinField);
		oldPinPanel.add(oldPinLabel);
		oldPinPanel.add(Box.createHorizontalStrut(5));
		oldPinPanel.add(oldPinField);
		mainPanel.add(oldPinPanel);

		mainPanel.add(Box.createVerticalStrut(5));

		final JPasswordField newPinField = new JPasswordField(MAX_PIN_SIZE);
		final Box newPinPanel = Box.createHorizontalBox();
		final JLabel newPinLabel = new JLabel(this.messages
				.getMessage(MESSAGE_ID.NEW_PIN)
				+ ":");
		newPinLabel.setLabelFor(newPinField);
		newPinPanel.add(newPinLabel);
		newPinPanel.add(Box.createHorizontalStrut(5));
		newPinPanel.add(newPinField);
		mainPanel.add(newPinPanel);

		mainPanel.add(Box.createVerticalStrut(5));

		final JPasswordField new2PinField = new JPasswordField(MAX_PIN_SIZE);
		{
			final Box new2PinPanel = Box.createHorizontalBox();
			final JLabel new2PinLabel = new JLabel(this.messages
					.getMessage(MESSAGE_ID.NEW_PIN)
					+ ":");
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
		if (false == Arrays.equals(newPinField.getPassword(), new2PinField
				.getPassword())) {
			throw new RuntimeException("new PINs not equal");
		}
		final char[] oldPin = new char[oldPinField.getPassword().length];
		final char[] newPin = new char[newPinField.getPassword().length];
		System.arraycopy(oldPinField.getPassword(), 0, oldPin, 0, oldPinField
				.getPassword().length);
		System.arraycopy(newPinField.getPassword(), 0, newPin, 0, newPinField
				.getPassword().length);
		Arrays.fill(oldPinField.getPassword(), (char) 0);
		Arrays.fill(newPinField.getPassword(), (char) 0);
		return new char[][]{oldPin, newPin};
	}

	@Override
	public char[] obtainPIN(final int retriesLeft, final PINPurpose reason) {
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
		mainPanel.add(Box.createVerticalStrut(10));

		if (-1 != retriesLeft) {
			final Box retriesPanel = Box.createHorizontalBox();
			final JLabel retriesLabel = new JLabel(this.messages
					.getMessage(MESSAGE_ID.RETRIES_LEFT)
					+ ": " + retriesLeft);
			retriesLabel.setForeground(Color.RED);
			retriesPanel.add(retriesLabel);
			retriesPanel.add(Box.createHorizontalGlue());
			mainPanel.add(retriesPanel);
			mainPanel.add(Box.createVerticalStrut(10));
		}

		final Box passwordPanel = Box.createHorizontalBox();
		final JLabel promptLabel = new JLabel(this.messages
				.getMessage(MESSAGE_ID.LABEL_PIN)
				+ ": ");
		passwordPanel.add(promptLabel);
		passwordPanel.add(Box.createHorizontalStrut(5));
		final JPasswordField passwordField = new JPasswordField(MAX_PIN_SIZE);
		promptLabel.setLabelFor(passwordField);
		passwordPanel.add(passwordField);
		mainPanel.add(passwordPanel);

		// button panel
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT)) {
			private static final long serialVersionUID = 1L;

			@Override
			public Insets getInsets() {
				return new Insets(0, 0, 5, 5);
			}
		};
		final JButton okButton = new JButton(this.messages
				.getMessage(MESSAGE_ID.OK));
		okButton.setEnabled(false);
		buttonPanel.add(okButton);
		final JButton cancelButton = new JButton(this.messages
				.getMessage(MESSAGE_ID.CANCEL));
		buttonPanel.add(cancelButton);

		// dialog box
		final JDialog dialog = new JDialog((Frame) null, this.messages
				.getMessage(MESSAGE_ID.ENTER_PIN), true);
		dialog.setLayout(new BorderLayout());
		dialog.getContentPane().add(mainPanel, BorderLayout.CENTER);
		dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		final DialogResult dialogResult = new DialogResult();

		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				dialogResult.result = DialogResult.Result.OK;
				dialog.dispose();
			}
		});
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				dialogResult.result = DialogResult.Result.CANCEL;
				dialog.dispose();
			}
		});
		passwordField.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				final int pinSize = passwordField.getPassword().length;
				if (MIN_PIN_SIZE <= pinSize && pinSize <= MAX_PIN_SIZE) {
					dialogResult.result = DialogResult.Result.OK;
					dialog.dispose();
				}
			}
		});
		passwordField.addKeyListener(new KeyListener() {

			public void keyPressed(final KeyEvent e) {
			}

			public void keyReleased(final KeyEvent e) {
				final int pinSize = passwordField.getPassword().length;
				if (MIN_PIN_SIZE <= pinSize && pinSize <= MAX_PIN_SIZE) {
					okButton.setEnabled(true);
				} else {
					okButton.setEnabled(false);
				}
			}

			public void keyTyped(final KeyEvent e) {
			}
		});

		dialog.pack();
		if (this.parentComponent != null) {
			dialog.setLocationRelativeTo(this.parentComponent);
		}

		dialog.setVisible(true);
		// setVisible will wait until some button or so has been pressed

		if (dialogResult.result == DialogResult.Result.OK) {
			return passwordField.getPassword();
		}
		throw new RuntimeException(OPERATION_CANCELLED);
	}

	@Override
	public char[][] obtainPUKCodes(final int retriesLeft) {
		final Box mainPanel = Box.createVerticalBox();

		if (-1 != retriesLeft) {
			final Box retriesPanel = Box.createHorizontalBox();
			final JLabel retriesLabel = new JLabel(this.messages
					.getMessage(MESSAGE_ID.RETRIES_LEFT)
					+ ": " + retriesLeft);
			retriesLabel.setForeground(Color.RED);
			retriesPanel.add(retriesLabel);
			retriesPanel.add(Box.createHorizontalGlue());
			mainPanel.add(retriesPanel);
			mainPanel.add(Box.createVerticalStrut(5));
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
		System.arraycopy(puk1Field.getPassword(), 0, puk1, 0, puk1Field
				.getPassword().length);
		System.arraycopy(puk2Field.getPassword(), 0, puk2, 0, puk2Field
				.getPassword().length);
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

		if (this.parentComponent != null) {
			this.secureReaderTransactionFrame
					.setLocationRelativeTo(this.parentComponent);
		}
		this.secureReaderTransactionFrame.setVisible(true);
	}

	@Override
	public void adviseSecureReaderOperationEnd() {
		disposeSecureReaderFrame();
	}

	/*
	 * **********************************************************************************************************************
	 */

	private void showPINPadFrame(final int retriesLeft, final String title,
			final String message) {
		if (null != this.pinPadFrame) {
			disposePINPadFrame();
		}
		this.pinPadFrame = new JFrame(title);
		JPanel panel = new JPanel() {
			private static final long serialVersionUID = 1L;

			@Override
			public Insets getInsets() {
				return new Insets(10, 30, 10, 30);
			}
		};
		final BoxLayout boxLayout = new BoxLayout(panel, BoxLayout.PAGE_AXIS);
		panel.setLayout(boxLayout);

		if (-1 != retriesLeft) {
			final JLabel retriesLabel = new JLabel(this.messages
					.getMessage(MESSAGE_ID.RETRIES_LEFT)
					+ ": " + retriesLeft);
			retriesLabel.setForeground(Color.RED);
			panel.add(retriesLabel);
		}
		panel.add(new JLabel(message));
		this.pinPadFrame.getContentPane().add(panel);
		this.pinPadFrame.pack();

		if (this.parentComponent != null) {
			this.pinPadFrame.setLocationRelativeTo(this.parentComponent);
		}
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

}
