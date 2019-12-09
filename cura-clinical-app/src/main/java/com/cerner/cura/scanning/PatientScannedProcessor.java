package com.cerner.cura.scanning;

import com.cerner.cura.base.PatientContext;
import com.cerner.cura.ppr.utils.PPRUtils;
import com.cerner.cura.requestor.IRetrieverContext;
import com.cerner.cura.scanning.datamodel.ScanViewResponse;
import com.cerner.cura.utils.ActiveModuleManager;
import com.cerner.ion.log.Logger;
import com.cerner.ion.security.IonActivity;
import com.cerner.nursing.nursing.R;
import com.cerner.nursing.nursing.ui.PatientChartActivity;
import com.cerner.nursing.nursing.ui.PatientListActivity;

/**
 * This processor will receive the barcode from the scan manager and verifying barcode as well as navigate to the proper module.
 *
 * @author Lam Tran (lt028506) on 07/31/2014.
 * @author Mark Lear (ml015922) on 12/03/2014.
 */
public class PatientScannedProcessor extends CuraPatientScannedProcessor {
    private static final String TAG = PatientScannedProcessor.class.getSimpleName();

    public PatientScannedProcessor() {
        super();
    }

    @Override
    public void onBarcodeReceived(final IonActivity activity, final ScanViewResponse scanViewResponse) {
        if (activity == null) {
            Logger.d(TAG, "activity is null");
            return;
        }

        // verify if the barcode is a patient barcode and the scanned patient is the same as the patient in context
        if (verifyPatientBarcode(scanViewResponse)) {
            final OnCurrentPatientScannedConfirmationListener confirmationListener = getConfirmationListener();
            if (PatientContext.isEncounterIdSet() && PatientContext.isPatientIdSet()
                && PatientContext.getEncounterId().equals(scanViewResponse.patientResponse.encounterId)
                && PatientContext.getPatientId().equals(scanViewResponse.patientResponse.id)) {
                //Scanned patient is the same as the current patient
                if (confirmationListener != null) {
                    confirmationListener.onCurrentPatientScannedConfirmation();
                } else if (PatientContext.isLatestModuleSet()) {
                    ActiveModuleManager.gotoModule(activity, PatientContext.getLatestModuleClass(), PatientContext.getLatestModuleRequestcode());
                } else {
                    ActiveModuleManager.gotoModule(activity, PatientChartActivity.class, PatientListActivity.REQUESTCODE_PATIENTCHART_DEFAULT);
                }
            } else {
                PPRUtils.loadPatientInformation(activity, (IRetrieverContext) activity, PatientChartActivity.class, PatientListActivity.REQUESTCODE_PATIENTCHART_DEFAULT,
                        R.string.unable_to_open_patient_summary_title, R.string.unable_to_open_patient_summary, null,
                        scanViewResponse.patientResponse.id, scanViewResponse.patientResponse.encounterId, scanViewResponse.contextId, null);
            }
        } else {
            //TODO ml015922: need error message for an invalid patient scan
        }
    }
}