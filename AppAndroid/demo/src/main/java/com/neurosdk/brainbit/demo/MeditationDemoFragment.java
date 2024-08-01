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
import com.neuromd.neurosdk.MeditationAnalyzer;
import com.neuromd.neurosdk.ParameterName;
import com.neuromd.neurosdk.SourceChannel;
import com.neurosdk.brainbit.example.utils.CommonHelper;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MeditationDemoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MeditationDemoFragment extends Fragment implements IBrainbitFragment {
    private final String TAG = "[MeditationDemo]";
    private Future<?> _futureUpd;

    private MeditationAnalyzer _meditationAnalyzer;
    private final AtomicInteger _meditationLevel = new AtomicInteger(0);
    private final AtomicReference<Double> _meditationProgress = new AtomicReference<>(0.0);


    public MeditationDemoFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment
     *
     * @return A new instance of fragment DemoModeFragment.
     */
    public static MeditationDemoFragment newInstance() {
        return new MeditationDemoFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_meditation_demo, container, false);
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

    private void updateData() {
        FragmentActivity activity = getActivity();
        if (activity != null && isAdded()) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    View view = getView();
                    if (view != null && isAdded()) {
                        TextView txtLevelValue = view.findViewById(R.id.txt_level_value);
                        if (txtLevelValue != null)
                            txtLevelValue.setText(String.valueOf(_meditationLevel.get()));
                        TextView txtLevelProgressValue = view.findViewById(R.id.txt_level_progress_value);
                        if (txtLevelProgressValue != null)
                            txtLevelProgressValue.setText(String.valueOf(Math.round(_meditationProgress.get() * 100)));
                    }
                }
            });
        }
    }

    private void configureDevice(Device device) {
        device.execute(Command.StartSignal);
    }

    private void initChannels(Device device, ChannelInfo chInfO1, ChannelInfo chInfO2, ChannelInfo chInfT3, ChannelInfo chInfT4) {
        EegIndexChannel eegIndexChannel = new EegIndexChannel(new EegChannel(device, chInfT3),
                new EegChannel(device, chInfT4),
                new EegChannel(device, chInfO1),
                new EegChannel(device, chInfO2));
        _meditationAnalyzer = new MeditationAnalyzer(eegIndexChannel);
        _meditationAnalyzer.levelChanged.subscribe(new INotificationCallback<Integer>() {
            @Override
            public void onNotify(Object sender, Integer nParam) {
                _meditationLevel.set(nParam);
            }
        });
        _meditationAnalyzer.levelProgressChanged.subscribe(new INotificationCallback<Double>() {
            @Override
            public void onNotify(Object sender, Double nParam) {
                _meditationProgress.set(nParam);
            }
        });
    }

    @Override
    public void stopProcess() {
        try {
            Device device = DevHolder.inst().device();
            if (device != null) {
                if (_meditationAnalyzer != null) {
                    _meditationAnalyzer.levelChanged.unsubscribe();
                    _meditationAnalyzer.levelProgressChanged.unsubscribe();
                    _meditationAnalyzer = null;
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