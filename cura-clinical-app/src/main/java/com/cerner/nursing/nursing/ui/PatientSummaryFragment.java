package com.cerner.nursing.nursing.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cerner.cura.allergies.ui.AllergyFragment;
import com.cerner.cura.base.CuraRefreshAuthnFragment;
import com.cerner.cura.base.OnDrawerListener;
import com.cerner.cura.base.PatientContext;
import com.cerner.cura.datamodel.PatientViewSummary;
import com.cerner.cura.items_for_review.ui.ItemsForReviewFragment;
import com.cerner.cura.items_for_review.ui.elements.ItemsForReviewCardListItem;
import com.cerner.cura.requestor.CuraResponseListener;
import com.cerner.cura.requestor.JsonRequestor;
import com.cerner.cura.ui.elements.IListItem;
import com.cerner.cura.ui.elements.TextListItem;
import com.cerner.cura.ui.elements.cards.ListCard;
import com.cerner.cura.utils.AppUtils;
import com.cerner.cura.vitals.ui.AssessmentsFragment;
import com.cerner.cura.vitals.ui.ResultListItem;
import com.cerner.cura.vitals.ui.VitalsAndMeasurementsFragment;
import com.cerner.cura.vitals.ui.VitalsHistoryFragment;
import com.cerner.cura.vitals.utils.Utils;
import com.cerner.cura.vitals.utils.VitalsConversionUtils;
import com.cerner.ion.log.Logger;
import com.cerner.ion.request.CernResponse;
import com.cerner.nursing.nursing.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shows the patient selected
 *
 * @author Mark Lear (ML015922)
 */
public class PatientSummaryFragment extends CuraRefreshAuthnFragment {
    private transient OnFragmentSelected mCallback;
    private transient OnDrawerListener mDrawerCallback;
    private transient ViewHolder mViewHolder;
    private boolean mLockDownItemsForReviewCard;
    private boolean mEnableAllergiesCard = true;

    private static final String NO_KNOWN_ALLERGIES = "NO_KNOWN_ALLERGIES";
    private static final String MEDICATION_ALLERGIES_NOT_RECORDED = "MEDICATION_ALLERGIES_NOT_RECORDED";
    private static final String NO_KNOWN_MEDICATION_ALLERGIES = "NO_KNOWN_MEDICATION_ALLERGIES";

    public PatientSummaryFragment() {
        TAG = PatientSummaryFragment.class.getSimpleName();
        mCheckpointName = "CURA_NURSING_PATIENTSUMMARY_READ";
        setDefaultCacheLookback(300);
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        if (activity instanceof OnFragmentSelected) {
            mCallback = (OnFragmentSelected) activity;
        } else {
            throw new ClassCastException(activity + " must implement OnFragmentSelected");
        }

        //If the starting activity has a drawer (not required) then we will need it to flex the indicator
        if (activity instanceof OnDrawerListener) {
            mDrawerCallback = (OnDrawerListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mCallback = null;
        mDrawerCallback = null;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        AppUtils.logCheckPoint(getActivity(), mCheckpointName, AppUtils.CHECKPOINT_EVENT_LOAD);

        final View view = inflater.inflate(R.layout.patient_summary_fragment, container, false);

        if (getActivity().isFinishing()) {
            return view;
        }

        mViewHolder = new ViewHolder(view);
        setupClickListeners();

        if (Utils.canChartVitals()) {
            mViewHolder.mVitalsAndMeasurementsCard.setTitleButtonImage(R.drawable.plus_button_with_background);
        }

        setRefreshLayout(view.findViewById(R.id.refresh_layout_patient_summary));

        return view;
    }

    @Override
    public void onAuthnStart() {
        super.onAuthnStart();
        getData(DataArgs.REFRESH);
    }

    @Override
    public void setupActionBar() {
        if (mDrawerCallback != null) {
            mDrawerCallback.setDrawerIndicatorEnabled(true);
        }

        if (getActivity() != null) {
            final ActionBar bar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (bar != null) {
                bar.setTitle(R.string.patient_summary_fragment_title);
                bar.setDisplayShowHomeEnabled(true);
                bar.setHomeButtonEnabled(true);
            }
        }
    }

    @Override
    public void getData(final DataArgs dataArgs) {
        Logger.i(TAG, "GetData");
        super.getData(dataArgs);

        if (PatientContext.isPatientSelected()) {
            if (PatientContext.hasRelationship()) {
                final CuraResponseListener<PatientViewSummary> responseListener = new CuraResponseListener<>(
                        mRefreshData.getDialogController(), TAG, mCheckpointName, PatientViewSummary.class, mResponseProcessor, getActivity(), true);
                JsonRequestor.sendRequest(responseListener, this, getCacheLookback(), mRefreshData.getCacheOnly(), null, PatientContext.getPatientId(), PatientContext.getEncounterId());
            } else {
                Logger.d(TAG, "No relationship established with " + PatientContext.getPatientId());
            }
        } else {
            Logger.e(TAG, "No patient is selected in the patient context.");
        }
    }

    @Override
    public void onResponse(@NonNull final Object model) {
        super.onResponse(model);
        mLockDownItemsForReviewCard = false;
        mEnableAllergiesCard = true;
        Logger.i(TAG, "onResponse");

        if (getView() == null || getActivity() == null) {
            return;
        }

        // if the response is the patient view summary
        if (model instanceof PatientViewSummary) {
            final PatientViewSummary patientViewSummary = (PatientViewSummary) model;

            final List<IListItem> itemsForReviewList = new ArrayList<>();

            if (patientViewSummary.sections != null && patientViewSummary.sections.itemsForReviewCount != null) {
                if (patientViewSummary.sections.itemsForReviewCount.criticalCount == 0 && patientViewSummary.sections.itemsForReviewCount.nonCriticalCount == 0) {
                    mLockDownItemsForReviewCard = true;
                }

                itemsForReviewList.add(new ItemsForReviewCardListItem(getActivity().getString(R.string.items_for_review_fragment_title), patientViewSummary.sections.itemsForReviewCount.criticalCount,
                                                                      patientViewSummary.sections.itemsForReviewCount.nonCriticalCount).setItemClickable(!mLockDownItemsForReviewCard));
            } else {
                mLockDownItemsForReviewCard = true;
                itemsForReviewList.add(new ItemsForReviewCardListItem(getActivity().getString(R.string.items_for_review_fragment_title), 0, 0).setItemClickable(false));
            }

            mViewHolder.mItemsForReviewCard.setItemsList(itemsForReviewList);

            mViewHolder.mVitalsAndMeasurementsCard.setItemsList(buildVitalAndMeasurementCard(patientViewSummary));
            mViewHolder.mVitalsAndMeasurementsCard.setTitleButtonListener(patientViewSummary.sections != null && patientViewSummary.sections.vitalSigns != null
                                                                          && patientViewSummary.sections.vitalSigns.allowVitalsCharting ? __ -> {
                if (mCallback != null) {
                    mCallback.onFragmentSelected(AssessmentsFragment.class, null);
                }
            } : null);

            final List<IListItem> allergiesList = new ArrayList<>();
            allergiesList.add(new TextListItem(buildAllergiesDisplay(patientViewSummary), R.layout.allergy_text_layout).setItemClickable(mEnableAllergiesCard));
            mViewHolder.mAllergiesCard.setItemsList(allergiesList);
        } else {
            throw new IllegalArgumentException("Model not of type PatientViewSummary");
        }

        setFragmentVisibility(true);
        notifyResponseReceivedListeners(PatientSummaryFragment.class);
    }

    @Override
    public void onNoContentResponse(@NonNull final CernResponse response, final Class clazz) {
        super.onNoContentResponse(response, clazz);

        setFragmentVisibility(true);
    }

    /**
     * Check for the severity of the allergy, if its type is severe, it will be red. Otherwise, it will be black.
     */
    private String setAllergyDisplayStringAndColor(final PatientViewSummary.Allergy allergy) {
        if (getActivity() == null) {
            return null;
        }

        if (allergy != null && allergy.substance != null && !allergy.substance.isEmpty()) {
            int colorResource = R.color.ContentPrimary;
            if (AppUtils.ALLERGY_HIGH.equals(allergy.importance)) {
                colorResource = R.color.StatusCritical;
            }

            return AppUtils.setHtmlTextColor(allergy.substance, ContextCompat.getColor(getActivity(), colorResource), getActivity());
        }

        return null;
    }

    /**
     * Sort a list of {@link com.cerner.cura.datamodel.PatientViewSummary.Allergy} items
     *
     * @param allergies list of {@link com.cerner.cura.datamodel.PatientViewSummary.Allergy} items
     * @return sorted list of {@link com.cerner.cura.datamodel.PatientViewSummary.Allergy} items
     */
    private static List<PatientViewSummary.Allergy> sortAllergiesArray(final List<PatientViewSummary.Allergy> allergies) {
        Collections.sort(allergies, (lhs, rhs) -> {
            if (rhs == null) {
                if (lhs == null) {
                    return 0;
                }
                return -1;
            } else if (lhs == null) {
                return 1;
            }

            if (!lhs.importance.equals(rhs.importance)) {
                return allergySeverityToInt(lhs).compareTo(allergySeverityToInt(rhs));
            }

            if (rhs.substance == null) {
                if (lhs.substance == null) {
                    return 0;
                }
                return -1;
            } else if (lhs.substance == null) {
                return 1;
            }

            return lhs.substance.compareToIgnoreCase(rhs.substance);
        });

        return allergies;
    }

    //NOTE: keep in sync with the same function in PatientSummaryFragment
    private static Integer allergySeverityToInt(@NonNull final PatientViewSummary.Allergy allergy) {
        switch (allergy.importance) {
            case AppUtils.ALLERGY_HIGH:
                return 1;
            case AppUtils.ALLERGY_NORMAL:
                return 2;
            case AppUtils.ALLERGY_LOW:
                return 3;
            case "":
                return 5;
            default:
                return 4;
        }
    }

    private List<IListItem> buildVitalAndMeasurementCard(final PatientViewSummary viewSummary) {
        final Context context = getActivity();

        if (context == null || getResources() == null) {
            return null;
        }

        final List<IListItem> vitalItemList = new ArrayList<>();

        if (viewSummary.sections == null || viewSummary.sections.vitalSigns == null) {
            return vitalItemList;
        }

        if (viewSummary.sections.vitalSigns.temperature != null) {
            vitalItemList.add(new ResultListItem<>(context, VitalsConversionUtils.getResult(context, viewSummary.sections.vitalSigns.temperature), false, false));
        }

        if (viewSummary.sections.vitalSigns.heartRateGeneric != null) {
            vitalItemList.add(new ResultListItem<>(context, VitalsConversionUtils.getResult(context, viewSummary.sections.vitalSigns.heartRateGeneric), false, false));
        }

        if (viewSummary.sections.vitalSigns.respiratoryRate != null) {
            vitalItemList.add(new ResultListItem<>(context, VitalsConversionUtils.getResult(context, viewSummary.sections.vitalSigns.respiratoryRate), false, false));
        }

        if (viewSummary.sections.vitalSigns.bloodPressureGeneric != null) {
            vitalItemList.add(new ResultListItem<>(context, VitalsConversionUtils.getResult(context, viewSummary.sections.vitalSigns.bloodPressureGeneric), false, false));
        }

        if (viewSummary.sections.vitalSigns.oxygenSaturation != null) {
            vitalItemList.add(new ResultListItem<>(context, VitalsConversionUtils.getResult(context, viewSummary.sections.vitalSigns.oxygenSaturation), false, false));
        }

        if (viewSummary.sections.vitalSigns.painScale != null) {
            vitalItemList.add(new ResultListItem<>(context, VitalsConversionUtils.getResult(context, viewSummary.sections.vitalSigns.painScale), false, false));
        }

        if (viewSummary.sections.vitalSigns.weight != null) {
            vitalItemList.add(new ResultListItem<>(context, VitalsConversionUtils.getResult(context, viewSummary.sections.vitalSigns.weight), false, false));
        }

        if (viewSummary.sections.vitalSigns.height != null) {
            vitalItemList.add(new ResultListItem<>(context, VitalsConversionUtils.getResult(context, viewSummary.sections.vitalSigns.height), false, false));
        }

        if (viewSummary.sections.vitalSigns.bodyMassIndex != null) {
            vitalItemList.add(new ResultListItem<>(context, VitalsConversionUtils.getResult(context, viewSummary.sections.vitalSigns.bodyMassIndex), false, false));
        }

        return vitalItemList;
    }

    private void setupClickListeners() {
        mViewHolder.mItemsForReviewCard.setOnClickListener(view -> {
            if (mCallback != null && !mLockDownItemsForReviewCard) {
                Logger.d(TAG, String.format("Summary section %s selected", ItemsForReviewFragment.class.toString()));
                mCallback.onFragmentSelected(ItemsForReviewFragment.class, null);
            }
        });

        mViewHolder.mVitalsAndMeasurementsCard.setViewAllClickHandler(view -> {
            if (mCallback != null) {
                Logger.d(TAG, String.format("Summary section %s selected", VitalsAndMeasurementsFragment.class.toString()));
                mCallback.onFragmentSelected(VitalsAndMeasurementsFragment.class, null);
            }
        });

        mViewHolder.mVitalsAndMeasurementsCard.setOnListCardItemClickListener(listItem -> {
            if (listItem instanceof ResultListItem && mCallback != null) {
                mCallback.onFragmentSelected(VitalsHistoryFragment.class, VitalsHistoryFragment.buildArguments(((ResultListItem<Object>) listItem).getData()));
            }
        });

        mViewHolder.mAllergiesCard.setOnClickListener(view -> {
            if (mCallback != null && mEnableAllergiesCard) {
                Logger.d(TAG, String.format("Summary section %s selected", AllergyFragment.class.toString()));
                mCallback.onFragmentSelected(AllergyFragment.class, null);
            }
        });
    }

    private String buildAllergiesDisplay(@NonNull final PatientViewSummary patient) {
        //noinspection ConstantConditions
        if (getActivity() == null || patient == null || patient.sections == null) {
            return null;
        }

        if (patient.sections.allergy == null || patient.sections.allergy.notRecorded || patient.sections.allergy.allergies == null) {
            mEnableAllergiesCard = false;
            return AppUtils.setHtmlTextColor(getString(R.string.no_allergies_recorded), ContextCompat.getColor(getActivity(), R.color.StatusCritical), getActivity());
        } else if (patient.sections.allergy.sectionStates.contains(NO_KNOWN_ALLERGIES)) {
            mEnableAllergiesCard = false;
            return AppUtils.setHtmlTextColor(getString(R.string.no_known_allergies), ContextCompat.getColor(getActivity(), R.color.ContentPrimary), getActivity());
        } else if (patient.sections.allergy.sectionStates.contains(MEDICATION_ALLERGIES_NOT_RECORDED)) {
            mEnableAllergiesCard = false;
            return AppUtils.setHtmlTextColor(getString(R.string.no_medication_allergies_recorded), ContextCompat.getColor(getActivity(), R.color.ContentPrimary), getActivity());
        } else if (patient.sections.allergy.sectionStates.contains(NO_KNOWN_MEDICATION_ALLERGIES)) {
            mEnableAllergiesCard = false;
            return AppUtils.setHtmlTextColor(getString(R.string.no_known_medication_allergies), ContextCompat.getColor(getActivity(), R.color.ContentPrimary), getActivity());
        }

        mEnableAllergiesCard = true;
        if (patient.sections.allergy.allAllergiesNotLoaded) {
            return AppUtils.setHtmlTextColor(getString(R.string.all_allergies_not_shown_title), ContextCompat.getColor(getActivity(), R.color.ContentPrimary), getActivity());
        }

        final StringBuilder allergiesDisplay = new StringBuilder();
        final List<PatientViewSummary.Allergy> allergies = sortAllergiesArray(patient.sections.allergy.allergies);

        for (final PatientViewSummary.Allergy allergy : allergies) {
            if (allergiesDisplay.length() > 0) {
                allergiesDisplay.append(AppUtils.setHtmlTextColor(getString(R.string.list_delineator), ContextCompat.getColor(getActivity(), R.color.ContentPrimary), getActivity()));
            }

            allergiesDisplay.append(setAllergyDisplayStringAndColor(allergy));
        }

        return allergiesDisplay.toString();
    }

    static class ViewHolder {
        final ListCard mItemsForReviewCard;
        final ListCard mVitalsAndMeasurementsCard;
        final ListCard mAllergiesCard;

        public ViewHolder(final View root) throws IllegalArgumentException, NullPointerException {
            if (root == null) {
                throw new IllegalArgumentException("root may not be null");
            }

            mItemsForReviewCard = root.findViewById(R.id.patientsummary_itemforreviewcard);
            mVitalsAndMeasurementsCard = root.findViewById(R.id.patientsummary_vitalsandmeasurements);
            mAllergiesCard = root.findViewById(R.id.patientsummary_allergiescard);

            if (mItemsForReviewCard == null || mVitalsAndMeasurementsCard == null || mAllergiesCard == null) {
                throw new NullPointerException("ViewHolder failed to find all views");
            }
        }
    }
}
