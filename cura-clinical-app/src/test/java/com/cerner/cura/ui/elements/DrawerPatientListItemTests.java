package com.cerner.cura.ui.elements;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cerner.cura.base.UserContext;
import com.cerner.cura.datamodel.common.FuzzyDateTime;
import com.cerner.cura.demographics.datamodel.PatientDemogBanner;
import com.cerner.cura.test.helper.TestUtils;
import com.cerner.cura.utils.TimeCalculator;
import com.cerner.nursing.nursing.R;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import androidx.test.core.app.ApplicationProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Sandeep Kuturu  (SK028413) on 4/10/14.
 * @author Mark Lear (ML015922)
 */
@RunWith (RobolectricTestRunner.class)
public class DrawerPatientListItemTests {
    @BeforeClass
    public static void ClassSetup() {
        System.gc();
    }

    @After
    public void tearDown() {
        UserContext.removeContextStorageObject(TestUtils.getStaticVariable(DrawerPatientListItem.class, "PATIENT_MODULES_EXPANDED_STORAGE_ID"));
    }

    @Test
    public void drawerPatientListItemConstructor_PatientValid_ExpandedSet() {
        final PatientDemogBanner patient = new PatientDemogBanner();
        patient.name_full_formatted = "Full_Name_Formatted";

        UserContext.putContextStorageObject(TestUtils.getStaticVariable(DrawerPatientListItem.class, "PATIENT_MODULES_EXPANDED_STORAGE_ID"), false);

        final DrawerPatientListItem drawerPatientListItem = new DrawerPatientListItem(patient);
        assertEquals("Full_Name_Formatted", drawerPatientListItem.getTitle());
        assertFalse(drawerPatientListItem.isExpanded());
    }

    @Test
    public void drawerPatientListItemConstructor_PatientNull() {
        final DrawerPatientListItem drawerPatientListItem = new DrawerPatientListItem(null);
        assertNull(drawerPatientListItem.getTitle());
    }

    @Test
    public void onClick_currentlyExpanded() {
        final String expandedKey = TestUtils.getStaticVariable(DrawerPatientListItem.class, "PATIENT_MODULES_EXPANDED_STORAGE_ID");
        UserContext.removeContextStorageObject(expandedKey);
        final DrawerPatientListItem drawerPatientListItem = new DrawerPatientListItem(null);
        drawerPatientListItem.onClick();
        assertFalse(drawerPatientListItem.isExpanded());
        assertFalse(UserContext.getContextStorageObject(expandedKey, boolean.class));
    }

    @Test
    public void onClick_currentlyCollapsed() {
        final String expandedKey = TestUtils.getStaticVariable(DrawerPatientListItem.class, "PATIENT_MODULES_EXPANDED_STORAGE_ID");
        UserContext.putContextStorageObject(expandedKey, false);
        final DrawerPatientListItem drawerPatientListItem = new DrawerPatientListItem(null);
        drawerPatientListItem.onClick();
        assertTrue(drawerPatientListItem.isExpanded());
        assertTrue(UserContext.getContextStorageObject(expandedKey, boolean.class));
    }

    @Test
    public void createViewHolder() {
        final DrawerPatientListItem item = new DrawerPatientListItem(null);
        final LayoutInflater inflater = Mockito.mock(LayoutInflater.class);
        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        final TextView nameView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(nameView).when(view).findViewById(R.id.nameView);
        final TextView ageView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(ageView).when(view).findViewById(R.id.ageView);
        final TextView sexView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(sexView).when(view).findViewById(R.id.sexView);
        final TextView dobView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(dobView).when(view).findViewById(R.id.dobView);
        final LinearLayout patientInfoView = new LinearLayout(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(patientInfoView).when(view).findViewById(R.id.patientInfoView);
        final ImageView dropDownInd = new ImageView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(dropDownInd).when(view).findViewById(R.id.dropDownInd);

        Mockito.doReturn(view).when(inflater).inflate(Mockito.anyInt(), Mockito.any(), Mockito.anyBoolean());

        final IItem.ViewHolder viewHolder = item.createViewHolder(inflater, null);
        assertNotNull(viewHolder);
        assertEquals(view, viewHolder.itemView);
    }

    @Test (expected = NullPointerException.class)
    public void createViewHolder_inflateNull() {
        final DrawerPatientListItem item = new DrawerPatientListItem(null);
        final LayoutInflater inflater = Mockito.mock(LayoutInflater.class);
        Mockito.doReturn(null).when(inflater).inflate(Mockito.anyInt(), Mockito.any(), Mockito.anyBoolean());
        item.createViewHolder(inflater, null);
    }

    @Test
    public void bindViewHolder_nulls() {
        final PatientDemogBanner patientDemogBanner = new PatientDemogBanner();
        patientDemogBanner.name_full_formatted = "Full formatted name";
        final DrawerPatientListItem item = Mockito.spy(new DrawerPatientListItem(patientDemogBanner));
        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        final TextView nameView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(nameView).when(view).findViewById(R.id.nameView);
        final TextView ageView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(ageView).when(view).findViewById(R.id.ageView);
        final TextView sexView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(sexView).when(view).findViewById(R.id.sexView);
        final TextView dobView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(dobView).when(view).findViewById(R.id.dobView);
        final LinearLayout patientInfoView = new LinearLayout(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(patientInfoView).when(view).findViewById(R.id.patientInfoView);
        final ImageView dropDownInd = new ImageView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(dropDownInd).when(view).findViewById(R.id.dropDownInd);

        Mockito.doReturn(null).doCallRealMethod().when(view).getContext();
        item.bindViewHolder(new DrawerPatientListItem.ViewHolder(view));
        Mockito.verify(item, Mockito.never()).getData();
    }

    @Test
    public void bindViewHolder_noPatient() {
        final DrawerPatientListItem item = new DrawerPatientListItem(null);
        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        final TextView nameView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(nameView).when(view).findViewById(R.id.nameView);
        final TextView ageView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(ageView).when(view).findViewById(R.id.ageView);
        final TextView sexView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(sexView).when(view).findViewById(R.id.sexView);
        final TextView dobView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(dobView).when(view).findViewById(R.id.dobView);
        final LinearLayout patientInfoView = new LinearLayout(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(patientInfoView).when(view).findViewById(R.id.patientInfoView);
        final ImageView dropDownInd = new ImageView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(dropDownInd).when(view).findViewById(R.id.dropDownInd);

        item.bindViewHolder(new DrawerPatientListItem.ViewHolder(view));
        assertEquals(ApplicationProvider.getApplicationContext().getString(R.string.patient_not_selected), nameView.getText().toString());
        assertEquals(0.5f, nameView.getAlpha(), 0.0);
        assertEquals(View.GONE, patientInfoView.getVisibility());
    }

    @Test
    public void bindViewHolder_emptyPatient() {
        final PatientDemogBanner patientDemogBanner = new PatientDemogBanner();
        final DrawerPatientListItem item = new DrawerPatientListItem(patientDemogBanner);
        item.setExpanded(false);
        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        final TextView nameView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(nameView).when(view).findViewById(R.id.nameView);
        final TextView ageView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(ageView).when(view).findViewById(R.id.ageView);
        final TextView sexView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(sexView).when(view).findViewById(R.id.sexView);
        final TextView dobView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(dobView).when(view).findViewById(R.id.dobView);
        final LinearLayout patientInfoView = new LinearLayout(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(patientInfoView).when(view).findViewById(R.id.patientInfoView);
        final ImageView dropDownInd = new ImageView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(dropDownInd).when(view).findViewById(R.id.dropDownInd);

        item.bindViewHolder(new DrawerPatientListItem.ViewHolder(view));
        assertEquals(View.VISIBLE, patientInfoView.getVisibility());
        assertEquals(1.0f, nameView.getAlpha(), 0.0);
        assertEquals("", nameView.getText().toString());
        assertEquals("--", ageView.getText().toString());
        assertEquals("", sexView.getText().toString());
        assertEquals("--", dobView.getText().toString());
    }

    @Test
    public void bindViewHolder_validPatient() {
        final PatientDemogBanner patientDemogBanner = new PatientDemogBanner();
        patientDemogBanner.name_full_formatted = "primary, patient";
        patientDemogBanner.gender_display = "S";
        patientDemogBanner.age_display = "9 years";

        patientDemogBanner.birth_date = new FuzzyDateTime();
        patientDemogBanner.birth_date.dateTime = DateTime.now().minus(3650);
        patientDemogBanner.birth_date.precision = FuzzyDateTime.PRECISION_FULL;
        final String dobResult = TimeCalculator.getDateString(ApplicationProvider.getApplicationContext(), patientDemogBanner.birth_date.dateTime, patientDemogBanner.birth_date.precision);

        final DrawerPatientListItem item = new DrawerPatientListItem(patientDemogBanner);
        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        final TextView nameView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(nameView).when(view).findViewById(R.id.nameView);
        final TextView ageView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(ageView).when(view).findViewById(R.id.ageView);
        final TextView sexView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(sexView).when(view).findViewById(R.id.sexView);
        final TextView dobView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(dobView).when(view).findViewById(R.id.dobView);
        final LinearLayout patientInfoView = new LinearLayout(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(patientInfoView).when(view).findViewById(R.id.patientInfoView);
        final ImageView dropDownInd = new ImageView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(dropDownInd).when(view).findViewById(R.id.dropDownInd);

        item.bindViewHolder(new DrawerPatientListItem.ViewHolder(view));
        assertEquals(View.VISIBLE, patientInfoView.getVisibility());
        assertEquals(1.0f, nameView.getAlpha(), 0.0);
        assertEquals("primary, patient", nameView.getText().toString());
        assertEquals("9 years", ageView.getText().toString());
        assertEquals("S", sexView.getText().toString());
        assertEquals(dobResult, dobView.getText().toString());
    }

    @Test
    public void bindViewHolder_validPatient_DiffTimeZone() {
        final PatientDemogBanner patientDemogBanner = new PatientDemogBanner();
        patientDemogBanner.name_full_formatted = "primary, patient";
        patientDemogBanner.gender_display = "S";
        patientDemogBanner.age_display = "9 years";

        patientDemogBanner.birth_date = new FuzzyDateTime();
        patientDemogBanner.birth_date.dateTime = DateTime.now().minus(3650);
        patientDemogBanner.birth_date.precision = FuzzyDateTime.PRECISION_FULL;
        patientDemogBanner.birth_date.originalTimeZone = "+12:00";
        final String dobResult = TimeCalculator.getDateString(ApplicationProvider.getApplicationContext(), patientDemogBanner.birth_date.dateTime, "+12:00", patientDemogBanner.birth_date.precision);

        final DrawerPatientListItem item = new DrawerPatientListItem(patientDemogBanner);
        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        final TextView nameView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(nameView).when(view).findViewById(R.id.nameView);
        final TextView ageView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(ageView).when(view).findViewById(R.id.ageView);
        final TextView sexView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(sexView).when(view).findViewById(R.id.sexView);
        final TextView dobView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(dobView).when(view).findViewById(R.id.dobView);
        final LinearLayout patientInfoView = new LinearLayout(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(patientInfoView).when(view).findViewById(R.id.patientInfoView);
        final ImageView dropDownInd = new ImageView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(dropDownInd).when(view).findViewById(R.id.dropDownInd);

        item.bindViewHolder(new DrawerPatientListItem.ViewHolder(view));
        assertEquals(View.VISIBLE, patientInfoView.getVisibility());
        assertEquals(1.0f, nameView.getAlpha(), 0.0);
        assertEquals("primary, patient", nameView.getText().toString());
        assertEquals("9 years", ageView.getText().toString());
        assertEquals("S", sexView.getText().toString());
        assertEquals(dobResult, dobView.getText().toString());
    }

    @Test (expected = IllegalArgumentException.class)
    public void viewHolder_nullRoot() {
        new DrawerPatientListItem.ViewHolder(null);
    }

    @Test (expected = NullPointerException.class)
    public void viewHolder_nullEverything() {
        new DrawerPatientListItem.ViewHolder(new View(ApplicationProvider.getApplicationContext()));
    }

    @Test (expected = NullPointerException.class)
    public void viewHolder_nullNot1() {
        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        final TextView nameView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(nameView).when(view).findViewById(R.id.nameView);
        new DrawerPatientListItem.ViewHolder(view);
    }

    @Test (expected = NullPointerException.class)
    public void viewHolder_nullNot2() {
        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        final TextView nameView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(nameView).when(view).findViewById(R.id.nameView);
        final TextView ageView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(ageView).when(view).findViewById(R.id.ageView);
        new DrawerPatientListItem.ViewHolder(view);
    }

    @Test (expected = NullPointerException.class)
    public void viewHolder_nullNot3() {
        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        final TextView nameView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(nameView).when(view).findViewById(R.id.nameView);
        final TextView ageView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(ageView).when(view).findViewById(R.id.ageView);
        final TextView sexView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(sexView).when(view).findViewById(R.id.sexView);
        new DrawerPatientListItem.ViewHolder(view);
    }

    @Test (expected = NullPointerException.class)
    public void viewHolder_nullNot4() {
        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        final TextView nameView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(nameView).when(view).findViewById(R.id.nameView);
        final TextView ageView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(ageView).when(view).findViewById(R.id.ageView);
        final TextView sexView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(sexView).when(view).findViewById(R.id.sexView);
        final TextView dobView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(dobView).when(view).findViewById(R.id.dobView);
        new DrawerPatientListItem.ViewHolder(view);
    }

    @Test (expected = NullPointerException.class)
    public void viewHolder_nullNot5() {
        final View view = Mockito.spy(new View(ApplicationProvider.getApplicationContext()));
        final TextView nameView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(nameView).when(view).findViewById(R.id.nameView);
        final TextView ageView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(ageView).when(view).findViewById(R.id.ageView);
        final TextView sexView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(sexView).when(view).findViewById(R.id.sexView);
        final TextView dobView = new TextView(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(dobView).when(view).findViewById(R.id.dobView);
        final LinearLayout patientInfoView = new LinearLayout(ApplicationProvider.getApplicationContext());
        Mockito.doReturn(patientInfoView).when(view).findViewById(R.id.patientInfoView);
        new DrawerPatientListItem.ViewHolder(view);
    }

    @Test
    public void setData_ModelPatientDemogBanner() {
        final PatientDemogBanner patient = new PatientDemogBanner();
        patient.name_full_formatted = "Full_Name_Formatted";
        patient.gender_display = "M";

        final DrawerPatientListItem drawerPatientListItem = new DrawerPatientListItem(patient);
        drawerPatientListItem.setData(patient);

        assertEquals("Full_Name_Formatted", drawerPatientListItem.getTitle());
        assertEquals(patient, drawerPatientListItem.getData());
    }

    @Test
    public void setData_ModelNull() {
        final DrawerPatientListItem drawerPatientListItem = new DrawerPatientListItem(null);

        drawerPatientListItem.setData(null);
        assertNull(drawerPatientListItem.getTitle());
        assertNull(drawerPatientListItem.getData());
    }
}
