package com.cerner.cura.datamodel;

import android.support.annotation.NonNull;

/**
 * POJO used for storing information about a patient list fetched by list id.
 *
 * @author Brad Barnhill (bb024928)
 */
public class PatientListById extends PatientList {
    @NonNull
    @Override
    public String getRequestURL() {
        return "/patient_list/list_id/%s";
    }
}
