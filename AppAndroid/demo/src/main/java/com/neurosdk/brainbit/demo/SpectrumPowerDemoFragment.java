package com.neurosdk.brainbit.demo;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.androidplot.xy.XYPlot;
import com.neuromd.neurosdk.ChannelInfo;
import com.neuromd.neurosdk.ChannelType;
import com.neuromd.neurosdk.Command;
import com.neuromd.neurosdk.Device;
import com.neuromd.neurosdk.DeviceState;
import com.neuromd.neurosdk.EegChannel;
import com.neuromd.neurosdk.INotificationCallback;
import com.neuromd.neurosdk.ParameterName;
import com.neuromd.neurosdk.ResistanceChannel;
import com.neuromd.neurosdk.SourceChannel;
import com.neuromd.neurosdk.SpectrumChannel;
import com.neuromd.neurosdk.SpectrumPowerChannel;
import com.neurosdk.brainbit.example.utils.CommonHelper;
import com.neurosdk.brainbit.example.utils.PlotHolder;
import com.neurosdk.brainbit.example.utils.PlotHolderSpectrum;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SpectrumPowerDemoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SpectrumPowerDemoFragment extends Fragment implements IBrainbitFragment {
    private final String TAG = "[SpectrumPowerDemo]";

    private Future<?> _futureUpd;

    private SpectrumPowerChannel _sppChannelO1;
    private SpectrumPowerChannel _sppChannelO2;
    private SpectrumPowerChannel _sppChannelT3;
    private SpectrumPowerChannel _sppChannelT4;

    private final AtomicReference<Double> _sppO1Val = new AtomicReference<>(0.0);
    private final AtomicReference<Double> _sppO2Val = new AtomicReference<>(0.0);
    private final AtomicReference<Double> _sppT3Val = new AtomicReference<>(0.0);
    private final AtomicReference<Double> _sppT4Val = new AtomicReference<>(0.0);

    public SpectrumPowerDemoFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment
     *
     * @return A new instance of fragment DemoModeFragment.
     */
    public static SpectrumPowerDemoFragment newInstance() {
        return new SpectrumPowerDemoFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_spectrum_power_demo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            Device device = DevHolder.inst().device();
            if (device != null) {
                ChannelInfo channelInfoO1 = DevHolder.inst().getDevChannel(SourceChannel.O1.name(), ChannelType.Signal);
                ChannelInfo channelInfoO2 = DevHolder.inst().getDevChannel(SourceChannel.O2.name(), ChannelType.Signal);
                ChannelInfo channelInfoT3 = DevHolder.inst().getDevChannel(SourceChannel.T3.name(), ChannelType.Signal);
                ChannelInfo channelInfoT4 = DevHolder.inst().getDevChannel(SourceChannel.T4.name(), ChannelType.Signal);
                if (channelInfoO1 != null && channelInfoO2 != null && channelInfoT3 != null && channelInfoT4 != null) {
                    configureDevice(device);
                    initChannels(device, channelInfoO1, channelInfoO2, channelInfoT3, channelInfoT4);
                    _futureUpd = Executors.newFixedThreadPool(1).submit(new Runnable() {
                        @SuppressWarnings("BusyWait")
                        @Override
                        public void run() {
                            try {
                                while (!Thread.currentThread().isInterrupted()) {
                                    Thread.sleep(500);
                                    updateData();
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    });
                }
            }
        } catch (Exception ex) {
            Log.d(TAG, "Failed start signal", ex);
            CommonHelper.showMessage(this, R.string.err_start_signal);
        }
    }

    /**
     * Update value in view
     *
     * @param txtValue display text field
     * @param sppVal   spectrum power value
     */
    private void updateViewValue(TextView txtValue, double sppVal) {
        if (txtValue != null) {
            txtValue.setText(getString(R.string.spectrum_power_value, sppVal * 1000.0));
        }
    }

    private void updateData() {
        FragmentActivity activity = getActivity();
        if (activity != null && isAdded()) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    View view = getView();
                    if (view != null && isAdded()) {
                        updateViewValue((TextView) view.findViewById(R.id.txt_o1_value), _sppO1Val.get());
                        updateViewValue((TextView) view.findViewById(R.id.txt_o2_value), _sppO2Val.get());
                        updateViewValue((TextView) view.findViewById(R.id.txt_t3_value), _sppT3Val.get());
                        updateViewValue((TextView) view.findViewById(R.id.txt_t4_value), _sppT4Val.get());
                    }
                }
            });
        }
    }

    private void updateSPPValue(SpectrumPowerChannel resCh, AtomicReference<Double> sppVal) {
        int tt = resCh.totalLength();
        if (tt > 0) {
            double[] sppVals = resCh.readData(tt - 1, 1);
            if (sppVals != null && sppVals.length > 0) {
                sppVal.set(sppVals[0]);
            }
        }
    }

    private void initChannels(Device device, ChannelInfo chInfO1, ChannelInfo chInfO2, ChannelInfo chInfT3, ChannelInfo chInfT4) {
        // Alpha rhythm frequency 8-13 Hz
        _sppChannelO1 = new SpectrumPowerChannel(new SpectrumChannel[]{new SpectrumChannel(new EegChannel(device, chInfO1))}, 8, 13, "O1_8_13Hz");
        _sppChannelO2 = new SpectrumPowerChannel(new SpectrumChannel[]{new SpectrumChannel(new EegChannel(device, chInfO2))}, 8, 13, "O2_8_13Hz");
        _sppChannelT3 = new SpectrumPowerChannel(new SpectrumChannel[]{new SpectrumChannel(new EegChannel(device, chInfT3))}, 8, 13, "T3_8_13Hz");
        _sppChannelT4 = new SpectrumPowerChannel(new SpectrumChannel[]{new SpectrumChannel(new EegChannel(device, chInfT4))}, 8, 13, "T4_8_13Hz");

        _sppChannelO1.dataLengthChanged.subscribe(new INotificationCallback<Integer>() {
            @Override
            public void onNotify(Object sender, Integer nParam) {
                updateSPPValue(_sppChannelO1, _sppO1Val);
            }
        });
        _sppChannelO2.dataLengthChanged.subscribe(new INotificationCallback<Integer>() {
            @Override
            public void onNotify(Object sender, Integer nParam) {
                updateSPPValue(_sppChannelO2, _sppO2Val);
            }
        });
        _sppChannelT3.dataLengthChanged.subscribe(new INotificationCallback<Integer>() {
            @Override
            public void onNotify(Object sender, Integer nParam) {
                updateSPPValue(_sppChannelT3, _sppT3Val);
            }
        });
        _sppChannelT4.dataLengthChanged.subscribe(new INotificationCallback<Integer>() {
            @Override
            public void onNotify(Object sender, Integer nParam) {
                updateSPPValue(_sppChannelT4, _sppT4Val);
            }
        });
    }

    private void configureDevice(Device device) {
        device.execute(Command.StartSignal);
    }

    @Override
    public void stopProcess() {
        try {
            Device device = DevHolder.inst().device();
            if (device != null) {
                if (_sppChannelO1 != null) {
                    _sppChannelO1.dataLengthChanged.unsubscribe();
                    _sppChannelO1 = null;
                }
                if (_sppChannelO2 != null) {
                    _sppChannelO2.dataLengthChanged.unsubscribe();
                    _sppChannelO2 = null;
                }
                if (_sppChannelT3 != null) {
                    _sppChannelT3.dataLengthChanged.unsubscribe();
                    _sppChannelT3 = null;
                }
                if (_sppChannelT4 != null) {
                    _sppChannelT4.dataLengthChanged.unsubscribe();
                    _sppChannelT4 = null;
                }
                Future<?> futureUpd = _futureUpd;
                if (futureUpd != null) {
                    _futureUpd = null;
                    futureUpd.cancel(true);
                }
                if (device.readParam(ParameterName.State) == DeviceState.Connected) {
                    device.execute(Command.StopSignal);
                }
            }
        } catch (Exception ex) {
            Log.d(TAG, "Failed stop signal", ex);
            CommonHelper.showMessage(this, R.string.err_stop_signal);
        }
    }
}