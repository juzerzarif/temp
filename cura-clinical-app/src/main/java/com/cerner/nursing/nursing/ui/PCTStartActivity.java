package com.cerner.nursing.nursing.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import com.cerner.cura.base.UserContext;
import com.cerner.cura.requestor.MockDataManager;
import com.cerner.ion.provisioning.ProvisionedTenantStore;
import com.cerner.ion.security.IonActivity;
import com.cerner.ion.session.IonSessionUtils;
import com.cerner.ion.util.CertUtil;
import com.cerner.nursing.nursing.R;

import java.util.Locale;

/**
 * Initial activity of the app started by choosing the app in the launcher the first time.
 *
 * @author Mark Lear (ml015922)
 */
public class PCTStartActivity extends IonActivity {
    /**
     * Handle clicks to the certificate removal button.
     *
     * @param sender View button that was clicked
     */
    @SuppressWarnings ("MethodMayBeStatic")
    public void removeCertClick(final View sender) {
        ProvisionedTenantStore.removeAll();
        CertUtil.resetAll();
    }

    /**
     * Handle clicks to the Home button.
     *
     * @param sender View button that was clicked
     */
    public void homeClick(final View sender) {
        final EditText responseTimeText = findViewById(R.id.responseTimeText);

        try {
            MockDataManager.setMockTimeZone(this);
            MockDataManager.setResponseTime(Integer.valueOf(responseTimeText.getText().toString()));
        } catch (NumberFormatException | NullPointerException e) {
            MockDataManager.setResponseTime(10);
        }

        final EditText dataBranchText = findViewById(R.id.dataBranchText);
        if (dataBranchText != null) {
            MockDataManager.setMockBranch(dataBranchText.getText().toString());
        }

        final Intent intent = new Intent(PCTStartActivity.this, PatientListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Handle click to get mock data - it will download mock data into sd card of the emulator
     */
    public void getMockDataClick(final View view) {
        final Button home = findViewById(R.id.homeButton);
        home.setEnabled(false);
        final EditText dataBranchText = findViewById(R.id.dataBranchText);
        if (dataBranchText != null) {
            MockDataManager.setMockBranch(dataBranchText.getText().toString());
            MockDataManager.setMockDataStatus(MockDataManager.MockState.READ);
            ((Switch) view.getRootView().findViewById(R.id.readDataSwitch)).setChecked(true);
            ((Switch) view.getRootView().findViewById(R.id.writeDataSwitch)).setChecked(false);
            MockDataManager.getMockData(this, true, false);
        }
        home.setEnabled(true);
    }

    public void onWriteSwitchClicked(final View view) {
        // Is the toggle on?
        final boolean on = ((Switch) view).isChecked();

        if (on) {
            MockDataManager.setMockDataStatus(MockDataManager.MockState.WRITE);
            ((Switch) findViewById(R.id.readDataSwitch)).setChecked(false);
        } else {
            MockDataManager.setMockDataStatus(MockDataManager.MockState.NOTHING);
        }
    }

    public void onReadSwitchClicked(final View view) {
        // Is the toggle on?
        final boolean on = ((Switch) view).isChecked();

        if (on) {
            MockDataManager.setMockDataStatus(MockDataManager.MockState.READ);
            ((Switch) findViewById(R.id.writeDataSwitch)).setChecked(false);
        } else {
            MockDataManager.setMockDataStatus(MockDataManager.MockState.NOTHING);
        }
    }

    @SuppressWarnings ("MethodMayBeStatic")
    public void onHonorWifiSwitchClicked(final View view) {
        MockDataManager.setHonorWifiState(((Switch) view).isChecked());
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pct_start_activity);

        final Button btnRemoveSession = findViewById(R.id.btnRemoveSession);
        btnRemoveSession.setOnClickListener(view -> {
            IonSessionUtils.clear(PCTStartActivity.this);
            UserContext.clearContext();
        });

        if (MockDataManager.isMockDataAllowed()) {
            final View getMockButton = findViewById(R.id.getMockButton);
            if (getMockButton != null) {
                getMockButton.setVisibility(View.VISIBLE);
            }

            final View dataBranchLayout = findViewById(R.id.dataBranchLayout);
            if (dataBranchLayout != null) {
                dataBranchLayout.setVisibility(View.VISIBLE);
            }

            final View responseTimeLayout = findViewById(R.id.responseTimeLayout);
            if (responseTimeLayout != null) {
                responseTimeLayout.setVisibility(View.VISIBLE);
            }

            final View writeSwitchView = findViewById(R.id.writeDataSwitch);
            if (writeSwitchView != null) {
                writeSwitchView.setVisibility(View.VISIBLE);
            }

            final View readSwitchView = findViewById(R.id.readDataSwitch);
            if (readSwitchView != null) {
                readSwitchView.setVisibility(View.VISIBLE);
            }

            final View honorWifiSwitchView = findViewById(R.id.honorWifiSwitch);
            if (honorWifiSwitchView != null) {
                honorWifiSwitchView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        final EditText dataBranchText = findViewById(R.id.dataBranchText);
        if (dataBranchText != null) {
            dataBranchText.setText(MockDataManager.getMockBranch());
        }

        final EditText responseTimeText = findViewById(R.id.responseTimeText);
        if (responseTimeText != null) {
            responseTimeText.setText(String.format(Locale.getDefault(), "%d", MockDataManager.getResponseTime()));
        }

        final Switch writeSwitchView = findViewById(R.id.writeDataSwitch);
        if (writeSwitchView != null) {
            writeSwitchView.setChecked(MockDataManager.getWriteMockDataFlag());
        }

        final Switch readSwitchView = findViewById(R.id.readDataSwitch);
        if (readSwitchView != null) {
            readSwitchView.setChecked(MockDataManager.getReadMockDataFlag());
        }

        if (MockDataManager.honorWifiState()) {
            final Switch honorWifiSwitchView = findViewById(R.id.honorWifiSwitch);
            if (honorWifiSwitchView != null) {
                honorWifiSwitchView.setChecked(true);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (MockDataManager.isMockDataForced()) {
            homeClick(null);
        }
    }
}
