package com.cerner.nursing.nursing.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.cerner.cura.allergies.ui.AllergyFragment;
import com.cerner.cura.base.CuraAuthnFragment;
import com.cerner.cura.base.PatientContext;
import com.cerner.cura.demographics.ui.DemographicsFragment;
import com.cerner.cura.ppr.ui.PPRFragment;
import com.cerner.cura.requestor.MockDataManager;
import com.cerner.cura.scanning.BarcodeAcceptanceTypeContext;
import com.cerner.cura.scanning.ScanCategory;
import com.cerner.cura.scanning.ScanManager;
import com.cerner.cura.scanning.ScanProcessor;
import com.cerner.cura.test.helper.TestUtils;
import com.cerner.cura.ui.elements.drawer.DrawerFragment;
import com.cerner.cura.utils.ActiveModuleManager;
import com.cerner.cura.utils.ActivityUtils;
import com.cerner.cura.utils.PatientRelationshipExpiredHandlerUtil;
import com.cerner.cura.utils.SerializablePair;
import com.cerner.nursing.nursing.R;

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
import static org.mockito.Matchers.eq;

/**
 * @author Lam Tran (lt028506) on 10/22/13.
 */
@RunWith (RobolectricTestRunner.class)
public class PatientChartActivityTests {
    private ActivityController<PatientChartActivity> mActivityController;
    private PatientChartActivity mMockActivity;
    private DrawerLayout mLayoutView;
    private static final int mViewToReplace = R.id.content_pane_fragment_container;

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
        mActivityController = Robolectric.buildActivity(PatientChartActivity.class).create().postCreate(null);
        mMockActivity = Mockito.spy(mActivityController.get());

        mLayoutView = Mockito.spy(new DrawerLayout(ApplicationProvider.getApplicationContext()));
        Mockito.doNothing().when(mLayoutView).closeDrawer(Mockito.any(View.class));
        Mockito.doReturn(mLayoutView).when(mMockActivity).findViewById(R.id.drawer_layout);
        final LinearLayout listView = Mockito.spy(new LinearLayout(ApplicationProvider.getApplicationContext()));
        Mockito.doReturn(listView).when(mLayoutView).findViewById(R.id.drawer_fragment);
        final FrameLayout bottomActionToolbar = Mockito.spy(new FrameLayout(ApplicationProvider.getApplicationContext()));
        Mockito.doReturn(bottomActionToolbar).when(mLayoutView).findViewById(R.id.bottom_action_toolbar);
        Mockito.doReturn(new Button(ApplicationProvider.getApplicationContext())).when(mLayoutView).findViewById(R.id.floating_info_button);
        final ContentActivity.ViewHolder viewHolder = new ContentActivity.ViewHolder(mMockActivity);
        TestUtils.setVariable(mMockActivity, "mViewHolder", viewHolder);
    }

    /**
     * Clean mock activity
     */
    @After
    public void tearDown() {
        mMockActivity = null;
        mActivityController = null;
        PatientRelationshipExpiredHandlerUtil.setPatientRelationshipExpiredHandler(null);
    }

    @Test
    public void activityIsNotNull() {
        assertNotNull(mMockActivity);
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
        final View view = Mockito.mock(View.class);
        Mockito.doReturn(null).when(mMockActivity).findViewById(mViewToReplace);
        mMockActivity.onSelectionSuccess();
        Mockito.verify(view, Mockito.never()).setVisibility(View.GONE);
    }

    @Test
    public void isDrawerOpen_viewHolderNull() {
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
    public void onStart_noPatientSelected() {
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn(true).when(intent).getBooleanExtra(Mockito.anyString(), Mockito.anyBoolean());
        PatientContext.clearContext();
        Mockito.doReturn(intent).when(mMockActivity).getIntent();
        mMockActivity.onStart();
        Mockito.verify(mMockActivity).setResultAndFinish(eq(ActivityUtils.RESULT_PATIENT_NOT_IN_CONTEXT));
    }

    @Test
    public void onStart_samePatientInstance() {
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn(true).when(intent).getBooleanExtra(Mockito.anyString(), Mockito.anyBoolean());
        PatientContext.clearContext();
        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "test_encounter_id_" + System.currentTimeMillis());
        PatientContext.setHasRelationship(false);
        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", PatientContext.getInstanceId());
        Mockito.doReturn(intent).when(mMockActivity).getIntent();
        ActiveModuleManager.setActiveModule(PatientChartActivity.class, 0, null);
        mMockActivity.onStart();
        Mockito.verify(mMockActivity, Mockito.never()).recreate();
        Mockito.verify(mMockActivity).onScanReset();
    }

    @Test
    public void onStart_differentPatientInstance() {
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn(true).when(intent).getBooleanExtra(Mockito.anyString(), Mockito.anyBoolean());
        PatientContext.clearContext();
        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "test_encounter_id_" + System.currentTimeMillis());
        PatientContext.setHasRelationship(false);
        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", UUID.randomUUID());
        Mockito.doReturn(intent).when(mMockActivity).getIntent();
        ActiveModuleManager.setActiveModule(PatientChartActivity.class, 0, null);
        mMockActivity.onStart();
        Mockito.verify(mMockActivity).recreate();
    }

    @Test
    public void onResume_noRelationship() {
        PatientContext.clearContext();
        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "test_encounter_id_" + System.currentTimeMillis());
        PatientContext.setHasRelationship(false);
        TestUtils.invokePrivateMethod(mMockActivity, "onResume");
        Mockito.verify(mMockActivity).onFragmentSelected(PPRFragment.class, null, true, true);
    }

    @Test
    public void onResume_noRelationship_firstVisit() {
        PatientContext.clearContext();
        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "test_encounter_id_" + System.currentTimeMillis());
        PatientContext.setHasRelationship(false);
        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", null);
        TestUtils.invokePrivateMethod(mMockActivity, "onResume");
        Mockito.verify(mMockActivity).onFragmentSelected(PPRFragment.class, null, true, true);
    }

    @Test
    public void onResume_relationshipValid_firstVisit() {
        PatientContext.setHasRelationship(true);
        PatientContext.setHasAccess(true);
        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", null);
        final ViewGroup viewGroup = Mockito.mock(ViewGroup.class);
        Mockito.doReturn(viewGroup).when(mMockActivity).findViewById(mViewToReplace);
        Mockito.doReturn(0).when(viewGroup).getChildCount();
        TestUtils.invokePrivateMethod(mMockActivity, "onResume");
        Mockito.verify(viewGroup).setVisibility(View.VISIBLE);
        Mockito.verify(mMockActivity).onFragmentSelected(PatientSummaryFragment.class, null);
    }

    @Test
    public void onResume_relationshipValid() {
        PatientContext.setHasRelationship(true);
        PatientContext.setHasAccess(true);
        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", PatientContext.getInstanceId());
        final ViewGroup viewGroup = Mockito.mock(ViewGroup.class);
        Mockito.doReturn(viewGroup).when(mMockActivity).findViewById(mViewToReplace);
        Mockito.doReturn(0).when(viewGroup).getChildCount();
        TestUtils.invokePrivateMethod(mMockActivity, "onResume");
        Mockito.verify(viewGroup).setVisibility(View.VISIBLE);
        Mockito.verify(mMockActivity).onFragmentSelected(PatientSummaryFragment.class, null);
    }

    @Test
    public void onResume_samePatientInstanceId_viewNotFound() {
        PatientContext.setHasRelationship(true);
        PatientContext.setHasAccess(true);
        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", PatientContext.getInstanceId());
        Mockito.doReturn(null).when(mMockActivity).findViewById(mViewToReplace);
        TestUtils.invokePrivateMethod(mMockActivity, "onResume");
        Mockito.verify(mMockActivity, Mockito.never()).onFragmentSelected(PatientSummaryFragment.class, null);
    }

    @Test
    public void onResume_differentInstanceIds() {
        PatientContext.setHasRelationship(true);
        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", UUID.randomUUID());
        TestUtils.invokePrivateMethod(mMockActivity, "onResume");
        Mockito.verify(mMockActivity, Mockito.never()).onFragmentSelected(PatientSummaryFragment.class, null);
    }

    @Test
    public void onResume_differentInstanceIds_initialLoad() {
        PatientContext.setHasRelationship(true);
        PatientContext.setHasAccess(true);
        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", null);
        TestUtils.invokePrivateMethod(mMockActivity, "onResume");
        Mockito.verify(mMockActivity).onFragmentSelected(PatientSummaryFragment.class, null);
    }

    @Test
    public void onResume_noPatientSelected() {
        PatientContext.clearContext();
        PatientContext.setHasRelationship(false);
        Mockito.doNothing().when(mMockActivity).setResultAndFinish(ActivityUtils.RESULT_PATIENT_NOT_IN_CONTEXT);
        TestUtils.invokePrivateMethod(mMockActivity, "onResume");
        Mockito.verify(mMockActivity).setResultAndFinish(ActivityUtils.RESULT_PATIENT_NOT_IN_CONTEXT);
    }

    @Test
    public void onFragmentSelected() {
        Mockito.doReturn(true).when(mMockActivity).processResponses();
        final FragmentManager fragmentManager = Mockito.spy(mMockActivity.getFragmentManager());
        Mockito.doReturn(fragmentManager).when(mMockActivity).getFragmentManager();

        Mockito.doReturn(new PatientSummaryFragment()).when(fragmentManager).findFragmentById(mViewToReplace);

        mMockActivity.onFragmentSelected(AllergyFragment.class, null, false, false);

        final ArrayList<SerializablePair<Class<? extends Fragment>, Boolean>> fragmentBackStack = TestUtils
                .getVariable(mMockActivity, "mFragmentBackStack");
        assertEquals(1, fragmentBackStack.size());
        assertEquals(PatientSummaryFragment.class, fragmentBackStack.get(0).first);
        assertEquals(AllergyFragment.class, TestUtils.getVariable(mMockActivity, "mCurrentFragment"));
    }

    @Test
    public void onFragmentSelected_patientInstanceId_null() {
        final FragmentManager fragmentManager = Mockito.spy(mMockActivity.getFragmentManager());
        Mockito.doReturn(fragmentManager).when(mMockActivity).getFragmentManager();
        Mockito.doReturn(new PatientSummaryFragment()).when(fragmentManager).findFragmentById(mViewToReplace);
        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", null);
        mMockActivity.onFragmentSelected(AllergyFragment.class, null, false, false);
        Mockito.verify(mMockActivity, Mockito.never()).recreate();
    }

    @Test
    public void onFragmentSelected_patientInstanceId_PatientContext_instance_same() {
        final FragmentManager fragmentManager = Mockito.spy(mMockActivity.getFragmentManager());
        Mockito.doReturn(fragmentManager).when(mMockActivity).getFragmentManager();
        Mockito.doReturn(new PatientSummaryFragment()).when(fragmentManager).findFragmentById(mViewToReplace);
        final UUID uuid = PatientContext.getInstanceId();
        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", uuid);
        mMockActivity.onFragmentSelected(AllergyFragment.class, null, false, false);
        Mockito.verify(mMockActivity, Mockito.never()).recreate();
    }

    @Test
    public void onFragmentSelected_patientInstanceId_PatientContext_instance_different() {
        final FragmentManager fragmentManager = Mockito.spy(mMockActivity.getFragmentManager());
        Mockito.doReturn(fragmentManager).when(mMockActivity).getFragmentManager();
        Mockito.doReturn(new PatientSummaryFragment()).when(fragmentManager).findFragmentById(mViewToReplace);
        final UUID uuid = UUID.randomUUID();
        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", uuid);
        mMockActivity.onFragmentSelected(AllergyFragment.class, null, false, false);
        Mockito.verify(mMockActivity).recreate();
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
        final Activity parent = Mockito.mock(Activity.class);
        Mockito.doReturn(true).when(parent).onOptionsItemSelected(Mockito.any(MenuItem.class));
        TestUtils.setVariable(mMockActivity, "mParent", parent);
        mMockActivity.onOptionsItemSelected(item);
        Mockito.verify(toggle).onOptionsItemSelected(Mockito.any(MenuItem.class));
    }

    @Test
    public void onCreate_patientNotSelected() {
        PatientContext.clearContext();
        Mockito.doReturn(false).when(mMockActivity).requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        TestUtils.invokePrivateMethod(mMockActivity, "init");
        Mockito.verify(mMockActivity).onNavigateUp();
    }

    @Test (expected = NullPointerException.class)
    public void onCreate_nullViewHolder() throws Throwable {
        Mockito.doReturn(false).when(mMockActivity).requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        Mockito.doNothing().when(mMockActivity).setContentView(Mockito.anyInt());

        Mockito.doReturn(null).when(mMockActivity).findViewById(R.id.drawer_layout);
        try {
            TestUtils.invokePrivateMethod(mMockActivity, "init");
        } catch (final RuntimeException ex) {
            throw ex.getCause().getCause();
        }
        Mockito.verify(mMockActivity, Mockito.never()).setResultAndFinish(ActivityUtils.RESULT_NOT_ENOUGH_IDENTIFIERS);
    }

    @Test
    public void onCreate_demogFragmentNotFound() {
        final FragmentManager fragmentManager = Mockito.mock(FragmentManager.class);

        Mockito.doNothing().when(mMockActivity).setContentView(Mockito.anyInt());
        Mockito.doReturn(false).when(mMockActivity).requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        Mockito.doReturn(fragmentManager).when(mMockActivity).getFragmentManager();
        Mockito.doReturn(null).when(fragmentManager).findFragmentById(R.id.PatientDemographicsFragment);

        TestUtils.invokePrivateMethod(mMockActivity, "init");

        Mockito.verify(mMockActivity).setResultAndFinish(ActivityUtils.RESULT_NOT_ENOUGH_IDENTIFIERS);
    }

    @Test
    public void onPostCreate_openDrawer() {
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
    public void onPostCreate_openDrawer_drawerFragmentNotFound() {
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
    public void onPostCreate_closeDrawer() {
        final ActionBarDrawerToggle toggle = TestUtils.getVariable(mMockActivity, "mDrawerToggle");
        TestUtils.setVariable(toggle, "this$0", mMockActivity);
        ActiveModuleManager.setActiveModule(mMockActivity.getClass(), 0, null);
        toggle.onDrawerClosed(null);

        Mockito.verify(mMockActivity).invalidateOptionsMenu();
        Mockito.verify(mMockActivity, Mockito.never()).startActivity(Mockito.any(Intent.class));
    }

    @Test
    public void onPostCreate_closeDrawer_differentActiveModule() {
        final ActionBar actionBar = Mockito.mock(ActionBar.class);
        final DrawerFragment drawerFragment = Mockito.mock(DrawerFragment.class);
        final DemographicsFragment demographicsFragment = Mockito.mock(DemographicsFragment.class);
        final FragmentManager fragmentManager = Mockito.mock(FragmentManager.class);
        final PatientSummaryFragment patientSummaryFragment = Mockito.mock(PatientSummaryFragment.class);

        PatientContext.setHasRelationship(true);

        Mockito.doNothing().when(mMockActivity).setContentView(Mockito.anyInt());
        Mockito.doReturn(fragmentManager).when(mMockActivity).getFragmentManager();
        Mockito.doReturn(demographicsFragment).when(fragmentManager).findFragmentById(R.id.PatientDemographicsFragment);
        Mockito.doReturn(false).when(mMockActivity).requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        Mockito.doReturn(drawerFragment).when(fragmentManager).findFragmentById(R.id.drawer_fragment);
        Mockito.doReturn(patientSummaryFragment).when(fragmentManager).findFragmentById(mViewToReplace);
        Mockito.doNothing().when(mMockActivity).startActivity(Mockito.any(Intent.class));

        Mockito.doReturn(actionBar).when(mMockActivity).getActionBar();

        final ActionBarDrawerToggle toggle = TestUtils.getVariable(mMockActivity, "mDrawerToggle");
        TestUtils.setVariable(toggle, "this$0", mMockActivity);
        ActiveModuleManager.setActiveModule(CareTeamActivity.class, 0, null);
        toggle.onDrawerClosed(null);

        Mockito.verify(mMockActivity).invalidateOptionsMenu();
        Mockito.verify(mMockActivity).startActivity(Mockito.any(Intent.class));
    }

    @Test
    public void onDestroy() {
        mMockActivity.onDestroy();
        assertNull(TestUtils.getVariable(mMockActivity, "mViewHolder"));
        assertNull(TestUtils.getVariable(mMockActivity, "mDrawerToggle"));
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
        new ContentActivity.ViewHolder(null);
    }

    @Test (expected = NullPointerException.class)
    public void viewHolder_nullEverything() {
        Mockito.doReturn(null).when(mMockActivity).findViewById(R.id.drawer_layout);
        new ContentActivity.ViewHolder(mMockActivity);
    }

    @Test (expected = NullPointerException.class)
    public void viewHolder_nullNot1() {
        Mockito.doReturn(null).when(mLayoutView).findViewById(R.id.drawer_fragment);
        new ContentActivity.ViewHolder(mMockActivity);
    }

    @Test
    public void setDrawerLockMode() {
        mMockActivity.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
        Mockito.verify(mLayoutView).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
    }

    @Test
    public void setDrawerLockMode_viewHolder_null() {
        TestUtils.setVariable(mMockActivity, "mViewHolder", null);
        mMockActivity.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
        Mockito.verify(mLayoutView, Mockito.never()).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
    }

    @Test
    public void setDrawerIndicatorEnabled_drawerToggle_null() {
        TestUtils.setVariable(mMockActivity, "mDrawerToggle", null);
        mMockActivity.setDrawerIndicatorEnabled(true);
    }

    @Test
    public void setDrawerIndicatorEnabled_drawerToggle() {
        final ActionBarDrawerToggle drawerToggle = Mockito.mock(ActionBarDrawerToggle.class);
        TestUtils.setVariable(mMockActivity, "mDrawerToggle", drawerToggle);
        mMockActivity.setDrawerIndicatorEnabled(true);
        Mockito.verify(drawerToggle).setDrawerIndicatorEnabled(true);
    }

    @Test
    public void onAuthnResume() {
        BarcodeAcceptanceTypeContext.removeIonActivity(mMockActivity);
        mMockActivity.onAuthnResume();
        assertNotNull(BarcodeAcceptanceTypeContext.getIonActivity());
    }

    @Test
    public void onPause() {
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
    public void getMainViewClass() {
        TestUtils.setVariable(mMockActivity, "mCurrentFragment", null);
        assertEquals(PPRFragment.class, mMockActivity.getMainViewClass());

        TestUtils.setVariable(mMockActivity, "mCurrentFragment", PatientSummaryFragment.class);
        assertEquals(PatientSummaryFragment.class, mMockActivity.getMainViewClass());
    }

    @Test
    public void onSaveInstanceState() {
        final Bundle b = new Bundle();

        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", PatientContext.getInstanceId());
        TestUtils.invokePrivateMethod(mMockActivity, "onSaveInstanceState", new Class[]{android.os.Bundle.class}, b);
        assertEquals(PatientContext.getInstanceId().toString(), b.getString(PatientContext.PATIENT_INSTANCE_ID_STORAGEID));
    }

    @Test
    public void isPatientContextModule() {
        assertTrue(mMockActivity.isPatientContextModule());
    }

    @Test
    public void onRelationshipEstablished() {
        mMockActivity.onRelationshipEstablished();
        Mockito.verify(mMockActivity).onFragmentSelected(PatientSummaryFragment.class, null);
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
}