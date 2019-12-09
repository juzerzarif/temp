package com.cerner.nursing.nursing.ui;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.SearchView;

import com.android.volley.VolleyError;
import com.cerner.cura.base.CuraRefreshAuthnFragment;
import com.cerner.cura.datamodel.AvailableOrgs;
import com.cerner.cura.datamodel.StoredOrgRelation;
import com.cerner.cura.requestor.CuraResponseListener;
import com.cerner.cura.requestor.JsonRequestor;
import com.cerner.cura.ui.elements.ChoiceIndicatorListItem;
import com.cerner.cura.ui.elements.CursorAdapterLoader;
import com.cerner.cura.ui.elements.CursorRecyclerAdapter;
import com.cerner.cura.ui.elements.IListItem;
import com.cerner.cura.ui.elements.ListArrayRecyclerAdapter;
import com.cerner.cura.ui.elements.TextListItem;
import com.cerner.cura.ui.elements.decoration.DecoratorFactory;
import com.cerner.cura.utils.ActivityUtils;
import com.cerner.cura.utils.AppUtils;
import com.cerner.ion.log.Logger;
import com.cerner.ion.request.CernResponse;
import com.cerner.nursing.nursing.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows the org selection for the current logged in individual
 *
 * @author Mark Lear (ML015922)
 */
public class OrgSelectionFragment extends CuraRefreshAuthnFragment {
    private static final String CHECKPOINT_ORGANIZATION_WRITE = "CURA_NURSING_ORGANIZATION_WRITE";
    private static final String ORG_SEARCH_QUERY = "CURA_ORG_SEARCH_QUERY";

    private transient ViewHolder mViewHolder;
    private transient CursorRecyclerAdapter<AvailableOrgs.Org, ChoiceIndicatorListItem<AvailableOrgs.Org>> mCursorAdapter;
    private transient OrgSearchDbHelper mOrgDatabaseHelper;
    private ListArrayRecyclerAdapter mNoContentAdapter;
    private OrgSelectGlobalLayout mGlobalLayoutListener;
    private OnOrgSelectedListener mCallback;
    private String mSearchQuery;
    private String mOriginalOrgId;

    private StoredOrgRelation mSelectedOrg;

    // Container Activity must implement this interface
    public interface OnOrgSelectedListener {
        void onOrgSelected();
    }

    private static final class OrgSearchDbHelper extends SQLiteOpenHelper {
        private static final String TEXT_TYPE = " TEXT";
        private static final String TEXT_UNIQUE_TYPE = TEXT_TYPE + " UNIQUE";
        private static final String COMMA_SEP = ",";
        private static final String SQL_CREATE_ENTRIES =
                "CREATE VIRTUAL TABLE " + OrgEntry.ORG_FTS_VIRTUAL_TABLE + " USING fts3(" +
                OrgEntry._ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                OrgEntry.COLUMN_NAME_ID + TEXT_UNIQUE_TYPE + COMMA_SEP +
                OrgEntry.COLUMN_NAME_NAME + TEXT_TYPE + ")";

        private static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS " + OrgEntry.ORG_FTS_VIRTUAL_TABLE;
        static final String SQL_DELETE_ALL_ENTRIES = "DELETE FROM " + OrgEntry.ORG_FTS_VIRTUAL_TABLE;

        private abstract class OrgEntry implements BaseColumns {
            private static final String ORG_FTS_VIRTUAL_TABLE = "ORG_FTS";
            private static final String COLUMN_NAME_ID = "id";
            private static final String COLUMN_NAME_NAME = "name";

            // Constants that define the order of columns in the returned cursor
            private static final int ID_CURSOR_INDEX = 1;
            private static final int NAME_CURSOR_INDEX = 2;
        }

        private OrgSearchDbHelper(final Context context) {
            super(context, null, null, 1);
            getWritableDatabase();
        }

        @Override
        public void onCreate(final SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_ENTRIES);
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
            db.execSQL(SQL_DROP_TABLE);
            onCreate(db);
        }

        @Override
        public void onDowngrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }

        private static AvailableOrgs.Org convertCursorToOrganization(@NonNull final Cursor cursor) {
            final AvailableOrgs.Org org = new AvailableOrgs.Org();
            org.organizationId = cursor.getString(OrgEntry.ID_CURSOR_INDEX);
            org.organizationName = cursor.getString(OrgEntry.NAME_CURSOR_INDEX);

            return org;
        }
    }

    private static final class OrgSelectGlobalLayout implements ViewTreeObserver.OnGlobalLayoutListener {
        private static final String TAG = OrgSelectGlobalLayout.class.getSimpleName();
        private final WeakReference<OrgSelectionFragment> mOrgSelectFragmentWeakReference;
        private boolean mInitializedOriginal;

        private OrgSelectGlobalLayout(@NonNull final OrgSelectionFragment codeListFragment) {
            mOrgSelectFragmentWeakReference =  new WeakReference<>(codeListFragment);
        }

        @Override
        public void onGlobalLayout() {
            if (mInitializedOriginal) {
                //Already initialized, bail early
                return;
            }

            final OrgSelectionFragment orgSelectFragment = mOrgSelectFragmentWeakReference.get();
            if (orgSelectFragment == null) {
                Logger.d(TAG, "Fragment is out of scope so global layout should have been removed.");
                return;
            }

            if (TextUtils.isEmpty(orgSelectFragment.mOriginalOrgId) || orgSelectFragment.mViewHolder.mOrgListView.getAdapter() == null
                || orgSelectFragment.mViewHolder.mOrgListView.getAdapter() instanceof ListArrayRecyclerAdapter) {
                //No current value or no items so we didn't bind against the cursor adapter
                return;
            }

            final int cursorCount = orgSelectFragment.mCursorAdapter.getItemCount();
            if (cursorCount != 0) {
                //Even if we don't find it, we tried so mark it as initialized
                mInitializedOriginal = true;

                for (int index = 0; index < cursorCount; ++index) {
                    final AvailableOrgs.Org currentOrg = orgSelectFragment.mCursorAdapter.getObject(index);
                    if (currentOrg != null && currentOrg.organizationId.equals(orgSelectFragment.mOriginalOrgId)) {
                        final int scrollPosition = (cursorCount - 5 < index) ? cursorCount - 1 : index + 4; //Account for collapsible action bar and scroll a little past
                        orgSelectFragment.mViewHolder.mOrgListView.scrollToPosition(scrollPosition);
                        return;
                    }
                }
            }
        }
    }

    private static class CursorThreadOperator implements CursorAdapterLoader.OnPerformThreadedOperation {
        private static final String mSelection = OrgSearchDbHelper.OrgEntry.COLUMN_NAME_NAME + " LIKE ?";
        private static final String mSortOrder = OrgSearchDbHelper.OrgEntry.COLUMN_NAME_NAME + " ASC";

        private final WeakReference<OrgSelectionFragment> mWeakOrgSelectionFragment;
        private final String[] mColumns = {OrgSearchDbHelper.OrgEntry._ID, OrgSearchDbHelper.OrgEntry.COLUMN_NAME_ID, OrgSearchDbHelper.OrgEntry.COLUMN_NAME_NAME};

        private CursorThreadOperator(final OrgSelectionFragment orgSelectionFragment) {
            mWeakOrgSelectionFragment = new WeakReference<>(orgSelectionFragment);
        }

        @Override
        public boolean onPerformThreadedDataUpdate(final Object... params) {
            final OrgSelectionFragment orgSelectionFragment = mWeakOrgSelectionFragment.get();
            if (orgSelectionFragment == null) {
                return false;
            }

            final SQLiteDatabase database = orgSelectionFragment.mOrgDatabaseHelper.getWritableDatabase();
            database.execSQL(OrgSearchDbHelper.SQL_DELETE_ALL_ENTRIES);  //To clear it out every time

            if (params == null || params.length == 0 || !(params[0] instanceof List)) {
                return true;
            }

            //noinspection unchecked
            final List<AvailableOrgs.Org> orgs = (List<AvailableOrgs.Org>) params[0];

            ContentValues values;
            for (final AvailableOrgs.Org org : orgs) {
                if (org != null) {
                    values = new ContentValues();
                    values.put(OrgSearchDbHelper.OrgEntry.COLUMN_NAME_ID, org.organizationId);
                    values.put(OrgSearchDbHelper.OrgEntry.COLUMN_NAME_NAME, org.organizationName);

                    database.insert(OrgSearchDbHelper.OrgEntry.ORG_FTS_VIRTUAL_TABLE, null, values);
                }
            }

            return true;
        }

        @Override
        public Cursor onPerformThreadedCursorUpdate() {
            final OrgSelectionFragment orgSelectionFragment = mWeakOrgSelectionFragment.get();
            if (orgSelectionFragment == null) {
                return null;
            }

            final String[] selectionArgs = new String[1];

            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (orgSelectionFragment) {
                if (!TextUtils.isEmpty(orgSelectionFragment.mSearchQuery)) {
                    //The regex here will remove all(+) asterisks(*) at the beginning(^) or(|) at the end($)
                    final String searchQuery = orgSelectionFragment.mSearchQuery.replaceAll("(^\\*+)|(\\*+$)", "");

                    if (!searchQuery.isEmpty()) {
                        //The percent symbols being attached here acts as the grouper for multi word queries (i.e. %lear, mark%).
                        selectionArgs[0] = "%" + searchQuery + "%";
                    }
                }
            }

            final SQLiteDatabase database = orgSelectionFragment.mOrgDatabaseHelper.getReadableDatabase();
            return database.query(OrgSearchDbHelper.OrgEntry.ORG_FTS_VIRTUAL_TABLE, mColumns, selectionArgs[0] == null ? null : mSelection,
                                  selectionArgs[0] == null ? null : selectionArgs, null, null, mSortOrder);
        }
    }

    public OrgSelectionFragment() {
        TAG = OrgSelectionFragment.class.getSimpleName();
        mCheckpointName = "CURA_NURSING_ORGANIZATION_READ";
        setDefaultCacheLookback(300);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if (getArguments() != null && getArguments().getInt(SettingsActivity.LAUNCH_ACTION_IDENTIFIER, SettingsActivity.LAUNCH_DEFAULT) == SettingsActivity.LAUNCH_PICK_ORG) {
            inflater.inflate(R.menu.org_select, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_logout:
                ActivityUtils.logout(getIonActivity());
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void getData(final DataArgs dataArgs) {
        Logger.i(TAG, "Download available orgs");
        super.getData(dataArgs);

        final CuraResponseListener<AvailableOrgs> responseListener = new CuraResponseListener<>(
                mRefreshData.getDialogController(), TAG, mCheckpointName, AvailableOrgs.class, mResponseProcessor, getActivity(), true);
        JsonRequestor.sendRequest(responseListener, this, getCacheLookback(), mRefreshData.getCacheOnly(), null);
    }

    @Override
    public void onResponse(@NonNull final Object model) {
        super.onResponse(model);

        Logger.d(TAG, "Available orgs were read");

        if (model instanceof AvailableOrgs) {
            final AvailableOrgs availableOrgs = (AvailableOrgs) model;

            if (availableOrgs.accessibleOrganizations == null || availableOrgs.accessibleOrganizations.size() <= 0) {
                onNoContentResponse(null, AvailableOrgs.class);
                return;
            }

            if (availableOrgs.accessibleOrganizations.size() < 15) {
                mViewHolder.mSearchView.setVisibility(View.GONE);
                synchronized (this) {
                    mSearchQuery = null;
                }
            } else {
                mViewHolder.mSearchView.setVisibility(View.VISIBLE);
            }

            mCursorAdapter.executeAsyncDataUpdate(availableOrgs.accessibleOrganizations);

            mViewHolder.mOrgListView.removeItemDecoration(DecoratorFactory.getDefaultRecyclerDecorator(getActivity()));
            mViewHolder.mOrgListView.addItemDecoration(DecoratorFactory.getDefaultRecyclerDecorator(getActivity()));
            mViewHolder.mOrgListView.setAdapter(mCursorAdapter);
            mCursorAdapter.setOnItemClickListener((viewHolder, item) -> {
                final AvailableOrgs.Org org = item.getData();
                if (org != null && !org.organizationId.equals(mOriginalOrgId)) {
                    mSelectedOrg = new StoredOrgRelation();
                    mSelectedOrg.org_id = org.organizationId;
                    mSelectedOrg.org_name = org.organizationName;
                    Logger.d(TAG, String.format("Org %s selected", mSelectedOrg.org_name));

                    setRefreshData(DataArgs.FORCE_REFRESH);
                    final CuraResponseListener<Object> responseListener = new CuraResponseListener<>(
                            mRefreshData.getDialogController(), TAG, CHECKPOINT_ORGANIZATION_WRITE, null, mResponseProcessor, getActivity(), true);
                    JsonRequestor.sendRequest(responseListener, OrgSelectionFragment.this, 0, false, mSelectedOrg);
                }
            });
        } else {
            throw new IllegalArgumentException("Model not of type AvailableOrgs");
        }

        setFragmentVisibility(true);
        notifyResponseReceivedListeners(OrgSelectionFragment.class);
    }

    @Override
    public void onNoContentResponse(@SuppressWarnings ("NullableProblems") final CernResponse response, final Class clazz) {
        super.onNoContentResponse(response, clazz);
        setFragmentVisibility(true);

        if (clazz == null) {
            Logger.d(TAG, String.format("Org %s set on backend", mSelectedOrg == null ? "" : mSelectedOrg.org_name));
            if (mCallback != null) {
                mCallback.onOrgSelected();
            }
        } else {
            mViewHolder.mSearchView.setVisibility(View.GONE);
            synchronized (this) {
                mSearchQuery = null;
            }

            mViewHolder.mOrgListView.removeItemDecoration(DecoratorFactory.getDefaultRecyclerDecorator(getActivity()));
            mViewHolder.mOrgListView.setAdapter(mNoContentAdapter);
        }
    }

    @Override
    public void onErrorResponse(@NonNull final VolleyError volleyError, final Class clazz) {
        if (clazz == null) {
            mCursorAdapter.requeryCursor();
        } else {
            super.onErrorResponse(volleyError, clazz);
        }
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        if (activity instanceof OnOrgSelectedListener) {
            mCallback = (OnOrgSelectedListener) activity;
        } else {
            throw new ClassCastException(activity + " must implement OnOrgSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mCallback = null;
    }

    @Override
    public void onAuthnResume() {
        super.onAuthnResume();
        mViewHolder.mOrgListView.getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener);
    }

    @Override
    public void onPause() {
        mViewHolder.mOrgListView.getViewTreeObserver().removeOnGlobalLayoutListener(mGlobalLayoutListener);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mOrgDatabaseHelper != null) {
            mOrgDatabaseHelper.close();
        }

        setHasBackPressAction(false);

        super.onDestroy();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        AppUtils.logCheckPoint(getActivity(), mCheckpointName, AppUtils.CHECKPOINT_EVENT_LOAD);

        final View view = inflater.inflate(R.layout.org_selection_fragment, container, false);

        if (getActivity().isFinishing()) {
            return view;
        }

        mViewHolder = new ViewHolder(view);

        mViewHolder.mOrgListView.setHasFixedSize(true);
        mViewHolder.mOrgListView.setAdapter(mNoContentAdapter);
        mViewHolder.mOrgListView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mViewHolder.mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(final String query) {
                AppUtils.hideKeyboard(getActivity());
                return true;
            }

            @Override
            public boolean onQueryTextChange(final String newText) {
                synchronized (OrgSelectionFragment.this) {
                    mSearchQuery = newText;
                }

                mCursorAdapter.requeryCursor();

                return false;
            }
        });

        mViewHolder.mOrgListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(final RecyclerView view, final int scrollState) {
                if (scrollState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    AppUtils.hideKeyboard(getActivity());
                }
            }
        });

        setRefreshLayout(mViewHolder.mSwipeRefreshLayout);

        return view;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        setHasBackPressAction(true);
        super.onCreate(savedInstanceState);

        if(getActivity().isFinishing()) {
            return;
        }

        if (getArguments() != null) {
            mOriginalOrgId = getArguments().getString(CURA_BUNDLE_PARAMETER);
        }

        mGlobalLayoutListener = new OrgSelectGlobalLayout(this);
        initializeCursorAdapter(savedInstanceState);

        final List<IListItem> items = new ArrayList<>();
        items.add(new TextListItem(getString(R.string.org_nodataavailable)).setItemClickable(false));
        mNoContentAdapter = new ListArrayRecyclerAdapter(getActivity(), items);
        mNoContentAdapter.setHasStableIds(true);
    }

    private void initializeCursorAdapter(final Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            synchronized (this) {
                mSearchQuery = savedInstanceState.getString(ORG_SEARCH_QUERY);
            }
        }

        mOrgDatabaseHelper = new OrgSearchDbHelper(getActivity());

        mCursorAdapter = new CursorRecyclerAdapter<>(getActivity(), new ChoiceIndicatorListItem<>("", null, false),
                                                     new CursorRecyclerAdapter.ListItemFromCursor<AvailableOrgs.Org, ChoiceIndicatorListItem<AvailableOrgs.Org>>() {
                                                         @Override
                                                         public @NonNull AvailableOrgs.Org convertCursorToObject(@NonNull final Cursor cursor) {
                                                             return OrgSearchDbHelper.convertCursorToOrganization(cursor);
                                                         }

                                                         @Override
                                                         public @NonNull ChoiceIndicatorListItem<AvailableOrgs.Org> populateListItemFromObject(@NonNull final AvailableOrgs.Org org) {
                                                             final ChoiceIndicatorListItem<AvailableOrgs.Org> orgItem = new ChoiceIndicatorListItem<>(org.organizationName, org, false);
                                                             orgItem.setChecked(org.organizationId.equals(mOriginalOrgId));
                                                             return orgItem;
                                                         }
                                                     }, new CursorAdapterLoader(getActivity(), new CursorThreadOperator(this)), getLoaderManager());
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
                bar.setTitle(R.string.org_select_title);
            }
        }
    }

    @Override
    public boolean onBackPressed() {
        final boolean hasQuery;
        synchronized (this) {
            hasQuery = !TextUtils.isEmpty(mSearchQuery);
            if (hasQuery) {
                mSearchQuery = null;
            }
        }

        if (hasQuery) {
            mViewHolder.mSearchView.setQuery("", false);
            return true;
        }

        //Could return false, but we may do some default behaviour in the future
        return super.onBackPressed();
    }

    static class ViewHolder {
        final View mRootView;
        final SearchView mSearchView;
        final SwipeRefreshLayout mSwipeRefreshLayout;
        final RecyclerView mOrgListView;

        public ViewHolder(final View root) throws IllegalArgumentException, NullPointerException {
            if (root == null) {
                throw new IllegalArgumentException("root may not be null");
            }

            mRootView = root;

            mSearchView = root.findViewById(R.id.org_search_searchview);
            mSwipeRefreshLayout = root.findViewById(R.id.refresh_layout_org_select);
            mOrgListView = mSwipeRefreshLayout.findViewById(R.id.orgselect_listView);

            if (mSearchView == null || mOrgListView == null) {
                throw new NullPointerException("ViewHolder failed to find all views");
            }
        }
    }
}