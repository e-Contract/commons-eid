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

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import javax.smartcardio.ResponseAPDU;

/**
 * 
 * @author Frank Marien
 * 
 */
public class BeIDCardMachine implements Runnable {
	private final Logger logger;
	private State state = State.READY;
	private Timer timePassesEventClock;
	private BELPICCard card;
	private LinkedBlockingQueue<Mission> missionQueue;
	private Mission mission;

	public BeIDCardMachine(BELPICCard card, Logger logger) {
		this.card = card;
		this.logger = logger;
		this.missionQueue = new LinkedBlockingQueue<Mission>();
	}

	public BeIDCardMachine(BELPICCard card) {
		this(card, new VoidLogger());
	}

	public BeIDCardMachine addMission(Mission mission) {
		missionQueue.add(mission);
		return this;
	}

	private enum State {
		READY, ACCESS_EXCLUSIVE, FILE_SELECTED, DATA_READ_SUCCESS, SHARED;
	}

	private enum EventType {
		START, OK, EXCEPTION, TIME_PASSES, DATA, APDU;
	}

	enum ActionType {
		BEGINEXCLUSIVE, SELECTFILE, READBINARY, ENDEXCLUSIVE;
	}

	private void onTIMEPASSESEvent() {
		// TODO Auto-generated method stub

	}

	private void onEXCEPTIONEvent() {
		// TODO Auto-generated method stub

	}

	private void onAPDUEvent() {
		// TODO Auto-generated method stub

	}

	private void onDATAEvent() {
		// TODO Auto-generated method stub

	}

	private void onOKEvent() {
		// TODO Auto-generated method stub

	}

	private void onStartEvent() {
		// TODO Auto-generated method stub

	}

	private void setState(State newState) {
		leavingState(this.state);
		this.state = newState;
		enteredState(newState);
	}

	private void leavingState(State oldState) {
		logger.debug("leaving State [" + oldState + "]");

		switch (oldState) {
			// leaving READY state, enable time passing events
			case READY :
				enableTimePassingEvents();
				break;
		}
	}

	private void enteredState(State newState) {
		logger.debug("entering State [" + newState + "]");

		switch (newState) {
			// entered READY state, disable time passing events, and wait for new orders
			case READY : {

				disableTimePassingEvents();
				try {
					if (missionQueue.isEmpty())
						logger.debug("waiting for next mission..");
					this.mission = missionQueue.take();
				} catch (InterruptedException e) {
					logger.debug("interrupted waiting for next mission");
				}
				break;
			}
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

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

	private synchronized BeIDCardMachine event(Event event) {
		switch (event.getEventType()) {
			case START :
				onStartEvent();
				break;
			case OK :
				onOKEvent();
				break;
			case DATA :
				onDATAEvent();
				break;
			case APDU :
				onAPDUEvent();
				break;
			case EXCEPTION :
				onEXCEPTIONEvent();
				break;
			case TIME_PASSES :
				onTIMEPASSESEvent();
				break;
		}
		return this;
	}
}
