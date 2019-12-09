package com.cerner.nursing.nursing.ui;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.android.internal.view.menu.MenuItemImpl;
import com.android.volley.VolleyError;
import com.cerner.cura.base.CuraRefreshAuthnFragment;
import com.cerner.cura.base.MockAuthenticationManager;
import com.cerner.cura.base.OnDrawerListener;
import com.cerner.cura.base.PatientContext;
import com.cerner.cura.base.UserContext;
import com.cerner.cura.datamodel.PatientList;
import com.cerner.cura.datamodel.PatientListById;
import com.cerner.cura.datamodel.PatientLists;
import com.cerner.cura.datamodel.common.FuzzyDateTime;
import com.cerner.cura.datamodel.common.PatientListPatient;
import com.cerner.cura.medications.legacy.datamodel.MedChartingReference;
import com.cerner.cura.medications.legacy.utils.CodeCache;
import com.cerner.cura.requestor.IDataRetriever;
import com.cerner.cura.requestor.MockDataManager;
import com.cerner.cura.test.TestActivity;
import com.cerner.cura.test.helper.TestUtils;
import com.cerner.cura.ui.elements.ListArrayRecyclerAdapter;
import com.cerner.cura.ui.elements.PatientListItem;
import com.cerner.cura.ui.elements.TextListItem;
import com.cerner.cura.utils.ActiveModuleManager;
import com.cerner.ion.security.DialogController;
import com.cerner.ion.security.IonAuthnActivity;
import com.cerner.ion.session.IonAuthnSessionUtils;
import com.cerner.nursing.nursing.R;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import androidx.test.core.app.ApplicationProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests {@link PatientListFragment}.
 *
 * @author Brad Barnhill (BB024928)
 */
@RunWith (RobolectricTestRunner.class)
public class PatientListFragmentTests {
    // Set of default value to be used in the test
    private static final String mDefaultEncounterId = "test_encounter_id_";
    private static final String mDefaultPatientId = "test_patient_id_";

    private ActivityController<PatientListActivity> mActivityController;
    private PatientListActivity mockActivity;
    private PatientListFragment mFragment;

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
        PatientContext.clearContext();
        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "test_encounter_id_" + System.currentTimeMillis());
        PatientContext.setHasRelationship(true);
        mActivityController = Robolectric.buildActivity(PatientListActivity.class).create();
        mockActivity = mActivityController.get();
        mFragment = (PatientListFragment) mockActivity.getFragmentManager().findFragmentById(R.id.patient_list_fragment);
    }

    /**
     * Clean up fragment and mock activity
     */
    @After
    public void tearDown() {
        mFragment = null;
        mockActivity = null;
        mActivityController = null;
    }

    @Test
    public void onCreateView() {
        assertEquals(mFragment.getView().findViewById(R.id.refresh_layout_patient_list), TestUtils.getVariable(mFragment, "mRefreshLayout"));
    }

    @Test
    public void onCreateView_activityFinishing() {
        mFragment = Mockito.spy(mFragment);
        final LayoutInflater layoutInflater = Mockito.mock(LayoutInflater.class);
        final RecyclerView view = Mockito.mock(RecyclerView.class);
        doReturn(view).when(layoutInflater).inflate(R.layout.recycler_view_basic, null, false);

        mFragment.getActivity().finish();
        mFragment.onCreateView(layoutInflater, null, null);

        verify(view, never()).setHasFixedSize(Mockito.anyBoolean());
    }

    @Test
    public void fragmentIsVisible() {
        assertNotNull(mFragment);
        assertNotNull(mFragment.getActivity());
        assertNotNull(mFragment.getView());
        assertEquals(View.VISIBLE, mFragment.getView().getVisibility());
    }

    @Test
    public void enum_PatientListSortType() {
        TestUtils.verifyEnumStatics(PatientListFragment.PatientListSortType.class);
    }

    @Test
    public void onAttach_NoDrawerListener() {
        TestUtils.setVariable(mFragment, "mDrawerCallback", null);
        mFragment.onAttach(new NoDrawerListenerClass());

        assertNull(TestUtils.getVariable(mFragment, "mDrawerCallback"));
    }

    @Test
    public void fragment_onDetach() {
        final FragmentTransaction fragmentTransaction = mockActivity.getFragmentManager().beginTransaction();
        fragmentTransaction.detach(mockActivity.getFragmentManager().findFragmentById(R.id.patient_list_fragment));
        fragmentTransaction.commit();

        //should never detach because it's coded in the layout
        assertFalse(mockActivity.getFragmentManager().executePendingTransactions());
        assertNotNull(TestUtils.getVariable(mFragment, "mCallback"));
        assertNotNull(TestUtils.getVariable(mFragment, "mDrawerCallback"));

        //this should never happen in production code but want to check if this setting happens to cover that code
        mFragment.onDetach();
        assertNull(TestUtils.getVariable(mFragment, "mCallback"));
        assertNull(TestUtils.getVariable(mFragment, "mDrawerCallback"));
        assertNull(TestUtils.getVariable(mFragment, "mCurrentDialog"));
    }

    @Test
    public void onAuthnStart() {
        mFragment = Mockito.spy(mFragment);
        Mockito.doNothing().when(mFragment).getData(IDataRetriever.DataArgs.REFRESH);
        mFragment.onAuthnStart();
        verify(mFragment).getData(IDataRetriever.DataArgs.REFRESH);
    }

    @Test
    public void getData() {
        mFragment = Mockito.spy(mFragment);
        final CuraRefreshAuthnFragment.RefreshData refreshData = Mockito.mock(CuraRefreshAuthnFragment.RefreshData.class);
        final DialogController dialogController = Mockito.mock(DialogController.class);
        doReturn(dialogController).when(mFragment).showProgressDialog(Mockito.any(IDataRetriever.class));
        Mockito.doNothing().when(dialogController).hideProgressDialog();
        TestUtils.setVariable(mFragment, "mRefreshData", refreshData);

        UserContext.removeContextStorageObject(TestUtils.getStaticVariable(PatientListFragment.class, "PATIENT_LIST_DISPLAYED_KEY"));
        mFragment.getData(IDataRetriever.DataArgs.REFRESH);
        UserContext.putContextStorageObject(TestUtils.getStaticVariable(PatientListFragment.class, "PATIENT_LIST_DISPLAYED_KEY"), "ASSIGNED");
        mFragment.getData(IDataRetriever.DataArgs.REFRESH);
    }

    @Test
    public void getData_codesCached() {
        TestUtils.invokePrivateStaticMethod(MockAuthenticationManager.class, "mockCredentials", new Class[]{Context.class}, ApplicationProvider.getApplicationContext());
        final AtomicReference<MedChartingReference> referenceData = TestUtils.getStaticVariable(CodeCache.class, "smReferenceData");
        referenceData.set(null);
        mFragment.getData(IDataRetriever.DataArgs.NONE);

        referenceData.set(new MedChartingReference());
        mFragment.getData(IDataRetriever.DataArgs.NONE);

        IonAuthnSessionUtils.getAuthnResponse().getCapabilities().remove("nursing/medication_administration");
        mFragment.getData(IDataRetriever.DataArgs.NONE);
    }

    @Test
    public void onCreateOptionsMenu_drawerCallbackNull() {
        TestUtils.setVariable(mFragment, "mDrawerCallback", null);
        final MenuInflater menuInflater = Mockito.mock(MenuInflater.class);
        mFragment.onCreateOptionsMenu(Mockito.mock(Menu.class), menuInflater);

        final OnDrawerListener onDrawerListener = Mockito.mock(OnDrawerListener.class);
        TestUtils.setVariable(mFragment, "mDrawerCallback", onDrawerListener);
        doReturn(true).when(onDrawerListener).isDrawerOpen();
        mFragment.onCreateOptionsMenu(null, null);

        doReturn(false).when(onDrawerListener).isDrawerOpen();
        mFragment.onCreateOptionsMenu(Mockito.mock(Menu.class), menuInflater);
        verify(menuInflater, Mockito.times(2)).inflate(Mockito.anyInt(), Mockito.any(Menu.class));
    }

    @Test
    public void onCreateView_loadSortBy() {
        final LayoutInflater inflater = Mockito.mock(LayoutInflater.class);
        final View view = Mockito.mock(View.class);
        final RecyclerView listView = Mockito.mock(RecyclerView.class);
        doReturn(listView).when(view).findViewById(R.id.patientlist_listView);
        UserContext.putContextStorageObject(TestUtils.getStaticVariable(PatientListFragment.class, "PATIENT_LIST_SORT_BY_KEY"), PatientListFragment.PatientListSortType.NAME);
        doReturn(view).when(inflater).inflate(Mockito.any(Integer.class), Mockito.any(ViewGroup.class), eq(false));
        assertNotNull(mFragment.onCreateView(inflater, Mockito.mock(ViewGroup.class), Mockito.mock(Bundle.class)));
        assertEquals(PatientListFragment.PatientListSortType.NAME, TestUtils.getVariable(mFragment, "mSortBy"));
    }

    @Test
    public void onOptionsItemSelected_changeList() {
        mockActivity = Mockito.spy(mockActivity);
        mFragment = Mockito.spy(mFragment);

        final MenuItem menuItem = Mockito.mock(MenuItemImpl.class);
        Mockito.when(menuItem.getItemId()).thenReturn(R.id.change_list);
        doReturn(null).when(mFragment).getActivity();
        mFragment.onOptionsItemSelected(menuItem);
    }

    @Test
    public void onOptionsItemSelected_sortList() {
        mockActivity = Mockito.spy(mockActivity);
        mFragment = Mockito.spy(mFragment);

        final MenuItem menuItem = Mockito.mock(MenuItemImpl.class);
        Mockito.when(menuItem.getItemId()).thenReturn(R.id.sort_list);
        doReturn(null).when(mFragment).getActivity();
        mFragment.onOptionsItemSelected(menuItem);
    }

    @Test
    public void onOptionsItemSelected_defaultCase() {
        final MenuItem menuItem = Mockito.mock(MenuItemImpl.class);
        Mockito.when(menuItem.getItemId()).thenReturn(R.id.menu_item_logout);
        mFragment.onOptionsItemSelected(menuItem);
    }

    @Test
    public void onResponse_activityNull() {
        mFragment = Mockito.spy(mFragment);
        Mockito.when(mFragment.getActivity()).thenReturn(null);

        mFragment.onResponse((Object) null);
    }

    @Test
    public void onResponse_nullPatientList() {
        UserContext.removeContextStorageObject(TestUtils.getStaticVariable(PatientListFragment.class, "PATIENT_LIST_DISPLAYED_KEY"));
        final PatientList pList = createDefaultPatientListMock();
        pList.patientList = null;

        mFragment.onResponse(pList);

        //null patient list came back
        final RecyclerView list = mFragment.getView().findViewById(R.id.patientlist_listView);
        final ListArrayRecyclerAdapter adapter = (ListArrayRecyclerAdapter) list.getAdapter();
        assertEquals(1, adapter.getItemCount());
        assertEquals(mockActivity.getString(R.string.patientlist_nodataavailable), adapter.get(0).getTitle());
    }

    @Test
    public void onResponse_viewIsNull() {
        final PatientListFragment fragment = new PatientListFragment();
        fragment.onResponse(createDefaultPatientListMock());
    }

    @Test
    public void onResponse_onAttachSetsDrawerCallback() {
        assertNotNull(TestUtils.getVariable(mFragment, "mDrawerCallback"));
        assertEquals(mockActivity, TestUtils.getVariable(mFragment, "mDrawerCallback"));
    }

    @Test (expected = ClassCastException.class)
    public void onResponse_onAttachNoInterfacesImplemented() {
        mFragment.onAttach(Mockito.mock(IonAuthnActivity.class));
    }

    @Test
    public void onResponse_listHeaderPrimary() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -3650);
        final RecyclerView list = mFragment.getView().findViewById(R.id.patientlist_listView);

        final PatientList pList = createDefaultPatientListMock();
        pList.patientListName = "Primary";
        final PatientListPatient patient = new PatientListPatient();
        patient.personId = mDefaultPatientId + System.currentTimeMillis();
        patient.encounterId = mDefaultEncounterId + System.currentTimeMillis();
        patient.nameFullFormatted = "primarypatient";
        patient.sexAbbr = "M";
        patient.bedLocationDisplay = "bed1";
        patient.roomLocationDisplay = "room1";
        patient.dateOfBirth = new FuzzyDateTime();
        patient.dateOfBirth.dateTime = new DateTime(cal.getTime());
        patient.dateOfBirth.precision = FuzzyDateTime.PRECISION_FULL;
        patient.relationshipInd = true;
        patient.ageDisplay = "9 years";

        pList.patientList.add(patient);
        mFragment.onResponse(pList);
        final ListArrayRecyclerAdapter adapter = (ListArrayRecyclerAdapter) list.getAdapter();

        //test one patient being added to the list
        assertNotNull(adapter);
        assertEquals(2, adapter.getItemCount());
        assertEquals(pList.patientListName, adapter.get(0).getTitle());
    }

    @Test
    public void onResponse_listHeaderSecondary() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -3650);
        final RecyclerView list = mFragment.getView().findViewById(R.id.patientlist_listView);

        final PatientList pList = createDefaultPatientListMock();
        pList.patientListName = "Secondary";
        final PatientListPatient patient = new PatientListPatient();
        patient.personId = mDefaultPatientId + System.currentTimeMillis();
        patient.encounterId = mDefaultEncounterId + System.currentTimeMillis();
        patient.nameFullFormatted = "secondarypatient";
        patient.sexAbbr = "M";
        patient.bedLocationDisplay = "bed1";
        patient.roomLocationDisplay = "room1";
        patient.dateOfBirth = new FuzzyDateTime();
        patient.dateOfBirth.dateTime = new DateTime(cal.getTime());
        patient.dateOfBirth.precision = FuzzyDateTime.PRECISION_FULL;
        patient.relationshipInd = true;
        patient.ageDisplay = "9 years";

        pList.patientList.add(patient);
        mFragment.onResponse(pList);

        final ListArrayRecyclerAdapter adapter = (ListArrayRecyclerAdapter) list.getAdapter();

        //test one patient being added to the list
        assertEquals(2, adapter.getItemCount());
        assertEquals(pList.patientListName, adapter.get(0).getTitle());
    }

    @Test
    public void onResponse_noOrganizationSelected() {
        final PatientList pList = new PatientList();
        pList.has_device_organization = 0;

        mFragment = Mockito.spy(mFragment);
        mockActivity = Mockito.spy(mockActivity);

        doReturn(mockActivity).when(mFragment).getActivity();

        ActiveModuleManager.setActiveModule(TestActivity.class, 0, null);
        mFragment.onResponse(pList);
        assertEquals(SettingsActivity.class, ActiveModuleManager.getActiveModule());
    }

    @Test
    public void onResponse_addSinglePatient() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -3650);
        final RecyclerView list = mFragment.getView().findViewById(R.id.patientlist_listView);

        final PatientList pList = createDefaultPatientListMock();
        pList.patientListId = null;
        pList.patientListName = "Primary";
        final PatientListPatient patient = new PatientListPatient();
        patient.personId = mDefaultPatientId + System.currentTimeMillis();
        patient.encounterId = mDefaultEncounterId + System.currentTimeMillis();
        patient.nameFullFormatted = "primarypatient";
        patient.sexAbbr = "M";
        patient.bedLocationDisplay = "bed1";
        patient.roomLocationDisplay = "room1";
        patient.dateOfBirth = new FuzzyDateTime();
        patient.dateOfBirth.dateTime = new DateTime(cal.getTime());
        patient.dateOfBirth.precision = FuzzyDateTime.PRECISION_FULL;
        patient.relationshipInd = true;
        patient.ageDisplay = "9 years";

        pList.patientList.add(patient);
        mFragment.onResponse(pList);

        final ListArrayRecyclerAdapter adapter = (ListArrayRecyclerAdapter) list.getAdapter();

        assertTrue(adapter.get(0) instanceof TextListItem);
        assertTrue(adapter.get(1) instanceof PatientListItem);
    }

    @Test
    public void onResponse_displayedListAlreadySet() {
        UserContext.putContextStorageObject(TestUtils.getStaticVariable(PatientListFragment.class, "PATIENT_LIST_DISPLAYED_KEY"), "AlreadySet");

        final PatientList pList = createDefaultPatientListMock();
        mFragment.onResponse(pList);

        assertEquals("AlreadySet", UserContext.getContextStorageObject(TestUtils.getStaticVariable(PatientListFragment.class, "PATIENT_LIST_DISPLAYED_KEY"), String.class));
    }

    @Test
    public void onResponse_patientListItemClickSuccess() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -3650);

        final PatientList pList = createDefaultPatientListMock();
        pList.patientListName = "Primary";
        final PatientListPatient patient = new PatientListPatient();
        patient.personId = mDefaultPatientId + System.currentTimeMillis();
        patient.encounterId = mDefaultEncounterId + System.currentTimeMillis();
        patient.nameFullFormatted = "primarypatient";

        pList.patientList.add(patient);
        mFragment.onResponse(pList);

        final PatientListFragment.OnPatientListItemSelectedListener listener = Mockito.mock(PatientListFragment.OnPatientListItemSelectedListener.class);
        TestUtils.setVariable(mFragment, "mCallback", listener);

        final ListArrayRecyclerAdapter adapter = ((ListArrayRecyclerAdapter)((RecyclerView) mFragment.getView().findViewById(R.id.patientlist_listView)).getAdapter());
        adapter.getOnItemClickListener().onItemClick(null, adapter.get(1));

        verify(listener).onPatientSelected(Mockito.any(PatientListPatient.class));
    }

    @Test
    public void onResponse_patientListItemClickCallbackNull() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -3650);

        final PatientList pList = createDefaultPatientListMock();
        pList.patientListName = "Primary";
        final PatientListPatient patient = Mockito.mock(PatientListPatient.class);

        pList.patientList.add(patient);
        mFragment.onResponse(pList);

        final PatientListFragment.OnPatientListItemSelectedListener listener = null;
        TestUtils.setVariable(mFragment, "mCallback", listener);

        final PatientListItem patientListPatientListItem = Mockito.mock(PatientListItem.class);

        final RecyclerView listView = Mockito.spy(mFragment.getView().findViewById(R.id.patientlist_listView));
        final ListArrayRecyclerAdapter adapter = (ListArrayRecyclerAdapter) listView.getAdapter();

        adapter.getOnItemClickListener().onItemClick(null, patientListPatientListItem);

        verify(patientListPatientListItem, never()).getData();
    }

    @Test (expected = IllegalArgumentException.class)
    public void onResponse_objectNull() {
        mFragment.onResponse((Object) null);
    }

    @Test
    public void onResponse_objectOfTypePatientLists_NotInStateToShow() {
        final PatientLists patientLists = new PatientLists();
        patientLists.listOfLists = new ArrayList<>();
        final PatientLists.PatientListInfo patientListInfo = new PatientLists.PatientListInfo();
        patientListInfo.patientListId = "ASSIGNED";
        patientListInfo.patientListName = "Assigned";
        final PatientLists.PatientListInfo patientListInfo2 = new PatientLists.PatientListInfo();
        patientListInfo2.patientListId = "Primary";
        patientListInfo2.patientListName = "Primary";
        patientLists.listOfLists.add(0, patientListInfo);
        patientLists.listOfLists.add(1, patientListInfo2);

        mFragment = Mockito.spy(mFragment);

        UserContext.putContextStorageObject(TestUtils.getStaticVariable(PatientListFragment.class, "PATIENT_LIST_DISPLAYED_KEY"), "ASSIGNED");

        TestUtils.setVariable(mFragment, "mShowLists", false);
        TestUtils.setVariable(mFragment, "mCurrentDialog", null);

        mFragment.onResponse(patientLists);

        assertNull(TestUtils.getVariable(mFragment, "mCurrentDialog"));
    }

    @Test
    public void onResponse_objectOfTypePatientLists() {
        final PatientLists patientLists = new PatientLists();
        patientLists.listOfLists = new ArrayList<>();
        final PatientLists.PatientListInfo patientListInfo = new PatientLists.PatientListInfo();
        patientListInfo.patientListId = "ASSIGNED";
        patientListInfo.patientListName = "Assigned";
        final PatientLists.PatientListInfo patientListInfo2 = new PatientLists.PatientListInfo();
        patientListInfo2.patientListId = "Primary";
        patientListInfo2.patientListName = "Primary";
        patientLists.listOfLists.add(0, patientListInfo);
        patientLists.listOfLists.add(1, patientListInfo2);

        mFragment = Mockito.spy(mFragment);

        UserContext.putContextStorageObject(TestUtils.getStaticVariable(PatientListFragment.class, "PATIENT_LIST_DISPLAYED_KEY"), "ASSIGNED");

        TestUtils.setVariable(mFragment, "mShowLists", true);
        TestUtils.setVariable(mFragment, "mCurrentDialog", null);

        mFragment.onResponse(patientLists);

        assertNotNull(TestUtils.getVariable(mFragment, "mCurrentDialog"));
    }

    @Test
    public void onNoContentResponse_patientListGetViewIsNull() {
        mFragment = Mockito.spy(mFragment);

        doReturn(null).when(mFragment).getView();

        mFragment.onNoContentResponse(null, PatientList.class);

        verify(mFragment).getView();
    }

    @Test
    public void onNoContentResponse_patientList() {
        UserContext.removeContextStorageObject(TestUtils.getStaticVariable(PatientListFragment.class, "PATIENT_LIST_DISPLAYED_KEY"));
        mFragment.onNoContentResponse(null, PatientList.class);

        final RecyclerView list = mFragment.getView().findViewById(R.id.patientlist_listView);
        final ListArrayRecyclerAdapter adapter = (ListArrayRecyclerAdapter) list.getAdapter();
        assertNotNull(list);
        assertEquals(1, adapter.getItemCount());
        assertEquals(mockActivity.getString(R.string.patientlist_nodataavailable), adapter.get(0).getTitle());
    }

    @Test
    public void onNoContentResponse_patientListById() {
        UserContext.removeContextStorageObject(TestUtils.getStaticVariable(PatientListFragment.class, "PATIENT_LIST_DISPLAYED_KEY"));
        mFragment.onNoContentResponse(null, PatientListById.class);

        final RecyclerView list = mFragment.getView().findViewById(R.id.patientlist_listView);
        final ListArrayRecyclerAdapter adapter = (ListArrayRecyclerAdapter) list.getAdapter();
        assertNotNull(adapter);
        assertEquals(1, adapter.getItemCount());
        assertEquals(mockActivity.getString(R.string.patientlist_nodataavailable), adapter.get(0).getTitle());
    }

    @Test
    public void onNoContentResponse_patientLists() {
        mFragment.onNoContentResponse(null, PatientLists.class);
    }

    @Test
    public void onNoContentResponse_patientListBlank() {
        UserContext.putContextStorageObject(TestUtils.getStaticVariable(PatientListFragment.class, "PATIENT_LIST_DISPLAYED_KEY"), "");
        mFragment.onNoContentResponse(null, PatientList.class);
    }

    @Test
    public void onNoContentResponse_getActivityNull() {
        mFragment = Mockito.spy(mFragment);
        final View view = Mockito.mock(View.class);
        final RecyclerView listView = Mockito.mock(RecyclerView.class);

        doReturn(view).when(mFragment).getView();
        doReturn(listView).when(view).findViewById(R.id.patientlist_listView);
        doReturn(null).when(mFragment).getActivity();

        mFragment.onNoContentResponse(null, PatientList.class);
        verify(listView, never()).setAdapter(null);
    }

    @Test
    public void onNoContentResponse_null() {
        mFragment.onNoContentResponse(null, null);
    }

    @Test
    public void populateChangeListDialog_getActivityNull() {
        mFragment = Mockito.spy(mFragment);
        doReturn(null).when(mFragment).getActivity();

        final Object o = null;
        TestUtils.invokePrivateMethod(mFragment, "populateChangeListDialog", new Class[]{PatientLists.class}, o);
    }

    @Test
    public void populateChangeListDialog_populateList() {
        mFragment = Mockito.spy(mFragment);
        final PatientLists patientLists = new PatientLists();
        patientLists.listOfLists = new ArrayList<>();
        final PatientLists.PatientListInfo patientListInfo = new PatientLists.PatientListInfo();
        patientListInfo.patientListId = "ASSIGNED";
        patientListInfo.patientListName = "Assigned";
        final PatientLists.PatientListInfo patientListInfo2 = new PatientLists.PatientListInfo();
        patientListInfo2.patientListId = "Primary";
        patientListInfo2.patientListName = "Primary";
        patientLists.listOfLists.add(0, patientListInfo);
        patientLists.listOfLists.add(1, patientListInfo2);

        UserContext.putContextStorageObject(TestUtils.getStaticVariable(PatientListFragment.class, "PATIENT_LIST_DISPLAYED_KEY"), "ASSIGNED");

        TestUtils.invokePrivateMethod(mFragment, "populateChangeListDialog", new Class[]{PatientLists.class}, patientLists);
    }

    @Test
    public void sortPatientList_Location() {
        List<PatientListPatient> patients = new ArrayList<>();

        final PatientListPatient patient = new PatientListPatient();
        patient.personId = mDefaultPatientId + System.currentTimeMillis();
        patient.encounterId = mDefaultEncounterId + System.currentTimeMillis();
        patient.nameFullFormatted = "Alpha";
        patient.bedLocationDisplay = "A";
        patient.roomLocationDisplay = "104";

        patients.add(patient);

        final PatientListPatient patient2 = new PatientListPatient();
        patient2.personId = mDefaultPatientId + System.currentTimeMillis() + 1;
        patient2.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 1;
        patient2.nameFullFormatted = "Bravo";
        patient2.bedLocationDisplay = "C";
        patient2.roomLocationDisplay = "101";

        patients.add(patient2);

        final PatientListPatient patient3 = new PatientListPatient();
        patient3.personId = mDefaultPatientId + System.currentTimeMillis() + 2;
        patient3.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 2;
        patient3.nameFullFormatted = "Charlie";
        patient3.bedLocationDisplay = "A";
        patient3.roomLocationDisplay = "101";

        patients.add(patient3);

        TestUtils.setVariable(mFragment, "mSortBy", PatientListFragment.PatientListSortType.LOCATION);
        final Object oPatientList = TestUtils.invokePrivateStaticMethod(PatientListFragment.class, "sortPatientList", new Class[]{List.class, PatientListFragment.PatientListSortType.class}, patients,
                                                                        PatientListFragment.PatientListSortType.LOCATION);
        patients = (List<PatientListPatient>) oPatientList;

        //check sort by Location
        assertEquals(3, patients.size());
        assertEquals("101", patients.get(0).roomLocationDisplay);
        assertEquals("A", patients.get(0).bedLocationDisplay);
        assertEquals("101", patients.get(1).roomLocationDisplay);
        assertEquals("C", patients.get(1).bedLocationDisplay);
        assertEquals("104", patients.get(2).roomLocationDisplay);
        assertEquals("A", patients.get(2).bedLocationDisplay);
    }

    @Test
    public void sortPatientList_Name() {
        List<PatientListPatient> patients = new ArrayList<>();

        final PatientListPatient patient = new PatientListPatient();
        patient.personId = mDefaultPatientId + System.currentTimeMillis();
        patient.encounterId = mDefaultEncounterId + System.currentTimeMillis();
        patient.nameFullFormatted = "Charlie, Mike";

        patients.add(patient);

        final PatientListPatient patient2 = new PatientListPatient();
        patient2.personId = mDefaultPatientId + System.currentTimeMillis() + 1;
        patient2.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 1;
        patient2.nameFullFormatted = "Alpha, Zulu";

        patients.add(patient2);

        final PatientListPatient patient3 = new PatientListPatient();
        patient3.personId = mDefaultPatientId + System.currentTimeMillis() + 2;
        patient3.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 2;
        patient3.nameFullFormatted = "Alpha, Tango";

        patients.add(patient3);

        final PatientListPatient patient4 = new PatientListPatient();
        patient4.personId = mDefaultPatientId + System.currentTimeMillis() + 3;
        patient4.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 3;
        patient4.nameFullFormatted = "Bravo, Lima";

        patients.add(patient4);

        TestUtils.setVariable(mFragment, "mSortBy", PatientListFragment.PatientListSortType.NAME);
        final Object oPatientList = TestUtils.invokePrivateStaticMethod(PatientListFragment.class, "sortPatientList", new Class[]{List.class, PatientListFragment.PatientListSortType.class}, patients,
                                                                        PatientListFragment.PatientListSortType.NAME);
        patients = (List<PatientListPatient>) oPatientList;

        //check sort by Name
        assertEquals(4, patients.size());
        assertEquals("Alpha, Tango", patients.get(0).nameFullFormatted);

        assertEquals("Alpha, Zulu", patients.get(1).nameFullFormatted);

        assertEquals("Bravo, Lima", patients.get(2).nameFullFormatted);

        assertEquals("Charlie, Mike", patients.get(3).nameFullFormatted);
    }

    @Test
    public void sortPatientList_InvalidSortBy() {
        List<PatientListPatient> patients = new ArrayList<>();

        final PatientListPatient patient = new PatientListPatient();
        patient.personId = mDefaultPatientId + System.currentTimeMillis();
        patient.encounterId = mDefaultEncounterId + System.currentTimeMillis();
        patient.nameFullFormatted = "Charlie, Mike";

        patients.add(patient);

        final PatientListPatient patient2 = new PatientListPatient();
        patient2.personId = mDefaultPatientId + System.currentTimeMillis() + 1;
        patient2.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 1;
        patient2.nameFullFormatted = "Alpha, Zulu";

        patients.add(patient2);

        final PatientListPatient patient3 = new PatientListPatient();
        patient3.personId = mDefaultPatientId + System.currentTimeMillis() + 2;
        patient3.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 2;
        patient3.nameFullFormatted = "Alpha, Tango";

        patients.add(patient3);

        final PatientListPatient patient4 = new PatientListPatient();
        patient4.personId = mDefaultPatientId + System.currentTimeMillis() + 3;
        patient4.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 3;
        patient4.nameFullFormatted = "Bravo, Lima";

        patients.add(patient4);

        TestUtils.setVariable(mFragment, "mSortBy", null);
        final Object oPatientList = TestUtils.invokePrivateStaticMethod(PatientListFragment.class, "sortPatientList",
                                                                        new Class[]{List.class, PatientListFragment.PatientListSortType.class}, patients, null);
        patients = (List<PatientListPatient>) oPatientList;

        //check sort by invalid sort option should leave list in the same order as passed in
        assertEquals(4, patients.size());
        assertEquals("Charlie, Mike", patients.get(0).nameFullFormatted);

        assertEquals("Alpha, Zulu", patients.get(1).nameFullFormatted);

        assertEquals("Alpha, Tango", patients.get(2).nameFullFormatted);

        assertEquals("Bravo, Lima", patients.get(3).nameFullFormatted);
    }

    @Test
    public void sortPatientList_LeftNull() {
        List<PatientListPatient> patients = new ArrayList<>();

        final PatientListPatient patient2 = new PatientListPatient();
        patient2.personId = mDefaultPatientId + System.currentTimeMillis() + 1;
        patient2.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 1;
        patient2.nameFullFormatted = "Alpha, Zulu";

        patients.add(patient2);
        patients.add(null);

        TestUtils.setVariable(mFragment, "mSortBy", null);
        final Object oPatientList = TestUtils.invokePrivateStaticMethod(PatientListFragment.class, "sortPatientList", new Class[]{List.class, PatientListFragment.PatientListSortType.class}, patients,
                                                                        PatientListFragment.PatientListSortType.LOCATION);
        patients = (List<PatientListPatient>) oPatientList;

        assertEquals(2, patients.size());
        assertEquals("Alpha, Zulu", patients.get(0).nameFullFormatted);
        assertNull(patients.get(1));
    }

    @Test
    public void sortPatientList_RightNull() {
        List<PatientListPatient> patients = new ArrayList<>();

        final PatientListPatient patient2 = new PatientListPatient();
        patient2.personId = mDefaultPatientId + System.currentTimeMillis() + 1;
        patient2.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 1;
        patient2.nameFullFormatted = "Alpha, Zulu";

        patients.add(null);
        patients.add(patient2);

        TestUtils.setVariable(mFragment, "mSortBy", null);
        final Object oPatientList = TestUtils.invokePrivateStaticMethod(PatientListFragment.class, "sortPatientList", new Class[]{List.class, PatientListFragment.PatientListSortType.class}, patients,
                                                                        PatientListFragment.PatientListSortType.LOCATION);
        patients = (List<PatientListPatient>) oPatientList;

        assertEquals(2, patients.size());
        assertEquals("Alpha, Zulu", patients.get(0).nameFullFormatted);
        assertNull(patients.get(1));
    }

    @Test
    public void sortPatientList_LocationBothRoomNull() {
        List<PatientListPatient> patients = new ArrayList<>();

        final PatientListPatient patient1 = new PatientListPatient();
        patient1.personId = mDefaultPatientId + System.currentTimeMillis();
        patient1.encounterId = mDefaultEncounterId + System.currentTimeMillis();
        patient1.nameFullFormatted = "Beta, Yellow";

        final PatientListPatient patient2 = new PatientListPatient();
        patient2.personId = mDefaultPatientId + System.currentTimeMillis() + 1;
        patient2.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 1;
        patient2.nameFullFormatted = "Alpha, Zulu";

        patients.add(patient1);
        patients.add(patient2);

        TestUtils.setVariable(mFragment, "mSortBy", null);
        final Object oPatientList = TestUtils.invokePrivateStaticMethod(PatientListFragment.class, "sortPatientList", new Class[]{List.class, PatientListFragment.PatientListSortType.class}, patients,
                                                                        PatientListFragment.PatientListSortType.LOCATION);
        patients = (List<PatientListPatient>) oPatientList;

        assertEquals(2, patients.size());
        assertEquals(patient1, patients.get(1));
        assertEquals(patient2, patients.get(0));
    }

    @Test
    public void sortPatientList_LeftLocationNull() {
        List<PatientListPatient> patients = new ArrayList<>();

        final PatientListPatient patient1 = new PatientListPatient();
        patient1.personId = mDefaultPatientId + System.currentTimeMillis();
        patient1.encounterId = mDefaultEncounterId + System.currentTimeMillis();
        patient1.nameFullFormatted = "Beta, Yellow";

        final PatientListPatient patient2 = new PatientListPatient();
        patient2.personId = mDefaultPatientId + System.currentTimeMillis() + 1;
        patient2.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 1;
        patient2.nameFullFormatted = "Alpha, Zulu";
        patient2.roomLocationDisplay = "Room2";

        patients.add(patient1);
        patients.add(patient2);

        TestUtils.setVariable(mFragment, "mSortBy", null);
        final Object oPatientList = TestUtils.invokePrivateStaticMethod(PatientListFragment.class, "sortPatientList", new Class[]{List.class, PatientListFragment.PatientListSortType.class}, patients,
                                                                        PatientListFragment.PatientListSortType.LOCATION);
        patients = (List<PatientListPatient>) oPatientList;

        assertEquals(2, patients.size());
        assertEquals(patient1.personId, patients.get(0).personId);
        assertEquals(patient2.personId, patients.get(1).personId);
    }

    @Test
    public void sortPatientList_RightLocationNull() {
        List<PatientListPatient> patients = new ArrayList<>();

        final PatientListPatient patient1 = new PatientListPatient();
        patient1.personId = mDefaultPatientId + System.currentTimeMillis();
        patient1.encounterId = mDefaultEncounterId + System.currentTimeMillis();
        patient1.nameFullFormatted = "Beta, Yellow";

        final PatientListPatient patient2 = new PatientListPatient();
        patient2.personId = mDefaultPatientId + System.currentTimeMillis() + 1;
        patient2.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 1;
        patient2.nameFullFormatted = "Alpha, Zulu";
        patient2.roomLocationDisplay = "Room2";

        patients.add(patient2);
        patients.add(patient1);

        TestUtils.setVariable(mFragment, "mSortBy", null);
        final Object oPatientList = TestUtils.invokePrivateStaticMethod(PatientListFragment.class, "sortPatientList", new Class[]{List.class, PatientListFragment.PatientListSortType.class}, patients,
                                                                        PatientListFragment.PatientListSortType.LOCATION);
        patients = (List<PatientListPatient>) oPatientList;

        assertEquals(2, patients.size());
        assertEquals(patient1.personId, patients.get(0).personId);
        assertEquals(patient2.personId, patients.get(1).personId);
    }

    @Test
    public void sortPatientList_RightAndLeftLocationNull() {
        List<PatientListPatient> patients = new ArrayList<>();

        final PatientListPatient patient1 = new PatientListPatient();
        patient1.personId = mDefaultPatientId + System.currentTimeMillis();
        patient1.encounterId = mDefaultEncounterId + System.currentTimeMillis();
        patient1.nameFullFormatted = "Beta, Yellow";

        final PatientListPatient patient2 = new PatientListPatient();
        patient2.personId = mDefaultPatientId + System.currentTimeMillis() + 1;
        patient2.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 1;
        patient2.nameFullFormatted = "Alpha, Zulu";

        patients.add(patient1);
        patients.add(patient2);

        TestUtils.setVariable(mFragment, "mSortBy", null);
        final Object oPatientList = TestUtils.invokePrivateStaticMethod(PatientListFragment.class, "sortPatientList", new Class[]{List.class, PatientListFragment.PatientListSortType.class}, patients,
                                                                        PatientListFragment.PatientListSortType.LOCATION);
        patients = (List<PatientListPatient>) oPatientList;

        assertEquals(2, patients.size());
        assertEquals(patient2.personId, patients.get(0).personId);
        assertEquals(patient1.personId, patients.get(1).personId);
    }

    @Test
    public void sortPatientList_LocationLeftRoomNull() {
        List<PatientListPatient> patients = new ArrayList<>();

        final PatientListPatient patient1 = new PatientListPatient();
        patient1.personId = mDefaultPatientId + System.currentTimeMillis();
        patient1.encounterId = mDefaultEncounterId + System.currentTimeMillis();
        patient1.nameFullFormatted = "Beta, Yellow";

        final PatientListPatient patient2 = new PatientListPatient();
        patient2.personId = mDefaultPatientId + System.currentTimeMillis() + 1;
        patient2.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 1;
        patient2.nameFullFormatted = "Alpha, Zulu";
        patient2.roomLocationDisplay = "Room2";

        patients.add(patient1);
        patients.add(patient2);

        TestUtils.setVariable(mFragment, "mSortBy", null);
        final Object oPatientList = TestUtils.invokePrivateStaticMethod(PatientListFragment.class, "sortPatientList", new Class[]{List.class, PatientListFragment.PatientListSortType.class}, patients,
                                                                        PatientListFragment.PatientListSortType.LOCATION);
        patients = (List<PatientListPatient>) oPatientList;

        assertEquals(2, patients.size());
        assertEquals(patient1.personId, patients.get(0).personId);
        assertEquals(patient2.personId, patients.get(1).personId);
    }

    @Test
    public void sortPatientList_LocationRightRoomNull() {
        List<PatientListPatient> patients = new ArrayList<>();

        final PatientListPatient patient1 = new PatientListPatient();
        patient1.personId = mDefaultPatientId + System.currentTimeMillis();
        patient1.encounterId = mDefaultEncounterId + System.currentTimeMillis();
        patient1.nameFullFormatted = "Beta, Yellow";
        patient1.roomLocationDisplay = "Room1";

        final PatientListPatient patient2 = new PatientListPatient();
        patient2.personId = mDefaultPatientId + System.currentTimeMillis() + 1;
        patient2.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 1;
        patient2.nameFullFormatted = "Alpha, Zulu";

        patients.add(patient1);
        patients.add(patient2);

        TestUtils.setVariable(mFragment, "mSortBy", null);
        final Object oPatientList = TestUtils.invokePrivateStaticMethod(PatientListFragment.class, "sortPatientList", new Class[]{List.class, PatientListFragment.PatientListSortType.class}, patients,
                                                                        PatientListFragment.PatientListSortType.LOCATION);
        patients = (List<PatientListPatient>) oPatientList;

        assertEquals(2, patients.size());
        assertEquals(patient2.personId, patients.get(0).personId);
        assertEquals(patient1.personId, patients.get(1).personId);
    }

    @Test
    public void sortPatientList_LocationBothBedNull() {
        List<PatientListPatient> patients = new ArrayList<>();

        final PatientListPatient patient1 = new PatientListPatient();
        patient1.personId = mDefaultPatientId + System.currentTimeMillis();
        patient1.encounterId = mDefaultEncounterId + System.currentTimeMillis();
        patient1.nameFullFormatted = "Beta, Yellow";
        patient1.roomLocationDisplay = "Room";

        final PatientListPatient patient2 = new PatientListPatient();
        patient2.personId = mDefaultPatientId + System.currentTimeMillis() + 1;
        patient2.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 1;
        patient2.nameFullFormatted = "Alpha, Zulu";
        patient2.roomLocationDisplay = "Room";

        patients.add(patient1);
        patients.add(patient2);

        TestUtils.setVariable(mFragment, "mSortBy", null);
        final Object oPatientList = TestUtils.invokePrivateStaticMethod(PatientListFragment.class, "sortPatientList", new Class[]{List.class, PatientListFragment.PatientListSortType.class}, patients,
                                                                        PatientListFragment.PatientListSortType.LOCATION);
        patients = (List<PatientListPatient>) oPatientList;

        assertEquals(2, patients.size());
        assertEquals(patient1.personId, patients.get(1).personId);
        assertEquals(patient2.personId, patients.get(0).personId);
    }

    @Test
    public void sortPatientList_LocationRightBedNull() {
        List<PatientListPatient> patients = new ArrayList<>();

        final PatientListPatient patient1 = new PatientListPatient();
        patient1.personId = mDefaultPatientId + System.currentTimeMillis();
        patient1.encounterId = mDefaultEncounterId + System.currentTimeMillis();
        patient1.nameFullFormatted = "Beta, Yellow";
        patient1.roomLocationDisplay = "Room1";
        patient1.bedLocationDisplay = "Bed1";

        final PatientListPatient patient2 = new PatientListPatient();
        patient2.personId = mDefaultPatientId + System.currentTimeMillis() + 1;
        patient2.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 1;
        patient2.nameFullFormatted = "Alpha, Zulu";
        patient2.roomLocationDisplay = "Room2";

        patients.add(patient1);
        patients.add(patient2);

        TestUtils.setVariable(mFragment, "mSortBy", null);
        final Object oPatientList = TestUtils.invokePrivateStaticMethod(PatientListFragment.class, "sortPatientList", new Class[]{List.class, PatientListFragment.PatientListSortType.class}, patients,
                                                                        PatientListFragment.PatientListSortType.LOCATION);
        patients = (List<PatientListPatient>) oPatientList;

        assertEquals(2, patients.size());
        assertEquals(patient1.personId, patients.get(0).personId);
        assertEquals(patient2.personId, patients.get(1).personId);
    }

    @Test
    public void sortPatientList_LocationLeftBedNull() {
        List<PatientListPatient> patients = new ArrayList<>();

        final PatientListPatient patient1 = new PatientListPatient();
        patient1.personId = mDefaultPatientId + System.currentTimeMillis();
        patient1.encounterId = mDefaultEncounterId + System.currentTimeMillis();
        patient1.nameFullFormatted = "Beta, Yellow";
        patient1.roomLocationDisplay = "Room1";

        final PatientListPatient patient2 = new PatientListPatient();
        patient2.personId = mDefaultPatientId + System.currentTimeMillis() + 1;
        patient2.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 1;
        patient2.nameFullFormatted = "Alpha, Zulu";
        patient2.roomLocationDisplay = "Room2";
        patient2.bedLocationDisplay = "Bed2";

        patients.add(patient1);
        patients.add(patient2);

        TestUtils.setVariable(mFragment, "mSortBy", null);
        final Object oPatientList = TestUtils.invokePrivateStaticMethod(PatientListFragment.class, "sortPatientList", new Class[]{List.class, PatientListFragment.PatientListSortType.class}, patients,
                                                                        PatientListFragment.PatientListSortType.LOCATION);
        patients = (List<PatientListPatient>) oPatientList;

        assertEquals(2, patients.size());
        assertEquals(patient1.personId, patients.get(0).personId);
        assertEquals(patient2.personId, patients.get(1).personId);
    }

    @Test
    public void sortPatientList_BothPatientsNull() {
        List<PatientListPatient> patients = new ArrayList<>();

        patients.add(null);
        patients.add(null);

        TestUtils.setVariable(mFragment, "mSortBy", null);
        final Object oPatientList = TestUtils.invokePrivateStaticMethod(PatientListFragment.class, "sortPatientList", new Class[]{List.class, PatientListFragment.PatientListSortType.class}, patients,
                                                                        PatientListFragment.PatientListSortType.LOCATION);
        patients = (List<PatientListPatient>) oPatientList;

        assertEquals(2, patients.size());
        assertNull(patients.get(0));
        assertNull(patients.get(1));
    }

    @Test
    public void sortPatientList_patientLeftAndRightNull() {
        List<PatientListPatient> patients = new ArrayList<>();

        patients.add(null);
        patients.add(null);

        TestUtils.setVariable(mFragment, "mSortBy", null);
        final Object oPatientList = TestUtils.invokePrivateStaticMethod(PatientListFragment.class, "sortPatientList", new Class[]{List.class, PatientListFragment.PatientListSortType.class}, patients,
                                                                        PatientListFragment.PatientListSortType.NAME);
        patients = (List<PatientListPatient>) oPatientList;

        assertEquals(2, patients.size());
        assertNull(patients.get(0));
        assertNull(patients.get(1));
    }

    @Test
    public void sortPatientList_patientLeftNull() {
        List<PatientListPatient> patients = new ArrayList<>();

        final PatientListPatient patient = new PatientListPatient();
        patient.personId = mDefaultPatientId + System.currentTimeMillis() + 1;
        patient.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 1;
        patient.nameFullFormatted = "Alpha, Zulu";

        patients.add(patient);
        patients.add(null);

        TestUtils.setVariable(mFragment, "mSortBy", null);
        final Object oPatientList = TestUtils.invokePrivateStaticMethod(PatientListFragment.class, "sortPatientList", new Class[]{List.class, PatientListFragment.PatientListSortType.class}, patients,
                                                                        PatientListFragment.PatientListSortType.NAME);
        patients = (List<PatientListPatient>) oPatientList;

        assertEquals(2, patients.size());
        assertEquals(patient.personId, patients.get(0).personId);
        assertNull(patients.get(1));
    }

    @Test
    public void sortPatientList_patientRightNull() {
        List<PatientListPatient> patients = new ArrayList<>();

        final PatientListPatient patient = new PatientListPatient();
        patient.personId = mDefaultPatientId + System.currentTimeMillis() + 1;
        patient.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 1;
        patient.nameFullFormatted = "Alpha, Zulu";

        patients.add(null);
        patients.add(patient);

        TestUtils.setVariable(mFragment, "mSortBy", null);
        final Object oPatientList = TestUtils.invokePrivateStaticMethod(PatientListFragment.class, "sortPatientList", new Class[]{List.class, PatientListFragment.PatientListSortType.class}, patients,
                                                                        PatientListFragment.PatientListSortType.NAME);
        patients = (List<PatientListPatient>) oPatientList;

        assertEquals(2, patients.size());
        assertEquals("Alpha, Zulu", patients.get(0).nameFullFormatted);
        assertNull(patients.get(1));
    }

    @Test
    public void sortPatientList_patientRightNameEmpty() {
        List<PatientListPatient> patients = new ArrayList<>();

        final PatientListPatient patient1 = new PatientListPatient();
        patient1.personId = mDefaultPatientId + System.currentTimeMillis();
        patient1.encounterId = mDefaultEncounterId + System.currentTimeMillis();
        patient1.nameFullFormatted = "Test";

        final PatientListPatient patient2 = new PatientListPatient();
        patient2.personId = mDefaultPatientId + System.currentTimeMillis() + 1;
        patient2.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 1;
        patient2.nameFullFormatted = " ";

        patients.add(patient2);
        patients.add(patient1);

        TestUtils.setVariable(mFragment, "mSortBy", null);
        final Object oPatientList = TestUtils.invokePrivateStaticMethod(PatientListFragment.class, "sortPatientList", new Class[]{List.class, PatientListFragment.PatientListSortType.class}, patients,
                                                                        PatientListFragment.PatientListSortType.NAME);
        patients = (List<PatientListPatient>) oPatientList;

        assertEquals(2, patients.size());
        assertEquals(patient2.personId, patients.get(0).personId);
        assertEquals(patient1.personId, patients.get(1).personId);
    }

    @Test
    public void sortPatientList_patientLeftNameEmpty() {
        List<PatientListPatient> patients = new ArrayList<>();

        final PatientListPatient patient1 = new PatientListPatient();
        patient1.personId = mDefaultPatientId + System.currentTimeMillis();
        patient1.encounterId = mDefaultEncounterId + System.currentTimeMillis();
        patient1.nameFullFormatted = "Test";

        final PatientListPatient patient2 = new PatientListPatient();
        patient2.personId = mDefaultPatientId + System.currentTimeMillis() + 1;
        patient2.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 1;
        patient2.nameFullFormatted = " ";

        patients.add(patient1);
        patients.add(patient2);

        TestUtils.setVariable(mFragment, "mSortBy", null);
        final Object oPatientList = TestUtils.invokePrivateStaticMethod(PatientListFragment.class, "sortPatientList", new Class[]{List.class, PatientListFragment.PatientListSortType.class}, patients,
                                                                        PatientListFragment.PatientListSortType.NAME);
        patients = (List<PatientListPatient>) oPatientList;

        assertEquals(2, patients.size());
        assertEquals(patient2.personId, patients.get(0).personId);
        assertEquals(patient1.personId, patients.get(1).personId);
    }

    @Test
    public void sortPatientList_patientRightNameNull() {
        List<PatientListPatient> patients = new ArrayList<>();

        final PatientListPatient patient1 = new PatientListPatient();
        patient1.personId = mDefaultPatientId + System.currentTimeMillis();
        patient1.encounterId = mDefaultEncounterId + System.currentTimeMillis();
        patient1.nameFullFormatted = "Test";

        final PatientListPatient patient2 = new PatientListPatient();
        patient2.personId = mDefaultPatientId + System.currentTimeMillis() + 1;
        patient2.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 1;
        patient2.nameFullFormatted = null;

        patients.add(patient2);
        patients.add(patient1);

        TestUtils.setVariable(mFragment, "mSortBy", null);
        final Object oPatientList = TestUtils.invokePrivateStaticMethod(PatientListFragment.class, "sortPatientList", new Class[]{List.class, PatientListFragment.PatientListSortType.class}, patients,
                                                                        PatientListFragment.PatientListSortType.NAME);
        patients = (List<PatientListPatient>) oPatientList;

        assertEquals(2, patients.size());
        assertEquals(patient2.personId, patients.get(0).personId);
        assertEquals(patient1.personId, patients.get(1).personId);
    }

    @Test
    public void sortPatientList_patientLeftNameNull() {
        List<PatientListPatient> patients = new ArrayList<>();

        final PatientListPatient patient1 = new PatientListPatient();
        patient1.personId = mDefaultPatientId + System.currentTimeMillis();
        patient1.encounterId = mDefaultEncounterId + System.currentTimeMillis();
        patient1.nameFullFormatted = "Test";

        final PatientListPatient patient2 = new PatientListPatient();
        patient2.personId = mDefaultPatientId + System.currentTimeMillis() + 1;
        patient2.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 1;
        patient2.nameFullFormatted = null;

        patients.add(patient1);
        patients.add(patient2);

        TestUtils.setVariable(mFragment, "mSortBy", null);
        final Object oPatientList = TestUtils.invokePrivateStaticMethod(PatientListFragment.class, "sortPatientList", new Class[]{List.class, PatientListFragment.PatientListSortType.class}, patients,
                                                                        PatientListFragment.PatientListSortType.NAME);
        patients = (List<PatientListPatient>) oPatientList;

        assertEquals(2, patients.size());
        assertEquals(patient2.personId, patients.get(0).personId);
        assertEquals(patient1.personId, patients.get(1).personId);
    }

    @Test
    public void sortPatientList_patientLeftandRightNameNull() {
        List<PatientListPatient> patients = new ArrayList<>();

        final PatientListPatient patient1 = new PatientListPatient();
        patient1.personId = mDefaultPatientId + System.currentTimeMillis();
        patient1.encounterId = mDefaultEncounterId + System.currentTimeMillis();
        patient1.nameFullFormatted = null;

        final PatientListPatient patient2 = new PatientListPatient();
        patient2.personId = mDefaultPatientId + System.currentTimeMillis() + 1;
        patient2.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 1;
        patient2.nameFullFormatted = null;

        patients.add(patient1);
        patients.add(patient2);

        TestUtils.setVariable(mFragment, "mSortBy", null);
        final Object oPatientList = TestUtils.invokePrivateStaticMethod(PatientListFragment.class, "sortPatientList", new Class[]{List.class, PatientListFragment.PatientListSortType.class}, patients,
                                                                        PatientListFragment.PatientListSortType.NAME);
        patients = (List<PatientListPatient>) oPatientList;

        assertEquals(2, patients.size());
        assertEquals(patient1.personId, patients.get(0).personId);
        assertEquals(patient2.personId, patients.get(1).personId);
    }

    @Test
    public void sortPatientList_patientLeftandRightNameEmpty() {
        List<PatientListPatient> patients = new ArrayList<>();

        final PatientListPatient patient1 = new PatientListPatient();
        patient1.personId = mDefaultPatientId + System.currentTimeMillis();
        patient1.encounterId = mDefaultEncounterId + System.currentTimeMillis();
        patient1.nameFullFormatted = " ";

        final PatientListPatient patient2 = new PatientListPatient();
        patient2.personId = mDefaultPatientId + System.currentTimeMillis() + 1;
        patient2.encounterId = mDefaultEncounterId + System.currentTimeMillis() + 1;
        patient2.nameFullFormatted = " ";

        patients.add(patient1);
        patients.add(patient2);

        TestUtils.setVariable(mFragment, "mSortBy", null);
        final Object oPatientList = TestUtils.invokePrivateStaticMethod(PatientListFragment.class, "sortPatientList", new Class[]{List.class, PatientListFragment.PatientListSortType.class}, patients,
                                                                        PatientListFragment.PatientListSortType.NAME);
        patients = (List<PatientListPatient>) oPatientList;

        assertEquals(2, patients.size());
        assertEquals(patient1.personId, patients.get(0).personId);
        assertEquals(patient2.personId, patients.get(1).personId);
    }

    @Test
    public void onActivityResult_ResultOk() {
        mFragment = Mockito.spy(mFragment);
        Mockito.doNothing().when(mFragment).getData(IDataRetriever.DataArgs.FORCE_REFRESH);
        mFragment.onActivityResult(PatientListFragment.REQUESTCODE_SETTINGS_ORG_SELECT, AppCompatActivity.RESULT_OK, new Intent());
    }

    @Test
    public void onActivityResult_ResultCancel() {
        mFragment.onActivityResult(PatientListFragment.REQUESTCODE_SETTINGS_ORG_SELECT, AppCompatActivity.RESULT_CANCELED, new Intent());
    }

    @Test
    public void onActivityResult_super() {
        mFragment.onActivityResult(PatientListFragment.REQUESTCODE_SETTINGS_ORG_SELECT + 1, AppCompatActivity.RESULT_OK, new Intent());
    }

    @Test
    public void onNoContentResponse() {
        mFragment = Mockito.spy(mFragment);
        mFragment.onNoContentResponse(null, null);
        verify(mFragment).setFragmentVisibility(true);
    }

    @Test
    public void onErrorResponse() {
        mFragment = Mockito.spy(mFragment);
        mFragment.onErrorResponse(new VolleyError("TestExceptionMessage"), PatientList.class);
        verify(mFragment).onNoContentResponse(null, PatientList.class);

        mFragment.onErrorResponse(new VolleyError("TestExceptionMessage"), PatientLists.class);
        verify(mFragment).onNoContentResponse(null, PatientLists.class);
    }

    @Test
    public void onFailedResponse() {
        mFragment = Mockito.spy(mFragment);
        mFragment.onFailedResponse(Object.class, true);
    }

    @Test
    public void onFailedResponse_class_patientLists() {
        mFragment = Mockito.spy(mFragment);
        TestUtils.setVariable(mFragment, "mCurrentDialog", null);
        final ListView listView = Mockito.mock(ListView.class);
        mFragment.onFailedResponse(PatientLists.class, true);
        assertNull(TestUtils.getVariable(mFragment, "mCurrentDialog"));
        verify(listView, never()).setOnItemClickListener(Mockito.any(AdapterView.OnItemClickListener.class));
    }

    @Test
    public void onFailedResponse_class_patientLists_cacheReturned_false() {
        mFragment = Mockito.spy(mFragment);
        TestUtils.setVariable(mFragment, "mCurrentDialog", null);
        mFragment.onFailedResponse(PatientLists.class, false);
        assertNull(TestUtils.getVariable(mFragment, "mCurrentDialog"));
    }

    @Test
    public void setupActionBar_getSupportActionBarNull() {
        mockActivity = Mockito.spy(mockActivity);
        mFragment = Mockito.spy(mFragment);

        doReturn(mockActivity).when(mFragment).getActivity();
        doReturn(null).when(mockActivity).getSupportActionBar();

        TestUtils.invokePrivateMethod(mFragment, "setupActionBar");

        verify(mockActivity).getSupportActionBar();
    }

    @Test
    public void setupActionBar() {
        mFragment = Mockito.spy(mFragment);
        doReturn(null).when(mFragment).getActivity();
        mFragment.setupActionBar();

        final AppCompatActivity activity = Mockito.mock(AppCompatActivity.class);
        doReturn(activity).when(mFragment).getActivity();
        mFragment.setupActionBar();
        verify(activity).getSupportActionBar();

        final ActionBar actionBar = Mockito.mock(ActionBar.class);
        doReturn(null).when(activity).getSupportActionBar();
        mFragment.setupActionBar();

        doReturn(actionBar).when(activity).getSupportActionBar();
        mFragment.setupActionBar();
        verify(actionBar).setTitle(R.string.patientlist_title);
        verify(actionBar).setDisplayShowHomeEnabled(true);
        verify(actionBar).setHomeButtonEnabled(true);
    }

    @Test
    public void setCanBeVisible() {
        TestUtils.setVariable(mFragment, "mCanBeVisible", true);
        mFragment.setCanBeVisible(false);
        assertFalse(TestUtils.getVariable(mFragment, "mCanBeVisible"));
    }

    /**
     * Create empty patient list and no patient
     */
    private static PatientList createDefaultPatientListMock() {
        // create patient list object
        final PatientList patientList = new PatientList();
        patientList.has_device_organization = 1;
        patientList.patientListId = "default";

        patientList.patientListName = "Primary";

        // create patients
        patientList.patientList = new ArrayList<>();

        return patientList;
    }

    private class NoDrawerListenerClass extends IonAuthnActivity implements PatientListFragment.OnPatientListItemSelectedListener {
        public NoDrawerListenerClass() {
        }

        @Override
        public void onPatientSelected(final PatientListPatient patient) {
        }
    }
}