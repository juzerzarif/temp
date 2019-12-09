package com.cerner.nursing.nursing.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.cerner.cura.allergies.ui.AllergyFragment;
import com.cerner.cura.base.ICuraFragment;
import com.cerner.cura.base.PatientContext;
import com.cerner.cura.datamodel.PatientViewSummary;
import com.cerner.cura.items_for_review.datamodel.ItemsForReviewList;
import com.cerner.cura.items_for_review.datamodel.common.OrderAction;
import com.cerner.cura.items_for_review.ui.ItemsForReviewFragment;
import com.cerner.cura.items_for_review.ui.elements.ItemsForReviewCardListItem;
import com.cerner.cura.items_for_review.ui.elements.OrderListItem;
import com.cerner.cura.requestor.IDataRetriever;
import com.cerner.cura.requestor.MockDataManager;
import com.cerner.cura.test.helper.TestUtils;
import com.cerner.cura.ui.elements.IListItem;
import com.cerner.cura.ui.elements.TextListItem;
import com.cerner.cura.ui.elements.cards.ListCard;
import com.cerner.cura.utils.AppUtils;
import com.cerner.cura.vitals.datamodel.common.LegacyVitalSigns;
import com.cerner.cura.vitals.datamodel.common.LegacyVitalsResult;
import com.cerner.cura.vitals.datamodel.common.LegacyVitalsResultGroup;
import com.cerner.cura.vitals.datamodel.common.QuantityResult;
import com.cerner.cura.vitals.datamodel.common.Result;
import com.cerner.cura.vitals.ui.AssessmentsFragment;
import com.cerner.cura.vitals.ui.ItemDetailsResultFragment;
import com.cerner.cura.vitals.ui.ResultListItem;
import com.cerner.cura.vitals.ui.VitalsAndMeasurementsFragment;
import com.cerner.cura.vitals.ui.VitalsHistoryFragment;
import com.cerner.ion.security.IonAuthnActivity;
import com.cerner.ion.session.AuthnResponse;
import com.cerner.ion.session.IonAuthnSessionUtils;
import com.cerner.ion.session.SessionCheckHandler;
import com.cerner.nursing.nursing.R;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

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
 * @author Lam Tran (lt028506) on 11/6/13.
 * @author Mark Lear (ML015922)
 */
@RunWith (RobolectricTestRunner.class)
public class PatientSummaryFragmentTests {
    private View mView;
    private PatientChartActivity mockActivity;
    private PatientSummaryFragment mFragment;

    @BeforeClass
    public static void ClassSetup() {
        System.gc();
    }

    /**
     * set up empty mock activity to attach tested fragment
     */
    @Before
    public void setup() {
        TestUtils.setStaticVariable(MockDataManager.class, "smState", MockDataManager.MockState.READ);
        PatientContext.clearContext();
        PatientContext.setPatientAndEncounter("test_patient_id_" + System.currentTimeMillis(), "test_encounter_id_" + System.currentTimeMillis());
        PatientContext.setHasRelationship(true);

        mockActivity = Robolectric.buildActivity(PatientChartActivity.class).create().start().get();

        TestUtils.invokePrivateMethod(mockActivity, "navigateToFragment", new Class[]{Class.class, Bundle.class, int.class, boolean.class, boolean.class},
                                      PatientSummaryFragment.class, null, R.id.content_pane_fragment_container, false, false);
        mFragment = (PatientSummaryFragment) mockActivity.getFragmentManager().findFragmentByTag(PatientSummaryFragment.class.toString());
        mView = mFragment.getView().findViewById(R.id.patient_summary_table);
    }

    /**
     * Clean up fragment and mock activity
     */
    @After
    public void tearDown() {
        mView = null;
        mFragment = null;
        mockActivity = null;
    }

    @Test
    public void onCreateView() {
        assertEquals(mFragment.getView().findViewById(R.id.refresh_layout_patient_summary), TestUtils.getVariable(mFragment, "mRefreshLayout"));
    }

    @Test
    public void onCreateView_activityFinishing() {
        mFragment = Mockito.spy(mFragment);
        final LayoutInflater layoutInflater = Mockito.mock(LayoutInflater.class);
        final RecyclerView view = Mockito.mock(RecyclerView.class);
        doReturn(view).when(layoutInflater).inflate(R.layout.patient_summary_fragment, null, false);

        mFragment.getActivity().finish();
        mFragment.onCreateView(layoutInflater, null, null);

        verify(mFragment, never()).setRefreshLayout(view.findViewById(Mockito.anyInt()));
    }

    @Test
    public void onCreateView_noVitalCharting() {
        final LayoutInflater layoutInflater = Mockito.mock(LayoutInflater.class);
        doReturn(mFragment.getView()).when(layoutInflater).inflate(R.layout.patient_summary_fragment, null, false);
        final HashMap<String, Boolean> map = new HashMap<>();
        final AuthnResponse response = AuthnResponse.newAuthnResponse(null, null, map, SessionCheckHandler.Status.unlocked.name());
        IonAuthnSessionUtils.setAuthnResponse(ApplicationProvider.getApplicationContext(), response);
        final ListCard listCard = mView.findViewById(R.id.patientsummary_vitalsandmeasurements);
        TestUtils.setVariable(listCard, "mTitleButtonDrawable", null);

        mFragment.onCreateView(layoutInflater, null, null);
        assertNull(TestUtils.getVariable(listCard, "mTitleButtonDrawable"));
    }

    @Test
    public void fragmentIsVisible() {
        assertNotNull(mockActivity);
        assertNotNull(mFragment);
        assertNotNull(mFragment.getActivity());
    }

    @Test
    public void onDetach() {
        mFragment.onDetach();
        assertNull(TestUtils.getVariable(mFragment, "mCallback"));
        assertNull(TestUtils.getVariable(mFragment, "mDrawerCallback"));
    }

    @Test
    public void onAuthnStart() {
        mFragment = Mockito.spy(mFragment);
        Mockito.doNothing().when(mFragment).getData(IDataRetriever.DataArgs.REFRESH);
        mFragment.onAuthnStart();
        verify(mFragment).getData(IDataRetriever.DataArgs.REFRESH);
    }

    @Test
    public void getData_noPatientSelected() {
        PatientContext.clearContext();
        mFragment.getData(IDataRetriever.DataArgs.NONE);
    }

    @Test
    public void getData_noPatientRelationship() {
        PatientContext.setHasRelationship(false);
        mFragment.getData(IDataRetriever.DataArgs.NONE);
    }

    @Test
    public void onResponse_getActivityNull() {
        mFragment = Mockito.spy(mFragment);
        doReturn(null).when(mFragment).getActivity();
        mFragment.onResponse(new Object());
        verify(mFragment).getActivity();
        verify(mFragment, never()).getResources();
    }

    @Test
    public void onResponse_nullOnGetView() {
        final PatientSummaryFragment fragment = new PatientSummaryFragment();
        fragment.onResponse((Object) null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void onResponse_modelNotInstanceOfPatientViewSummary() {
        mFragment.onResponse(new Object());
    }

    @Test
    public void onResponse_validModel() {
        mFragment = Mockito.spy(mFragment);
        mFragment.onResponse(createDefaultMockPatientViewSummary());
        verify(mFragment).setFragmentVisibility(true);
        verify(mFragment).notifyResponseReceivedListeners(PatientSummaryFragment.class);
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
        final AppCompatActivity activity = Mockito.mock(AppCompatActivity.class);
        doReturn(activity).when(mFragment).getActivity();
        mFragment.onErrorResponse(new VolleyError("TestExceptionMessage"), null);
        verify(activity).onBackPressed();
    }

    @Test
    public void onErrorResponse_getActivityNull() {
        mFragment = Mockito.spy(mFragment);
        doReturn(null).when(mFragment).getActivity();
        mFragment.onErrorResponse(new VolleyError("TestExceptionMessage"), null);

        verify(mFragment).getActivity();

        assertNull(ShadowAlertDialog.getLatestAlertDialog());
    }

    /* ------------------------- Item for review card --------------------------------------------------*/

    /**
     * Item for review card should be visible with no title and has 1 element in its list.
     */
    @Test
    public void itemForReviewCard_onResponse_noNotification() {
        //Sections is null
        PatientViewSummary summary = new PatientViewSummary();
        summary.sections = null;
        mFragment.onResponse(summary);
        final ListCard itemForReview = mView.findViewById(R.id.patientsummary_itemforreviewcard);

        // Test list item
        List<IListItem> list = TestUtils.getVariable(itemForReview, "mList");
        assertNotNull(list);
        assertEquals(1, list.size());

        // Test value item
        ItemsForReviewCardListItem valueItem = (ItemsForReviewCardListItem) list.get(0);

        assertNotNull(valueItem);
        assertTrue(TestUtils.getVariable(mFragment, "mLockDownItemsForReviewCard"));
        assertFalse(valueItem.isItemClickable());

        summary = createDefaultMockPatientViewSummary();
        summary.sections.itemsForReviewCount = new PatientViewSummary.ItemsForReviewCountSection();
        summary.sections.itemsForReviewCount.nonCriticalCount = 0;
        summary.sections.itemsForReviewCount.criticalCount = 0;

        mFragment.onResponse(summary);

        // Test list item
        list = TestUtils.getVariable(itemForReview, "mList");
        assertNotNull(list);
        assertEquals(1, list.size());

        // Test value item
        valueItem = (ItemsForReviewCardListItem) list.get(0);

        assertNotNull(valueItem);

        final TextView titleView = mView.findViewById(R.id.itemsforreview_card_nameView);
        final TextView noti1View = mView.findViewById(R.id.itemsforreview_card_critical);
        final TextView noti2View = mView.findViewById(R.id.itemsforreview_card_non_critical);
        final TextView noNewItemView = mView.findViewById(R.id.itemsforreview_card_no_new_items);

        assertNotNull(titleView);
        assertEquals(View.VISIBLE, titleView.getVisibility());

        assertEquals(mView.getResources().getString(R.string.items_for_review_fragment_title), titleView.getText().toString());

        assertNotNull(noti1View);
        assertEquals(View.GONE, noti1View.getVisibility());
        assertEquals(String.valueOf(0), noti1View.getText().toString());

        assertNotNull(noti1View.getCompoundDrawablesRelative()[0]);

        assertNotNull(noti2View);
        assertEquals(View.GONE, noti2View.getVisibility());
        assertEquals(String.valueOf(0), noti2View.getText().toString());

        assertNotNull(noNewItemView);
        assertEquals(View.VISIBLE, noNewItemView.getVisibility());
        assertEquals("No New Items", noNewItemView.getText().toString());

        assertNotNull(noti2View.getCompoundDrawablesRelative()[0]);
        assertTrue(TestUtils.getVariable(mFragment, "mLockDownItemsForReviewCard"));
        assertFalse(valueItem.isItemClickable());
    }

    /**
     * Item for review card with 1 critical and 0 new notification
     */
    @Test
    public void itemForReviewCard_onResponse_onlyCriticalNotification() {
        final PatientViewSummary summary = createDefaultMockPatientViewSummary();
        summary.sections.itemsForReviewCount = new PatientViewSummary.ItemsForReviewCountSection();
        summary.sections.itemsForReviewCount.nonCriticalCount = 0;
        summary.sections.itemsForReviewCount.criticalCount = 1;

        mFragment.onResponse(summary);
        final ListCard itemForReview = mView.findViewById(R.id.patientsummary_itemforreviewcard);

        // Test list item
        final List<IListItem> list = TestUtils.getVariable(itemForReview, "mList");
        assertNotNull(list);
        assertEquals(1, list.size());

        // Test value item
        final ItemsForReviewCardListItem valueItem = (ItemsForReviewCardListItem) list.get(0);

        assertNotNull(valueItem);

        final TextView titleView = mView.findViewById(R.id.itemsforreview_card_nameView);
        final TextView noti1View = mView.findViewById(R.id.itemsforreview_card_critical);
        final TextView noti2View = mView.findViewById(R.id.itemsforreview_card_non_critical);

        assertNotNull(titleView);
        assertEquals(View.VISIBLE, titleView.getVisibility());

        assertEquals(mView.getResources().getString(R.string.items_for_review_fragment_title), titleView.getText().toString());

        assertNotNull(noti1View);
        assertEquals(View.VISIBLE, noti1View.getVisibility());

        assertEquals(String.valueOf(1), noti1View.getText().toString());

        assertNotNull(noti1View.getCompoundDrawablesRelative()[0]);

        assertNotNull(noti2View);
        assertEquals(View.GONE, noti2View.getVisibility());
        assertEquals(String.valueOf(0), noti2View.getText().toString());

        assertNotNull(noti2View.getCompoundDrawablesRelative()[0]);
        assertTrue(valueItem.isItemClickable());
    }

    /**
     * Item for review card with 0 critical and 5 new notification
     */
    @Test
    public void itemForReviewCard_onResponse_onlyNewNotification() {
        final PatientViewSummary summary = createDefaultMockPatientViewSummary();
        summary.sections.itemsForReviewCount = new PatientViewSummary.ItemsForReviewCountSection();
        summary.sections.itemsForReviewCount.nonCriticalCount = 5;
        summary.sections.itemsForReviewCount.criticalCount = 0;

        mFragment.onResponse(summary);

        final TextView titleView = mView.findViewById(R.id.itemsforreview_card_nameView);
        final TextView noti1View = mView.findViewById(R.id.itemsforreview_card_critical);
        final TextView noti2View = mView.findViewById(R.id.itemsforreview_card_non_critical);

        assertNotNull(titleView);
        assertEquals(View.VISIBLE, titleView.getVisibility());

        assertEquals(mView.getResources().getString(R.string.items_for_review_fragment_title), titleView.getText().toString());

        assertNotNull(noti1View);
        assertEquals(View.GONE, noti1View.getVisibility());
        assertEquals(String.valueOf(0), noti1View.getText().toString());

        assertNotNull(noti1View.getCompoundDrawablesRelative()[0]);

        assertNotNull(noti2View);
        assertEquals(View.VISIBLE, noti2View.getVisibility());
        assertEquals(String.valueOf(5), noti2View.getText().toString());

        assertNotNull(noti2View.getCompoundDrawablesRelative()[0]);
    }

    /**
     * Item for review card with 10 critical and 15 new notification
     */
    @Test
    public void itemForReviewCard_onResponse_haveBothNotifications() {
        final PatientViewSummary summary = createDefaultMockPatientViewSummary();
        summary.sections.itemsForReviewCount = new PatientViewSummary.ItemsForReviewCountSection();
        summary.sections.itemsForReviewCount.nonCriticalCount = 15;
        summary.sections.itemsForReviewCount.criticalCount = 10;

        mFragment = Mockito.spy(mFragment);
        mFragment.onResponse(summary);

        final TextView titleView = mView.findViewById(R.id.itemsforreview_card_nameView);
        final TextView noti1View = mView.findViewById(R.id.itemsforreview_card_critical);
        final TextView noti2View = mView.findViewById(R.id.itemsforreview_card_non_critical);

        assertNotNull(titleView);
        assertEquals(View.VISIBLE, titleView.getVisibility());

        assertEquals(mView.getResources().getString(R.string.items_for_review_fragment_title), titleView.getText().toString());

        assertNotNull(noti1View);
        assertEquals(View.VISIBLE, noti1View.getVisibility());

        assertEquals(String.valueOf(10), noti1View.getText().toString());

        assertNotNull(noti2View);
        assertEquals(View.VISIBLE, noti2View.getVisibility());

        assertEquals(String.valueOf(15), noti2View.getText().toString());

        assertNotNull(noti1View.getCompoundDrawablesRelative()[0]);

        assertNotNull(noti2View.getCompoundDrawablesRelative()[0]);

        verify(mFragment).setFragmentVisibility(true);
        verify(mFragment).notifyResponseReceivedListeners(PatientSummaryFragment.class);
    }

    /* ------------------------- Allergy card --------------------------------------------------*/

    @Test
    public void allergyCard_isVisible_noAllergyRecorded() {
        final ListCard allergyCard = mView.findViewById(R.id.patientsummary_allergiescard);

        assertNotNull(allergyCard);
        assertEquals(View.VISIBLE, allergyCard.getVisibility());

        final PatientViewSummary summary = createDefaultMockPatientViewSummary();
        summary.sections.allergy.allergies = null;
        summary.sections.allergy.notRecorded = true;
        mFragment.onResponse(summary);

        final List<IListItem> list = TestUtils.getVariable(allergyCard, "mList");
        assertNotNull(list);
        assertEquals(1, list.size());

        allergyCardHelper(allergyCard, AppUtils.setHtmlTextColor(mView.getResources().getString(R.string.no_allergies_recorded),
                                                                 mView.getResources().getColor(R.color.StatusCritical), mView.getContext()));
    }

    @Test
    public void allergyCard_onResponse_notRecorded() {
        final ListCard allergyCard = mView.findViewById(R.id.patientsummary_allergiescard);
        final PatientViewSummary summary = createDefaultMockPatientViewSummary();

        summary.sections.allergy.notRecorded = true;
        mFragment.onResponse(summary);
        allergyCardHelper(allergyCard, AppUtils.setHtmlTextColor(mView.getResources().getString(R.string.no_allergies_recorded),
                                                                 mView.getResources().getColor(R.color.StatusCritical), mView.getContext()));
    }

    @Test
    public void allergyCard_onResponse_onlyNKA() {
        final PatientViewSummary summary = createDefaultMockPatientViewSummary();
        summary.sections.allergy.sectionStates.add("NO_KNOWN_ALLERGIES");
        summary.sections.allergy.notRecorded = false;

        //item not NKA
        summary.sections.allergy.allergies = new ArrayList<>();
        summary.sections.allergy.allergies.add(createMockAllergy("allergy1", "Other"));
        summary.sections.allergy.sectionStates.clear();
        mFragment.onResponse(summary);
        assertTrue(TestUtils.getVariable(mFragment, "mEnableAllergiesCard"));

        //item is NKA. Happy flow.
        summary.sections.allergy.allergies.remove(0);
        summary.sections.allergy.allergies.add(createMockAllergy("allergy1", "Other"));
        summary.sections.allergy.sectionStates.add("NO_KNOWN_ALLERGIES");
        summary.sections.allergy.notRecorded = false;
        mFragment.onResponse(summary);
        assertFalse(TestUtils.getVariable(mFragment, "mEnableAllergiesCard"));

        //more than 1 item
        summary.sections.allergy.allergies.add(createMockAllergy("allergy2", "Other"));
        summary.sections.allergy.sectionStates.clear();
        summary.sections.allergy.notRecorded = false;
        mFragment.onResponse(summary);
        assertTrue(TestUtils.getVariable(mFragment, "mEnableAllergiesCard"));

        summary.sections.allergy.allergies = null;
        mFragment.onResponse(summary);
        assertFalse(TestUtils.getVariable(mFragment, "mEnableAllergiesCard"));
    }

    @Test
    public void allergyCard_onResponse_onlyLowAllergy() {
        final ListCard allergyCard = mView.findViewById(R.id.patientsummary_allergiescard);

        final PatientViewSummary summary = createDefaultMockPatientViewSummary();
        summary.sections.allergy.allergies.add(createMockAllergy("a", AppUtils.ALLERGY_LOW));

        mFragment.onResponse(summary);
        allergyCardHelper(allergyCard, AppUtils.setHtmlTextColor("a", mView.getResources().getColor(R.color.ContentPrimary), mView.getContext()));
    }

    @Test
    public void allergyCard_onResponse_onlySevereAllergy() {
        final ListCard allergyCard = mView.findViewById(R.id.patientsummary_allergiescard);

        final PatientViewSummary summary = createDefaultMockPatientViewSummary();
        summary.sections.allergy.allergies.add(createMockAllergy("a", AppUtils.ALLERGY_HIGH));

        mFragment.onResponse(summary);
        allergyCardHelper(allergyCard, AppUtils.setHtmlTextColor("a", mView.getResources().getColor(R.color.StatusCritical), mView.getContext()));
    }

    @Test
    public void allergyCard_onResponse_correctOrderAllergy() {
        final ListCard allergyCard = mView.findViewById(R.id.patientsummary_allergiescard);

        final PatientViewSummary summary = createDefaultMockPatientViewSummary();
        summary.sections.allergy.allergies.add(createMockAllergy("a", AppUtils.ALLERGY_HIGH));
        summary.sections.allergy.allergies.add(createMockAllergy("b", AppUtils.ALLERGY_HIGH));
        summary.sections.allergy.allergies.add(createMockAllergy("c", AppUtils.ALLERGY_NORMAL));
        summary.sections.allergy.allergies.add(createMockAllergy("d", "other"));

        mFragment = Mockito.spy(mFragment);
        mFragment.onResponse(summary);
        final String expectedAllergies = AppUtils.setHtmlTextColor("a", mView.getResources().getColor(R.color.StatusCritical), mView.getContext())
                                         + AppUtils.setHtmlTextColor(mView.getResources().getString(R.string.list_delineator), mView.getResources().getColor(R.color.ContentPrimary), mView.getContext())
                                         + AppUtils.setHtmlTextColor("b", mView.getResources().getColor(R.color.StatusCritical), mView.getContext())
                                         + AppUtils.setHtmlTextColor(mView.getResources().getString(R.string.list_delineator), mView.getResources().getColor(R.color.ContentPrimary), mView.getContext())
                                         + AppUtils.setHtmlTextColor("c", mView.getResources().getColor(R.color.ContentPrimary), mView.getContext())
                                         + AppUtils.setHtmlTextColor(mView.getResources().getString(R.string.list_delineator), mView.getResources().getColor(R.color.ContentPrimary), mView.getContext())
                                         + AppUtils.setHtmlTextColor("d", mView.getResources().getColor(R.color.ContentPrimary), mView.getContext());

        allergyCardHelper(allergyCard, expectedAllergies);

        verify(mFragment).setFragmentVisibility(true);
        verify(mFragment).notifyResponseReceivedListeners(PatientSummaryFragment.class);
    }

    private static void allergyCardHelper(final ListCard listcard, final String expectedAllergies) {
        assertNotNull(listcard);
        assertEquals(View.VISIBLE, listcard.getVisibility());

        final List<IListItem> list = TestUtils.getVariable(listcard, "mList");

        assertNotNull(list);
        assertEquals(1, list.size());
        final TextListItem textItem = (TextListItem) list.get(0);

        assertEquals(R.layout.allergy_text_layout, (int) TestUtils.getVariable(textItem, "mLayout"));
        assertEquals(expectedAllergies, textItem.getTitle());
    }

    /* ------------------------- Vital Card--------------------------------------------------*/

    @Test
    public void vital_onResponse_emptyVitalObject() {
        mFragment = Mockito.spy(mFragment);
        final PatientViewSummary vital = new PatientViewSummary();
        vital.sections = new PatientViewSummary.Sections();
        vital.sections.vitalSigns = new LegacyVitalSigns();
        mFragment.onResponse(vital);

        verify(mFragment).setFragmentVisibility(true);
        verify(mFragment).notifyResponseReceivedListeners(PatientSummaryFragment.class);

        final ListCard listCard = mView.findViewById(R.id.patientsummary_vitalsandmeasurements);
        //noinspection unchecked
        final List<IListItem> items = TestUtils.getVariable(listCard, "mList");
        assertEquals(0, items.size());
    }

    @Test
    public void vital_onResponse_allowVitalsCharting() {
        final ICuraFragment.OnFragmentSelected fragmentSelected = Mockito.mock(ICuraFragment.OnFragmentSelected.class);
        TestUtils.setVariable(mFragment, "mCallback", fragmentSelected);
        final PatientViewSummary vital = new PatientViewSummary();
        vital.sections = new PatientViewSummary.Sections();
        vital.sections.vitalSigns = new LegacyVitalSigns();
        vital.sections.vitalSigns.allowVitalsCharting = true;
        mFragment.onResponse(vital);

        final ListCard listCard = mView.findViewById(R.id.patientsummary_vitalsandmeasurements);
        View.OnClickListener clickListener = TestUtils.getVariable(listCard, "mTitleButtonListener");
        clickListener.onClick(null);
        verify(fragmentSelected).onFragmentSelected(AssessmentsFragment.class, null);

        TestUtils.setVariable(mFragment, "mCallback", null);
        mFragment.onResponse(vital);
        clickListener = TestUtils.getVariable(listCard, "mTitleButtonListener");
        clickListener.onClick(null);

        vital.sections.vitalSigns.allowVitalsCharting = false;
        mFragment.onResponse(vital);
        assertNull(TestUtils.getVariable(listCard, "mTitleButtonListener"));
    }

    @Test
    public void vital_onResponse_hasTemp() {
        final PatientViewSummary vital = new PatientViewSummary();
        vital.sections = new PatientViewSummary.Sections();
        vital.sections.vitalSigns = new LegacyVitalSigns();
        vital.sections.vitalSigns.temperature = new LegacyVitalsResultGroup();
        vital.sections.vitalSigns.temperature.resultName = "Temp";
        vital.sections.vitalSigns.temperature.mostRecentResult = new LegacyVitalsResult();
        vital.sections.vitalSigns.temperature.mostRecentResult.quantityResult = new QuantityResult();
        vital.sections.vitalSigns.temperature.mostRecentResult.quantityResult.resultValue = "15.5";
        mFragment.onResponse(vital);

        final ListCard listCard = mView.findViewById(R.id.patientsummary_vitalsandmeasurements);
        final View child = ((LinearLayout)((CardView)listCard.getChildAt(0)).getChildAt(0)).getChildAt(2);
        assertEquals("Temp", ((TextView) child.findViewById(R.id.result_nameView)).getText().toString());
        assertEquals("15.5", ((TextView) child.findViewById(R.id.result1_valueView)).getText().toString());
    }

    @Test
    public void vital_onResponse_hasEmptyTempDisplay() {
        final PatientViewSummary vital = new PatientViewSummary();
        vital.sections = new PatientViewSummary.Sections();
        vital.sections.vitalSigns = new LegacyVitalSigns();
        vital.sections.vitalSigns.temperature = new LegacyVitalsResultGroup();
        vital.sections.vitalSigns.temperature.mostRecentResult = new LegacyVitalsResult();
        vital.sections.vitalSigns.temperature.mostRecentResult.quantityResult = new QuantityResult();
        vital.sections.vitalSigns.temperature.resultName = "";
        vital.sections.vitalSigns.temperature.mostRecentResult.quantityResult.resultValue = "40";
        mFragment.onResponse(vital);
        final ListCard listCard = mView.findViewById(R.id.patientsummary_vitalsandmeasurements);
        final View child = ((LinearLayout)((CardView)listCard.getChildAt(0)).getChildAt(0)).getChildAt(2);
        assertEquals("", ((TextView) child.findViewById(R.id.result_nameView)).getText().toString());
        assertEquals("40", ((TextView) child.findViewById(R.id.result1_valueView)).getText().toString());
    }

    @Test
    public void vital_onResponse_hasHeartRate() {
        final PatientViewSummary vital = new PatientViewSummary();
        vital.sections = new PatientViewSummary.Sections();
        vital.sections.vitalSigns = new LegacyVitalSigns();
        vital.sections.vitalSigns.heartRateGeneric = new LegacyVitalsResultGroup();
        vital.sections.vitalSigns.heartRateGeneric.mostRecentResult = new LegacyVitalsResult();
        vital.sections.vitalSigns.heartRateGeneric.mostRecentResult.quantityResult = new QuantityResult();
        vital.sections.vitalSigns.heartRateGeneric.resultName = "HR";
        vital.sections.vitalSigns.heartRateGeneric.mostRecentResult.quantityResult.resultValue = "90";
        mFragment.onResponse(vital);
        final ListCard listCard = mView.findViewById(R.id.patientsummary_vitalsandmeasurements);
        final View child = ((LinearLayout)((CardView)listCard.getChildAt(0)).getChildAt(0)).getChildAt(2);
        assertEquals("HR", ((TextView) child.findViewById(R.id.result_nameView)).getText().toString());
        assertEquals("90", ((TextView) child.findViewById(R.id.result1_valueView)).getText().toString());
    }

    @Test
    public void vital_onResponse_hasEmptyHeartRateDisplay() {
        final PatientViewSummary vital = new PatientViewSummary();
        vital.sections = new PatientViewSummary.Sections();
        vital.sections.vitalSigns = new LegacyVitalSigns();
        vital.sections.vitalSigns.heartRateGeneric = new LegacyVitalsResultGroup();
        vital.sections.vitalSigns.heartRateGeneric.mostRecentResult = new LegacyVitalsResult();
        vital.sections.vitalSigns.heartRateGeneric.resultName = "";
        mFragment.onResponse(vital);
        final ListCard listCard = mView.findViewById(R.id.patientsummary_vitalsandmeasurements);
        final View child = ((LinearLayout)((CardView)listCard.getChildAt(0)).getChildAt(0)).getChildAt(2);
        assertEquals("", ((TextView) child.findViewById(R.id.result_nameView)).getText().toString());
        assertEquals(mFragment.getActivity().getString(R.string.value_default), ((TextView) child.findViewById(R.id.result1_valueView)).getText().toString());
    }

    @Test
    public void vital_onResponse_hasResRate() {
        final PatientViewSummary vital = new PatientViewSummary();
        vital.sections = new PatientViewSummary.Sections();
        vital.sections.vitalSigns = new LegacyVitalSigns();
        vital.sections.vitalSigns.respiratoryRate = new LegacyVitalsResultGroup();
        vital.sections.vitalSigns.respiratoryRate.mostRecentResult = new LegacyVitalsResult();
        vital.sections.vitalSigns.respiratoryRate.mostRecentResult.quantityResult = new QuantityResult();
        vital.sections.vitalSigns.respiratoryRate.resultName = "RR";
        vital.sections.vitalSigns.respiratoryRate.mostRecentResult.quantityResult.resultValue = "30";
        mFragment.onResponse(vital);
        final ListCard listCard = mView.findViewById(R.id.patientsummary_vitalsandmeasurements);
        final View child = ((LinearLayout)((CardView)listCard.getChildAt(0)).getChildAt(0)).getChildAt(2);
        assertEquals("RR", ((TextView) child.findViewById(R.id.result_nameView)).getText().toString());
        assertEquals("30", ((TextView) child.findViewById(R.id.result1_valueView)).getText().toString());
    }

    @Test
    public void vital_onResponse_hasNullResRateDisplay() {
        final PatientViewSummary vital = new PatientViewSummary();
        vital.sections = new PatientViewSummary.Sections();
        vital.sections.vitalSigns = new LegacyVitalSigns();
        vital.sections.vitalSigns.respiratoryRate = new LegacyVitalsResultGroup();
        vital.sections.vitalSigns.respiratoryRate.resultName = "RR";
        mFragment.onResponse(vital);
        final ListCard listCard = mView.findViewById(R.id.patientsummary_vitalsandmeasurements);
        final View child = ((LinearLayout)((CardView)listCard.getChildAt(0)).getChildAt(0)).getChildAt(2);
        assertEquals("RR", ((TextView) child.findViewById(R.id.result_nameView)).getText().toString());
        assertEquals(mFragment.getActivity().getString(R.string.value_default), ((TextView) child.findViewById(R.id.result1_valueView)).getText().toString());
    }

    @Test
    public void vital_onResponse_hasEmptyResRateDisplay() {
        final PatientViewSummary vital = new PatientViewSummary();
        vital.sections = new PatientViewSummary.Sections();
        vital.sections.vitalSigns = new LegacyVitalSigns();
        vital.sections.vitalSigns.respiratoryRate = new LegacyVitalsResultGroup();
        vital.sections.vitalSigns.respiratoryRate.resultName = "RR";
        mFragment.onResponse(vital);
        final ListCard listCard = mView.findViewById(R.id.patientsummary_vitalsandmeasurements);
        final View child = ((LinearLayout)((CardView)listCard.getChildAt(0)).getChildAt(0)).getChildAt(2);
        assertEquals("RR", ((TextView) child.findViewById(R.id.result_nameView)).getText().toString());
        assertEquals(mFragment.getActivity().getString(R.string.value_default), ((TextView) child.findViewById(R.id.result1_valueView)).getText().toString());
    }

    @Test
    public void vital_onResponse_hasbloodPressureGeneric_systolicAndDiastolic() {
        final PatientViewSummary vital = new PatientViewSummary();
        vital.sections = new PatientViewSummary.Sections();
        vital.sections.vitalSigns = new LegacyVitalSigns();
        vital.sections.vitalSigns.bloodPressureGeneric = new LegacyVitalsResultGroup();
        vital.sections.vitalSigns.bloodPressureGeneric.resultName = "BP";
        vital.sections.vitalSigns.bloodPressureGeneric.mostRecentResult = new LegacyVitalsResult();
        vital.sections.vitalSigns.bloodPressureGeneric.mostRecentResult.systolic = new LegacyVitalsResult();
        vital.sections.vitalSigns.bloodPressureGeneric.mostRecentResult.diastolic = new LegacyVitalsResult();
        vital.sections.vitalSigns.bloodPressureGeneric.mostRecentResult.systolic.quantityResult = new QuantityResult();
        vital.sections.vitalSigns.bloodPressureGeneric.mostRecentResult.diastolic.quantityResult = new QuantityResult();
        vital.sections.vitalSigns.bloodPressureGeneric.mostRecentResult.systolic.quantityResult.resultValue = "120";
        vital.sections.vitalSigns.bloodPressureGeneric.mostRecentResult.diastolic.quantityResult.resultValue = "80";
        mFragment.onResponse(vital);
        final ListCard listCard = mView.findViewById(R.id.patientsummary_vitalsandmeasurements);
        final View child = ((LinearLayout)((CardView)listCard.getChildAt(0)).getChildAt(0)).getChildAt(2);
        assertEquals("BP", ((TextView) child.findViewById(R.id.result_nameView)).getText().toString());
        assertEquals("120", ((TextView) child.findViewById(R.id.result1_valueView)).getText().toString());
        assertEquals("80", ((TextView) child.findViewById(R.id.result2_valueView)).getText().toString());
    }

    @Test
    public void vital_onResponse_hasEmpty_bloodPressureGeneric_Display() {
        final PatientViewSummary vital = new PatientViewSummary();
        vital.sections = new PatientViewSummary.Sections();
        vital.sections.vitalSigns = new LegacyVitalSigns();
        vital.sections.vitalSigns.bloodPressureGeneric = new LegacyVitalsResultGroup();
        vital.sections.vitalSigns.bloodPressureGeneric.resultName = "";
        vital.sections.vitalSigns.bloodPressureGeneric.mostRecentResult = new LegacyVitalsResult();
        vital.sections.vitalSigns.bloodPressureGeneric.mostRecentResult.systolic = new LegacyVitalsResult();
        vital.sections.vitalSigns.bloodPressureGeneric.mostRecentResult.diastolic = new LegacyVitalsResult();
        vital.sections.vitalSigns.bloodPressureGeneric.mostRecentResult.systolic.quantityResult = new QuantityResult();
        vital.sections.vitalSigns.bloodPressureGeneric.mostRecentResult.diastolic.quantityResult = new QuantityResult();
        vital.sections.vitalSigns.bloodPressureGeneric.mostRecentResult.systolic.quantityResult.resultValue = "120";
        vital.sections.vitalSigns.bloodPressureGeneric.mostRecentResult.diastolic.quantityResult.resultValue = "80";
        mFragment.onResponse(vital);
        final ListCard listCard = mView.findViewById(R.id.patientsummary_vitalsandmeasurements);
        final View child = ((LinearLayout)((CardView)listCard.getChildAt(0)).getChildAt(0)).getChildAt(2);
        assertEquals("", ((TextView) child.findViewById(R.id.result_nameView)).getText().toString());
        assertEquals("120", ((TextView) child.findViewById(R.id.result1_valueView)).getText().toString());
    }

    @Test
    public void measurement_onResponse_hasWeight() {
        final PatientViewSummary measurement = new PatientViewSummary();
        measurement.sections = new PatientViewSummary.Sections();
        measurement.sections.vitalSigns = new LegacyVitalSigns();
        measurement.sections.vitalSigns.weight = new LegacyVitalsResultGroup();
        measurement.sections.vitalSigns.weight.mostRecentResult = new LegacyVitalsResult();
        measurement.sections.vitalSigns.weight.mostRecentResult.quantityResult = new QuantityResult();
        measurement.sections.vitalSigns.weight.resultName = "Weight";
        measurement.sections.vitalSigns.weight.mostRecentResult.quantityResult.resultValue = "200.2";
        mFragment.onResponse(measurement);
        final ListCard listCard = mView.findViewById(R.id.patientsummary_vitalsandmeasurements);
        final View child = ((LinearLayout)((CardView)listCard.getChildAt(0)).getChildAt(0)).getChildAt(2);
        assertEquals("Weight", ((TextView) child.findViewById(R.id.result_nameView)).getText().toString());
        assertEquals("200.2", ((TextView) child.findViewById(R.id.result1_valueView)).getText().toString());
    }

    @Test
    public void vital_onResponse_hasOxygen() {
        final PatientViewSummary vital = new PatientViewSummary();
        vital.sections = new PatientViewSummary.Sections();
        vital.sections.vitalSigns = new LegacyVitalSigns();
        vital.sections.vitalSigns.oxygenSaturation = new LegacyVitalsResultGroup();
        vital.sections.vitalSigns.oxygenSaturation.mostRecentResult = new LegacyVitalsResult();
        vital.sections.vitalSigns.oxygenSaturation.mostRecentResult.quantityResult = new QuantityResult();
        vital.sections.vitalSigns.oxygenSaturation.resultName = "O2 Sat";
        vital.sections.vitalSigns.oxygenSaturation.mostRecentResult.quantityResult.resultValue = "77";
        mFragment = Mockito.spy(mFragment);
        mFragment.onResponse(vital);
        final ListCard listCard = mView.findViewById(R.id.patientsummary_vitalsandmeasurements);
        final View child = ((LinearLayout)((CardView)listCard.getChildAt(0)).getChildAt(0)).getChildAt(2);
        assertEquals("O2 Sat", ((TextView) child.findViewById(R.id.result_nameView)).getText().toString());
        assertEquals("77", ((TextView) child.findViewById(R.id.result1_valueView)).getText().toString());
        verify(mFragment).setFragmentVisibility(true);
        verify(mFragment).notifyResponseReceivedListeners(PatientSummaryFragment.class);
    }

    @Test
    public void vital_onResponse_hasNullOxygenDisplay() {
        final PatientViewSummary vital = new PatientViewSummary();
        vital.sections = new PatientViewSummary.Sections();
        vital.sections.vitalSigns = new LegacyVitalSigns();
        vital.sections.vitalSigns.oxygenSaturation = new LegacyVitalsResultGroup();
        vital.sections.vitalSigns.oxygenSaturation.resultName = "O2 Sat";
        mFragment.onResponse(vital);
        final ListCard listCard = mView.findViewById(R.id.patientsummary_vitalsandmeasurements);
        final View child = ((LinearLayout)((CardView)listCard.getChildAt(0)).getChildAt(0)).getChildAt(2);
        assertEquals("O2 Sat", ((TextView) child.findViewById(R.id.result_nameView)).getText().toString());
        assertEquals(mFragment.getActivity().getString(R.string.value_default), ((TextView) child.findViewById(R.id.result1_valueView)).getText().toString());
    }

    /* ------------------------- Test helper method--------------------------------------------------*/

    @Test
    public void onResponse_cardsNull() {
        mFragment = Mockito.spy(mFragment);
        final View view = Mockito.spy(mFragment.getView());
        final PatientViewSummary vitals = new PatientViewSummary();
        doReturn(view).when(mFragment).getView();
        doReturn(null).when(view).findViewById(Mockito.anyInt());
        mFragment.onResponse(vitals);
    }

    @Test
    public void getIntRepresentAllergyTest() {
        final Class[] classes = {PatientViewSummary.Allergy.class};
        assertEquals(1, (int) TestUtils.invokePrivateMethod(mFragment, "allergySeverityToInt", classes, createMockAllergy("a", AppUtils.ALLERGY_HIGH)));
        assertEquals(2, (int) TestUtils.invokePrivateMethod(mFragment, "allergySeverityToInt", classes, createMockAllergy("b", AppUtils.ALLERGY_NORMAL)));
        assertEquals(3, (int) TestUtils.invokePrivateMethod(mFragment, "allergySeverityToInt", classes, createMockAllergy("c", AppUtils.ALLERGY_LOW)));
        assertEquals(4, (int) TestUtils.invokePrivateMethod(mFragment, "allergySeverityToInt", classes, createMockAllergy("d", "Other")));
        assertEquals(5, (int) TestUtils.invokePrivateMethod(mFragment, "allergySeverityToInt", classes, createMockAllergy("d", "")));
    }

    @Test
    public void setAllergyDisplayStringAndColor_nullActivity() {
        final Class[] classes = {PatientViewSummary.Allergy.class};
        TestUtils.setVariable(mFragment, "mHost", null);
        assertNull(TestUtils.invokePrivateMethod(mFragment, "setAllergyDisplayStringAndColor", classes, createMockAllergy(null, null)));
    }

    @Test
    public void setAllergyDisplayStringAndColor_DisplayNameNull() {
        final Class[] classes = {PatientViewSummary.Allergy.class};

        assertNull(TestUtils.invokePrivateMethod(mFragment, "setAllergyDisplayStringAndColor", classes, createMockAllergy(null, null)));
    }

    @Test
    public void setAllergyDisplayStringAndColor_displayNameButMeaningNull() {
        final Class[] classes = {PatientViewSummary.Allergy.class};
        assertEquals(AppUtils.setHtmlTextColor("display", mView.getResources().getColor(R.color.ContentPrimary), mView.getContext()),
                     TestUtils.invokePrivateMethod(mFragment, "setAllergyDisplayStringAndColor", classes, createMockAllergy("display", null)));
    }

    @Test
    public void setAllergyDisplayStringAndColor_allergyWithDisplayName() {
        final Class[] classes = {PatientViewSummary.Allergy.class};

        //Non-severe allergies with black color
        assertEquals(AppUtils.setHtmlTextColor("a", mView.getResources().getColor(R.color.ContentPrimary), mView.getContext()),
                     TestUtils.invokePrivateMethod(mFragment, "setAllergyDisplayStringAndColor", classes, createMockAllergy("a", AppUtils.ALLERGY_NORMAL)));
        assertEquals(AppUtils.setHtmlTextColor("b", mView.getResources().getColor(R.color.ContentPrimary), mView.getContext()),
                     TestUtils.invokePrivateMethod(mFragment, "setAllergyDisplayStringAndColor", classes, createMockAllergy("b", AppUtils.ALLERGY_LOW)));
        assertEquals(AppUtils.setHtmlTextColor("c", mView.getResources().getColor(R.color.ContentPrimary), mView.getContext()),
                     TestUtils.invokePrivateMethod(mFragment, "setAllergyDisplayStringAndColor", classes, createMockAllergy("c", "aa")));

        // Severe allergy with red color
        assertEquals(AppUtils.setHtmlTextColor("d", mView.getResources().getColor(R.color.StatusCritical), mView.getContext()),
                     TestUtils.invokePrivateMethod(mFragment, "setAllergyDisplayStringAndColor", classes, createMockAllergy("d", AppUtils.ALLERGY_HIGH)));
    }

    @Test
    public void sortAllergiesArray_sameOrder() {
        final Class[] classes = {List.class};
        final List<PatientViewSummary.Allergy> list = new ArrayList<>();

        //empty list
        assertEquals(list, TestUtils.invokePrivateMethod(mFragment, "sortAllergiesArray", classes, list));

        list.add(createMockAllergy("a", AppUtils.ALLERGY_SEVERE));
        assertEquals(list, TestUtils.invokePrivateMethod(mFragment, "sortAllergiesArray", classes, list));

        list.add(createMockAllergy("b", AppUtils.ALLERGY_SEVERE));
        assertEquals(list, TestUtils.invokePrivateMethod(mFragment, "sortAllergiesArray", classes, list));

        list.add(createMockAllergy("c", AppUtils.ALLERGY_SEVERE));
        assertEquals(list, TestUtils.invokePrivateMethod(mFragment, "sortAllergiesArray", classes, list));

        list.add(createMockAllergy("d", AppUtils.ALLERGY_MODERATE));
        assertEquals(list, TestUtils.invokePrivateMethod(mFragment, "sortAllergiesArray", classes, list));

        list.add(createMockAllergy("e", AppUtils.ALLERGY_MILD));
        assertEquals(list, TestUtils.invokePrivateMethod(mFragment, "sortAllergiesArray", classes, list));

        list.add(createMockAllergy("f", AppUtils.ALLERGY_MILD));
        assertEquals(list, TestUtils.invokePrivateMethod(mFragment, "sortAllergiesArray", classes, list));

        list.add(createMockAllergy("g", "Other"));
        assertEquals(list, TestUtils.invokePrivateMethod(mFragment, "sortAllergiesArray", classes, list));
    }

    @Test
    public void sortAllergiesArray_reverseOrder() {
        final Class[] classes = {List.class};
        final List<PatientViewSummary.Allergy> list = new ArrayList<>();

        //empty list
        assertEquals(list, TestUtils.invokePrivateMethod(mFragment, "sortAllergiesArray", classes, list));

        list.add(createMockAllergy("a", "Other"));
        list.add(createMockAllergy("b", AppUtils.ALLERGY_LOW));
        list.add(createMockAllergy("c", AppUtils.ALLERGY_NORMAL));
        list.add(createMockAllergy("d", AppUtils.ALLERGY_HIGH));

        final List<PatientViewSummary.Allergy> expectedList = new ArrayList<>(list);
        Collections.reverse(expectedList);
        assertEquals(expectedList, TestUtils.invokePrivateMethod(mFragment, "sortAllergiesArray", classes, list));
    }

    @Test
    public void sortAllergiesArray_randomOrder() {
        final Class[] classes = {List.class};
        final List<PatientViewSummary.Allergy> list = new ArrayList<>();

        list.add(createMockAllergy("a", "Other"));
        list.add(createMockAllergy("d", AppUtils.ALLERGY_HIGH));
        list.add(createMockAllergy("c", AppUtils.ALLERGY_NORMAL));
        list.add(createMockAllergy("b", AppUtils.ALLERGY_LOW));

        final List<String> expectedList = new ArrayList<>();
        expectedList.add("d");
        expectedList.add("c");
        expectedList.add("b");
        expectedList.add("a");

        TestUtils.invokePrivateMethod(mFragment, "sortAllergiesArray", classes, list);
        final List<String> actualList = new ArrayList<>();
        for (final PatientViewSummary.Allergy allergy : list) {
            actualList.add(allergy.substance);
        }

        assertEquals(expectedList, actualList);
    }

    @Test
    public void sortAllergiesArray_hasNullAllergy() {
        final Class[] classes = {List.class};
        final List<PatientViewSummary.Allergy> list = new ArrayList<>();

        list.add(createMockAllergy("a", "Other"));
        list.add(null);
        list.add(createMockAllergy("d", AppUtils.ALLERGY_HIGH));
        list.add(createMockAllergy("c", AppUtils.ALLERGY_NORMAL));
        list.add(null);
        list.add(null);
        list.add(createMockAllergy("b", AppUtils.ALLERGY_LOW));
        list.add(null);

        final List<String> expectedList = new ArrayList<>();
        expectedList.add("d");
        expectedList.add("c");
        expectedList.add("b");
        expectedList.add("a");
        expectedList.add(null);
        expectedList.add(null);
        expectedList.add(null);
        expectedList.add(null);

        TestUtils.invokePrivateMethod(mFragment, "sortAllergiesArray", classes, list);
        final List<String> actualList = new ArrayList<>();
        for (final PatientViewSummary.Allergy allergy : list) {
            if (allergy != null) {
                actualList.add(allergy.substance);
            } else {
                actualList.add(null);
            }
        }

        assertEquals(expectedList, actualList);
    }

    @Test
    public void sortAllergiesArray_hasTwoNullDisplay() {
        final List<PatientViewSummary.Allergy> list = new ArrayList<>();

        final PatientViewSummary.Allergy allergy1 = createMockAllergy(null, "Other");
        final PatientViewSummary.Allergy allergy2 = createMockAllergy(null, "Other");

        list.add(allergy1);
        list.add(allergy2);

        final List<PatientViewSummary.Allergy> sorted = TestUtils.invokePrivateMethod(mFragment, "sortAllergiesArray", new Class[]{List.class}, list);

        assertEquals(sorted.get(0), allergy1);
        assertEquals(sorted.get(1), allergy2);
    }

    @Test
    public void sortAllergiesArray_hasRightHandNullDisplay() {
        final List<PatientViewSummary.Allergy> list = new ArrayList<>();

        final PatientViewSummary.Allergy allergy1 = createMockAllergy(null, "Other");
        final PatientViewSummary.Allergy allergy2 = createMockAllergy("allergy", "Other");

        list.add(allergy1);
        list.add(allergy2);

        final List<PatientViewSummary.Allergy> sorted = TestUtils.invokePrivateMethod(mFragment, "sortAllergiesArray", new Class[]{List.class}, list);

        assertEquals(sorted.get(0), allergy2);
        assertEquals(sorted.get(1), allergy1);
    }

    @Test
    public void sortAllergiesArray_hasLeftHandNullDisplay() {
        final List<PatientViewSummary.Allergy> list = new ArrayList<>();

        final PatientViewSummary.Allergy allergy1 = createMockAllergy("allergy", "Other");
        final PatientViewSummary.Allergy allergy2 = createMockAllergy(null, "Other");

        list.add(allergy1);
        list.add(allergy2);

        final List<PatientViewSummary.Allergy> sorted = TestUtils.invokePrivateMethod(mFragment, "sortAllergiesArray", new Class[]{List.class}, list);

        assertEquals(sorted.get(0), allergy1);
        assertEquals(sorted.get(1), allergy2);
    }

    @Test (expected = ClassCastException.class)
    public void onAttach_activityNotInstanceOfOnFragmentSelected() {
        mFragment.onAttach(Mockito.mock(IonAuthnActivity.class));
    }

    @Test
    public void buildVitalAndMeasurementCard_getActivityNull() {
        mFragment = Mockito.spy(mFragment);
        doReturn(null).when(mFragment).getActivity();
        assertNull(TestUtils.invokePrivateMethod(mFragment, "buildVitalAndMeasurementCard", new Class[]{PatientViewSummary.class}, new PatientViewSummary()));
    }

    @Test
    public void buildVitalAndMeasurementCard_getResourcesNull() {
        mFragment = Mockito.spy(mFragment);
        doReturn(null).when(mFragment).getResources();
        assertNull(TestUtils.invokePrivateMethod(mFragment, "buildVitalAndMeasurementCard", new Class[]{PatientViewSummary.class}, new PatientViewSummary()));
    }

    @Test
    public void buildVitalAndMeasurementCard_measurementsNull() {
        final PatientViewSummary vitalsAndMeasurements = new PatientViewSummary();
        vitalsAndMeasurements.sections = new PatientViewSummary.Sections();
        vitalsAndMeasurements.sections.vitalSigns = new LegacyVitalSigns();
        vitalsAndMeasurements.sections.vitalSigns.weight = null;
        final List<IListItem> list = TestUtils.invokePrivateMethod(mFragment, "buildVitalAndMeasurementCard", new Class[]{PatientViewSummary.class}, vitalsAndMeasurements);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void buildVitalAndMeasurementCard_measurementsActualWeightListDisplayNull() {
        final PatientViewSummary vitalsAndMeasurements = new PatientViewSummary();
        vitalsAndMeasurements.sections = new PatientViewSummary.Sections();
        vitalsAndMeasurements.sections.vitalSigns = new LegacyVitalSigns();
        vitalsAndMeasurements.sections.vitalSigns.weight = new LegacyVitalsResultGroup();
        final List<IListItem> list = TestUtils.invokePrivateMethod(mFragment, "buildVitalAndMeasurementCard", new Class[]{PatientViewSummary.class}, vitalsAndMeasurements);
        assertNotNull(list);
        final ResultListItem<Object> vitalItem = (ResultListItem<Object>) list.get(0);
        assertEquals("", vitalItem.getTitle());
        assertEquals(mFragment.getString(R.string.value_default), vitalItem.getData().getValue1());
    }

    @Test
    public void buildVitalAndMeasurementCard_measurementsActualWeightListDisplayIsEmpty() {
        final PatientViewSummary vitalsAndMeasurements = new PatientViewSummary();
        vitalsAndMeasurements.sections = new PatientViewSummary.Sections();
        vitalsAndMeasurements.sections.vitalSigns = new LegacyVitalSigns();
        vitalsAndMeasurements.sections.vitalSigns.weight = new LegacyVitalsResultGroup();
        vitalsAndMeasurements.sections.vitalSigns.weight.resultName = "";
        final List<IListItem> list = TestUtils.invokePrivateMethod(mFragment, "buildVitalAndMeasurementCard", new Class[]{PatientViewSummary.class}, vitalsAndMeasurements);
        assertNotNull(list);
        final ResultListItem<Object> vitalItem = (ResultListItem<Object>) list.get(0);
        assertEquals("", vitalItem.getTitle());
        assertEquals(mFragment.getString(R.string.value_default), vitalItem.getData().getValue1());
    }

    @Test
    public void buildVitalAndMeasurementCard_bloodPressureGeneric_systolicNullAndDiastolicNull()
            {
        final PatientViewSummary vitalsAndMeasurements = new PatientViewSummary();
        vitalsAndMeasurements.sections = new PatientViewSummary.Sections();
        vitalsAndMeasurements.sections.vitalSigns = new LegacyVitalSigns();
        vitalsAndMeasurements.sections.vitalSigns.bloodPressureGeneric = new LegacyVitalsResultGroup();
        vitalsAndMeasurements.sections.vitalSigns.bloodPressureGeneric.resultName = "BP";

        final List<IListItem> list = TestUtils.invokePrivateMethod(mFragment, "buildVitalAndMeasurementCard", new Class[]{PatientViewSummary.class}, vitalsAndMeasurements);
        assertNotNull(list);
        final ResultListItem<Object> resultListItem = (ResultListItem<Object>) list.get(0);

        assertEquals("BP", resultListItem.getTitle());
        assertEquals(mFragment.getResources().getString(R.string.value_default), resultListItem.getData().getValue1());
    }

    @Test
    public void buildVitalAndMeasurementCard_bloodPressureGeneric_systolicValidAndDiastolicNull()
            {
        final PatientViewSummary vitalsAndMeasurements = new PatientViewSummary();
        vitalsAndMeasurements.sections = new PatientViewSummary.Sections();
        vitalsAndMeasurements.sections.vitalSigns = new LegacyVitalSigns();
        vitalsAndMeasurements.sections.vitalSigns.bloodPressureGeneric = new LegacyVitalsResultGroup();
        vitalsAndMeasurements.sections.vitalSigns.bloodPressureGeneric.mostRecentResult = new LegacyVitalsResult();
        vitalsAndMeasurements.sections.vitalSigns.bloodPressureGeneric.mostRecentResult.systolic = new LegacyVitalsResult();
        vitalsAndMeasurements.sections.vitalSigns.bloodPressureGeneric.mostRecentResult.systolic.quantityResult = new QuantityResult();
        vitalsAndMeasurements.sections.vitalSigns.bloodPressureGeneric.mostRecentResult.systolic.quantityResult.resultValue = "56";

        final List<IListItem> list = TestUtils.invokePrivateMethod(mFragment, "buildVitalAndMeasurementCard", new Class[]{PatientViewSummary.class}, vitalsAndMeasurements);
        assertNotNull(list);
        final ResultListItem<Object> resultListItem = (ResultListItem<Object>) list.get(0);

        assertEquals("", resultListItem.getTitle());
        assertEquals("56", resultListItem.getData().getValue1());
        assertEquals(mFragment.getResources().getString(R.string.value_default), resultListItem.getData().getValue2());
    }

    @Test
    public void buildVitalAndMeasurementCard_bloodPressureGeneric_systolicNullAndDiastolicValid()
            {
        final PatientViewSummary vitalsAndMeasurements = new PatientViewSummary();
        vitalsAndMeasurements.sections = new PatientViewSummary.Sections();
        vitalsAndMeasurements.sections.vitalSigns = new LegacyVitalSigns();
        vitalsAndMeasurements.sections.vitalSigns.bloodPressureGeneric = new LegacyVitalsResultGroup();
        vitalsAndMeasurements.sections.vitalSigns.bloodPressureGeneric.resultName = "BP";
        vitalsAndMeasurements.sections.vitalSigns.bloodPressureGeneric.mostRecentResult = new LegacyVitalsResult();
        vitalsAndMeasurements.sections.vitalSigns.bloodPressureGeneric.mostRecentResult.diastolic = new LegacyVitalsResult();
        vitalsAndMeasurements.sections.vitalSigns.bloodPressureGeneric.mostRecentResult.diastolic.quantityResult = new QuantityResult();
        vitalsAndMeasurements.sections.vitalSigns.bloodPressureGeneric.mostRecentResult.diastolic.quantityResult.resultValue = "56";

        final List<IListItem> list = TestUtils.invokePrivateMethod(mFragment, "buildVitalAndMeasurementCard", new Class[]{PatientViewSummary.class}, vitalsAndMeasurements);
        assertNotNull(list);
        final ResultListItem<Object> resultListItem = (ResultListItem<Object>) list.get(0);

        assertEquals("BP", resultListItem.getTitle());
        assertEquals(mFragment.getResources().getString(R.string.value_default), resultListItem.getData().getValue1());
        assertEquals("56", resultListItem.getData().getValue2());
    }

    @Test
    public void buildVitalAndMeasurementCard_bloodPressureGeneric_systolicValidAndDiastolicValid()
            {
        final PatientViewSummary vitalsAndMeasurements = new PatientViewSummary();
        vitalsAndMeasurements.sections = new PatientViewSummary.Sections();
        vitalsAndMeasurements.sections.vitalSigns = new LegacyVitalSigns();
        vitalsAndMeasurements.sections.vitalSigns.bloodPressureGeneric = new LegacyVitalsResultGroup();
        vitalsAndMeasurements.sections.vitalSigns.bloodPressureGeneric.resultName = "Blood Pressure";
        vitalsAndMeasurements.sections.vitalSigns.bloodPressureGeneric.mostRecentResult = new LegacyVitalsResult();
        vitalsAndMeasurements.sections.vitalSigns.bloodPressureGeneric.mostRecentResult.systolic = new LegacyVitalsResult();
        vitalsAndMeasurements.sections.vitalSigns.bloodPressureGeneric.mostRecentResult.systolic.quantityResult = new QuantityResult();
        vitalsAndMeasurements.sections.vitalSigns.bloodPressureGeneric.mostRecentResult.systolic.quantityResult.resultValue = "76";
        vitalsAndMeasurements.sections.vitalSigns.bloodPressureGeneric.mostRecentResult.diastolic = new LegacyVitalsResult();
        vitalsAndMeasurements.sections.vitalSigns.bloodPressureGeneric.mostRecentResult.diastolic.quantityResult = new QuantityResult();
        vitalsAndMeasurements.sections.vitalSigns.bloodPressureGeneric.mostRecentResult.diastolic.quantityResult.resultValue = "56";

        final List<IListItem> list = TestUtils.invokePrivateMethod(mFragment, "buildVitalAndMeasurementCard", new Class[]{PatientViewSummary.class}, vitalsAndMeasurements);
        assertNotNull(list);
        final ResultListItem<Object> resultListItem = (ResultListItem<Object>) list.get(0);

        assertEquals("Blood Pressure", resultListItem.getTitle());
        assertEquals("76", resultListItem.getData().getValue1());
        assertEquals("56", resultListItem.getData().getValue2());
    }

    @Test
    public void buildVitalAndMeasurementCard_painScore() {
        final PatientViewSummary vitalsAndMeasurements = new PatientViewSummary();
        vitalsAndMeasurements.sections = new PatientViewSummary.Sections();
        vitalsAndMeasurements.sections.vitalSigns = new LegacyVitalSigns();
        vitalsAndMeasurements.sections.vitalSigns.painScale = new LegacyVitalsResultGroup();
        vitalsAndMeasurements.sections.vitalSigns.painScale.resultName = "Pain Score";
        final List<IListItem> list = TestUtils.invokePrivateMethod(mFragment, "buildVitalAndMeasurementCard", new Class[]{PatientViewSummary.class}, vitalsAndMeasurements);
        assertNotNull(list);
        final ResultListItem<Object> vitalItem = (ResultListItem<Object>) list.get(0);
        assertEquals("Pain Score", vitalItem.getTitle());
        assertEquals(mFragment.getString(R.string.value_default), vitalItem.getData().getValue1());
    }

    @Test
    public void buildVitalAndMeasurementCard_bmi() {
        final PatientViewSummary vitalsAndMeasurements = new PatientViewSummary();
        vitalsAndMeasurements.sections = new PatientViewSummary.Sections();
        vitalsAndMeasurements.sections.vitalSigns = new LegacyVitalSigns();
        vitalsAndMeasurements.sections.vitalSigns.bodyMassIndex = new LegacyVitalsResultGroup();
        vitalsAndMeasurements.sections.vitalSigns.bodyMassIndex.resultName = "BMI";
        final List<IListItem> list = TestUtils.invokePrivateMethod(mFragment, "buildVitalAndMeasurementCard", new Class[]{PatientViewSummary.class}, vitalsAndMeasurements);
        assertNotNull(list);
        final ResultListItem<Object> vitalItem = (ResultListItem<Object>) list.get(0);
        assertEquals("BMI", vitalItem.getTitle());
        assertEquals(mFragment.getString(R.string.value_default), vitalItem.getData().getValue1());
    }

    @Test
    public void buildVitalAndMeasurementCard_height() {
        final PatientViewSummary vitalsAndMeasurements = new PatientViewSummary();
        vitalsAndMeasurements.sections = new PatientViewSummary.Sections();
        vitalsAndMeasurements.sections.vitalSigns = new LegacyVitalSigns();
        vitalsAndMeasurements.sections.vitalSigns.height = new LegacyVitalsResultGroup();
        vitalsAndMeasurements.sections.vitalSigns.height.resultName = "Height";
        final List<IListItem> list = TestUtils.invokePrivateMethod(mFragment, "buildVitalAndMeasurementCard", new Class[]{PatientViewSummary.class}, vitalsAndMeasurements);
        assertNotNull(list);
        final ResultListItem<Object> vitalItem = (ResultListItem<Object>) list.get(0);
        assertEquals("Height", vitalItem.getTitle());
        assertEquals(mFragment.getString(R.string.value_default), vitalItem.getData().getValue1());
    }

    @Test
    public void setupClickListeners_viewNull() {
        TestUtils.invokePrivateMethod(mFragment, "setupClickListeners");
    }

    @Test
    public void setupClickListeners_itemReviewCardClickListener() {
        final View view = mFragment.getView();
        assertNotNull(view);
        TestUtils.invokePrivateMethod(mFragment, "setupClickListeners");
        final ICuraFragment.OnFragmentSelected callback = Mockito.spy((ICuraFragment.OnFragmentSelected) TestUtils.getVariable(mFragment, "mCallback"));
        TestUtils.setVariable(mFragment, "mCallback", callback);
        mFragment.getView().findViewById(R.id.patientsummary_itemforreviewcard).performClick();
        verify(callback).onFragmentSelected(ItemsForReviewFragment.class, null);
    }

    @Test
    public void setupClickListeners_itemReviewCardClickListener_callbackNull() {
        TestUtils.setVariable(mFragment, "mCallback", null);
        mFragment.getView().findViewById(R.id.patientsummary_itemforreviewcard).performClick();
    }

    @Test
    public void setupClickListeners_itemReviewCardClickListener_noItemsForReview() {
        TestUtils.setVariable(mFragment, "mLockDownItemsForReviewCard", true);
        mFragment.getView().findViewById(R.id.patientsummary_itemforreviewcard).performClick();
    }

    @Test
    public void setupClickListeners_vitalCardClickListener() {
        final View view = mFragment.getView();
        assertNotNull(view);
        TestUtils.invokePrivateMethod(mFragment, "setupClickListeners");
        final ICuraFragment.OnFragmentSelected callback = Mockito.spy((ICuraFragment.OnFragmentSelected) TestUtils.getVariable(mFragment, "mCallback"));
        TestUtils.setVariable(mFragment, "mCallback", callback);
        Mockito.doNothing().when(callback).onFragmentSelected(VitalsAndMeasurementsFragment.class, null);
        mFragment.getView().findViewById(R.id.patientsummary_vitalsandmeasurements).findViewById(R.id.listcard_viewAllText).performClick();
        verify(callback).onFragmentSelected(VitalsAndMeasurementsFragment.class, null);
    }

    @Test
    public void setupClickListeners_vitalCardClickListener_callbackNull() {
        TestUtils.setVariable(mFragment, "mCallback", null);
        mFragment.getView().findViewById(R.id.patientsummary_vitalsandmeasurements).findViewById(R.id.listcard_viewAllText).performClick();
    }

    @Test
    public void setupClickListeners_allergyCardClickListener() {
        final View view = mFragment.getView();
        assertNotNull(view);
        TestUtils.invokePrivateMethod(mFragment, "setupClickListeners");
        final ICuraFragment.OnFragmentSelected callback = Mockito.spy((ICuraFragment.OnFragmentSelected) TestUtils.getVariable(mFragment, "mCallback"));
        TestUtils.setVariable(mFragment, "mCallback", callback);
        mFragment.getView().findViewById(R.id.patientsummary_allergiescard).performClick();
        verify(callback).onFragmentSelected(AllergyFragment.class, null);
    }

    @Test
    public void setupClickListeners_allergyCardClickListener_callbackNull() {
        TestUtils.setVariable(mFragment, "mCallback", null);
        mFragment.getView().findViewById(R.id.patientsummary_allergiescard).performClick();
    }

    @Test
    public void setupClickListeners_allergyCardClickListener_noKnownAllergies() {
        TestUtils.setVariable(mFragment, "mEnableAllergiesCard", false);
        mFragment.getView().findViewById(R.id.patientsummary_allergiescard).performClick();
    }

    @Test
    public void setupClickListeners_cellClickListener() {
        final PatientViewSummary summary = new PatientViewSummary();
        summary.sections = new PatientViewSummary.Sections();
        summary.sections.vitalSigns = new LegacyVitalSigns();
        summary.sections.vitalSigns.temperature = new LegacyVitalsResultGroup();
        summary.sections.vitalSigns.temperature.mostRecentResult = new LegacyVitalsResult();
        summary.sections.vitalSigns.temperature.mostRecentResult.quantityResult = new QuantityResult();
        summary.sections.vitalSigns.temperature.resultName = "Temperature";
        summary.sections.vitalSigns.temperature.mostRecentResult.quantityResult.resultValue = "33.0";

        final List<IListItem> listItemList = TestUtils
                .invokePrivateMethod(mFragment, "buildVitalAndMeasurementCard", new Class[]{PatientViewSummary.class}, ((PatientViewSummary) summary));
        final ListCard listCard = mView.findViewById(R.id.patientsummary_vitalsandmeasurements);
        listCard.setItemsList(listItemList);

        final ICuraFragment.OnFragmentSelected callback = Mockito.spy((ICuraFragment.OnFragmentSelected) TestUtils.getVariable(mFragment, "mCallback"));
        TestUtils.setVariable(mFragment, "mCallback", callback);

        final View vitalItem = mView.findViewById(R.id.resultItem);
        assertEquals(summary.sections.vitalSigns.temperature.resultName, ((TextView) vitalItem.findViewById(R.id.result_nameView)).getText().toString());
        vitalItem.performClick();

        verify(callback).onFragmentSelected(eq(VitalsHistoryFragment.class), Mockito.any(Bundle.class));
    }

    @Test
    public void onListCardItemClick_IListItemNotValid() {
        final ItemsForReviewList.OrderResult orderResult = new ItemsForReviewList.OrderResult();
        orderResult.latestAction = new OrderAction();
        final OrderListItem item = new OrderListItem(orderResult);
        final List<IListItem> listItemList = new ArrayList<>();
        listItemList.add(item);
        final ListCard listCard = mView.findViewById(R.id.patientsummary_vitalsandmeasurements);
        listCard.setItemsList(listItemList);

        final ICuraFragment.OnFragmentSelected callback = Mockito.spy((ICuraFragment.OnFragmentSelected) TestUtils.getVariable(mFragment, "mCallback"));
        TestUtils.setVariable(mFragment, "mCallback", callback);

        mView = Mockito.spy(mView);

        final View vitalItem = mView.findViewById(R.id.orderListItem);
        vitalItem.performClick();
        verify(callback, never()).onFragmentSelected(eq(ItemDetailsResultFragment.class), Mockito.any(Bundle.class));
    }

    @Test
    public void onListCardItemClick_IListItemNotRealItem() {
        mFragment = (PatientSummaryFragment) mockActivity.getFragmentManager().findFragmentByTag(PatientSummaryFragment.class.toString());

        final IListItem item = new ResultListItem<Object>(ApplicationProvider.getApplicationContext(), new Result(), false, true);
        final List<IListItem> listItemList = new ArrayList<>();
        listItemList.add(item);
        final ListCard listCard = mView.findViewById(R.id.patientsummary_vitalsandmeasurements);
        listCard.setItemsList(listItemList);

        final ICuraFragment.OnFragmentSelected callback = Mockito.spy((ICuraFragment.OnFragmentSelected) TestUtils.getVariable(mFragment, "mCallback"));

        final View vitalItem = mView.findViewById(R.id.resultItem);
        vitalItem.performClick();
        verify(callback, never()).onFragmentSelected(eq(ItemDetailsResultFragment.class), Mockito.any(Bundle.class));
    }

    @Test
    public void onListCardItemClick_IListItemNotRealItems() {
        mFragment = (PatientSummaryFragment) mockActivity.getFragmentManager().findFragmentByTag(PatientSummaryFragment.class.toString());
        final Result result = new Result();
        final IListItem item = new ResultListItem<Object>(ApplicationProvider.getApplicationContext(), result, false, false);
        final List<IListItem> listItemList = new ArrayList<>();
        listItemList.add(item);
        final ListCard listCard = mView.findViewById(R.id.patientsummary_vitalsandmeasurements);
        listCard.setItemsList(listItemList);

        final ICuraFragment.OnFragmentSelected callback = Mockito.spy((ICuraFragment.OnFragmentSelected) TestUtils.getVariable(mFragment, "mCallback"));

        final View vitalItem = mView.findViewById(R.id.resultItem);
        vitalItem.performClick();
        verify(callback, never()).onFragmentSelected(eq(ItemDetailsResultFragment.class), Mockito.any(Bundle.class));
    }

    @Test
    public void onListCardItemClick_IListItemCallBackNull() {
        mFragment = (PatientSummaryFragment) mockActivity.getFragmentManager().findFragmentByTag(PatientSummaryFragment.class.toString());

        IListItem item = new ResultListItem<Object>(ApplicationProvider.getApplicationContext(), new Result(), false, false);
        item = Mockito.spy(item);
        final List<IListItem> listItemList = new ArrayList<>();
        listItemList.add(item);
        final ListCard listCard = mView.findViewById(R.id.patientsummary_vitalsandmeasurements);
        listCard.setItemsList(listItemList);

        TestUtils.setVariable(mFragment, "mCallback", null);
        final View vitalItem = mView.findViewById(R.id.resultItem);
        vitalItem.performClick();
    }

    @Test
    public void buildAllergiesDisplay() {
        mFragment = Mockito.spy(mFragment);

        PatientViewSummary patientViewSummary = null;
        assertNull(TestUtils.invokePrivateMethod(mFragment, "buildAllergiesDisplay", new Class[]{PatientViewSummary.class}, patientViewSummary));

        //allergy null
        patientViewSummary = new PatientViewSummary();
        assertNull(TestUtils.invokePrivateMethod(mFragment, "buildAllergiesDisplay", new Class[]{PatientViewSummary.class}, patientViewSummary));

        //allergies null
        patientViewSummary.sections = new PatientViewSummary.Sections();
        patientViewSummary.sections.allergy = new PatientViewSummary.AllergySection();
        patientViewSummary.sections.allergy.allergies = null;
        assertNotNull(TestUtils.invokePrivateMethod(mFragment, "buildAllergiesDisplay", new Class[]{PatientViewSummary.class}, patientViewSummary));

        //allergies empty
        patientViewSummary.sections.allergy.allergies = new ArrayList<>();
        patientViewSummary.sections.allergy.sectionStates = new ArrayList<>();
        assertNotNull(TestUtils.invokePrivateMethod(mFragment, "buildAllergiesDisplay", new Class[]{PatientViewSummary.class}, patientViewSummary));

        //no known allergies
        patientViewSummary.sections.allergy.sectionStates = new ArrayList<>();
        patientViewSummary.sections.allergy.sectionStates.add("NO_KNOWN_ALLERGIES");
        patientViewSummary.sections.allergy.notRecorded = false;
        assertNotNull(TestUtils.invokePrivateMethod(mFragment, "buildAllergiesDisplay", new Class[]{PatientViewSummary.class}, patientViewSummary));

        //allergies null (no allergies recorded)
        patientViewSummary.sections.allergy.allergies = null;
        assertEquals(AppUtils.setHtmlTextColor(ApplicationProvider.getApplicationContext().getString(R.string.no_allergies_recorded), mockActivity.getResources().getColor(R.color.StatusCritical), mockActivity), TestUtils.invokePrivateMethod(mFragment, "buildAllergiesDisplay", new Class[]{PatientViewSummary.class}, patientViewSummary));

        //no known allergies
        doReturn(mockActivity).when(mFragment).getActivity();
        patientViewSummary.sections.allergy.allergies = new ArrayList<>();
        patientViewSummary.sections.allergy.sectionStates.clear();
        patientViewSummary.sections.allergy.allergies.add(createMockAllergy("a", "a"));
        patientViewSummary.sections.allergy.sectionStates.add("NO_KNOWN_ALLERGIES");
        patientViewSummary.sections.allergy.notRecorded = false;
        assertEquals(AppUtils.setHtmlTextColor(ApplicationProvider.getApplicationContext().getString(R.string.no_known_allergies), mockActivity.getResources().getColor(R.color.ContentPrimary), mockActivity), TestUtils.invokePrivateMethod(mFragment, "buildAllergiesDisplay", new Class[]{PatientViewSummary.class}, patientViewSummary));

        //medication allergies not recorded
        patientViewSummary.sections.allergy.allergies.clear();
        patientViewSummary.sections.allergy.sectionStates.clear();
        patientViewSummary.sections.allergy.allergies.add(createMockAllergy("a", "a"));
        patientViewSummary.sections.allergy.sectionStates.add("MEDICATION_ALLERGIES_NOT_RECORDED");
        patientViewSummary.sections.allergy.notRecorded = false;
        assertEquals(AppUtils.setHtmlTextColor(ApplicationProvider.getApplicationContext().getString(R.string.no_medication_allergies_recorded), mockActivity.getResources().getColor(R.color.ContentPrimary), mockActivity), TestUtils.invokePrivateMethod(mFragment, "buildAllergiesDisplay", new Class[]{PatientViewSummary.class}, patientViewSummary));

        //no known medication allergies
        patientViewSummary.sections.allergy.allergies.clear();
        patientViewSummary.sections.allergy.sectionStates.clear();
        patientViewSummary.sections.allergy.allergies.add(createMockAllergy("a", "a"));
        patientViewSummary.sections.allergy.sectionStates.add("NO_KNOWN_MEDICATION_ALLERGIES");
        patientViewSummary.sections.allergy.notRecorded = false;
        assertEquals(AppUtils.setHtmlTextColor(ApplicationProvider.getApplicationContext().getString(R.string.no_known_medication_allergies), mockActivity.getResources().getColor(R.color.ContentPrimary), mockActivity), TestUtils.invokePrivateMethod(mFragment, "buildAllergiesDisplay", new Class[]{PatientViewSummary.class}, patientViewSummary));

        //all allergies not loaded due to system error message
        patientViewSummary.sections.allergy.allergies.clear();
        patientViewSummary.sections.allergy.sectionStates.clear();
        patientViewSummary.sections.allergy.allergies.add(createMockAllergy("a", "a"));
        patientViewSummary.sections.allergy.allergies.add(createMockAllergy("b", "b"));
        patientViewSummary.sections.allergy.allergies.add(createMockAllergy("c", "c"));
        patientViewSummary.sections.allergy.allAllergiesNotLoaded = true;
        assertEquals(AppUtils.setHtmlTextColor(mockActivity.getString(R.string.all_allergies_not_shown_title), mockActivity.getResources().getColor(R.color.ContentPrimary), mockActivity), TestUtils.invokePrivateMethod(mFragment, "buildAllergiesDisplay", new Class[]{PatientViewSummary.class}, patientViewSummary));

        //multiple allergies present
        patientViewSummary.sections.allergy.allergies.clear();
        patientViewSummary.sections.allergy.sectionStates.clear();
        patientViewSummary.sections.allergy.allergies.add(createMockAllergy("a", "a"));
        patientViewSummary.sections.allergy.allergies.add(createMockAllergy("b", "b"));
        patientViewSummary.sections.allergy.allergies.add(createMockAllergy("c", "c"));
        patientViewSummary.sections.allergy.notRecorded = false;
        patientViewSummary.sections.allergy.allAllergiesNotLoaded = false;
        assertNotNull(TestUtils.invokePrivateMethod(mFragment, "buildAllergiesDisplay", new Class[]{PatientViewSummary.class}, patientViewSummary));
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
    public void setupActionBar_mDrawerCallbackNull() {
        mockActivity = Mockito.spy(mockActivity);
        mFragment = Mockito.spy(mFragment);

        TestUtils.setVariable(mFragment, "mDrawerCallback", null);

        doReturn(mockActivity).when(mFragment).getActivity();

        TestUtils.invokePrivateMethod(mFragment, "setupActionBar");
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
        verify(actionBar).setTitle(R.string.patient_summary_fragment_title);
        verify(actionBar).setDisplayShowHomeEnabled(true);
        verify(actionBar).setHomeButtonEnabled(true);
    }

    @Test (expected = IllegalArgumentException.class)
    public void viewHolder_nullRoot() {
        new PatientSummaryFragment.ViewHolder(null);
    }

    @Test (expected = NullPointerException.class)
    public void viewHolder_nullEverything() {
        new PatientSummaryFragment.ViewHolder(new View(ApplicationProvider.getApplicationContext()));
    }

    @Test (expected = NullPointerException.class)
    public void viewHolder_nullNot1() {
        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        final ListCard itemForReviewCardView = new ListCard(ApplicationProvider.getApplicationContext());
        doReturn(itemForReviewCardView).when(view).findViewById(R.id.patientsummary_itemforreviewcard);
        new PatientSummaryFragment.ViewHolder(view);
    }

    @Test (expected = NullPointerException.class)
    public void viewHolder_nullNot2() {
        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        final ListCard itemForReviewCardView = new ListCard(ApplicationProvider.getApplicationContext());
        doReturn(itemForReviewCardView).when(view).findViewById(R.id.patientsummary_itemforreviewcard);
        final ListCard vitalsAndMeasurementsView = new ListCard(ApplicationProvider.getApplicationContext());
        doReturn(vitalsAndMeasurementsView).when(view).findViewById(R.id.patientsummary_vitalsandmeasurements);
        new PatientSummaryFragment.ViewHolder(view);
    }

    private static PatientViewSummary createDefaultMockPatientViewSummary() {
        final PatientViewSummary summary = new PatientViewSummary();
        summary.sections = new PatientViewSummary.Sections();
        summary.sections.allergy = new PatientViewSummary.AllergySection();
        summary.sections.allergy.allergies = new ArrayList<>();
        summary.sections.allergy.sectionStates = new ArrayList<>();

        return summary;
    }

    private static PatientViewSummary.Allergy createMockAllergy(final String substance, final String importance) {
        final PatientViewSummary.Allergy allergy = new PatientViewSummary.Allergy();
        allergy.substance = substance;
        allergy.importance = importance;
        if (importance != null) {
            allergy.importance = importance.toUpperCase(Locale.US);
        } else {
            allergy.importance = null;
        }
        return allergy;
    }
}
