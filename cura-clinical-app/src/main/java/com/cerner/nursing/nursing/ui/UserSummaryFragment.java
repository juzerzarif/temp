package com.cerner.nursing.nursing.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.VolleyError;
import com.cerner.cura.base.CuraRefreshAuthnFragment;
import com.cerner.cura.datamodel.PersonnelInfoSummary;
import com.cerner.cura.requestor.CuraResponseListener;
import com.cerner.cura.requestor.JsonRequestor;
import com.cerner.cura.ui.elements.IListItem;
import com.cerner.cura.ui.elements.ListArrayRecyclerAdapter;
import com.cerner.cura.ui.elements.SettingsPageHeaderListItem;
import com.cerner.cura.ui.elements.TextListItem;
import com.cerner.cura.ui.elements.ValueListItem;
import com.cerner.cura.ui.elements.decoration.DecoratorFactory;
import com.cerner.cura.utils.AppUtils;
import com.cerner.ion.log.Logger;
import com.cerner.ion.request.CernResponse;
import com.cerner.ion.security.AboutAppActivity;
import com.cerner.ion.security.IonActivity;
import com.cerner.ion.security.authentication.PinManager;
import com.cerner.ion.session.IonAuthnSessionUtils;
import com.cerner.nursing.nursing.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the current users summary/settings information
 *
 * @author Mark Lear (ML015922)
 * @author Brad Barnhill (bb024928)
 */
public class UserSummaryFragment extends CuraRefreshAuthnFragment {
    private OnFragmentSelected mCallback;
    private ListArrayRecyclerAdapter mAdapter;
    private Bitmap mUserImage;
    private ViewHolder mViewHolder;
    private String mOrganizationId;
    private boolean mHasMultipleOrgs = true;
//    private String CHECKPOINT_PERSONNELPROFILE_READ = "CURA_NURSING_PERSONNELPROFILE_READ";

    public UserSummaryFragment() {
        TAG = UserSummaryFragment.class.getSimpleName();
        mCheckpointName = "CURA_NURSING_USERSETTINGS_READ";
        setDefaultCacheLookback(300);
    }

    @Override
    public void getData(final DataArgs dataArgs) {
        Logger.i(TAG, "GetData");
        super.getData(dataArgs);

        final CuraResponseListener<PersonnelInfoSummary> responseListener = new CuraResponseListener<>(
                mRefreshData.getDialogController(), TAG, mCheckpointName, PersonnelInfoSummary.class, mResponseProcessor, getActivity(), true);
        JsonRequestor.sendRequest(responseListener, this, getCacheLookback(), mRefreshData.getCacheOnly(), null);
    }

    @Override
    public void onResponse(@NonNull final Object model) {
        super.onResponse(model);

        Logger.d(TAG, "Personnel Info read");

        if (model instanceof PersonnelInfoSummary) {
            final PersonnelInfoSummary prsnl = (PersonnelInfoSummary) model;

            //TODO AV032294: uncomment once we start getting the images back from services
            /*if (mUserImage == null && prsnl.person_id != null) {
                AppUtils.logCheckPoint(getActivity(), CHECKPOINT_PERSONNELPROFILE_READ, AppUtils.CHECKPOINT_EVENT_LOAD);
                final CuraResponseListener<PersonnelPhoto> responseListener = new CuraResponseListener<>(
                        null, TAG, CHECKPOINT_PERSONNELPROFILE_READ, PersonnelPhoto.class, mResponseProcessor, getActivity());
                JsonRequestor.sendRequest(responseListener, this, 86400, false, null, prsnl.person_id);
            }*/

            mViewHolder.mSettingsPageHeaderListItem.setPersonnelImage(mUserImage);
            mViewHolder.mSettingsPageHeaderListItem
                    .setPersonnelName(IonAuthnSessionUtils.getUser() != null ? getString(R.string.user_full_name, IonAuthnSessionUtils.getUser().getLastName(),
                                                                                         IonAuthnSessionUtils.getUser().getFirstName()) : "");
            mViewHolder.mOrganization.setData(prsnl.organization_name);
            mOrganizationId = prsnl.organization_id;
            mHasMultipleOrgs = prsnl.has_one_organization != 1;

            updateFragmentList();
        }
        //TODO AV032294: CURA-2510 uncomment once we start getting the images back from services
        /*else if (model instanceof PersonnelPhoto) {
            if (mUserImage == null || !mUserImage.sameAs(((PersonnelPhoto) model).image)) {
                mUserImage = ((PersonnelPhoto) model).image;
                if (mAdapter != null) {
                    mViewHolder.mSettingsPageHeaderListItem.setPersonnelImage(mUserImage);
                    mAdapter.notifyDataSetChanged();
                }
            }
            AppUtils.logCheckPoint(getActivity(), CHECKPOINT_PERSONNELPROFILE_READ, AppUtils.CHECKPOINT_EVENT_RESPONSE_WITH_CONTENT);
        }*/
        else {
            throw new IllegalArgumentException("Model not of type PersonnelInfoSummary or PersonnelPhoto");
        }
    }

    @Override
    public void onNoContentResponse(@SuppressWarnings ("NullableProblems") final CernResponse response, final Class clazz) {
        super.onNoContentResponse(response, clazz);

        /*if (clazz == PersonnelPhoto.class) {
            AppUtils.logCheckPoint(getActivity(), CHECKPOINT_PERSONNELPROFILE_READ, AppUtils.CHECKPOINT_EVENT_RESPONSE_WITH_NO_CONTENT);
        } else {*/
        updateFragmentList();
    }

    @Override
    public void onErrorResponse(@NonNull final VolleyError volleyError, final Class clazz) {
        /*if (clazz == PersonnelPhoto.class) {
            AppUtils.logCheckPoint(getActivity(), CHECKPOINT_PERSONNELPROFILE_READ, AppUtils.CHECKPOINT_EVENT_RESPONSE_FAIL);
        }*/
        setDataRetrieved(true);
        updateFragmentList();
    }

    @Override
    public void onFailedResponse(final Class clazz, final boolean cacheReturned) {
        if (!cacheReturned) {
            onNoContentResponse(null, clazz);
        }
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        if (activity instanceof OnFragmentSelected) {
            mCallback = (OnFragmentSelected) activity;
        } else {
            throw new ClassCastException(activity + " must implement OnFragmentSelected");
        }
    }

    private void updateFragmentList() {
        mViewHolder.mOrganization.setItemClickable(mHasMultipleOrgs);

        final List<IListItem> newAdapterList = new ArrayList<>();
        newAdapterList.add(mViewHolder.mSettingsPageHeaderListItem);
        newAdapterList.add(mViewHolder.mOrganization);
        newAdapterList.add(mViewHolder.mBlankHeader);
        newAdapterList.add(mViewHolder.mAbout);

        if (PinManager.Factory.get() != null) {
            newAdapterList.add(mViewHolder.mBlankHeader);

            if (IonAuthnSessionUtils.getAuthnResponse() != null && IonAuthnSessionUtils.getAuthnResponse().getUser() != null
                && IonAuthnSessionUtils.getAuthnResponse().getUser().hasPin()) {
                newAdapterList.add(mViewHolder.mChangePin);
                newAdapterList.add(mViewHolder.mRemovePin);
            } else {
                newAdapterList.add(mViewHolder.mCreatePin);
            }
        }
        mAdapter.replaceAll(newAdapterList);

        setFragmentVisibility(true);
        notifyResponseReceivedListeners(UserSummaryFragment.class);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mCallback = null;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        AppUtils.logCheckPoint(getActivity(), mCheckpointName, AppUtils.CHECKPOINT_EVENT_LOAD);
        final Activity activity = getActivity();
        final View view = inflater.inflate(R.layout.user_summary_fragment, container, false);

        if (activity.isFinishing()) {
            return view;
        }

        mViewHolder = new ViewHolder(view, activity);

        setRefreshLayout(view.findViewById(R.id.refresh_layout_user_summary));

        mAdapter = new ListArrayRecyclerAdapter(activity);
        mViewHolder.mSettingsList.setHasFixedSize(true);
        mViewHolder.mSettingsList.setAdapter(mAdapter);
        mViewHolder.mSettingsList.setLayoutManager(new LinearLayoutManager(activity));
        mViewHolder.mSettingsList.addItemDecoration(DecoratorFactory.getDefaultRecyclerDecorator(activity));

        mAdapter.setOnItemClickListener((viewHolder, item) -> {
            if (item.getTitle() == null) {
                return;
            }

            Logger.d(TAG, "Settings item clicked: " + item.getTitle());
            handleClicks(item.getTitle());
        });

        return view;
    }

    @Override
    public void onAuthnStart() {
        super.onAuthnStart();
        getData(DataArgs.REFRESH);
    }

    @Override
    public void setupActionBar() {
        if (getActivity() != null) {
            final ActionBar bar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (bar != null) {
                bar.setTitle(R.string.settings_title);
                bar.setDisplayShowHomeEnabled(true);
                bar.setHomeButtonEnabled(true);
            }
        }
    }

    private void handleClicks(final String itemText) {
        if (getActivity() == null) {
            return;
        }

        if (itemText.equals(getString(R.string.settings_createpin))) {
            if (getActivity() instanceof IonActivity) {
                PinManager.Factory.get().startCreatePin(getIonActivity());
            }
        } else if (itemText.equals(getString(R.string.settings_changepin))) {
            if (getActivity() instanceof IonActivity) {
                PinManager.Factory.get().startChangePin(getIonActivity());
            }
        } else if (itemText.equals(getString(R.string.settings_removepin))) {
            if (getActivity() instanceof IonActivity) {
                PinManager.Factory.get().startDeletePin(getIonActivity());
            }
        } else if (itemText.equals(getString(R.string.settings_organization))) {
            //organization selected
            if (mHasMultipleOrgs && mCallback != null) {
                final Bundle bundle = new Bundle();
                bundle.putString(CURA_BUNDLE_PARAMETER, mOrganizationId);
                mCallback.onFragmentSelected(OrgSelectionFragment.class, bundle);
            }
        } else if (itemText.equals(getString(R.string.about))) {
            if (getActivity() != null) {
                startActivity(new Intent(getActivity(), AboutAppActivity.class));
            }
        } else {
            throw new IllegalArgumentException(TAG + ": View was clicked that is not supported.");
        }
    }

    static class ViewHolder {
        private final RecyclerView mSettingsList;
        private final SettingsPageHeaderListItem mSettingsPageHeaderListItem;
        private final ValueListItem mOrganization;
        private final TextListItem mCreatePin;
        private final TextListItem mRemovePin;
        private final TextListItem mChangePin;
        private final IListItem mBlankHeader;
        private final TextListItem mAbout;

        public ViewHolder(final View root, final Context context) {
            mSettingsList = root.findViewById(R.id.settingsList);

            if (mSettingsList == null) {
                throw new NullPointerException(UserSummaryFragment.class.getSimpleName() + " ViewHolder failed to find all views");
            }

            mSettingsPageHeaderListItem = new SettingsPageHeaderListItem();
            mSettingsPageHeaderListItem.setItemClickable(false);
            mCreatePin = new TextListItem(context.getString(R.string.settings_createpin));
            mChangePin = new TextListItem(context.getString(R.string.settings_changepin));
            mRemovePin = new TextListItem(context.getString(R.string.settings_removepin));
            mOrganization = new ValueListItem(context.getString(R.string.settings_organization), AppUtils.ResultType.NONE, context.getString(R.string.value_default));

            mBlankHeader = new TextListItem(null, R.layout.header_item).setItemClickable(false);
            mAbout = new TextListItem(context.getString(R.string.about));
        }
    }
}
