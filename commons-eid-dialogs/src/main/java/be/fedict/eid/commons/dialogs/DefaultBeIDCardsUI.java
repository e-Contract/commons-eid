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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.ListCellRenderer;
import org.jdesktop.swingx.JXList;
import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDFileType;
import be.fedict.commons.eid.client.event.BeIDCardListener;
import be.fedict.commons.eid.client.spi.BeIDCardsUI;
import be.fedict.commons.eid.consumer.Identity;
import be.fedict.commons.eid.consumer.text.Format;
import be.fedict.commons.eid.consumer.tlv.TlvParser;

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
					"DefaultBeIDCardsUI is a GUI and cannot run in a headless environment");
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
		final JDialog dialog = new JDialog((Frame) this.parentComponent,
				"Select eID card", true);
		final ListData selectedListData = new ListData(null);
		final JPanel masterPanel = new JPanel(new BorderLayout());
		masterPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

		final DefaultListModel<ListData> listModel = new DefaultListModel<ListData>();

		for (BeIDCard card : availableCards) {
			listModel.addElement(new ListData(card));
		}

		final ListModelUpdater listModelUpdater = new ListModelUpdater(
				listModel);

		JXList list = new JXList(listModel);
		list.setCellRenderer(new EidListCellRenderer());
		masterPanel.add(list);
		dialog.getContentPane().add(masterPanel);

		MouseListener mouseListener = new MouseAdapter() {
			public void mouseClicked(MouseEvent mouseEvent) {
				JList<?> theList = (JList<?>) mouseEvent.getSource();
				if (mouseEvent.getClickCount() == 2) {
					int index = theList.locationToIndex(mouseEvent.getPoint());
					if (index >= 0) {
						listModelUpdater.stop();
						Object object = theList.getModel().getElementAt(index);
						ListData listData = (ListData) object;
						selectedListData.card = listData.getCard();
						dialog.dispose();
					}
				}
			}
		};
		list.addMouseListener(mouseListener);

		listModelUpdater.waitUntilIdentitiesRead();

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
		private Identity identity;
		private ImageIcon photo;
		private int photoProgress, photoSizeEstimate;
		private boolean error;

		public ListData(BeIDCard card) {
			super();
			this.card = card;
		}

		public BeIDCard getCard() {
			return this.card;
		}

		public ImageIcon getPhoto() {
			return this.photo;
		}

		public Identity getIdentity() {
			return this.identity;
		}

		public void setIdentity(Identity identity) {
			this.identity = identity;
		}

		public void setPhoto(ImageIcon photo) {
			this.photo = photo;
		}

		public int getPhotoProgress() {
			return photoProgress;
		}

		public void setPhotoProgress(int photoProgress) {
			this.photoProgress = photoProgress;
		}

		public void setPhotoSizeEstimate(int photoSizeEstimate) {
			this.photoSizeEstimate = photoSizeEstimate;
		}

		public int getPhotoSizeEstimate() {
			return this.photoSizeEstimate;
		}

		public boolean hasError() {
			return error;
		}

		public void setError() {
			this.error = true;
		}

	}

	private static class EidListCellRenderer extends JPanel
			implements
				ListCellRenderer<Object> {
		private static final long serialVersionUID = -6914001662919942232L;

		public Component getListCellRendererComponent(JList<?> list,
				Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			JPanel panel = new JPanel();
			ListData listData = (ListData) value;
			panel.setLayout(new FlowLayout(FlowLayout.LEFT));

			if (listData.hasError()) {
				panel.add(new JLabel("!"));
				panel.add(new JLabel("!"));
			} else {
				ImageIcon photoIcon = listData.getPhoto();

				if (photoIcon == null) {
					panel.add(new ProgressPanel(listData.getPhotoProgress(),
							listData.getPhotoSizeEstimate()));
				} else {
					panel.add(new JLabel(photoIcon));
				}

				if (isSelected) {
					panel.setBackground(list.getSelectionBackground());
				} else {
					panel.setBackground(list.getBackground());
				}

				panel.add(new IdentityPanel(listData.getIdentity()));
			}
			return panel;
		}
	}

	private static class ProgressPanel extends JPanel {
		private static final long serialVersionUID = 3198272925609784396L;
		private JProgressBar progressBar;

		public ProgressPanel(int progress, int max) {
			super(new GridBagLayout());
			setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

			this.progressBar = new JProgressBar(0, max);
			this.progressBar.setIndeterminate(false);
			this.progressBar.setValue(progress);

			Dimension fixedSize = new Dimension(140, 200);
			setPreferredSize(fixedSize);
			setMinimumSize(fixedSize);
			setMaximumSize(fixedSize);

			fixedSize = new Dimension(100, 16);
			this.progressBar.setPreferredSize(fixedSize);

			add(this.progressBar);
		}
	}

	private static class IdentityPanel extends JPanel {
		private static final long serialVersionUID = 1293396834578252226L;

		public IdentityPanel(Identity identity) {
			super(new GridBagLayout());
			setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
			setOpaque(false);

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.LINE_START;
			gbc.ipady = 4;
			add(new JLabel(identity.getName()), gbc);

			gbc = new GridBagConstraints();
			gbc.gridy = 1;
			gbc.anchor = GridBagConstraints.LINE_START;
			gbc.ipady = 4;
			add(new JLabel(identity.getFirstName() + " "
					+ identity.getMiddleName()), gbc);

			gbc = new GridBagConstraints();
			gbc.gridy = 2;
			gbc.ipady = 8;
			add(Box.createVerticalGlue(), gbc);

			gbc = new GridBagConstraints();
			gbc.gridy = 3;
			gbc.anchor = GridBagConstraints.LINE_START;
			gbc.ipady = 4;
			DateFormat dateFormat = DateFormat.getDateInstance(
					DateFormat.DEFAULT, Locale.getDefault());
			add(new JLabel(identity.getPlaceOfBirth() + " "
					+ dateFormat.format(identity.getDateOfBirth().getTime())),
					gbc);

			gbc = new GridBagConstraints();
			gbc.gridy = 4;
			gbc.ipady = 8;
			add(Box.createVerticalGlue(), gbc);

			gbc = new GridBagConstraints();
			gbc.gridy = 5;
			gbc.anchor = GridBagConstraints.LINE_START;
			gbc.ipady = 4;
			add(new JLabel(identity.getNationality().toUpperCase()), gbc);

			gbc = new GridBagConstraints();
			gbc.gridy = 6;
			gbc.ipady = 8;
			add(Box.createVerticalGlue(), gbc);

			gbc = new GridBagConstraints();
			gbc.gridy = 7;
			gbc.anchor = GridBagConstraints.LINE_START;
			gbc.ipady = 4;
			add(new JLabel(Format.formatCardNumber(identity.getCardNumber())),
					gbc);
		}
	}

	private static class ListModelUpdater {
		private final CountDownLatch identitiesReadLatch;
		private final ArrayList<ListItemUpdater> updaters = new ArrayList<ListItemUpdater>();

		public ListModelUpdater(DefaultListModel<ListData> listModel) {
			this.identitiesReadLatch = new CountDownLatch(listModel.size());
			for (int i = 0; i < listModel.size(); i++) {
				updaters.add(new ListItemUpdater(listModel, i,
						identitiesReadLatch));
			}
		}

		public void waitUntilIdentitiesRead() {
			try {
				identitiesReadLatch.await();
			} catch (InterruptedException e) {
				return;
			}
		}

		public void stop() {
			for (ListItemUpdater updater : updaters) {
				updater.interrupt();
			}

			for (ListItemUpdater updater : updaters) {
				try {
					updater.join();
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}

	private static class ListItemUpdater implements Runnable {
		private DefaultListModel<ListData> listModel;
		private int index;
		private CountDownLatch nameRead;
		private Thread worker;

		public ListItemUpdater(DefaultListModel<ListData> listModel, int index,
				CountDownLatch nameRead) {
			super();
			this.listModel = listModel;
			this.index = index;
			this.nameRead = nameRead;
			worker = new Thread(this, "ListModelUpdater (" + index + ")");
			worker.setDaemon(true);
			worker.start();
		}

		public void join() throws InterruptedException {
			this.worker.join();
		}

		public void interrupt() {
			this.worker.interrupt();
		}

		private void updateInList(ListData listItem) {
			synchronized (this.listModel) {
				this.listModel.set(this.index, listItem);
			}
		}

		@Override
		public void run() {
			final ListData listItem = this.listModel.get(this.index);

			try {
				Identity identity = TlvParser.parse(listItem.getCard()
						.readFile(BeIDFileType.Identity), Identity.class);
				listItem.setIdentity(identity);
				updateInList(listItem);
			} catch (Exception ex) {
				listItem.setError();
				updateInList(listItem);
			} finally {
				this.nameRead.countDown();
			}

			try {
				listItem.setPhotoSizeEstimate(BeIDFileType.Photo
						.getEstimatedMaxSize());
				updateInList(listItem);

				listItem.getCard().addCardListener(new BeIDCardListener() {
					@Override
					public void notifyReadProgress(int offset,
							int estimatedMaxSize) {
						listItem.setPhotoProgress(offset);
						updateInList(listItem);
					}
				});

				byte[] photoFile = listItem.getCard().readFile(
						BeIDFileType.Photo);
				BufferedImage photoImage = ImageIO
						.read(new ByteArrayInputStream(photoFile));
				listItem.setPhoto(new ImageIcon(photoImage));
				updateInList(listItem);
			} catch (Exception ex) {
				listItem.setError();
				updateInList(listItem);
			}
		}
	}
}
