package be.fedict.commons.eid.client.spi;

import java.util.Collection;
import be.fedict.commons.eid.client.BeIDCard;

public class BeIDCardsIUAdapter implements BeIDCardsUI {

	@Override
	public void adviseCardTerminalRequired() {
	}

	@Override
	public void adviseBeIDCardRequired() {
	}

	@Override
	public BeIDCard selectBeIDCard(final Collection<BeIDCard> availableCards) {
		return availableCards.iterator().next();
	}

	@Override
	public void eIDCardInsertedDuringSelection(final BeIDCard card) {
	}

	@Override
	public void eIDCardRemovedDuringSelection(final BeIDCard card) {
	}

	@Override
	public void adviseEnd() {
	}
}
