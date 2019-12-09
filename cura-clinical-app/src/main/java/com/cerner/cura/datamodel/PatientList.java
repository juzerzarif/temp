package com.cerner.cura.datamodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.Request;
import com.cerner.cura.datamodel.common.PatientListPatient;
import com.cerner.cura.utils.SerializablePair;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;

/**
 * POJO used for storing information about a patient list.
 *
 * @author Mark Lear(ML015922)
 */
public class PatientList implements IRemoteDataModel {
    @NonNull
    @Override
    public String getRequestURL() {
        return "/patient_list/default_list";
    }

    @Override
    public int getRequestMethod() {
        return Request.Method.GET;
    }

    @NonNull
    @Override
    public CacheBucket getCacheBucket() {
        return CacheBucket.USER;
    }

    @Nullable
    @Override
    public ImmutableList<SerializablePair<String, String>> getCustomHeaders() {
        return null;
    }

    public short has_device_organization;
    public String patientListId;
    public String patientListName;
    public ArrayList<PatientListPatient> patientList;
}
