package com.cerner.nursing.nursing.base;

import com.android.volley.Request;
import com.cerner.cura.requestor.CuraRequest;
import com.cerner.cura.requestor.CuraResponseListener;
import com.cerner.cura.requestor.MockDataManager;
import com.cerner.cura.requestor.MultiResponseListener;
import com.cerner.cura.test.helper.TestUtils;
import com.cerner.nursing.nursing.ui.CareTeamActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertNull;

/**
 * @author Lam Tran (lt028506).
 */
@RunWith (RobolectricTestRunner.class)
public class CareTeamRequestQueueTests {
    @Before
    public void setup() {
        TestUtils.setStaticVariable(MockDataManager.class, "smState", MockDataManager.MockState.NOTHING);
    }

    @Test (expected = IllegalArgumentException.class)
    public void add_non_CuraRequest() {
        final CareTeamRequestQueue careTeamRequestQueue = new CareTeamRequestQueue(Mockito.mock(CareTeamActivity.class));
        careTeamRequestQueue.add(Mockito.mock(Request.class));
    }

    @Test (expected = IllegalArgumentException.class)
    public void add_CuraRequest_curaResponseListener() {
        final CareTeamRequestQueue careTeamRequestQueue = new CareTeamRequestQueue(Mockito.mock(CareTeamActivity.class));
        final CuraRequest curaRequest = Mockito.mock(CuraRequest.class);
        Mockito.doReturn(Mockito.mock(CuraResponseListener.class)).when(curaRequest).getCernResponseListener();
        careTeamRequestQueue.add(curaRequest);
        Mockito.verify(curaRequest).getCernResponseListener();
    }

    @Test
    public void add_CuraRequest_multiResponseListener() {
        final CareTeamRequestQueue careTeamRequestQueue = new CareTeamRequestQueue(Mockito.mock(CareTeamActivity.class));
        final CuraRequest curaRequest = Mockito.mock(CuraRequest.class);
        Mockito.doReturn(Mockito.mock(MultiResponseListener.class)).when(curaRequest).getCernResponseListener();
        careTeamRequestQueue.add(curaRequest);
        Mockito.verify(curaRequest, Mockito.times(2)).getCernResponseListener();
    }

    @Test
    public void add_CuraRequest_null() {
        final CareTeamRequestQueue careTeamRequestQueue = new CareTeamRequestQueue(Mockito.mock(CareTeamActivity.class));
        assertNull(careTeamRequestQueue.add(null));
    }
}
