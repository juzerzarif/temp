package com.cerner.cura.datamodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.Request;
import com.cerner.cura.utils.SerializablePair;
import com.google.common.collect.ImmutableList;

/**
 * POJO used for storing a summary about a personnel.
 *
 * @author Mark Lear(ML015922)
 */
public class PersonnelInfoSummary implements IRemoteDataModel {
    @NonNull
    @Override
    public String getRequestURL() {
        return "/personnel/summary";
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

    public String organization_name;
    public String organization_id;
    public long has_one_organization;
}
