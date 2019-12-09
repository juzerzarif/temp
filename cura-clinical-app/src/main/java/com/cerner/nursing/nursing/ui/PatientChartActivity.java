package com.cerner.nursing.nursing.ui;

import com.cerner.cura.scanning.BarcodeAcceptanceTypeContext;
import com.cerner.cura.scanning.ScanCategory;

/**
 * Shows the patient selected
 *
 * @author Mark Lear (ML015922)
 */
public class PatientChartActivity extends ContentActivity {
    public PatientChartActivity() {
        super(PatientSummaryFragment.class, null);
        TAG = PatientChartActivity.class.getSimpleName();
    }

    @Override
    public void onScanReset() {
        BarcodeAcceptanceTypeContext.enableScanningProcessors(ScanCategory.MAX_ALLOWED_SCAN_CATEGORIES);
    }
}
