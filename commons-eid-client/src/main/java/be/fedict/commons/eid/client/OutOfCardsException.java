package be.fedict.commons.eid.client;

/**
 * OutOfCardsException is thrown by a UI when the last BeID card is removed
 * while it is attempting to let the user choose between several.
 */
public class OutOfCardsException extends BeIDCardsException {
	private static final long serialVersionUID = -6156756157792880325L;
}
