package com.cerner.nursing.nursing.ui;

import android.app.Fragment;
import android.content.Intent;
import android.content.pm.ResolveInfo;

import com.cerner.cura.base.PatientContext;
import com.cerner.cura.requestor.MockDataManager;
import com.cerner.cura.test.helper.TestUtils;
import com.cerner.cura.utils.PatientRelationshipExpiredHandlerUtil;
import com.cerner.ion.session.AuthnResponse;
import com.cerner.ion.session.Capabilities;
import com.cerner.ion.session.IonAuthnSessionUtils;
import com.cerner.ion.session.SessionCheckHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

import java.util.Collections;
import java.util.HashMap;

import androidx.test.core.app.ApplicationProvider;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests {@link ContentActivity}.
 *
 * @author Nan Ma (NM049374) on 4/20/2018.
 */
@RunWith (RobolectricTestRunner.class)
public class ContentActivityTests {
    private PatientChartActivity mMockActivity;
    private static final String FEATURE_USE_MICROSERVICES = "android/use_microservices";

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

        mMockActivity = Robolectric.buildActivity(PatientChartActivity.class).create().start().get();
    }

    /**
     * Clean up fragment and mock activity
     */
    @After
    public void tearDown() {
        mMockActivity = null;
        PatientRelationshipExpiredHandlerUtil.setPatientRelationshipExpiredHandler(null);
    }

    @Test
    public void onPause_UnregisterRelationshipHandler() {
        TestUtils.setVariable(mMockActivity, "mCurrentFragment", Fragment.class);
        mMockActivity.onPause();
        assertNull(PatientRelationshipExpiredHandlerUtil.getPatientRelationshipExpiredHandler());
    }

    @Test
    public void onResume_RegisterRelationshipHandler() {
        TestUtils.setVariable(mMockActivity, "mCurrentFragment", Fragment.class);
        TestUtils.invokePrivateMethod(mMockActivity, "onResume");

        assertNotNull(PatientRelationshipExpiredHandlerUtil.getPatientRelationshipExpiredHandler());
    }

    @Test
    public void onResume_RegisterRelationshipHandler_noRelationship() {
        PatientContext.setHasRelationship(false);
        PatientContext.setHasAccess(true);
        TestUtils.setVariable(mMockActivity, "mCurrentFragment", Fragment.class);
        TestUtils.invokePrivateMethod(mMockActivity, "onResume");

        assertNotNull(PatientRelationshipExpiredHandlerUtil.getPatientRelationshipExpiredHandler());
    }

    @Test
    public void onResume_RegisterRelationshipHandler_noAccess() {
        PatientContext.setHasRelationship(true);
        PatientContext.setHasAccess(false);
        TestUtils.setVariable(mMockActivity, "mCurrentFragment", Fragment.class);
        TestUtils.invokePrivateMethod(mMockActivity, "onResume");

        assertNotNull(PatientRelationshipExpiredHandlerUtil.getPatientRelationshipExpiredHandler());
    }

    @Test
    public void onResume_RegisterRelationshipHandler_noContext() {
        PatientContext.setHasRelationship(true);
        PatientContext.setHasAccess(true);
        enableMicroservices();
        TestUtils.setVariable(mMockActivity, "mCurrentFragment", Fragment.class);
        TestUtils.invokePrivateMethod(mMockActivity, "onResume");

        assertNotNull(PatientRelationshipExpiredHandlerUtil.getPatientRelationshipExpiredHandler());
    }

    private static void enableMicroservices() {
        final HashMap<String, Boolean> map = new HashMap<>();
        map.put(FEATURE_USE_MICROSERVICES, true);
        final Capabilities capabilities = Capabilities.newCapabilities(Collections.singletonList("com.cerner.nursing.cura.dev"), 0, 0, 0, "OxMOCKTENANTGUIDTHATM-EANSNOTHIN", false, true);
        final AuthnResponse response = AuthnResponse.newAuthnResponse(null, capabilities, map, SessionCheckHandler.Status.unlocked.name());
        IonAuthnSessionUtils.setAuthnResponse(ApplicationProvider.getApplicationContext(), response);
    }
}
