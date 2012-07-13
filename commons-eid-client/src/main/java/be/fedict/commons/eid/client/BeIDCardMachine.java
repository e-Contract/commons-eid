/*
 * Commons eID Project.
 * Copyright (C) 2008-2012 FedICT.
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

package be.fedict.commons.eid.client;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

/**
 * 
 * @author Frank Marien
 * 
 */
public class BeIDCardMachine {
	private final Logger logger;
	private State state = State.READY;
	private Timer timePassesEventClock;
	private BeIDCard card;
	private LinkedBlockingQueue<Mission> missionQueue;

	public BeIDCardMachine(BeIDCard card, Logger logger) {
		this.card = card;
		this.logger = logger;
		this.missionQueue = new LinkedBlockingQueue<Mission>();
	}

	public BeIDCardMachine(BeIDCard card) {
		this(card, new VoidLogger());
	}

	public BeIDCardMachine addMission(Mission mission) {
		missionQueue.add(mission);
		return this;
	}

	private enum State {
		READY, APPLET_SELECTED, ACCESS_EXCLUSIVE, FILE_SELECTED, DATA_READ_SUCCESS, ACCESS_SHARED;
	}

	private enum EventType {
		START, OK, EXCEPTION, TIME_PASSES, DATA, APDU;
	}

	private synchronized BeIDCardMachine event(Event event) {
		switch (event.getEventType()) {
			case START :
				setState(State.READY);
				break;

			case MISSION :
				logger.debug("new mission received");
				switch (event.getMission().getType()) {
					case RETRIEVE_FILE :
						break;
				}
				break;

		}
		return this;
	}

	private synchronized BeIDCardMachine setState(State newState) {
		State oldState = this.state;
		state_will_change(oldState, newState);
		this.state = newState;
		state_has_changed(oldState, newState);
		return this;
	}

	private void state_has_changed(State oldState, State newState) {
		logger.debug("state changed from [" + oldState + "] to [" + newState
				+ "]");

		switch (newState) {

			case READY :
				// entered READY state, block until next mission arrives.
				try {
					if (missionQueue.isEmpty())
						logger.debug("waiting for next mission..");
					event(new Event(missionQueue.take()));
				} catch (InterruptedException e) {
					logger.debug("interrupted waiting for next mission");
				}
				break;
		}
	}

	private void state_will_change(State oldState, State newState) {
		logger.debug("changing state from [" + oldState + "] to [" + newState
				+ "]");

		switch (oldState) {

			case IDLE :
				// leaving IDLE state, enable time passing events
				enableTimePassingEvents();
				break;
		}

		switch (newState) {

			case IDLE :
				// entering IDLE state, disable time passing events
				disableTimePassingEvents();
				break;
		}
	}

	private class Event {
		private final EventType eventType;
		private final byte[] data;
		private final Exception exception;
		private final ResponseAPDU apdu;

		private Event(EventType eventType) {
			this.eventType = eventType;
			this.data = null;
			this.exception = null;
			this.apdu = null;
		}

		private Event(byte[] data) {
			this.eventType = EventType.MISSION;
			this.data = data;
			this.exception = null;
			this.apdu = null;
		}

		private Event(Exception exception) {
			this.eventType = EventType.EXCEPTION;
			this.data = null;
			this.exception = exception;
			this.apdu = null;
		}

		private Event(ResponseAPDU apdu) {
			this.eventType = EventType.APDU;
			this.data = null;
			this.exception = null;
			this.apdu = apdu;
		}

		public EventType getEventType() {
			return this.eventType;
		}

		public Exception getException() {
			return this.exception;
		}

		public ResponseAPDU getResponseAPDU() {
			return this.apdu;
		}

		public byte[] getData() {
			return this.data;
		}

		public String toString() {
			return eventType.toString();
		}
	}

	private void disableTimePassingEvents() {
		logger.debug("Cancelling TIME_PASSES events");
		timePassesEventClock.cancel();
	}

	private void enableTimePassingEvents() {
		logger.debug("Enabling TIME_PASSES events");
		timePassesEventClock = new Timer("timePassesEventClock", true);
		timePassesEventClock.schedule(new TimerTask() {
			@Override
			public void run() {
				logger.debug("Scheduling TIME_PASSES event every second");
				BeIDCardMachine.this.event(new Event(EventType.TIME_PASSES));
			}

		}, 0, 1000);
	}
}
