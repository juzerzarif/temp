package com.cerner.nursing.nursing.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.cerner.cura.base.PatientContext;
import com.cerner.cura.requestor.MockDataManager;
import com.cerner.cura.scanning.BarcodeAcceptanceTypeContext;
import com.cerner.cura.scanning.ScanCategory;
import com.cerner.cura.scanning.ScanManager;
import com.cerner.cura.scanning.ScanProcessor;
import com.cerner.cura.test.helper.TestUtils;
import com.cerner.cura.utils.PatientRelationshipExpiredHandlerUtil;
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

import androidx.test.core.app.ApplicationProvider;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class SpecimenCollectActivityTest {

    private ActivityController<SpecimenCollectActivity> mActivityController;
    private SpecimenCollectActivity mMockActivity;
    private DrawerLayout mLayoutView;

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
        mActivityController = Robolectric.buildActivity(SpecimenCollectActivity.class).create().postCreate(null);
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
    public void onScanReset() {
        final ScanProcessor scanProcessor = Mockito.mock(ScanProcessor.class);
        Mockito.doReturn(true).when(scanProcessor).isEnabled(Mockito.any(Context.class));
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_MEDICATION, scanProcessor);
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_PATIENT, scanProcessor);
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_DEVICE, scanProcessor);
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_SPECIMEN, scanProcessor);
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_PRSNL, scanProcessor);
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_PRINTER, scanProcessor);

        BarcodeAcceptanceTypeContext.setIonActivity(mMockActivity, ScanCategory.SCAN_CATEGORY_NOT_ALLOWED);
        mMockActivity.onScanReset();
        assertTrue(BarcodeAcceptanceTypeContext.isProcessorScanningEnabled(ScanCategory.SCAN_CATEGORY_PATIENT));
        assertTrue(BarcodeAcceptanceTypeContext.isProcessorScanningEnabled(ScanCategory.SCAN_CATEGORY_MEDICATION));
        assertTrue(BarcodeAcceptanceTypeContext.isProcessorScanningEnabled(ScanCategory.SCAN_CATEGORY_PRSNL));
        assertTrue(BarcodeAcceptanceTypeContext.isProcessorScanningEnabled(ScanCategory.SCAN_CATEGORY_SPECIMEN));
        assertTrue(BarcodeAcceptanceTypeContext.isProcessorScanningEnabled(ScanCategory.SCAN_CATEGORY_DEVICE));
        assertTrue(BarcodeAcceptanceTypeContext.isProcessorScanningEnabled(ScanCategory.SCAN_CATEGORY_PRINTER));
    }


    @Test
    public void activityIsNotNull() {
        assertNotNull(mMockActivity);
    }

}