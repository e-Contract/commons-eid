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

package be.fedict.commons.eid.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.client.OutOfCardsException;
import be.fedict.commons.eid.client.CancelledException;
import be.fedict.commons.eid.client.event.BeIDCardListener;
import be.fedict.commons.eid.consumer.Identity;
import be.fedict.commons.eid.consumer.text.Format;
import be.fedict.commons.eid.consumer.tlv.TlvParser;

/**
 * Dynamically changing dialog listing BeIDCards by photo and main identity data
 * part of the DefaultBeIDCardsUI. Based on the original, static BeID selector
 * dialog from eid-applet.
 * 
 * @author Frank Marien
 * 
 */
public class BeIDSelector {
	private JDialog dialog;
	private JPanel masterPanel;
	private DefaultListModel listModel;
	private JList list;

	private final Component parentComponent;
	private final ListData selectedListData;
	private final Map<BeIDCard, ListDataUpdater> updaters;
	private int identitiesbeingRead;
	private boolean outOfCards;

	public BeIDSelector(final Component parentComponent, final String title,
			final Collection<BeIDCard> initialCards) {
		this.parentComponent = parentComponent;
		this.selectedListData = new ListData(null);
		this.updaters = new HashMap<BeIDCard, ListDataUpdater>();
		this.identitiesbeingRead = 0;
		this.outOfCards = false;

		initComponents(title, initialCards);

		for (BeIDCard card : initialCards) {
			addEIDCard(card);
		}

		MouseListener mouseListener = new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent mouseEvent) {
				final JList theList = (JList) mouseEvent.getSource();
				if (mouseEvent.getClickCount() == 2) {
					final int index = theList.locationToIndex(mouseEvent
							.getPoint());
					if (index >= 0) {
						stop();
						final Object object = theList.getModel().getElementAt(
								index);
						final ListData listData = (ListData) object;
						BeIDSelector.this.selectedListData.card = listData
								.getCard();
						BeIDSelector.this.dialog.dispose();
					}
				}
			}
		};
		this.list.addMouseListener(mouseListener);
	}

	public void addEIDCard(final BeIDCard card) {
		final ListData listData = new ListData(card);
		this.addToList(listData);
		final ListDataUpdater listDataUpdater = new ListDataUpdater(this,
				listData);
		this.updaters.put(card, listDataUpdater);
		listDataUpdater.start();
	}

	public void removeEIDCard(final BeIDCard card) {
		final ListDataUpdater listDataUpdater = this.updaters.get(card);
		listDataUpdater.stop();
		this.updaters.remove(card);
		this.removeFromList(listDataUpdater.getListData());
	}

	public synchronized void startReadingIdentity() {
		this.identitiesbeingRead++;
		notifyAll();
	}

	public synchronized void endReadingIdentity() {
		this.identitiesbeingRead--;
		this.repack();
		notifyAll();
	}

	public synchronized void waitUntilIdentitiesRead() {
		try {
			while (this.identitiesbeingRead > 0) {
				this.wait();
			}
		} catch (final InterruptedException iex) {
			return;
		}
	}

	public void stop() {
		for (ListDataUpdater updater : this.updaters.values()) {
			updater.stop();
		}

		for (ListDataUpdater updater : this.updaters.values()) {
			try {
				updater.join();
			} catch (final InterruptedException iex) {
				return;
			}
		}
	}

	public BeIDCard choose() throws OutOfCardsException, CancelledException {
		this.waitUntilIdentitiesRead();

		if (this.parentComponent != null) {
			this.dialog.setLocationRelativeTo(this.parentComponent);
		} else {
			final Dimension screen = Toolkit.getDefaultToolkit()
					.getScreenSize();
			this.dialog.setLocation(
					(screen.width - this.dialog.getSize().width) / 2,
					(screen.height - this.dialog.getSize().height) / 2);
		}

		this.dialog.setResizable(false);
		this.dialog.setVisible(true);

		// dialog is modal so setVisible will block until dispose is called.
		// mouseListener calls dispose after setting selection, on double-click
		// removeFromList calls dispose after setting outOfCards when last card
		// removed
		// user closing dialog will have no selection and outOfCards not set
		// indicating cancel

		if (this.outOfCards) {
			throw new OutOfCardsException();
		}
		if (this.selectedListData.getCard() == null) {
			throw new CancelledException();
		}

		return this.selectedListData.getCard();

	}

	// ----------------------------------------------------------------------------------------------------
	// methods to alter the dialog in a Swing-Thread safe way
	// ----------------------------------------------------------------------------------------------------

	private void initComponents(final String title,
			final Collection<BeIDCard> initialCards) {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					BeIDSelector.this.dialog = new JDialog((Frame) null, title,
							true);
					BeIDSelector.this.masterPanel = new JPanel(
							new BorderLayout());
					BeIDSelector.this.masterPanel.setBorder(BorderFactory
							.createEmptyBorder(16, 16, 16, 16));
					BeIDSelector.this.listModel = new DefaultListModel();
					BeIDSelector.this.list = new JList(
							BeIDSelector.this.listModel);
					BeIDSelector.this.list
							.setCellRenderer(new EidListCellRenderer());
					BeIDSelector.this.masterPanel.add(BeIDSelector.this.list);
					BeIDSelector.this.dialog.add(BeIDSelector.this.masterPanel);
				}
			});
		} catch (final InterruptedException e) {
		} catch (final InvocationTargetException e) {
		}
	}

	private synchronized void updateListData(
			final ListDataUpdater listDataUpdater, final ListData listData) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				final int index = BeIDSelector.this.listModel.indexOf(listData);
				if (index != -1) {
					BeIDSelector.this.listModel.set(index, listData);
				}
			}
		});
	}

	private void addToList(final ListData listData) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				BeIDSelector.this.listModel.addElement(listData);
			}
		});
	}

	private void removeFromList(final ListData listData) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				BeIDSelector.this.listModel.removeElement(listData);
				if (BeIDSelector.this.listModel.isEmpty()) {
					BeIDSelector.this.selectedListData.card = null;
					BeIDSelector.this.outOfCards = true;
					BeIDSelector.this.dialog.dispose();
				} else {
					BeIDSelector.this.dialog.pack();
				}
			}
		});
	}

	private void repack() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				BeIDSelector.this.dialog.pack();
			}
		});
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

		public ListData(final BeIDCard card) {
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

		public void setIdentity(final Identity identity) {
			this.identity = identity;
		}

		public void setPhoto(final ImageIcon photo) {
			this.photo = photo;
		}

		public int getPhotoProgress() {
			return this.photoProgress;
		}

		public void setPhotoProgress(final int photoProgress) {
			this.photoProgress = photoProgress;
		}

		public void setPhotoSizeEstimate(final int photoSizeEstimate) {
			this.photoSizeEstimate = photoSizeEstimate;
		}

		public int getPhotoSizeEstimate() {
			return this.photoSizeEstimate;
		}

		public boolean hasError() {
			return this.error;
		}

		public void setError() {
			this.error = true;
		}

	}

	private static class EidListCellRenderer extends JPanel
			implements
				ListCellRenderer {
		private static final long serialVersionUID = -6914001662919942232L;

		@Override
		public Component getListCellRendererComponent(final JList list,
				final Object value, final int index, final boolean isSelected,
				final boolean cellHasFocus) {
			final JPanel panel = new JPanel();
			final ListData listData = (ListData) value;
			panel.setLayout(new FlowLayout(FlowLayout.LEFT));

			if (listData.hasError()) {
				panel.setBackground(redden(isSelected ? list
						.getSelectionBackground() : list.getBackground()));
			} else {
				panel.setBackground(isSelected
						? list.getSelectionBackground()
						: list.getBackground());
			}

			panel.add(new PhotoPanel(listData.getPhoto(), listData
					.getPhotoProgress(), listData.getPhotoSizeEstimate()));
			panel.add(new IdentityPanel(listData.getIdentity()));

			return panel;
		}

		private Color redden(final Color originalColor) {
			final Color less = originalColor.darker().darker();
			final Color more = originalColor.brighter().brighter();
			return new Color(more.getRed(), less.getGreen(), less.getBlue());
		}
	}

	private static class PhotoPanel extends JPanel {
		private static final long serialVersionUID = -8779658857811406077L;
		private JProgressBar progressBar;

		public PhotoPanel(final ImageIcon photo, final int progress,
				final int max) {
			super(new GridBagLayout());
			setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
			Dimension fixedSize = new Dimension(140, 200);
			setPreferredSize(fixedSize);
			setMinimumSize(fixedSize);
			setMaximumSize(fixedSize);

			if (photo == null) {
				this.progressBar = new JProgressBar(0, max);
				this.progressBar.setIndeterminate(false);
				this.progressBar.setValue(progress);
				fixedSize = new Dimension(100, 16);
				this.progressBar.setPreferredSize(fixedSize);
				this.add(this.progressBar);
			} else {
				this.add(new JLabel(photo));
			}
		}
	}

	private static class IdentityPanel extends JPanel {
		private static final long serialVersionUID = 1293396834578252226L;

		public IdentityPanel(final Identity identity) {
			super(new GridBagLayout());
			setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
			setMinimumSize(new Dimension(140, 200));
			setOpaque(false);

			if (identity == null) {
				this.add(new JLabel("-"));
			} else {
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
				final DateFormat dateFormat = DateFormat.getDateInstance(
						DateFormat.DEFAULT, Locale.getDefault());
				add(new JLabel(
						identity.getPlaceOfBirth()
								+ " "
								+ dateFormat.format(identity.getDateOfBirth()
										.getTime())), gbc);

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
				add(new JLabel(
						Format.formatCardNumber(identity.getCardNumber())), gbc);
			}
		}
	}

	private static class ListDataUpdater implements Runnable {
		final private BeIDSelector selectionDialog;
		final private ListData listData;
		final private Thread worker;

		public ListDataUpdater(final BeIDSelector selectionDialog,
				final ListData listData) {
			super();
			this.selectionDialog = selectionDialog;
			this.listData = listData;
			this.worker = new Thread(this, "ListDataUpdater");
			this.worker.setDaemon(true);
			setWorkerName(null, null);
			this.selectionDialog.startReadingIdentity();
		}

		public void stop() {
			this.worker.interrupt();
		}

		public void start() {
			this.worker.start();
		}

		public void join() throws InterruptedException {
			this.worker.join();
		}

		@Override
		public void run() {
			Identity identity = null;
			setWorkerName(null, "Reading Identity");

			try {
				identity = TlvParser.parse(
						this.listData.getCard().readFile(FileType.Identity),
						Identity.class);
				this.listData.setIdentity(identity);
				this.selectionDialog.updateListData(this, this.listData);
				setWorkerName(identity, "Identity Read");
			} catch (final Exception ex) {
				this.listData.setError();
				this.selectionDialog.updateListData(this, this.listData);
				setWorkerName(identity, "Error Reading Identity");
			} finally {
				this.selectionDialog.endReadingIdentity();
			}

			setWorkerName(identity, "Reading Photo");

			try {
				this.listData.setPhotoSizeEstimate(FileType.Photo
						.getEstimatedMaxSize());
				this.selectionDialog.updateListData(this, this.listData);

				this.listData.getCard().addCardListener(new BeIDCardListener() {
					@Override
					public void notifyReadProgress(final FileType fileType,
							final int offset, final int estimatedMaxSize) {
						ListDataUpdater.this.listData.setPhotoProgress(offset);
						ListDataUpdater.this.selectionDialog.updateListData(
								ListDataUpdater.this,
								ListDataUpdater.this.listData);
					}

					@Override
					public void notifySigningBegin(final FileType keyType) {
						// can safely ignore this here
					}

					@Override
					public void notifySigningEnd(final FileType keyType) {
						// can safely ignore this here
					}
				});

				final byte[] photoFile = this.listData.getCard().readFile(
						FileType.Photo);
				final BufferedImage photoImage = ImageIO
						.read(new ByteArrayInputStream(photoFile));
				this.listData.setPhoto(new ImageIcon(photoImage));
				this.selectionDialog.updateListData(this, this.listData);
				setWorkerName(identity, "All Done");
			} catch (final Exception ex) {
				this.listData.setError();
				this.selectionDialog.updateListData(this, this.listData);
				setWorkerName(identity, "Error Reading Photo");
			}
		}

		private void setWorkerName(final Identity identity,
				final String activity) {
			final StringBuilder builder = new StringBuilder("ListDataUpdater");

			if (identity != null) {
				builder.append(" [");
				if (identity.getFirstName() != null) {
					builder.append(identity.getFirstName());
					builder.append(" ");
				}

				if (identity.getName() != null) {
					builder.append(identity.getName());
				}

				builder.append("]");

			}

			if (activity != null) {
				builder.append(" [");
				builder.append(activity);
				builder.append("]");
			}

			this.worker.setName(builder.toString());
		}

		public ListData getListData() {
			return this.listData;
		}
	}
}
