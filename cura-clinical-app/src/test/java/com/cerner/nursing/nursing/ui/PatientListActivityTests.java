package com.cerner.nursing.nursing.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.cerner.cura.base.PatientContext;
import com.cerner.cura.datamodel.common.PatientListPatient;
import com.cerner.cura.scanning.BarcodeAcceptanceTypeContext;
import com.cerner.cura.scanning.ScanCategory;
import com.cerner.cura.scanning.ScanManager;
import com.cerner.cura.scanning.ScanProcessor;
import com.cerner.cura.test.TestActivity;
import com.cerner.cura.test.helper.TestUtils;
import com.cerner.cura.utils.ActiveModuleManager;
import com.cerner.cura.utils.ActivityUtils;
import com.cerner.ion.security.DialogController;
import com.cerner.ion.session.AuthnResponse;
import com.cerner.ion.session.Capabilities;
import com.cerner.ion.session.IonAuthnSessionUtils;
import com.cerner.ion.session.SessionCheckHandler;
import com.cerner.nursing.nursing.R;
import com.cerner.nursing.nursing.utils.AppSwitchUtils;

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

import java.util.Collections;
import java.util.HashMap;

import androidx.test.core.app.ApplicationProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link PatientListActivity}.
 *
 * @author Lam Tran (LT028056)
 * @author Brad Barnhill (bb024928)
 */
@RunWith (RobolectricTestRunner.class)
public class PatientListActivityTests {
    private ActivityController<PatientListActivity> mActivityController;
    private PatientListActivity mMockActivity;
    private DrawerLayout mLayoutView;
    private static final int mViewToReplace = R.id.patient_list_fragment;

    @BeforeClass
    public static void ClassSetup() {
        System.gc();
    }

    /**
     * Create mock activity from robolectric
     */
    @Before
    public void setup() {
        IonAuthnSessionUtils.setAuthnResponse(ApplicationProvider.getApplicationContext(), null);
        IonAuthnSessionUtils.setTenant(null);

        PatientContext.clearContext();
        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "test_encounter_id_" + System.currentTimeMillis());
        PatientContext.setHasRelationship(true);
        Shadows.shadowOf(ApplicationProvider.getApplicationContext().getPackageManager()).addResolveInfoForIntent(new Intent("com.cerner.scanning.service.BIND"), Mockito.mock(ResolveInfo.class));
        mActivityController = Robolectric.buildActivity(PatientListActivity.class).create().postCreate(null);
        mMockActivity = Mockito.spy(mActivityController.get());

        mLayoutView = Mockito.spy(new DrawerLayout(ApplicationProvider.getApplicationContext()));
        Mockito.doNothing().when(mLayoutView).closeDrawer(Mockito.any(View.class));
        Mockito.doReturn(mLayoutView).when(mMockActivity).findViewById(R.id.drawer_patientlist_layout);
        final LinearLayout listView = Mockito.spy(new LinearLayout(ApplicationProvider.getApplicationContext()));
        Mockito.doReturn(listView).when(mLayoutView).findViewById(R.id.drawer_fragment);
        final PatientListActivity.ViewHolder viewHolder = new PatientListActivity.ViewHolder(mMockActivity);
        TestUtils.setVariable(mMockActivity, "mViewHolder", viewHolder);

        final DialogController dialogs = Mockito.spy((DialogController) TestUtils.getVariable(mMockActivity, "dialogs"));
        TestUtils.setVariable(mMockActivity, "dialogs", dialogs);
        Mockito.doReturn(null).when(dialogs).showProgressDialog();
        Mockito.doNothing().when(dialogs).hideProgressDialog();
        Mockito.doReturn(null).when(dialogs).showError(Mockito.anyString(),
                                                       Mockito.anyString(),
                                                       Mockito.anyString(),
                                                       Mockito.any(DialogInterface.OnClickListener.class),
                                                       Mockito.isNull(),
                                                       Mockito.isNull(),
                                                       Mockito.isNull(),
                                                       Mockito.isNull());
    }

    @After
    public void tearDown() {
        mMockActivity = null;
        mActivityController = null;
    }

    @Test
    public void onConfigurationChanged() {
        final ActionBarDrawerToggle drawerToggle = Mockito.spy((ActionBarDrawerToggle) TestUtils.getVariable(mMockActivity, "mDrawerToggle"));
        TestUtils.setVariable(mMockActivity, "mDrawerToggle", drawerToggle);
        mMockActivity.onConfigurationChanged(null);
        Mockito.verify(drawerToggle).onConfigurationChanged(Mockito.isNull());
    }

    @Test
    public void activityIsNotNull() {
        assertNotNull(mMockActivity);
    }

    @Test
    public void checkLayoutElements() {
        // test if the fragment is on the activity
        assertNotNull(mMockActivity.getFragmentManager().findFragmentById(mViewToReplace));
        assertNotNull(mMockActivity.findViewById(mViewToReplace));
        assertEquals(View.VISIBLE, mMockActivity.findViewById(mViewToReplace).getVisibility());
        assertNotNull(mMockActivity.findViewById(R.id.drawer_patientlist_layout));
        assertEquals(View.VISIBLE, mMockActivity.findViewById(R.id.drawer_patientlist_layout).getVisibility());
    }

    @Test
    public void onStart_notAppSwitch() {
        TestUtils.invokePrivateMethod(mMockActivity, "onStart");
        Mockito.verify(mMockActivity).onScanReset();
        Mockito.verify(mMockActivity, Mockito.never()).finish();
    }

    @Test
    public void onStart_notAppSwitch_first_nullFragment() {
        final FragmentManager manager = Mockito.mock(FragmentManager.class);
        Mockito.doReturn(null).when(manager).findFragmentById(Mockito.anyInt());
        Mockito.doReturn(manager).when(mMockActivity).getFragmentManager();

        TestUtils.invokePrivateMethod(mMockActivity, "onStart");
        Mockito.verify(mMockActivity, Mockito.never()).finish();
    }

    @Test
    public void onStart_notAppSwitch_first_nullViewFragment() {
        final FragmentManager manager = Mockito.mock(FragmentManager.class);
        final Fragment fragment = Mockito.mock(Fragment.class);
        Mockito.doReturn(null).when(fragment).getView();
        Mockito.doReturn(fragment).when(manager).findFragmentById(Mockito.anyInt());
        Mockito.doReturn(manager).when(mMockActivity).getFragmentManager();

        TestUtils.invokePrivateMethod(mMockActivity, "onStart");
        Mockito.verify(mMockActivity, Mockito.never()).finish();
    }

    @Test
    public void onStart_notAppSwitch_first() {
        final FragmentManager manager = Mockito.mock(FragmentManager.class);
        final Fragment fragment = Mockito.mock(Fragment.class);
        final View view = Mockito.mock(View.class);
        Mockito.doReturn(view).when(fragment).getView();
        Mockito.doReturn(fragment).when(manager).findFragmentById(Mockito.anyInt());
        Mockito.doReturn(manager).when(mMockActivity).getFragmentManager();

        TestUtils.invokePrivateMethod(mMockActivity, "onStart");
        Mockito.verify(mMockActivity, Mockito.never()).finish();
    }

    @Test
    public void onCuraActivityResult_appSwitch() {
        final DialogController dialogs = Mockito.mock(DialogController.class);
        TestUtils.setVariable(mMockActivity, "dialogs", dialogs);
        mMockActivity.onCuraActivityResult(0, ActivityUtils.RESULT_APP_SWITCH, null);
        Mockito.verify(dialogs, Mockito.never()).showError(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(DialogInterface.OnClickListener.class),
                                                           Mockito.anyString(), Mockito.any(DialogInterface.OnClickListener.class), Mockito.anyString(),
                                                           Mockito.any(DialogInterface.OnClickListener.class));
    }

    @Test
    public void onCuraActivityResult_notAppSwitch_right_patientChartRequestCode() {
        final DialogController dialogs = Mockito.mock(DialogController.class);
        TestUtils.setVariable(mMockActivity, "dialogs", dialogs);
        mMockActivity.onCuraActivityResult(PatientListActivity.REQUESTCODE_PATIENTCHART_DEFAULT, 100, null);
        Mockito.verify(dialogs, Mockito.never()).showError(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(DialogInterface.OnClickListener.class),
                                                           Mockito.anyString(), Mockito.any(DialogInterface.OnClickListener.class), Mockito.anyString(),
                                                           Mockito.any(DialogInterface.OnClickListener.class));
    }

    @Test
    public void onCuraActivityResult_notAppSwitch_right_chartingRequestCode() {
        final DialogController dialogs = Mockito.mock(DialogController.class);
        TestUtils.setVariable(mMockActivity, "dialogs", dialogs);
        mMockActivity.onCuraActivityResult(PatientListActivity.REQUESTCODE_CHARTING_DEFAULT, 100, null);
        Mockito.verify(dialogs, Mockito.never()).showError(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(DialogInterface.OnClickListener.class),
                Mockito.anyString(), Mockito.any(DialogInterface.OnClickListener.class), Mockito.anyString(),
                Mockito.any(DialogInterface.OnClickListener.class));
    }

    @Test
    public void onCuraActivityResult_notAppSwitch_right_careTeamRequestCode() {
        final DialogController dialogs = Mockito.mock(DialogController.class);
        TestUtils.setVariable(mMockActivity, "dialogs", dialogs);
        mMockActivity.onCuraActivityResult(PatientListActivity.REQUESTCODE_CARETEAM_DEFAULT, 100, null);
        Mockito.verify(dialogs, Mockito.never()).showError(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(DialogInterface.OnClickListener.class),
                                                           Mockito.anyString(), Mockito.any(DialogInterface.OnClickListener.class), Mockito.anyString(),
                                                           Mockito.any(DialogInterface.OnClickListener.class));
    }

    @Test
    public void onCuraActivityResult_notAppSwitch_right_deviceAssociationRequestCode() {
        final DialogController dialogs = Mockito.mock(DialogController.class);
        TestUtils.setVariable(mMockActivity, "dialogs", dialogs);
        mMockActivity.onCuraActivityResult(PatientListActivity.REQUESTCODE_DEVICEASSOCIATION_DEFAULT, 100, null);
        Mockito.verify(dialogs, Mockito.never()).showError(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(DialogInterface.OnClickListener.class),
                                                           Mockito.anyString(), Mockito.any(DialogInterface.OnClickListener.class), Mockito.anyString(),
                                                           Mockito.any(DialogInterface.OnClickListener.class));
    }

    @Test
    public void onCuraActivityResult_intentNotNull_false() {
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn(false).when(intent).getBooleanExtra(Mockito.anyString(), Mockito.anyBoolean());
        mMockActivity.onCuraActivityResult(0, ActivityUtils.RESULT_NOT_ENOUGH_IDENTIFIERS, intent);
    }

    @Test
    public void onCuraActivityResult_intentNotNull_true() {
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn(true).when(intent).getBooleanExtra(Mockito.anyString(), Mockito.anyBoolean());
        mMockActivity.onCuraActivityResult(0, ActivityUtils.RESULT_NOT_ENOUGH_IDENTIFIERS, intent);
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
    public void onPatientSelected_patientNull() {
        mMockActivity.onPatientSelected(null);
    }

    @Test
    public void onPatientSelected_patientNotNull() {
        final PatientListPatient patient = new PatientListPatient();
        patient.personId = PatientContext.getPatientId();
        patient.encounterId = PatientContext.getEncounterId();
        mMockActivity.onPatientSelected(patient);
    }

    @Test
    public void onPatientSelected_hasPatientSet() {
        PatientContext.clearContext();

        final PatientListPatient patient = new PatientListPatient();
        patient.encounterId = "encounter";
        patient.personId = "patient";
        mMockActivity.onPatientSelected(patient);

        PatientContext.setPatientAndEncounter("patient1", "encounter1");
        mMockActivity.onPatientSelected(patient);

        patient.relationshipInd = true;
        mMockActivity.onPatientSelected(patient);

        PatientContext.setPatientAndEncounter("patient", "encounter");
        mMockActivity.onPatientSelected(patient);

        PatientContext.setLatestModule(Activity.class, 2);
        mMockActivity.onPatientSelected(patient);

        PatientContext.setLatestModule(null, Integer.MIN_VALUE);
        mMockActivity.onPatientSelected(patient);
    }

    @Test
    public void onPatientSelected_hasPatientSetWithFlagOn() {
        PatientContext.clearContext();
        enableMicroservices();

        final PatientListPatient patient = new PatientListPatient();
        patient.encounterId = "encounter";
        patient.personId = "patient";
        patient.relationshipInd = true;
        PatientContext.setPatientAndEncounter("patient", "encounter");
        mMockActivity.onPatientSelected(patient);
    }

    @Test
    public void canAllFragmentsShow() {
        assertTrue(mMockActivity.canAllFragmentsShow());
    }

    @Test
    public void onBackPressed_drawerClosed() {
        Mockito.doReturn(false).when(mMockActivity).isDrawerOpen();
        mMockActivity.onBackPressed();
        Mockito.verify(mMockActivity, Mockito.never()).closeDrawer();
    }

    @Test
    public void onBackPressed_drawerOpen() {
        Mockito.doReturn(true).when(mMockActivity).isDrawerOpen();
        mMockActivity.onBackPressed();
        Mockito.verify(mMockActivity).closeDrawer();
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
    public void onCreate_openDrawer() {
        final ActionBarDrawerToggle drawerToggle = Mockito.spy((ActionBarDrawerToggle) TestUtils.getVariable(mMockActivity, "mDrawerToggle"));
        TestUtils.setVariable(mMockActivity, "mDrawerToggle", drawerToggle);
        TestUtils.setVariable(drawerToggle, "this$0", mMockActivity);

        drawerToggle.onDrawerOpened(Mockito.mock(View.class));
        Mockito.verify(drawerToggle).onDrawerOpened(Mockito.any(View.class));
        Mockito.verify(mMockActivity).invalidateOptionsMenu();

        final ActionBar actionBar = Mockito.mock(ActionBar.class);
        Mockito.doReturn(null).when(actionBar).getTitle();
        Mockito.doReturn(actionBar).when(mMockActivity).getSupportActionBar();
        drawerToggle.onDrawerOpened(Mockito.mock(View.class));

        Mockito.doReturn(null).when(mMockActivity).getSupportActionBar();
        drawerToggle.onDrawerOpened(Mockito.mock(View.class));
    }

    @Test
    public void onCreate_closeDrawer() {
        final ActionBarDrawerToggle toggle = TestUtils.getVariable(mMockActivity, "mDrawerToggle");
        TestUtils.setVariable(toggle, "this$0", mMockActivity);
        ActiveModuleManager.setActiveModule(PatientListActivity.class, 0, null);
        toggle.onDrawerClosed(null);

        Mockito.verify(mMockActivity).invalidateOptionsMenu();
        Mockito.verify(mMockActivity, Mockito.never()).startActivity(Mockito.any(Intent.class));
    }

    @Test
    public void onDestroy() {
        mMockActivity.onDestroy();
        assertNull(TestUtils.getVariable(mMockActivity, "mViewHolder"));
        assertNull(TestUtils.getVariable(mMockActivity, "mDrawerToggle"));
    }

    @Test (expected = IllegalArgumentException.class)
    public void viewHolder_nullRoot() {
        new PatientListActivity.ViewHolder(null);
    }

    @Test (expected = NullPointerException.class)
    public void viewHolder_nullEverything() {
        Mockito.doReturn(null).when(mMockActivity).findViewById(R.id.drawer_patientlist_layout);
        new PatientListActivity.ViewHolder(mMockActivity);
    }

    @Test (expected = NullPointerException.class)
    public void viewHolder_nullNot1() {
        Mockito.doReturn(null).when(mLayoutView).findViewById(R.id.drawer_fragment);
        new PatientListActivity.ViewHolder(mMockActivity);
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
        TestUtils.setStaticVariable(AppSwitchUtils.class, "smStoredIntent", null);
        ActiveModuleManager.clearActiveModule();
        BarcodeAcceptanceTypeContext.removeIonActivity(mMockActivity);
        final View view = Mockito.mock(View.class);
        Mockito.doReturn(view).when(mMockActivity).findViewById(mViewToReplace);
        mMockActivity.onAuthnResume();
        Mockito.verify(view).setVisibility(View.VISIBLE);
        assertNotNull(BarcodeAcceptanceTypeContext.getIonActivity());
        assertFalse(TestUtils.getVariable(mMockActivity, "mSetActiveModuleOnResume"));
    }

    @Test
    public void onAuthnResume_viewNull() {
        TestUtils.setStaticVariable(AppSwitchUtils.class, "smStoredIntent", null);
        BarcodeAcceptanceTypeContext.removeIonActivity(mMockActivity);
        Mockito.doReturn(null).when(mMockActivity).findViewById(mViewToReplace);
        mMockActivity.onAuthnResume();
        assertNotNull(BarcodeAcceptanceTypeContext.getIonActivity());
    }

    @Test
    public void onAuthnResume_activeModuleNotCurrentActivity() {
        TestUtils.setStaticVariable(AppSwitchUtils.class, "smStoredIntent", null);
        Mockito.doNothing().when(mMockActivity).startActivity(Mockito.any(Intent.class));
        ActiveModuleManager.setActiveModule(TestActivity.class, 0, null);
        BarcodeAcceptanceTypeContext.removeIonActivity(mMockActivity);
        Mockito.doNothing().when(mMockActivity).startActivity(Mockito.any(Intent.class));
        mMockActivity.onAuthnResume();
        Mockito.verify(mMockActivity).startActivity(Mockito.any(Intent.class));
    }

    @Test
    public void onPause() {
        mMockActivity.onPause();
        assertNotNull(BarcodeAcceptanceTypeContext.getSavedClass());
    }

    @Test
    public void onStop() {
        BarcodeAcceptanceTypeContext.setIonActivity(mMockActivity, ScanCategory.SCAN_CATEGORY_DEVICE | ScanCategory.SCAN_CATEGORY_PATIENT);
        mMockActivity.onStop();
        assertNull(BarcodeAcceptanceTypeContext.getIonActivity());
    }

    @Test
    public void getMainViewClass() {
        assertEquals(PatientListFragment.class, mMockActivity.getMainViewClass());
    }

    @Test
    public void onResponseReceived() {
        mMockActivity.onResponseReceived(Class.class);
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

    private static void enableMicroservices() {
        final HashMap<String, Boolean> map = new HashMap<>();
        map.put("android/use_microservices", true);
        final Capabilities capabilities = Capabilities.newCapabilities(Collections.singletonList("com.cerner.nursing.cura.dev"), 0, 0, 0, "OxMOCKTENANTGUIDTHATM-EANSNOTHIN", false, true);
        final AuthnResponse response = AuthnResponse.newAuthnResponse(null, capabilities, map, SessionCheckHandler.Status.unlocked.name());
        IonAuthnSessionUtils.setAuthnResponse(ApplicationProvider.getApplicationContext(), response);
    }
}