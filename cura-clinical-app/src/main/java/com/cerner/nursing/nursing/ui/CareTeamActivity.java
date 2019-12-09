package com.cerner.nursing.nursing.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.android.volley.VolleyError;
import com.cerner.careaware.connect.contacts.fragments.ContactsListFragment;
import com.cerner.careaware.connect.contacts.fragments.ReadContactDetailFragment;
import com.cerner.careaware.connect.contacts.model.ContactsCollection;
import com.cerner.careaware.connect.contacts.utilities.ContactGson;
import com.cerner.cura.base.CuraAuthnActivity;
import com.cerner.cura.base.CuraAuthnFragment;
import com.cerner.cura.base.OnDrawerListener;
import com.cerner.cura.base.PatientContext;
import com.cerner.cura.datamodel.CareTeamList;
import com.cerner.cura.demographics.ui.DemographicsFragment;
import com.cerner.cura.requestor.CuraResponseListener;
import com.cerner.cura.requestor.IDataRetriever;
import com.cerner.cura.requestor.JsonRequestor;
import com.cerner.cura.requestor.SaveResponseDataRetriever;
import com.cerner.cura.scanning.BarcodeAcceptanceTypeContext;
import com.cerner.cura.scanning.IScanView;
import com.cerner.cura.scanning.ScanCategory;
import com.cerner.cura.scanning.ScanManager;
import com.cerner.cura.utils.ActiveModuleManager;
import com.cerner.cura.utils.ActivityUtils;
import com.cerner.cura.utils.AppUtils;
import com.cerner.cura.utils.RequestorUtils;
import com.cerner.ion.log.Logger;
import com.cerner.ion.request.CernRequest;
import com.cerner.ion.request.CernResponse;
import com.cerner.nursing.nursing.R;
import com.cerner.nursing.nursing.base.CareTeamImageLoader;

import java.util.UUID;

/**
 * Shows the device association screen for the selected patient.
 *
 * @author Brad Barnhill (bb024928)
 */
public class CareTeamActivity extends CuraAuthnActivity
        implements OnDrawerListener, IScanView, IDataRetriever, ContactsListFragment.RefreshFragmentListener, ContactsListFragment.ContactItemClickListener, ScanManager.ScanResetListener {
    public static final int RESULT_PATIENT_NOT_IN_CONTEXT = RESULT_FIRST_USER + 2;
    private static final String CHECKPOINT_NAME = "CURA_NURSING_CARETEAM_READ";
    private transient ActionBarDrawerToggle mDrawerToggle;
    private transient ViewHolder mViewHolder;
    private final SaveResponseDataRetriever mResponseProcessor = new SaveResponseDataRetriever(this, this);
    private UUID mPatientInstanceId;
    private ContactsListFragment mContactsListFragment;
    private ReadContactDetailFragment mReadContactDetailFragment;
    private boolean mDataRetrieved;
    private CareTeamImageLoader mImageLoader;

    public CareTeamActivity() {
        TAG = CareTeamActivity.class.getSimpleName();
    }

    @Override
    public void onContactClicked(final Class<? extends Fragment> targetFragment, final Bundle bundle) {
        if (targetFragment != null && bundle != null) {
            getIntent().putExtras(bundle);
            onFragmentSelected(targetFragment, bundle);
        } else {
            Logger.d(TAG, "targetFragment and bundle both must not be null.");
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
        final View view = findViewById(R.id.careteam_fragment_container);
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
        super.onFragmentSelected(fragmentClass, oArg, incognitoFragment, forceNewFragment);
        if (fragmentClass == ContactsListFragment.class) {
            /* can only call to this from inside onAuthnResume or the app will crash due to framework expecting the request to come from
             * an authenticated entity */
            mDataRetrieved = false;
            setMainFragmentVisibility(false);   //Set the main fragment as gone since we could have a failed transaction

            mContactsListFragment = (ContactsListFragment) navigateToFragment(fragmentClass, oArg, R.id.careteam_fragment_container, incognitoFragment, forceNewFragment);
            getData(DataArgs.NONE);
        } else {
            final Fragment fragment = navigateToFragment(fragmentClass, oArg, R.id.careteam_fragment_container, incognitoFragment, forceNewFragment);
            if (fragment instanceof ReadContactDetailFragment) {
                mReadContactDetailFragment = (ReadContactDetailFragment) fragment;
                getData(DataArgs.NONE);
            }
        }

        setupActionBar();
    }

    @Override
    public void getData(final DataArgs dataArgs) {
        Logger.i(TAG, "GetData");
        if (mImageLoader == null) {
            mImageLoader = new CareTeamImageLoader(this);
        }

        if (mReadContactDetailFragment != null && mCurrentFragment == ReadContactDetailFragment.class) {
            mReadContactDetailFragment.setImageLoader(mImageLoader);
            return;
        }

        if (mContactsListFragment != null && mCurrentFragment == ContactsListFragment.class && dataArgs != DataArgs.PULL_TO_REFRESH) {
            mContactsListFragment.setImageLoader(mImageLoader);
            //Note: this will turn off the fragment specific, non-locking, wait cursor so that we can show the framework, locking, wait cursor
            mContactsListFragment.addOrUpdateData(CareTeamActivity.this, new ContactsCollection[0]);
        }

        if (PatientContext.isPatientSelected()) {
            final CuraResponseListener<CareTeamList> responseListener = new CuraResponseListener<>(
                    dataArgs == DataArgs.PULL_TO_REFRESH ? null : RequestorUtils.showProgressDialog(this, mResponseProcessor),
                    TAG, CHECKPOINT_NAME, CareTeamList.class, mResponseProcessor, this, true);
            JsonRequestor.sendRequest(ContactGson.get(), responseListener, this, 0, false, null, null, PatientContext.getPatientId(), PatientContext.getEncounterId());
        }
    }

    @Override
    public void onResponse(@NonNull final CernResponse response) {
        Logger.d(TAG, "CareTeamActivity.onResponse");

        mDataRetrieved = true;

        if (response.data instanceof CareTeamList && mContactsListFragment != null && mCurrentFragment == ContactsListFragment.class) {
            final CareTeamList careTeamList = (CareTeamList) response.data;
            Logger.d(TAG, "CareTeam returned successfully");
            AppUtils.logCheckPoint(this, CHECKPOINT_NAME, AppUtils.CHECKPOINT_EVENT_RESPONSE_WITH_CONTENT);
            try {
                final ContactsCollection[] contactsCollections = new ContactsCollection[careTeamList.primary_section_list.size()];
                careTeamList.primary_section_list.toArray(contactsCollections);
                mContactsListFragment.addOrUpdateData(CareTeamActivity.this, contactsCollections);
            } catch (final Exception ex) {
                //***** catch all exceptions due to instability concerns with the care team fragment *****
                Logger.e(TAG, "CARETEAM_FRAGMENT", ex);
            }
        }

        setMainFragmentVisibility(true);
    }

    @Override
    public void onNoContentResponse(@NonNull final CernResponse response, final Class clazz) {
        Logger.d(TAG, "CareTeamActivity.onNoContentResponse");

        mDataRetrieved = true;

        if (mContactsListFragment != null && mCurrentFragment == ContactsListFragment.class) {
            mContactsListFragment.addOrUpdateData(CareTeamActivity.this, new ContactsCollection[0]);
        }

        AppUtils.logCheckPoint(this, CHECKPOINT_NAME, AppUtils.CHECKPOINT_EVENT_RESPONSE_WITH_NO_CONTENT);

        setMainFragmentVisibility(true);
    }

    @Override
    public void onErrorResponse(@NonNull final VolleyError volleyError, final Class clazz) {
        Logger.e(TAG, volleyError.getMessage());
        onBackPressed();
    }

    @Override
    public void onFailedResponse(final Class clazz, final boolean cacheReturned) {
        //Do nothing upon failure so that the fragment is only made visible if a cached copy was sent back
    }

    @Override
    public void setActionBarWaitCursor(final boolean b) {
    }

    @Override
    public boolean processResponses() {
        return mProcessResponses;
    }

    @Override
    public boolean cancelRequest(@NonNull final CernRequest request) {
        return false;
    }

    @Override
    public boolean backgroundAfterCancel(@NonNull final CernRequest request) {
        return true;
    }

    @Override
    public void setRequestMethod(final int requestMethod, final String tag) {
        //Do nothing since this doesn't care about the method
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final boolean returnValue = super.onCreateOptionsMenu(menu);
        setupActionBar();
        return returnValue;
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

    private void init() {
        if (!PatientContext.isPatientSelected()) {
            Log.d(TAG, "Patient not in context when initializing activity.");
            setResultAndFinish(RESULT_PATIENT_NOT_IN_CONTEXT);
            return;
        }

        setContentView(R.layout.care_team_activity);

        //Setup demog banner
        final DemographicsFragment demogFrag = (DemographicsFragment) getFragmentManager().findFragmentById(R.id.PatientDemographicsFragment);
        if (demogFrag == null) {
            Log.d(TAG, "Can not find view to replace with the demographics bar.");
            setResultAndFinish(ActivityUtils.RESULT_NOT_ENOUGH_IDENTIFIERS);
            return;
        }

        demogFrag.setDetailFrameLayoutId(R.id.careteam_fragment_container);
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

                if (ActiveModuleManager.getActiveModule() != CareTeamActivity.class) {
                    ActiveModuleManager.gotoActiveModule(CareTeamActivity.this);
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
        if (!PatientContext.isPatientSelected()) {
            return;
        }

        mDataRetrieved = false;
        setMainFragmentVisibility(false);

        ScanManager.setupScanning(this, this, ScanCategory.SCAN_CATEGORY_NOT_ALLOWED, ScanCategory.MAX_ALLOWED_SCAN_CATEGORIES);
        BarcodeAcceptanceTypeContext.removeSavedVariables();

        if (PatientContext.hasRelationship() && PatientContext.hasAccess()) {
            if (mPatientInstanceId == null || mPatientInstanceId.compareTo(PatientContext.getInstanceId()) != 0) {
                mFragmentBackStack.clear();
                mCurrentFragment = null;

                if (mPatientInstanceId == null) {
                    mPatientInstanceId = PatientContext.getInstanceId();
                }

                onFragmentSelected(ContactsListFragment.class, null);
            } else {
                final ViewGroup viewGroup = findViewById(R.id.careteam_fragment_container);
                if (viewGroup != null && viewGroup.getChildCount() == 0 || mCurrentFragment == ContactsListFragment.class) {
                    mCurrentFragment = null;
                    onFragmentSelected(ContactsListFragment.class, null);
                } else {
                    //If we are in detail fragment just make it visible
                    mDataRetrieved = true;
                    setMainFragmentVisibility(true);
                }
            }
        } else {
            Logger.e(TAG, "Not allowed to access CareTeam without having a relationship with a patient.  How did you get here without a relationship?");
            setResultAndFinish(RESULT_PATIENT_NOT_IN_CONTEXT);
        }

        super.onAuthnResume();
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        outState.putString(PatientContext.PATIENT_INSTANCE_ID_STORAGEID, mPatientInstanceId.toString());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        if (!PatientContext.isPatientSelected()) {
            setResultAndFinish(RESULT_PATIENT_NOT_IN_CONTEXT);
            super.onResume();
            return;
        }

        if (mPatientInstanceId != null && mPatientInstanceId.compareTo(PatientContext.getInstanceId()) != 0) {
            mPatientInstanceId = PatientContext.getInstanceId();
            Logger.d(TAG, "Activity being recreated due to patient instance id changing");
            recreate();
        }

        AppUtils.logCheckPoint(this, CHECKPOINT_NAME, AppUtils.CHECKPOINT_EVENT_LOAD);

        /* do not call any onFragmentSelected from in here
           or the app will crash due to framework expecting the
           request to come from an authenticated entity */

        super.onResume();
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
    protected void onStop() {
        ScanManager.cleanupScanning(this);
        super.onStop();

        mDataRetrieved = false;
        setMainFragmentVisibility(false);
    }

    @Override
    public Class getMainViewClass() {
        return mCurrentFragment;
    }

    @Override
    public void onScanReset() {
        BarcodeAcceptanceTypeContext.enableScanningProcessors(ScanCategory.MAX_ALLOWED_SCAN_CATEGORIES);
    }

    @Override
    public void onRefreshRequested(final ContactsListFragment fragment, final boolean triggeredFromPull) {
        getData(DataArgs.PULL_TO_REFRESH);
    }

    @Override
    public void onListItemClick(final Bundle contactDetailsBundle, final ListView listView, final View view, final int position, final long id) {
        if (mCurrentFragment == ContactsListFragment.class) {
            mContactsListFragment.setRefreshing(false);
        }
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

            mDrawerLayout = root.findViewById(R.id.drawer_layout);
            if (mDrawerLayout == null) {
                throw new NullPointerException("ViewHolder failed to find all views");
            }

            mDrawerList = mDrawerLayout.findViewById(R.id.drawer_fragment);
            if (mDrawerList == null) {
                throw new NullPointerException("ViewHolder failed to find all views");
            }
        }
    }

    @Override
    public void onResponseReceived(final Class clazz) {
        if (clazz != DemographicsFragment.class) {
            return;
        }

        mCanShowFragments = true;

        final DemographicsFragment demogFrag = (DemographicsFragment) getFragmentManager().findFragmentById(R.id.PatientDemographicsFragment);
        if (demogFrag != null) {
            demogFrag.setFragmentVisibility(true);

            final Fragment fragment = getFragmentManager().findFragmentById(R.id.careteam_fragment_container);
            if (fragment instanceof CuraAuthnFragment) {
                ((CuraAuthnFragment) fragment).setFragmentVisibility(true);
            } else {
                setMainFragmentVisibility(true);
            }
        }
    }

    /**
     * Sets the main fragment's visibility if the data has been retrieved for that fragment and if all required fragments are able to show.
     *
     * @param visible boolean representing whether to show or hide the fragment if able.
     */
    public void setMainFragmentVisibility(final boolean visible) {
        final View view = findViewById(R.id.careteam_fragment_container);
        if (view != null) {
            view.setVisibility(visible && mDataRetrieved && canAllFragmentsShow() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean isPatientContextModule() {
        return true;
    }

    private void setupActionBar() {
        setDrawerIndicatorEnabled(mCurrentFragment == ContactsListFragment.class);

        //this will throw a null exception which is better to fail fast if there is no action bar
        final ActionBar bar = getSupportActionBar();
        //noinspection ConstantConditions
        bar.setTitle(R.string.careteam_title);
        bar.setDisplayShowHomeEnabled(true);
        bar.setHomeButtonEnabled(true);
    }
}
