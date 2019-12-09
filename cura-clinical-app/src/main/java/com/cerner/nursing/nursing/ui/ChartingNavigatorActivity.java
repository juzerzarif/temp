package com.cerner.nursing.nursing.ui;

import com.cerner.cura.charting.navigator.ui.ChartingNavigatorFragment;
import com.cerner.cura.scanning.BarcodeAcceptanceTypeContext;
import com.cerner.cura.scanning.ScanCategory;

/**
 * Shows the charting navigator screen for the selected patient.
 *
 * @author Charlotte Clark (cc035054)
 */
public class ChartingNavigatorActivity extends ContentActivity {
    public ChartingNavigatorActivity() {
        super(ChartingNavigatorFragment.class, null);
        TAG = ChartingNavigatorActivity.class.getSimpleName();
    }

    @Override
    public void onScanReset() {
        BarcodeAcceptanceTypeContext.enableScanningProcessors(ScanCategory.MAX_ALLOWED_SCAN_CATEGORIES);
    }
}