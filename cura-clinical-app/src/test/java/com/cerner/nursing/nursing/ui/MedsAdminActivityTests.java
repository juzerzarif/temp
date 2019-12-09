package com.cerner.nursing.nursing.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
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

import com.cerner.cura.base.CuraAuthnFragment;
import com.cerner.cura.base.PatientContext;
import com.cerner.cura.demographics.ui.DemographicsFragment;
import com.cerner.cura.medications.legacy.ui.MedsTaskSelectionBaseFragment;
import com.cerner.cura.medications.ui.LastGivenDetailsFragment;
import com.cerner.cura.medications.ui.MedsActivityListFragment;
import com.cerner.cura.medications.ui.MedsAlertBaseFragment;
import com.cerner.cura.medications.ui.MedsAlertFragment;
import com.cerner.cura.medications.ui.MedsAlertOverdueFragment;
import com.cerner.cura.medications.ui.MedsChartingDetailsFragment;
import com.cerner.cura.medications.ui.MedsOrderDetailsFragment;
import com.cerner.cura.ppr.ui.PPRFragment;
import com.cerner.cura.requestor.MockDataManager;
import com.cerner.cura.scanning.BarcodeAcceptanceTypeContext;
import com.cerner.cura.scanning.ScanCategory;
import com.cerner.cura.scanning.ScanManager;
import com.cerner.cura.scanning.ScanProcessor;
import com.cerner.cura.scanning.datamodel.ScanViewResponse;
import com.cerner.cura.test.helper.TestUtils;
import com.cerner.cura.ui.elements.drawer.DrawerFragment;
import com.cerner.cura.utils.ActiveModuleManager;
import com.cerner.cura.utils.ActivityUtils;
import com.cerner.cura.utils.PatientRelationshipExpiredHandlerUtil;
import com.cerner.cura.utils.SerializablePair;
import com.cerner.ion.session.AuthnResponse;
import com.cerner.ion.session.IonAuthnSessionUtils;
import com.cerner.ion.session.SessionCheckHandler;
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
import java.util.HashMap;
import java.util.Map;
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
public class MedsAdminActivityTests {
    private MedsAdminActivity mMockActivity;
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
        mockActivity();
    }

    private void mockActivity() {
        final ActivityController<MedsAdminActivity> activityController = Robolectric.buildActivity(MedsAdminActivity.class).create().postCreate(null);
        mMockActivity = Mockito.spy(activityController.get());

        mLayoutView = Mockito.spy(new DrawerLayout(ApplicationProvider.getApplicationContext()));
        Mockito.doNothing().when(mLayoutView).closeDrawer(Mockito.any(View.class));
        Mockito.doReturn(false).when(mLayoutView).isDrawerOpen(Mockito.any(LinearLayout.class));
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
        PatientRelationshipExpiredHandlerUtil.setPatientRelationshipExpiredHandler(null);
    }

    @Test
    public void onNewIntent() {
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn(new ScanViewResponse()).when(intent).getSerializableExtra(ScanProcessor.INTENT_SCAN_EXTRA);
        mMockActivity.onNewIntent(intent);
        assertNotNull(TestUtils.getVariable(mMockActivity, "mScanViewResponse"));
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
        final ScanProcessor scanProcessor = Mockito.mock(ScanProcessor.class);
        Mockito.doReturn(true).when(scanProcessor).isEnabled(Mockito.isNull());
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_MEDICATION, scanProcessor);
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_PATIENT, scanProcessor);
        BarcodeAcceptanceTypeContext.enableScanningProcessors(ScanCategory.SCAN_CATEGORY_NOT_ALLOWED);
        mMockActivity.onStop();
        assertTrue(BarcodeAcceptanceTypeContext.isProcessorScanningEnabled(ScanCategory.SCAN_CATEGORY_MEDICATION));
        assertNull(BarcodeAcceptanceTypeContext.getIonActivity());
    }

    @Test
    public void onStart() {
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn(true).when(intent).getBooleanExtra(Mockito.anyString(), Mockito.anyBoolean());
        PatientContext.clearContext();
        Mockito.doReturn(intent).when(mMockActivity).getIntent();

        mMockActivity.onStart();
        Mockito.verify(mMockActivity).onScanReset();
        Mockito.verify(mMockActivity, Mockito.never()).recreate();
    }

    @Test
    public void onAuthnResume() {
        BarcodeAcceptanceTypeContext.removeIonActivity(mMockActivity);
        mMockActivity.onAuthnResume();
        assertNotNull(BarcodeAcceptanceTypeContext.getIonActivity());
    }

    @Test
    public void onDestroy() {
        TestUtils.setVariable(mMockActivity, "mScanViewResponse", new ScanViewResponse());
        mMockActivity.getIntent().putExtra(ScanProcessor.INTENT_SCAN_EXTRA, "Test");
        mMockActivity.onDestroy();
        assertNull(mMockActivity.getIntent().getExtras().getSerializable(ScanProcessor.INTENT_SCAN_EXTRA));
        assertNull(TestUtils.getVariable(mMockActivity, "mScanViewResponse"));
        assertNull(TestUtils.getVariable(mMockActivity, "mViewHolder"));
        assertNull(TestUtils.getVariable(mMockActivity, "mDrawerToggle"));
    }

    @Test
    public void getScannedBarcode() {
        final ScanViewResponse scannedBarcode = new ScanViewResponse();
        TestUtils.setVariable(mMockActivity, "mScanViewResponse", scannedBarcode);
        assertEquals(scannedBarcode, mMockActivity.getScannedBarcode());
    }

    @Test
    public void clearScannedBarcode() {
        TestUtils.setVariable(mMockActivity, "mScanViewResponse", new ScanViewResponse());
        mMockActivity.clearScannedBarcode();
        assertNull(TestUtils.getVariable(mMockActivity, "mScanViewResponse"));
    }

    @Test
    public void onStart_noPatientSelected() {
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn(true).when(intent).getBooleanExtra(Mockito.anyString(), Mockito.anyBoolean());
        PatientContext.clearContext();
        Mockito.doReturn(intent).when(mMockActivity).getIntent();
        mMockActivity.onStart();
        Mockito.verify(mMockActivity, Mockito.never()).recreate();
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
        ActiveModuleManager.setActiveModule(MedsAdminActivity.class, 0, null);
        mMockActivity.onStart();
        Mockito.verify(mMockActivity, Mockito.never()).recreate();
    }

    @Test
    public void onStart_differentPatientInstance() {
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn(true).when(intent).getBooleanExtra(Mockito.anyString(), Mockito.anyBoolean());
        PatientContext.clearContext();
        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "test_encounter_id_" + System.currentTimeMillis());
        PatientContext.setHasRelationship(false);
        TestUtils.setVariable(mMockActivity, "mScanViewResponse", new ScanViewResponse());
        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", UUID.randomUUID());
        Mockito.doReturn(intent).when(mMockActivity).getIntent();
        ActiveModuleManager.setActiveModule(MedsAdminActivity.class, 0, null);
        mMockActivity.onStart();
        assertNull(TestUtils.getVariable(mMockActivity, "mScanViewResponse"));
        Mockito.verify(mMockActivity).recreate();
    }

    @Test
    public void setDrawerIndicatorEnable() {
        final ActionBarDrawerToggle toggle = Mockito.spy((ActionBarDrawerToggle) TestUtils.getVariable(mMockActivity, "mDrawerToggle"));
        TestUtils.setVariable(mMockActivity, "mDrawerToggle", toggle);
        mMockActivity.setDrawerIndicatorEnabled(true);
        Mockito.verify(toggle).setDrawerIndicatorEnabled(true);
    }

    @Test
    public void setDrawerIndicatorEnable_toggleNull() {
        TestUtils.setVariable(mMockActivity, "mDrawerToggle", null);
        mMockActivity.setDrawerIndicatorEnabled(true);
    }

    @Test
    public void getMainViewClass() {
        TestUtils.setVariable(mMockActivity, "mCurrentFragment", null);
        assertEquals(PPRFragment.class, mMockActivity.getMainViewClass());
        TestUtils.setVariable(mMockActivity, "mCurrentFragment", MedsActivityListFragment.class);
        assertEquals(MedsActivityListFragment.class, mMockActivity.getMainViewClass());
    }

    @Test
    public void setDrawerLockMode() {
        mMockActivity.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
        Mockito.verify(mLayoutView).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
    }

    @Test
    public void setDrawerLockMode_viewHolderNull() {
        TestUtils.setVariable(mMockActivity, "mViewHolder", null);
        mMockActivity.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
        Mockito.verify(mLayoutView, Mockito.never()).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
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
        mMockActivity.onLogout();
        Mockito.verify(mMockActivity).setResultAndFinish(ActivityUtils.RESULT_LOGOUT);
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
    public void onResume_RelationshipValid_firstVisit_MicroService() {
        final Map<String, Boolean> featureMap = new HashMap<>();
        featureMap.put("android/use_microservices", true);
        final AuthnResponse response = AuthnResponse.newAuthnResponse(null, null, featureMap, SessionCheckHandler.Status.unlocked.name());
        IonAuthnSessionUtils.setAuthnResponse(ApplicationProvider.getApplicationContext(), response);
        mockActivity();

        PatientContext.clearContext();
        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "test_encounter_id_" + System.currentTimeMillis());
        PatientContext.setHasRelationship(true);
        PatientContext.setContextId("context_id");
        PatientContext.setHasAccess(true);
        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", null);
        final ViewGroup viewGroup = Mockito.mock(ViewGroup.class);
        Mockito.doReturn(viewGroup).when(mMockActivity).findViewById(mViewToReplace);
        Mockito.doReturn(0).when(viewGroup).getChildCount();
        TestUtils.invokePrivateMethod(mMockActivity, "onResume");
        Mockito.verify(viewGroup).setVisibility(View.VISIBLE);
        Mockito.verify(mMockActivity).onFragmentSelected(MedsActivityListFragment.class, null);

        //Clear for other tests
        IonAuthnSessionUtils.setAuthnResponse(ApplicationProvider.getApplicationContext(), null);
    }

    @Test
    public void onResume_RelationshipValid_firstVisit() {
        PatientContext.clearContext();
        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "test_encounter_id_" + System.currentTimeMillis());
        PatientContext.setHasRelationship(true);
        PatientContext.setHasAccess(true);
        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", null);
        final ViewGroup viewGroup = Mockito.mock(ViewGroup.class);
        Mockito.doReturn(viewGroup).when(mMockActivity).findViewById(mViewToReplace);
        Mockito.doReturn(0).when(viewGroup).getChildCount();
        TestUtils.invokePrivateMethod(mMockActivity, "onResume");
        Mockito.verify(viewGroup).setVisibility(View.VISIBLE);
        Mockito.verify(mMockActivity).onFragmentSelected(com.cerner.cura.medications.legacy.ui.MedsTaskListFragment.class, null);
    }

    @Test
    public void onResume_RelationshipValid() {
        PatientContext.clearContext();
        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "test_encounter_id_" + System.currentTimeMillis());
        PatientContext.setHasRelationship(true);
        PatientContext.setHasAccess(true);
        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", PatientContext.getInstanceId());
        final ViewGroup viewGroup = Mockito.mock(ViewGroup.class);
        Mockito.doReturn(viewGroup).when(mMockActivity).findViewById(mViewToReplace);
        Mockito.doReturn(0).when(viewGroup).getChildCount();
        TestUtils.invokePrivateMethod(mMockActivity, "onResume");
        Mockito.verify(viewGroup).setVisibility(View.VISIBLE);
        Mockito.verify(mMockActivity).onFragmentSelected(com.cerner.cura.medications.legacy.ui.MedsTaskListFragment.class, null);
    }

    @Test
    public void onResume_samePatientInstanceId_viewNotFound() {
        PatientContext.clearContext();
        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "test_encounter_id_" + System.currentTimeMillis());
        PatientContext.setHasRelationship(true);
        PatientContext.setHasAccess(true);
        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", PatientContext.getInstanceId());
        Mockito.doReturn(null).when(mMockActivity).findViewById(mViewToReplace);
        TestUtils.invokePrivateMethod(mMockActivity, "onResume");
        Mockito.verify(mMockActivity, Mockito.never()).onFragmentSelected(MedsActivityListFragment.class, null);
        Mockito.verify(mMockActivity, Mockito.never()).onFragmentSelected(com.cerner.cura.medications.legacy.ui.MedsTaskListFragment.class, null);
    }

    @Test
    public void onResume_differentInstanceIds() {
        PatientContext.clearContext();
        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "test_encounter_id_" + System.currentTimeMillis());
        PatientContext.setHasRelationship(true);
        PatientContext.setHasAccess(true);
        TestUtils.setVariable(mMockActivity, "mScanViewResponse", new ScanViewResponse());
        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", UUID.randomUUID());
        TestUtils.invokePrivateMethod(mMockActivity, "onResume");
        assertNull(TestUtils.getVariable(mMockActivity, "mScanViewResponse"));
        Mockito.verify(mMockActivity, Mockito.never()).onFragmentSelected(MedsActivityListFragment.class, null);
        Mockito.verify(mMockActivity, Mockito.never()).onFragmentSelected(com.cerner.cura.medications.legacy.ui.MedsTaskListFragment.class, null);
    }

    @Test
    public void onResume_differentInstanceIds_initialLoad() {
        PatientContext.clearContext();
        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "test_encounter_id_" + System.currentTimeMillis());
        PatientContext.setHasRelationship(true);
        PatientContext.setHasAccess(true);
        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", null);
        TestUtils.invokePrivateMethod(mMockActivity, "onResume");
        Mockito.verify(mMockActivity).onFragmentSelected(com.cerner.cura.medications.legacy.ui.MedsTaskListFragment.class, null);
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
    public void getPreviousFragment_backFromAlertDetail_allIncognito() {
        TestUtils.setVariable(mMockActivity, "mCurrentFragment", MedsOrderDetailsFragment.class);
        final ArrayList<SerializablePair<Class<? extends Fragment>, Boolean>> fragmentBackStack = TestUtils
                .getVariable(mMockActivity, "mFragmentBackStack");
        fragmentBackStack.add(new SerializablePair<>(MedsAlertBaseFragment.class, true));
        fragmentBackStack.add(new SerializablePair<>(Fragment.class, true));
        assertEquals(MedsAlertBaseFragment.class, mMockActivity.getPreviousFragment());
    }

    @Test
    public void getPreviousFragment_notBackFromAlertDetail() {
        TestUtils.setVariable(mMockActivity, "mCurrentFragment", Fragment.class);
        final ArrayList<SerializablePair<Class<? extends Fragment>, Boolean>> fragmentBackStack = TestUtils
                .getVariable(mMockActivity, "mFragmentBackStack");
        fragmentBackStack.add(new SerializablePair<>(MedsAlertBaseFragment.class, true));
        fragmentBackStack.add(new SerializablePair<>(Fragment.class, true));
        assertNull(mMockActivity.getPreviousFragment());
    }

    @Test
    public void getPreviousFragment_backFromChartingDetail_allIncognito() {
        TestUtils.setVariable(mMockActivity, "mCurrentFragment", LastGivenDetailsFragment.class);
        final ArrayList<SerializablePair<Class<? extends Fragment>, Boolean>> fragmentBackStack = TestUtils
                .getVariable(mMockActivity, "mFragmentBackStack");
        fragmentBackStack.add(new SerializablePair<>(MedsChartingDetailsFragment.class, true));
        fragmentBackStack.add(new SerializablePair<>(Fragment.class, true));
        assertEquals(MedsChartingDetailsFragment.class, mMockActivity.getPreviousFragment());
    }

    @Test
    public void getPreviousFragment_notBackFromChartingDetail() {
        TestUtils.setVariable(mMockActivity, "mCurrentFragment", Fragment.class);
        final ArrayList<SerializablePair<Class<? extends Fragment>, Boolean>> fragmentBackStack = TestUtils
                .getVariable(mMockActivity, "mFragmentBackStack");
        fragmentBackStack.add(new SerializablePair<>(MedsChartingDetailsFragment.class, true));
        fragmentBackStack.add(new SerializablePair<>(Fragment.class, true));
        assertNull(mMockActivity.getPreviousFragment());
    }

    @Test
    public void getPreviousFragment_backFromTaskSelection_allIncognito() {
        TestUtils.setVariable(mMockActivity, "mCurrentFragment", MedsChartingDetailsFragment.class);
        final ArrayList<SerializablePair<Class<? extends Fragment>, Boolean>> fragmentBackStack = TestUtils
                .getVariable(mMockActivity, "mFragmentBackStack");
        fragmentBackStack.add(new SerializablePair<>(MedsTaskSelectionBaseFragment.class, true));
        fragmentBackStack.add(new SerializablePair<>(Fragment.class, true));
        assertEquals(MedsTaskSelectionBaseFragment.class, mMockActivity.getPreviousFragment());
    }

    @Test
    public void getPreviousFragment_notBackFromTaskSelection() {
        TestUtils.setVariable(mMockActivity, "mCurrentFragment", Fragment.class);
        final ArrayList<SerializablePair<Class<? extends Fragment>, Boolean>> fragmentBackStack = TestUtils
                .getVariable(mMockActivity, "mFragmentBackStack");
        fragmentBackStack.add(new SerializablePair<>(MedsTaskSelectionBaseFragment.class, true));
        fragmentBackStack.add(new SerializablePair<>(Fragment.class, true));
        assertNull(mMockActivity.getPreviousFragment());
    }

    @Test
    public void getPreviousFragment_notIncognito() {
        TestUtils.setVariable(mMockActivity, "mCurrentFragment", null);
        final ArrayList<SerializablePair<Class<? extends Fragment>, Boolean>> fragmentBackStack = TestUtils
                .getVariable(mMockActivity, "mFragmentBackStack");
        fragmentBackStack.add(new SerializablePair<>(MedsAlertOverdueFragment.class, false));
        fragmentBackStack.add(new SerializablePair<>(Fragment.class, false));
        assertEquals(Fragment.class, mMockActivity.getPreviousFragment());
    }

    @Test
    public void onFragmentSelected() {
        Mockito.doReturn(true).when(mMockActivity).processResponses();
        final FragmentManager fragmentManager = Mockito.spy(mMockActivity.getFragmentManager());
        Mockito.doReturn(fragmentManager).when(mMockActivity).getFragmentManager();

        final MedsAlertFragment currentFragment = Mockito.mock(MedsAlertFragment.class);
        Mockito.doReturn(currentFragment).when(fragmentManager).findFragmentById(mViewToReplace);
        Mockito.doReturn(Mockito.mock(FragmentTransaction.class)).when(fragmentManager).beginTransaction();
        final Bundle bundle = new Bundle();
        bundle.putString("CuraBundleParameter", "activityId");

        mMockActivity.onFragmentSelected(MedsOrderDetailsFragment.class, bundle, false, false);

        final ArrayList<SerializablePair<Class<? extends Fragment>, Boolean>> fragmentBackStack = TestUtils
                .getVariable(mMockActivity, "mFragmentBackStack");
        assertEquals(1, fragmentBackStack.size());
        assertEquals(currentFragment.getClass(), fragmentBackStack.get(0).first);
        assertEquals(MedsOrderDetailsFragment.class, TestUtils.getVariable(mMockActivity, "mCurrentFragment"));
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
        Mockito.when(toggle.onOptionsItemSelected(Mockito.any(MenuItem.class))).thenReturn(false);
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
    public void setDrawerIndicatorEnabled_drawerToggleNull() {
        TestUtils.setVariable(mMockActivity, "mDrawerToggle", null);
        mMockActivity.setDrawerIndicatorEnabled(true);
    }

    @Test
    public void onCreate_patientNotSelected() {
        PatientContext.clearContext();
        TestUtils.setVariable(mMockActivity, "mViewHolder", null);
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
        final Fragment fragment = Mockito.mock(Fragment.class);

        PatientContext.setHasRelationship(true);

        Mockito.doNothing().when(mMockActivity).setContentView(Mockito.anyInt());
        Mockito.doReturn(fragmentManager).when(mMockActivity).getFragmentManager();
        Mockito.doReturn(demographicsFragment).when(fragmentManager).findFragmentById(R.id.PatientDemographicsFragment);
        Mockito.doReturn(false).when(mMockActivity).requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        Mockito.doReturn(drawerFragment).when(fragmentManager).findFragmentById(R.id.drawer_fragment);
        Mockito.doReturn(fragment).when(fragmentManager).findFragmentById(mViewToReplace);
        Mockito.doNothing().when(mMockActivity).startActivity(Mockito.any(Intent.class));

        Mockito.doReturn(actionBar).when(mMockActivity).getActionBar();
        ActiveModuleManager.setActiveModule(Activity.class, 0, null);

        final ActionBarDrawerToggle toggle = TestUtils.getVariable(mMockActivity, "mDrawerToggle");
        TestUtils.setVariable(toggle, "this$0", mMockActivity);
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

        ActiveModuleManager.setActiveModule(DeviceAssociationActivity.class, 0);
        final ActionBarDrawerToggle toggle = TestUtils.getVariable(mMockActivity, "mDrawerToggle");
        TestUtils.setVariable(toggle, "this$0", mMockActivity);
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

        TestUtils.setVariable(mMockActivity, "mCanShowFragments", false);

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

        assertTrue(TestUtils.getVariable(mMockActivity, "mCanShowFragments"));
        Mockito.verify(demographicsFragment).setFragmentVisibility(true);
        Mockito.verify(fragment).setFragmentVisibility(true);
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
    public void onSaveInstanceState() {
        final Bundle b = new Bundle();

        TestUtils.setVariable(mMockActivity, "mPatientInstanceId", PatientContext.getInstanceId());
        TestUtils.invokePrivateMethod(mMockActivity, "onSaveInstanceState", new Class[]{Bundle.class}, b);
        assertEquals(PatientContext.getInstanceId().toString(), b.getString(PatientContext.PATIENT_INSTANCE_ID_STORAGEID));
    }

    @Test
    public void isPatientContextModule() {
        assertTrue(mMockActivity.isPatientContextModule());
    }

    @Test
    public void onScanReset() {
        final ScanProcessor scanProcessor = Mockito.mock(ScanProcessor.class);
        Mockito.doReturn(true).when(scanProcessor).isEnabled(Mockito.any(Context.class));
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_MEDICATION, scanProcessor);
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_PATIENT, scanProcessor);
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_DEVICE, scanProcessor);
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_PRSNL, scanProcessor);

        BarcodeAcceptanceTypeContext.setIonActivity(mMockActivity, ScanCategory.MAX_ALLOWED_SCAN_CATEGORIES);
        mMockActivity.onScanReset();
        assertFalse(BarcodeAcceptanceTypeContext.isProcessorScanningEnabled(ScanCategory.SCAN_CATEGORY_PATIENT));
        assertFalse(BarcodeAcceptanceTypeContext.isProcessorScanningEnabled(ScanCategory.SCAN_CATEGORY_MEDICATION));
        assertFalse(BarcodeAcceptanceTypeContext.isProcessorScanningEnabled(ScanCategory.SCAN_CATEGORY_PRSNL));
        assertFalse(BarcodeAcceptanceTypeContext.isProcessorScanningEnabled(ScanCategory.SCAN_CATEGORY_DEVICE));
    }
}
