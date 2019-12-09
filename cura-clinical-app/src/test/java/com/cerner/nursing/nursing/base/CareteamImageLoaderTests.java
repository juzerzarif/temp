package com.cerner.nursing.nursing.base;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertNotNull;

/**
 * @author Lam Tran (lt028506).
 */
@RunWith (RobolectricTestRunner.class)
public class CareteamImageLoaderTests {
    @Test
    public void createRequest() {
        assertNotNull(new CareTeamImageLoader(null).createRequest("/url", null, 0, 0, null, null));
    }
}
