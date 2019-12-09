package com.cerner.nursing.nursing.ui;

import android.content.Intent;
import android.os.Bundle;

import com.cerner.cura.base.CuraAuthnActivity;
import com.cerner.nursing.nursing.R;
import com.cerner.nursing.nursing.utils.AppSwitchUtils;

/**
 * @author Lam Tran (lt028506)
 * @author Brad Barnhill (bb024928)
 * @author Mark Lear (ML015922)
 */
public class AppSwitchEntryActivity extends CuraAuthnActivity {
    @Override
    public void onResponseReceived(final Class clazz) {
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    private void init() {
        if (getIntent() == null || getIntent().getAction() == null || getIntent().getExtras() == null) {
            finish();
            return;
        }

        setActiveModuleOnResume(false);
        AppSwitchUtils.performAppSwitch(this, this, getIntent());
        setContentView(R.layout.app_switch_entry_activity);
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        AppSwitchUtils.performAppSwitch(this, this, intent);
    }

    @Override
    public void onAuthnResume() {
        super.onAuthnResume();
        AppSwitchUtils.performAppSwitch(this, this, null);
    }
}