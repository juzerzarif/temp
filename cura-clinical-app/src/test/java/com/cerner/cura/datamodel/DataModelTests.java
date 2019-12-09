package com.cerner.cura.datamodel;

import com.android.volley.Request;
import com.cerner.cura.datamodel.common.PatientListPatient;
import com.cerner.cura.test.helper.TestUtils;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test all of the Datamodels so they don't skew our numbers
 *
 * @author Mark Lear (ML015922)
 */
@RunWith (RobolectricTestRunner.class)
public class DataModelTests {
    @BeforeClass
    public static void ClassSetup() {
        System.gc();
    }

    @Test
    public void enumReviewIndicator() {
        TestUtils.verifyEnumStatics(PatientListPatient.REVIEW_INDICATOR.class);
    }

    @Test
    public void availableOrgs() throws InstantiationException, IllegalAccessException {
        testModel(AvailableOrgs.class);
    }

    @Test
    public void orgForDevice() throws InstantiationException, IllegalAccessException {
        testModel(OrgForDevice.class);
    }

    @Test
    public void patientList() throws InstantiationException, IllegalAccessException {
        testModel(PatientList.class);
    }

    @Test
    public void patientListById() throws InstantiationException, IllegalAccessException {
        testModel(PatientListById.class);
    }

    @Test
    public void patientLists() throws InstantiationException, IllegalAccessException {
        testModel(PatientLists.class);
    }

    @Test
    public void patientViewSummary() throws InstantiationException, IllegalAccessException {
        testModel(PatientViewSummary.class);
    }

    @Test
    public void personnelInfoSummary() throws InstantiationException, IllegalAccessException {
        testModel(PersonnelInfoSummary.class);
    }

    @Test
    public void personnelPhoto() throws InstantiationException, IllegalAccessException {
        testModel(PersonnelPhoto.class);
    }

    @Test
    public void storedOrgRelation() throws InstantiationException, IllegalAccessException {
        testModel(StoredOrgRelation.class);
    }

    @Test
    public void careTeamPhotoRequest() {
        final CareTeamPhotoRequest careTeamPhotoRequest = new CareTeamPhotoRequest("/personnel/aaa/photo");
        assertEquals("/personnel/aaa/photo", careTeamPhotoRequest.getRequestURL());

        final int method = careTeamPhotoRequest.getRequestMethod();
        assertTrue(method > Request.Method.DEPRECATED_GET_OR_POST && method < Request.Method.DELETE);
    }

    @Test
    public void common() {
        new AvailableOrgs.Org();
        new PatientLists.PatientListInfo();
        new PatientViewSummary.Allergy();
        new PatientViewSummary.AllergySection();
        new PatientViewSummary.DateResult();
        new PatientViewSummary.ItemsForReviewCountSection();
        new PatientViewSummary.LabResultGroup();
        new PatientViewSummary.LabResultsSection();
        new PatientViewSummary.MetricImperialValue();
        new PatientViewSummary.Sections();
        new PatientViewSummary.StringResult();

        new CareTeamPhotoResponse();
        new PatientListPatient();
    }

    private static void testModel(final Class<? extends IRemoteDataModel> modelClass) throws IllegalAccessException, InstantiationException {
        final IRemoteDataModel model = modelClass.newInstance();

        final String url = model.getRequestURL();
        assertNotNull(url);
        assertFalse(url.isEmpty());
        assertTrue(url.startsWith("/"));

        final int method = model.getRequestMethod();
        assertTrue(method > Request.Method.DEPRECATED_GET_OR_POST && method < Request.Method.DELETE);

        assertNotNull(model.getCacheBucket());
    }
}
