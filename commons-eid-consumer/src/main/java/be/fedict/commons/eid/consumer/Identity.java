/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2015-2020 e-Contract.be BV.
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

package be.fedict.commons.eid.consumer;

import java.io.Serializable;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.GregorianCalendar;

import be.fedict.commons.eid.consumer.tlv.ChipNumberDataConvertor;
import be.fedict.commons.eid.consumer.tlv.ConvertData;
import be.fedict.commons.eid.consumer.tlv.DateOfBirthDataConvertor;
import be.fedict.commons.eid.consumer.tlv.DateOfBirthMaskDataConvertor;
import be.fedict.commons.eid.consumer.tlv.DocumentTypeConvertor;
import be.fedict.commons.eid.consumer.tlv.GenderDataConvertor;
import be.fedict.commons.eid.consumer.tlv.OriginalData;
import be.fedict.commons.eid.consumer.tlv.SpecialOrganisationConvertor;
import be.fedict.commons.eid.consumer.tlv.SpecialStatusConvertor;
import be.fedict.commons.eid.consumer.tlv.TlvField;
import be.fedict.commons.eid.consumer.tlv.ValidityDateDataConvertor;
import be.fedict.commons.eid.consumer.tlv.WorkPermitConvertor;

/**
 * Holds all fields within the eID identity file. The annotations are used by
 * the TLV parser to parse the identity file as stored in the eID card to an
 * object of this class.
 * 
 * @author Frank Cornelis
 * @see Address
 * @see TlvField
 * @see ConvertData
 * 
 */
public class Identity implements Serializable {

	/*
	 * We implement serializable to allow this class to be used in distributed
	 * containers as defined in the Servlet v2.4 specification.
	 */
	private static final long serialVersionUID = 1L;

	@TlvField(1)
	public String cardNumber;

	@TlvField(2)
	@ConvertData(ChipNumberDataConvertor.class)
	public String chipNumber;

	@TlvField(3)
	@ConvertData(ValidityDateDataConvertor.class)
	public GregorianCalendar cardValidityDateBegin;

	@TlvField(4)
	@ConvertData(ValidityDateDataConvertor.class)
	public GregorianCalendar cardValidityDateEnd;

	@TlvField(5)
	public String cardDeliveryMunicipality;

	@TlvField(6)
	public String nationalNumber;

	@TlvField(7)
	public String name;

	@TlvField(8)
	public String firstName;

	@TlvField(9)
	public String middleName;

	@TlvField(10)
	public String nationality;

	@TlvField(11)
	public String placeOfBirth;

	@TlvField(12)
	@ConvertData(DateOfBirthDataConvertor.class)
	public GregorianCalendar dateOfBirth;

	@TlvField(12)
	@ConvertData(DateOfBirthMaskDataConvertor.class)
	public DateMask dateOfBirthMask;

	@TlvField(13)
	@ConvertData(GenderDataConvertor.class)
	public Gender gender;

	/**
	 * Optional Noble Condition.
	 */
	@TlvField(14)
	public String nobleCondition;

	@TlvField(15)
	@ConvertData(DocumentTypeConvertor.class)
	public DocumentType documentType;

	@TlvField(16)
	@ConvertData(SpecialStatusConvertor.class)
	public SpecialStatus specialStatus;

	@TlvField(17)
	public byte[] photoDigest;

	@TlvField(18)
	public String duplicate;

	@TlvField(19)
	@ConvertData(SpecialOrganisationConvertor.class)
	public SpecialOrganisation specialOrganisation;

	@TlvField(20)
	public boolean memberOfFamily;

	@TlvField(21)
	public String dateAndCountryOfProtection;

	@TlvField(22)
	@ConvertData(WorkPermitConvertor.class)
	public WorkPermit workPermit;

	@TlvField(23)
	public String employerVATNumber1;

	@TlvField(24)
	public String employerVATNumber2;

	@TlvField(25)
	public String regionalFileNumber;

	@TlvField(26)
	public byte[] basicPublicKeyDigest;

	@OriginalData
	public byte[] data;

	/*
	 * We're also providing getters and a toString to make this class more useful
	 * within web frameworks like JBoss Seam.
	 */

	public String getCardNumber() {
		return this.cardNumber;
	}

	public String getChipNumber() {
		return this.chipNumber;
	}

	public GregorianCalendar getCardValidityDateBegin() {
		return this.cardValidityDateBegin;
	}

	public GregorianCalendar getCardValidityDateEnd() {
		return this.cardValidityDateEnd;
	}

	public String getCardDeliveryMunicipality() {
		return this.cardDeliveryMunicipality;
	}

	public String getNationalNumber() {
		return this.nationalNumber;
	}

	public String getName() {
		return this.name;
	}

	public String getFirstName() {
		return this.firstName;
	}

	public String getMiddleName() {
		return this.middleName;
	}

	public String getNationality() {
		return this.nationality;
	}

	public String getPlaceOfBirth() {
		return this.placeOfBirth;
	}

	public GregorianCalendar getDateOfBirth() {
		return this.dateOfBirth;
	}

	public DateMask getDateOfBirthMask() {
		return dateOfBirthMask;
	}

	public Gender getGender() {
		return this.gender;
	}

	public String getNobleCondition() {
		return this.nobleCondition;
	}

	public DocumentType getDocumentType() {
		return this.documentType;
	}

	public byte[] getPhotoDigest() {
		return this.photoDigest;
	}

	public SpecialStatus getSpecialStatus() {
		return this.specialStatus;
	}

	public String getDuplicate() {
		return this.duplicate;
	}

	public boolean isMemberOfFamily() {
		return this.memberOfFamily;
	}

	public SpecialOrganisation getSpecialOrganisation() {
		return this.specialOrganisation;
	}

	public String getDateAndCountryOfProtection() {
		return this.dateAndCountryOfProtection;
	}

	public WorkPermit getWorkPermit() {
		return this.workPermit;
	}

	public String getEmployerVATNumber1() {
		return this.employerVATNumber1;
	}

	public String getEmployerVATNumber2() {
		return this.employerVATNumber2;
	}

	public String getRegionalFileNumber() {
		return this.regionalFileNumber;
	}

	public byte[] getData() {
		return this.data;
	}

	/**
	 * Gives back the digest value of the basic public key. This field is available
	 * on belpic applet version 1.8 eID cards.
	 * 
	 * @return
	 */
	public byte[] getBasicPublicKeyDigest() {
		return this.basicPublicKeyDigest;
	}

	/**
	 * Gives back the age of the person.
	 * 
	 * @return
	 */
	public int getAge() {
		return getAge(null);
	}

	public int getAge(Clock clock) {
		LocalDate dob = LocalDate.of(this.dateOfBirth.get(GregorianCalendar.YEAR),
				this.dateOfBirth.get(GregorianCalendar.MONTH) + 1,
				this.dateOfBirth.get(GregorianCalendar.DAY_OF_MONTH));
		LocalDate now;
		if (null == clock) {
			now = LocalDate.now();
		} else {
			now = LocalDate.now(clock);
		}
		Period age = Period.between(dob, now);
		int years = age.getYears();
		return years;
	}

	@Override
	public String toString() {
		return "[" + this.name + " " + this.firstName + "]";
	}
}
