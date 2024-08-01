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

import com.neuromd.neurosdk.ChannelInfo;
import com.neuromd.neurosdk.ChannelType;
import com.neuromd.neurosdk.Command;
import com.neuromd.neurosdk.Device;
import com.neuromd.neurosdk.DeviceState;
import com.neuromd.neurosdk.EegChannel;
import com.neuromd.neurosdk.EegIndexChannel;
import com.neuromd.neurosdk.EegIndexValues;
import com.neuromd.neurosdk.INotificationCallback;
import com.neuromd.neurosdk.ParameterName;
import com.neuromd.neurosdk.SourceChannel;
import com.neurosdk.brainbit.example.utils.CommonHelper;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EegIndexDemoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EegIndexDemoFragment extends Fragment implements IBrainbitFragment {
    private final String TAG = "[EegIndexDemo]";
    private Future<?> _futureUpd;

    private EegIndexChannel _eegIndexChannel;

    private final AtomicReference<Double> _alphaVal = new AtomicReference<>(0.0);
    private final AtomicReference<Double> _betaVal = new AtomicReference<>(0.0);
    private final AtomicReference<Double> _deltaVal = new AtomicReference<>(0.0);
    private final AtomicReference<Double> _thetaVal = new AtomicReference<>(0.0);


    public EegIndexDemoFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment
     *
     * @return A new instance of fragment DemoModeFragment.
     */
    public static EegIndexDemoFragment newInstance() {
        return new EegIndexDemoFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_eeg_index_demo, container, false);
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
     * @param indexVal index value
     */
    private void updateViewValue(TextView txtValue, double indexVal) {
        if (txtValue != null) {
            txtValue.setText(getString(R.string.eeg_index_value, indexVal));
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
                        updateViewValue((TextView) view.findViewById(R.id.txt_alpha_index_value), _alphaVal.get());
                        updateViewValue((TextView) view.findViewById(R.id.txt_beta_index_value), _betaVal.get());
                        updateViewValue((TextView) view.findViewById(R.id.txt_delta_index_value), _deltaVal.get());
                        updateViewValue((TextView) view.findViewById(R.id.txt_theta_index_value), _thetaVal.get());
                    }
                }
            });
        }
    }

    private void configureDevice(Device device) {
        device.execute(Command.StartSignal);
    }

    private void initChannels(Device device, ChannelInfo chInfO1, ChannelInfo chInfO2, ChannelInfo chInfT3, ChannelInfo chInfT4) {
        _eegIndexChannel = new EegIndexChannel(new EegChannel(device, chInfT3),
                new EegChannel(device, chInfT4),
                new EegChannel(device, chInfO1),
                new EegChannel(device, chInfO2));
        _eegIndexChannel.dataLengthChanged.subscribe(new INotificationCallback<Integer>() {
            @Override
            public void onNotify(Object sender, Integer nParam) {
                int tt = _eegIndexChannel.totalLength();
                if (tt > 0) {
                    EegIndexValues[] indexVals = _eegIndexChannel.readData(tt - 1, 1);
                    if (indexVals != null && indexVals.length > 0) {
                        EegIndexValues indexVal = indexVals[0];
                        _alphaVal.set(indexVal.alphaRate());
                        _betaVal.set(indexVal.betaRate());
                        _deltaVal.set(indexVal.deltaRate());
                        _thetaVal.set(indexVal.thetaRate());
                    }
                }
            }
        });
    }

    @Override
    public void stopProcess() {
        try {
            Device device = DevHolder.inst().device();
            if (device != null) {
                if (_eegIndexChannel != null) {
                    _eegIndexChannel.dataLengthChanged.unsubscribe();
                    _eegIndexChannel = null;
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