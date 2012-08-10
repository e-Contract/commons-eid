package be.fedict.commons.eid.examples.events;

import java.io.IOException;
import java.util.Set;
import javax.smartcardio.CardException;
import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.client.BeIDFileType;
import be.fedict.commons.eid.consumer.Identity;
import be.fedict.commons.eid.consumer.tlv.TlvParser;

public class BeIDCardsExample {
	/*
	 * get information about BeID cards inserted, from the current thread:
	 */
	public void demonstrate() {
		//-------------------------------------------------------------------------------------------------------
		// instantiate a BeIDCardManager with default settings (no logging, private CardAndTerminalManager)
		//-------------------------------------------------------------------------------------------------------
		BeIDCards beIDCards = new BeIDCards();

		if (!beIDCards.hasBeIDCards())
			System.out.println("There are not BeID Cards.. please insert some");

		//-------------------------------------------------------------------------------------------------------
		// ask it for all CardTerminals that currently contain BeID cards (which may block until there is at least one)
		//-------------------------------------------------------------------------------------------------------
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

	public static void main(String[] args) throws InterruptedException {
		BeIDCardsExample examples = new BeIDCardsExample();
		examples.demonstrate();
	}

}
