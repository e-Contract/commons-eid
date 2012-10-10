package be.fedict.commons.eid.client;

/**
 * CancelledException is thrown when a method is blocking on
 * a user action through a UI, and the users declines to
 * respond (e.g. by closing the requesting dialog)
 */
public class CancelledException extends BeIDCardsException {
	private static final long serialVersionUID = -4460739679363065683L;
}
