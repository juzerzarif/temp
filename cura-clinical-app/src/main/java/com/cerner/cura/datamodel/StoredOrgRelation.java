package com.cerner.cura.datamodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.Request;
import com.cerner.cura.utils.SerializablePair;
import com.google.common.collect.ImmutableList;

/**
 * POJO used for submitting org relation information.
 *
 * @author Mark Lear(ML015922)
 */
public class StoredOrgRelation implements IRemoteDataModel {
    @NonNull
    @Override
    public String getRequestURL() {
        return "/organization/store_device_org_reltn";
    }

    @Override
    public int getRequestMethod() {
        return Request.Method.POST;
    }

    @NonNull
    @Override
    public CacheBucket getCacheBucket() {
        return CacheBucket.NONE;
    }

    @Nullable
    @Override
    public ImmutableList<SerializablePair<String, String>> getCustomHeaders() {
        return null;
    }

    public String org_id;
    public String org_name;
}
