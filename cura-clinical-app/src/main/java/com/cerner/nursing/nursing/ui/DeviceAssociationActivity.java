package com.cerner.nursing.nursing.ui;

import android.app.Fragment;

import com.cerner.cura.device_association.scanning.CuraDeviceAssociationScannedProcessor;
import com.cerner.cura.device_association.ui.AssociationSummaryFragment;
import com.cerner.cura.scanning.BarcodeAcceptanceTypeContext;
import com.cerner.cura.scanning.ScanCategory;

/**
 * Shows the device association screen for the selected patient.
 *
 * @author Mark Lear (ML015922)
 */
public class DeviceAssociationActivity extends ContentActivity {
    public DeviceAssociationActivity() {
        super(getAssociationSummaryFragment(), null);
        TAG = DeviceAssociationActivity.class.getSimpleName();
    }

    @Override
    public void onScanReset() {
        BarcodeAcceptanceTypeContext.enableScanningProcessors(ScanCategory.SCAN_CATEGORY_NOT_ALLOWED);
    }

    private static Class<? extends Fragment> getAssociationSummaryFragment() {
        if (CuraDeviceAssociationScannedProcessor.isMicroserviceDeviceAssociation()) {
            return AssociationSummaryFragment.class;
        }

        return com.cerner.cura.device_association.legacy.ui.AssociationSummaryFragment.class;
    }
}
