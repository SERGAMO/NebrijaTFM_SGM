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
import com.neuromd.neurosdk.INotificationCallback;
import com.neuromd.neurosdk.ParameterName;
import com.neuromd.neurosdk.ResistanceChannel;
import com.neuromd.neurosdk.SourceChannel;
import com.neurosdk.brainbit.example.utils.CommonHelper;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ResistanceDemoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ResistanceDemoFragment extends Fragment implements IBrainbitFragment {
    private final String TAG = "[ResistanceDemo]";
    private Future<?> _futureUpd;

    private ResistanceChannel _resistChannelO1;
    private ResistanceChannel _resistChannelO2;
    private ResistanceChannel _resistChannelT3;
    private ResistanceChannel _resistChannelT4;

    private final AtomicReference<Double> _resO1Val = new AtomicReference<>(0.0);
    private final AtomicReference<Double> _resO2Val = new AtomicReference<>(0.0);
    private final AtomicReference<Double> _resT3Val = new AtomicReference<>(0.0);
    private final AtomicReference<Double> _resT4Val = new AtomicReference<>(0.0);


    public ResistanceDemoFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment
     *
     * @return A new instance of fragment DemoModeFragment.
     */
    public static ResistanceDemoFragment newInstance() {
        return new ResistanceDemoFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_resistance_demo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            Device device = DevHolder.inst().device();
            if (device != null) {
                ChannelInfo channelInfoO1 = DevHolder.inst().getDevChannel(SourceChannel.O1.name(), ChannelType.Resistance);
                ChannelInfo channelInfoO2 = DevHolder.inst().getDevChannel(SourceChannel.O2.name(), ChannelType.Resistance);
                ChannelInfo channelInfoT3 = DevHolder.inst().getDevChannel(SourceChannel.T3.name(), ChannelType.Resistance);
                ChannelInfo channelInfoT4 = DevHolder.inst().getDevChannel(SourceChannel.T4.name(), ChannelType.Resistance);
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
     * @param resVal   resistance value in Ohms
     */
    private void updateViewValue(TextView txtValue, double resVal) {
        if (txtValue != null) {
            txtValue.setText(resVal >= Double.POSITIVE_INFINITY || resVal <= Double.NEGATIVE_INFINITY ?
                    getString(R.string.el_resistance_infinity_value) :
                    getString(R.string.el_resistance_value, resVal / 1000.0));
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
                        updateViewValue((TextView) view.findViewById(R.id.txt_o1_value), _resO1Val.get());
                        updateViewValue((TextView) view.findViewById(R.id.txt_o2_value), _resO2Val.get());
                        updateViewValue((TextView) view.findViewById(R.id.txt_t3_value), _resT3Val.get());
                        updateViewValue((TextView) view.findViewById(R.id.txt_t4_value), _resT4Val.get());
                    }
                }
            });
        }
    }

    private void configureDevice(Device device) {
        device.execute(Command.StartResist);
    }

    private void updateResist(ResistanceChannel resCh, AtomicReference<Double> resVal) {
        int tt = resCh.totalLength();
        if (tt > 0) {
            double[] resVals = resCh.readData(tt - 1, 1);
            if (resVals != null && resVals.length > 0) {
                resVal.set(resVals[0]);
            }
        }
    }

    private void initChannels(Device device, ChannelInfo chInfO1, ChannelInfo chInfO2, ChannelInfo chInfT3, ChannelInfo chInfT4) {
        _resistChannelO1 = new ResistanceChannel(device, chInfO1);
        _resistChannelO2 = new ResistanceChannel(device, chInfO2);
        _resistChannelT3 = new ResistanceChannel(device, chInfT3);
        _resistChannelT4 = new ResistanceChannel(device, chInfT4);

        _resistChannelO1.dataLengthChanged.subscribe(new INotificationCallback<Integer>() {
            @Override
            public void onNotify(Object sender, Integer nParam) {
                updateResist(_resistChannelO1, _resO1Val);
            }
        });
        _resistChannelO2.dataLengthChanged.subscribe(new INotificationCallback<Integer>() {
            @Override
            public void onNotify(Object sender, Integer nParam) {
                updateResist(_resistChannelO2, _resO2Val);
            }
        });
        _resistChannelT3.dataLengthChanged.subscribe(new INotificationCallback<Integer>() {
            @Override
            public void onNotify(Object sender, Integer nParam) {
                updateResist(_resistChannelT3, _resT3Val);
            }
        });
        _resistChannelT4.dataLengthChanged.subscribe(new INotificationCallback<Integer>() {
            @Override
            public void onNotify(Object sender, Integer nParam) {
                updateResist(_resistChannelT4, _resT4Val);
            }
        });
    }

    @Override
    public void stopProcess() {
        try {
            Device device = DevHolder.inst().device();
            if (device != null) {
                if (_resistChannelO1 != null) {
                    _resistChannelO1.dataLengthChanged.unsubscribe();
                    _resistChannelO1 = null;
                }
                if (_resistChannelO2 != null) {
                    _resistChannelO2.dataLengthChanged.unsubscribe();
                    _resistChannelO2 = null;
                }
                if (_resistChannelT3 != null) {
                    _resistChannelT3.dataLengthChanged.unsubscribe();
                    _resistChannelT3 = null;
                }
                if (_resistChannelT4 != null) {
                    _resistChannelT4.dataLengthChanged.unsubscribe();
                    _resistChannelT4 = null;
                }
                Future<?> futureUpd = _futureUpd;
                if (futureUpd != null) {
                    _futureUpd = null;
                    futureUpd.cancel(true);
                }
                if (device.readParam(ParameterName.State) == DeviceState.Connected) {
                    device.execute(Command.StopResist);
                }
            }
        } catch (Exception ex) {
            Log.d(TAG, "Failed stop signal", ex);
            CommonHelper.showMessage(this, R.string.err_stop_signal);
        }
    }
}