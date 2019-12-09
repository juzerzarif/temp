package com.cerner.nursing.nursing.ui;

import android.app.Fragment;

import com.cerner.cura.base.CuraAuthnFragment;
import com.cerner.cura.items_for_review.ui.ItemDetailsOrdersFragment;
import com.cerner.cura.medications.ui.CodeListFragment;
import com.cerner.cura.medications.ui.CommentsFragment;
import com.cerner.cura.medications.ui.FreetextEntryFragment;
import com.cerner.cura.medications.ui.LastGivenDetailsFragment;
import com.cerner.cura.medications.ui.MedIntervalDetailFragment;
import com.cerner.cura.medications.ui.MedsAlertBaseFragment;
import com.cerner.cura.medications.ui.MedsChartingDetailsFragment;
import com.cerner.cura.medications.ui.MedsDTADetailsFragment;
import com.cerner.cura.medications.ui.MedsIncrementalScanningFragment;
import com.cerner.cura.medications.ui.MedsOrderDetailsFragment;
import com.cerner.cura.medications.ui.MedsSigningAlertFragment;
import com.cerner.cura.medications.ui.MedsActivityListFragment;
import com.cerner.cura.medications.ui.MedsActivitySelectionBaseFragment;
import com.cerner.cura.scanning.BarcodeAcceptanceTypeContext;
import com.cerner.cura.scanning.ScanCategory;
import com.cerner.cura.utils.CapabilitiesAndFeatures;
import com.cerner.cura.utils.SerializablePair;
import com.cerner.cura.vitals.ui.ItemDetailsResultFragment;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Medication Administration module.
 *
 * @author Brad Barnhill (bb024928)
 */
public class MedsAdminActivity extends ContentActivity {
    @SuppressWarnings ("unchecked")
    private static final HashSet<Class<? extends CuraAuthnFragment>> ALERT_NAVIGATION_FRAGMENT_LIST = new HashSet<>(
            Arrays.asList(MedsChartingDetailsFragment.class, MedsOrderDetailsFragment.class, CodeListFragment.class, MedIntervalDetailFragment.class, ItemDetailsOrdersFragment.class,
                          com.cerner.cura.medications.legacy.ui.MedsChartingDetailsFragment.class, com.cerner.cura.medications.legacy.ui.MedsOrderDetailsFragment.class,
                          com.cerner.cura.medications.legacy.ui.CodeListFragment.class, com.cerner.cura.medications.legacy.ui.MedIntervalDetailFragment.class));

    @SuppressWarnings ("unchecked")
    private static final HashSet<Class<? extends CuraAuthnFragment>> SIGNING_ALERT_NAVIGATION_FRAGMENT_LIST = new HashSet<>(
            Arrays.asList(MedsChartingDetailsFragment.class, CodeListFragment.class, MedsIncrementalScanningFragment.class,
                          com.cerner.cura.medications.legacy.ui.MedsChartingDetailsFragment.class, com.cerner.cura.medications.legacy.ui.CodeListFragment.class));

    @SuppressWarnings ("unchecked")
    private static final HashSet<Class<? extends CuraAuthnFragment>> CHARTING_NAVIGATION_FRAGMENT_LIST = new HashSet<>(
            Arrays.asList(LastGivenDetailsFragment.class, CommentsFragment.class, MedsDTADetailsFragment.class, FreetextEntryFragment.class, ItemDetailsResultFragment.class,
                          com.cerner.cura.medications.legacy.ui.LastGivenDetailsFragment.class, com.cerner.cura.medications.legacy.ui.CommentsFragment.class,
                          com.cerner.cura.medications.legacy.ui.MedsDTADetailsFragment.class, com.cerner.cura.medications.legacy.ui.FreetextEntryFragment.class));

    @SuppressWarnings ("unchecked")
    private static final HashSet<Class<? extends CuraAuthnFragment>> MEDS_TASK_SELECTION_FRAGMENT_LIST = new HashSet<>(
            Arrays.asList(MedsChartingDetailsFragment.class, CodeListFragment.class,
                          com.cerner.cura.medications.legacy.ui.MedsChartingDetailsFragment.class, com.cerner.cura.medications.legacy.ui.CodeListFragment.class));

    public MedsAdminActivity() {
        super(getActivityListClassByForFeatureFlag(), null);
        TAG = MedsAdminActivity.class.getSimpleName();
        mActiveScanCategory = ScanCategory.SCAN_CATEGORY_MEDICATION;
    }

    @SuppressWarnings ("SuspiciousMethodCalls")
    @Override
    public Class<? extends Fragment> getPreviousFragment() {
        final boolean backFromAlertDetail = ALERT_NAVIGATION_FRAGMENT_LIST.contains(mCurrentFragment);
        final boolean backFromChartingDetail = CHARTING_NAVIGATION_FRAGMENT_LIST.contains(mCurrentFragment);
        final boolean backFromSigningAlertDetail = SIGNING_ALERT_NAVIGATION_FRAGMENT_LIST.contains(mCurrentFragment);
        final boolean backFromTaskSelection = MEDS_TASK_SELECTION_FRAGMENT_LIST.contains(mCurrentFragment);

        SerializablePair<Class<? extends Fragment>, Boolean> backStackItem;
        for (int i = mFragmentBackStack.size() - 1; i >= 0; --i) {
            backStackItem = mFragmentBackStack.get(i);
            if (!backStackItem.second
                || (backFromAlertDetail && (MedsAlertBaseFragment.class.isAssignableFrom(backStackItem.first)
                                            || com.cerner.cura.medications.legacy.ui.MedsAlertBaseFragment.class.isAssignableFrom(backStackItem.first)))
                || (backFromChartingDetail && (MedsChartingDetailsFragment.class.isAssignableFrom(backStackItem.first)
                                               || com.cerner.cura.medications.legacy.ui.MedsChartingDetailsFragment.class.isAssignableFrom(backStackItem.first)))
                || (backFromSigningAlertDetail && (MedsSigningAlertFragment.class.isAssignableFrom(backStackItem.first)
                                                   || com.cerner.cura.medications.legacy.ui.MedsSigningAlertFragment.class.isAssignableFrom(backStackItem.first)))
                || (backFromTaskSelection && (MedsActivitySelectionBaseFragment.class.isAssignableFrom(backStackItem.first)
                                              || com.cerner.cura.medications.legacy.ui.MedsTaskSelectionBaseFragment.class.isAssignableFrom(backStackItem.first)))) {
                //If not incognito (or if coming back from a detail fragment) then return it
                return backStackItem.first;
            }
        }

        return null;
    }

    @Override
    protected void init() {
        super.init();

        if (mViewHolder != null) {
            setToolbarContainer(mViewHolder.mBottomToolbarContainer);
            setFloatingInfoButton(mViewHolder.mFloatingInfoButton);
        }
    }

    @Override
    public void onScanReset() {
        // default setting for Med
        BarcodeAcceptanceTypeContext.enableScanningProcessors(ScanCategory.SCAN_CATEGORY_NOT_ALLOWED);
    }

    private static Class<? extends Fragment> getActivityListClassByForFeatureFlag() {
        if (CapabilitiesAndFeatures.isMicroservicesFeatureFlagEnabled()) {
            return MedsActivityListFragment.class;
        }

        return com.cerner.cura.medications.legacy.ui.MedsTaskListFragment.class;
    }
}
