package com.cerner.nursing.nursing.base;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Pair;

import com.cerner.cura.base.PatientContext;
import com.cerner.cura.charting.navigator.utils.Utils;
import com.cerner.cura.demographics.datamodel.PatientDemogBanner;
import com.cerner.cura.medications.scanning.CuraMedicationScannedProcessor;
import com.cerner.cura.requestor.IDataRetriever;
import com.cerner.cura.requestor.MockDataManager;
import com.cerner.cura.test.helper.TestUtils;
import com.cerner.cura.ui.elements.DrawerPatientListItem;
import com.cerner.cura.ui.elements.IListItem;
import com.cerner.cura.ui.elements.drawer.DrawerListItem;
import com.cerner.cura.utils.ActiveModuleManager;
import com.cerner.ion.request.CernResponse;
import com.cerner.ion.security.DialogController;
import com.cerner.ion.security.IonActivity;
import com.cerner.ion.session.AuthnResponse;
import com.cerner.ion.session.Capabilities;
import com.cerner.ion.session.IonAuthnSessionUtils;
import com.cerner.ion.session.SessionCheckHandler;
import com.cerner.nursing.collections.android.scanning.SpecimenCollectionsScannedProcessor;
import com.cerner.nursing.nursing.ui.CareTeamActivity;
import com.cerner.nursing.nursing.ui.ChartingNavigatorActivity;
import com.cerner.nursing.nursing.ui.DeviceAssociationActivity;
import com.cerner.nursing.nursing.ui.MedsAdminActivity;
import com.cerner.nursing.nursing.ui.PatientChartActivity;
import com.cerner.nursing.nursing.ui.PatientListActivity;
import com.cerner.nursing.nursing.ui.SettingsActivity;
import com.cerner.nursing.nursing.ui.SpecimenCollectActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import androidx.test.core.app.ApplicationProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Lear (ML015922)
 */
@RunWith (RobolectricTestRunner.class)
public class NursingDrawerManagerTests {
    private static final String FEATURE_USE_MICROSERVICES = "android/use_microservices";
    private List<IListItem> mDrawerItems;
    private String mPatientId;
    private String mEncounterId;
    private AuthnResponse mResponseCopy;
    private ArrayList<ActiveModuleManager.ActiveModuleListener> mListeners;

    @BeforeClass
    public static void ClassSetup() {
        System.gc();
    }

    @Before
    public void setup() {
        TestUtils.setStaticVariable(MockDataManager.class, "smState", MockDataManager.MockState.READ);
        PatientContext.clearContext();
        mPatientId = "test_patient_id_" + System.currentTimeMillis();
        mEncounterId = "test_encounter_id_" + System.currentTimeMillis();
        PatientContext.setPatientAndEncounter(mPatientId, mEncounterId);
        PatientContext.setHasRelationship(true);
        mListeners = TestUtils.getStaticVariable(ActiveModuleManager.class, "smActiveModuleListeners");
        mListeners.clear();
        setupCapabilitiesAndFeatures();
        mDrawerItems = NursingDrawerManager.getDrawerItems(ApplicationProvider.getApplicationContext());
    }

    private void setupCapabilitiesAndFeatures() {
        final HashMap<String, Boolean> map = new HashMap<>();
        map.put("android/use_spp", true);
        map.put(SpecimenCollectionsScannedProcessor.FEATURE_COLLECTIONS, true);
        map.put(FEATURE_USE_MICROSERVICES, true);
        map.put(TestUtils.getStaticVariable(NursingDrawerManager.class, "FEATURE_CARE_TEAM"), true);
        map.put(TestUtils.getStaticVariable(Utils.class, "FEATURE_CHARTING_NAVIGATOR"), true);
        final Capabilities capabilities = Capabilities.newCapabilities(Collections.singletonList("com.cerner.nursing.cura.dev"), 0, 0, 0, "OxMOCKTENANTGUIDTHATM-EANSNOTHIN", false, true);
        capabilities.put("device_association", true);
        capabilities.put(CuraMedicationScannedProcessor.CAPABILITY_MEDS, true);
        capabilities.put(SpecimenCollectionsScannedProcessor.CAPABILITY_COLLECTIONS, true);
        capabilities.put(TestUtils.getStaticVariable(Utils.class,"CAPABILITY_CHARTING_NAVIGATOR"), true);
        final AuthnResponse response = AuthnResponse.newAuthnResponse(null, capabilities, map, SessionCheckHandler.Status.unlocked.name());
        mResponseCopy = AuthnResponse.newAuthnResponse(null, capabilities, map, SessionCheckHandler.Status.unlocked.name());
        IonAuthnSessionUtils.setAuthnResponse(ApplicationProvider.getApplicationContext(), response);
    }

    @After
    public void tearDown() {
        mDrawerItems = null;
        mPatientId = null;
        mEncounterId = null;
        mResponseCopy = null;

        mListeners.clear();
        mListeners = null;
    }

    @Test
    public void getDrawerItems_FeaturesOff() {
        IonAuthnSessionUtils.setAuthnResponse(ApplicationProvider.getApplicationContext(), null);
        mDrawerItems = NursingDrawerManager.getDrawerItems(ApplicationProvider.getApplicationContext());
        assertEquals(0, mDrawerItems.size());
    }

    @Test
    public void getDrawerItems_FeaturesOn_sameUserRelogin() {
        IonAuthnSessionUtils.setAuthnResponse(ApplicationProvider.getApplicationContext(), mResponseCopy);
        mDrawerItems = NursingDrawerManager.getDrawerItems(ApplicationProvider.getApplicationContext());
        assertEquals(10, mDrawerItems.size());
    }

    @Test
    public void getDrawerItems_FeaturesOn() {
        assertEquals(10, mDrawerItems.size());
    }

    @Test
    public void activeModuleChanged_FeaturesOff() {
        IonAuthnSessionUtils.setAuthnResponse(ApplicationProvider.getApplicationContext(), AuthnResponse.newAuthnResponse(null, null, null, SessionCheckHandler.Status.unlocked.name()));
        mDrawerItems = NursingDrawerManager.getDrawerItems(ApplicationProvider.getApplicationContext());

        TestUtils.invokePrivateStaticMethod(ActiveModuleManager.class, "notifyActiveModuleListeners", new Class[]{Class.class, int.class, Bundle.class}, PatientListActivity.class, 0, null);
        TestUtils.invokePrivateStaticMethod(ActiveModuleManager.class, "notifyActiveModuleListeners", new Class[]{Class.class, int.class, Bundle.class}, PatientChartActivity.class, 0, null);
        TestUtils.invokePrivateStaticMethod(ActiveModuleManager.class, "notifyActiveModuleListeners", new Class[]{Class.class, int.class, Bundle.class}, MedsAdminActivity.class, 0, null);
        TestUtils.invokePrivateStaticMethod(ActiveModuleManager.class, "notifyActiveModuleListeners", new Class[]{Class.class, int.class, Bundle.class}, SpecimenCollectActivity.class, 0, null);
        TestUtils.invokePrivateStaticMethod(ActiveModuleManager.class, "notifyActiveModuleListeners", new Class[]{Class.class, int.class, Bundle.class}, ChartingNavigatorActivity.class, 0, null);
        TestUtils.invokePrivateStaticMethod(ActiveModuleManager.class, "notifyActiveModuleListeners", new Class[]{Class.class, int.class, Bundle.class}, CareTeamActivity.class, 0, null);
        TestUtils.invokePrivateStaticMethod(ActiveModuleManager.class, "notifyActiveModuleListeners", new Class[]{Class.class, int.class, Bundle.class}, DeviceAssociationActivity.class, 0, null);
        TestUtils.invokePrivateStaticMethod(ActiveModuleManager.class, "notifyActiveModuleListeners", new Class[]{Class.class, int.class, Bundle.class}, SettingsActivity.class, 0, null);
    }

    @Test
    public void activeModuleChanged() {
        TestUtils.invokePrivateStaticMethod(ActiveModuleManager.class, "notifyActiveModuleListeners", new Class[]{Class.class, int.class, Bundle.class}, PatientListActivity.class, 0, null);
        TestUtils.invokePrivateStaticMethod(ActiveModuleManager.class, "notifyActiveModuleListeners", new Class[]{Class.class, int.class, Bundle.class}, PatientChartActivity.class, 0, null);
        TestUtils.invokePrivateStaticMethod(ActiveModuleManager.class, "notifyActiveModuleListeners", new Class[]{Class.class, int.class, Bundle.class}, MedsAdminActivity.class, 0, null);
        TestUtils.invokePrivateStaticMethod(ActiveModuleManager.class, "notifyActiveModuleListeners", new Class[]{Class.class, int.class, Bundle.class}, SpecimenCollectActivity.class, 0, null);
        TestUtils.invokePrivateStaticMethod(ActiveModuleManager.class, "notifyActiveModuleListeners", new Class[]{Class.class, int.class, Bundle.class}, ChartingNavigatorActivity.class, 0, null);
        TestUtils.invokePrivateStaticMethod(ActiveModuleManager.class, "notifyActiveModuleListeners", new Class[]{Class.class, int.class, Bundle.class}, CareTeamActivity.class, 0, null);
        TestUtils.invokePrivateStaticMethod(ActiveModuleManager.class, "notifyActiveModuleListeners", new Class[]{Class.class, int.class, Bundle.class}, DeviceAssociationActivity.class, 0, null);
        TestUtils.invokePrivateStaticMethod(ActiveModuleManager.class, "notifyActiveModuleListeners", new Class[]{Class.class, int.class, Bundle.class}, SettingsActivity.class, 0, null);
    }

    @Test
    public void onDrawerItemClicked() {
        ActiveModuleManager.clearActiveModule();
        assertTrue(((DrawerListItem) mDrawerItems.get(0)).onClick(null));
        assertEquals(PatientListActivity.class, ActiveModuleManager.getActiveModule());
        assertFalse(((DrawerListItem) mDrawerItems.get(0)).onClick(null));

        assertTrue(((DrawerListItem) mDrawerItems.get(2)).onClick(null));
        assertEquals(PatientChartActivity.class, ActiveModuleManager.getActiveModule());
        assertFalse(((DrawerListItem) mDrawerItems.get(2)).onClick(null));

        assertTrue(((DrawerListItem) mDrawerItems.get(3)).onClick(null));
        assertEquals(MedsAdminActivity.class, ActiveModuleManager.getActiveModule());
        assertFalse(((DrawerListItem) mDrawerItems.get(3)).onClick(null));

        assertTrue(((DrawerListItem) mDrawerItems.get(4)).onClick(null));
        assertEquals(SpecimenCollectActivity.class, ActiveModuleManager.getActiveModule());
        assertFalse(((DrawerListItem) mDrawerItems.get(4)).onClick(null));

        assertTrue(((DrawerListItem) mDrawerItems.get(5)).onClick(null));
        assertEquals(ChartingNavigatorActivity.class, ActiveModuleManager.getActiveModule());
        assertFalse(((DrawerListItem) mDrawerItems.get(5)).onClick(null));

        assertTrue(((DrawerListItem) mDrawerItems.get(6)).onClick(null));
        assertEquals(CareTeamActivity.class, ActiveModuleManager.getActiveModule());
        assertFalse(((DrawerListItem) mDrawerItems.get(6)).onClick(null));

        assertTrue(((DrawerListItem) mDrawerItems.get(7)).onClick(null));
        assertEquals(DeviceAssociationActivity.class, ActiveModuleManager.getActiveModule());
        assertFalse(((DrawerListItem) mDrawerItems.get(7)).onClick(null));

        assertTrue(((DrawerListItem) mDrawerItems.get(8)).onClick(null));
        assertEquals(SettingsActivity.class, ActiveModuleManager.getActiveModule());
        assertFalse(((DrawerListItem) mDrawerItems.get(8)).onClick(null));

        assertFalse(((DrawerListItem) mDrawerItems.get(9)).onClick(null));
    }

    @Test
    public void onDrawerItemClicked_logoutPrompt() {
        final IonActivity activity = mock(IonActivity.class);
        final DialogController dialogController = mock(DialogController.class);
        Mockito.doReturn(dialogController).when(activity).getDialogs();
        Mockito.doReturn(mock(AlertDialog.class)).when(dialogController).showError(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.anyString(),
                                                                                   Mockito.isNull(), Mockito.isNull(), Mockito.isNull());
        assertFalse(((DrawerListItem) mDrawerItems.get(9)).onClick(activity));
        final ArgumentCaptor<DialogInterface.OnClickListener> captor = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        Mockito.verify(dialogController).showError(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), captor.capture(), Mockito.anyString(),
                                                   Mockito.isNull(), Mockito.isNull(), Mockito.isNull());
        captor.getValue().onClick(null, 0);
    }

    @Test
    public void drawerPatientRetriever() {
        final IDataRetriever drawerPatientRetriever = TestUtils.getStaticVariable(NursingDrawerManager.class, "smDrawerPatientRetriever");
        drawerPatientRetriever.setRequestMethod(0, null);
        assertFalse(drawerPatientRetriever.cancelRequest(null));
        assertTrue(drawerPatientRetriever.backgroundAfterCancel(null));
        drawerPatientRetriever.setActionBarWaitCursor(true);

        //onFailedResponse
        final Pair<String, String> pair = new Pair<>("", "");
        TestUtils.setVariable(drawerPatientRetriever, "smLoadedPatient", pair);
        drawerPatientRetriever.onFailedResponse(PatientDemogBanner.class, false);
        assertNull(TestUtils.getVariable(drawerPatientRetriever, "smLoadedPatient"));

        //onErrorResponse
        TestUtils.setVariable(drawerPatientRetriever, "smLoadedPatient", pair);
        drawerPatientRetriever.onErrorResponse(null, PatientDemogBanner.class);
        assertNull(TestUtils.getVariable(drawerPatientRetriever, "smLoadedPatient"));

        //onNoContentResponse
        final PatientDemogBanner patientDemogBanner = new PatientDemogBanner();
        final DrawerPatientListItem drawerPatientListItem = TestUtils.getStaticVariable(NursingDrawerManager.class, "smDrawerPatientListItem");
        drawerPatientListItem.setData(patientDemogBanner);
        TestUtils.setVariable(drawerPatientRetriever, "smLoadedPatient", pair);
        drawerPatientRetriever.onNoContentResponse(null, PatientDemogBanner.class);
        assertNull(TestUtils.getVariable(drawerPatientRetriever, "smLoadedPatient"));
        assertNull(drawerPatientListItem.getData());

        //onResponse
        TestUtils.setVariable(drawerPatientRetriever, "smLoadedPatient", pair);
        final CernResponse response = new CernResponse();
        response.data = patientDemogBanner;
        drawerPatientRetriever.onResponse(response);
        assertEquals(pair, TestUtils.getVariable(drawerPatientRetriever, "smLoadedPatient"));
        assertEquals(patientDemogBanner, drawerPatientListItem.getData());
    }

    @Test
    public void drawerPatientRetriever_getData() {
        final IDataRetriever drawerPatientRetriever = TestUtils.getStaticVariable(NursingDrawerManager.class, "smDrawerPatientRetriever");
        //no patient
        TestUtils.setVariable(drawerPatientRetriever, "smLoadedPatient", null);
        drawerPatientRetriever.getData(IDataRetriever.DataArgs.NONE);
        final Pair<String, String> loadedPatient = TestUtils.getVariable(drawerPatientRetriever, "smLoadedPatient");
        assertEquals(mPatientId, loadedPatient.first);
        assertEquals(mEncounterId, loadedPatient.second);

        //new patient
        Pair<String, String> pair = new Pair<>("", "");
        TestUtils.setVariable(drawerPatientRetriever, "smLoadedPatient", pair);
        drawerPatientRetriever.getData(IDataRetriever.DataArgs.NONE);
        assertFalse(pair.equals(TestUtils.getVariable(drawerPatientRetriever, "smLoadedPatient")));

        //new encounter
        pair = new Pair<>(mPatientId, "");
        TestUtils.setVariable(drawerPatientRetriever, "smLoadedPatient", pair);
        drawerPatientRetriever.getData(IDataRetriever.DataArgs.NONE);
        assertFalse(pair.equals(TestUtils.getVariable(drawerPatientRetriever, "smLoadedPatient")));

        //same patient
        pair = new Pair<>(mPatientId, mEncounterId);
        TestUtils.setVariable(drawerPatientRetriever, "smLoadedPatient", pair);
        drawerPatientRetriever.getData(IDataRetriever.DataArgs.NONE);
        assertEquals(pair, TestUtils.getVariable(drawerPatientRetriever, "smLoadedPatient"));

        //no patient, one previously
        PatientContext.clearContext();
        TestUtils.setVariable(drawerPatientRetriever, "smLoadedPatient", pair);
        drawerPatientRetriever.getData(IDataRetriever.DataArgs.NONE);
        assertNull(TestUtils.getVariable(drawerPatientRetriever, "smLoadedPatient"));

        //no patient, none previously
        drawerPatientRetriever.getData(IDataRetriever.DataArgs.NONE);
    }
}