package com.cerner.nursing.nursing.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;

import com.cerner.cura.base.PatientContext;
import com.cerner.cura.requestor.MockDataManager;
import com.cerner.cura.scanning.BarcodeAcceptanceTypeContext;
import com.cerner.cura.scanning.ScanCategory;
import com.cerner.cura.scanning.ScanManager;
import com.cerner.cura.scanning.ScanProcessor;
import com.cerner.cura.test.helper.TestUtils;
import com.cerner.cura.utils.ActivityUtils;
import com.cerner.cura.utils.SerializablePair;
import com.cerner.ion.session.IonSessionUtils;
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

import androidx.test.core.app.ApplicationProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;

/**
 * Tests {@link com.cerner.nursing.nursing.ui.SettingsActivity}.
 *
 * @author Brad Barnhill (bb024928)
 */
@RunWith (RobolectricTestRunner.class)
public class SettingsActivityTests {
    private ActivityController<SettingsActivity> mActivityController;
    private SettingsActivity mockActivity;

    @BeforeClass
    public static void ClassSetup() {
        System.gc();
    }

    @Before
    public void setup() {
        TestUtils.setStaticVariable(MockDataManager.class, "smState", MockDataManager.MockState.READ);
        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "test_encounter_id_" + System.currentTimeMillis());
        PatientContext.setHasRelationship(true);
        Shadows.shadowOf(ApplicationProvider.getApplicationContext().getPackageManager()).addResolveInfoForIntent(new Intent("com.cerner.scanning.service.BIND"), Mockito.mock(ResolveInfo.class));
        final Intent intent = new Intent();
        intent.putExtra(SettingsActivity.LAUNCH_ACTION_IDENTIFIER, SettingsActivity.LAUNCH_DEFAULT);
        mActivityController = Robolectric.buildActivity(SettingsActivity.class, intent).create().start();
        mockActivity = Mockito.spy(mActivityController.get());
        Mockito.doReturn(true).when(mockActivity).processResponses();
    }

    @After
    public void tearDown() {
        mockActivity = null;
        mActivityController = null;
    }

    @Test
    public void onOrgSelected_pickOrgRequestCode() {
        TestUtils.setVariable(mockActivity, "mLoadAction", SettingsActivity.LAUNCH_PICK_ORG);
        mockActivity.onOrgSelected();
        Mockito.verify(mockActivity).setResultAndFinish(Activity.RESULT_OK);
    }

    @Test
    public void onOrgSelected_notPickOrgRequestCode() {
        TestUtils.setVariable(mockActivity, "mLoadAction", SettingsActivity.LAUNCH_DEFAULT);
        mockActivity.onOrgSelected();
        Mockito.verify(mockActivity, Mockito.never()).setResultAndFinish(Mockito.anyInt());
        Mockito.verify(mockActivity).onFragmentSelected(UserSummaryFragment.class, null);
    }

    @Test
    public void onOptionsItemSelected_homeSelected() {
        final MenuItem item = Mockito.mock(MenuItem.class);
        Mockito.doReturn(android.R.id.home).when(item).getItemId();
        assertTrue(mockActivity.onOptionsItemSelected(item));
        Mockito.verify(mockActivity).onBackPressed();
    }

    @Test
    public void onOptionsItemSelected_homeNotSelected() {
        final MenuItem item = Mockito.mock(MenuItem.class);
        Mockito.doReturn(android.R.id.home + 1).when(item).getItemId();
        assertFalse(mockActivity.onOptionsItemSelected(item));
        Mockito.verify(mockActivity, Mockito.never()).onBackPressed();
    }

    @Test
    public void onAuthnResume() {
        BarcodeAcceptanceTypeContext.removeIonActivity(mockActivity);
        mockActivity.onAuthnResume();
        assertNotNull(BarcodeAcceptanceTypeContext.getIonActivity());
    }

    @Test
    public void onPause() {
        Mockito.doReturn(true).when(mockActivity).processResponses();
        mockActivity.onPause();
        assertNotNull(BarcodeAcceptanceTypeContext.getSavedClass());
    }

    @Test
    public void onStop() {
        BarcodeAcceptanceTypeContext.setIonActivity(mockActivity, ScanCategory.SCAN_CATEGORY_DEVICE | ScanCategory.SCAN_CATEGORY_PATIENT);
        mockActivity.onStop();
        assertNull(BarcodeAcceptanceTypeContext.getIonActivity());
    }

    @Test
    public void getMainViewClass() {
        assertEquals(UserSummaryFragment.class, mockActivity.getMainViewClass());
    }

    @Test
    public void onFragmentSelected() {
        Mockito.doReturn(true).when(mockActivity).processResponses();
        final FragmentManager fragmentManager = Mockito.spy(mockActivity.getFragmentManager());
        Mockito.doReturn(fragmentManager).when(mockActivity).getFragmentManager();

        Mockito.doReturn(new UserSummaryFragment()).when(fragmentManager).findFragmentById(R.id.fragment_container);

        mockActivity.onFragmentSelected(OrgSelectionFragment.class, null, false, false);

        final ArrayList<SerializablePair<Class<? extends Fragment>, Boolean>> fragmentBackStack = TestUtils
                .getVariable(mockActivity, "mFragmentBackStack");
        assertEquals(1, fragmentBackStack.size());
        assertEquals(UserSummaryFragment.class, fragmentBackStack.get(0).first);
        assertEquals(OrgSelectionFragment.class, TestUtils.getVariable(mockActivity, "mCurrentFragment"));
    }

    @Test
    public void onBackPressed_notifyFragmentOnbackPressSet() {
        Mockito.doReturn(true).when(mockActivity).notifyFragmentsOnBackPressed();
        mockActivity.onBackPressed();
        Mockito.verify(mockActivity, Mockito.never()).setResult(Mockito.anyInt());
    }

    @Test
    public void onBackPressed_loadActionPickOrg() {
        IonSessionUtils.setLoggingOutToken("test");
        mockActivity = Mockito.spy(mockActivity);
        TestUtils.setVariable(mockActivity, "mLoadAction", SettingsActivity.LAUNCH_PICK_ORG);
        Mockito.doReturn(true).when(mockActivity).notifyFragmentsOnBackPressed();
        mockActivity.onBackPressed();
        Mockito.verify(mockActivity).setResultAndFinish(ActivityUtils.RESULT_LOGOUT);
    }

    @Test
    public void onBackPressed_backStackIsNotEmpty() {
        final ArrayList<SerializablePair<Class<? extends Fragment>, Boolean>> backStack = TestUtils
                .getVariable(mockActivity, "mFragmentBackStack");
        backStack.add(new SerializablePair<>(PatientSummaryFragment.class, false));

        mockActivity.onBackPressed();

        Mockito.verify(mockActivity, Mockito.never()).setResultAndFinish(Activity.RESULT_OK);
    }

    @Test
    public void onBackPressed_backStackIsEmpty() {
        mockActivity.onBackPressed();

        Mockito.verify(mockActivity).setResultAndFinish(Activity.RESULT_OK);
    }

    @Test
    public void init_fragmentIsNull_saveInstanceBundleNull() {
        Mockito.doReturn(false).when(mockActivity).requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        Mockito.doReturn(null).when(mockActivity).findViewById(Mockito.anyInt());
        Mockito.doNothing().when(mockActivity).setContentView(Mockito.anyInt());

        final Bundle bundle = null;
        TestUtils.invokePrivateMethod(mockActivity, "init", new Class[]{Bundle.class}, bundle);

        Mockito.verify(mockActivity, Mockito.never()).onFragmentSelected(Mockito.any(Class.class), Mockito.any(Bundle.class));
    }

    @Test
    public void init_fragmentIsNull_saveInstanceBundleIsNotNull() {
        Mockito.doReturn(false).when(mockActivity).requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        Mockito.doReturn(null).when(mockActivity).findViewById(Mockito.anyInt());
        Mockito.doNothing().when(mockActivity).setContentView(Mockito.anyInt());

        TestUtils.invokePrivateMethod(mockActivity, "init", new Class[] {Bundle.class} ,new Bundle());

        Mockito.verify(mockActivity, Mockito.never()).onFragmentSelected(Mockito.any(Class.class), Mockito.any(Bundle.class));
    }

    @Test
    public void onResponseReceived() {
        mockActivity.onResponseReceived(Class.class);
    }

    @Test
    public void onLogout() {
        mockActivity.onLogout();
        Mockito.verify(mockActivity).setResultAndFinish(ActivityUtils.RESULT_LOGOUT);
    }

    @Test
    public void onScanReset() {
        final ScanProcessor scanProcessor = Mockito.mock(ScanProcessor.class);
        Mockito.doReturn(true).when(scanProcessor).isEnabled(Mockito.any(Context.class));
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_MEDICATION, scanProcessor);
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_PATIENT, scanProcessor);
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_DEVICE, scanProcessor);
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_PRSNL, scanProcessor);

        BarcodeAcceptanceTypeContext.setIonActivity(mockActivity, ScanCategory.MAX_ALLOWED_SCAN_CATEGORIES);
        mockActivity.onScanReset();
        assertFalse(BarcodeAcceptanceTypeContext.isProcessorScanningEnabled(ScanCategory.SCAN_CATEGORY_PATIENT));
        assertFalse(BarcodeAcceptanceTypeContext.isProcessorScanningEnabled(ScanCategory.SCAN_CATEGORY_MEDICATION));
        assertFalse(BarcodeAcceptanceTypeContext.isProcessorScanningEnabled(ScanCategory.SCAN_CATEGORY_PRSNL));
        assertFalse(BarcodeAcceptanceTypeContext.isProcessorScanningEnabled(ScanCategory.SCAN_CATEGORY_DEVICE));
    }

    @Test
    public void onPostCreate() {
        final ActionBar bar = Mockito.mock(ActionBar.class);

        Mockito.doReturn(bar).when(mockActivity).getSupportActionBar();
        Mockito.doNothing().when(bar).setDisplayHomeAsUpEnabled(true);
        Mockito.doNothing().when(mockActivity).setContentView(Mockito.anyInt());
        mockActivity.onPostCreate(null, null);

        Mockito.verify(bar).setDisplayHomeAsUpEnabled(true);
    }

    @Test
    public void onStart() {
        mockActivity.onStart();
        Mockito.verify(mockActivity).onScanReset();
    }

    @Test
    public void onAuthnStart_OrgSelectionFragment() {
        final Intent intent = new Intent();
        intent.putExtra(SettingsActivity.LAUNCH_ACTION_IDENTIFIER, SettingsActivity.LAUNCH_PICK_ORG);
        mockActivity = Mockito.spy(Robolectric.buildActivity(SettingsActivity.class, intent).create().get());
        Mockito.doNothing().when(mockActivity).setContentView(Mockito.anyInt());

        final ViewGroup viewGroup = Mockito.mock(ViewGroup.class);
        Mockito.doReturn(viewGroup).when(mockActivity).findViewById(R.id.fragment_container);
        Mockito.doReturn(0).when(viewGroup).getChildCount();

        final Bundle bundle = null;
        TestUtils.invokePrivateMethod(mockActivity, "init", new Class[]{Bundle.class}, bundle);

        mockActivity.onAuthnStart();

        Mockito.verify(mockActivity).onFragmentSelected(eq(OrgSelectionFragment.class), Mockito.any(Bundle.class));
    }

    @Test
    public void onAuthnStart_UserSummaryFragment() {
        final Intent intent = new Intent();
        intent.putExtra(SettingsActivity.LAUNCH_ACTION_IDENTIFIER, SettingsActivity.LAUNCH_DEFAULT);
        mockActivity = Mockito.spy(Robolectric.buildActivity(SettingsActivity.class, intent).create().get());
        Mockito.doNothing().when(mockActivity).setContentView(Mockito.anyInt());

        final ViewGroup viewGroup = Mockito.mock(ViewGroup.class);
        Mockito.doReturn(viewGroup).when(mockActivity).findViewById(R.id.fragment_container);
        Mockito.doReturn(0).when(viewGroup).getChildCount();

        final Bundle bundle = null;
        TestUtils.invokePrivateMethod(mockActivity, "init", new Class[]{Bundle.class}, bundle);

        mockActivity.onAuthnStart();

        Mockito.verify(mockActivity).onFragmentSelected(UserSummaryFragment.class, null);
    }

    @Test
    public void canAllFragmentsShow() {
        assertTrue(mockActivity.canAllFragmentsShow());
    }
}