package com.cerner.cura.datamodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.Request;
import com.cerner.cura.utils.SerializablePair;
import com.cerner.cura.vitals.datamodel.common.LegacyVitalSigns;
import com.google.common.collect.ImmutableList;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * POJO used for storing information about the patient lists.
 *
 * @author Brad Barnhill(BB024928)
 * @author Mark Lear(ML015922)
 */
public class PatientViewSummary implements IRemoteDataModel {
    @NonNull
    @Override
    public String getRequestURL() {
        return "/patients/%s/encounters/%s/summary";
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

    public Sections sections;

    public static class Sections implements Serializable {
        public AllergySection allergy;
        public LabResultsSection labResults;
        public LegacyVitalSigns vitalSigns;
        public ItemsForReviewCountSection itemsForReviewCount;
    }

    public static class ItemsForReviewCountSection implements Serializable {
        public int nonCriticalCount;
        public int criticalCount;
    }

    public static class AllergySection implements Serializable {
        public ArrayList<Allergy> allergies;
        public ArrayList<String> permissions;
        public boolean allowNKA;
        public boolean allowNKMA;
        @SuppressWarnings ("NegativelyNamedBooleanVariable")
        public boolean notRecorded;
        public ArrayList<String> sectionStates;
        public boolean allAllergiesNotLoaded;
    }

    public static class LabResultsSection implements Serializable {
        public ArrayList<LabResultGroup> sections;
    }

    public static class LabResultGroup implements Serializable {
        public String groupTag;
        public int mostRecentResultCount;
        public String mostRecentResultElapsedTime;
        public String sectionDisplay;
        public int sequence;
    }

    public static class Allergy implements Serializable {
        public int id;
        public String substance;
        public String importance;
    }

    public static class MetricImperialValue implements Serializable {
        public double resultValue;
        public String units;
    }

    public static class DateResult implements Serializable {
        public String preciseDateTime;
        public String isoDate;
        public String isoTime;
    }

    public static class StringResult implements Serializable {
        public String resultString;
    }
}
