package be.fedict.commons.eid.examples.events;

import java.io.IOException;
import java.util.Set;
import javax.smartcardio.CardException;
import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.client.BeIDCardsException;
import be.fedict.commons.eid.client.BeIDFileType;
import be.fedict.commons.eid.consumer.Identity;
import be.fedict.commons.eid.consumer.tlv.TlvParser;

public class BeIDCardsExample {
	/*
	 * get information about BeID cards inserted, from the current thread:
	 */
	public void demonstrate() throws InterruptedException {
		// -------------------------------------------------------------------------------------------------------
		// instantiate a BeIDCardManager with default settings (no logging)
		// -------------------------------------------------------------------------------------------------------
		BeIDCards beIDCards = new BeIDCards();

		// -------------------------------------------------------------------------------------------------------
		// ask it for all CardTerminals that currently contain BeID cards (which
		// may block and possibly interact with the user until there is at least one)
		// -------------------------------------------------------------------------------------------------------
		Set<BeIDCard> cards = beIDCards.getBeIDCards();

		System.out.println("BeID Cards:");

		for (BeIDCard card : cards) {
			byte[] idData;

			try {
				idData = card.readFile(BeIDFileType.Identity);
			} catch (CardException e) {
				System.err.println("oops! can't read identity file");
				return;
			} catch (IOException e) {
				System.err.println("oops! can't read identity file");
				return;
			}

			Identity id = TlvParser.parse(idData, Identity.class);
			System.out.println(id.firstName + "'s card");
		}
	}

	public static void main(String[] args) throws InterruptedException,
			BeIDCardsException {
		BeIDCardsExample examples = new BeIDCardsExample();
		examples.demonstrate();
	}

}
