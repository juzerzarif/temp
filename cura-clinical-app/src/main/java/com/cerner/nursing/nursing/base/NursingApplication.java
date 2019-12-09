package com.cerner.nursing.nursing.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.StrictMode;

import com.cerner.cura.base.CuraAppBuildConfig;
import com.cerner.cura.base.CuraAuthnApplication;
import com.cerner.cura.device_association.scanning.CuraDeviceAssociationScannedProcessor;
import com.cerner.cura.medications.scanning.CuraMedicationScannedProcessor;
import com.cerner.cura.scanning.CuraPrinterScannedProcessor;
import com.cerner.cura.scanning.CuraPrsnlScannedProcessor;
import com.cerner.cura.scanning.PatientScannedProcessor;
import com.cerner.cura.scanning.ScanCategory;
import com.cerner.cura.scanning.ScanManager;
import com.cerner.cura.ui.elements.IListItem;
import com.cerner.cura.ui.elements.drawer.DrawerFragment;
import com.cerner.ion.log.Logger;
import com.cerner.nursing.collections.android.scanning.SpecimenCollectionsScannedProcessor;
import com.cerner.nursing.nursing.BuildConfig;
import com.cerner.nursing.nursing.ui.DeviceAssociationActivity;
import com.cerner.nursing.nursing.ui.MedsAdminActivity;
import com.cerner.nursing.nursing.ui.PatientListActivity;
import com.cerner.nursing.nursing.ui.SpecimenCollectActivity;
import com.newrelic.agent.android.NewRelic;

import java.util.List;

/**
 * @author Lam Tran (lt028506) on 7/14/14.
 * @author Mark Lear (ML015922)
 */
public class NursingApplication extends CuraAuthnApplication implements DrawerFragment.DrawerManager {
    @Override
    public void onCreate() {
        CuraAppBuildConfig.initCuraAppBuildConfig(BuildConfig.class);

        if (BuildConfig.DEBUG) {
            Logger.d(NursingApplication.class.getSimpleName(), "Strict mode is on");
            //Don't like permitting disk reads/writes on the main thread, but the security team requires it
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().permitDiskReads().permitDiskWrites().penaltyLog().penaltyDialog().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
        }

        super.onCreate();
        NewRelic.withApplicationToken((String)CuraAppBuildConfig.getValue("NEW_RELIC_APP_TOKEN")).start(this);
//        LeakCanary.install(this);
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_PATIENT, new PatientScannedProcessor());
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_DEVICE, new CuraDeviceAssociationScannedProcessor(DeviceAssociationActivity.class, PatientListActivity.REQUESTCODE_DEVICEASSOCIATION_DEFAULT));
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_MEDICATION, new CuraMedicationScannedProcessor(MedsAdminActivity.class, PatientListActivity.REQUESTCODE_MEDSADMIN_DEFAULT));
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_PRSNL, new CuraPrsnlScannedProcessor());
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_SPECIMEN, new SpecimenCollectionsScannedProcessor(SpecimenCollectActivity.class, PatientListActivity.REQUESTCODE_SPECCOL_DEFAULT));
        ScanManager.setScanProcessor(ScanCategory.SCAN_CATEGORY_PRINTER, new CuraPrinterScannedProcessor());
    }

    @SuppressLint ("SwitchIntDef")
    @Override
    public void onTrimMemory(final int level) {
        super.onTrimMemory(level);

        //Reference https://developer.android.com/reference/android/content/ComponentCallbacks2.html
        switch (level) {
            case TRIM_MEMORY_UI_HIDDEN:
            case TRIM_MEMORY_BACKGROUND:
                ScanManager.endScanning();
                break;
        }
    }

    @Override
    public List<IListItem> getDrawerItems(final Activity activity) {
        return NursingDrawerManager.getDrawerItems(this);
    }
}