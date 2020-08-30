/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2017-2020 e-Contract.be BV.
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

package test.integ.be.fedict.commons.eid.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.client.BeIDCardsException;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.client.event.BeIDCardListener;
import be.fedict.commons.eid.client.impl.CCID;

public class CCIDExercises {
	protected static final Logger LOGGER = LoggerFactory.getLogger(CCIDExercises.class);
	private BeIDCards beIDCards;

	@Test
	public void listCCIDFeatures() throws Exception {
		final BeIDCard beIDCard = getBeIDCard();
		beIDCard.addCardListener(new TestBeIDCardListener());

		for (CCID.FEATURE feature : CCID.FEATURE.values()) {
			LOGGER.info(feature.name() + "\t"
					+ (beIDCard.cardTerminalHasCCIDFeature(feature) ? "AVAILABLE" : "NOT AVAILABLE"));
		}
	}

	@Test
	public void listCCIDFeaturesWithPPDU() throws Exception {
		CCID.addPPDUName("digipass 870");
		CCID.addPPDUName("digipass 875");
		CCID.addPPDUName("digipass 920");
		final BeIDCard beIDCard = getBeIDCard();
		beIDCard.addCardListener(new TestBeIDCardListener());

		for (CCID.FEATURE feature : CCID.FEATURE.values()) {
			LOGGER.info(feature.name() + "\t"
					+ (beIDCard.cardTerminalHasCCIDFeature(feature) ? "AVAILABLE" : "NOT AVAILABLE"));
		}
	}

	protected BeIDCard getBeIDCard() {
		this.beIDCards = new BeIDCards(new TestLogger());
		BeIDCard beIDCard = null;
		try {
			beIDCard = this.beIDCards.getOneBeIDCard();
			assertNotNull(beIDCard);

			beIDCard.addCardListener(new BeIDCardListener() {
				@Override
				public void notifyReadProgress(final FileType fileType, final int offset, final int estimatedMaxSize) {
					LOGGER.debug("read progress of {}: {} of {}", fileType.name(), offset, estimatedMaxSize);
				}

				@Override
				public void notifySigningBegin(final FileType keyType) {
					LOGGER.debug("signing with {} key has begun",
							(keyType == FileType.AuthentificationCertificate ? "authentication" : "non-repudiation"));
				}

				@Override
				public void notifySigningEnd(final FileType keyType) {
					LOGGER.debug("signing with {} key has ended",
							(keyType == FileType.AuthentificationCertificate ? "authentication" : "non-repudiation"));
				}
			});
		} catch (final BeIDCardsException bcex) {
			System.err.println(bcex.getMessage());
		}

		return beIDCard;
	}
}
