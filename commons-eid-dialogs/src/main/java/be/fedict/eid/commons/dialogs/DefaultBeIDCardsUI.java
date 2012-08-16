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
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.smartcardio.CardException;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDFileType;
import be.fedict.commons.eid.client.spi.BeIDCardsUI;
import be.fedict.commons.eid.consumer.Identity;
import be.fedict.commons.eid.consumer.tlv.TlvParser;

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
		if (GraphicsEnvironment.isHeadless()) {
			throw new UnsupportedOperationException(
					"DefaultBeIDCardsUI is a GUI and hence requires an interactive GraphicsEnvironment");
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
		showAdvise(this.messages
				.getMessage(Messages.MESSAGE_ID.INSERT_CARD_QUESTION),
				this.messages
						.getMessage(Messages.MESSAGE_ID.INSERT_CARD_QUESTION));
	}

	@Override
	public BeIDCard selectBeIDCard(Collection<BeIDCard> availableCards) {
		final JDialog dialog = new JDialog((Frame) null, "Select eID card",
				true);
		final ListData selectedListData = new ListData(null);
		dialog.setLayout(new BorderLayout());

		DefaultListModel listModel = new DefaultListModel();
		for (BeIDCard card : availableCards) {
			listModel.addElement(new ListData(card));
		}

		JList list = new JList(listModel);
		list.setCellRenderer(new EidListCellRenderer());
		dialog.getContentPane().add(list);

		MouseListener mouseListener = new MouseAdapter() {
			public void mouseClicked(MouseEvent mouseEvent) {
				JList theList = (JList) mouseEvent.getSource();
				if (mouseEvent.getClickCount() == 2) {
					int index = theList.locationToIndex(mouseEvent.getPoint());
					if (index >= 0) {
						Object object = theList.getModel().getElementAt(index);
						ListData listData = (ListData) object;
						selectedListData.card = listData.getCard();
						dialog.dispose();
					}
				}
			}
		};
		list.addMouseListener(mouseListener);

		dialog.pack();

		if (parentComponent != null) {
			dialog.setLocationRelativeTo(this.parentComponent);
		}

		dialog.setResizable(false);
		dialog.setVisible(true);
		return selectedListData.getCard();
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

	/*
	 * **********************************************************************************************************************
	 */

	private static class ListData {
		private BeIDCard card;
		private ImageIcon photo;
		private String name;

		public ListData(BeIDCard card) {
			this.card = card;
		}

		public BeIDCard getCard() {
			return this.card;
		}

		public ImageIcon getPhoto() {
			if (this.photo == null) {
				try {
					byte[] photoFile = getCard().readFile(BeIDFileType.Photo);
					BufferedImage photoImage = ImageIO
							.read(new ByteArrayInputStream(photoFile));
					this.photo = new ImageIcon(photoImage);
				} catch (CardException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			return this.photo;
		}

		public String getName() {
			if (this.name == null) {
				try {
					Identity identity = TlvParser.parse(getCard().readFile(
							BeIDFileType.Identity), Identity.class);
					this.name = identity.getFirstName() + " "
							+ identity.getName();
				} catch (CardException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			return this.name;
		}
	}

	private static class EidListCellRenderer extends JPanel
			implements
				ListCellRenderer {
		private static final long serialVersionUID = 6770429822588450447L;

		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			JPanel panel = new JPanel();
			ListData listData = (ListData) value;
			panel.setLayout(new FlowLayout(FlowLayout.LEFT));
			JLabel photoLabel = new JLabel(listData.getPhoto());
			panel.add(photoLabel);
			JLabel nameLabel = new JLabel(listData.getName());
			if (isSelected) {
				panel.setBackground(list.getSelectionBackground());
			} else {
				panel.setBackground(list.getBackground());
			}
			panel.add(nameLabel);
			return panel;
		}
	}
}
