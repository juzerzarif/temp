package com.cerner.nursing.nursing.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.VolleyError;
import com.cerner.cura.base.ICuraFragment;
import com.cerner.cura.datamodel.PersonnelInfoSummary;
import com.cerner.cura.requestor.IDataRetriever;
import com.cerner.cura.requestor.MockDataManager;
import com.cerner.cura.test.helper.TestUtils;
import com.cerner.cura.ui.elements.IListItem;
import com.cerner.cura.ui.elements.ListArrayRecyclerAdapter;
import com.cerner.cura.ui.elements.SettingsPageHeaderListItem;
import com.cerner.cura.ui.elements.TextListItem;
import com.cerner.cura.ui.elements.ValueListItem;
import com.cerner.ion.provisioning.ProvisionedTenant;
import com.cerner.ion.request.CernResponse;
import com.cerner.ion.security.IonAuthnActivity;
import com.cerner.ion.security.IonAuthnApplication;
import com.cerner.ion.security.authentication.PinManager;
import com.cerner.ion.session.AuthnResponse;
import com.cerner.ion.session.Capabilities;
import com.cerner.ion.session.IonAuthnSessionUtils;
import com.cerner.ion.session.SessionCheckHandler;
import com.cerner.ion.session.User;
import com.cerner.nursing.nursing.R;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowApplication;

import java.util.Collections;
import java.util.Map;

import androidx.test.core.app.ApplicationProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests {@link com.cerner.nursing.nursing.ui.UserSummaryFragment}.
 *
 * @author Brad Barnhill (BB024928)
 */
@RunWith (RobolectricTestRunner.class)
public class UserSummaryFragmentTests {
    private ActivityController<SettingsActivity> mActivityController;
    private SettingsActivity mockActivity;
    private UserSummaryFragment mFragment;

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
        mActivityController = Robolectric.buildActivity(SettingsActivity.class).create().start();
        mockActivity = spy(mActivityController.get());
        mFragment = (UserSummaryFragment) mockActivity.getFragmentManager().findFragmentById(R.id.fragment_container);
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
        assertEquals(mFragment.getView().findViewById(R.id.refresh_layout_user_summary), TestUtils.getVariable(mFragment, "mRefreshLayout"));
    }

    @Test
    public void onCreateView_activityFinishing() {
        mFragment = Mockito.spy(mFragment);
        final LayoutInflater layoutInflater = Mockito.mock(LayoutInflater.class);
        final RecyclerView view = Mockito.mock(RecyclerView.class);
        doReturn(view).when(layoutInflater).inflate(R.layout.recycler_view_basic, null, false);

        mFragment.getActivity().finish();
        mFragment.onCreateView(layoutInflater, null, null);

        verify(mFragment, never()).setRefreshLayout(view.findViewById(Mockito.anyInt()));
    }

    @Test (expected = NullPointerException.class)
    public void onCreateView_invalidInflate() {
        final LayoutInflater inflater = Mockito.mock(LayoutInflater.class);
        doReturn(Mockito.mock(View.class)).when(inflater).inflate(Mockito.any(Integer.class), Mockito.any(ViewGroup.class), Mockito.eq(false));
        assertNotNull(mFragment.onCreateView(inflater, Mockito.mock(ViewGroup.class), Mockito.mock(Bundle.class)));
    }

    @Test
    public void onAuthnStart_getView() {
        mFragment = spy(mFragment);
        doReturn(Mockito.mock(ViewGroup.class)).when(mFragment).getView();
        Mockito.doNothing().when(mFragment).getData(IDataRetriever.DataArgs.REFRESH);
        Mockito.doNothing().when(mFragment).setFragmentVisibility(false);
        mFragment.onAuthnStart();
        verify(mFragment).getData(IDataRetriever.DataArgs.REFRESH);
        verify(mFragment).setFragmentVisibility(false);
    }

    @Test
    public void onDetach() {
        final ICuraFragment.OnFragmentSelected o = new ICuraFragment.OnFragmentSelected() {
            @Override
            public void onFragmentSelected(final Class<? extends Fragment> fragmentClass, final Bundle oArg, final boolean incognitoFragment, final boolean forceNewFragment) {
            }

            @Override
            public void onFragmentSelected(final Class<? extends Fragment> fragmentClass, final Bundle bundle) {
                onFragmentSelected(fragmentClass, bundle, false, false);
            }
        };

        TestUtils.setVariable(mFragment, "mCallback", o);
        mFragment.onDetach();
        assertNull(TestUtils.getVariable(mFragment, "mCallback"));
    }

    @Test
    public void onAttach_validActivity() {
        assertNotNull(TestUtils.getVariable(mFragment, "mCallback"));
    }

    @Test (expected = ClassCastException.class)
    public void onAttach_invalidActivity() {
        TestUtils.setVariable(mFragment, "mCallback", null);
        mFragment.onAttach(Mockito.mock(IonAuthnActivity.class));
    }

    @Test
    public void handleClicks_createPin() {
        mFragment = spy(mFragment);
        doReturn(mockActivity).when(mFragment).getActivity();
        final PinManager pinManager = spy(PinManager.Factory.get());
        TestUtils.setVariable(IonAuthnApplication.getInstance().getAuthenticationManager(), "mPinManager", pinManager);
        TestUtils.invokePrivateMethod(mFragment, "handleClicks", new Class[]{String.class}, mFragment.getString(R.string.settings_createpin));
        verify(pinManager).startCreatePin(Mockito.any(IonAuthnActivity.class));

        IonAuthnApplication.getInstance().getAuthenticationManager().logout(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void handleClicks_changePin() {
        mFragment = spy(mFragment);
        mockActivity = spy(mockActivity);
        doReturn(mockActivity).when(mFragment).getActivity();
        final PinManager pinManager = spy(PinManager.Factory.get());
        TestUtils.setVariable(IonAuthnApplication.getInstance().getAuthenticationManager(), "mPinManager", pinManager);
        TestUtils.invokePrivateMethod(mFragment, "handleClicks", new Class[]{String.class}, mFragment.getString(R.string.settings_changepin));
        verify(pinManager).startChangePin(Mockito.any(IonAuthnActivity.class));

        IonAuthnApplication.getInstance().getAuthenticationManager().logout(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void handleClicks_removePin() {
        mFragment = spy(mFragment);
        doReturn(mockActivity).when(mFragment).getActivity();
        final PinManager pinManager = spy(PinManager.Factory.get());
        TestUtils.setVariable(IonAuthnApplication.getInstance().getAuthenticationManager(), "mPinManager", pinManager);
        TestUtils.invokePrivateMethod(mFragment, "handleClicks", new Class[]{String.class}, mFragment.getString(R.string.settings_removepin));
        verify(pinManager).startDeletePin(Mockito.any(IonAuthnActivity.class));

        IonAuthnApplication.getInstance().getAuthenticationManager().logout(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void handleClicks_organization() {
        TestUtils.invokePrivateMethod(mFragment, "handleClicks", new Class[]{String.class}, mFragment.getString(R.string.settings_organization));
    }

    @Test
    public void handleClicks_organization_oneOrg() {
        TestUtils.setVariable(mFragment, "mHasMultipleOrgs", true);
        TestUtils.invokePrivateMethod(mFragment, "handleClicks", new Class[]{String.class}, mFragment.getString(R.string.settings_organization));
    }

    @Test
    public void handleClicks_organization_callbackNull() {
        TestUtils.setVariable(mFragment, "mCallback", null);
        TestUtils.invokePrivateMethod(mFragment, "handleClicks", new Class[]{String.class}, mFragment.getString(R.string.settings_organization));
    }

    @Test
    public void handleClicks_aboutApp() {
        TestUtils.invokePrivateMethod(mFragment, "handleClicks", new Class[]{String.class}, mFragment.getString(R.string.about));
    }

    @Test (expected = IllegalArgumentException.class)
    public void handleClicks_unknownString() throws Throwable {
        try {
            TestUtils.invokePrivateMethod(mFragment, "handleClicks", new Class[]{String.class}, "unknownstring");
        } catch (final RuntimeException ex) {
            throw ex.getCause().getCause();
        }
    }

    @Test
    public void handleClicks_getActivityNull() {
        mFragment = spy(mFragment);
        doReturn(null).when(mFragment).getActivity();
        TestUtils.invokePrivateMethod(mFragment, "handleClicks", new Class[]{String.class}, "");
        verify(mFragment).getActivity();
    }

    @Test (expected = IllegalArgumentException.class)
    public void onResponse_modelNull() {
        final CernResponse response = new CernResponse();
        mFragment.onResponse(response);
        verify(mFragment).getView();
    }

    @Test (expected = IllegalArgumentException.class)
    public void onResponse_modelInvalidType() {
        mFragment.onResponse(new Object());
        verify(mFragment).getView();
    }

    @Test
    public void onResponse_modelValid_hasPin() {
        final PersonnelInfoSummary personnelInfoSummary = buildPersonnelInfoSummary();

        IonAuthnSessionUtils.getUser().setHasPin(true);

        mFragment = spy(mFragment);
        mFragment.onResponse(personnelInfoSummary);
        verify(mFragment).setFragmentVisibility(true);
        verify(mFragment).notifyResponseReceivedListeners(UserSummaryFragment.class);

        final RecyclerView listView = mFragment.getView().findViewById(R.id.settingsList);
        final ListArrayRecyclerAdapter adapter = (ListArrayRecyclerAdapter) listView.getAdapter();
        assertNotNull(listView);
        assertEquals(7, adapter.getItemCount());
        assertTrue(adapter.get(0) instanceof SettingsPageHeaderListItem);
        assertTrue(adapter.get(1) instanceof ValueListItem);
        assertTrue(adapter.get(2) instanceof TextListItem);
        assertTrue(adapter.get(3) instanceof TextListItem);
        assertTrue(adapter.get(4) instanceof TextListItem);
        assertTrue(adapter.get(5) instanceof TextListItem);
        assertTrue(adapter.get(6) instanceof TextListItem);

        assertEquals(mFragment.getString(R.string.settings_organization), adapter.get(1).getTitle());
        assertNull(adapter.get(2).getTitle());
        assertEquals(mFragment.getString(R.string.about), adapter.get(3).getTitle());
        assertNull(adapter.get(4).getTitle());
        assertEquals(mFragment.getString(R.string.settings_changepin), adapter.get(5).getTitle());
        assertEquals(mFragment.getString(R.string.settings_removepin), adapter.get(6).getTitle());

        IonAuthnApplication.getInstance().getAuthenticationManager().logout(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void onResponse_modelValid_hasNoPin() {
        final PersonnelInfoSummary personnelInfoSummary = buildPersonnelInfoSummary();

        IonAuthnSessionUtils.getUser().setHasPin(false);

        mFragment = spy(mFragment);
        mFragment.onResponse(personnelInfoSummary);
        verify(mFragment).setFragmentVisibility(true);
        verify(mFragment).notifyResponseReceivedListeners(UserSummaryFragment.class);

        final RecyclerView listView = mFragment.getView().findViewById(R.id.settingsList);
        final ListArrayRecyclerAdapter adapter = (ListArrayRecyclerAdapter) listView.getAdapter();
        assertNotNull(listView);
        assertEquals(6, adapter.getItemCount());
        assertTrue(adapter.get(0) instanceof SettingsPageHeaderListItem);
        assertTrue(adapter.get(1) instanceof ValueListItem);
        assertTrue(adapter.get(2) instanceof TextListItem);
        assertTrue(adapter.get(3) instanceof TextListItem);
        assertTrue(adapter.get(4) instanceof TextListItem);
        assertTrue(adapter.get(5) instanceof TextListItem);

        assertEquals(mFragment.getString(R.string.settings_organization), adapter.get(1).getTitle());
        assertNull(adapter.get(2).getTitle());
        assertEquals(mFragment.getString(R.string.about), adapter.get(3).getTitle());
        assertNull(adapter.get(4).getTitle());
        assertEquals(mFragment.getString(R.string.settings_createpin), adapter.get(5).getTitle());

        IonAuthnApplication.getInstance().getAuthenticationManager().logout(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void onResponse_modelValid_oneOrg() {
        final PersonnelInfoSummary personnelInfoSummary = buildPersonnelInfoSummary();

        mFragment = spy(mFragment);
        mFragment.onResponse(personnelInfoSummary);
        verify(mFragment).setFragmentVisibility(true);
        verify(mFragment).notifyResponseReceivedListeners(UserSummaryFragment.class);

        final UserSummaryFragment.ViewHolder viewHolder = TestUtils.getVariable(mFragment, "mViewHolder");
        final ValueListItem orgItem = TestUtils.getVariable(viewHolder, "mOrganization");

        assertFalse(orgItem.isItemClickable());
    }

    @Test
    public void onResponse_modelValid_multipleOrg() {
        final PersonnelInfoSummary personnelInfoSummary = buildPersonnelInfoSummary();
        personnelInfoSummary.has_one_organization = 0;

        mFragment = spy(mFragment);
        mFragment.onResponse(personnelInfoSummary);
        verify(mFragment).setFragmentVisibility(true);
        verify(mFragment).notifyResponseReceivedListeners(UserSummaryFragment.class);

        final UserSummaryFragment.ViewHolder viewHolder = TestUtils.getVariable(mFragment, "mViewHolder");
        final ValueListItem orgItem = TestUtils.getVariable(viewHolder, "mOrganization");

        assertTrue(orgItem.isItemClickable());
    }

    @Test
    public void onResponse_onItemClickTextNull() {
        final PersonnelInfoSummary personnelInfoSummary = buildPersonnelInfoSummary();

        mFragment = spy(mFragment);
        mFragment.onResponse(personnelInfoSummary);
        verify(mFragment).setFragmentVisibility(true);
        verify(mFragment).notifyResponseReceivedListeners(UserSummaryFragment.class);

        final RecyclerView listView = spy(mFragment.getView().findViewById(R.id.settingsList));

        ((ListArrayRecyclerAdapter)listView.getAdapter()).getOnItemClickListener().onItemClick(null, Mockito.mock(IListItem.class));
    }

    @Test
    public void onResponse_onItemClickValid() {
        final PersonnelInfoSummary personnelInfoSummary = buildPersonnelInfoSummary();

        mFragment = spy(mFragment);
        mFragment.onResponse(personnelInfoSummary);
        verify(mFragment).setFragmentVisibility(true);
        verify(mFragment).notifyResponseReceivedListeners(UserSummaryFragment.class);

        final RecyclerView listView = spy(mFragment.getView().findViewById(R.id.settingsList));
        final IListItem item = spy(new TextListItem(ApplicationProvider.getApplicationContext().getString(R.string.about)));

        ((ListArrayRecyclerAdapter)listView.getAdapter()).getOnItemClickListener().onItemClick(null, item);

        verify(item, Mockito.times(3)).getTitle();
    }

    @Test
    public void onResponse_onItemClickItemTitleNull() {
        final PersonnelInfoSummary personnelInfoSummary = buildPersonnelInfoSummary();

        mFragment = spy(mFragment);
        mFragment.onResponse(personnelInfoSummary);
        verify(mFragment).setFragmentVisibility(true);
        verify(mFragment).notifyResponseReceivedListeners(UserSummaryFragment.class);

        final RecyclerView listView = spy(mFragment.getView().findViewById(R.id.settingsList));
        final ListArrayRecyclerAdapter adapter = (ListArrayRecyclerAdapter) listView.getAdapter();
        final IListItem item = spy(new TextListItem("TestListItem"));
        doReturn(null).when(item).getTitle();

        adapter.getOnItemClickListener().onItemClick(null, item);

        verify(item).getTitle();
    }

    @Test
    public void onResponse_PersonnelInfoSummary_ImageSet() {
        final PersonnelInfoSummary personnelInfoSummary = buildPersonnelInfoSummary();

        mFragment = spy(mFragment);
        TestUtils.setVariable(mFragment, "mUserImage", Mockito.mock(Bitmap.class));
        mFragment.onResponse(personnelInfoSummary);
        verify(mFragment).setFragmentVisibility(true);
        verify(mFragment).notifyResponseReceivedListeners(UserSummaryFragment.class);
    }

    //TODO AV032294: CURA-2510 uncomment once we start getting the images back from services
    /*@Test
    public void onResponse_PersonnelPhoto_nullAdapter() {
        final PersonnelPhoto personnelPhoto = new PersonnelPhoto();
        personnelPhoto.image = Mockito.mock(Bitmap.class);

        mFragment = Mockito.spy(mFragment);
        TestUtils.setVariable(mFragment, "mAdapter", null);
        assertNull(TestUtils.getVariable(mFragment, "mUserImage"));
        mFragment.onResponse(personnelPhoto);
        assertNotNull(TestUtils.getVariable(mFragment, "mUserImage"));
    }

    @Test
    public void onResponse_PersonnelPhoto_validNotCurSet() {
        final PersonnelPhoto personnelPhoto = new PersonnelPhoto();
        personnelPhoto.image = Mockito.mock(Bitmap.class);
        final UserSummaryFragment.ViewHolder viewHolder = new UserSummaryFragment.ViewHolder(ApplicationProvider.getApplicationContext());
        TestUtils.setVariable(mFragment, "mViewHolder", viewHolder);

        mFragment = Mockito.spy(mFragment);
        final ListArrayAdapter adapter = Mockito.mock(ListArrayAdapter.class);
        TestUtils.setVariable(mFragment, "mAdapter", adapter);
        assertNull(TestUtils.getVariable(mFragment, "mUserImage"));
        mFragment.onResponse(personnelPhoto);
        Mockito.verify(adapter).notifyDataSetChanged();
        assertEquals(personnelPhoto.image, TestUtils.getVariable(mFragment, "mUserImage"));
    }

    @Test
    public void onResponse_PersonnelPhoto_validCurSet_SameImage() {
        final PersonnelPhoto personnelPhoto = new PersonnelPhoto();
        personnelPhoto.image = Mockito.mock(Bitmap.class);
        final SettingsPageHeaderListItem headerItem = Mockito.mock(SettingsPageHeaderListItem.class);
        final UserSummaryFragment.ViewHolder viewHolder = new UserSummaryFragment.ViewHolder(ApplicationProvider.getApplicationContext());
        TestUtils.setVariable(viewHolder, "mSettingsPageHeaderListItem", headerItem);

        Mockito.when(personnelPhoto.image.sameAs(Mockito.any(Bitmap.class))).thenReturn(true);
        mFragment = Mockito.spy(mFragment);
        TestUtils.setVariable(mFragment, "mViewHolder", viewHolder);
        TestUtils.setVariable(mFragment, "mUserImage", personnelPhoto.image);
        final ListArrayAdapter adapter = Mockito.mock(ListArrayAdapter.class);
        TestUtils.setVariable(mFragment, "mAdapter", adapter);
        mFragment.onResponse(personnelPhoto);
        Mockito.verify(adapter, Mockito.never()).notifyDataSetChanged();
    }

    @Test
    public void onResponse_PersonnelPhoto_validCurSet_DifferentImage() {
        final PersonnelPhoto personnelPhoto = new PersonnelPhoto();
        personnelPhoto.image = Mockito.mock(Bitmap.class);
        final SettingsPageHeaderListItem headerItem = Mockito.mock(SettingsPageHeaderListItem.class);
        final UserSummaryFragment.ViewHolder viewHolder = new UserSummaryFragment.ViewHolder(ApplicationProvider.getApplicationContext());
        TestUtils.setVariable(viewHolder, "mSettingsPageHeaderListItem", headerItem);
        TestUtils.setVariable(mFragment, "mUserImage", personnelPhoto.image);

        Mockito.when(personnelPhoto.image.sameAs(Mockito.any(Bitmap.class))).thenReturn(false);
        mFragment = Mockito.spy(mFragment);
        final ListArrayAdapter adapter = Mockito.mock(ListArrayAdapter.class);
        TestUtils.setVariable(mFragment, "mAdapter", adapter);
        mFragment.onResponse(personnelPhoto);
        Mockito.verify(adapter).notifyDataSetChanged();
    }*/

    @Test
    public void onNoContentResponse() {
        mFragment = spy(mFragment);

        //TODO AV032294: CURA-2510 uncomment once we start getting the images back from services
        /* mFragment.onNoContentResponse(null, PersonnelPhoto.class);
        Mockito.verify(mFragment, Mockito.never()).setFragmentVisibility(true); */

        mFragment.onNoContentResponse(null, null);
        verify(mFragment).setFragmentVisibility(true);
    }

    @Test
    public void onErrorResponse() {
        mFragment = spy(mFragment);
        mFragment.onErrorResponse(new VolleyError("TestExceptionMessage"), null);

        verify(mFragment).setFragmentVisibility(true);

        //TODO AV032294: CURA-2510 uncomment once we start getting the images back from services
        /* assertNull(ShadowAlertDialog.getLatestAlertDialog());
        mFragment.onErrorResponse(new VolleyError("TestExceptionMessage"), PersonnelPhoto.class);
        Mockito.verify(mFragment).setFragmentVisibility(true); //Ensures that this hasn't been hit a second time.  default case uses Mockito.times(1) */
    }

    @Test
    public void onFailedResponse_cacheUsed() {
        mFragment = spy(mFragment);
        Mockito.doNothing().when(mFragment).onNoContentResponse(null, Object.class);
        mFragment.onFailedResponse(Object.class, true);
        verify(mFragment, never()).onNoContentResponse(null, Object.class);
    }

    @Test
    public void onFailedResponse_cacheNotUsed() {
        mFragment = spy(mFragment);
        Mockito.doNothing().when(mFragment).onNoContentResponse(null, Object.class);
        mFragment.onFailedResponse(Object.class, false);
        verify(mFragment).onNoContentResponse(null, Object.class);
    }

    @Test
    public void updateFragmentList_pinManagerNull() {
        mFragment = spy(mFragment);
        TestUtils.setVariable(IonAuthnApplication.getInstance().getAuthenticationManager(), "mPinManager", null);
        TestUtils.invokePrivateMethod(mFragment, "updateFragmentList");

        final ListArrayRecyclerAdapter adapter = TestUtils.getVariable(mFragment, "mAdapter");
        assertEquals(4, adapter.getItemCount());

        IonAuthnApplication.getInstance().getAuthenticationManager().logout(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void setupActionBar_activityNull() {
        mFragment = spy(mFragment);

        doReturn(null).when(mFragment).getActivity();

        mFragment.setupActionBar();

        verify(mFragment).getActivity();
    }

    @Test
    public void setupActionBar_actionBarNull() {
        mockActivity = spy(mockActivity);
        mFragment = spy(mFragment);

        doReturn(mockActivity).when(mFragment).getActivity();
        doReturn(null).when(mockActivity).getActionBar();

        mFragment.setupActionBar();
    }

    @Test
    public void setupActionBar() {
        mockActivity = spy(mockActivity);
        mFragment = spy(mFragment);
        final ActionBar bar = Mockito.mock(ActionBar.class);

        doReturn(mockActivity).when(mFragment).getActivity();
        doReturn(bar).when(mockActivity).getSupportActionBar();

        mFragment.setupActionBar();

        verify(bar).setTitle(R.string.settings_title);
        verify(bar).setDisplayShowHomeEnabled(true);
        verify(bar).setHomeButtonEnabled(true);
    }

    @Test
    public void handleClicks_createPin_NotIonActivity() {
        mFragment = spy(mFragment);
        final Activity activity = new Activity();
        doReturn(activity).when(mFragment).getActivity();
        TestUtils.invokePrivateMethod(mFragment, "handleClicks", new Class[]{String.class}, mFragment.getString(R.string.settings_createpin));
        final Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertNull(intent);
    }

    @Test
    public void handleClicks_changePin_NotIonActivity() {
        mFragment = spy(mFragment);
        final Activity activity = new Activity();
        doReturn(activity).when(mFragment).getActivity();
        TestUtils.invokePrivateMethod(mFragment, "handleClicks", new Class[]{String.class}, mFragment.getString(R.string.settings_changepin));
        final Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertNull(intent);
    }

    @Test
    public void handleClicks_removePin_NotIonActivity() {
        mFragment = spy(mFragment);
        final Activity activity = new Activity();
        doReturn(activity).when(mFragment).getActivity();
        TestUtils.invokePrivateMethod(mFragment, "handleClicks", new Class[]{String.class}, mFragment.getString(R.string.settings_removepin));
        final Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertNull(intent);
    }

    @Test
    public void onResponse_modelValid_GetAuthResponseNull() {
        final PersonnelInfoSummary personnelInfoSummary = buildPersonnelInfoSummary();

        IonAuthnSessionUtils.setAuthnResponse(ApplicationProvider.getApplicationContext().getApplicationContext(), null);

        mFragment = spy(mFragment);
        mFragment.onResponse(personnelInfoSummary);
        verify(mFragment).setFragmentVisibility(true);
        verify(mFragment).notifyResponseReceivedListeners(UserSummaryFragment.class);

        final RecyclerView listView = mFragment.getView().findViewById(R.id.settingsList);
        final ListArrayRecyclerAdapter adapter = (ListArrayRecyclerAdapter) listView.getAdapter();
        assertNotNull(listView);
        assertEquals(6, adapter.getItemCount());
        assertTrue(adapter.get(0) instanceof SettingsPageHeaderListItem);
        assertTrue(adapter.get(1) instanceof ValueListItem);
        assertTrue(adapter.get(2) instanceof TextListItem);
        assertTrue(adapter.get(3) instanceof TextListItem);
        assertTrue(adapter.get(4) instanceof TextListItem);
        assertTrue(adapter.get(5) instanceof TextListItem);

        assertEquals(mFragment.getString(R.string.settings_organization), adapter.get(1).getTitle());
        assertNull(adapter.get(2).getTitle());
        assertEquals(mFragment.getString(R.string.about), adapter.get(3).getTitle());
        assertNull(adapter.get(4).getTitle());
        assertEquals(mFragment.getString(R.string.settings_createpin), adapter.get(5).getTitle());

        IonAuthnApplication.getInstance().getAuthenticationManager().logout(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void onResponse_modelValid_GetUserNull() {
        final PersonnelInfoSummary personnelInfoSummary = buildPersonnelInfoSummary();

        final User user = null;
        final Capabilities capabilities = Capabilities.newCapabilities(Collections.singletonList("com.cerner.nursing.nursing.dev"),
                                                                       0, 0, 0, "OxMOCKTENANTGUIDTHATM-EANSNOTHIN", false, true); //The timeout is in milliseconds for some reason

        final Map<String, Boolean> features = Collections.singletonMap("feature/useMock", true);

        final AuthnResponse response = AuthnResponse.newAuthnResponse(user, capabilities, features, SessionCheckHandler.Status.unlocked.name());

        final ProvisionedTenant tenant = new ProvisionedTenant();
        tenant.tenantId = "OxMOCKTENANTGUIDTHATM-EANSNOTHIN";

        IonAuthnSessionUtils.setAuthnResponse(ApplicationProvider.getApplicationContext().getApplicationContext(), response);
        IonAuthnSessionUtils.setTenant(tenant);

        mFragment = spy(mFragment);
        mFragment.onResponse(personnelInfoSummary);
        verify(mFragment).setFragmentVisibility(true);
        verify(mFragment).notifyResponseReceivedListeners(UserSummaryFragment.class);

        final RecyclerView listView = mFragment.getView().findViewById(R.id.settingsList);
        final ListArrayRecyclerAdapter adapter = (ListArrayRecyclerAdapter) listView.getAdapter();
        assertNotNull(listView);
        assertEquals(6, adapter.getItemCount());
        assertTrue(adapter.get(0) instanceof SettingsPageHeaderListItem);
        assertTrue(adapter.get(1) instanceof ValueListItem);
        assertTrue(adapter.get(2) instanceof TextListItem);
        assertTrue(adapter.get(3) instanceof TextListItem);
        assertTrue(adapter.get(4) instanceof TextListItem);
        assertTrue(adapter.get(5) instanceof TextListItem);

        assertEquals(mFragment.getString(R.string.settings_organization), adapter.get(1).getTitle());
        assertNull(adapter.get(2).getTitle());
        assertEquals(mFragment.getString(R.string.about), adapter.get(3).getTitle());
        assertNull(adapter.get(4).getTitle());
        assertEquals(mFragment.getString(R.string.settings_createpin), adapter.get(5).getTitle());

        IonAuthnApplication.getInstance().getAuthenticationManager().logout(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void getData() {
        mFragment = spy(mFragment);
        mFragment.getData(IDataRetriever.DataArgs.NONE);
    }

    private static PersonnelInfoSummary buildPersonnelInfoSummary() {
        final PersonnelInfoSummary personnelInfoSummary = new PersonnelInfoSummary();
        personnelInfoSummary.organization_id = "orgid";
        personnelInfoSummary.has_one_organization = 1;
        personnelInfoSummary.organization_name = "orgname";
        return personnelInfoSummary;
    }
}