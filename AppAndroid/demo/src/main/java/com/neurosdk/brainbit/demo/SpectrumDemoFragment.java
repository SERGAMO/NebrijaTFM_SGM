package com.neurosdk.brainbit.demo;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.androidplot.xy.XYPlot;
import com.neuromd.neurosdk.ChannelInfo;
import com.neuromd.neurosdk.ChannelType;
import com.neuromd.neurosdk.Command;
import com.neuromd.neurosdk.Device;
import com.neuromd.neurosdk.DeviceState;
import com.neuromd.neurosdk.EegChannel;
import com.neuromd.neurosdk.ParameterName;
import com.neuromd.neurosdk.SourceChannel;
import com.neuromd.neurosdk.SpectrumChannel;
import com.neurosdk.brainbit.example.utils.CommonHelper;
import com.neurosdk.brainbit.example.utils.PlotHolder;
import com.neurosdk.brainbit.example.utils.PlotHolderSpectrum;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SpectrumDemoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SpectrumDemoFragment extends Fragment implements IBrainbitFragment {
    private final String TAG = "[SpectrumDemo]";
    private PlotHolderSpectrum plotO1;
    private PlotHolderSpectrum plotO2;
    private PlotHolderSpectrum plotT3;
    private PlotHolderSpectrum plotT4;

    public SpectrumDemoFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment
     *
     * @return A new instance of fragment DemoModeFragment.
     */
    public static SpectrumDemoFragment newInstance() {
        return new SpectrumDemoFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return initPlot(inflater.inflate(R.layout.fragment_spectrum_demo, container, false));
    }

    private View initPlot(View rootView) {
        plotO1 = new PlotHolderSpectrum((XYPlot) rootView.findViewById(R.id.plot_signal_1));
        plotO2 = new PlotHolderSpectrum((XYPlot) rootView.findViewById(R.id.plot_signal_2));
        plotT3 = new PlotHolderSpectrum((XYPlot) rootView.findViewById(R.id.plot_signal_3));
        plotT4 = new PlotHolderSpectrum((XYPlot) rootView.findViewById(R.id.plot_signal_4));
        return rootView;
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
                    // Create channel
                    plotO1.startRender(new SpectrumChannel(new EegChannel(device, channelInfoO1)), PlotHolderSpectrum.ZoomVal.V_AUTO_1);
                    plotO2.startRender(new SpectrumChannel(new EegChannel(device, channelInfoO2)), PlotHolderSpectrum.ZoomVal.V_AUTO_1);
                    plotT3.startRender(new SpectrumChannel(new EegChannel(device, channelInfoT3)), PlotHolderSpectrum.ZoomVal.V_AUTO_1);
                    plotT4.startRender(new SpectrumChannel(new EegChannel(device, channelInfoT4)), PlotHolderSpectrum.ZoomVal.V_AUTO_1);
                }
            }
        } catch (Exception ex) {
            Log.d(TAG, "Failed start signal", ex);
            CommonHelper.showMessage(this, R.string.err_start_signal);
        }
    }

    private void configureDevice(Device device) {
        device.execute(Command.StartSignal);
    }

    @Override
    public void stopProcess() {
        if (plotO1 != null) {
            plotO1.stopRender();
        }
        if (plotO2 != null) {
            plotO2.stopRender();
        }
        if (plotT3 != null) {
            plotT3.stopRender();
        }
        if (plotT4 != null) {
            plotT4.stopRender();
        }
        try {
            Device device = DevHolder.inst().device();
            if (device != null) {
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