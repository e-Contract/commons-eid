package be.fedict.commons.eid.client.spi;

import java.util.Collection;
import be.fedict.commons.eid.client.BeIDCard;

/**
 * An adapter implementing empty or simple implementations of the BeIDCardsUI interface.
 * Intended to be extended by a useful class without having to implement unused methods.
 * For example, in an embedded application where only one card reader is possible,
 * only adviseBeIDCardRequired() and adviseEnd() would ever be called.
 * @author Frank Marien
 *
 */
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
