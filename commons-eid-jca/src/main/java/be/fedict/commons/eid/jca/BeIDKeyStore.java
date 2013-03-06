/*
 * Commons eID Project.
 * Copyright (C) 2008-2012 FedICT.
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

package be.fedict.commons.eid.jca;

import java.awt.Component;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.LoadStoreParameter;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import javax.smartcardio.CardTerminal;
import javax.swing.JFrame;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.client.CancelledException;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.client.impl.VoidLogger;
import be.fedict.commons.eid.client.spi.BeIDCardUI;
import be.fedict.commons.eid.client.spi.BeIDCardsUI;
import be.fedict.commons.eid.client.spi.Logger;
import be.fedict.eid.commons.dialogs.DefaultBeIDCardUI;
import be.fedict.eid.commons.dialogs.DefaultBeIDCardsUI;
import be.fedict.eid.commons.dialogs.Messages;

/**
 * eID based JCA {@link KeyStore}. Used to load eID key material via standard
 * JCA API calls. Once the JCA security provider has been registered you have a
 * new key store available named "BeID". Two key aliases are available:
 * <ul>
 * <li>"Authentication" which gives you access to the eID authentication private
 * key and corresponding certificate chain.</li>
 * <li>"Signature" which gives you access to the eID non-repudiation private key
 * and corresponding certificate chain.</li>
 * </ul>
 * Further the Citizen CA certificate can be accessed via the "CA" alias and the
 * Root CA certificate can be accessed via the "Root" alias.
 * <p/>
 * Supports the eID specific {@link BeIDKeyStoreParameter} key store parameter.
 * You can also let any {@link JFrame} implement the
 * {@link KeyStore.LoadStoreParameter} interface. If you pass this to
 * {@link KeyStore#load(LoadStoreParameter)} the keystore will use that Swing
 * frame as parent for positioning the dialogs.
 * <p/>
 * Usage:
 * <p/>
 * 
 * <pre>
 * import java.security.KeyStore;
 * import java.security.cert.X509Certificate;
 * import java.security.PrivateKey;
 * 
 * ...
 * KeyStore keyStore = KeyStore.getInstance("BeID");
 * keyStore.load(null);
 * X509Certificate authnCertificate = (X509Certificate) keyStore
 * 			.getCertificate("Authentication");
 * PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey(
 * 			"Authentication", null);
 * Certificate[] signCertificateChain = keyStore.getCertificateChain("Signature");
 * </pre>
 * 
 * @author Frank Cornelis
 * @see BeIDKeyStoreParameter
 * @see BeIDProvider
 */
public class BeIDKeyStore extends KeyStoreSpi {

	private static final Log LOG = LogFactory.getLog(BeIDKeyStore.class);

	private BeIDKeyStoreParameter keyStoreParameter;

	private BeIDCard beIDCard;

	private List<X509Certificate> authnCertificateChain;

	private List<X509Certificate> signCertificateChain;

	private X509Certificate citizenCaCertificate;

	private X509Certificate rootCaCertificate;

	private X509Certificate authnCertificate;

	private X509Certificate signCertificate;

	private CardTerminal cardTerminal;

	@Override
	public Key engineGetKey(final String alias, final char[] password)
			throws NoSuchAlgorithmException, UnrecoverableKeyException {
		LOG.debug("engineGetKey: " + alias);
		final BeIDCard beIDCard = getBeIDCard();
		boolean logoff;
		boolean autoRecovery;
		if (null == this.keyStoreParameter) {
			logoff = false;
			autoRecovery = false;
		} else {
			logoff = this.keyStoreParameter.getLogoff();
			autoRecovery = this.keyStoreParameter.getAutoRecovery();
		}
		if ("Authentication".equals(alias)) {
			final BeIDPrivateKey beIDPrivateKey = new BeIDPrivateKey(
					FileType.AuthentificationCertificate, beIDCard, logoff,
					autoRecovery, this);
			return beIDPrivateKey;
		}
		if ("Signature".equals(alias)) {
			final BeIDPrivateKey beIDPrivateKey = new BeIDPrivateKey(
					FileType.NonRepudiationCertificate, beIDCard, logoff,
					autoRecovery, this);
			return beIDPrivateKey;
		}
		return null;
	}

	@Override
	public Certificate[] engineGetCertificateChain(final String alias) {
		LOG.debug("engineGetCertificateChain: " + alias);
		final BeIDCard beIDCard = getBeIDCard();
		if ("Signature".equals(alias)) {
			try {
				if (null == this.signCertificateChain) {
					this.signCertificateChain = beIDCard
							.getSigningCertificateChain();
					this.signCertificate = this.signCertificateChain.get(0);
					this.citizenCaCertificate = this.signCertificateChain
							.get(1);
					this.rootCaCertificate = this.signCertificateChain.get(2);
				}
			} catch (final Exception ex) {
				LOG.error("error: " + ex.getMessage(), ex);
				return null;
			}
			return this.signCertificateChain.toArray(new X509Certificate[]{});
		}
		if ("Authentication".equals(alias)) {
			try {
				if (null == this.authnCertificateChain) {
					this.authnCertificateChain = beIDCard
							.getAuthenticationCertificateChain();
					this.authnCertificate = this.authnCertificateChain.get(0);
					this.citizenCaCertificate = this.authnCertificateChain
							.get(1);
					this.rootCaCertificate = this.authnCertificateChain.get(2);
				}
			} catch (final Exception ex) {
				LOG.error("error: " + ex.getMessage(), ex);
				return null;
			}
			return this.authnCertificateChain.toArray(new X509Certificate[]{});
		}
		return null;
	}

	@Override
	public Certificate engineGetCertificate(final String alias) {
		LOG.debug("engineGetCertificate: " + alias);
		final BeIDCard beIDCard = getBeIDCard();
		if ("Signature".equals(alias)) {
			try {
				if (null == this.signCertificate) {
					this.signCertificate = beIDCard.getSigningCertificate();
				}
			} catch (final Exception ex) {
				LOG.warn("error: " + ex.getMessage(), ex);
				return null;
			}
			return this.signCertificate;
		}
		if ("Authentication".equals(alias)) {
			try {
				if (null == this.authnCertificate) {
					this.authnCertificate = beIDCard
							.getAuthenticationCertificate();
				}
			} catch (final Exception ex) {
				LOG.warn("error: " + ex.getMessage(), ex);
				return null;
			}
			return this.authnCertificate;
		}
		if ("CA".equals(alias)) {
			try {
				if (null == this.citizenCaCertificate) {
					this.citizenCaCertificate = beIDCard.getCACertificate();
				}
			} catch (Exception e) {
				LOG.warn("error: " + e.getMessage(), e);
				return null;
			}
			return this.citizenCaCertificate;
		}
		if ("Root".equals(alias)) {
			try {
				if (null == this.rootCaCertificate) {
					this.rootCaCertificate = beIDCard.getRootCACertificate();
				}
			} catch (Exception e) {
				LOG.warn("error: " + e.getMessage(), e);
				return null;
			}
			return this.rootCaCertificate;
		}
		return null;
	}

	@Override
	public Date engineGetCreationDate(final String alias) {
		final X509Certificate certificate = (X509Certificate) this
				.engineGetCertificate(alias);
		if (null == certificate) {
			return null;
		}
		return certificate.getNotBefore();
	}

	@Override
	public void engineSetKeyEntry(final String alias, final Key key,
			final char[] password, final Certificate[] chain)
			throws KeyStoreException {
		throw new KeyStoreException();
	}

	@Override
	public void engineSetKeyEntry(final String alias, final byte[] key,
			final Certificate[] chain) throws KeyStoreException {
		throw new KeyStoreException();
	}

	@Override
	public void engineSetCertificateEntry(final String alias,
			final Certificate cert) throws KeyStoreException {
		throw new KeyStoreException();
	}

	@Override
	public void engineDeleteEntry(final String alias) throws KeyStoreException {
		throw new KeyStoreException();
	}

	@Override
	public Enumeration<String> engineAliases() {
		LOG.debug("engineAliases");
		final Vector<String> aliases = new Vector<String>();
		aliases.add("Authentication");
		aliases.add("Signature");
		aliases.add("CA");
		aliases.add("Root");
		return aliases.elements();
	}

	@Override
	public boolean engineContainsAlias(final String alias) {
		LOG.debug("engineContainsAlias: " + alias);
		if ("Authentication".equals(alias)) {
			return true;
		}
		if ("Signature".equals(alias)) {
			return true;
		}
		if ("Root".equals(alias)) {
			return true;
		}
		if ("CA".equals(alias)) {
			return true;
		}
		return false;
	}

	@Override
	public int engineSize() {
		return 2;
	}

	@Override
	public boolean engineIsKeyEntry(final String alias) {
		LOG.debug("engineIsKeyEntry: " + alias);
		if ("Authentication".equals(alias)) {
			return true;
		}
		if ("Signature".equals(alias)) {
			return true;
		}
		return false;
	}

	@Override
	public boolean engineIsCertificateEntry(final String alias) {
		LOG.debug("engineIsCertificateEntry: " + alias);
		if ("Root".equals(alias)) {
			return true;
		}
		if ("CA".equals(alias)) {
			return true;
		}
		return false;
	}

	@Override
	public void engineStore(LoadStoreParameter param) throws IOException,
			NoSuchAlgorithmException, CertificateException {
		LOG.debug("engineStore");
		super.engineStore(param);
	}

	@Override
	public Entry engineGetEntry(String alias, ProtectionParameter protParam)
			throws KeyStoreException, NoSuchAlgorithmException,
			UnrecoverableEntryException {
		LOG.debug("engineGetEntry: " + alias);
		if ("Authentication".equals(alias) || "Signature".equals(alias)) {
			PrivateKey privateKey = (PrivateKey) engineGetKey(alias, null);
			Certificate[] chain = engineGetCertificateChain(alias);
			PrivateKeyEntry privateKeyEntry = new PrivateKeyEntry(privateKey,
					chain);
			return privateKeyEntry;
		}
		if ("CA".equals(alias) || "Root".equals(alias)) {
			Certificate certificate = engineGetCertificate(alias);
			TrustedCertificateEntry trustedCertificateEntry = new TrustedCertificateEntry(
					certificate);
			return trustedCertificateEntry;
		}
		return super.engineGetEntry(alias, protParam);
	}

	@Override
	public void engineSetEntry(String alias, Entry entry,
			ProtectionParameter protParam) throws KeyStoreException {
		LOG.debug("engineSetEntry: " + alias);
		super.engineSetEntry(alias, entry, protParam);
	}

	@Override
	public boolean engineEntryInstanceOf(String alias,
			Class<? extends Entry> entryClass) {
		LOG.debug("engineEntryInstanceOf: " + alias);
		return super.engineEntryInstanceOf(alias, entryClass);
	}

	@Override
	public String engineGetCertificateAlias(final Certificate cert) {
		return null;
	}

	@Override
	public void engineStore(final OutputStream stream, final char[] password)
			throws IOException, NoSuchAlgorithmException, CertificateException {
	}

	@Override
	public void engineLoad(final InputStream stream, final char[] password)
			throws IOException, NoSuchAlgorithmException, CertificateException {
	}

	@Override
	public void engineLoad(final LoadStoreParameter param) throws IOException,
			NoSuchAlgorithmException, CertificateException {
		LOG.debug("engineLoad"); /*
									 * Allows for a KeyStore to be re-loaded several
									 * times.
									 */
		this.beIDCard = null;
		this.authnCertificateChain = null;
		this.signCertificateChain = null;
		this.authnCertificate = null;
		this.signCertificate = null;
		this.citizenCaCertificate = null;
		this.rootCaCertificate = null;
		if (null == param) {
			return;
		}
		if (param instanceof BeIDKeyStoreParameter) {
			this.keyStoreParameter = (BeIDKeyStoreParameter) param;
			return;
		}
		if (param instanceof JFrame) {
			this.keyStoreParameter = new BeIDKeyStoreParameter();
			JFrame frame = (JFrame) param;
			this.keyStoreParameter.setParentComponent(frame);
			return;
		}
		throw new NoSuchAlgorithmException();
	}

	private BeIDCard getBeIDCard() {
		return getBeIDCard(false);
	}

	public BeIDCard getBeIDCard(boolean recover) {
		boolean cardReaderStickiness;
		if (null != this.keyStoreParameter) {
			cardReaderStickiness = this.keyStoreParameter
					.getCardReaderStickiness();
		} else {
			cardReaderStickiness = false;
		}
		if (recover) {
			LOG.debug("recovering from error");
			this.beIDCard = null;
		}
		if (null != this.beIDCard) {
			return this.beIDCard;
		}
		if (null != this.keyStoreParameter) {
			this.beIDCard = this.keyStoreParameter.getBeIDCard();
		}
		if (null != this.beIDCard) {
			return this.beIDCard;
		}
		Component parentComponent;
		Locale locale;
		Logger logger;
		if (null != this.keyStoreParameter) {
			parentComponent = this.keyStoreParameter.getParentComponent();
			locale = this.keyStoreParameter.getLocale();
			logger = this.keyStoreParameter.getLogger();
		} else {
			parentComponent = null;
			locale = null;
			logger = null;
		}
		if (null == locale) {
			locale = Locale.getDefault();
		}
		if (null == logger) {
			logger = new VoidLogger();
		}
		final Messages messages = new Messages(locale);
		final BeIDCardsUI ui = new DefaultBeIDCardsUI(parentComponent, messages);
		final BeIDCards beIDCards = new BeIDCards(logger, ui);
		beIDCards.setLocale(locale);
		try {
			CardTerminal stickyCardTerminal;
			if (cardReaderStickiness) {
				stickyCardTerminal = this.cardTerminal;
			} else {
				stickyCardTerminal = null;
			}
			this.beIDCard = beIDCards.getOneBeIDCard(stickyCardTerminal);
			if (cardReaderStickiness) {
				this.cardTerminal = this.beIDCard.getCardTerminal();
				LOG.debug("sticky card reader: " + this.cardTerminal.getName());
			}
			final BeIDCardUI userInterface = new DefaultBeIDCardUI(
					parentComponent, messages);
			this.beIDCard.setUI(userInterface);
		} catch (final CancelledException cex) {
			throw new SecurityException("user cancelled");
		}
		if (null == this.beIDCard) {
			throw new SecurityException("missing eID card");
		}
		return this.beIDCard;
	}
}
