package com.cerner.cura.datamodel.common;

import java.io.Serializable;

/**
 * POJO used for storing information about a patient list patients.
 *
 * @author Brad Barnhill (BB024928)
 */
public class PatientListPatient implements Serializable {
    public enum REVIEW_INDICATOR {NONE, NON_CRITICAL, CRITICAL}

    public String personId;
    public String encounterId;
    public String nameFullFormatted;
    public FuzzyDateTime dateOfBirth;
    public String ageDisplay;
    public String sexAbbr;
    public String roomLocationDisplay;
    public String bedLocationDisplay;
    public boolean relationshipInd;
    public short reviewIndicator;
    public boolean nameAlertInd;
    public String pprCd;
}