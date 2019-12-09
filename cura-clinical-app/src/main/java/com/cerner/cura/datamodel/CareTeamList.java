package com.cerner.cura.datamodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.Request;
import com.cerner.careaware.connect.contacts.model.ContactsCollection;
import com.cerner.cura.utils.SerializablePair;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;

/**
 * POJO used for storing information about  the available relationships.
 *
 * @author Brad Barnhill(bb024928)
 */
public class CareTeamList implements IRemoteDataModel {
    @NonNull
    @Override
    public String getRequestURL() {
        return "/ibus/patient/%s/encounter/%s/careTeam";
    }

    @Override
    public int getRequestMethod() {
        return Request.Method.GET;
    }

    @NonNull
    @Override
    public CacheBucket getCacheBucket() {
        return CacheBucket.PATIENT;
    }

    @Nullable
    @Override
    public ImmutableList<SerializablePair<String, String>> getCustomHeaders() {
        return null;
    }

    public ArrayList<ContactsCollection> primary_section_list;
}