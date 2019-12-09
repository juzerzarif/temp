package com.cerner.nursing.nursing.ui;

import android.app.AlarmManager;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;

import com.cerner.cura.requestor.MockDataManager;
import com.cerner.cura.test.helper.TestUtils;
import com.cerner.nursing.nursing.BuildConfig;
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
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowIntent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.robolectric.Shadows.shadowOf;

/**
 * Tests {@link com.cerner.nursing.nursing.ui.PCTStartActivity}.
 *
 * @author Lam Tran (LT028056)
 */

@RunWith (RobolectricTestRunner.class)
public class PCTStartActivityTests {
    private ActivityController<PCTStartActivity> mActivityController;
    private PCTStartActivity mockActivity;

    @BeforeClass
    public static void ClassSetup() {
        System.gc();
    }

    /**
     * Build mock activity from robolectric
     */
    @Before
    public void setup() {
        TestUtils.setStaticVariable(MockDataManager.class, "smState", MockDataManager.MockState.NOTHING);
        mActivityController = Robolectric.buildActivity(PCTStartActivity.class).create().start();
        mockActivity = mActivityController.get();
    }

    /**
     * Clean out mock activity
     */
    @After
    public void tearDown() {
        mockActivity = null;
        mActivityController = null;
    }

    /**
     * Ensures the activity exists
     */
    @Test
    public void activityIsNotNull() {
        assertNotNull(mockActivity);
    }

    /**
     * Test if every component are display correct on screen
     */
    @Test
    public void activityIsVisible() {
        // test home button
        Button button = mockActivity.findViewById(R.id.homeButton);
        assertNotNull(button);
        assertEquals(View.VISIBLE, button.getVisibility());
        assertTrue(button.isClickable());

        // test remove certificate button
        button = mockActivity.findViewById(R.id.btnRemoveCert);
        assertNotNull(button);
        assertEquals(View.VISIBLE, button.getVisibility());
        assertTrue(button.isClickable());

        // test remove session button
        button = mockActivity.findViewById(R.id.btnRemoveSession);
        assertNotNull(button);
        assertEquals(View.VISIBLE, button.getVisibility());
        assertTrue(button.isClickable());

        // test write data switch
        Switch toggle = mockActivity.findViewById(R.id.writeDataSwitch);
        assertNotNull(toggle);
        assertEquals(BuildConfig.DEBUG ? View.VISIBLE : View.GONE, toggle.getVisibility());
        assertTrue(toggle.isClickable());

        // test read data switch
        toggle = mockActivity.findViewById(R.id.readDataSwitch);
        assertNotNull(toggle);
        assertEquals(BuildConfig.DEBUG ? View.VISIBLE : View.GONE, toggle.getVisibility());
        assertTrue(toggle.isClickable());
    }

    /**
     * Test if PatientListActivity is calling
     */
    @Test
    public void homeButtonNavigation() {
        mockActivity = Mockito.spy(mockActivity);
        final Button homeButton = mockActivity.findViewById(R.id.homeButton);
        assertNotNull(homeButton);

        final AlarmManager am = Mockito.mock(AlarmManager.class);
        Mockito.doReturn(am).when(mockActivity).getSystemService(Context.ALARM_SERVICE);
        Mockito.doNothing().when(am).setTimeZone(eq("America/Chicago"));

        // click on the home button
        homeButton.performClick();
        final ShadowActivity shadowActivity = shadowOf(mockActivity);
        final ShadowIntent shadowIntent = shadowOf(shadowActivity.getNextStartedActivity());
        assertEquals(PatientListActivity.class, shadowIntent.getIntentClass());
    }

    /**
     * Click both of use and write switch and ensure that only one of them is on
     */
    @Test
    public void mutuallyExclusiveSwitches() {
        final Switch writeSwitch = mockActivity.findViewById(R.id.writeDataSwitch);
        final Switch readSwitch = mockActivity.findViewById(R.id.readDataSwitch);

        // click on write data
        writeSwitch.performClick();
        assertTrue(writeSwitch.isChecked());

        // click on read data
        readSwitch.performClick();
        assertTrue(readSwitch.isChecked());

        // write data should nolonger be checked
        assertFalse(writeSwitch.isChecked());
    }

    @Test
    public void wifiSwitchClick() {
        final Switch honorWifiSwitch = mockActivity.findViewById(R.id.honorWifiSwitch);

        // click on switch
        honorWifiSwitch.performClick();
        assertTrue(honorWifiSwitch.isChecked());
        assertTrue(MockDataManager.honorWifiState());

        // click on switch again
        honorWifiSwitch.performClick();

        // switch should nolonger be checked
        assertFalse(honorWifiSwitch.isChecked());
        assertFalse(MockDataManager.honorWifiState());
    }
}