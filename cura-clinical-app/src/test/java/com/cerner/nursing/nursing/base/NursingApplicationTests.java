package com.cerner.nursing.nursing.base;

import android.content.ComponentCallbacks2;

import com.cerner.cura.scanning.ScanCategory;
import com.cerner.cura.scanning.ScanManager;
import com.cerner.cura.test.helper.TestUtils;
import com.cerner.cura.utils.ActiveModuleManager;
import com.cerner.nursing.nursing.ui.PatientListActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;

import static org.junit.Assert.assertNotNull;

/**
 * @author Lam Tran (lt028506) on 7/22/14.
 */
@RunWith (RobolectricTestRunner.class)
public class NursingApplicationTests {
    private PatientListActivity mActivity;
    private NursingApplication mApplication;

    @BeforeClass
    public static void ClassSetup() {
        System.gc();
    }

    @Before
    public void setup() {
        mActivity = Robolectric.buildActivity(PatientListActivity.class).create().get();
        mApplication = (NursingApplication) mActivity.getApplication();
    }

    @After
    public void tearDown() {
        mActivity = null;
        mApplication = null;
    }

    @Test
    public void onCreate() {
        mApplication.onCreate();
        assertNotNull(ScanManager.getScanProcessor(ScanCategory.SCAN_CATEGORY_PATIENT));
        assertNotNull(ScanManager.getScanProcessor(ScanCategory.SCAN_CATEGORY_DEVICE));
        assertNotNull(ScanManager.getScanProcessor(ScanCategory.SCAN_CATEGORY_MEDICATION));
        assertNotNull(ScanManager.getScanProcessor(ScanCategory.SCAN_CATEGORY_PRSNL));
        assertNotNull(ScanManager.getScanProcessor(ScanCategory.SCAN_CATEGORY_SPECIMEN));
        assertNotNull(ScanManager.getScanProcessor(ScanCategory.SCAN_CATEGORY_PRINTER));
    }

    @Test
    public void onTrimMemory() {
        //Disconnect scanning service
        mApplication.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN);
        mApplication.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND);

        //Don't do anything
        mApplication.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE);
        mApplication.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL);
        mApplication.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE);
        mApplication.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW);
        mApplication.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_MODERATE);
    }

    @Test
    public void getDrawerItems() {
        assertNotNull(mApplication.getDrawerItems(mActivity));
        final ArrayList<ActiveModuleManager.ActiveModuleListener> listeners = TestUtils.getStaticVariable(ActiveModuleManager.class, "smActiveModuleListeners");
        listeners.clear();
    }
}
