package com.neurosdk.brainbit.demo;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {
    private DemoModeFragment _demoModeFragment;
    private DevSearchFragment _deDevSearchFragment;
    private DeviceInfoFragment _deviceInfoFragment;
    private SignalDemoFragment _signalDemoFragment;
    private ResistanceDemoFragment _resistanceDemoFragment;
    private EegDemoFragment _eegDemoFragment;
    private EegIndexDemoFragment _eegIndexDemoFragment;
    private MeditationDemoFragment _meditationDemoFragment;
    private SpectrumDemoFragment _spectrumDemoFragment;
    private SpectrumPowerDemoFragment _spectrumPowerDemoFragment;

    private TextView txtDevState;
    private TextView txtDevBatteryPower;
    private DemoMode _demoMode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        txtDevState = findViewById(R.id.txt_dev_state);
        txtDevBatteryPower = findViewById(R.id.txt_dev_battery_power);

        DevHolder.inst().init(this);
        DevHolder.inst().addCallback(new DevHolder.IDeviceHolderCallback() {
            @Override
            public void batteryChanged(int val) {
                txtDevBatteryPower.setText(getString(R.string.dev_power_prc, val));
            }

            @Override
            public void deviceState(boolean state) {
                if (state) {
                    txtDevState.setText(R.string.dev_state_connected);
                } else {
                    txtDevState.setText(R.string.dev_state_disconnected);
                    txtDevBatteryPower.setText(R.string.dev_power_empty);
                }
                updateContentFragment(state);
            }
        });
        showDemoMode();
    }

    private void updateDemoModeButton(boolean connectionState) {
        if (!connectionState) {
            if (_demoModeFragment == null || _demoMode != DemoMode.START)
                return;
            _demoModeFragment.updateButtonState();
        }
    }

    private void updateContentFragment(boolean connectionState) {
        updateDemoModeButton(connectionState);
        if (connectionState || _demoMode != DemoMode.DEV_SEARCH)
            showDemoMode();
    }

    private void stopProcess() {
        FragmentManager fm = getSupportFragmentManager();
        for (Fragment it : fm.getFragments()) {
            if (it.getClass().isAssignableFrom(IBrainbitFragment.class)) {
                ((IBrainbitFragment) it).stopProcess();
            }
        }
    }

    private void hideBackButton() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(null);
    }

    private void showBackButton() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void showDemoMode() {
        stopProcess();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        if (_demoModeFragment == null) {
            _demoModeFragment = DemoModeFragment.newInstance();
            _demoModeFragment.setCallback(new DemoModeFragment.IDemoModeCallback() {
                @Override
                public void modeDevSearch() {
                    showDevSearch();
                }

                @Override
                public void modeDevInfo() {
                    showDevInfo();
                }

                @Override
                public void modeSignalDemo() {
                    showSignalDemoMode();
                }

                @Override
                public void modeResistanceDemo() {
                    showResistanceDemoMode();
                }

                @Override
                public void modeEEGDemo() {
                    showEEGDemoMode();
                }

                @Override
                public void modeEEGIndexDemo() {
                    showEEGIndexDemoMode();
                }

                @Override
                public void modeMeditationDemo() {
                    showMeditationDemoMode();
                }

                @Override
                public void modeSpectrumDemo() {
                    showSpectrumDemoMode();
                }

                @Override
                public void modeSpectrumPowerDemo() {
                    showSpectrumPowerDemoMode();
                }
            });
        }
        ft.replace(R.id.container, _demoModeFragment);
        ft.commit();
        _demoMode = DemoMode.START;
        hideBackButton();
    }

    private void showDevSearch() {
        stopProcess();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        if (_deDevSearchFragment == null) {
            _deDevSearchFragment = DevSearchFragment.newInstance();
        }
        ft.replace(R.id.container, _deDevSearchFragment);
        ft.commit();
        _demoMode = DemoMode.DEV_SEARCH;
        showBackButton();
    }

    private void showDevInfo() {
        stopProcess();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        if (_deviceInfoFragment == null) {
            _deviceInfoFragment = DeviceInfoFragment.newInstance();
        }
        ft.replace(R.id.container, _deviceInfoFragment);
        ft.commit();
        _demoMode = DemoMode.DEV_INF;
        showBackButton();
    }

    private void showSignalDemoMode() {
        stopProcess();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        if (_signalDemoFragment == null) {
            _signalDemoFragment = SignalDemoFragment.newInstance();
        }
        ft.replace(R.id.container, _signalDemoFragment);
        ft.commit();
        _demoMode = DemoMode.SIGNAL_MODE;
        showBackButton();
    }

    private void showResistanceDemoMode() {
        stopProcess();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        if (_resistanceDemoFragment == null) {
            _resistanceDemoFragment = ResistanceDemoFragment.newInstance();
        }
        ft.replace(R.id.container, _resistanceDemoFragment);
        ft.commit();
        _demoMode = DemoMode.RESISTANCE_MODE;
        showBackButton();
    }

    private void showEEGDemoMode() {
        stopProcess();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        if (_eegDemoFragment == null) {
            _eegDemoFragment = EegDemoFragment.newInstance();
        }
        ft.replace(R.id.container, _eegDemoFragment);
        ft.commit();
        _demoMode = DemoMode.EEG_MODE;
        showBackButton();
    }

    private void showEEGIndexDemoMode() {
        stopProcess();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        if (_eegIndexDemoFragment == null) {
            _eegIndexDemoFragment = EegIndexDemoFragment.newInstance();
        }
        ft.replace(R.id.container, _eegIndexDemoFragment);
        ft.commit();
        _demoMode = DemoMode.EEG_INDEX_MODE;
        showBackButton();
    }

    private void showMeditationDemoMode() {
        stopProcess();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        if (_meditationDemoFragment == null) {
            _meditationDemoFragment = MeditationDemoFragment.newInstance();
        }
        ft.replace(R.id.container, _meditationDemoFragment);
        ft.commit();
        _demoMode = DemoMode.MEDITATION_MODE;
        showBackButton();
    }

    private void showSpectrumDemoMode() {
        stopProcess();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        if (_spectrumDemoFragment == null) {
            _spectrumDemoFragment = SpectrumDemoFragment.newInstance();
        }
        ft.replace(R.id.container, _spectrumDemoFragment);
        ft.commit();
        _demoMode = DemoMode.SPECTRUM_MODE;
        showBackButton();
    }

    private void showSpectrumPowerDemoMode() {
        stopProcess();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        if (_spectrumPowerDemoFragment == null) {
            _spectrumPowerDemoFragment = SpectrumPowerDemoFragment.newInstance();
        }
        ft.replace(R.id.container, _spectrumPowerDemoFragment);
        ft.commit();
        _demoMode = DemoMode.SPECTRUM_POWER_MODE;
        showBackButton();
    }

    @Override
    public void onBackPressed() {
        if (_demoMode == DemoMode.START)
            super.onBackPressed();
        else {
            showDemoMode();
        }
    }

    private enum DemoMode {
        START,
        DEV_SEARCH,
        DEV_INF,
        SIGNAL_MODE,
        RESISTANCE_MODE,
        EEG_MODE,
        EEG_INDEX_MODE,
        MEDITATION_MODE,
        SPECTRUM_MODE,
        SPECTRUM_POWER_MODE,
    }
}
