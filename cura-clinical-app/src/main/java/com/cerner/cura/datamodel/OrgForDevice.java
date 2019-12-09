package com.cerner.cura.datamodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.Request;
import com.cerner.cura.utils.SerializablePair;
import com.google.common.collect.ImmutableList;

/**
 * POJO used for storing information about a patient list.
 *
 * @author Mark Lear(ML015922)
 */
public class OrgForDevice implements IRemoteDataModel {
    @NonNull
    @Override
    public String getRequestURL() {
        return "/organization/organization_for_device";
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

    public String org_id;
    public String org_display;
}
