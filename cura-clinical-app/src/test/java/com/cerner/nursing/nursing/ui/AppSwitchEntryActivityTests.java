package com.cerner.nursing.nursing.ui;

import android.content.Intent;
import android.os.Bundle;

import com.cerner.cura.requestor.MockDataManager;
import com.cerner.cura.test.helper.TestUtils;
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

/**
 * @author Lam Tran (lt028506) on 3/25/14.
 */
@RunWith (RobolectricTestRunner.class)
public class AppSwitchEntryActivityTests {
    private ActivityController<AppSwitchEntryActivity> mActivityController;
    private AppSwitchEntryActivity mMockActivity;

    @BeforeClass
    public static void ClassSetup() {
        System.gc();
    }

    /**
     * Create mock activity from robolectric
     */
    @Before
    public void setup() {
        TestUtils.setStaticVariable(MockDataManager.class, "smState", MockDataManager.MockState.READ);
        mActivityController = Robolectric.buildActivity(AppSwitchEntryActivity.class).create();
        mMockActivity = Mockito.spy(mActivityController.get());
    }

    @After
    public void tearDown() {
        mMockActivity = null;
        mActivityController = null;
    }

    @Test
    public void onNewIntent() {
        mMockActivity.onNewIntent(Mockito.mock(Intent.class));
    }

    @Test
    public void onAuthnResume() {
        mMockActivity.onAuthnResume();
    }

    @Test
    public void init_getIntentNull() {
        mMockActivity = Mockito.spy(Robolectric.buildActivity(AppSwitchEntryActivity.class).get());
        Mockito.doReturn(null).when(mMockActivity).getIntent();
        TestUtils.invokePrivateMethod(mMockActivity, "init");
        Mockito.verify(mMockActivity).finish();
        Mockito.verify(mMockActivity, Mockito.never()).setContentView(R.layout.app_switch_entry_activity);
    }

    @Test
    public void init_getIntentActionNull() {
        mMockActivity = Mockito.spy(Robolectric.buildActivity(AppSwitchEntryActivity.class).get());
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn(intent).when(mMockActivity).getIntent();
        Mockito.doReturn(null).when(intent).getAction();
        TestUtils.invokePrivateMethod(mMockActivity, "init");
        Mockito.verify(mMockActivity).finish();
        Mockito.verify(mMockActivity, Mockito.never()).setContentView(R.layout.app_switch_entry_activity);
    }

    @Test
    public void init_getIntentExtrasNull() {
        mMockActivity = Mockito.spy(Robolectric.buildActivity(AppSwitchEntryActivity.class).get());
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn(intent).when(mMockActivity).getIntent();
        Mockito.doReturn("Action").when(intent).getAction();
        Mockito.doReturn(null).when(intent).getExtras();
        TestUtils.invokePrivateMethod(mMockActivity, "init");
        Mockito.verify(mMockActivity).finish();
        Mockito.verify(mMockActivity, Mockito.never()).setContentView(R.layout.app_switch_entry_activity);
    }

    @Test
    public void init() {
        mMockActivity = Mockito.spy(Robolectric.buildActivity(AppSwitchEntryActivity.class).get());
        Mockito.doNothing().when(mMockActivity).setContentView(Mockito.anyInt());
        final Intent intent = Mockito.mock(Intent.class);
        Mockito.doReturn(intent).when(mMockActivity).getIntent();
        Mockito.doReturn("Action").when(intent).getAction();
        Mockito.doReturn(new Bundle()).when(intent).getExtras();
        TestUtils.invokePrivateMethod(mMockActivity, "init");
        Mockito.verify(mMockActivity, Mockito.never()).finish();
        Mockito.verify(mMockActivity).setContentView(R.layout.app_switch_entry_activity);
    }
}