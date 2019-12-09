package com.cerner.cura.scanning;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.cerner.cura.base.CuraAuthnActivity;
import com.cerner.cura.base.PatientContext;
import com.cerner.cura.requestor.MockDataManager;
import com.cerner.cura.scanning.datamodel.PatientResponse;
import com.cerner.cura.scanning.datamodel.ScanViewResponse;
import com.cerner.cura.test.TestActivity;
import com.cerner.cura.test.helper.TestUtils;
import com.cerner.cura.utils.ActiveModuleManager;
import com.cerner.ion.security.DialogController;
import com.cerner.ion.security.IonActivity;
import com.cerner.nursing.nursing.ui.PatientChartActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

/**
 * @author Lam Tran (lt028506) on 8/12/14.
 */
@RunWith (RobolectricTestRunner.class)
public class PatientScannedProcessorTests {
    PatientScannedProcessor mProcessor;

    @BeforeClass
    public static void ClassSetup() {
        System.gc();
    }

    @Before
    public void setup() {
        TestUtils.setStaticVariable(MockDataManager.class, "smState", MockDataManager.MockState.READ);
        mProcessor = new PatientScannedProcessor();
        PatientContext.clearContext();
    }

    @After
    public void tearDown() {
        mProcessor = null;
    }

    @Test
    public void onBarcodeReceived_notPatientSet() {
        final CuraAuthnActivity activity = Mockito.mock(CuraAuthnActivity.class);
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn(new Bundle()).when(intent).getExtras();

        ActiveModuleManager.setActiveModule(TestActivity.class, 0, null);
        mProcessor.onBarcodeReceived(activity, null);
        assertEquals(TestActivity.class, ActiveModuleManager.getActiveModule());
    }

    @Test
    public void onBarcodeReceived_hasPatientSet_noLatestModule() {
        final CuraAuthnActivity activity = Mockito.spy(Robolectric.buildActivity(TestActivity.class).create().get());
        Mockito.doReturn(Mockito.mock(DialogController.class)).when(activity).getDialogs();
        final ScanViewResponse barcode = new ScanViewResponse();
        barcode.patientResponse = new PatientResponse();
        barcode.patientResponse.encounterId = "1234L";
        barcode.patientResponse.id = "5678L";

        PatientContext.clearContext();
        mProcessor = Mockito.spy(mProcessor);
        final CuraPatientScannedProcessor.OnCurrentPatientScannedConfirmationListener listener = Mockito.mock(CuraPatientScannedProcessor.OnCurrentPatientScannedConfirmationListener.class);
        Mockito.doReturn(listener).when(mProcessor).getConfirmationListener();

        ActiveModuleManager.setActiveModule(TestActivity.class, 0, null);
        mProcessor.onBarcodeReceived(null, barcode);
        //TODO ml015922: also confirm that the proper message is shown once it is added
        Mockito.verify(listener, Mockito.never()).onCurrentPatientScannedConfirmation();
        assertEquals(TestActivity.class, ActiveModuleManager.getActiveModule());

        Mockito.doReturn(null).when(mProcessor).getConfirmationListener();
        ActiveModuleManager.setActiveModule(TestActivity.class, 0, null);
        mProcessor.onBarcodeReceived(activity, barcode);
        assertEquals(TestActivity.class, ActiveModuleManager.getActiveModule());

        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "4321L");
        ActiveModuleManager.setActiveModule(TestActivity.class, 0, null);
        mProcessor.onBarcodeReceived(activity, barcode);
        assertEquals(TestActivity.class, ActiveModuleManager.getActiveModule());

        PatientContext.setPatientAndEncounter("8765L", "test_encounter_id_" + System.currentTimeMillis());
        ActiveModuleManager.setActiveModule(TestActivity.class, 0, null);
        mProcessor.onBarcodeReceived(activity, barcode);
        assertEquals(TestActivity.class, ActiveModuleManager.getActiveModule());

        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "1234L");
        ActiveModuleManager.setActiveModule(TestActivity.class, 0, null);
        mProcessor.onBarcodeReceived(activity, barcode);
        assertEquals(TestActivity.class, ActiveModuleManager.getActiveModule());

        PatientContext.setPatientAndEncounter("5678L", "1234L");
        ActiveModuleManager.setActiveModule(TestActivity.class, 0, null);
        mProcessor.onBarcodeReceived(activity, barcode);
        assertEquals(PatientChartActivity.class, ActiveModuleManager.getActiveModule());

        PatientContext.setLatestModule(Activity.class, 2);
        ActiveModuleManager.setActiveModule(TestActivity.class, 0, null);
        mProcessor.onBarcodeReceived(activity, barcode);
        assertEquals(Activity.class, ActiveModuleManager.getActiveModule());

        Mockito.doReturn(listener).when(mProcessor).getConfirmationListener();
        ActiveModuleManager.setActiveModule(TestActivity.class, 0, null);
        mProcessor.onBarcodeReceived(activity, barcode);
        Mockito.verify(listener).onCurrentPatientScannedConfirmation();
        assertEquals(TestActivity.class, ActiveModuleManager.getActiveModule());
    }

    @Test
    public void onBarcodeReceived_hasPatientSet_notCuraAuthnActivity() {
        final IonActivity activity = null;
        final ScanViewResponse barcode = new ScanViewResponse();
        barcode.patientResponse = new PatientResponse();
        barcode.patientResponse.encounterId = "1234L";
        barcode.patientResponse.id = "5678L";

        mProcessor.onBarcodeReceived(activity, barcode);
    }

    @Test
    public void onBarcodeReceived_hasPatientSet_hasActiveModuleInList() {
        final CuraAuthnActivity activity = Mockito.mock(CuraAuthnActivity.class);
        final ScanViewResponse barcode = new ScanViewResponse();
        PatientContext.setPatientAndEncounter("5678L", "1234L");

        barcode.patientResponse = new PatientResponse();
        barcode.patientResponse.encounterId = "1234L";
        barcode.patientResponse.id = "5678L";
        PatientContext.setLatestModule(PatientChartActivity.class, 2);

        mProcessor.onBarcodeReceived(activity, barcode);
    }
}
