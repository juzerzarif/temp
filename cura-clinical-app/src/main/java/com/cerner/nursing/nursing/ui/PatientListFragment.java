package com.cerner.nursing.nursing.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.VolleyError;
import com.cerner.cura.base.CuraAuthnActivity;
import com.cerner.cura.base.CuraRefreshAuthnFragment;
import com.cerner.cura.base.OnDrawerListener;
import com.cerner.cura.base.UserContext;
import com.cerner.cura.datamodel.PatientList;
import com.cerner.cura.datamodel.PatientListById;
import com.cerner.cura.datamodel.PatientLists;
import com.cerner.cura.datamodel.common.PatientListPatient;
import com.cerner.cura.medications.scanning.CuraMedicationScannedProcessor;
import com.cerner.cura.medications.legacy.utils.CodeCache;
import com.cerner.cura.requestor.CuraResponseListener;
import com.cerner.cura.requestor.JsonRequestor;
import com.cerner.cura.ui.elements.IListItem;
import com.cerner.cura.ui.elements.ListArrayRecyclerAdapter;
import com.cerner.cura.ui.elements.PatientListItem;
import com.cerner.cura.ui.elements.TextListItem;
import com.cerner.cura.ui.elements.decoration.DecoratorFactory;
import com.cerner.cura.ui.elements.utils.DialogUtils;
import com.cerner.cura.utils.ActiveModuleManager;
import com.cerner.cura.utils.ActivityUtils;
import com.cerner.cura.utils.AppUtils;
import com.cerner.cura.utils.RequestorUtils;
import com.cerner.cura.utils.SerializablePair;
import com.cerner.ion.log.Logger;
import com.cerner.ion.request.CernResponse;
import com.cerner.nursing.nursing.R;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shows the patient list of the logged in individual
 *
 * @author Mark Lear (ML015922)
 */
public class PatientListFragment extends CuraRefreshAuthnFragment {
    private static final String PATIENT_LIST_SORT_BY_KEY = "CURA_CLINICAL_PATIENT_LIST_SORT_BY_KEY";
    private static final String PATIENT_LIST_DISPLAYED_KEY = "CURA_CLINICAL_PATIENT_LIST_DISPLAYED_KEY";
    private static final String CHECKPOINT_PATIENTLISTS_READ = "CURA_NURSING_PATIENTLISTS_READ";

    private transient OnPatientListItemSelectedListener mCallback;
    private transient OnDrawerListener mDrawerCallback;
    private transient Dialog mCurrentDialog;
    private transient RecyclerView mPatientListView;
    private ListArrayRecyclerAdapter mAdapter;
    private PatientListSortType mSortBy = PatientListSortType.LOCATION;
    private boolean mCanBeVisible = true;
    private boolean mShowLists;

    public static final int REQUESTCODE_SETTINGS_ORG_SELECT = 0;

    public enum PatientListSortType {NAME, LOCATION}

    // Container Activity must implement this interface
    public interface OnPatientListItemSelectedListener {
        void onPatientSelected(PatientListPatient patient);
    }

    public PatientListFragment() {
        TAG = PatientListFragment.class.getSimpleName();
        mCheckpointName = "CURA_NURSING_PATIENTLIST_READ";
        setDefaultCacheLookback(30);
        setAnimation(ActivityUtils.AnimationStyle.EXPANDCOLLAPSE);
    }

    @Override
    public void getData(final DataArgs dataArgs) {
        Logger.i(TAG, "GetData");
        super.getData(dataArgs);

        if (UserContext.isContextStorageObjectSet(PATIENT_LIST_DISPLAYED_KEY, String.class)) {
            final CuraResponseListener<PatientListById> responseListener = new CuraResponseListener<>(
                    mRefreshData.getDialogController(), TAG, "PATIENTLISTBYID_READ", PatientListById.class, mResponseProcessor, getActivity(), true);
            JsonRequestor.sendRequest(responseListener, this, getCacheLookback(), mRefreshData.getCacheOnly(), null,
                                      UserContext.getContextStorageObject(PATIENT_LIST_DISPLAYED_KEY, String.class));
        } else {
            final CuraResponseListener<PatientList> responseListener = new CuraResponseListener<>(
                    mRefreshData.getDialogController(), TAG, mCheckpointName, PatientList.class, mResponseProcessor, getActivity(), true);
            JsonRequestor.sendRequest(responseListener, this, 0, false, null);
        }

        //Pre-fetch codes
        if (CuraMedicationScannedProcessor.isMedicationsEnabled() && !CodeCache.isCached()) {
            CodeCache.prefetchCodes(null);
        }
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if ((mDrawerCallback == null || !mDrawerCallback.isDrawerOpen())) {
            inflater.inflate(R.menu.patient_list, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.change_list:
                AppUtils.logCheckPoint(getActivity(), CHECKPOINT_PATIENTLISTS_READ, AppUtils.CHECKPOINT_EVENT_LOAD);
                showChangeListDialog();
                break;
            case R.id.sort_list:
                showSortListDialog();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == REQUESTCODE_SETTINGS_ORG_SELECT) {
            if (resultCode == Activity.RESULT_OK) {
                getData(DataArgs.FORCE_REFRESH);
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        if (activity instanceof OnPatientListItemSelectedListener) {
            mCallback = (OnPatientListItemSelectedListener) activity;
        } else {
            throw new ClassCastException(activity + " must implement OnPatientListItemSelectedListener");
        }

        //If the starting activity has a drawer (not required) then we will need it to flex the options menu
        if (activity instanceof OnDrawerListener) {
            mDrawerCallback = (OnDrawerListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mCallback = null;
        mDrawerCallback = null;
        mCurrentDialog = null;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        AppUtils.logCheckPoint(getActivity(), mCheckpointName, AppUtils.CHECKPOINT_EVENT_LOAD);

        final View view = inflater.inflate(R.layout.patient_list_fragment, container, false);

        if (getActivity().isFinishing()) {
            return view;
        }

        setRefreshLayout(view.findViewById(R.id.refresh_layout_patient_list));
        mPatientListView = view.findViewById(R.id.patientlist_listView);

        mAdapter = new ListArrayRecyclerAdapter(getActivity());
        mPatientListView.setHasFixedSize(true);
        mPatientListView.setAdapter(mAdapter);
        mPatientListView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mAdapter.setOnItemClickListener((viewHolder, item) -> {
            if (mCallback != null && item.getClass() == PatientListItem.class) {
                mCallback.onPatientSelected(((PatientListItem) item).getData());
            }
        });

        if (UserContext.isContextStorageObjectSet(PATIENT_LIST_SORT_BY_KEY, PatientListSortType.class)) {
            mSortBy = UserContext.getContextStorageObject(PATIENT_LIST_SORT_BY_KEY, PatientListSortType.class);
        } else {
            UserContext.putContextStorageObject(PATIENT_LIST_SORT_BY_KEY, mSortBy);
        }

        return view;
    }

    @Override
    public void onAuthnStart() {
        super.onAuthnStart();
        getData(DataArgs.REFRESH);
    }

    @Override
    public void setupActionBar() {
        if (mDrawerCallback != null) {
            mDrawerCallback.setDrawerIndicatorEnabled(true);
        }

        if (getActivity() != null) {
            final ActionBar bar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (bar != null) {
                bar.setTitle(R.string.patientlist_title);
                bar.setDisplayShowHomeEnabled(true);
                bar.setHomeButtonEnabled(true);
            }
        }
    }

    @Override
    public void onResponse(@NonNull final Object model) {
        super.onResponse(model);

        Logger.i(TAG, "onResponse");

        if (getActivity() == null) {
            Logger.d(TAG, "Activity is null");
            return;
        }

        if (model instanceof PatientList) {
            final PatientList patientList = (PatientList) model;

            if (patientList.has_device_organization == 0) {
                RequestorUtils.clearCache();
                if (getActivity() instanceof CuraAuthnActivity) {
                    final Bundle bundle = new Bundle();
                    bundle.putInt(SettingsActivity.LAUNCH_ACTION_IDENTIFIER, SettingsActivity.LAUNCH_PICK_ORG);
                    ActiveModuleManager.gotoModule(getActivity(), SettingsActivity.class, REQUESTCODE_SETTINGS_ORG_SELECT, bundle);
                }

                return;
            }

            //set the displayed list if the initial load
            if (patientList.patientListId == null) {
                Logger.e(TAG, "Invalid patient list id returned" + (patientList.patientListName == null ? "" : " for list: " + patientList.patientListName));
            } else if (!UserContext.isContextStorageObjectSet(PATIENT_LIST_DISPLAYED_KEY, String.class)) {
                UserContext.putContextStorageObject(PATIENT_LIST_DISPLAYED_KEY, patientList.patientListId);
            }

            if (patientList.patientList == null || patientList.patientList.isEmpty()) {
                onNoContentResponse(null, PatientList.class);
                return;
            }

            final ArrayList<IListItem> newAdapterList = new ArrayList<>();
            newAdapterList.add(new TextListItem(patientList.patientListName, R.layout.header_item).setItemClickable(false));

            final List<PatientListPatient> sortedPatientList = sortPatientList(patientList.patientList, mSortBy);
            for (final PatientListPatient patient : sortedPatientList) {
                final PatientListItem pitem = new PatientListItem(patient);
                pitem.setNameAlert(patient.nameAlertInd);
                newAdapterList.add(pitem);
            }

            mPatientListView.removeItemDecoration(DecoratorFactory.getDefaultRecyclerDecorator(getActivity()));
            mPatientListView.addItemDecoration(DecoratorFactory.getDefaultRecyclerDecorator(getActivity()));

            mAdapter.replaceAll(newAdapterList);
        } else if (model instanceof PatientLists) {
            Logger.d(TAG, "Patient lists have been loaded");
            populateChangeListDialog((PatientLists) model);
        } else {
            throw new IllegalArgumentException("Model not of type PatientList");
        }

        if (mCanBeVisible) {
            setFragmentVisibility(true);
            notifyResponseReceivedListeners(PatientListFragment.class);
        }
    }

    @Override
    public void onNoContentResponse(@SuppressWarnings ("NullableProblems") final CernResponse response, final Class clazz) {
        super.onNoContentResponse(response, clazz);

        if (clazz == PatientList.class || clazz == PatientListById.class) {
            if (getActivity() == null) {
                Logger.d(TAG, "Activity is null");
                return;
            }

            mPatientListView.removeItemDecoration(DecoratorFactory.getDefaultRecyclerDecorator(getActivity()));

            final ArrayList<IListItem> newAdapterList = new ArrayList<>();
            newAdapterList.add(new TextListItem(getString(response == null || response.statusCode != HttpURLConnection.HTTP_ENTITY_TOO_LARGE
                                                          ? R.string.patientlist_nodataavailable : R.string.patientlist_toomanypatients)).setItemClickable(false));
            mAdapter.replaceAll(newAdapterList);
        } else if (clazz == PatientLists.class) {
            populateChangeListDialog(null);
            return;
        }

        setFragmentVisibility(true);
    }

    @Override
    public void onFailedResponse(final Class clazz, final boolean cacheReturned) {
        if (!cacheReturned) {
            if (clazz == PatientLists.class) {
                populateChangeListDialog(null);
            } else {
                //Call no content so that we don't get a white screen with no wait cursor
                onNoContentResponse(null, clazz);
            }
        }
    }

    @Override
    public void onErrorResponse(@NonNull final VolleyError volleyError, final Class clazz) {
        final CernResponse<Object> cernResponse;
        if (volleyError.networkResponse != null) {
            cernResponse = new CernResponse<>();
            cernResponse.statusCode = volleyError.networkResponse.statusCode;
        } else {
            cernResponse = null;
        }

        onNoContentResponse(cernResponse, clazz);
    }

    private static List<PatientListPatient> sortPatientList(@NonNull final List<PatientListPatient> patients, final PatientListSortType sortBy) {
        if (sortBy == PatientListSortType.LOCATION) {
            //TODO ml015922: this sort should really be going off of a location sort order provided by the services, based on some other rules CURA-2331

            Collections.sort(patients, (lhs, rhs) -> {
                if (rhs == null) {
                    if (lhs == null) {
                        return 0;
                    }
                    return -1;
                } else if (lhs == null) {
                    return 1;
                }

                if (rhs.roomLocationDisplay == null && rhs.bedLocationDisplay == null) {
                    if (lhs.roomLocationDisplay == null && lhs.bedLocationDisplay == null) {
                        return compareNames(lhs, rhs);
                    }
                    return 1;
                } else if (lhs.roomLocationDisplay == null && lhs.bedLocationDisplay == null) {
                    return -1;
                }

                if (rhs.roomLocationDisplay == null) {
                    if (lhs.roomLocationDisplay == null) {
                        return compareNames(lhs, rhs);
                    }
                    return 1;
                } else if (lhs.roomLocationDisplay == null) {
                    return -1;
                }

                if (lhs.roomLocationDisplay.equalsIgnoreCase(rhs.roomLocationDisplay)) {
                    if (rhs.bedLocationDisplay == null) {
                        if (lhs.bedLocationDisplay == null) {
                            return compareNames(lhs, rhs);
                        }
                        return 1;
                    } else if (lhs.bedLocationDisplay == null) {
                        return -1;
                    }
                }

                return (lhs.roomLocationDisplay + "_" + lhs.bedLocationDisplay).compareToIgnoreCase((rhs.roomLocationDisplay + "_" + rhs.bedLocationDisplay));
            });
        } else if (sortBy == PatientListSortType.NAME) {
            Collections.sort(patients, (lhs, rhs) -> {
                if (rhs == null) {
                    if (lhs == null) {
                        return 0;
                    }
                    return -1;
                } else if (lhs == null) {
                    return 1;
                }

                return compareNames(lhs, rhs);
            });
        }

        return patients;
    }

    private static int compareNames(final PatientListPatient lhs, final PatientListPatient rhs) {
        if (rhs.nameFullFormatted == null || rhs.nameFullFormatted.trim().isEmpty()) {
            if (lhs.nameFullFormatted == null || lhs.nameFullFormatted.trim().isEmpty()) {
                return 0;
            }
            return 1;
        } else if (lhs.nameFullFormatted == null || lhs.nameFullFormatted.trim().isEmpty()) {
            return -1;
        }

        return lhs.nameFullFormatted.compareToIgnoreCase(rhs.nameFullFormatted);
    }

    private void showChangeListDialog() {
        if (getActivity() == null) {
            return;
        }

        mShowLists = true;
        final CuraResponseListener<PatientLists> responseListener = new CuraResponseListener<>(
                showProgressDialog(mResponseProcessor), TAG, CHECKPOINT_PATIENTLISTS_READ, PatientLists.class, mResponseProcessor, getActivity(), true);
        JsonRequestor.sendRequest(responseListener, this, 30, false, null);
    }

    private void populateChangeListDialog(final PatientLists patientLists) {
        if (!mShowLists || getActivity() == null) {
            return;
        }

        final List<SerializablePair<String, String>> changeListItems = new ArrayList<>();

        if (patientLists != null) {
            for (final PatientLists.PatientListInfo info : patientLists.listOfLists) {
                changeListItems.add(new SerializablePair<>(info.patientListName, info.patientListId));
            }
        }

        //Hide these since we could previously hit the showAcknowledgeMessage below
        getIonActivity().getDialogs().hideErrorDialogs();

        if (changeListItems.isEmpty()) {
            DialogUtils.showAcknowledgeMessage(getIonActivity(), dialogInterface -> {
                Logger.i(TAG, "Empty change list dialog closed");
                mShowLists = false;
            }, R.string.select_list, getString(R.string.patientlist_changelist_nodataavailable), android.R.string.ok);
            return;
        }

        String selectedItemId = null;

        //select the already being shown list
        if (UserContext.isContextStorageObjectSet(PATIENT_LIST_DISPLAYED_KEY, String.class)) {
            final String displayedList = UserContext.getContextStorageObject(PATIENT_LIST_DISPLAYED_KEY, String.class);
            for (final SerializablePair<String, String> item : changeListItems) {
                if (item.second.equals(displayedList)) {
                    selectedItemId = item.second;
                    break;
                }
            }
        }

        //List was updated, so cancel and recreate the dialog
        if (mCurrentDialog != null) {
            mCurrentDialog.cancel();
        }

        final AlertDialog newDialog = DialogUtils
                .buildDialogListSingleSelection(getActivity(), getString(R.string.select_list), changeListItems, selectedItemId, selectedItem -> {
                    Logger.i(TAG, "Change list dialog item selected");
                    UserContext.putContextStorageObject(PATIENT_LIST_DISPLAYED_KEY, selectedItem.second);
                    getData(DataArgs.REFRESH);
                    mShowLists = false;
                    mCurrentDialog = null;
                });

        newDialog.setOnCancelListener(dialog -> {
            Logger.i(TAG, "Change list dialog canceled");
            mShowLists = false;
            mCurrentDialog = null;
        });

        newDialog.setOnShowListener(dialogInterface -> {
            //reset back to true and set the dialog, since the old dialog's cancel listeners could have cleared them
            mCurrentDialog = newDialog;
            mShowLists = true;
        });

        mCurrentDialog = newDialog;
        newDialog.show();
        Logger.i(TAG, "Change list dialog shown");
    }

    private void showSortListDialog() {
        if (getActivity() == null || mCurrentDialog != null) {
            return;
        }

        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final RecyclerView dialogLayout = (RecyclerView) inflater.inflate(R.layout.dialog_listview, (ViewGroup) getView(), false);

        if (dialogLayout == null) {
            Logger.e(TAG, "Sort List dialog layout not inflated.");
            return;
        }

        final List<SerializablePair<String, PatientListSortType>> dialogListItems = new ArrayList<>();

        dialogListItems.add(new SerializablePair<>(getString(R.string.sortlist_option_location), PatientListSortType.LOCATION));
        dialogListItems.add(new SerializablePair<>(getString(R.string.sortlist_option_name), PatientListSortType.NAME));

        mCurrentDialog = DialogUtils.buildDialogListSingleSelection(getActivity(), getString(R.string.sort_list), dialogListItems, mSortBy,
                                                                    selectedItem -> {
                                                                        Logger.i(TAG, "Sort list dialog item selected");
                                                                        mSortBy = selectedItem.second;
                                                                        UserContext.putContextStorageObject(PATIENT_LIST_SORT_BY_KEY, mSortBy);
                                                                        getData(DataArgs.REFRESH);
                                                                        mCurrentDialog = null;
                                                                    });

        mCurrentDialog.setOnCancelListener(dialog -> {
            Logger.i(TAG, "Sort list dialog canceled");
            mCurrentDialog = null;
        });

        mCurrentDialog.show();
        Logger.i(TAG, "Sort list dialog shown");
    }

    public void setCanBeVisible(final boolean canBeVisible) {
        mCanBeVisible = canBeVisible;
    }
}