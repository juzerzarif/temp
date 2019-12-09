package com.cerner.cura.datamodel;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.Request;
import com.cerner.cura.utils.SerializablePair;
import com.google.common.collect.ImmutableList;

/**
 * POJO used for storing images.
 *
 * @author Mark Lear(ML015922)
 */
public class PersonnelPhoto implements IRemoteDataModel {
    @NonNull
    @Override
    public String getRequestURL() {
        return "/personnel/%s/photo";
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

    public Bitmap image;
}
