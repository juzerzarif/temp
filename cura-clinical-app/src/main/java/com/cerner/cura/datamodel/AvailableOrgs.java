package com.cerner.cura.datamodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.Request;
import com.cerner.cura.utils.SerializablePair;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;

/**
 * POJO used for storing information about the available organizations.
 *
 * @author Mark Lear(ML015922)
 */
public class AvailableOrgs implements IRemoteDataModel {
    @NonNull
    @Override
    public String getRequestURL() {
        return "/organization/available_organizations";
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

    public ArrayList<Org> accessibleOrganizations;

    public static class Org {
        public String organizationId;
        public String organizationName;
    }
}
