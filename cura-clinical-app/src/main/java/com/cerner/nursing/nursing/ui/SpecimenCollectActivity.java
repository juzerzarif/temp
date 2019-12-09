package com.cerner.nursing.nursing.ui;

import com.cerner.cura.scanning.BarcodeAcceptanceTypeContext;
import com.cerner.cura.scanning.ScanCategory;
import com.cerner.nursing.collections.android.ui.CollectionsFragment;

/**
 * Shows the specimen collect screen for the selected patient.
 */
public class SpecimenCollectActivity extends ContentActivity {
    public SpecimenCollectActivity() {
        super(CollectionsFragment.class, null);
        TAG = SpecimenCollectActivity.class.getSimpleName();
    }

    @Override
    public void onScanReset() {
        BarcodeAcceptanceTypeContext.enableScanningProcessors(ScanCategory.MAX_ALLOWED_SCAN_CATEGORIES);
    }
}
