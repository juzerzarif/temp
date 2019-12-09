package com.cerner.nursing.nursing.base;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Pair;

import com.android.volley.VolleyError;
import com.cerner.cura.base.PatientContext;
import com.cerner.cura.charting.navigator.utils.Utils;
import com.cerner.cura.demographics.datamodel.PatientDemogBanner;
import com.cerner.cura.device_association.scanning.CuraDeviceAssociationScannedProcessor;
import com.cerner.cura.medications.scanning.CuraMedicationScannedProcessor;
import com.cerner.cura.requestor.CuraResponseListener;
import com.cerner.cura.requestor.IDataRetriever;
import com.cerner.cura.requestor.JsonRequestor;
import com.cerner.cura.ui.elements.DrawerPatientListItem;
import com.cerner.cura.ui.elements.IListItem;
import com.cerner.cura.ui.elements.drawer.DrawerListItem;
import com.cerner.cura.utils.ActiveModuleManager;
import com.cerner.cura.utils.ActivityUtils;
import com.cerner.cura.utils.CapabilitiesAndFeatures;
import com.cerner.ion.request.CernRequest;
import com.cerner.ion.request.CernResponse;
import com.cerner.ion.security.IonActivity;
import com.cerner.ion.security.IonApplication;
import com.cerner.ion.session.AuthnResponse;
import com.cerner.ion.session.IonAuthnSessionUtils;
import com.cerner.nursing.collections.android.scanning.SpecimenCollectionsScannedProcessor;
import com.cerner.nursing.nursing.R;
import com.cerner.nursing.nursing.ui.CareTeamActivity;
import com.cerner.nursing.nursing.ui.DeviceAssociationActivity;
import com.cerner.nursing.nursing.ui.MedsAdminActivity;
import com.cerner.nursing.nursing.ui.PatientChartActivity;
import com.cerner.nursing.nursing.ui.PatientListActivity;
import com.cerner.nursing.nursing.ui.SettingsActivity;
import com.cerner.nursing.nursing.ui.SpecimenCollectActivity;
import com.cerner.nursing.nursing.ui.ChartingNavigatorActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mark Lear (ML015922)
 */
public final class NursingDrawerManager {
    private static final String TAG = NursingDrawerManager.class.getSimpleName();
    private static final String FEATURE_CARE_TEAM = "android/can_view_care_team";

    private static AuthnResponse smLatestAuthnResponse;
    private static ActiveModuleManager.ActiveModuleListener smActiveModuleListener;
    private static final DrawerPatientListItem smDrawerPatientListItem = new DrawerPatientListItem(null);
    private static final ArrayList<IListItem> smDrawerItems = new ArrayList<>();
    private static final IDataRetriever smDrawerPatientRetriever = new IDataRetriever() {
        private static final String NURSING_DRAWER_CHECKPOINT = "CURA_NURSING_DRAWER_READ";
        private Pair<String, String> smLoadedPatient;

        @Override
        public void getData(final DataArgs dataArgs) {
            if (PatientContext.isPatientSelected()) {
                if (smLoadedPatient != null && smLoadedPatient.first.equals(PatientContext.getPatientId()) && smLoadedPatient.second.equals(PatientContext.getEncounterId())) {
                    return;
                }

                smLoadedPatient = new Pair<>(PatientContext.getPatientId(), PatientContext.getEncounterId());
                final CuraResponseListener<PatientDemogBanner> responseListener = new CuraResponseListener<>(
                        null, TAG, NURSING_DRAWER_CHECKPOINT, PatientDemogBanner.class, this, IonApplication.getInstance(), false);
                JsonRequestor.sendRequest(responseListener, null, 300, false, null, PatientContext.getPatientId(), PatientContext.getEncounterId());
            } else if (smLoadedPatient != null) {
                blankResponse();
            }
        }

        @Override
        public void onResponse(@NonNull final CernResponse response) {
            final PatientDemogBanner data = (PatientDemogBanner) response.data;

            if (data == null) {
                blankResponse();
                return;
            }

            smDrawerPatientListItem.setData(data);
            smDrawerPatientListItem.invalidate();
        }

        @Override
        public void onNoContentResponse(@NonNull final CernResponse response, final Class clazz) {
            blankResponse();
        }

        @Override
        public void onErrorResponse(@NonNull final VolleyError volleyError, final Class clazz) {
            blankResponse();
        }

        @Override
        public void onFailedResponse(final Class clazz, final boolean cacheReturned) {
            blankResponse();
        }

        @Override
        public void setActionBarWaitCursor(final boolean isVisible) {
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
        }

        private void blankResponse() {
            smLoadedPatient = null;
            smDrawerPatientListItem.setData(null);
            smDrawerPatientListItem.invalidate();
        }
    };

    private NursingDrawerManager() {
    }

    public static List<IListItem> getDrawerItems(@NonNull final Context context) {
        final AuthnResponse latestAuthnResponse = IonAuthnSessionUtils.getAuthnResponse();
        final boolean userUnauth = latestAuthnResponse == null;
        if (!smDrawerItems.isEmpty() && (!userUnauth && latestAuthnResponse.equals(smLatestAuthnResponse))) {
            return smDrawerItems;
        }

        smDrawerItems.clear();
        ActiveModuleManager.removeActiveModuleListener(smActiveModuleListener);
        smLatestAuthnResponse = latestAuthnResponse;

        if (userUnauth) {
            return smDrawerItems;
        }

        final Class activeModule = ActiveModuleManager.getActiveModule();

        final DrawerListItem<Object> patientList = new DrawerListItem<>(context, context.getString(R.string.patientlist_title), 0, null, activeModule == PatientListActivity.class,
                                                                        true, null, (DrawerListItem.DrawerItemClickListener) activity -> {
                                                                            if (ActiveModuleManager.getActiveModule() != PatientListActivity.class) {
                                                                                ActiveModuleManager.setActiveModule(PatientListActivity.class, PatientListActivity.REQUESTCODE_PATIENTLIST_DEFAULT);
                                                                                return true;
                                                                            }

                                                                            return false;
                                                                        });
        smDrawerItems.add(patientList);

        smDrawerItems.add(smDrawerPatientListItem);

        final DrawerListItem<Object> patientChart = new DrawerListItem<>(context, context.getString(R.string.drawer_patient_chart), 0, null, activeModule == PatientChartActivity.class,
                                                                         PatientContext.isPatientSelected(), smDrawerPatientListItem, (DrawerListItem.DrawerItemClickListener) activity -> {
                                                                             if (ActiveModuleManager.getActiveModule() != PatientChartActivity.class) {
                                                                                 ActiveModuleManager.setActiveModule(PatientChartActivity.class, PatientListActivity.REQUESTCODE_PATIENTCHART_DEFAULT);
                                                                                 return true;
                                                                             }

                                                                             return false;
                                                                         });
        smDrawerItems.add(patientChart);

        final DrawerListItem<Object> medsAdmin;
        if (CuraMedicationScannedProcessor.isMedicationsEnabled()) {
            medsAdmin = new DrawerListItem<>(context, context.getString(R.string.drawer_medsadmin), 0, null, activeModule == MedsAdminActivity.class,
                                             PatientContext.isPatientSelected(), smDrawerPatientListItem, (DrawerListItem.DrawerItemClickListener) activity -> {
                                                 if (ActiveModuleManager.getActiveModule() != MedsAdminActivity.class) {
                                                     ActiveModuleManager.setActiveModule(MedsAdminActivity.class, PatientListActivity.REQUESTCODE_MEDSADMIN_DEFAULT);
                                                     return true;
                                                 }

                                                 return false;
                                             });
            smDrawerItems.add(medsAdmin);
        } else {
            medsAdmin = null;
        }

        final DrawerListItem<Object> specCol;
        if (SpecimenCollectionsScannedProcessor.isSpecimenCollectionsEnabled()) {
            specCol = new DrawerListItem<>(context, context.getString(R.string.drawer_speccol), 0, null, activeModule == SpecimenCollectActivity.class,
                                           PatientContext.isPatientSelected(), smDrawerPatientListItem, (DrawerListItem.DrawerItemClickListener) activity -> {
                                               if (ActiveModuleManager.getActiveModule() != SpecimenCollectActivity.class) {
                                                   ActiveModuleManager.setActiveModule(SpecimenCollectActivity.class, PatientListActivity.REQUESTCODE_SPECCOL_DEFAULT);
                                                   return true;
                                               }

                                               return false;
                                           });
            smDrawerItems.add(specCol);
        } else {
            specCol = null;
        }

        final DrawerListItem<Object> chartingNavigator;
        if (Utils.canChartChartingNavigator()) {
            chartingNavigator = new DrawerListItem<Object>(context, context.getString(R.string.drawer_charting_navigator), 0, null, activeModule == ChartingNavigatorActivity.class,
                    PatientContext.isPatientSelected(), smDrawerPatientListItem, (DrawerListItem.DrawerItemClickListener) activity -> {
                        if (ActiveModuleManager.getActiveModule() != ChartingNavigatorActivity.class) {
                            ActiveModuleManager.setActiveModule(ChartingNavigatorActivity.class, PatientListActivity.REQUESTCODE_CHARTING_DEFAULT);
                            return true;
                         }

                        return false;
                    });
            smDrawerItems.add(chartingNavigator);
        } else {
            chartingNavigator = null;
        }

        final DrawerListItem<Object> careTeam;
        if (CapabilitiesAndFeatures.isFeatureEnabled(FEATURE_CARE_TEAM)) {
            careTeam = new DrawerListItem<>(context, context.getString(R.string.careteam_title), 0, null, activeModule == CareTeamActivity.class,
                                            PatientContext.isPatientSelected(), smDrawerPatientListItem, (DrawerListItem.DrawerItemClickListener) activity -> {
                                                if (ActiveModuleManager.getActiveModule() != CareTeamActivity.class) {
                                                    ActiveModuleManager.setActiveModule(CareTeamActivity.class, PatientListActivity.REQUESTCODE_CARETEAM_DEFAULT);
                                                    return true;
                                                }

                                                return false;
                                            });
            smDrawerItems.add(careTeam);
        } else {
            careTeam = null;
        }

        final DrawerListItem<Object> deviceAssociation;
        if (CuraDeviceAssociationScannedProcessor.isDeviceAssociationEnabled()) {
            deviceAssociation = new DrawerListItem<>(context, context.getString(R.string.deviceassociation_title), 0, null,
                                                   activeModule == DeviceAssociationActivity.class, PatientContext.isPatientSelected(), smDrawerPatientListItem,
                                                     (DrawerListItem.DrawerItemClickListener) activity -> {
                                                         if (ActiveModuleManager.getActiveModule() != DeviceAssociationActivity.class) {
                                                             ActiveModuleManager.setActiveModule(DeviceAssociationActivity.class, PatientListActivity.REQUESTCODE_DEVICEASSOCIATION_DEFAULT);
                                                             return true;
                                                         }

                                                         return false;
                                                     });
            smDrawerItems.add(deviceAssociation);
        } else {
            deviceAssociation = null;
        }

        smDrawerItems.add(new DrawerListItem<>(context, context.getString(R.string.settings_title), 0, null, false, true, null, (DrawerListItem.DrawerItemClickListener) activity -> {
            if (ActiveModuleManager.getActiveModule() != SettingsActivity.class) {
                ActiveModuleManager.setActiveModule(SettingsActivity.class, PatientListActivity.REQUESTCODE_SETTINGS_DEFAULT);
                return true;
            }

            return false;
        }));

        smDrawerItems.add(new DrawerListItem<>(context, context.getString(R.string.logout), 0, null, false, true, null, (DrawerListItem.DrawerItemClickListener) activity -> {
            if (activity instanceof IonActivity) {
                final boolean workInProgress = PatientContext.isWorkInProgress();
                ((IonActivity) activity).getDialogs().showError(context.getString(workInProgress ? R.string.work_in_progress_title : R.string.logout),
                                                                context.getString(workInProgress ? R.string.work_in_progress_logout : R.string.drawer_logout_confirmation),
                                                                context.getString(R.string.yes),
                                                                (dialog, which) -> ActivityUtils.logout(context),
                                                                context.getString(R.string.no), null, null, null).show();
            }

            return false;
        }));

        smActiveModuleListener = (activeModule1, requestCode, extras) -> {
            final boolean patientInContext = PatientContext.isPatientSelected();

            smDrawerPatientRetriever.getData(IDataRetriever.DataArgs.NONE);

            patientList.setSelected(activeModule1 == PatientListActivity.class);

            patientChart.setEnabled(patientInContext);
            patientChart.setSelected(activeModule1 == PatientChartActivity.class);

            if (medsAdmin != null) {
                medsAdmin.setEnabled(patientInContext);
                medsAdmin.setSelected(activeModule1 == MedsAdminActivity.class);
            }

            if (specCol != null) {
                specCol.setEnabled(patientInContext);
                specCol.setSelected(activeModule1 == SpecimenCollectActivity.class);
            }

            if (chartingNavigator != null) {
                chartingNavigator.setEnabled(patientInContext);
                chartingNavigator.setSelected(activeModule == ChartingNavigatorActivity.class);
            }

            if (careTeam != null) {
                careTeam.setEnabled(patientInContext);
                careTeam.setSelected(activeModule1 == CareTeamActivity.class);
            }

            if (deviceAssociation != null) {
                deviceAssociation.setEnabled(patientInContext);
                deviceAssociation.setSelected(activeModule1 == DeviceAssociationActivity.class);
            }
        };
        ActiveModuleManager.addActiveModuleListener(smActiveModuleListener);

        return smDrawerItems;
    }
}