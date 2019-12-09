package com.cerner.cura.datamodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.Request;
import com.cerner.cura.utils.SerializablePair;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;

/**
 * POJO used for storing information about the patient lists.
 *
 * @author Mark Lear(ML015922)
 */
public class PatientLists implements IRemoteDataModel {
    @NonNull
    @Override
    public String getRequestURL() {
        return "/patient_lists/all";
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

    public ArrayList<PatientListInfo> listOfLists;

    public static class PatientListInfo {
        public String patientListName;
        public String patientListId;
    }
}
