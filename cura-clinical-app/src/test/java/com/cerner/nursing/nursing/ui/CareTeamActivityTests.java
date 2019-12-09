package com.cerner.nursing.nursing.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.cerner.careaware.connect.contacts.fragments.ContactsListFragment;
import com.cerner.careaware.connect.contacts.fragments.ReadContactDetailFragment;
import com.cerner.cura.base.CuraAuthnFragment;
import com.cerner.cura.base.PatientContext;
import com.cerner.cura.datamodel.CareTeamList;
import com.cerner.cura.demographics.ui.DemographicsFragment;
import com.cerner.cura.requestor.IDataRetriever;
import com.cerner.cura.requestor.MockDataManager;
import com.cerner.cura.scanning.BarcodeAcceptanceTypeContext;
import com.cerner.cura.scanning.ScanCategory;
import com.cerner.cura.scanning.ScanManager;
import com.cerner.cura.scanning.ScanProcessor;
import com.cerner.cura.test.TestActivity;
import com.cerner.cura.test.helper.TestUtils;
import com.cerner.cura.ui.elements.drawer.DrawerFragment;
import com.cerner.cura.utils.ActiveModuleManager;
import com.cerner.cura.utils.ActivityUtils;
import com.cerner.cura.utils.SerializablePair;
import com.cerner.ion.request.CernResponse;
import com.cerner.ion.security.DialogController;
import com.cerner.nursing.nursing.R;
import com.cerner.nursing.nursing.base.CareTeamImageLoader;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;

import java.util.ArrayList;
import java.util.UUID;

import androidx.test.core.app.ApplicationProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Brad Barnhill (BB024928) on 6/24/2014.
 * @author Mark Lear (ML015922)
 */
@RunWith (RobolectricTestRunner.class)
public class CareTeamActivityTests {
    private ActivityController<CareTeamActivity> mActivityController;
    private CareTeamActivity mMockActivity;
    private DrawerLayout mLayoutView;
    private static final int mViewToReplace = R.id.careteam_fragment_container;

    @BeforeClass
    public static void ClassSetup() {
        System.gc();
    }

    /**
     * Create mock activity from robolectric
     */
    @Before
    public void setup() {
        TestUtils.setStaticVariable(MockDataManager.class, "smState", MockDataManager.MockState.READ);
        PatientContext.clearContext();
        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "test_encounter_id_" + System.currentTimeMillis());
        PatientContext.setHasRelationship(true);
        PatientContext.setHasAccess(true);
        Shadows.shadowOf(ApplicationProvider.getApplicationContext().getPackageManager()).addResolveInfoForIntent(new Intent("com.cerner.scanning.service.BIND"), Mockito.mock(ResolveInfo.class));
        mActivityController = Robolectric.buildActivity(CareTeamActivity.class).create().postCreate(null).start().resume();
        mMockActivity = Mockito.spy(mActivityController.get());

        Mockito.doReturn(true).when(mMockActivity).onCreateOptionsMenu(Mockito.any(Menu.class));
        mLayoutView = Mockito.spy(new DrawerLayout(ApplicationProvider.getApplicationContext()));
        Mockito.doNothing().when(mLayoutView).closeDrawer(Mockito.any(View.class));
        Mockito.doReturn(false).when(mLayoutView).isDrawerOpen(Mockito.any(LinearLayout.class));
        Mockito.doReturn(mLayoutView).when(mMockActivity).findViewById(R.id.drawer_layout);
        final LinearLayout listView = Mockito.spy(new LinearLayout(ApplicationProvider.getApplicationContext()));
        Mockito.doReturn(listView).when(mLayoutView).findViewById(R.id.drawer_fragment);
        final CareTeamActivity.ViewHolder viewHolder = new CareTeamActivity.ViewHolder(mMockActivity);
        TestUtils.setVariable(mMockActivity, "mViewHolder", viewHolder);
    }

    /**
     * Clean mock activity
     */
    @After
    public void tearDown() {
        mMockActivity = null;
        mActivityController = null;
    }

    @Test
    public void onPause() {
        TestUtils.setVariable(mMockActivity, "mCurrentFragment", Fragment.class);
        mMockActivity.onPause();
        assertNotNull(BarcodeAcceptanceTypeContext.getSavedClass());
    }

    @Test
    public void onStop() {
        BarcodeAcceptanceTypeContext.setIonActivity(mMockActivity, ScanCategory.MAX_ALLOWED_SCAN_CATEGORIES);
        mMockActivity.onStop();
        assertNull(BarcodeAcceptanceTypeContext.getIonActivity());
    }

    @Test
    public void getData() {
        final DialogController dialog = Mockito.mock(DialogController.class);
        Mockito.doReturn(dialog).when(mMockActivity).getDialogs();
        Mockito.doReturn(null).when(dialog).showProgressDialog();
        mMockActivity.getData(IDataRetriever.DataArgs.NONE);
        Mockito.verify(mMockActivity).getDialogs();
        assertNotNull(TestUtils.getVariable(mMockActivity, "mImageLoader"));
    }

    @Test
    public void getData_ReadContactDetailFragment() {
        TestUtils.setVariable(mMockActivity, "mImageLoader", new CareTeamImageLoader(mMockActivity));
        final DialogController dialog = Mockito.mock(DialogController.class);
        Mockito.doReturn(dialog).when(mMockActivity).getDialogs();
        Mockito.doReturn(null).when(dialog).showProgressDialog();

        final ReadContactDetailFragment readContactDetailFragment = Mockito.mock(ReadContactDetailFragment.class);
        TestUtils.setVariable(mMockActivity, "mReadContactDetailFragment", readContactDetailFragment);
        TestUtils.setVariable(mMockActivity, "mCurrentFragment", Fragment.class);
        mMockActivity.getData(IDataRetriever.DataArgs.NONE);
        Mockito.verify(readContactDetailFragment, Mockito.never()).setImageLoader(Mockito.any(ImageLoader.class));
        Mockito.verify(mMockActivity).getDialogs();

        TestUtils.setVariable(mMockActivity, "mCurrentFragment", ReadContactDetailFragment.class);
        mMockActivity.getData(IDataRetriever.DataArgs.NONE);
        Mockito.verify(readContactDetailFragment).setImageLoader(Mockito.any(ImageLoader.class));
        Mockito.verify(mMockActivity).getDialogs();
    }

    @Test
    public void onResponse_modelNull() {
        final CernResponse response = new CernResponse();
        mMockActivity.onResponse(response);
    }

    @Test
    public void onResponse_modelCareTeamList_exception() {
        final CareTeamList careTeamList = new CareTeamList();
        final CernResponse response = new CernResponse();
        response.data = careTeamList;
        mMockActivity.onResponse(response);
    }

    @Test
    public void onNoContentResponse() {
        mMockActivity.onNoContentResponse(null, null);
    }

    @Test
    public void onErrorResponse() {
        mMockActivity.onErrorResponse(new VolleyError("TestExceptionMessage"), null);
        Mockito.verify(mMockActivity).onBackPressed();
    }

    @Test
    public void onFailedResponse() {
        mMockActivity.onFailedResponse(null, true);
    }

    @Test
    public void setActionBarWaitCursor() {
        mMockActivity.setActionBarWaitCursor(true);
    }

    @Test
    public void cancelRequest() {
        assertFalse(mMockActivity.cancelRequest(null));
    }

    @Test
    public void backgroundAfterCancel() {
        assertTrue(mMockActivity.backgroundAfterCancel(null));
    }

    @Test
    public void getMainViewClass() {
        TestUtils.setVariable(mMockActivity, "mCurrentFragment", ContactsListFragment.class);
        assertEquals(ContactsListFragment.class, mMockActivity.getMainViewClass());
    }

    @Test
    public void setDrawerLockMode() {
        mMockActivity.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
        Mockito.verify(mLayoutView).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
    }

    @Test
    public void onDrawerItemSelected() {
        mMockActivity.onDrawerItemSelected(null);
        Mockito.verify(mMockActivity).closeDrawer();
    }

    @Test
    public void onSelectionSuccess() {
        final View view = Mockito.mock(View.class);
        Mockito.doReturn(view).when(mMockActivity).findViewById(mViewToReplace);
        mMockActivity.onSelectionSuccess();
        Mockito.verify(view).setVisibility(View.GONE);
    }

    @Test
    public void onSelectionSuccess_viewNull() {
        Mockito.doReturn(null).when(mMockActivity).findViewById(mViewToReplace);
        mMockActivity.onSelectionSuccess();
    }

    @Test
    public void isDrawerOpen_mViewHolderNull() {
        TestUtils.setVariable(mMockActivity, "mViewHolder", null);
        assertFalse(mMockActivity.isDrawerOpen());
    }

    @Test
    public void isDrawerOpen_drawerNotOpen() {
        Mockito.doReturn(false).when(mLayoutView).isDrawerOpen(Mockito.any(LinearLayout.class));
        assertFalse(mMockActivity.isDrawerOpen());
    }

    @Test
    public void isDrawerOpen_true() {
        Mockito.doReturn(true).when(mLayoutView).isDrawerOpen(Mockito.any(LinearLayout.class));
        assertTrue(mMockActivity.isDrawerOpen());
    }

    @Test
    public void closeDrawer_mViewHolderNull() {
        TestUtils.setVariable(mMockActivity, "mViewHolder", null);
        mMockActivity.closeDrawer();
        Mockito.verify(mLayoutView, Mockito.never()).closeDrawer(Mockito.any(View.class));
    }

    @Test
    public void closeDrawer() {
        mMockActivity.closeDrawer();
        Mockito.verify(mLayoutView).closeDrawer(Mockito.any(View.class));
    }

    @Test
    public void onLogout() {
        mMockActivity = Mockito.spy(mActivityController.get());
        mMockActivity.onLogout();
        Mockito.verify(mMockActivity).setResultAndFinish(ActivityUtils.RESULT_LOGOUT);
    }

    @Test
    public void onDestroy() {
        //NOTE: Logging out to clear the looper queue so that any future processing of the looper doesn't fail (i.e. logout being called after this test)
        ActivityUtils.logout(mMockActivity);
        mMockActivity.onDestroy();
        assertNull(TestUtils.getVariable(mMockActivity, "mViewHolder"));
        assertNull(TestUtils.getVariable(mMockActivity, "mDrawerToggle"));
    }

    @Test
    public void onResume_noRelationship() {
        PatientContext.clearContext();
        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "test_encounter_id_" + System.currentTimeMillis());
        PatientContext.setHasRelationship(false);
        mActivityController = Robolectric.buildActivity(CareTeamActivity.class).create().postCreate(null).start();
        mMockActivity = Mockito.spy(mActivityController.get());
        Mockito.doNothing().when(mMockActivity).getData(IDataRetriever.DataArgs.NONE);
        TestUtils.invokePrivateMethod(mMockActivity, "onResume");
        Mockito.verify(mMockActivity).setResultAndFinish(CareTeamActivity.RESULT_PATIENT_NOT_IN_CONTEXT);
    }

    @Test
    public void onResume_relationshipValid() {
        PatientContext.clearContext();
        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "test_encounter_id_" + System.currentTimeMillis());
        PatientContext.setHasRelationship(true);
        PatientContext.setHasAccess(true);
        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", PatientContext.getInstanceId());
        final ViewGroup viewGroup = Mockito.mock(ViewGroup.class);
        Mockito.doReturn(viewGroup).when(mMockActivity).findViewById(mViewToReplace);
        Mockito.doReturn(0).when(viewGroup).getChildCount();
        Mockito.doNothing().when(mMockActivity).getData(IDataRetriever.DataArgs.NONE);
        TestUtils.invokePrivateMethod(mMockActivity, "onResume");
        Mockito.verify(mMockActivity).onFragmentSelected(ContactsListFragment.class, null);
        Mockito.verify(mMockActivity, Mockito.times(2)).setMainFragmentVisibility(false);
    }

    @Test
    public void onResume_relationshipValid_firstVisit() {
        PatientContext.clearContext();
        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "test_encounter_id_" + System.currentTimeMillis());
        PatientContext.setHasRelationship(true);
        PatientContext.setHasAccess(true);
        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", null);
        final ViewGroup viewGroup = Mockito.mock(ViewGroup.class);
        Mockito.doReturn(viewGroup).when(mMockActivity).findViewById(mViewToReplace);
        Mockito.doReturn(0).when(viewGroup).getChildCount();
        Mockito.doNothing().when(mMockActivity).getData(IDataRetriever.DataArgs.NONE);
        TestUtils.invokePrivateMethod(mMockActivity, "onResume");
        Mockito.verify(mMockActivity).onFragmentSelected(ContactsListFragment.class, null);
        Mockito.verify(mMockActivity, Mockito.times(2)).setMainFragmentVisibility(false);
    }

    @Test
    public void onResume_samePatientInstanceId_viewNotFound() {
        PatientContext.clearContext();
        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "test_encounter_id_" + System.currentTimeMillis());
        PatientContext.setHasRelationship(true);
        PatientContext.setHasAccess(true);
        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", PatientContext.getInstanceId());
        Mockito.doReturn(null).when(mMockActivity).findViewById(mViewToReplace);
        Mockito.doNothing().when(mMockActivity).getData(IDataRetriever.DataArgs.NONE);
        TestUtils.invokePrivateMethod(mMockActivity, "onResume");
        Mockito.verify(mMockActivity, Mockito.never()).recreate();
    }

    @Test
    public void onResume_differentInstanceIds() {
        PatientContext.clearContext();
        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "test_encounter_id_" + System.currentTimeMillis());
        PatientContext.setHasRelationship(true);
        PatientContext.setHasAccess(true);
        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", UUID.randomUUID());
        Mockito.doNothing().when(mMockActivity).getData(IDataRetriever.DataArgs.NONE);
        TestUtils.invokePrivateMethod(mMockActivity, "onResume");
        Mockito.verify(mMockActivity).recreate();
    }

    @Test
    public void onResume_noPatientSelected() {
        PatientContext.clearContext();
        PatientContext.setHasRelationship(false);
        Mockito.doNothing().when(mMockActivity).setResultAndFinish(CareTeamActivity.RESULT_PATIENT_NOT_IN_CONTEXT);
        TestUtils.invokePrivateMethod(mMockActivity, "onResume");
        Mockito.verify(mMockActivity).setResultAndFinish(CareTeamActivity.RESULT_PATIENT_NOT_IN_CONTEXT);
    }

    @Test
    public void onResume_childCountNotZero() {
        Mockito.doNothing().when(mMockActivity).getData(IDataRetriever.DataArgs.NONE);

        PatientContext.clearContext();
        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "test_encounter_id_" + System.currentTimeMillis());
        PatientContext.setHasRelationship(true);
        PatientContext.setHasAccess(true);

        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", PatientContext.getInstanceId());
        TestUtils.setVariable(mMockActivity, "mCurrentFragment", ContactsListFragment.class);

        final ViewGroup viewGroup = Mockito.mock(ViewGroup.class);
        Mockito.doReturn(viewGroup).when(mMockActivity).findViewById(mViewToReplace);
        Mockito.doReturn(1).when(viewGroup).getChildCount();

        TestUtils.invokePrivateMethod(mMockActivity, "onResume");

        Mockito.verify(viewGroup).getChildCount();
        Mockito.verify(mMockActivity).onFragmentSelected(ContactsListFragment.class, null);
        Mockito.verify(mMockActivity).getData(IDataRetriever.DataArgs.NONE);
    }

    @Test
    public void onResume_patientInstance_equal() {
        Mockito.doNothing().when(mMockActivity).getData(Mockito.any(IDataRetriever.DataArgs.class));

        PatientContext.clearContext();
        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "test_encounter_id_" + System.currentTimeMillis());
        PatientContext.setHasRelationship(true);
        PatientContext.setHasAccess(true);
        final ViewGroup viewGroup = Mockito.mock(ViewGroup.class);

        Mockito.doReturn(viewGroup).when(mMockActivity).findViewById(mViewToReplace);
        Mockito.doReturn(0).when(viewGroup).getChildCount();

        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", PatientContext.getInstanceId());

        TestUtils.invokePrivateMethod(mMockActivity, "onResume");

        Mockito.verify(mMockActivity).onFragmentSelected(ContactsListFragment.class, null);
        Mockito.verify(mMockActivity, Mockito.never()).recreate();
    }

    @Test
    public void onFragmentSelected_nullBackStackItem() {
        final ArrayList<SerializablePair<Class<? extends Fragment>, Boolean>> backStack = TestUtils
                .getVariable(mMockActivity, "mFragmentBackStack");
        backStack.add(new SerializablePair<>(null, false));
        mMockActivity.onFragmentSelected(null, new Bundle(), false, false);

        Mockito.verify(mMockActivity, Mockito.never()).getFragmentManager();
    }

    @Test
    public void onFragmentSelected_hasBackStackItem_notEqualsFragment() {
        final ArrayList<SerializablePair<Class<? extends Fragment>, Boolean>> backStack = TestUtils
                .getVariable(mMockActivity, "mFragmentBackStack");
        backStack.add(new SerializablePair<>(ContactsListFragment.class, false));

        mMockActivity.onFragmentSelected(null, new Bundle(), false, false);
        Mockito.verify(mMockActivity, Mockito.never()).getFragmentManager();
    }

    @Test
    public void onFragmentSelected_ReadContactDetailFragment() {
        Mockito.doReturn(true).when(mMockActivity).processResponses();
        Mockito.doNothing().when(mMockActivity).getData(IDataRetriever.DataArgs.NONE);
        mMockActivity.onFragmentSelected(ReadContactDetailFragment.class, new Bundle(), false, false);
        Mockito.verify(mMockActivity).getData(IDataRetriever.DataArgs.NONE);
    }

    @Test
    public void onFragmentSelected_other_fragment() {
        Mockito.doNothing().when(mMockActivity).getData(IDataRetriever.DataArgs.NONE);
        mMockActivity.onFragmentSelected(Fragment.class, new Bundle(), false, false);
        Mockito.verify(mMockActivity, Mockito.never()).getData(IDataRetriever.DataArgs.NONE);
    }

    @Test
    public void onCreateOptionsMenu() {
        Mockito.reset(mMockActivity);
        mMockActivity.onCreateOptionsMenu(null);
    }

    @Test
    public void onConfigurationChanged() {
        final ActionBarDrawerToggle toggle = Mockito.mock(ActionBarDrawerToggle.class);
        TestUtils.setVariable(mMockActivity, "mDrawerToggle", toggle);
        final Configuration configuration = new Configuration();
        mMockActivity.onConfigurationChanged(configuration);
        Mockito.verify(toggle).onConfigurationChanged(configuration);
    }

    @Test
    public void onOptionsItemSelected_toggleItemSelected_true() {
        final ActionBarDrawerToggle toggle = Mockito.mock(ActionBarDrawerToggle.class);
        TestUtils.setVariable(mMockActivity, "mDrawerToggle", toggle);
        Mockito.doReturn(true).when(toggle).onOptionsItemSelected(Mockito.any(MenuItem.class));
        final MenuItem menuItem = Mockito.mock(MenuItem.class);
        mMockActivity.onOptionsItemSelected(menuItem);
        Mockito.verify(toggle).onOptionsItemSelected(menuItem);
    }

    @Test
    public void onOptionsItemSelected_toggleItemSelected_false() {
        final ActionBarDrawerToggle toggle = Mockito.mock(ActionBarDrawerToggle.class);
        TestUtils.setVariable(mMockActivity, "mDrawerToggle", toggle);
        Mockito.doReturn(false).when(toggle).onOptionsItemSelected(Mockito.any(MenuItem.class));
        final MenuItem item = Mockito.mock(MenuItem.class);
        mMockActivity.onOptionsItemSelected(item);
        Mockito.verify(toggle).onOptionsItemSelected(Mockito.any(MenuItem.class));
    }

    @Test
    public void onOptionsItemSelected_superOnOptionsItemSelected_true() {
        final ActionBarDrawerToggle toggle = Mockito.mock(ActionBarDrawerToggle.class);
        TestUtils.setVariable(mMockActivity, "mDrawerToggle", toggle);
        Mockito.doReturn(false).when(toggle).onOptionsItemSelected(Mockito.any(MenuItem.class));
        final MenuItem item = Mockito.mock(MenuItem.class);
        final AppCompatActivity parent = Mockito.mock(AppCompatActivity.class);
        Mockito.doReturn(true).when(parent).onOptionsItemSelected(Mockito.any(MenuItem.class));
        TestUtils.setVariable(mMockActivity, "mParent", parent);
        mMockActivity.onOptionsItemSelected(item);
        Mockito.verify(toggle).onOptionsItemSelected(Mockito.any(MenuItem.class));
    }

    @Test
    public void setDrawerIndicatorEnabled_drawerToggleNull() {
        TestUtils.setVariable(mMockActivity, "mDrawerToggle", null);
        mMockActivity.setDrawerIndicatorEnabled(true);
    }

    @Test
    public void init_patientNotSelected() {
        PatientContext.clearContext();
        Mockito.doReturn(false).when(mMockActivity).requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        TestUtils.invokePrivateMethod(mMockActivity, "init");
        Mockito.verify(mMockActivity).onNavigateUp();
    }

    @Test
    public void init_demogFragmentNotFound() {
        final FragmentManager fragmentManager = Mockito.mock(FragmentManager.class);

        Mockito.doNothing().when(mMockActivity).setContentView(Mockito.anyInt());
        Mockito.doReturn(false).when(mMockActivity).requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        Mockito.doReturn(fragmentManager).when(mMockActivity).getFragmentManager();
        Mockito.doReturn(null).when(fragmentManager).findFragmentById(R.id.PatientDemographicsFragment);

        TestUtils.invokePrivateMethod(mMockActivity, "init");

        Mockito.verify(mMockActivity).setResultAndFinish(ActivityUtils.RESULT_NOT_ENOUGH_IDENTIFIERS);
    }

    @Test
    public void onCreate_openDrawer() {
        final DrawerFragment drawerFragment = Mockito.mock(DrawerFragment.class);
        final DemographicsFragment demographicsFragment = Mockito.mock(DemographicsFragment.class);
        final FragmentManager fragmentManager = Mockito.mock(FragmentManager.class);

        PatientContext.setHasRelationship(true);

        Mockito.doNothing().when(mMockActivity).setContentView(Mockito.anyInt());
        Mockito.doReturn(fragmentManager).when(mMockActivity).getFragmentManager();
        Mockito.doReturn(demographicsFragment).when(fragmentManager).findFragmentById(R.id.PatientDemographicsFragment);
        Mockito.doReturn(false).when(mMockActivity).requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        Mockito.doReturn(drawerFragment).when(fragmentManager).findFragmentById(R.id.drawer_fragment);

        final ActionBarDrawerToggle toggle = TestUtils.getVariable(mMockActivity, "mDrawerToggle");
        TestUtils.setVariable(toggle, "this$0", mMockActivity);
        toggle.onDrawerOpened(null);

        Mockito.verify(mMockActivity).invalidateOptionsMenu();
    }

    @Test
    public void onCreate_openDrawer_drawerFragmentNotFound() {
        final DemographicsFragment demographicsFragment = Mockito.mock(DemographicsFragment.class);
        final FragmentManager fragmentManager = Mockito.mock(FragmentManager.class);

        PatientContext.setHasRelationship(true);

        Mockito.doNothing().when(mMockActivity).setContentView(Mockito.anyInt());
        Mockito.doReturn(fragmentManager).when(mMockActivity).getFragmentManager();
        Mockito.doReturn(demographicsFragment).when(fragmentManager).findFragmentById(R.id.PatientDemographicsFragment);
        Mockito.doReturn(false).when(mMockActivity).requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        Mockito.doReturn(null).when(fragmentManager).findFragmentById(R.id.drawer_fragment);

        final ActionBarDrawerToggle toggle = TestUtils.getVariable(mMockActivity, "mDrawerToggle");
        TestUtils.setVariable(toggle, "this$0", mMockActivity);
        toggle.onDrawerOpened(null);

        Mockito.verify(mMockActivity).invalidateOptionsMenu();
    }

    @Test
    public void onCreate_closeDrawer() {
        final ActionBar actionBar = Mockito.mock(ActionBar.class);
        final DrawerFragment drawerFragment = Mockito.mock(DrawerFragment.class);
        final DemographicsFragment demographicsFragment = Mockito.mock(DemographicsFragment.class);
        final FragmentManager fragmentManager = Mockito.mock(FragmentManager.class);
        Mockito.doNothing().when(mMockActivity).getData(IDataRetriever.DataArgs.NONE);
        final Fragment fragment = Mockito.mock(Fragment.class);

        PatientContext.setHasRelationship(true);

        Mockito.doNothing().when(mMockActivity).setContentView(Mockito.anyInt());
        Mockito.doReturn(fragmentManager).when(mMockActivity).getFragmentManager();
        Mockito.doReturn(demographicsFragment).when(fragmentManager).findFragmentById(R.id.PatientDemographicsFragment);
        Mockito.doReturn(false).when(mMockActivity).requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        Mockito.doReturn(drawerFragment).when(fragmentManager).findFragmentById(R.id.drawer_fragment);
        Mockito.doReturn(fragment).when(fragmentManager).findFragmentById(mViewToReplace);
        Mockito.doNothing().when(mMockActivity).startActivity(Mockito.any(Intent.class));

        Mockito.doReturn(actionBar).when(mMockActivity).getSupportActionBar();

        final ActionBarDrawerToggle toggle = TestUtils.getVariable(mMockActivity, "mDrawerToggle");
        TestUtils.setVariable(toggle, "this$0", mMockActivity);
        ActiveModuleManager.setActiveModule(TestActivity.class, 0, null);
        toggle.onDrawerClosed(null);

        Mockito.verify(mMockActivity).invalidateOptionsMenu();
        Mockito.verify(mMockActivity).startActivity(Mockito.any(Intent.class));
    }

    @Test
    public void onCreate_closeDrawer_patientContextFragmentNotFound() {
        final FragmentManager fragmentManager = Mockito.mock(FragmentManager.class);

        PatientContext.setHasRelationship(true);

        Mockito.doReturn(fragmentManager).when(mMockActivity).getFragmentManager();
        Mockito.doReturn(null).when(fragmentManager).findFragmentById(mViewToReplace);
        Mockito.doNothing().when(mMockActivity).startActivity(Mockito.any(Intent.class));

        final ActionBarDrawerToggle toggle = TestUtils.getVariable(mMockActivity, "mDrawerToggle");
        TestUtils.setVariable(toggle, "this$0", mMockActivity);
        ActiveModuleManager.setActiveModule(TestActivity.class, 0, null);
        toggle.onDrawerClosed(null);

        Mockito.verify(mMockActivity).invalidateOptionsMenu();
        Mockito.verify(mMockActivity).startActivity(Mockito.any(Intent.class));
    }

    @Test
    public void onResponseReceived_notDemographicsFragment() {
        mMockActivity.onResponseReceived(Fragment.class);

        Mockito.verify(mMockActivity, Mockito.never()).getFragmentManager();
        assertFalse(TestUtils.getVariable(mMockActivity, "mCanShowFragments"));
    }

    @Test
    public void onResponseReceived_demographicsFragmentNotFound() {
        final FragmentManager fragmentManager = Mockito.mock(FragmentManager.class);

        Mockito.doReturn(fragmentManager).when(mMockActivity).getFragmentManager();
        Mockito.doReturn(null).when(fragmentManager).findFragmentById(R.id.PatientDemographicsFragment);

        mMockActivity.onResponseReceived(DemographicsFragment.class);
        assertTrue(TestUtils.getVariable(mMockActivity, "mCanShowFragments"));
        Mockito.verify(mMockActivity, Mockito.never()).setMainFragmentVisibility(true);
    }

    @Test
    public void onResponseReceived_fragmentNotCuraAuthnFragment() {
        final FragmentManager fragmentManager = Mockito.mock(FragmentManager.class);
        final DemographicsFragment demographicsFragment = Mockito.mock(DemographicsFragment.class);
        final Fragment fragment = Mockito.mock(Fragment.class);

        Mockito.doReturn(fragmentManager).when(mMockActivity).getFragmentManager();
        Mockito.doReturn(demographicsFragment).when(fragmentManager).findFragmentById(R.id.PatientDemographicsFragment);
        Mockito.doReturn(fragment).when(fragmentManager).findFragmentById(mViewToReplace);

        mMockActivity.onResponseReceived(DemographicsFragment.class);

        Mockito.verify(demographicsFragment).setFragmentVisibility(true);
        assertTrue(TestUtils.getVariable(mMockActivity, "mCanShowFragments"));
        Mockito.verify(mMockActivity).setMainFragmentVisibility(true);
    }

    @Test
    public void onResponseReceived() {
        final FragmentManager fragmentManager = Mockito.mock(FragmentManager.class);
        final DemographicsFragment demographicsFragment = Mockito.mock(DemographicsFragment.class);
        final CuraAuthnFragment fragment = Mockito.mock(CuraAuthnFragment.class);

        Mockito.doReturn(fragmentManager).when(mMockActivity).getFragmentManager();
        Mockito.doReturn(demographicsFragment).when(fragmentManager).findFragmentById(R.id.PatientDemographicsFragment);
        Mockito.doReturn(fragment).when(fragmentManager).findFragmentById(mViewToReplace);

        mMockActivity.onResponseReceived(DemographicsFragment.class);

        Mockito.verify(demographicsFragment).setFragmentVisibility(true);
        Mockito.verify(fragment).setFragmentVisibility(true);
        assertTrue(TestUtils.getVariable(mMockActivity, "mCanShowFragments"));
    }

    @Test (expected = IllegalArgumentException.class)
    public void viewHolder_nullRoot() {
        new CareTeamActivity.ViewHolder(null);
    }

    @Test (expected = NullPointerException.class)
    public void viewHolder_nullEverything() {
        Mockito.doReturn(null).when(mMockActivity).findViewById(R.id.drawer_layout);
        new CareTeamActivity.ViewHolder(mMockActivity);
    }

    @Test (expected = NullPointerException.class)
    public void viewHolder_nullNot1() {
        Mockito.doReturn(null).when(mLayoutView).findViewById(R.id.drawer_fragment);
        new CareTeamActivity.ViewHolder(mMockActivity);
    }

    @Test
    public void onSaveInstanceState() {
        final Bundle b = new Bundle();

        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", PatientContext.getInstanceId());
        TestUtils.invokePrivateMethod(mMockActivity, "onSaveInstanceState", new Class[]{android.os.Bundle.class}, b);
        assertEquals(PatientContext.getInstanceId().toString(), b.getString(PatientContext.PATIENT_INSTANCE_ID_STORAGEID));
    }

    @Test
    public void setupActionBar() {
        final ActionBar bar = Mockito.mock(ActionBar.class);

        Mockito.doReturn(bar).when(mMockActivity).getSupportActionBar();
        Mockito.doNothing().when(bar).setTitle(R.string.careteam_title);

        TestUtils.invokePrivateMethod(mMockActivity, "setupActionBar");

        Mockito.verify(bar).setTitle(R.string.careteam_title);
        Mockito.verify(bar).setDisplayShowHomeEnabled(true);
        Mockito.verify(bar).setHomeButtonEnabled(true);
    }

    @Test
    public void onContactClicked_targetNull() {
        mMockActivity.onContactClicked(null, new Bundle());
        Mockito.verify(mMockActivity, Mockito.never()).getIntent();
        Mockito.verify(mMockActivity, Mockito.never()).onFragmentSelected(Mockito.any(Class.class), Mockito.any(Bundle.class));
    }

    @Test
    public void onContactClicked_bundleNull() {
        mMockActivity.onContactClicked(ReadContactDetailFragment.class, null);
        Mockito.verify(mMockActivity, Mockito.never()).getIntent();
        Mockito.verify(mMockActivity, Mockito.never()).onFragmentSelected(Mockito.any(Class.class), Mockito.any(Bundle.class));
    }

    @Test
    public void onContactClicked() {
        final Bundle bundle = new Bundle();
        mMockActivity.onContactClicked(ReadContactDetailFragment.class, bundle);
        Mockito.verify(mMockActivity).getIntent();
        Mockito.verify(mMockActivity).onFragmentSelected(ReadContactDetailFragment.class, bundle);
    }

    @Test
    public void isPatientContextModule() {
        assertTrue(mMockActivity.isPatientContextModule());
    }

    @Test
    public void setMainFragmentVisibility() {
        TestUtils.setVariable(mMockActivity, "mDataRetrieved", false);
        Mockito.doReturn(true).when(mMockActivity).canAllFragmentsShow();
        final View view = Mockito.mock(View.class);
        Mockito.doReturn(view).when(mMockActivity).findViewById(mViewToReplace);
        mMockActivity.setMainFragmentVisibility(true);
        Mockito.verify(view).setVisibility(View.GONE);

        TestUtils.setVariable(mMockActivity, "mDataRetrieved", true);
        Mockito.doReturn(false).when(mMockActivity).canAllFragmentsShow();
        mMockActivity.setMainFragmentVisibility(true);
        Mockito.verify(view, Mockito.times(2)).setVisibility(View.GONE);

        Mockito.doReturn(true).when(mMockActivity).canAllFragmentsShow();
        mMockActivity.setMainFragmentVisibility(true);
        Mockito.verify(view).setVisibility(View.VISIBLE);

        mMockActivity.setMainFragmentVisibility(false);
        Mockito.verify(view, Mockito.times(3)).setVisibility(View.GONE);
    }

    @Test
    public void setMainFragmentVisibility_viewNull() {
        Mockito.doReturn(null).when(mMockActivity).findViewById(mViewToReplace);
        mMockActivity.setMainFragmentVisibility(true);
    }

    @Test
    public void onScanReset() {
        final ScanProcessor scanProcessor = Mockito.mock(ScanProcessor.class);
        Mockito.doReturn(true).when(scanProcessor).isEnabled(Mockito.any(Context.class));
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_MEDICATION, scanProcessor);
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_PATIENT, scanProcessor);
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_DEVICE, scanProcessor);
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_PRSNL, scanProcessor);

        BarcodeAcceptanceTypeContext.setIonActivity(mMockActivity, ScanCategory.SCAN_CATEGORY_NOT_ALLOWED);
        mMockActivity.onScanReset();
        assertTrue(BarcodeAcceptanceTypeContext.isProcessorScanningEnabled(ScanCategory.SCAN_CATEGORY_PATIENT));
        assertTrue(BarcodeAcceptanceTypeContext.isProcessorScanningEnabled(ScanCategory.SCAN_CATEGORY_MEDICATION));
        assertTrue(BarcodeAcceptanceTypeContext.isProcessorScanningEnabled(ScanCategory.SCAN_CATEGORY_PRSNL));
        assertTrue(BarcodeAcceptanceTypeContext.isProcessorScanningEnabled(ScanCategory.SCAN_CATEGORY_DEVICE));
    }

    @Test
    public void onStart() {
        mMockActivity.onStart();
        Mockito.verify(mMockActivity).onScanReset();
    }

    @Test
    public void onRefreshRequested() {
        Mockito.doNothing().when(mMockActivity).getData(IDataRetriever.DataArgs.PULL_TO_REFRESH);
        mMockActivity.onRefreshRequested(null, true);
        Mockito.verify(mMockActivity).getData(IDataRetriever.DataArgs.PULL_TO_REFRESH);
    }

    @Test
    public void onListItemClick_notContactListFragment() {
        TestUtils.setVariable(mMockActivity, "mCurrentFragment", Object.class);
        mMockActivity.onListItemClick(null, null, null, 0, 0);
    }
}
