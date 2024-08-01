package com.neurosdk.brainbit.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.neuromd.neurosdk.ChannelType;
import com.neuromd.neurosdk.Device;
import com.neuromd.neurosdk.DeviceState;
import com.neuromd.neurosdk.ParameterName;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DemoModeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DemoModeFragment extends Fragment {
    private IDemoModeCallback _callback;

    public DemoModeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment
     *
     * @return A new instance of fragment DemoModeFragment.
     */
    public static DemoModeFragment newInstance() {
        return new DemoModeFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_demo_mode, container, false);

        // Device search mode click
        rootView.findViewById(R.id.btn_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IDemoModeCallback cb = _callback;
                if (cb != null)
                    cb.modeDevSearch();
            }
        });
        // Device Info mode click
        rootView.findViewById(R.id.btn_device_info).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IDemoModeCallback cb = _callback;
                if (cb != null)
                    cb.modeDevInfo();
            }
        });
        // Signal mode click
        rootView.findViewById(R.id.btn_signal_demo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IDemoModeCallback cb = _callback;
                if (cb != null)
                    cb.modeSignalDemo();
            }
        });
        // Resistance mode click
        rootView.findViewById(R.id.btn_resistance_demo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IDemoModeCallback cb = _callback;
                if (cb != null)
                    cb.modeResistanceDemo();
            }
        });
        // EEG mode click
        rootView.findViewById(R.id.btn_eeg_demo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IDemoModeCallback cb = _callback;
                if (cb != null)
                    cb.modeEEGDemo();
            }
        });
        // EEG Index mode click
        rootView.findViewById(R.id.btn_eeg_index_demo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IDemoModeCallback cb = _callback;
                if (cb != null)
                    cb.modeEEGIndexDemo();
            }
        });
        // Meditation mode click
        rootView.findViewById(R.id.btn_meditation_demo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IDemoModeCallback cb = _callback;
                if (cb != null)
                    cb.modeMeditationDemo();
            }
        });
        // Spectrum mode click
        rootView.findViewById(R.id.btn_spectrum_demo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IDemoModeCallback cb = _callback;
                if (cb != null)
                    cb.modeSpectrumDemo();
            }
        });
        // Spectrum Power mode click
        rootView.findViewById(R.id.btn_spectrum_power_demo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IDemoModeCallback cb = _callback;
                if (cb != null)
                    cb.modeSpectrumPowerDemo();
            }
        });
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateButtonState();
    }

    public void setCallback(IDemoModeCallback callback) {
        _callback = callback;
    }

    public void updateButtonState() {
        View view = getView();
        if (view == null)
            return;
        Device device = DevHolder.inst().device();
        if (device == null || device.readParam(ParameterName.State) == DeviceState.Disconnected) {
            view.findViewById(R.id.btn_device_info).setEnabled(false);
            view.findViewById(R.id.btn_signal_demo).setEnabled(false);
            view.findViewById(R.id.btn_resistance_demo).setEnabled(false);
            view.findViewById(R.id.btn_eeg_demo).setEnabled(false);
            view.findViewById(R.id.btn_eeg_index_demo).setEnabled(false);
            view.findViewById(R.id.btn_meditation_demo).setEnabled(false);
            view.findViewById(R.id.btn_spectrum_demo).setEnabled(false);
            view.findViewById(R.id.btn_spectrum_power_demo).setEnabled(false);
        } else {
            view.findViewById(R.id.btn_device_info).setEnabled(true);
            boolean hasSignalChannel = DevHolder.inst().getDevChannel(ChannelType.Signal) != null;
            view.findViewById(R.id.btn_signal_demo).setEnabled(hasSignalChannel);
            view.findViewById(R.id.btn_resistance_demo).setEnabled(DevHolder.inst().getDevChannel(ChannelType.Resistance) != null);
            view.findViewById(R.id.btn_eeg_demo).setEnabled(hasSignalChannel);
            view.findViewById(R.id.btn_eeg_index_demo).setEnabled(hasSignalChannel);
            view.findViewById(R.id.btn_meditation_demo).setEnabled(hasSignalChannel);
            view.findViewById(R.id.btn_spectrum_demo).setEnabled(hasSignalChannel);
            view.findViewById(R.id.btn_spectrum_power_demo).setEnabled(hasSignalChannel);
        }
    }

    public interface IDemoModeCallback {
        void modeDevSearch();

        void modeDevInfo();

        void modeSignalDemo();

        void modeResistanceDemo();

        void modeEEGDemo();

        void modeEEGIndexDemo();

        void modeMeditationDemo();

        void modeSpectrumDemo();

        void modeSpectrumPowerDemo();
    }
}