package com.cerner.nursing.nursing.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.cerner.cura.base.CuraAuthnActivity;
import com.cerner.cura.base.OnDrawerListener;
import com.cerner.cura.base.PatientContext;
import com.cerner.cura.datamodel.common.PatientListPatient;
import com.cerner.cura.ppr.utils.PPRUtils;
import com.cerner.cura.requestor.MockDataManager;
import com.cerner.cura.scanning.BarcodeAcceptanceTypeContext;
import com.cerner.cura.scanning.IScanView;
import com.cerner.cura.scanning.ScanCategory;
import com.cerner.cura.scanning.ScanManager;
import com.cerner.cura.utils.ActiveModuleManager;
import com.cerner.cura.utils.ActivityUtils;
import com.cerner.nursing.nursing.R;
import com.cerner.nursing.nursing.utils.AppSwitchUtils;

/**
 * Shows the patient list of the logged in individual
 *
 * @author Mark Lear (ML015922)
 * @author Brad Barnhill (bb024928)
 * @author Lam Tran (lt028506)
 */
public class PatientListActivity extends CuraAuthnActivity implements PatientListFragment.OnPatientListItemSelectedListener, OnDrawerListener, IScanView, ScanManager.ScanResetListener {
    public static final int REQUESTCODE_PATIENTCHART_DEFAULT = 0;
    public static final int REQUESTCODE_DEVICEASSOCIATION_DEFAULT = 1;
    public static final int REQUESTCODE_CARETEAM_DEFAULT = 2;
    public static final int REQUESTCODE_PATIENTLIST_DEFAULT = 3;
    public static final int REQUESTCODE_MEDSADMIN_DEFAULT = 4;
    public static final int REQUESTCODE_SETTINGS_DEFAULT = 5;
    public static final int REQUESTCODE_SPECCOL_DEFAULT = 6;
    public static final int REQUESTCODE_CHARTING_DEFAULT = 7;
    private transient ActionBarDrawerToggle mDrawerToggle;
    private transient ViewHolder mViewHolder;

    public PatientListActivity() {
        TAG = PatientListActivity.class.getSimpleName();
    }

    @Override
    public void onPatientSelected(final PatientListPatient patient) {
        if (patient == null) {
            return;
        }

        if (PatientContext.isEncounterIdSet() && PatientContext.isPatientIdSet() && patient.relationshipInd
            && PatientContext.getEncounterId().equals(patient.encounterId) && PatientContext.getPatientId().equals(patient.personId) && PatientContext.hasContextId()) {

            PatientContext.setHasRelationship(true);
            PatientContext.setHasAccess(true);
            if (PatientContext.isLatestModuleSet() && PatientContext.getLatestModuleClass() != null) {
                ActiveModuleManager.gotoModule(this, PatientContext.getLatestModuleClass(), PatientContext.getLatestModuleRequestcode());
            } else {
                ActiveModuleManager.gotoModule(this, PatientChartActivity.class, PatientListActivity.REQUESTCODE_PATIENTCHART_DEFAULT);
            }
        } else {
            PPRUtils.loadPatientInformation(this, this, PatientChartActivity.class, PatientListActivity.REQUESTCODE_PATIENTCHART_DEFAULT, R.string.unable_to_open_patient_summary_title,
                                            R.string.unable_to_open_patient_summary, patient.nameFullFormatted, patient.personId, patient.encounterId, null, null);
        }
    }

    @Override
    public void setDrawerIndicatorEnabled(final boolean enable) {
        if (mDrawerToggle != null) {
            mDrawerToggle.setDrawerIndicatorEnabled(enable);
        }
    }

    @Override
    public void onDrawerItemSelected(final Object drawerSelectionItem) {
        closeDrawer();
    }

    @Override
    public void onSelectionSuccess() {
        final View view = findViewById(R.id.patient_list_fragment);
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean isDrawerOpen() {
        return mViewHolder != null && mViewHolder.mDrawerLayout.isDrawerOpen(mViewHolder.mDrawerList);
    }

    @Override
    public void closeDrawer() {
        if (mViewHolder != null) {
            mViewHolder.mDrawerLayout.closeDrawer((mViewHolder.mDrawerList));
        }
    }

    @Override
    public void setDrawerLockMode(final int lockMode) {
        if (mViewHolder != null) {
            mViewHolder.mDrawerLayout.setDrawerLockMode(lockMode);
        }
    }

    @Override
    public void onCuraActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        super.onCuraActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQUESTCODE_PATIENTCHART_DEFAULT
            || requestCode == REQUESTCODE_DEVICEASSOCIATION_DEFAULT
            || requestCode == REQUESTCODE_CARETEAM_DEFAULT
            || requestCode == REQUESTCODE_MEDSADMIN_DEFAULT
            || requestCode == REQUESTCODE_SPECCOL_DEFAULT
            || requestCode == REQUESTCODE_CHARTING_DEFAULT) {
            if (resultCode == ActivityUtils.RESULT_NOT_ENOUGH_IDENTIFIERS) {
                dialogs.showError(null, getString(R.string.patient_not_enough_identifiers_noname), getString(R.string.close), null, null, null, null, null);
            }

            //TODO bb024928: add message for RESULT_PATIENT_NOT_IN_CONTEXT to show no patient in context
        }
    }

    @Override
    public void onFragmentSelected(final Class<? extends Fragment> aClass, final Bundle bundle, final boolean incognitoFragment, final boolean forceNewFragment) {
        super.onFragmentSelected(aClass, bundle, incognitoFragment, forceNewFragment);
        throw new UnsupportedOperationException("Patient list should never navigate away from the patient list fragment");
    }

    @Override
    public void onBackPressed() {
        //this intentionally does not call super since we are on the patient list
        //and back is being blocked by this method being overridden
        if (isDrawerOpen()) {
            closeDrawer();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MockDataManager.getMockData(this, false, true);

        setContentView(R.layout.patient_list_activity);

        //only want to set the active module if we are creating the first time in order to set the request code
        if (savedInstanceState == null) {
            ActiveModuleManager.setActiveModule(PatientListActivity.class, REQUESTCODE_PATIENTLIST_DEFAULT);
        }

        try {
            mViewHolder = new ViewHolder(this);
        } catch (IllegalArgumentException | NullPointerException e) {
            mViewHolder = null;
        }
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        //this will throw a null exception which is better to fail fast if there is no action bar
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Set the drawer toggle as the DrawerListener
        mDrawerToggle = new ActionBarDrawerToggle(
                this, mViewHolder.mDrawerLayout,
                R.string.drawer_open,
                R.string.drawer_close) {
            @Override
            public void onDrawerClosed(final View drawerView) {
                super.onDrawerClosed(drawerView);

                if (ActiveModuleManager.getActiveModule() != PatientListActivity.class) {
                    ActiveModuleManager.gotoActiveModule(PatientListActivity.this);
                }

                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(final View drawerView) {
                invalidateOptionsMenu();
                super.onDrawerOpened(drawerView);
            }
        };

        mViewHolder.mDrawerLayout.removeDrawerListener(mDrawerToggle);
        mViewHolder.mDrawerLayout.addDrawerListener(mDrawerToggle);

        // Sync the toggle state after the DrawerLayout's instance state has been restored.
        mDrawerToggle.syncState();
    }

    @Override
    protected void onDestroy() {
        mViewHolder = null;
        mDrawerToggle = null;
        super.onDestroy();
    }

    @Override
    public void onAuthnResume() {
        //If there is no active module then become active, otherwise don't take precedence over any other
        setActiveModuleOnResume(ActiveModuleManager.getActiveModule() == null);
        super.onAuthnResume();

        //Once we have set the active module if needed, set back to default functionality so that this activity doesn't take precedence over any other
        setActiveModuleOnResume(false);

        if (AppSwitchUtils.performAppSwitch(this, this, null)) {
            //if appswitching then do nothing
            return;
        }

        if (ActiveModuleManager.getActiveModule() != PatientListActivity.class) {
            ActiveModuleManager.gotoActiveModule(this);
        }

        final View view = findViewById(R.id.patient_list_fragment);
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }

        ScanManager.setupScanning(this, this, ScanCategory.SCAN_CATEGORY_NOT_ALLOWED, ScanCategory.MAX_ALLOWED_SCAN_CATEGORIES);
        BarcodeAcceptanceTypeContext.removeSavedVariables();
    }

    @Override
    public Class getMainViewClass() {
        return PatientListFragment.class;
    }

    @Override
    protected void onStart() {
        final Fragment fragment = getFragmentManager().findFragmentById(R.id.patient_list_fragment);
        if (fragment != null) {
            if (fragment.getView() != null) {
                fragment.getView().setVisibility(View.VISIBLE);
            }

            if (fragment instanceof PatientListFragment) {
                ((PatientListFragment) fragment).setCanBeVisible(true);
            }
        }

        onScanReset();
        super.onStart();
    }

    @Override
    protected void onPause() {
        ScanManager.setUnAuth(this);
        super.onPause();
    }

    @Override
    protected void onStop() {
        ScanManager.cleanupScanning(this);
        super.onStop();
    }

    @Override
    public boolean canAllFragmentsShow() {
        return true;
    }

    @Override
    public void onResponseReceived(final Class clazz) {
    }

    @Override
    public void onScanReset() {
        BarcodeAcceptanceTypeContext.enableScanningProcessors(ScanCategory.MAX_ALLOWED_SCAN_CATEGORIES);
    }

    /**
     * View holder for all the views on the screen.
     */
    static class ViewHolder {
        final DrawerLayout mDrawerLayout;
        final LinearLayout mDrawerList;

        public ViewHolder(final Activity root) throws IllegalArgumentException, NullPointerException {
            if (root == null) {
                throw new IllegalArgumentException("root may not be null");
            }

            mDrawerLayout = root.findViewById(R.id.drawer_patientlist_layout);
            if (mDrawerLayout == null) {
                throw new NullPointerException("ViewHolder failed to find all views");
            }

            mDrawerList = mDrawerLayout.findViewById(R.id.drawer_fragment);
            if (mDrawerList == null) {
                throw new NullPointerException("ViewHolder failed to find all views");
            }
        }
    }
}