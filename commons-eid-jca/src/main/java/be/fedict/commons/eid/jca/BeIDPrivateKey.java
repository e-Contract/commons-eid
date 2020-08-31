/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2015-2020 e-Contract.be BV.
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

import java.io.IOException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.smartcardio.CardException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.client.impl.BeIDDigest;
import be.fedict.commons.eid.client.spi.UserCancelledException;

/**
 * eID based JCA private key. Should not be used directly, but via the
 * {@link BeIDKeyStore}.
 * 
 * @author Frank Cornelis
 * @see BeIDKeyStore
 */
public class BeIDPrivateKey implements PrivateKey {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LoggerFactory.getLogger(BeIDPrivateKey.class);

	private final FileType certificateFileType;

	private BeIDCard beIDCard;

	private final boolean logoff;

	private final boolean allowFailingLogoff;

	private final boolean autoRecovery;

	private final BeIDKeyStore beIDKeyStore;

	private final static Map<String, BeIDDigest> beIDDigests;

	private final String applicationName;

	private X509Certificate authenticationCertificate;

	static {
		beIDDigests = new HashMap<String, BeIDDigest>();
		beIDDigests.put("SHA-1", BeIDDigest.SHA_1);
		beIDDigests.put("SHA-224", BeIDDigest.SHA_224);
		beIDDigests.put("SHA-256", BeIDDigest.SHA_256);
		beIDDigests.put("SHA-384", BeIDDigest.SHA_384);
		beIDDigests.put("SHA-512", BeIDDigest.SHA_512);
		beIDDigests.put("NONE", BeIDDigest.NONE);
		beIDDigests.put("RIPEMD128", BeIDDigest.RIPEMD_128);
		beIDDigests.put("RIPEMD160", BeIDDigest.RIPEMD_160);
		beIDDigests.put("RIPEMD256", BeIDDigest.RIPEMD_256);
		beIDDigests.put("SHA-1-PSS", BeIDDigest.SHA_1_PSS);
		beIDDigests.put("SHA-256-PSS", BeIDDigest.SHA_256_PSS);
		beIDDigests.put("SHA-256-ECDSA", BeIDDigest.ECDSA_SHA_2_256);
		beIDDigests.put("SHA-384-ECDSA", BeIDDigest.ECDSA_SHA_2_384);
		beIDDigests.put("SHA-512-ECDSA", BeIDDigest.ECDSA_SHA_2_512);
		beIDDigests.put("SHA3-256-ECDSA", BeIDDigest.ECDSA_SHA_3_256);
		beIDDigests.put("SHA3-384-ECDSA", BeIDDigest.ECDSA_SHA_3_384);
		beIDDigests.put("SHA3-512-ECDSA", BeIDDigest.ECDSA_SHA_3_512);
	}

	/**
	 * Main constructor.
	 * 
	 * @param certificateFileType
	 * @param beIDCard
	 * @param logoff
	 * @param allowFailingLogoff
	 * @param autoRecovery
	 * @param beIDKeyStore
	 * @param applicationName
	 */
	public BeIDPrivateKey(final FileType certificateFileType, final BeIDCard beIDCard, final boolean logoff,
			final boolean allowFailingLogoff, boolean autoRecovery, BeIDKeyStore beIDKeyStore, String applicationName) {
		LOGGER.debug("constructor: {}", certificateFileType);
		this.certificateFileType = certificateFileType;
		this.beIDCard = beIDCard;
		this.logoff = logoff;
		this.allowFailingLogoff = allowFailingLogoff;
		this.autoRecovery = autoRecovery;
		this.beIDKeyStore = beIDKeyStore;
		this.applicationName = applicationName;
	}

	@Override
	public String getAlgorithm() {
		if (this.beIDCard.isEC()) {
			return "EC";
		}
		return "RSA";
	}

	@Override
	public String getFormat() {
		return null;
	}

	@Override
	public byte[] getEncoded() {
		return null;
	}

	byte[] sign(final byte[] digestValue, final String digestAlgo) throws SignatureException {
		LOGGER.debug("auto recovery: {}", this.autoRecovery);
		final BeIDDigest beIDDigest = beIDDigests.get(digestAlgo);
		if (null == beIDDigest) {
			throw new SignatureException("unsupported algo: " + digestAlgo);
		}
		byte[] signatureValue;
		try {
			if (this.autoRecovery) {
				/*
				 * We keep a copy of the authentication certificate to make sure that the
				 * automatic recovery only operates against the same eID card.
				 */
				if (null == this.authenticationCertificate) {
					try {
						this.authenticationCertificate = this.beIDCard.getAuthenticationCertificate();
					} catch (IOException | InterruptedException | CertificateException | CardException e) {
						// don't fail here
					}
				}
			}
			try {
				signatureValue = this.beIDCard.sign(digestValue, beIDDigest, this.certificateFileType, false,
						this.applicationName);
			} catch (UserCancelledException | IOException | InterruptedException | CardException e) {
				if (this.autoRecovery) {
					LOGGER.debug("trying to recover...");
					this.beIDCard = this.beIDKeyStore.getBeIDCard(true);
					if (null != this.authenticationCertificate) {
						X509Certificate newAuthenticationCertificate = this.beIDCard.getAuthenticationCertificate();
						if (false == this.authenticationCertificate.equals(newAuthenticationCertificate)) {
							throw new SignatureException("different eID card");
						}
					}
					signatureValue = this.beIDCard.sign(digestValue, beIDDigest, this.certificateFileType, false,
							this.applicationName);
				} else {
					throw e;
				}
			}
			if (this.logoff) {
				try {
					this.beIDCard.logoff();
				} catch (Exception e) {
					if (this.allowFailingLogoff) {
						LOGGER.error("eID logoff failed.");
					} else {
						throw e;
					}
				}
			}
		} catch (final Exception ex) {
			if (ex instanceof UserCancelledException) {
				throw new UserCancelledSignatureException(ex);
			}
			throw new SignatureException(ex);
		}
		return signatureValue;
	}
}
