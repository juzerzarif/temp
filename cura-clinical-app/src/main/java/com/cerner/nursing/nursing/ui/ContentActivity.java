package com.cerner.nursing.nursing.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.cerner.cura.base.CuraAuthnActivity;
import com.cerner.cura.base.CuraAuthnFragment;
import com.cerner.cura.base.OnDrawerListener;
import com.cerner.cura.base.PatientContext;
import com.cerner.cura.demographics.ui.DemographicsFragment;
import com.cerner.cura.ppr.ui.PPRFragment;
import com.cerner.cura.ppr.utils.PPRUtils;
import com.cerner.cura.scanning.BarcodeAcceptanceTypeContext;
import com.cerner.cura.scanning.IScanView;
import com.cerner.cura.scanning.IScannedBarcode;
import com.cerner.cura.scanning.ScanCategory;
import com.cerner.cura.scanning.ScanManager;
import com.cerner.cura.scanning.ScanProcessor;
import com.cerner.cura.scanning.datamodel.ScanViewResponse;
import com.cerner.cura.utils.ActiveModuleManager;
import com.cerner.cura.utils.ActivityUtils;
import com.cerner.ion.log.Logger;
import com.cerner.ion.security.IonActivity;
import com.cerner.nursing.nursing.R;

import java.io.Serializable;
import java.util.UUID;

/**
 * Basic structure of a content activity.
 *
 * @author Mark Lear (ML015922)
 */
public abstract class ContentActivity extends CuraAuthnActivity implements OnDrawerListener, IScanView, IScannedBarcode, PPRFragment.IPPRListener, ScanManager.ScanResetListener {
    protected transient ViewHolder mViewHolder;
    private transient ActionBarDrawerToggle mDrawerToggle;
    private UUID mPatientInstanceId;
    private ScanViewResponse mScanViewResponse;
    private final Class<? extends Fragment> mEntryFragment;
    private final Bundle mEntryFragmentBundle;
    protected @ScanCategory.SingleScanCategory int mActiveScanCategory = ScanCategory.SCAN_CATEGORY_NOT_ALLOWED;

    public ContentActivity(final Class<? extends Fragment> entryFragment, final Bundle entryFragmentBundle) {
        mEntryFragment = entryFragment;
        mEntryFragmentBundle = entryFragmentBundle;
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
        final View view = findViewById(R.id.content_pane_fragment_container);
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
    public void onLogout() {
        setResultAndFinish(ActivityUtils.RESULT_LOGOUT);
    }

    @Override
    public void setDrawerLockMode(final int lockMode) {
        if (mViewHolder != null) {
            mViewHolder.mDrawerLayout.setDrawerLockMode(lockMode);
        }
    }

    @Override
    public void onFragmentSelected(final Class<? extends Fragment> fragmentClass, final Bundle oArg, final boolean incognitoFragment, final boolean forceNewFragment) {
        if (mPatientInstanceId != null && mPatientInstanceId.compareTo(PatientContext.getInstanceId()) != 0) {
            recreateFromPatientChange();
            return;
        }

        super.onFragmentSelected(fragmentClass, oArg, incognitoFragment, forceNewFragment);
        navigateToFragment(fragmentClass, oArg, R.id.content_pane_fragment_container, incognitoFragment, forceNewFragment);
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mPatientInstanceId = UUID.fromString(savedInstanceState.getString(PatientContext.PATIENT_INSTANCE_ID_STORAGEID));
        }

        init();
    }

    protected void init() {
        if (!PatientContext.isPatientSelected()) {
            Log.d(TAG, "Patient not in context when initializing activity.");
            setResultAndFinish(ActivityUtils.RESULT_PATIENT_NOT_IN_CONTEXT);
            return;
        }

        verifyIntentData(getIntent());
        setContentView(R.layout.activity_with_context);

        //Setup demog banner
        final DemographicsFragment demogFrag = (DemographicsFragment) getFragmentManager().findFragmentById(R.id.PatientDemographicsFragment);
        if (demogFrag == null) {
            Log.d(TAG, "Can not find view to replace with the demographics bar.");
            setResultAndFinish(ActivityUtils.RESULT_NOT_ENOUGH_IDENTIFIERS);
            return;
        }

        demogFrag.setDetailFrameLayoutId(R.id.content_pane_fragment_container);
        demogFrag.addResponseReceivedListener(this);

        mViewHolder = new ViewHolder(this);
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

                if (ActiveModuleManager.getActiveModule() != ContentActivity.this.getClass()) {
                    ActiveModuleManager.gotoActiveModule(ContentActivity.this);
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
        mScanViewResponse = null;
        getIntent().putExtra(ScanProcessor.INTENT_SCAN_EXTRA, (Serializable) null);

        super.onDestroy();
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        verifyIntentData(intent);
        super.onNewIntent(intent);
    }

    /**
     * Verify intent received
     *
     * @param intent intent to launch the activity
     */
    private void verifyIntentData(final Intent intent) {
        if (intent == null) {
            return;
        }

        final Object scanViewResponse = intent.getSerializableExtra(ScanProcessor.INTENT_SCAN_EXTRA);
        // need to clear the scan variable in the bundle because if the activity get destroyed and restore by android,
        // they will resend the same intent which contain the same bundle.
        intent.putExtra(ScanProcessor.INTENT_SCAN_EXTRA, (Serializable) null);

        if (scanViewResponse instanceof ScanViewResponse) {
            mScanViewResponse = (ScanViewResponse) scanViewResponse;
        }
    }

    @Override
    public void onAuthnResume() {
        final View view = findViewById(R.id.content_pane_fragment_container);
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }

        ScanManager.setupScanning(this, this, mActiveScanCategory, ScanCategory.MAX_ALLOWED_SCAN_CATEGORIES);
        BarcodeAcceptanceTypeContext.removeSavedVariables();
        super.onAuthnResume();
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        outState.putString(PatientContext.PATIENT_INSTANCE_ID_STORAGEID, mPatientInstanceId.toString());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        PPRUtils.unregisterRelationshipHandler(ContentActivity.this);
        ScanManager.setUnAuth(this);
        super.onPause();
    }

    @Override
    protected void onStart() {
        if (!PatientContext.isPatientSelected()) {
            setResultAndFinish(ActivityUtils.RESULT_PATIENT_NOT_IN_CONTEXT);
        }

        if (mPatientInstanceId != null && mPatientInstanceId.compareTo(PatientContext.getInstanceId()) != 0) {
            recreateFromPatientChange();
        }

        onScanReset();
        super.onStart();
    }

    @Override
    protected void onStop() {
        ScanManager.cleanupScanning(this);
        super.onStop();
    }

    @Override
    protected void onResume() {
        if (!PatientContext.isPatientSelected()) {
            setResultAndFinish(ActivityUtils.RESULT_PATIENT_NOT_IN_CONTEXT);
            super.onResume();
            return;
        }

        if (mPatientInstanceId != null && mPatientInstanceId.compareTo(PatientContext.getInstanceId()) != 0) {
            recreateFromPatientChange();
            super.onResume();
            return;
        }

        PPRUtils.registerRelationshipHandler(this, this, this);
        if (PatientContext.hasRelationship() && PatientContext.hasAccess()) {
            if (mPatientInstanceId == null || mPatientInstanceId.compareTo(PatientContext.getInstanceId()) != 0) {
                mFragmentBackStack.clear();
                mCurrentFragment = null;

                if (mPatientInstanceId == null) {
                    mPatientInstanceId = PatientContext.getInstanceId();
                }

                if (PatientContext.hasContextId()) {
                    onFragmentSelected(mEntryFragment, mEntryFragmentBundle);
                } else {
                    PatientContext.setHasAccess(true);
                    finish();
                }

            } else {
                final ViewGroup viewGroup = findViewById(R.id.content_pane_fragment_container);
                if (viewGroup != null && viewGroup.getChildCount() == 0) {
                    onFragmentSelected(mEntryFragment, mEntryFragmentBundle);
                }
            }
        } else {
            mFragmentBackStack.clear();
            mCurrentFragment = null;

            if (mPatientInstanceId == null) {
                mPatientInstanceId = PatientContext.getInstanceId();
            }

            if (mPatientInstanceId.compareTo(PatientContext.getInstanceId()) == 0) {
                onFragmentSelected(PPRFragment.class, null, true, true);
            }
        }

        super.onResume();
    }

    @Override
    public Class getMainViewClass() {
        return mCurrentFragment != null ? mCurrentFragment : PPRFragment.class;
    }

    @Override
    public void setDemographicsLockMode(final boolean lockMode) {
        final Fragment fragment = getFragmentManager().findFragmentById(R.id.PatientDemographicsFragment);
        if (fragment instanceof DemographicsFragment) {
            ((DemographicsFragment) fragment).setLocked(lockMode);
        }
    }

    @Override
    public void onRelationshipEstablished() {
        onFragmentSelected(mEntryFragment, mEntryFragmentBundle);
    }

    @Override
    public ScanViewResponse getScannedBarcode() {
        return mScanViewResponse;
    }

    @Override
    public void clearScannedBarcode() {
        mScanViewResponse = null;
    }

    @Override
    public boolean isPatientContextModule() {
        return true;
    }

    @Override
    public void onResponseReceived(final Class clazz) {
        if (clazz == DemographicsFragment.class) {
            mCanShowFragments = true;

            final DemographicsFragment demogFrag = (DemographicsFragment) getFragmentManager().findFragmentById(R.id.PatientDemographicsFragment);
            if (demogFrag != null) {
                demogFrag.setFragmentVisibility(true);

                final Fragment fragment = getFragmentManager().findFragmentById(R.id.content_pane_fragment_container);
                if (fragment instanceof CuraAuthnFragment) {
                    ((CuraAuthnFragment) fragment).setFragmentVisibility(true);
                }
            }
        }
    }

    private void recreateFromPatientChange() {
        mScanViewResponse = null;
        mPatientInstanceId = PatientContext.getInstanceId();
        Logger.d(TAG, "Activity being recreated due to patient instance id changing");
        recreate();
    }

    /**
     * View holder for all the views on the screen.
     */
    static class ViewHolder {
        final DrawerLayout mDrawerLayout;
        final LinearLayout mDrawerList;
        final FrameLayout mBottomToolbarContainer;
        final Button mFloatingInfoButton;

        public ViewHolder(final Activity root) throws IllegalArgumentException, NullPointerException {
            if (root == null) {
                throw new IllegalArgumentException("root may not be null");
            }

            mDrawerLayout = root.findViewById(R.id.drawer_layout);
            if (mDrawerLayout == null) {
                throw new NullPointerException("ViewHolder failed to find all views");
            }

            mDrawerList = mDrawerLayout.findViewById(R.id.drawer_fragment);
            mBottomToolbarContainer = mDrawerLayout.findViewById(R.id.bottom_action_toolbar);
            mFloatingInfoButton = mDrawerLayout.findViewById(R.id.floating_info_button);

            if (mDrawerList == null || mBottomToolbarContainer == null || mFloatingInfoButton == null) {
                throw new NullPointerException("ViewHolder failed to find all views");
            }
        }
    }
}
