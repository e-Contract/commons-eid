/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2015 e-Contract.be BVBA.
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

import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.client.impl.BeIDDigest;

/**
 * eID based JCA private key. Should not be used directly, but via the
 * {@link BeIDKeyStore}.
 * 
 * @author Frank Cornelis
 * @see BeIDKeyStore
 */
public class BeIDPrivateKey implements PrivateKey {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory.getLog(BeIDPrivateKey.class);

	private final FileType certificateFileType;

	private BeIDCard beIDCard;

	private final boolean logoff;

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
	}

	/**
	 * Main constructor.
	 * 
	 * @param certificateFileType
	 * @param beIDCard
	 * @param logoff
	 * @param autoRecovery
	 * @param beIDKeyStore
	 * @param applicationName
	 */
	public BeIDPrivateKey(final FileType certificateFileType,
			final BeIDCard beIDCard, final boolean logoff,
			boolean autoRecovery, BeIDKeyStore beIDKeyStore,
			String applicationName) {
		LOG.debug("constructor: " + certificateFileType);
		this.certificateFileType = certificateFileType;
		this.beIDCard = beIDCard;
		this.logoff = logoff;
		this.autoRecovery = autoRecovery;
		this.beIDKeyStore = beIDKeyStore;
		this.applicationName = applicationName;
	}

	@Override
	public String getAlgorithm() {
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

	byte[] sign(final byte[] digestValue, final String digestAlgo)
			throws SignatureException {
		LOG.debug("auto recovery: " + this.autoRecovery);
		final BeIDDigest beIDDigest = beIDDigests.get(digestAlgo);
		if (null == beIDDigest) {
			throw new SignatureException("unsupported algo: " + digestAlgo);
		}
		byte[] signatureValue;
		try {
			if (this.autoRecovery) {
				/*
				 * We keep a copy of the authentication certificate to make sure
				 * that the automatic recovery only operates against the same
				 * eID card.
				 */
				if (null == this.authenticationCertificate) {
					try {
						this.authenticationCertificate = this.beIDCard
								.getAuthenticationCertificate();
					} catch (Exception e) {
						// don't fail here
					}
				}
			}
			try {
				signatureValue = this.beIDCard.sign(digestValue, beIDDigest,
						this.certificateFileType, false, this.applicationName);
			} catch (Exception e) {
				if (this.autoRecovery) {
					LOG.debug("trying to recover...");
					this.beIDCard = this.beIDKeyStore.getBeIDCard(true);
					if (null != this.authenticationCertificate) {
						X509Certificate newAuthenticationCertificate = this.beIDCard
								.getAuthenticationCertificate();
						if (false == this.authenticationCertificate
								.equals(newAuthenticationCertificate)) {
							throw new SignatureException("different eID card");
						}
					}
					signatureValue = this.beIDCard.sign(digestValue,
							beIDDigest, this.certificateFileType, false,
							this.applicationName);
				} else {
					throw e;
				}
			}
			if (this.logoff) {
				this.beIDCard.logoff();
			}
		} catch (final Exception ex) {
			throw new SignatureException(ex);
		}
		return signatureValue;
	}
}
