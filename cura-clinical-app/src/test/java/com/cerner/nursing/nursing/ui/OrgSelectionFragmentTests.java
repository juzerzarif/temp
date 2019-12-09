package com.cerner.nursing.nursing.ui;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.SearchView;

import com.android.internal.view.menu.MenuItemImpl;
import com.cerner.cura.base.CuraAuthnFragment;
import com.cerner.cura.base.CuraRefreshAuthnFragment;
import com.cerner.cura.datamodel.AvailableOrgs;
import com.cerner.cura.datamodel.StoredOrgRelation;
import com.cerner.cura.demographics.datamodel.PatientDemogDetail;
import com.cerner.cura.requestor.IDataRetriever;
import com.cerner.cura.requestor.MockDataManager;
import com.cerner.cura.test.helper.TestUtils;
import com.cerner.cura.ui.elements.ChoiceIndicatorListItem;
import com.cerner.cura.ui.elements.CursorAdapterLoader;
import com.cerner.cura.ui.elements.CursorRecyclerAdapter;
import com.cerner.cura.ui.elements.IListItem;
import com.cerner.cura.ui.elements.ListArrayRecyclerAdapter;
import com.cerner.ion.request.CernResponse;
import com.cerner.ion.security.DialogController;
import com.cerner.ion.security.IonAuthnActivity;
import com.cerner.ion.session.IonSessionUtils;
import com.cerner.nursing.nursing.R;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

import static com.cerner.cura.test.helper.TestUtils.getVariable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests {@link com.cerner.nursing.nursing.ui.OrgSelectionFragment}.
 *
 * @author Sandeep Kuturu (SK028413)
 */
@RunWith (RobolectricTestRunner.class)
public class OrgSelectionFragmentTests {
    private SettingsActivity mockActivity;
    private OrgSelectionFragment mFragment;

    @BeforeClass
    public static void ClassSetup() {
        System.gc();
    }

    /**
     * create mock activity to add fragment into it
     */
    @Before
    public void setup() {
        TestUtils.setStaticVariable(MockDataManager.class, "smState", MockDataManager.MockState.READ);
        mockActivity = Robolectric.buildActivity(SettingsActivity.class).create().start().get();
        mockActivity.onFragmentSelected(OrgSelectionFragment.class, null);
        mFragment = (OrgSelectionFragment) mockActivity.getFragmentManager().findFragmentByTag(OrgSelectionFragment.class.toString());
    }

    /**
     * Clean up fragment and mock activity
     */
    @After
    public void tearDown() {
        mFragment = null;
        mockActivity = null;
    }

    @Test
    public void orgSearchDbHelper_create() {
        final Object countDownTimer = getVariable(mFragment, "mOrgDatabaseHelper");
        final SQLiteDatabase database = Mockito.mock(SQLiteDatabase.class);
        TestUtils.invokePrivateMethod(countDownTimer, "onCreate", new Class[]{SQLiteDatabase.class}, database);
        verify(database).execSQL(Mockito.anyString());
    }

    @Test
    public void orgSearchDbHelper_update() {
        final Object countDownTimer = getVariable(mFragment, "mOrgDatabaseHelper");
        final SQLiteDatabase database = Mockito.mock(SQLiteDatabase.class);
        TestUtils.invokePrivateMethod(countDownTimer, "onUpgrade", new Class[]{SQLiteDatabase.class, int.class, int.class}, database, 1, 2);
        verify(database, Mockito.times(2)).execSQL(Mockito.anyString());
    }

    @Test
    public void orgSearchDbHelper_downgrade() {
        final Object countDownTimer = getVariable(mFragment, "mOrgDatabaseHelper");
        final SQLiteDatabase database = Mockito.mock(SQLiteDatabase.class);
        TestUtils.invokePrivateMethod(countDownTimer, "onDowngrade", new Class[]{SQLiteDatabase.class, int.class, int.class}, database, 1, 2);
        verify(database, Mockito.times(2)).execSQL(Mockito.anyString());
    }

    @Test
    public void orgSearchDbHelper_convertCursorToOrganization() {
        final Object countDownTimer = getVariable(mFragment, "mOrgDatabaseHelper");
        final Cursor cursor = Mockito.mock(Cursor.class);
        doReturn("SomeID1").when(cursor).getString(1);
        doReturn("Org name").when(cursor).getString(2);

        final AvailableOrgs.Org org = TestUtils.invokePrivateMethod(countDownTimer, "convertCursorToOrganization", new Class[]{Cursor.class}, cursor);
        assertEquals("SomeID1", org.organizationId);
        assertEquals("Org name", org.organizationName);
    }

    @Test
    public void onGlobalLayout_fragmentOutOfScope() {
        final ViewTreeObserver.OnGlobalLayoutListener orgSelectGlobalLayout = TestUtils.invokePrivateConstructor(
                ViewTreeObserver.OnGlobalLayoutListener.class, "com.cerner.nursing.nursing.ui.OrgSelectionFragment$OrgSelectGlobalLayout",
                new Class[]{OrgSelectionFragment.class}, new Object[]{null});

        orgSelectGlobalLayout.onGlobalLayout();
        assertFalse(TestUtils.getVariable(orgSelectGlobalLayout, "mInitializedOriginal"));
    }

    @Test
    public void onGlobalLayout_nulls() {
        final ViewTreeObserver.OnGlobalLayoutListener orgSelectGlobalLayout = TestUtils.invokePrivateConstructor(
                ViewTreeObserver.OnGlobalLayoutListener.class, "com.cerner.nursing.nursing.ui.OrgSelectionFragment$OrgSelectGlobalLayout",
                                                                                new Class[]{OrgSelectionFragment.class}, mFragment);

        orgSelectGlobalLayout.onGlobalLayout();

        TestUtils.setVariable(mFragment, "mOriginalOrgId", "origSelected");
        orgSelectGlobalLayout.onGlobalLayout();

        final RecyclerView orgListView = ((OrgSelectionFragment.ViewHolder) TestUtils.getVariable(mFragment, "mViewHolder")).mOrgListView;
        orgListView.setAdapter(Mockito.mock(ListArrayRecyclerAdapter.class));
        orgSelectGlobalLayout.onGlobalLayout();

        final CursorRecyclerAdapter cursorRecyclerAdapter = Mockito.mock(CursorRecyclerAdapter.class);
        orgListView.setAdapter(cursorRecyclerAdapter);
        orgSelectGlobalLayout.onGlobalLayout();

        assertFalse(TestUtils.getVariable(orgSelectGlobalLayout, "mInitializedOriginal"));

        TestUtils.setVariable(mFragment, "mCursorAdapter", cursorRecyclerAdapter);
        doReturn(1).when(cursorRecyclerAdapter).getItemCount();
        doReturn(null).when(cursorRecyclerAdapter).getObject(0);
        orgSelectGlobalLayout.onGlobalLayout();

        assertTrue(TestUtils.getVariable(orgSelectGlobalLayout, "mInitializedOriginal"));
    }

    @Test
    public void onGlobalLayout_itemAtBottomOfScreen() {
        final ViewTreeObserver.OnGlobalLayoutListener orgSelectGlobalLayout = TestUtils.invokePrivateConstructor(
                ViewTreeObserver.OnGlobalLayoutListener.class, "com.cerner.nursing.nursing.ui.OrgSelectionFragment$OrgSelectGlobalLayout",
                new Class[]{OrgSelectionFragment.class}, mFragment);

        TestUtils.setVariable(mFragment, "mOriginalOrgId", "origSelected");
        final CursorRecyclerAdapter cursorRecyclerAdapter = Mockito.mock(CursorRecyclerAdapter.class);
        TestUtils.setVariable(mFragment, "mCursorAdapter", cursorRecyclerAdapter);
        ((OrgSelectionFragment.ViewHolder) TestUtils.getVariable(mFragment, "mViewHolder")).mOrgListView.setAdapter(cursorRecyclerAdapter);
        doReturn(10).when(cursorRecyclerAdapter).getItemCount();
        final AvailableOrgs.Org selectedOrg = new AvailableOrgs.Org();
        selectedOrg.organizationId = "origSelected";
        doReturn(selectedOrg).when(cursorRecyclerAdapter).getObject(0);

        orgSelectGlobalLayout.onGlobalLayout();
        assertTrue(TestUtils.getVariable(orgSelectGlobalLayout, "mInitializedOriginal"));
    }

    @Test
    public void onGlobalLayout() {
        final ViewTreeObserver.OnGlobalLayoutListener orgSelectGlobalLayout = TestUtils.invokePrivateConstructor(
                ViewTreeObserver.OnGlobalLayoutListener.class, "com.cerner.nursing.nursing.ui.OrgSelectionFragment$OrgSelectGlobalLayout",
                new Class[]{OrgSelectionFragment.class}, mFragment);

        TestUtils.setVariable(mFragment, "mOriginalOrgId", "origSelected");
        final CursorRecyclerAdapter cursorRecyclerAdapter = Mockito.mock(CursorRecyclerAdapter.class);
        TestUtils.setVariable(mFragment, "mCursorAdapter", cursorRecyclerAdapter);
        ((OrgSelectionFragment.ViewHolder) TestUtils.getVariable(mFragment, "mViewHolder")).mOrgListView.setAdapter(cursorRecyclerAdapter);
        doReturn(3).when(cursorRecyclerAdapter).getItemCount();
        doReturn(null).when(cursorRecyclerAdapter).getObject(0);
        final AvailableOrgs.Org otherOrg = new AvailableOrgs.Org();
        otherOrg.organizationId = "notSelected";
        doReturn(otherOrg).when(cursorRecyclerAdapter).getObject(1);
        final AvailableOrgs.Org selectedOrg = new AvailableOrgs.Org();
        selectedOrg.organizationId = "origSelected";
        doReturn(selectedOrg).when(cursorRecyclerAdapter).getObject(2);

        orgSelectGlobalLayout.onGlobalLayout();
        assertTrue(TestUtils.getVariable(orgSelectGlobalLayout, "mInitializedOriginal"));

        orgSelectGlobalLayout.onGlobalLayout();
    }

    @Test
    public void onCreate_activityFinishing() {
        mFragment = Mockito.spy(mFragment);

        mFragment.getActivity().finish();
        mFragment.onCreate(null);

        verify(mFragment, never()).getArguments();
    }

    @Test
    public void onCreate_withArgs() {
        final Bundle bundle = new Bundle();
        bundle.putString(TestUtils.getStaticVariable(CuraAuthnFragment.class, "CURA_BUNDLE_PARAMETER"), "testOrdId");
        mockActivity.onFragmentSelected(OrgSelectionFragment.class, bundle, false, true);
        mFragment = (OrgSelectionFragment) mockActivity.getFragmentManager().findFragmentByTag(OrgSelectionFragment.class.toString());

        assertEquals("testOrdId", TestUtils.getVariable(mFragment, "mOriginalOrgId"));
    }

    @Test
    public void onCreateView() {
        //noinspection ConstantConditions
        assertEquals(mFragment.getView().findViewById(R.id.refresh_layout_org_select), getVariable(mFragment, "mRefreshLayout"));
    }

    @Test
    public void onCreateView_activityFinishing() {
        mFragment = Mockito.spy(mFragment);
        final LayoutInflater layoutInflater = Mockito.mock(LayoutInflater.class);
        final RecyclerView view = Mockito.mock(RecyclerView.class);
        doReturn(view).when(layoutInflater).inflate(R.layout.org_selection_fragment, null, false);

        mFragment.getActivity().finish();
        mFragment.onCreateView(layoutInflater, null, null);

        verify(view, never()).setHasFixedSize(Mockito.anyBoolean());
    }

    @Test
    public void onCreateView_OnQueryTextListener() {
        mFragment = Mockito.spy(mFragment);
        final OrgSelectionFragment.ViewHolder viewHolder = TestUtils.getVariable(mFragment, "mViewHolder");
        //noinspection unchecked
        final RecyclerView.OnScrollListener scrollListener = ((List<RecyclerView.OnScrollListener>) TestUtils.getVariable(viewHolder.mOrgListView, "mScrollListeners")).get(0);
        TestUtils.setVariable(scrollListener, "this$0", mFragment);

        scrollListener.onScrollStateChanged(null, RecyclerView.SCROLL_STATE_IDLE);
        scrollListener.onScrollStateChanged(null, RecyclerView.SCROLL_STATE_SETTLING);
        verify(mFragment, never()).getActivity();

        scrollListener.onScrollStateChanged(null, RecyclerView.SCROLL_STATE_DRAGGING);
        verify(mFragment).getActivity();
    }

    @Test
    public void onCreateView_onScrollStateChanged() {
        final OrgSelectionFragment.ViewHolder viewHolder = TestUtils.getVariable(mFragment, "mViewHolder");
        viewHolder.mSearchView.setQuery("testQueryString", true);

        assertEquals("testQueryString", TestUtils.getVariable(mFragment, "mSearchQuery"));
    }

    @Test
    public void initializeCursorAdapter_savedInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putString(TestUtils.getStaticVariable(OrgSelectionFragment.class, "ORG_SEARCH_QUERY"), "testOldQueryString");
        TestUtils.invokePrivateMethod(mFragment, "initializeCursorAdapter", new Class[]{Bundle.class}, bundle);

        assertEquals("testOldQueryString", TestUtils.getVariable(mFragment, "mSearchQuery"));
    }

    @Test
    public void initializeCursorAdapter_ListItemFromCursor() {
        //noinspection unchecked
        final CursorRecyclerAdapter.ListItemFromCursor<AvailableOrgs.Org, ChoiceIndicatorListItem<AvailableOrgs.Org>> itemFromCursor =
                TestUtils.getVariable(TestUtils.getVariable(mFragment, "mCursorAdapter"), "mListItemFromCursor");
        TestUtils.setVariable(mFragment, "mOriginalOrgId", "selected");

        assertNotNull(itemFromCursor.convertCursorToObject(Mockito.mock(Cursor.class)));

        final AvailableOrgs.Org org = new AvailableOrgs.Org();
        org.organizationId = "notSelected";
        ChoiceIndicatorListItem<AvailableOrgs.Org> listItem = itemFromCursor.populateListItemFromObject(org);
        assertNotNull(listItem);
        assertFalse(listItem.isChecked());

        org.organizationId = "selected";
        listItem = itemFromCursor.populateListItemFromObject(org);
        assertNotNull(listItem);
        assertTrue(listItem.isChecked());
    }

    @Test
    public void CursorThreadOperator_onPerformThreadedDataUpdate_fragmentOutOfScope() {
        final CursorAdapterLoader.OnPerformThreadedOperation performer = TestUtils.invokePrivateConstructor(
                CursorAdapterLoader.OnPerformThreadedOperation.class, "com.cerner.nursing.nursing.ui.OrgSelectionFragment$CursorThreadOperator",
                new Class[]{OrgSelectionFragment.class}, new Object[]{null});
        assertFalse(performer.onPerformThreadedDataUpdate());
    }

    @Test
    public void initializeCursorAdapter_onPerformThreadedDataUpdate_performer_nulls() {
        final CursorAdapterLoader.OnPerformThreadedOperation performer = TestUtils.getVariable(TestUtils.getVariable(TestUtils.getVariable(mFragment, "mCursorAdapter"), "mCursorAdapterLoader"), "mPerformer");
        assertTrue(performer.onPerformThreadedDataUpdate((Object[]) null));
        assertTrue(performer.onPerformThreadedDataUpdate());
        assertTrue(performer.onPerformThreadedDataUpdate(new Object()));
        assertTrue(performer.onPerformThreadedDataUpdate(new ArrayList<AvailableOrgs.Org>()));
    }

    @Test
    public void initializeCursorAdapter_onPerformThreadedDataUpdate_performer() {
        final CursorAdapterLoader.OnPerformThreadedOperation performer = TestUtils.getVariable(TestUtils.getVariable(TestUtils.getVariable(mFragment, "mCursorAdapter"), "mCursorAdapterLoader"), "mPerformer");
        final SQLiteDatabase database = Mockito.spy((SQLiteDatabase) TestUtils.getVariable(TestUtils.getVariable(mFragment, "mOrgDatabaseHelper"), "mDatabase"));
        TestUtils.setVariable(TestUtils.getVariable(mFragment, "mOrgDatabaseHelper"), "mDatabase", database);
        final List<AvailableOrgs.Org> orgs = new ArrayList<>();
        orgs.add(null);
        orgs.add(new AvailableOrgs.Org());

        assertTrue(performer.onPerformThreadedDataUpdate(orgs));
        verify(database).insert(Mockito.anyString(), Mockito.isNull(), Mockito.any(ContentValues.class));
    }

    @Test
    public void CursorThreadOperator_onPerformThreadedCursorUpdate_fragmentOutOfScope() {
        final CursorAdapterLoader.OnPerformThreadedOperation performer = TestUtils.invokePrivateConstructor(
                CursorAdapterLoader.OnPerformThreadedOperation.class, "com.cerner.nursing.nursing.ui.OrgSelectionFragment$CursorThreadOperator",
                new Class[]{OrgSelectionFragment.class}, new Object[]{null});
        assertNull(performer.onPerformThreadedCursorUpdate());
    }

    @Test
    public void initializeCursorAdapter_onPerformThreadedCursorUpdate_performer_queryEmpty() {
        final CursorAdapterLoader.OnPerformThreadedOperation performer = TestUtils.getVariable(TestUtils.getVariable(TestUtils.getVariable(mFragment, "mCursorAdapter"), "mCursorAdapterLoader"), "mPerformer");
        TestUtils.setVariable(mFragment, "mSearchQuery", null);
        final SQLiteDatabase database = Mockito.spy((SQLiteDatabase) TestUtils.getVariable(TestUtils.getVariable(mFragment, "mOrgDatabaseHelper"), "mDatabase"));
        TestUtils.setVariable(TestUtils.getVariable(mFragment, "mOrgDatabaseHelper"), "mDatabase", database);

        assertNotNull(performer.onPerformThreadedCursorUpdate());
        verify(database).query(Mockito.anyString(), Mockito.any(String[].class), Mockito.isNull(), Mockito.isNull(),
                               Mockito.isNull(), Mockito.isNull(), Mockito.anyString());
    }

    @Test
    public void initializeCursorAdapter_onPerformThreadedCursorUpdate_performer_queryInvalid() {
        final CursorAdapterLoader.OnPerformThreadedOperation performer = TestUtils.getVariable(TestUtils.getVariable(TestUtils.getVariable(mFragment, "mCursorAdapter"), "mCursorAdapterLoader"), "mPerformer");
        TestUtils.setVariable(mFragment, "mSearchQuery", "****");   //since the *s are implicit they will be stripped and this will be left as an empty string
        final SQLiteDatabase database = Mockito.spy((SQLiteDatabase) TestUtils.getVariable(TestUtils.getVariable(mFragment, "mOrgDatabaseHelper"), "mDatabase"));
        TestUtils.setVariable(TestUtils.getVariable(mFragment, "mOrgDatabaseHelper"), "mDatabase", database);

        assertNotNull(performer.onPerformThreadedCursorUpdate());
        verify(database).query(Mockito.anyString(), Mockito.any(String[].class), Mockito.isNull(), Mockito.isNull(),
                               Mockito.isNull(), Mockito.isNull(), Mockito.anyString());
    }

    @Test
    public void initializeCursorAdapter_onPerformThreadedCursorUpdate_performer_queryValid() {
        final CursorAdapterLoader.OnPerformThreadedOperation performer = TestUtils.getVariable(TestUtils.getVariable(TestUtils.getVariable(mFragment, "mCursorAdapter"), "mCursorAdapterLoader"), "mPerformer");
        TestUtils.setVariable(mFragment, "mSearchQuery", "**mock query**");   //"*something*" and "something" will query for the same thing since it is implicit
        final SQLiteDatabase database = Mockito.spy((SQLiteDatabase) TestUtils.getVariable(TestUtils.getVariable(mFragment, "mOrgDatabaseHelper"), "mDatabase"));
        TestUtils.setVariable(TestUtils.getVariable(mFragment, "mOrgDatabaseHelper"), "mDatabase", database);

        assertNotNull(performer.onPerformThreadedCursorUpdate());
        verify(database).query(Mockito.anyString(), Mockito.any(String[].class), Mockito.anyString(), Mockito.any(String[].class),
                               Mockito.isNull(), Mockito.isNull(), Mockito.anyString());
    }

    @Test
    public void onCreateOptionsMenu_getActivityNull() {
        mFragment = Mockito.spy(mFragment);
        final Menu menu = Mockito.mock(Menu.class);
        final MenuInflater inflater = Mockito.mock(MenuInflater.class);
        doReturn(null).when(mFragment).getActivity();
        mFragment.onCreateOptionsMenu(menu, inflater);
        verify(inflater, never()).inflate(Mockito.anyInt(), Mockito.any(Menu.class));
    }

    @Test
    public void onCreateOptionsMenu_getIntentNull() {
        mockActivity = Mockito.spy(mockActivity);
        mFragment = Mockito.spy(mFragment);
        final Menu menu = Mockito.mock(Menu.class);
        final MenuInflater inflater = Mockito.mock(MenuInflater.class);
        doReturn(mockActivity).when(mFragment).getActivity();
        doReturn(null).when(mockActivity).getIntent();
        mFragment.onCreateOptionsMenu(menu, inflater);
        verify(inflater, never()).inflate(Mockito.anyInt(), Mockito.any(Menu.class));
    }

    @Test
    public void onCreateOptionsMenu_getIntentParamDefault() {
        mockActivity = Mockito.spy(mockActivity);
        mFragment = Mockito.spy(mFragment);
        final Menu menu = Mockito.mock(Menu.class);
        final MenuInflater inflater = Mockito.mock(MenuInflater.class);
        final Bundle bundle = new Bundle();
        bundle.putInt(SettingsActivity.LAUNCH_ACTION_IDENTIFIER, SettingsActivity.LAUNCH_DEFAULT);
        doReturn(mockActivity).when(mFragment).getActivity();
        doReturn(bundle).when(mFragment).getArguments();
        mFragment.onCreateOptionsMenu(menu, inflater);
        verify(inflater, never()).inflate(Mockito.anyInt(), Mockito.any(Menu.class));
    }

    @Test
    public void onCreateOptionsMenu_getIntentParamPickOrg() {
        mockActivity = Mockito.spy(mockActivity);
        mFragment = Mockito.spy(mFragment);
        final Menu menu = Mockito.mock(Menu.class);
        final MenuInflater inflater = Mockito.mock(MenuInflater.class);
        final Bundle bundle = new Bundle();
        bundle.putInt(SettingsActivity.LAUNCH_ACTION_IDENTIFIER, SettingsActivity.LAUNCH_PICK_ORG);
        doReturn(mockActivity).when(mFragment).getActivity();
        doReturn(bundle).when(mFragment).getArguments();
        Mockito.doNothing().when(inflater).inflate(Mockito.anyInt(), Mockito.any(Menu.class));
        mFragment.onCreateOptionsMenu(menu, inflater);
        verify(inflater).inflate(Mockito.anyInt(), Mockito.any(Menu.class));
    }

    @Test
    public void onCreateOptionsMenu_RefreshActionVisible() {
        mockActivity = Mockito.spy(mockActivity);
        mFragment = Mockito.spy(mFragment);
        final Menu menu = Mockito.mock(Menu.class);
        final MenuInflater inflater = Mockito.mock(MenuInflater.class);
        final Bundle bundle = new Bundle();
        bundle.putInt(SettingsActivity.LAUNCH_ACTION_IDENTIFIER, SettingsActivity.LAUNCH_PICK_ORG);
        doReturn(mockActivity).when(mFragment).getActivity();
        doReturn(bundle).when(mFragment).getArguments();
        Mockito.doNothing().when(inflater).inflate(Mockito.anyInt(), Mockito.any(Menu.class));
        mFragment.onCreateOptionsMenu(menu, inflater);
        verify(inflater).inflate(Mockito.anyInt(), Mockito.any(Menu.class));
    }

    @Test
    public void onOptionsItemSelected_logOut() {
        mockActivity = Mockito.spy(mockActivity);
        mFragment = Mockito.spy(mFragment);

        IonSessionUtils.setLoggingOutToken("test");
        TestUtils.setStaticVariable(MockDataManager.class, "smState", MockDataManager.MockState.NOTHING);

        final MenuItem menuItem = Mockito.mock(MenuItemImpl.class);
        doReturn(R.id.menu_item_logout).when(menuItem).getItemId();
        doReturn(mockActivity).when(mFragment).getIonActivity();
        mFragment.onOptionsItemSelected(menuItem);
    }

    @Test
    public void onOptionsItemSelected_defaultCase() {
        final MenuItem menuItem = Mockito.mock(MenuItemImpl.class);
        doReturn(R.id.menu_item_logout).when(menuItem).getItemId();
        mFragment.onOptionsItemSelected(menuItem);
    }

    @Test
    public void setupActionBar_activityNull() {
        mFragment = Mockito.spy(mFragment);

        doReturn(null).when(mFragment).getActivity();

        mFragment.setupActionBar();

        verify(mFragment).getActivity();
    }

    @Test
    public void setupActionBar_actionBarNull() {
        mockActivity = Mockito.spy(mockActivity);
        mFragment = Mockito.spy(mFragment);

        doReturn(mockActivity).when(mFragment).getActivity();
        doReturn(null).when(mockActivity).getSupportActionBar();

        mFragment.setupActionBar();
    }

    @Test
    public void setupActionBar() {
        mockActivity = Mockito.spy(mockActivity);
        mFragment = Mockito.spy(mFragment);
        final ActionBar bar = Mockito.mock(ActionBar.class);

        doReturn(mockActivity).when(mFragment).getActivity();
        doReturn(bar).when(mockActivity).getSupportActionBar();

        mFragment.setupActionBar();

        verify(bar).setTitle(R.string.org_select_title);
    }

    @Test
    public void onAuthnStart_getView() {
        mFragment = Mockito.spy(mFragment);
        doReturn(Mockito.mock(ViewGroup.class)).when(mFragment).getView();
        Mockito.doNothing().when(mFragment).getData(IDataRetriever.DataArgs.REFRESH);
        Mockito.doNothing().when(mFragment).setFragmentVisibility(false);
        mFragment.onAuthnStart();
        verify(mFragment).getData(IDataRetriever.DataArgs.REFRESH);
        verify(mFragment).setFragmentVisibility(false);
    }

    @Test
    public void onAttach_validActivity() {
        assertNotNull(getVariable(mFragment, "mCallback"));
    }

    @Test (expected = ClassCastException.class)
    public void onAttach_invalidActivity() {
        TestUtils.setVariable(mFragment, "mCallback", null);
        mFragment.onAttach(Mockito.mock(IonAuthnActivity.class));
    }

    @Test
    public void onDetach() {
        final OrgSelectionFragment.OnOrgSelectedListener listener = () -> {};
        TestUtils.setVariable(mFragment, "mCallback", listener);
        mFragment.onDetach();
        assertNull(getVariable(mFragment, "mCallback"));
    }

    @Test
    public void onAuthnResume() {
        final RecyclerView recyclerView = Mockito.mock(RecyclerView.class);
        final ViewTreeObserver treeObserver = Mockito.mock(ViewTreeObserver.class);
        doReturn(treeObserver).when(recyclerView).getViewTreeObserver();

        final ViewGroup rootView = Mockito.mock(ViewGroup.class);
        final SwipeRefreshLayout refreshLayout = Mockito.mock(SwipeRefreshLayout.class);
        doReturn(refreshLayout).when(rootView).findViewById(R.id.refresh_layout_org_select);
        doReturn(Mockito.mock(SearchView.class)).when(rootView).findViewById(R.id.org_search_searchview);
        doReturn(recyclerView).when(refreshLayout).findViewById(R.id.orgselect_listView);
        final OrgSelectionFragment.ViewHolder viewHolder = new OrgSelectionFragment.ViewHolder(rootView);

        TestUtils.setVariable(mFragment, "mViewHolder", viewHolder);
        mFragment.onAuthnResume();
        verify(treeObserver).addOnGlobalLayoutListener(TestUtils.getVariable(mFragment, "mGlobalLayoutListener"));
    }

    @Test
    public void onPause() {
        final RecyclerView recyclerView = Mockito.mock(RecyclerView.class);
        final ViewTreeObserver treeObserver = Mockito.mock(ViewTreeObserver.class);
        doReturn(treeObserver).when(recyclerView).getViewTreeObserver();

        final ViewGroup rootView = Mockito.mock(ViewGroup.class);
        final SwipeRefreshLayout refreshLayout = Mockito.mock(SwipeRefreshLayout.class);
        doReturn(refreshLayout).when(rootView).findViewById(R.id.refresh_layout_org_select);
        doReturn(Mockito.mock(SearchView.class)).when(rootView).findViewById(R.id.org_search_searchview);
        doReturn(recyclerView).when(refreshLayout).findViewById(R.id.orgselect_listView);
        final OrgSelectionFragment.ViewHolder viewHolder = new OrgSelectionFragment.ViewHolder(rootView);

        TestUtils.setVariable(mFragment, "mViewHolder", viewHolder);
        mFragment.onPause();
        verify(treeObserver).removeOnGlobalLayoutListener(TestUtils.getVariable(mFragment, "mGlobalLayoutListener"));
    }

    @Test
    public void onDestroy() {
        mFragment = Mockito.spy(mFragment);
        mFragment.onDestroy();
        verify(mFragment).setHasBackPressAction(false);

        TestUtils.setVariable(mFragment, "mOrgDatabaseHelper", null);
        mFragment.onDestroy();
        verify(mFragment, Mockito.times(2)).setHasBackPressAction(false);
    }

    @Test
    public void onErrorResponse_valid() {
        mFragment = Mockito.spy(mFragment);
        mFragment.onErrorResponse(null, null);
        verify(mFragment, never()).getActivity();
    }

    @Test
    public void onErrorResponse_randomObject() {
        mFragment = Mockito.spy(mFragment);
        mFragment.onErrorResponse(null, Object.class);
        verify(mFragment, Mockito.atLeastOnce()).getActivity();
    }

    @Test
    public void onNoContentResponse() {
        mFragment = Mockito.spy(mFragment);
        final StoredOrgRelation selectedOrg = new StoredOrgRelation();
        selectedOrg.org_id = "1234";
        selectedOrg.org_name = "MyOrganization";
        TestUtils.setVariable(mFragment, "mSelectedOrg", selectedOrg);

        mFragment.onNoContentResponse(null, null);
        verify(mFragment).setFragmentVisibility(true);
    }

    @Test
    public void onNoContentResponse_classNull() {
        mFragment = Mockito.spy(mFragment);
        final StoredOrgRelation selectedOrg = new StoredOrgRelation();
        selectedOrg.org_id = "1234";
        selectedOrg.org_name = "MyOrganization";
        TestUtils.setVariable(mFragment, "mSelectedOrg", selectedOrg);

        mFragment.onNoContentResponse(null, null);
        verify(mFragment).setFragmentVisibility(true);
    }

    @Test
    public void onNoContentResponse_InvalidClass() {
        mFragment = Mockito.spy(mFragment);
        final StoredOrgRelation selectedOrg = new StoredOrgRelation();
        selectedOrg.org_id = "1234";
        selectedOrg.org_name = "MyOrganization";
        TestUtils.setVariable(mFragment, "mSelectedOrg", selectedOrg);

        mFragment.onNoContentResponse(null, Integer.class);
        verify(mFragment).setFragmentVisibility(true);
    }

    @Test
    public void onNoContentResponse_callBackNull_selectedNull() {
        mFragment = Mockito.spy(mFragment);
        TestUtils.setVariable(mFragment, "mSelectedOrg", null);
        TestUtils.setVariable(mFragment, "mCallback", null);
        mFragment.onNoContentResponse(null, null);
        verify(mFragment).setFragmentVisibility(true);
    }

    @Test
    public void getData() {
        mFragment = Mockito.spy(mFragment);
        final CuraRefreshAuthnFragment.RefreshData refreshData = Mockito.mock(CuraRefreshAuthnFragment.RefreshData.class);
        final DialogController dialogController = Mockito.mock(DialogController.class);
        doReturn(dialogController).when(mFragment).showProgressDialog(Mockito.any(IDataRetriever.class));
        Mockito.doNothing().when(dialogController).hideProgressDialog();
        TestUtils.setVariable(mFragment, "mRefreshData", refreshData);
        mFragment.getData(IDataRetriever.DataArgs.REFRESH);
        verify(mFragment).getData(IDataRetriever.DataArgs.REFRESH);
    }

    @Test (expected = IllegalArgumentException.class)
    public void onResponse_modelNull() {
        mFragment = Mockito.spy(mFragment);
        final CernResponse response = new CernResponse();
        mFragment.onResponse(response);
    }

    @Test (expected = IllegalArgumentException.class)
    public void onResponse_modelInvalidType() {
        mFragment = Mockito.spy(mFragment);
        final CernResponse response = new CernResponse();
        response.data = new Object();
        mFragment.onResponse(response);
    }

    @Test (expected = IllegalArgumentException.class)
    public void onResponse_viewNull() {
        mFragment = Mockito.spy(mFragment);
        doReturn(null).when(mFragment).getView();
        mFragment.onResponse(new PatientDemogDetail());
        verify(mFragment).setFragmentVisibility(true);
    }

    @Test
    public void onResponse_OrganizationListNull() {
        final AvailableOrgs availableOrgs = new AvailableOrgs();

        mFragment = Mockito.spy(mFragment);
        mFragment.onResponse(availableOrgs);

        verify(mFragment).onNoContentResponse(Mockito.isNull(), Mockito.any(Class.class));
    }

    @Test
    public void onResponse_OrganizationListEmpty() {
        final AvailableOrgs availableOrgs = new AvailableOrgs();
        availableOrgs.accessibleOrganizations = new ArrayList<>();

        mFragment = Mockito.spy(mFragment);
        mFragment.onResponse(availableOrgs);

        verify(mFragment).onNoContentResponse(Mockito.isNull(), Mockito.any(Class.class));
    }

    @Test
    public void onResponse_modelValid() {
        final AvailableOrgs availableOrgs = new AvailableOrgs();
        availableOrgs.accessibleOrganizations = new ArrayList<>();

        final AvailableOrgs.Org organization0 = new AvailableOrgs.Org();
        organization0.organizationId = "0";
        organization0.organizationName = "MY Organization0";

        final AvailableOrgs.Org organization1to15 = new AvailableOrgs.Org();
        organization1to15.organizationId = "1";
        organization1to15.organizationName = "MY Organization1";
        for (int i = 0; i < 14; ++i){
            availableOrgs.accessibleOrganizations.add(organization1to15);
        }

        final OrgSelectionFragment.ViewHolder viewHolder = TestUtils.getVariable(mFragment, "mViewHolder");
        TestUtils.setVariable(viewHolder, "mSearchView", Mockito.spy(viewHolder.mSearchView));
        mFragment = Mockito.spy(mFragment);
        Bundle bundle = new Bundle();
        bundle = Mockito.spy(bundle);
        doReturn("1").when(bundle).getSerializable(Mockito.anyString());
        TestUtils.setVariable(mFragment, "mArguments", bundle);
        mFragment.onResponse(availableOrgs);

        verify(viewHolder.mSearchView).setVisibility(View.GONE);
        verify(mFragment).setFragmentVisibility(true);
        verify(mFragment).notifyResponseReceivedListeners(OrgSelectionFragment.class);

        //noinspection unchecked
        final CursorRecyclerAdapter<AvailableOrgs.Org,ChoiceIndicatorListItem<AvailableOrgs.Org>> cursorAdapter =
                TestUtils.getVariable(mFragment, "mCursorAdapter");
        final IListItem item = new ChoiceIndicatorListItem<>(organization0.organizationName, organization0, false);

        final CuraRefreshAuthnFragment.RefreshData refreshData = Mockito.mock(CuraRefreshAuthnFragment.RefreshData.class);
        final DialogController dialogController = Mockito.mock(DialogController.class);
        doReturn(dialogController).when(mFragment).showProgressDialog(Mockito.mock(IDataRetriever.class));
        Mockito.doNothing().when(dialogController).hideProgressDialog();
        TestUtils.setVariable(mFragment, "mRefreshData", refreshData);

        //noinspection ConstantConditions
        cursorAdapter.getOnItemClickListener().onItemClick(null, item);

        final StoredOrgRelation org = getVariable(mFragment, "mSelectedOrg");
        assertEquals(organization0.organizationId, org.org_id);

        availableOrgs.accessibleOrganizations.add(organization1to15);
        mFragment.onResponse(availableOrgs);
        verify(viewHolder.mSearchView).setVisibility(View.VISIBLE);
    }

    @Test
    public void onResponse_clickOrgNull() {
        final AvailableOrgs availableOrgs = new AvailableOrgs();
        availableOrgs.accessibleOrganizations = new ArrayList<>();

        final AvailableOrgs.Org organization1 = new AvailableOrgs.Org();
        organization1.organizationId = "1";
        organization1.organizationName = "MY Organization1";
        availableOrgs.accessibleOrganizations.add(organization1);

        mFragment = Mockito.spy(mFragment);
        mFragment.onResponse(availableOrgs);

        verify(mFragment).setFragmentVisibility(true);
        verify(mFragment).notifyResponseReceivedListeners(OrgSelectionFragment.class);

        //noinspection unchecked
        final CursorRecyclerAdapter<AvailableOrgs.Org,ChoiceIndicatorListItem<AvailableOrgs.Org>> cursorAdapter =
                TestUtils.getVariable(mFragment, "mCursorAdapter");

        //noinspection ConstantConditions
        cursorAdapter.getOnItemClickListener().onItemClick(null, new ChoiceIndicatorListItem<>("orgName", null, false));

        assertNull(getVariable(mFragment, "mSelectedOrg"));
    }

    @Test
    public void onResponse_clickItemInValid() {
        final AvailableOrgs availableOrgs = new AvailableOrgs();
        availableOrgs.accessibleOrganizations = new ArrayList<>();

        final AvailableOrgs.Org organization1 = new AvailableOrgs.Org();
        organization1.organizationId = "1";
        organization1.organizationName = "MY Organization1";
        availableOrgs.accessibleOrganizations.add(organization1);

        mFragment = Mockito.spy(mFragment);
        mFragment.onResponse(availableOrgs);

        verify(mFragment).setFragmentVisibility(true);
        verify(mFragment).notifyResponseReceivedListeners(OrgSelectionFragment.class);

        //noinspection unchecked
        final CursorRecyclerAdapter<AvailableOrgs.Org,ChoiceIndicatorListItem<AvailableOrgs.Org>> cursorAdapter =
                TestUtils.getVariable(mFragment, "mCursorAdapter");
        final AvailableOrgs.Org organization0 = new AvailableOrgs.Org();
        organization0.organizationId = "selectedId";
        organization0.organizationName = "MY Organization0";
        TestUtils.setVariable(mFragment, "mOriginalOrgId", organization0.organizationId);

        //noinspection ConstantConditions
        cursorAdapter.getOnItemClickListener().onItemClick(null, new ChoiceIndicatorListItem<>(organization0.organizationName, organization0, false));

        assertNull(getVariable(mFragment, "mSelectedOrg"));
    }

    @Test
    public void onResponse_ValidArguments() {
        final AvailableOrgs availableOrgs = new AvailableOrgs();
        availableOrgs.accessibleOrganizations = new ArrayList<>();

        final AvailableOrgs.Org organization1 = new AvailableOrgs.Org();
        organization1.organizationId = "1";
        organization1.organizationName = "MY Organization1";
        availableOrgs.accessibleOrganizations.add(organization1);

        final AvailableOrgs.Org organization2 = new AvailableOrgs.Org();
        organization2.organizationId = "2";
        organization2.organizationName = "MY Organization2";
        availableOrgs.accessibleOrganizations.add(organization2);

        mFragment = Mockito.spy(mFragment);
        final Bundle bundle = new Bundle();
        bundle.putSerializable("CURA_BUNDLE_PARAMETER", "2");
        TestUtils.setVariable(mFragment, "mArguments", bundle);
        mFragment.onResponse(availableOrgs);

        verify(mFragment).setFragmentVisibility(true);
        verify(mFragment).notifyResponseReceivedListeners(OrgSelectionFragment.class);
    }

    @Test
    public void onResponse_organizationIdNull() {
        final AvailableOrgs availableOrgs = new AvailableOrgs();
        availableOrgs.accessibleOrganizations = new ArrayList<>();

        final AvailableOrgs.Org organization1 = new AvailableOrgs.Org();
        organization1.organizationId = null;
        organization1.organizationName = "MY Organization1";
        availableOrgs.accessibleOrganizations.add(organization1);

        final AvailableOrgs.Org organization2 = new AvailableOrgs.Org();
        organization2.organizationId = "2";
        organization2.organizationName = "MY Organization2";
        availableOrgs.accessibleOrganizations.add(organization2);

        mFragment = Mockito.spy(mFragment);
        final Bundle bundle = new Bundle();
        bundle.putSerializable("CURA_BUNDLE_PARAMETER", "4");
        TestUtils.setVariable(mFragment, "mArguments", bundle);
        mFragment.onResponse(availableOrgs);

        verify(mFragment).setFragmentVisibility(true);
        verify(mFragment).notifyResponseReceivedListeners(OrgSelectionFragment.class);
    }

    @Test
    public void onResponse_userOrganizationNull() {
        final AvailableOrgs availableOrgs = new AvailableOrgs();
        availableOrgs.accessibleOrganizations = new ArrayList<>();

        final AvailableOrgs.Org organization1 = new AvailableOrgs.Org();
        organization1.organizationId = "1";
        organization1.organizationName = "MY Organization1";
        availableOrgs.accessibleOrganizations.add(organization1);

        final AvailableOrgs.Org organization2 = new AvailableOrgs.Org();
        organization2.organizationId = "2";
        organization2.organizationName = "MY Organization2";
        availableOrgs.accessibleOrganizations.add(organization2);

        mFragment = Mockito.spy(mFragment);
        TestUtils.setVariable(mFragment, "mArguments", new Bundle());
        mFragment.onResponse(availableOrgs);

        verify(mFragment).setFragmentVisibility(true);
        verify(mFragment).notifyResponseReceivedListeners(OrgSelectionFragment.class);
    }

    @Test
    public void onBackPressed_searchQueryNull() {
        TestUtils.setVariable(mFragment, "mSearchQuery", null);
        mFragment.onBackPressed();
    }

    @Test
    public void onBackPressed() {
        TestUtils.setVariable(mFragment, "mSearchQuery", "mockQuery");
        mFragment.onBackPressed();
        assertTrue(((String) TestUtils.getVariable(mFragment, "mSearchQuery")).isEmpty());
    }

    @Test (expected = IllegalArgumentException.class)
    public void viewHolder_rootNull() {
        new OrgSelectionFragment.ViewHolder(null);
    }

    @Test (expected = NullPointerException.class)
    public void viewHolder_orgListNull() {
        final ViewGroup rootView = Mockito.mock(ViewGroup.class);
        doReturn(Mockito.mock(SwipeRefreshLayout.class)).when(rootView).findViewById(R.id.refresh_layout_org_select);
        doReturn(Mockito.mock(SearchView.class)).when(rootView).findViewById(R.id.org_search_searchview);
        new OrgSelectionFragment.ViewHolder(rootView);
    }

    @Test (expected = NullPointerException.class)
    public void viewHolder_serachViewNull() {
        final ViewGroup rootView = Mockito.mock(ViewGroup.class);
        final SwipeRefreshLayout refreshLayout = Mockito.mock(SwipeRefreshLayout.class);
        doReturn(refreshLayout).when(rootView).findViewById(R.id.refresh_layout_org_select);
        doReturn(Mockito.mock(RecyclerView.class)).when(refreshLayout).findViewById(R.id.orgselect_listView);
        new OrgSelectionFragment.ViewHolder(rootView);
    }

    @Test (expected = NullPointerException.class)
    public void viewHolder_refreshLayoutNull() {
        final ViewGroup rootView = Mockito.mock(ViewGroup.class);
        doReturn(Mockito.mock(SearchView.class)).when(rootView).findViewById(R.id.org_search_searchview);
        new OrgSelectionFragment.ViewHolder(rootView);
    }
}
