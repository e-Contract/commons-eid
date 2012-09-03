package be.fedict.eid.commons.dialogs;

import java.awt.BorderLayout;
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
import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.client.OutOfCardsException;
import be.fedict.commons.eid.client.CancelledException;
import be.fedict.commons.eid.client.event.BeIDCardListener;
import be.fedict.commons.eid.consumer.Identity;
import be.fedict.commons.eid.consumer.text.Format;
import be.fedict.commons.eid.consumer.tlv.TlvParser;

public class BeIDSelectionDialog extends JDialog {
	private static final long serialVersionUID = -2645729254647365948L;
	private final Component parentComponent;
	private final ListData selectedListData;
	private final JPanel masterPanel;
	private final DefaultListModel listModel;
	private final JList list;
	private final Map<BeIDCard, ListDataUpdater> updaters;
	private int identitiesbeingRead;
	private boolean outOfCards;

	public BeIDSelectionDialog(final Component parentComponent,
			final String title, final Collection<BeIDCard> initialCards) {
		super((Frame) null, title, true);
		this.parentComponent = parentComponent;
		this.selectedListData = new ListData(null);
		this.masterPanel = new JPanel(new BorderLayout());
		this.masterPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16,
				16));
		this.listModel = new DefaultListModel();
		this.list = new JList(this.listModel);
		this.list.setCellRenderer(new EidListCellRenderer());
		this.updaters = new HashMap<BeIDCard, ListDataUpdater>();
		this.masterPanel.add(this.list);
		this.add(this.masterPanel);
		this.identitiesbeingRead = 0;
		this.outOfCards = false;

		for (BeIDCard card : initialCards) {
			eIDCardInserted(card);
		}

		MouseListener mouseListener = new MouseAdapter() {
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
						BeIDSelectionDialog.this.selectedListData.card = listData
								.getCard();
						BeIDSelectionDialog.this.dispose();
					}
				}
			}
		};
		this.list.addMouseListener(mouseListener);
	}

	public void eIDCardInserted(final BeIDCard card) {
		final ListData listData = new ListData(card);
		final ListDataUpdater listDataUpdater = new ListDataUpdater(this,
				listData);
		this.listModel.addElement(listData);
		this.updaters.put(card, listDataUpdater);
		listDataUpdater.start();
	}

	public void eIDCardRemoved(final BeIDCard card) {
		final ListDataUpdater listDataUpdater = this.updaters.get(card);
		listDataUpdater.stop();
		this.listModel.removeElement(listDataUpdater.getListData());

		if (this.listModel.isEmpty()) {
			this.selectedListData.card = null;
			this.outOfCards = true;
			this.dispose();
		} else {
			this.pack();
		}
	}

	public synchronized void startReadingIdentity() {
		System.err.println("start");
		this.identitiesbeingRead++;
		notifyAll();
	}

	public synchronized void endReadingIdentity() {
		System.err.println("end");
		this.identitiesbeingRead--;
		this.pack();
		notifyAll();
	}

	public synchronized void updateListData(
			final ListDataUpdater listDataUpdater, final ListData listData) {
		final int index = this.listModel.indexOf(listData);
		this.listModel.set(index, listData);
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
			setLocationRelativeTo(this.parentComponent);
		} else {
			final Dimension screen = Toolkit.getDefaultToolkit()
					.getScreenSize();
			this.setLocation((screen.width - this.getSize().width) / 2,
					(screen.height - this.getSize().height) / 2);
		}

		this.setResizable(false);
		this.setVisible(true);

		if (this.outOfCards) {
			throw new OutOfCardsException();
		}

		if (this.selectedListData.getCard() == null) {
			throw new CancelledException();
		}

		return this.selectedListData.getCard();

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

		public Component getListCellRendererComponent(final JList list,
				final Object value, final int index, final boolean isSelected,
				final boolean cellHasFocus) {
			final JPanel panel = new JPanel();
			final ListData listData = (ListData) value;
			panel.setLayout(new FlowLayout(FlowLayout.LEFT));

			if (listData.hasError()) {
				panel.add(new JLabel("!"));
				panel.add(new JLabel("!"));
			} else {
				final ImageIcon photoIcon = listData.getPhoto();

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

				if (listData.getIdentity() != null) {
					panel.add(new IdentityPanel(listData.getIdentity()));
				} else {
					panel.add(new JLabel("-"));
				}
			}
			return panel;
		}
	}

	private static class ProgressPanel extends JPanel {
		private static final long serialVersionUID = 3198272925609784396L;
		private JProgressBar progressBar;

		public ProgressPanel(final int progress, final int max) {
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

		public IdentityPanel(final Identity identity) {
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
			final DateFormat dateFormat = DateFormat.getDateInstance(
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

	private static class ListDataUpdater implements Runnable {
		final private BeIDSelectionDialog selectionDialog;
		final private ListData listData;
		final private Thread worker;

		public ListDataUpdater(final BeIDSelectionDialog selectionDialog,
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
				identity = TlvParser.parse(this.listData.getCard().readFile(
						FileType.Identity), Identity.class);
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
