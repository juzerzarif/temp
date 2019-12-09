package com.cerner.nursing.nursing.ui;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.cerner.cura.base.CuraAuthnActivity;
import com.cerner.cura.base.ICuraFragment;
import com.cerner.cura.scanning.BarcodeAcceptanceTypeContext;
import com.cerner.cura.scanning.IScanView;
import com.cerner.cura.scanning.ScanCategory;
import com.cerner.cura.scanning.ScanManager;
import com.cerner.cura.utils.ActivityUtils;
import com.cerner.nursing.nursing.R;

/**
 * Shows the settings of the logged in individual
 *
 * @author Mark Lear (ML015922)
 */
public class SettingsActivity extends CuraAuthnActivity implements ICuraFragment.OnFragmentSelected, OrgSelectionFragment.OnOrgSelectedListener, IScanView, ScanManager.ScanResetListener {
    public static final int LAUNCH_DEFAULT = 0;
    public static final int LAUNCH_PICK_ORG = 1;

    public static final String LAUNCH_ACTION_IDENTIFIER = "SettingsActivity_Launch_Action";

    private int mLoadAction;

    @Override
    public void onOrgSelected() {
        if (mLoadAction == LAUNCH_PICK_ORG) {
            setResultAndFinish(Activity.RESULT_OK);
        } else {
            onFragmentSelected(UserSummaryFragment.class, null);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onFragmentSelected(final Class<? extends Fragment> fragmentClass, final Bundle oArg, final boolean incognitoFragment, final boolean forceNewFragment) {
        super.onFragmentSelected(fragmentClass, oArg, incognitoFragment, forceNewFragment);
        navigateToFragment(fragmentClass, oArg, R.id.fragment_container, incognitoFragment, forceNewFragment);
    }

    @Override
    public void onAuthnResume() {
        ScanManager.setupScanning(this, this, ScanCategory.SCAN_CATEGORY_NOT_ALLOWED, ScanCategory.SCAN_CATEGORY_NOT_ALLOWED);
        super.onAuthnResume();
    }

    @Override
    protected void onPause() {
        ScanManager.setUnAuth(this);
        super.onPause();
    }

    @Override
    protected void onStart() {
        onScanReset();
        super.onStart();
    }

    @Override
    public void onAuthnStart() {
        super.onAuthnStart();
        final ViewGroup viewGroup = findViewById(R.id.fragment_container);
        if (viewGroup != null && viewGroup.getChildCount() == 0) {
            if (mLoadAction == LAUNCH_PICK_ORG) {
                // Create an instance of OrgSelectionFragment
                final Bundle bundle = new Bundle();
                bundle.putInt(LAUNCH_ACTION_IDENTIFIER, mLoadAction);
                onFragmentSelected(OrgSelectionFragment.class, bundle);
            } else {
                onFragmentSelected(UserSummaryFragment.class, null);
            }
        }
    }

    @Override
    protected void onStop() {
        ScanManager.cleanupScanning(this);
        super.onStop();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init(savedInstanceState);
    }

    private void init(final Bundle savedInstanceState) {
        setContentView(R.layout.settings_activity);

        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout, but if we're being restored from a previous state,
        // then we don't need to do anything and should skip this or else
        // we could end up with overlapping fragments.
        if (savedInstanceState == null && findViewById(R.id.fragment_container) != null) {
            mLoadAction = getIntent().getIntExtra(LAUNCH_ACTION_IDENTIFIER, LAUNCH_DEFAULT);
        }

        BarcodeAcceptanceTypeContext.removeSavedVariables();
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        //this will throw a null exception which is better to fail fast if there is no action bar
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean canAllFragmentsShow() {
        return true;
    }

    @Override
    public void onBackPressed() {
        if (mLoadAction == LAUNCH_PICK_ORG) {
            ActivityUtils.logout(this);
            setResultAndFinish(ActivityUtils.RESULT_LOGOUT);
            return;
        }

        if (notifyFragmentsOnBackPressed()) {
            return; //One of the fragments handled the back press
        }

        final Class<? extends Fragment> previousFragment = getPreviousFragment();
        if (previousFragment != null) {
            onFragmentSelected(previousFragment, null, false, false);
            return;
        }

        mFragmentBackStack.clear(); //Clear since it could have incognito items
        setResultAndFinish(RESULT_OK);
    }

    @Override
    public void onResponseReceived(final Class clazz) {
    }

    @Override
    public Class getMainViewClass() {
        return mCurrentFragment;
    }

    @Override
    public void onLogout() {
        setResultAndFinish(ActivityUtils.RESULT_LOGOUT);
    }

    @Override
    public void onScanReset() {
        BarcodeAcceptanceTypeContext.enableScanningProcessors(ScanCategory.SCAN_CATEGORY_NOT_ALLOWED);
    }
}
