package com.cerner.nursing.nursing.utils;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;

import com.cerner.cura.base.CuraAppBuildConfig;
import com.cerner.cura.datamodel.OrgForDevice;
import com.cerner.cura.requestor.IDataRetriever;
import com.cerner.cura.requestor.IRetrieverContext;
import com.cerner.cura.requestor.MockDataManager;
import com.cerner.cura.test.TestActivity;
import com.cerner.cura.test.helper.TestUtils;
import com.cerner.cura.utils.ActiveModuleManager;
import com.cerner.ion.provisioning.ProvisionedTenant;
import com.cerner.ion.request.CernRequest;
import com.cerner.ion.request.CernResponse;
import com.cerner.ion.security.DialogController;
import com.cerner.ion.session.AuthnResponse;
import com.cerner.ion.session.IonAuthnSessionUtils;
import com.cerner.ion.session.IonSessionUtils;
import com.cerner.nursing.nursing.ui.AppSwitchEntryActivity;
import com.cerner.nursing.nursing.ui.PatientListActivity;
import com.cerner.nursing.nursing.ui.SettingsActivity;
import com.google.gson.Gson;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.lang.ref.WeakReference;
import java.util.Map;

import androidx.test.core.app.ApplicationProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Lam Tran (lt028506) on 3/25/14.
 */
@RunWith (RobolectricTestRunner.class)
public class AppSwitchUtilsTests {
    private ActivityController<AppSwitchEntryActivity> mActivityController;
    private AppSwitchEntryActivity mMockActivity;
    private IRetrieverContext mRetrieverContext;

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
        mActivityController = Robolectric.buildActivity(AppSwitchEntryActivity.class).create();
        mMockActivity = mActivityController.get();
        mRetrieverContext = Mockito.mock(IRetrieverContext.class);
        Mockito.doReturn(true).when(mRetrieverContext).processResponses();
    }

    @After
    public void tearDown() {
        mMockActivity = null;
        mRetrieverContext = null;
        mActivityController = null;
    }

    @Test
    public void privateConstructor() {
        TestUtils.invokePrivateConstructor(AppSwitchUtils.class);
    }

    @Test
    public void performAppSwitch_stillSwitching() {
        TestUtils.setStaticVariable(AppSwitchUtils.class, "smStoredIntent", Mockito.mock(Intent.class));
        AppSwitchUtils.performAppSwitch(mMockActivity, null, null);
    }

    @Test
    public void verifyIntentData_nullIntent() {
        TestUtils.setStaticVariable(AppSwitchUtils.class, "smStoredIntent", null);
        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, null);
        assertNull(TestUtils.getStaticVariable(AppSwitchUtils.class, "smStoredIntent"));
        final Intent intent = new Intent();
        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);
        assertNull(TestUtils.getStaticVariable(AppSwitchUtils.class, "smStoredIntent"));
    }

    @Test
    public void verifyIntentData_notAppSwitchIntent() {
        mMockActivity = Mockito.spy(mMockActivity);
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn("a").when(intent).getAction();

        final Fragment fragment = Mockito.mock(Fragment.class);
        Mockito.doReturn(Mockito.mock(View.class)).when(fragment).getView();
        final FragmentManager manager = Mockito.mock(FragmentManager.class);
        Mockito.doReturn(fragment).when(manager).findFragmentById(Mockito.anyInt());
        Mockito.doReturn(manager).when(mMockActivity).getFragmentManager();

        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);
        assertNull(TestUtils.getStaticVariable(AppSwitchUtils.class, "smStoredIntent"));
    }

    @Test
    public void verifyIntentData_appSwitchIntent() {
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn("com.cerner.nursing.nursing.OPEN_PATIENT").when(intent).getAction();
        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);

        assertNull(TestUtils.getStaticVariable(AppSwitchUtils.class, "smStoredIntent"));
    }

    @Test
    public void verifyIntentData_badTenantInfo() {
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn("com.cerner.nursing.nursing.OPEN_PATIENT").when(intent).getAction();
        Mockito.doReturn(null).when(intent).getStringExtra("tenant");
        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);
        assertNull(TestUtils.getStaticVariable(AppSwitchUtils.class, "smStoredIntent"));

        Mockito.doReturn("").when(intent).getStringExtra("tenant");
        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);
        assertNull(TestUtils.getStaticVariable(AppSwitchUtils.class, "smStoredIntent"));
    }

    @Test
    public void verifyIntentData_appSwitchIntent_nullPatientInfo() {
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn("com.cerner.nursing.nursing.OPEN_PATIENT").when(intent).getAction();
        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);

        Mockito.doReturn("aa").when(intent).getStringExtra("patient_id");
        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);

        Mockito.doReturn("aa").when(intent).getStringExtra("encounter_id");
        Mockito.doReturn("aa").when(intent).getStringExtra("patient_id");
        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);

        assertNull(TestUtils.getStaticVariable(AppSwitchUtils.class, "smStoredIntent"));
    }

    @Test
    public void verifyIntentData_appSwitchIntent_tenantNotJsonFormat() {
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn("com.cerner.nursing.nursing.OPEN_PATIENT").when(intent).getAction();
        Mockito.doReturn("{\"tenantColor\":0} this is not a correct json format").when(intent).getStringExtra("tenant");
        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);
        assertNull(TestUtils.getStaticVariable(AppSwitchUtils.class, "smStoredIntent"));
    }

    @Test
    public void verifyIntentData_appSwitchIntent_tenantNotNull() {
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn("com.cerner.nursing.nursing.OPEN_PATIENT").when(intent).getAction();
        Mockito.doReturn("{\"tenantId\":\"OxMOCKTENANTGUIDTHATM-EANSNOTHIN\",\"tenantColor\":0}").when(intent).getStringExtra("tenant");
        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);
        assertNull(TestUtils.getStaticVariable(AppSwitchUtils.class, "smStoredIntent"));
    }

    @Test
    public void verifyIntentData_appSwitchIntent_userNameNotNull() {
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn("com.cerner.nursing.nursing.OPEN_PATIENT").when(intent).getAction();
        Mockito.doReturn("{\"tenantId\":\"OxMOCKTENANTGUIDTHATM-EANSNOTHIN\",\"tenantColor\":0}").when(intent).getStringExtra("tenant");
        Mockito.doReturn("").when(intent).getStringExtra("username");
        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);

        assertNull(TestUtils.getStaticVariable(AppSwitchUtils.class, "smStoredIntent"));
    }

    @Test
    public void verifyIntentData_appSwitchIntent_userNameNotEmpty() {
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn("com.cerner.nursing.nursing.OPEN_PATIENT").when(intent).getAction();
        Mockito.doReturn("{\"tenantId\":\"OxMOCKTENANTGUIDTHATM-EANSNOTHIN\",\"tenantColor\":0}").when(intent).getStringExtra("tenant");
        Mockito.doReturn("username").when(intent).getStringExtra("username");
        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);

        assertNull(TestUtils.getStaticVariable(AppSwitchUtils.class, "smStoredIntent"));
    }

    @Test
    public void verifyIntentData_appSwitchIntent_patientIdNotNull() {
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn("com.cerner.nursing.nursing.OPEN_PATIENT").when(intent).getAction();
        Mockito.doReturn("{\"tenantId\":\"OxMOCKTENANTGUIDTHATM-EANSNOTHIN\",\"tenantColor\":0}").when(intent).getStringExtra("tenant");
        Mockito.doReturn("username").when(intent).getStringExtra("username");
        Mockito.doReturn("").when(intent).getStringExtra("patient_id");
        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);

        assertNull(TestUtils.getStaticVariable(AppSwitchUtils.class, "smStoredIntent"));
    }

    @Test
    public void verifyIntentData_appSwitchIntent_patientIdNotEmpty() {
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn("com.cerner.nursing.nursing.OPEN_PATIENT").when(intent).getAction();
        Mockito.doReturn("{\"tenantId\":\"OxMOCKTENANTGUIDTHATM-EANSNOTHIN\",\"tenantColor\":0}").when(intent).getStringExtra("tenant");
        Mockito.doReturn("username").when(intent).getStringExtra("username");
        Mockito.doReturn("patient_id").when(intent).getStringExtra("patientId");
        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);

        assertNull(TestUtils.getStaticVariable(AppSwitchUtils.class, "smStoredIntent"));
    }

    @Test
    public void verifyIntentData_appSwitchIntent_encounterIdNotNull() {
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn("com.cerner.nursing.nursing.OPEN_PATIENT").when(intent).getAction();
        Mockito.doReturn("{\"tenantId\":\"OxMOCKTENANTGUIDTHATM-EANSNOTHIN\",\"tenantColor\":0}").when(intent).getStringExtra("tenant");
        Mockito.doReturn("username").when(intent).getStringExtra("username");
        Mockito.doReturn("patient_id").when(intent).getStringExtra("patient_id");
        Mockito.doReturn("").when(intent).getStringExtra("encounter_id");
        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);
        assertNull(TestUtils.getStaticVariable(AppSwitchUtils.class, "smStoredIntent"));
    }

    @Test
    public void verifyIntentData_appSwitchIntent_encounterIdNotEmpty() {
        TestUtils.setStaticVariable(MockDataManager.class, "smState", MockDataManager.MockState.NOTHING);
        mMockActivity = Mockito.spy(mMockActivity);
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn("com.cerner.nursing.nursing.OPEN_PATIENT").when(intent).getAction();
        Mockito.doReturn("{\"tenantId\":\"OxMOCKTENANTGUIDTHATM-EANSNOTHIN\",\"tenantColor\":0}").when(intent).getStringExtra("tenant");
        Mockito.doReturn("username").when(intent).getStringExtra("username");
        Mockito.doReturn("patient_id").when(intent).getStringExtra("patient_id");
        Mockito.doReturn("encounter_id").when(intent).getStringExtra("encounter_id");
        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);
        assertEquals(intent, TestUtils.getStaticVariable(AppSwitchUtils.class, "smStoredIntent"));
    }

    @Test
    public void verifyIntentData_appSwitchIntent_tenantEqualsToSession() {
        TestUtils.setStaticVariable(MockDataManager.class, "smState", MockDataManager.MockState.NOTHING);
        mMockActivity = Mockito.spy(mMockActivity);
        final ProvisionedTenant tenant = new ProvisionedTenant();
        tenant.tenantId = "OxMOCKTENANTGUIDTHATM-EANSNOTHIN";
        IonAuthnSessionUtils.setTenant(tenant);
        IonAuthnSessionUtils.setAuthnResponse(ApplicationProvider.getApplicationContext(), null);

        final Gson gson = new Gson();
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn("com.cerner.nursing.nursing.OPEN_PATIENT").when(intent).getAction();
        Mockito.doReturn(gson.toJson(IonAuthnSessionUtils.getTenant())).when(intent).getStringExtra("tenant");
        Mockito.doReturn("username").when(intent).getStringExtra("username");
        Mockito.doReturn("patient_id").when(intent).getStringExtra("patient_id");
        Mockito.doReturn("encounter_id").when(intent).getStringExtra("encounter_id");
        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);
        assertEquals(intent, TestUtils.getStaticVariable(AppSwitchUtils.class, "smStoredIntent"));
    }

    @Test
    public void verifyIntentData_appSwitchIntent_userNotNull() {
        TestUtils.setStaticVariable(MockDataManager.class, "smState", MockDataManager.MockState.NOTHING);
        mMockActivity = Mockito.spy(mMockActivity);
        final ProvisionedTenant tenant = new ProvisionedTenant();
        tenant.tenantId = "OxMOCKTENANTGUIDTHATM-EANSNOTHIN";
        IonAuthnSessionUtils.setTenant(tenant);

        final Gson gson = new Gson();
        final AuthnResponse response = gson.fromJson(
                "{\"capabilities\":{\"authz/authorized_apps\":[\"com.cerner.nursing.nursing.staging\"],\"authn/inactivity_session_lock_max_sec\":0,\"authn/inactivity_session_logout_max_sec\":0,\"tenant\":\"OxMOCKTENANTGUIDTHATM-EANSNOTHIN\",\"utc\":false},\"features\":{\"feature/useMock\":true},\"user\":{\"emailAddresses\":[\"mock.email@cerner.com\"],\"firstName\":\"mockFirstName\",\"username\":\"mockUsername\",\"lastName\":\"mockLastName\",\"openId\":\"https://millennia.devcerner.com/instance/OxMOCKGUIDVALUETHATME-ANSNOT_SWITCHING/principal/0000.0000.02D5.2215\",\"hasPin\":false}}",
                AuthnResponse.class);
        IonAuthnSessionUtils.setAuthnResponse(ApplicationProvider.getApplicationContext().getApplicationContext(), response);

        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn("com.cerner.nursing.nursing.OPEN_PATIENT").when(intent).getAction();
        Mockito.doReturn(gson.toJson(IonAuthnSessionUtils.getTenant())).when(intent).getStringExtra("tenant");
        Mockito.doReturn("username").when(intent).getStringExtra("username");
        Mockito.doReturn("patient_id").when(intent).getStringExtra("patient_id");
        Mockito.doReturn("encounter_id").when(intent).getStringExtra("encounter_id");

        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);
        assertEquals(intent, TestUtils.getStaticVariable(AppSwitchUtils.class, "smStoredIntent"));
    }

    @Test
    public void verifyIntentData_appSwitchIntent_nullSession() {
        TestUtils.setStaticVariable(MockDataManager.class, "smState", MockDataManager.MockState.NOTHING);
        mMockActivity = Mockito.spy(mMockActivity);
        final ProvisionedTenant tenant = new ProvisionedTenant();
        tenant.tenantId = "OxMOCKTENANTGUIDTHATM-EANSNOTHIN";
        IonAuthnSessionUtils.setTenant(null);
        IonAuthnSessionUtils.setAuthnResponse(ApplicationProvider.getApplicationContext().getApplicationContext(), null);

        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn("com.cerner.nursing.nursing.OPEN_PATIENT").when(intent).getAction();
        Mockito.doReturn(new Gson().toJson(tenant)).when(intent).getStringExtra("tenant");
        Mockito.doReturn("mockUsername").when(intent).getStringExtra("username");
        Mockito.doReturn("patient_id").when(intent).getStringExtra("patient_id");
        Mockito.doReturn("encounter_id").when(intent).getStringExtra("encounter_id");

        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);
        Mockito.verify(mMockActivity, Mockito.never()).finish();
    }

    @Test
    public void verifyIntentData_appSwitchIntent_mockData() {
        mMockActivity = Mockito.spy(mMockActivity);
        final ProvisionedTenant tenant = new ProvisionedTenant();
        tenant.tenantId = "OxMOCKTENANTGUIDTHATM-EANSNOTHIN";
        IonAuthnSessionUtils.setTenant(null);
        IonAuthnSessionUtils.setAuthnResponse(ApplicationProvider.getApplicationContext().getApplicationContext(), null);

        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn("com.cerner.nursing.nursing.OPEN_PATIENT").when(intent).getAction();
        Mockito.doReturn(new Gson().toJson(tenant)).when(intent).getStringExtra("tenant");
        Mockito.doReturn("mockUsername").when(intent).getStringExtra("username");
        Mockito.doReturn("patient_id").when(intent).getStringExtra("patient_id");
        Mockito.doReturn("encounter_id").when(intent).getStringExtra("encounter_id");

        TestUtils.setStaticVariable(AppSwitchUtils.class, "smStoredIntent", null);
        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);
        assertNotNull(TestUtils.getStaticVariable(AppSwitchUtils.class, "smStoredIntent"));
    }

    @Test
    public void verifyIntentData_appSwitchIntent_notMockData_nullTenant() {
        mMockActivity = Mockito.spy(mMockActivity);
        final ProvisionedTenant tenant = new ProvisionedTenant();
        tenant.tenantId = "OxMOCKTENANTGUIDTHATM-EANSNOTHIN";
        IonAuthnSessionUtils.setTenant(tenant);

        final Gson gson = new Gson();
        final AuthnResponse response = gson.fromJson(
                "{\"capabilities\":{\"authz/authorized_apps\":[\"com.cerner.nursing.nursing.staging\"],\"authn/inactivity_session_lock_max_sec\":0,\"authn/inactivity_session_logout_max_sec\":0,\"tenant\":\"OxMOCKTENANTGUIDTHATM-EANSNOTHIN\",\"utc\":false},\"features\":{\"feature/useMock\":true},\"user\":{\"emailAddresses\":[\"mock.email@cerner.com\"],\"firstName\":\"mockFirstName\",\"username\":\"mockUsername\",\"lastName\":\"mockLastName\",\"openId\":\"https://millennia.devcerner.com/instance/OxMOCKGUIDVALUETHATME-ANSNOT_SWITCHING/principal/0000.0000.02D5.2215\",\"hasPin\":false}}",
                AuthnResponse.class);
        IonAuthnSessionUtils.setAuthnResponse(ApplicationProvider.getApplicationContext().getApplicationContext(), response);

        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn("com.cerner.nursing.nursing.OPEN_PATIENT").when(intent).getAction();
        Mockito.doReturn(gson.toJson(tenant)).when(intent).getStringExtra("tenant");
        Mockito.doReturn("mockUsername").when(intent).getStringExtra("username");
        Mockito.doReturn("patient_id").when(intent).getStringExtra("patient_id");
        Mockito.doReturn("encounter_id").when(intent).getStringExtra("encounter_id");
        IonSessionUtils.setLoggingOutToken("test");

        TestUtils.setStaticVariable(MockDataManager.class, "smState", MockDataManager.MockState.NOTHING);

        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);
        assertNotNull(IonAuthnSessionUtils.getTenant());
        assertNotNull(IonAuthnSessionUtils.getAuthnResponse());
    }

    @Test
    public void verifyIntentData_appSwitchIntent_notMockData_usePrincipal() {
        mMockActivity = Mockito.spy(mMockActivity);
        final ProvisionedTenant tenant = new ProvisionedTenant();
        tenant.tenantId = "OxMOCKTENANTGUIDTHATM-EANSNOTHIN";
        IonAuthnSessionUtils.setTenant(tenant);

        final Gson gson = new Gson();
        final AuthnResponse response = gson.fromJson(
                "{\"capabilities\":{\"authz/authorized_apps\":[\"com.cerner.nursing.nursing.staging\"],\"authn/inactivity_session_lock_max_sec\":0,\"authn/inactivity_session_logout_max_sec\":0,\"tenant\":\"OxMOCKTENANTGUIDTHATM-EANSNOTHIN\",\"utc\":false},\"features\":{\"feature/useMock\":true},\"user\":{\"emailAddresses\":[\"mock.email@cerner.com\"],\"firstName\":\"mockFirstName\",\"username\":\"mockUsername\",\"lastName\":\"mockLastName\",\"openId\":\"https://millennia.devcerner.com/instance/OxMOCKGUIDVALUETHATME-ANSNOT_SWITCHING/principal/0000.0000.02D5.2215\",\"hasPin\":false}}",
                AuthnResponse.class);
        IonAuthnSessionUtils.setAuthnResponse(ApplicationProvider.getApplicationContext().getApplicationContext(), response);

        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn("com.cerner.nursing.nursing.OPEN_PATIENT").when(intent).getAction();
        Mockito.doReturn(gson.toJson(tenant)).when(intent).getStringExtra("tenant");
        Mockito.doReturn("https://millennia.devcerner.com/instance/OxMOCKGUIDVALUETHATME-ANSNOT_SWITCHING/principal/0000.0000.02D5.2215").when(intent).getStringExtra("principal");
        Mockito.doReturn("patient_id").when(intent).getStringExtra("patient_id");
        Mockito.doReturn("encounter_id").when(intent).getStringExtra("encounter_id");
        IonSessionUtils.setLoggingOutToken("test");

        TestUtils.setStaticVariable(MockDataManager.class, "smState", MockDataManager.MockState.NOTHING);

        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);
        assertNotNull(IonAuthnSessionUtils.getTenant());
        assertNotNull(IonAuthnSessionUtils.getAuthnResponse());
    }

    @Test
    public void verifyIntentData_appSwitchIntent_notMockData_usePrincipal_different() {
        mMockActivity = Mockito.spy(mMockActivity);
        final ProvisionedTenant tenant = new ProvisionedTenant();
        tenant.tenantId = "OxMOCKTENANTGUIDTHATM-EANSNOTHIN";
        IonAuthnSessionUtils.setTenant(tenant);

        final Gson gson = new Gson();
        final AuthnResponse response = gson.fromJson(
                "{\"capabilities\":{\"authz/authorized_apps\":[\"com.cerner.nursing.nursing.staging\"],\"authn/inactivity_session_lock_max_sec\":0,\"authn/inactivity_session_logout_max_sec\":0,\"tenant\":\"OxMOCKTENANTGUIDTHATM-EANSNOTHIN\",\"utc\":false},\"features\":{\"feature/useMock\":true},\"user\":{\"emailAddresses\":[\"mock.email@cerner.com\"],\"firstName\":\"mockFirstName\",\"username\":\"mockUsername\",\"lastName\":\"mockLastName\",\"openId\":\"https://millennia.devcerner.com/instance/OxMOCKGUIDVALUETHATME-ANSNOT_SWITCHING/principal/0000.0000.02D5.2215\",\"hasPin\":false}}",
                AuthnResponse.class);
        IonAuthnSessionUtils.setAuthnResponse(ApplicationProvider.getApplicationContext().getApplicationContext(), response);

        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn("com.cerner.nursing.nursing.OPEN_PATIENT").when(intent).getAction();
        Mockito.doReturn(gson.toJson(tenant)).when(intent).getStringExtra("tenant");
        Mockito.doReturn("differentId").when(intent).getStringExtra("principal");
        Mockito.doReturn("patient_id").when(intent).getStringExtra("patient_id");
        Mockito.doReturn("encounter_id").when(intent).getStringExtra("encounter_id");
        IonSessionUtils.setLoggingOutToken("test");

        TestUtils.setStaticVariable(MockDataManager.class, "smState", MockDataManager.MockState.NOTHING);

        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);
        assertNotNull(IonAuthnSessionUtils.getTenant());
        assertNotNull(IonAuthnSessionUtils.getAuthnResponse());
    }

    @Test
    public void verifyIntentData_appSwitchIntent_notMockData_loggedIn() {
        mMockActivity = Mockito.spy(mMockActivity);
        final ProvisionedTenant tenant = new ProvisionedTenant();
        tenant.tenantId = "OxMOCKTENANTGUIDTHATM-EANSNOTHIN";
        IonAuthnSessionUtils.setTenant(tenant);

        final Gson gson = new Gson();
        final AuthnResponse response = gson.fromJson(
                "{\"capabilities\":{\"authz/authorized_apps\":[\"com.cerner.nursing.nursing.staging\"],\"authn/inactivity_session_lock_max_sec\":0,\"authn/inactivity_session_logout_max_sec\":0,\"tenant\":\"OxMOCKTENANTGUIDTHATM-EANSNOTHIN\",\"utc\":false},\"features\":{\"feature/useMock\":true},\"user\":{\"emailAddresses\":[\"mock.email@cerner.com\"],\"firstName\":\"mockFirstName\",\"username\":\"mockUsername\",\"lastName\":\"mockLastName\",\"openId\":\"https://millennia.devcerner.com/instance/OxMOCKGUIDVALUETHATME-ANSNOT_SWITCHING/principal/0000.0000.02D5.2215\",\"hasPin\":false}}",
                AuthnResponse.class);
        IonAuthnSessionUtils.setAuthnResponse(ApplicationProvider.getApplicationContext().getApplicationContext(), response);
        IonSessionUtils.setLoggingOutToken("test");

        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn("com.cerner.nursing.nursing.OPEN_PATIENT").when(intent).getAction();
        Mockito.doReturn(gson.toJson(IonAuthnSessionUtils.getTenant())).when(intent).getStringExtra("tenant");
        Mockito.doReturn("mockUsername").when(intent).getStringExtra("username");
        Mockito.doReturn("patient_id").when(intent).getStringExtra("patient_id");
        Mockito.doReturn("encounter_id").when(intent).getStringExtra("encounter_id");

        TestUtils.setStaticVariable(MockDataManager.class, "smState", MockDataManager.MockState.READ);
        mMockActivity = Mockito.spy(Robolectric.buildActivity(AppSwitchEntryActivity.class).create().get());
        TestUtils.setStaticVariable(MockDataManager.class, "smState", MockDataManager.MockState.NOTHING);

        DialogController dialogs = Mockito.spy((DialogController) TestUtils.getVariable(mMockActivity, "dialogs"));
        TestUtils.setVariable(mMockActivity, "dialogs", dialogs);
        Mockito.doReturn(null).when(dialogs).showProgressDialog();
        Mockito.doNothing().when(dialogs).hideProgressDialog();
        Mockito.doReturn(null).when(dialogs).showError(
                Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.any(DialogInterface.OnClickListener.class),
                Mockito.isNull(), Mockito.isNull(),
                Mockito.isNull(), Mockito.isNull());

        final PackageManager packageManager = Mockito.mock(PackageManager.class);
        final Intent activityIntent = Mockito.mock(Intent.class);
        Mockito.doReturn(packageManager).when(mMockActivity).getPackageManager();
        Mockito.when(packageManager.getLaunchIntentForPackage(Mockito.anyString())).thenReturn(activityIntent);
        Mockito.doNothing().when(mMockActivity).startActivity(activityIntent);
        assertFalse(MockDataManager.getReadMockDataFlag());

        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);
        Mockito.verify(mMockActivity, Mockito.never()).finish();

        TestUtils.setStaticVariable(MockDataManager.class, "smState", MockDataManager.MockState.READ);
        mMockActivity = Mockito.spy(Robolectric.buildActivity(AppSwitchEntryActivity.class).create().get());
        TestUtils.setStaticVariable(MockDataManager.class, "smState", MockDataManager.MockState.NOTHING);

        dialogs = Mockito.spy((DialogController) TestUtils.getVariable(mMockActivity, "dialogs"));
        TestUtils.setVariable(mMockActivity, "dialogs", dialogs);
        Mockito.doReturn(null).when(dialogs).showProgressDialog();
        Mockito.doNothing().when(dialogs).hideProgressDialog();
        Mockito.doReturn(null).when(dialogs).showError(Mockito.anyString(), Mockito.anyString(),
                                                       Mockito.anyString(), Mockito.any(DialogInterface.OnClickListener.class),
                                                       Mockito.isNull(), Mockito.isNull(),
                                                       Mockito.isNull(), Mockito.isNull());

        AppSwitchUtils.performAppSwitch(mMockActivity, mRetrieverContext, intent);
        Mockito.verify(mMockActivity, Mockito.never()).finish();
    }

    @Test
    public void setReadMockFlag_hasReadFlag() {
        mMockActivity = Mockito.spy(mMockActivity);
        TestUtils.setStaticVariable(MockDataManager.class, "smState", MockDataManager.MockState.NOTHING);
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn(true).when(intent).getBooleanExtra("readMock", false);

        final Map<String, Object> config = TestUtils.getStaticVariable(CuraAppBuildConfig.class, "mConfig");
        config.put("ALLOW_MOCK_DATA", true);

        TestUtils.invokePrivateStaticMethod(AppSwitchUtils.class, "setReadMockFlag", new Class[]{Intent.class}, intent);
        assertTrue(MockDataManager.getReadMockDataFlag());
    }

    @Test
    public void setReadMockFlag_notHasReadFlag() {
        mMockActivity = Mockito.spy(mMockActivity);
        final Intent intent = Mockito.mock(Intent.class);
        TestUtils.setStaticVariable(MockDataManager.class, "smState", MockDataManager.MockState.NOTHING);
        Mockito.doReturn(false).when(intent).getBooleanExtra("readMock", false);

        TestUtils.invokePrivateStaticMethod(AppSwitchUtils.class, "setReadMockFlag", new Class[]{Intent.class}, intent);
        assertFalse(MockDataManager.getReadMockDataFlag());
    }

    @Test
    public void getOrgForDevice_getData() {
        final IDataRetriever dataRetriever = AppSwitchUtils.getOrgForDevice(mMockActivity, mRetrieverContext, null, null);
        dataRetriever.getData(IDataRetriever.DataArgs.NONE);
    }

    @Test
    public void getOrgForDevice_onResponse_orgIdNull() {
        final IDataRetriever dataRetriever = AppSwitchUtils.getOrgForDevice(mMockActivity, mRetrieverContext, null, null);
        final CernResponse response = new CernResponse();
        response.data = new OrgForDevice();
        dataRetriever.onResponse(response);
    }

    @Test
    public void getOrgForDevice_onResponse_orgId0() {
        final IDataRetriever dataRetriever = AppSwitchUtils.getOrgForDevice(mMockActivity, mRetrieverContext, null, null);
        final OrgForDevice orgForDevice = new OrgForDevice();
        orgForDevice.org_id = "0";
        final CernResponse response = new CernResponse();
        response.data = orgForDevice;
        dataRetriever.onResponse(response);
    }

    @Test
    public void getOrgForDevice_onResponse() {
        final IDataRetriever dataRetriever = AppSwitchUtils.getOrgForDevice(mMockActivity, mRetrieverContext, null, null);
        final OrgForDevice orgForDevice = new OrgForDevice();
        orgForDevice.org_id = "SomeOrgID";
        final CernResponse response = new CernResponse();
        response.data = orgForDevice;
        dataRetriever.onResponse(response);
    }

    @Test
    public void getOrgForDevice_onNoContentResponse() {
        mMockActivity = Mockito.spy(mMockActivity);
        final IDataRetriever dataRetriever = AppSwitchUtils.getOrgForDevice(mMockActivity, mRetrieverContext, null, null);
        ActiveModuleManager.setActiveModule(TestActivity.class, 0, null);
        dataRetriever.onNoContentResponse(null, null);
        assertEquals(SettingsActivity.class, ActiveModuleManager.getActiveModule());
    }

    @Test
    public void getOrgForDevice_onErrorResponse() {
        mMockActivity = Mockito.spy(mMockActivity);
        final IDataRetriever dataRetriever = AppSwitchUtils.getOrgForDevice(mMockActivity, mRetrieverContext, null, null);
        ActiveModuleManager.setActiveModule(TestActivity.class, 0, null);
        dataRetriever.onErrorResponse(null, null);
        assertEquals(SettingsActivity.class, ActiveModuleManager.getActiveModule());
    }

    @Test
    public void getOrgForDevice_onFailedResponse() {
        mMockActivity = Mockito.spy(mMockActivity);
        final IDataRetriever dataRetriever = AppSwitchUtils.getOrgForDevice(mMockActivity, mRetrieverContext, null, null);
        final DialogController dialogController = Mockito.mock(DialogController.class);
        mMockActivity.setDialogs(dialogController);
        Mockito.doReturn(null).when(dialogController).showError(Mockito.any(), Mockito.any(),
                                            Mockito.any(), Mockito.any(DialogInterface.OnClickListener.class),
                                            Mockito.isNull(), Mockito.isNull(),
                                            Mockito.isNull(), Mockito.isNull());

        dataRetriever.onFailedResponse(null, false);
        ActiveModuleManager.setActiveModule(TestActivity.class, 0, null);

        final ArgumentCaptor<DialogInterface.OnClickListener> captor = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        Mockito.verify(dialogController).showError(Mockito.any(), Mockito.any(),
                                            Mockito.any(), captor.capture(),
                                            Mockito.isNull(), Mockito.isNull(),
                                            Mockito.isNull(), Mockito.isNull());
        captor.getValue().onClick(Mockito.mock(DialogInterface.class), 0);
        assertEquals(PatientListActivity.class, ActiveModuleManager.getActiveModule());
    }

    @Test
    public void getOrgForDevice_emptyMethod() {
        final IDataRetriever dataRetriever = AppSwitchUtils.getOrgForDevice(mMockActivity, mRetrieverContext, null, null);
        dataRetriever.setActionBarWaitCursor(true);
        assertFalse(dataRetriever.cancelRequest(null));
        assertTrue(dataRetriever.backgroundAfterCancel(null));
        dataRetriever.setRequestMethod(0, null);
    }

    @Test
    public void getOrgForDevice_processResponse_false() {
        mMockActivity = Mockito.spy(mMockActivity);
        Mockito.doReturn(false).when(mRetrieverContext).processResponses();
        final IDataRetriever dataRetriever = AppSwitchUtils.getOrgForDevice(mMockActivity, mRetrieverContext, null, null);
        final CernResponse response = new CernResponse();
        response.data = new OrgForDevice();
        dataRetriever.onResponse(response);
        dataRetriever.onErrorResponse(null, Class.class);
        dataRetriever.onFailedResponse(Class.class, true);
        dataRetriever.onNoContentResponse(null, Class.class);
        Mockito.verify(mMockActivity, Mockito.never()).getDialogs();
        assertFalse(dataRetriever.cancelRequest(Mockito.mock(CernRequest.class)));
    }

    @Test
    public void getOrgForDevice_Deallocated() {
        mMockActivity = Mockito.spy(mMockActivity);
        final IDataRetriever dataRetriever = AppSwitchUtils.getOrgForDevice(mMockActivity, null, null, null);
        final CernResponse response = new CernResponse();
        response.data = new OrgForDevice();
        dataRetriever.onResponse(response);
        dataRetriever.onErrorResponse(null, Class.class);
        dataRetriever.onFailedResponse(Class.class, true);
        dataRetriever.onNoContentResponse(null, Class.class);
        Mockito.verify(mMockActivity, Mockito.never()).getDialogs();

        TestUtils.setVariable(dataRetriever, "mWeakActivity", Mockito.mock(WeakReference.class));
        dataRetriever.getData(null);
        final CernResponse response2 = new CernResponse();
        response.data = new OrgForDevice();
        dataRetriever.onResponse(response2);
        dataRetriever.onErrorResponse(null, Class.class);
        dataRetriever.onFailedResponse(Class.class, true);
        dataRetriever.onNoContentResponse(null, Class.class);
    }

    @Test
    public void getIdFromUrn() {
        assertNull(TestUtils.invokePrivateStaticMethod(AppSwitchUtils.class, "getIdFromUrn", new Class[]{String.class}, (String) null));
        assertEquals("", TestUtils.invokePrivateStaticMethod(AppSwitchUtils.class, "getIdFromUrn", new Class[]{String.class}, ""));
        assertEquals("1234", TestUtils.invokePrivateStaticMethod(AppSwitchUtils.class, "getIdFromUrn", new Class[]{String.class}, "1234"));
        assertEquals("23412", TestUtils.invokePrivateStaticMethod(AppSwitchUtils.class, "getIdFromUrn", new Class[]{String.class}, "1lkjd-dfa:23412"));
        assertEquals("23412", TestUtils.invokePrivateStaticMethod(AppSwitchUtils.class, "getIdFromUrn", new Class[]{String.class}, ";dsdsa:1lkjd-dfa:23412"));
    }
}