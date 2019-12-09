package com.cerner.cura.datamodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.Request;
import com.cerner.cura.utils.SerializablePair;
import com.google.common.collect.ImmutableList;

/**
 * POJO used for storing care team images.
 *
 * @author Lam Tran
 */
public class CareTeamPhotoRequest implements IRemoteDataModel {
    private final transient String mUrl;

    public CareTeamPhotoRequest(final String url) {
        mUrl = url;
    }

    @NonNull
    @Override
    public String getRequestURL() {
        return mUrl;
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
}
