package com.cerner.nursing.nursing.utils;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.cerner.cura.datamodel.OrgForDevice;
import com.cerner.cura.demographics.utils.DemogUtils;
import com.cerner.cura.ppr.utils.PPRUtils;
import com.cerner.cura.requestor.CuraResponseListener;
import com.cerner.cura.requestor.IDataRetriever;
import com.cerner.cura.requestor.IRetrieverContext;
import com.cerner.cura.requestor.JsonRequestor;
import com.cerner.cura.requestor.MockDataManager;
import com.cerner.cura.utils.ActiveModuleManager;
import com.cerner.cura.utils.RequestorUtils;
import com.cerner.ion.log.Logger;
import com.cerner.ion.provisioning.ProvisionedTenant;
import com.cerner.ion.request.CernRequest;
import com.cerner.ion.request.CernResponse;
import com.cerner.ion.security.IonActivity;
import com.cerner.ion.security.IonAuthnActivity;
import com.cerner.ion.session.IonAuthnSessionUtils;
import com.cerner.ion.session.User;
import com.cerner.nursing.nursing.R;
import com.cerner.nursing.nursing.ui.PatientChartActivity;
import com.cerner.nursing.nursing.ui.PatientListActivity;
import com.cerner.nursing.nursing.ui.PatientListFragment;
import com.cerner.nursing.nursing.ui.SettingsActivity;
import com.google.gson.Gson;

import java.lang.ref.WeakReference;

/**
 * Utilities for switching applications.
 *
 * @author Brad Barnhill (BB024928)
 * @author Lam Tran (LT028506)
 * @author Mark Lear (ML015922)
 */
public final class AppSwitchUtils {
    private static final String TAG = AppSwitchUtils.class.getSimpleName();
    private static final String APP_SWITCHING_INTENT_ACTION = "com.cerner.nursing.nursing.OPEN_PATIENT";
    private static final String CHECKPOINT_ORG_FOR_DEVICE_READ = "CURA_NURSING_ORG_FOR_DEVICE_READ";
    private static final String ZERO = "0";
    private static final String URN_DELIMITER = ":";
    private static Intent smStoredIntent;

    private static class OrgForDeviceRetriever implements IDataRetriever {
        private static final String TAG = OrgForDeviceRetriever.class.getSimpleName();
        private final WeakReference<IonActivity> mWeakActivity;
        private final WeakReference<IRetrieverContext> mWeakRetrieverContext;
        private final String mPatientId;
        private final String mEncounterId;

        public OrgForDeviceRetriever(@NonNull final IonActivity activity, @NonNull final IRetrieverContext retrieverContext, @NonNull final String patientId, @NonNull final String encounterId) {
            mWeakActivity = new WeakReference<>(activity);
            mWeakRetrieverContext = new WeakReference<>(retrieverContext);
            mPatientId = patientId;
            mEncounterId = encounterId;
        }

        @Override
        public void getData(final DataArgs dataArgs) {
            final IonActivity activity = mWeakActivity.get();
            if (activity == null) {
                Logger.d(TAG, "Cannot request org for device due to activity cleanup.");
                return;
            }

            final CuraResponseListener<OrgForDevice> responseListener = new CuraResponseListener<>(RequestorUtils.showProgressDialog(activity, this), TAG,
                                                                                                   CHECKPOINT_ORG_FOR_DEVICE_READ, OrgForDevice.class, this, activity, false);
            JsonRequestor.sendRequest(responseListener, null, 0, false, null);
        }

        @Override
        public void onResponse(@NonNull final CernResponse response) {
            final IonActivity activity = mWeakActivity.get();
            final IRetrieverContext retrieverContext = mWeakRetrieverContext.get();
            if (activity == null || retrieverContext == null || !retrieverContext.processResponses()) {
                Logger.d(TAG, "onResponse(): processResponses call returned false or context out of scope.");
                return;
            }

            final OrgForDevice orgForDevice = (OrgForDevice) response.data;

            if (TextUtils.isEmpty(orgForDevice.org_id) || ZERO.equals(orgForDevice.org_id)) {
                onNoContentResponse(null, OrgForDevice.class);
                return;
            }

            smStoredIntent = null;

            // if there is org associated to the device already, load patient information
            PPRUtils.loadPatientInformation(activity, retrieverContext, PatientChartActivity.class, PatientListActivity.REQUESTCODE_PATIENTCHART_DEFAULT,
                                            R.string.unable_to_open_patient_summary_title, R.string.unable_to_open_patient_summary, null, mPatientId, mEncounterId, null,
                                            new DemogUtils.OnMessageClosedListener() {
                                                @Override
                                                public void onMessageAccepted() {
                                                    final IonActivity finishActivity = mWeakActivity.get();
                                                    if (finishActivity == null) {
                                                        Logger.d(TAG, "Cannot finish activity due to activity cleanup.");
                                                        return;
                                                    }
                                                    finishActivity.finish();
                                                }

                                                @Override
                                                public void onMessageCanceled() {
                                                    final IonActivity finishActivity = mWeakActivity.get();
                                                    if (finishActivity == null) {
                                                        Logger.d(TAG, "Cannot finish activity due to activity cleanup.");
                                                        return;
                                                    }
                                                    finishActivity.finish();
                                                }
                                            });
        }

        @Override
        public void onNoContentResponse(@SuppressWarnings ("NullableProblems") final CernResponse response, final Class clazz) {
            final IonActivity activity = mWeakActivity.get();
            final IRetrieverContext retrieverContext = mWeakRetrieverContext.get();
            if (activity == null || retrieverContext == null || !retrieverContext.processResponses()) {
                Logger.d(TAG, "onNoContentResponse(): processResponses call returned false or context out of scope.");
                return;
            }

            // if there is no org associated to the device, launch org selected screen and finish this activity
            final Bundle bundle = new Bundle();
            bundle.putInt(SettingsActivity.LAUNCH_ACTION_IDENTIFIER, SettingsActivity.LAUNCH_PICK_ORG);
            ActiveModuleManager.gotoModule(activity, SettingsActivity.class, PatientListFragment.REQUESTCODE_SETTINGS_ORG_SELECT, bundle);
        }

        @Override
        public void onErrorResponse(@NonNull final VolleyError volleyError, final Class clazz) {
            // there is no org associated to the device
            onNoContentResponse(null, clazz);
        }

        @Override
        public void onFailedResponse(final Class clazz, final boolean cacheReturned) {
            final IonActivity activity = mWeakActivity.get();
            final IRetrieverContext retrieverContext = mWeakRetrieverContext.get();
            if (activity == null || retrieverContext == null || !retrieverContext.processResponses()) {
                Logger.d(TAG, "onFailedResponse(): processResponses call returned false or context out of scope.");
                return;
            }

            activity.getDialogs().showError(activity.getString(R.string.unable_to_open_patient_summary_title), activity.getString(R.string.unable_to_open_patient_summary), activity.getString(
                    R.string.close), (dialog, which) -> {
                // TODO: LT028506 This flow is not ideal, it should finish the current activity and go back to the other app.
                ActiveModuleManager.gotoModule(activity, PatientListActivity.class, PatientListActivity.REQUESTCODE_PATIENTLIST_DEFAULT);
            }, null, null, null, null);
        }

        @Override
        public void setActionBarWaitCursor(final boolean isVisible) {
            //Do nothing since this isn't ever going to use cache
        }

        @Override
        public boolean cancelRequest(@NonNull final CernRequest request) {
            return false;
        }

        @Override
        public boolean backgroundAfterCancel(@NonNull final CernRequest request) {
            return true;
        }

        @Override
        public void setRequestMethod(final int requestMethod, final String tag) {
            //Do nothing since this doesn't care about the method
        }
    }

    private AppSwitchUtils() {
    }

    /**
     * Reads the intent and verifies if the intent is valid.
     *
     * @param intent intent started activity
     * @return {@link Boolean} if appswitching returns true, otherwise false.
     */
    public static boolean performAppSwitch(final IonAuthnActivity activity, final IRetrieverContext retrieverContext, final Intent intent) {
        if (((intent == null || !APP_SWITCHING_INTENT_ACTION.equals(intent.getAction())) && smStoredIntent == null)) {
            return false;
        }

        boolean newIntentInd = false;
        if (intent != null) {
            smStoredIntent = intent;
            newIntentInd = true;
        }

        setReadMockFlag(smStoredIntent);
        final String provisionedTenant = smStoredIntent.getStringExtra("tenant");
        final String patientId = getIdFromUrn(smStoredIntent.getStringExtra("patient_id"));
        final String encounterId = getIdFromUrn(smStoredIntent.getStringExtra("encounter_id"));
        final String userName = smStoredIntent.getStringExtra("username");
        final String principal = smStoredIntent.getStringExtra("principal");

        ProvisionedTenant tenant = null;

        if (!TextUtils.isEmpty(provisionedTenant)) {
            try {
                tenant = new Gson().fromJson(provisionedTenant, ProvisionedTenant.class);
            } catch (final RuntimeException e) {
                Logger.e(TAG, "Fail to read json in intent from app switch" + e.getMessage());
            }
        }

        final User loggedInUser = IonAuthnSessionUtils.getUser();
        final boolean useUsername = TextUtils.isEmpty(principal);
        if (tenant == null || (useUsername && TextUtils.isEmpty(userName)) || TextUtils.isEmpty(patientId) || TextUtils.isEmpty(encounterId)) {
            // if the data received contain errors, show the error message
            activity.getDialogs().showError(activity.getString(R.string.unable_to_open_patient_summary_title), activity.getString(R.string.unable_to_open_patient_summary),
                                            activity.getString(R.string.close), (dialog, which) -> {
                        //TODO bb024928: should go back to previous application but just goes back to patient list for now
                        activity.finish();
                    }, null, null, null, null);
            smStoredIntent = null;
            return false;
        } else if ((loggedInUser == null || !activity.getAuthenticationManager().isLoggedIn() || !tenant.equals(IonAuthnSessionUtils.getTenant())
                    || (!useUsername && !principal.equals(loggedInUser.getOpenId()))
                    || (useUsername && !userName.equals(loggedInUser.getUsername())))
                   && !MockDataManager.getReadMockDataFlag()) {
            //user not logged in or different user

            if (!newIntentInd) {
                //cancel appswitch due to logging in with different user than app switch was begun with
                smStoredIntent = null;
                return false;
            }

            if (IonAuthnSessionUtils.getAuthnResponse() != null) {
                // if the user is logged in, log them out
                activity.getAuthenticationManager().logout(activity);
            }

            IonAuthnSessionUtils.setTenant(tenant);
        } else {
            // at this point, the logged in user should be the same as the one in intent
            getOrgForDevice(activity, retrieverContext, patientId, encounterId).getData(IDataRetriever.DataArgs.FORCE_REFRESH);
        }

        return true;
    }

    /**
     * Reads the flag from intent and set mock data accordingly
     *
     * @param intent intent started activity
     */
    private static void setReadMockFlag(final Intent intent) {
        MockDataManager.isMockDataForced(); //Ignore response, initializes the state to READ, if set
        if (intent.getBooleanExtra("readMock", false)) {
            MockDataManager.setMockDataStatus(MockDataManager.MockState.READ);
        }
    }

    /**
     * Will request the org that is associated to this device from the server. If associated will continue onto the patient ppr checks, if not it will navigate to the org select screen.
     *
     * @param activity used to show dialogs and cursors
     * @param retrieverContext context to be used to determine if the org read should continue to process
     * @param patientId patient id
     * @param encounterId encouter id
     * @return IDataRetriever object to get organization for device
     */
    static IDataRetriever getOrgForDevice(@NonNull final IonActivity activity, @NonNull final IRetrieverContext retrieverContext, @NonNull final String patientId, @NonNull final String encounterId) {
        return new OrgForDeviceRetriever(activity, retrieverContext, patientId, encounterId);
    }

    /**
     * Gets the correct M+ id from the urn.
     *
     * @param item urn passed in
     * @return the M+ id from the urn
     */
    private static String getIdFromUrn(final String item) {
        if (item == null) {
            return null;
        }

        final String[] tokens = item.split(URN_DELIMITER);
        return tokens[tokens.length - 1];
    }
}
