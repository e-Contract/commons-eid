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

package test.integ.be.fedict.commons.eid.client;

import static org.junit.Assert.assertNotNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.client.BeIDCardsException;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.client.event.BeIDCardListener;
import be.fedict.commons.eid.client.impl.CCID;

public class CCIDExercises {
	protected static final Log LOG = LogFactory.getLog(CCIDExercises.class);
	private BeIDCards beIDCards;

	@Test
	public void listCCIDFeatures() throws Exception {
		final BeIDCard beIDCard = getBeIDCard();
		beIDCard.addCardListener(new TestBeIDCardListener());
		
		for (CCID.FEATURE feature : CCID.FEATURE.values()) {
			LOG.info(feature.name() + "\t" + (beIDCard.cardTerminalHasCCIDFeature(feature)?"AVAILABLE":"NOT AVAILABLE"));
		}
	}
	
	@Test
  public void listCCIDFeaturesWithPPDU() throws Exception {
	  CCID.riskPPDU(true);
    final BeIDCard beIDCard = getBeIDCard();
    beIDCard.addCardListener(new TestBeIDCardListener());
    
    for (CCID.FEATURE feature : CCID.FEATURE.values()) {
      LOG.info(feature.name() + "\t" + (beIDCard.cardTerminalHasCCIDFeature(feature)?"AVAILABLE":"NOT AVAILABLE"));
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
				public void notifyReadProgress(final FileType fileType,
						final int offset, final int estimatedMaxSize) {
					LOG.debug("read progress of " + fileType.name() + ":"
							+ offset + " of " + estimatedMaxSize);
				}

				@Override
				public void notifySigningBegin(final FileType keyType) {
					LOG.debug("signing with "
							+ (keyType == FileType.AuthentificationCertificate
									? "authentication"
									: "non-repudiation") + " key has begun");
				}

				@Override
				public void notifySigningEnd(final FileType keyType) {
					LOG.debug("signing with "
							+ (keyType == FileType.AuthentificationCertificate
									? "authentication"
									: "non-repudiation") + " key has ended");
				}
			});
		} catch (final BeIDCardsException bcex) {
			System.err.println(bcex.getMessage());
		}

		return beIDCard;
	}
}
