package com.cerner.cura.ui.elements;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.cerner.cura.datamodel.common.FuzzyDateTime;
import com.cerner.cura.datamodel.common.PatientListPatient;
import com.cerner.cura.test.helper.TestUtils;
import com.cerner.cura.utils.TimeCalculator;
import com.cerner.nursing.nursing.R;

import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import androidx.test.core.app.ApplicationProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class PatientListItemTest {
    @BeforeClass
    public static void ClassSetup() {
        System.gc();
    }

    @Test
    public void patientListItemConstructor_PatientValid() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";

        final PatientListItem patientListItem = new PatientListItem(patient);
        assertEquals(patient.nameFullFormatted, patientListItem.getTitle());
    }

    @Test (expected = NullPointerException.class)
    public void patientListItem_NullViews() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));
    }

    @Test
    public void patientListItem_PatientNoContext() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        Mockito.doReturn(null).when(view).getContext();
        final TextView alertView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(alertView).when(view).findViewById(com.cerner.nursing.nursing.R.id.alertView);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.setNameAlert(false);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));

        assertEquals(0, alertView.getVisibility());
    }

    @Test
    public void patientListItem_PatientValidBasic() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        final TextView nameView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(nameView).when(view).findViewById(com.cerner.nursing.nursing.R.id.nameView);
        final TextView alertView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(alertView).when(view).findViewById(com.cerner.nursing.nursing.R.id.alertView);
        final TextView noRelationshipView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(noRelationshipView).when(view).findViewById(com.cerner.nursing.nursing.R.id.noRelationshipView);
        final TextView ageView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(ageView).when(view).findViewById(com.cerner.nursing.nursing.R.id.ageView);
        final TextView sexView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(sexView).when(view).findViewById(R.id.sexView);
        final TextView dobAbrevView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(dobAbrevView).when(view).findViewById(R.id.dobAbrevView);
        final TextView dobView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(dobView).when(view).findViewById(R.id.dobView);
        final TextView roomBedView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(roomBedView).when(view).findViewById(R.id.roomBedView);
        final ImageView patientNotificationImageView = new ImageView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(patientNotificationImageView).when(view).findViewById(R.id.patientNotificationImageView);
        final ImageView chevron = new ImageView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(chevron).when(view).findViewById(R.id.chevron);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));

        assertEquals(patient.nameFullFormatted, nameView.getText().toString());
        assertEquals(View.GONE, alertView.getVisibility());
        assertEquals(View.VISIBLE, noRelationshipView.getVisibility());
        assertEquals(View.GONE, ageView.getVisibility());
        assertEquals(View.GONE, sexView.getVisibility());
        assertEquals(View.GONE, dobAbrevView.getVisibility());
        assertEquals(View.GONE, dobView.getVisibility());
        assertNotNull(dobView.getText());
        assertEquals("", roomBedView.getText());
        assertNull(patientNotificationImageView.getDrawable());
        assertEquals(View.VISIBLE, chevron.getVisibility());
    }

    @Test (expected = NullPointerException.class)
    public void patientListItem_PatientNullNameView() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        Mockito.doReturn(null).when(view).findViewById(com.cerner.nursing.nursing.R.id.nameView);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));
    }

    @Test (expected = NullPointerException.class)
    public void patientListItem_PatientNullAlertView() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        Mockito.doReturn(null).when(view).findViewById(com.cerner.nursing.nursing.R.id.alertView);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));
    }

    @Test (expected = NullPointerException.class)
    public void patientListItem_PatientNullNoRelationshipView() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        Mockito.doReturn(null).when(view).findViewById(com.cerner.nursing.nursing.R.id.noRelationshipView);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));
    }

    @Test (expected = NullPointerException.class)
    public void patientListItem_PatientNullAgeView() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        Mockito.doReturn(null).when(view).findViewById(com.cerner.nursing.nursing.R.id.ageView);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));
    }

    @Test (expected = NullPointerException.class)
    public void patientListItem_PatientNullSexView() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        Mockito.doReturn(null).when(view).findViewById(R.id.sexView);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));
    }

    @Test (expected = NullPointerException.class)
    public void patientListItem_PatientNullDobAbrevView() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        Mockito.doReturn(null).when(view).findViewById(R.id.dobAbrevView);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));
    }

    @Test (expected = NullPointerException.class)
    public void patientListItem_PatientNullDobView() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        Mockito.doReturn(null).when(view).findViewById(R.id.dobView);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));
    }

    @Test (expected = NullPointerException.class)
    public void patientListItem_PatientNullRoomBedView() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        Mockito.doReturn(null).when(view).findViewById(R.id.roomBedView);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));
    }

    @Test (expected = NullPointerException.class)
    public void patientListItem_PatientNullNotificationImageView() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        Mockito.doReturn(null).when(view).findViewById(R.id.patientNotificationImageView);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));
    }

    @Test (expected = NullPointerException.class)
    public void patientListItem_PatientNullChevronView() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        Mockito.doReturn(null).when(view).findViewById(R.id.chevron);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));
    }

    @Test
    public void patientListItem_PatientValidDOB() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";
        patient.dateOfBirth = new FuzzyDateTime();
        patient.dateOfBirth.dateTime = DateTime.now().minus(3650);
        patient.dateOfBirth.precision = FuzzyDateTime.PRECISION_FULL;
        final String dobResult = TimeCalculator.getDateString(ApplicationProvider.getApplicationContext(), patient.dateOfBirth.dateTime, patient.dateOfBirth.precision);

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        final TextView dobView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(dobView).when(view).findViewById(R.id.dobView);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));

        assertEquals(View.GONE, dobView.getVisibility());
        assertEquals(dobResult, dobView.getText().toString());
    }

    @Test
    public void patientListItem_PatientValidDOB_DiffTimeZone() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";
        patient.dateOfBirth = new FuzzyDateTime();
        patient.dateOfBirth.dateTime = DateTime.now().minus(3650);
        patient.dateOfBirth.precision = FuzzyDateTime.PRECISION_FULL;
        patient.dateOfBirth.originalTimeZone = "+12:00";
        final String dobResult = TimeCalculator.getDateString(ApplicationProvider.getApplicationContext(), patient.dateOfBirth.dateTime, "+12:00", patient.dateOfBirth.precision);

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        final TextView dobView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(dobView).when(view).findViewById(R.id.dobView);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));

        assertEquals(View.GONE, dobView.getVisibility());
        assertEquals(dobResult, dobView.getText().toString());
    }

    @Test
    public void patientListItem_PatientInValidDOB() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";
        patient.dateOfBirth = new FuzzyDateTime();
        patient.dateOfBirth.dateTime = DateTime.now().minus(3650);
        patient.dateOfBirth.precision = FuzzyDateTime.PRECISION_FULL;
        final String dobResult = TimeCalculator.getDateString(ApplicationProvider.getApplicationContext(), patient.dateOfBirth.dateTime, patient.dateOfBirth.precision);

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        final TextView dobView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(dobView).when(view).findViewById(R.id.dobView);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));

        assertEquals(View.GONE, dobView.getVisibility());
        assertEquals(dobResult, dobView.getText().toString());
    }

    @Test
    public void patientListItem_PatientValidNameAlert() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        final TextView nameView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(nameView).when(view).findViewById(com.cerner.nursing.nursing.R.id.nameView);
        final TextView alertView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(alertView).when(view).findViewById(com.cerner.nursing.nursing.R.id.alertView);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.setNameAlert(true);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));

        assertEquals("*" + patient.nameFullFormatted, nameView.getText().toString());
        assertEquals(View.VISIBLE, alertView.getVisibility());
    }

    @Test
    public void patientListItem_PatientValidRelationShipInd() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";
        patient.relationshipInd = true;
        patient.ageDisplay = "25";
        patient.sexAbbr = "F";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        final TextView ageView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(ageView).when(view).findViewById(com.cerner.nursing.nursing.R.id.ageView);
        final TextView sexView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(sexView).when(view).findViewById(R.id.sexView);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));

        assertEquals(patient.ageDisplay, ageView.getText().toString());
        assertEquals(View.VISIBLE, ageView.getVisibility());
        assertEquals(patient.sexAbbr, sexView.getText().toString());
        assertEquals(View.VISIBLE, sexView.getVisibility());
    }

    @Test
    public void patientListItem_PatientValidRelationShipIndNoAgeNoSex() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";
        patient.relationshipInd = true;
        patient.ageDisplay = "";
        patient.sexAbbr = "";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        final TextView ageView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(ageView).when(view).findViewById(com.cerner.nursing.nursing.R.id.ageView);
        final TextView sexView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(sexView).when(view).findViewById(R.id.sexView);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));

        assertEquals(patient.ageDisplay, ageView.getText().toString());
        assertEquals(View.GONE, ageView.getVisibility());
        assertEquals(patient.sexAbbr, sexView.getText().toString());
        assertEquals(View.GONE, sexView.getVisibility());
    }

    @Test
    public void patientListItem_PatientValidRoomLocationDisplay() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";
        patient.roomLocationDisplay = "Hobbiton";
        patient.bedLocationDisplay = "PrancingPony";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        final TextView roomBedView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(roomBedView).when(view).findViewById(R.id.roomBedView);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));

        assertEquals(patient.roomLocationDisplay + " - " + patient.bedLocationDisplay, roomBedView.getText());
    }

    @Test
    public void patientListItem_PatientValidRoomLocationDisplayEmpty() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";
        patient.roomLocationDisplay = "";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        final TextView roomBedView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(roomBedView).when(view).findViewById(R.id.roomBedView);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));

        assertEquals("", roomBedView.getText());
    }

    @Test
    public void patientListItem_PatientValidBedLocationDisplayEmpty() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";
        patient.roomLocationDisplay = "Hobbiton";
        patient.bedLocationDisplay = "";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        final TextView roomBedView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(roomBedView).when(view).findViewById(R.id.roomBedView);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));

        assertEquals(patient.roomLocationDisplay, roomBedView.getText());
    }

    @Test
    public void patientListItem_PatientValidRoomDisplay() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";
        patient.roomLocationDisplay = "Hobbiton";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        final TextView roomBedView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(roomBedView).when(view).findViewById(R.id.roomBedView);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));

        assertEquals(patient.roomLocationDisplay, roomBedView.getText());
    }

    @Test
    public void patientListItem_PatientValidBedDisplay() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";
        patient.bedLocationDisplay = "PrancingPony";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        final TextView roomBedView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(roomBedView).when(view).findViewById(R.id.roomBedView);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));

        assertEquals(patient.bedLocationDisplay, roomBedView.getText());
    }

    @Test
    public void patientListItem_PatientValidBedLocationDisplayEmptyRoomLocationDisplayEmpty() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";
        patient.roomLocationDisplay = "";
        patient.bedLocationDisplay = "";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        final TextView roomBedView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(roomBedView).when(view).findViewById(R.id.roomBedView);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));

        assertEquals(patient.roomLocationDisplay, roomBedView.getText());
    }

    @Test
    public void patientListItem_PatientValidCriticalReviewInd() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";
        patient.reviewIndicator = 2;

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        final ImageView patientNotificationImageView = new ImageView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(patientNotificationImageView).when(view).findViewById(R.id.patientNotificationImageView);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));

        assertEquals(R.drawable.icon_critical_alert, shadowOf(patientNotificationImageView.getDrawable()).getCreatedFromResId());
    }

    @Test
    public void patientListItem_PatientValidNonCriticalReviewInd() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";
        patient.reviewIndicator = 1;

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        final ImageView patientNotificationImageView = new ImageView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(patientNotificationImageView).when(view).findViewById(R.id.patientNotificationImageView);

        final PatientListItem patientListItem = new PatientListItem(patient);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));

        assertEquals(R.drawable.icon_non_critical_alert, shadowOf(patientNotificationImageView.getDrawable()).getCreatedFromResId());
    }

    @Test
    public void patientListItem_CreateViewHolder() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        final LayoutInflater inflater = Mockito.mock(LayoutInflater.class);
        Mockito.doReturn(view).when(inflater).inflate(Mockito.anyInt(), Mockito.any(), Mockito.anyBoolean());

        final PatientListItem patientListItem = new PatientListItem(patient);
        final IItem.ViewHolder viewHolder = patientListItem.createViewHolder(inflater, null);

        assertNotNull(viewHolder);
        assertEquals(view, viewHolder.itemView);
    }

    @Test
    public void patientListItem_HideChevronNotClickable() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        final ImageView chevron = new ImageView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(chevron).when(view).findViewById(R.id.chevron);

        final PatientListItem patientListItem = new PatientListItem(patient);
        TestUtils.setVariable(patientListItem, "mShowChevron", false);
        TestUtils.setVariable(patientListItem, "mIsItemClickable", false);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));

        assertEquals(View.GONE, chevron.getVisibility());
    }

    @Test
    public void patientListItem_ShowChevronNotClickable() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        final ImageView chevron = new ImageView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(chevron).when(view).findViewById(R.id.chevron);

        final PatientListItem patientListItem = new PatientListItem(patient);
        TestUtils.setVariable(patientListItem, "mIsItemClickable", false);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));

        assertEquals(View.GONE, chevron.getVisibility());
    }

    @Test
    public void patientListItem_HideChevronClickable() {
        final PatientListPatient patient = new PatientListPatient();
        patient.nameFullFormatted = "Donkey, Bill";

        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        mockViews(view);
        final ImageView chevron = new ImageView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(chevron).when(view).findViewById(R.id.chevron);

        final PatientListItem patientListItem = new PatientListItem(patient);
        TestUtils.setVariable(patientListItem, "mShowChevron", false);
        patientListItem.bindViewHolder(new PatientListItem.ViewHolder(view));

        assertEquals(View.GONE, chevron.getVisibility());
    }

    //Helper method to mock views
    private static void mockViews(final View view) {
        final TextView nameView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(nameView).when(view).findViewById(com.cerner.nursing.nursing.R.id.nameView);
        final TextView alertView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(alertView).when(view).findViewById(com.cerner.nursing.nursing.R.id.alertView);
        final TextView noRelationshipView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(noRelationshipView).when(view).findViewById(com.cerner.nursing.nursing.R.id.noRelationshipView);
        final TextView ageView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(ageView).when(view).findViewById(com.cerner.nursing.nursing.R.id.ageView);
        final TextView sexView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(sexView).when(view).findViewById(R.id.sexView);
        final TextView dobAbrevView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(dobAbrevView).when(view).findViewById(R.id.dobAbrevView);
        final TextView dobView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(dobView).when(view).findViewById(R.id.dobView);
        final TextView roomBedView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(roomBedView).when(view).findViewById(R.id.roomBedView);
        final ImageView patientNotificationImageView = new ImageView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(patientNotificationImageView).when(view).findViewById(R.id.patientNotificationImageView);
        final ImageView chevron = new ImageView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(chevron).when(view).findViewById(R.id.chevron);
    }
}