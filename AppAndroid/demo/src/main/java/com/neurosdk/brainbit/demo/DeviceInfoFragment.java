package com.neurosdk.brainbit.demo;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.neuromd.neurosdk.Command;
import com.neuromd.neurosdk.Device;
import com.neuromd.neurosdk.DeviceState;
import com.neuromd.neurosdk.FirmwareMode;
import com.neuromd.neurosdk.FirmwareVersion;
import com.neuromd.neurosdk.Parameter;
import com.neuromd.neurosdk.ParameterName;
import com.neurosdk.brainbit.example.utils.CommonHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DeviceInfoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DeviceInfoFragment extends Fragment implements IBrainbitFragment {
    private final String TAG = "[DeviceInfo]";
    private final ExecutorService _es = Executors.newFixedThreadPool(1);
    @SuppressWarnings("rawtypes")
    private Future _future;
    private final static String NEW_LINE = System.lineSeparator();

    public DeviceInfoFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment
     *
     * @return A new instance of fragment DemoModeFragment.
     */
    public static DeviceInfoFragment newInstance() {
        return new DeviceInfoFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_device_info, container, false);
        view.findViewById(R.id.btn_dev_info_copy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView txtDevInfo = view.findViewById(R.id.txt_dev_info);
                Context context = getContext();
                if (context != null && txtDevInfo != null && !TextUtils.isEmpty(txtDevInfo.getText())) {
                    try {
                        ClipboardManager clipBoard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clipData = ClipData.newPlainText("label", txtDevInfo.getText());
                        clipBoard.setPrimaryClip(clipData);
                        CommonHelper.showMessage(DeviceInfoFragment.this, R.string.dev_inf_copied);
                    } catch (Exception ex) {
                        Log.d(TAG, "Failed copied device info", ex);
                    }
                }
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        showDeviceInfo((TextView) view.findViewById(R.id.txt_dev_info));
    }

    private void updateDeviceInfoText(final TextView txtDevInfo, final CharSequence info) {
        final FragmentActivity activity = getActivity();
        if (activity != null && isAdded()) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isAdded())
                        txtDevInfo.setText(info);
                }
            });
        }
    }

    private void showDeviceInfo(final TextView txtDevInfo) {
        txtDevInfo.setText("");
        final Device device = DevHolder.inst().device();
        if (device == null)
            return;
        _future = _es.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (device.readParam(ParameterName.State) == DeviceState.Connected) {
                        StringBuilder sbInf = new StringBuilder();
                        sbInf.append(getString(R.string.dev_inf_common_params)).append(NEW_LINE);
                        sbInf.append(getString(R.string.dev_inf_name)).append("[").append((String) device.readParam(ParameterName.Name)).append("]").append(NEW_LINE);
                        sbInf.append(getString(R.string.dev_inf_address)).append("[").append((String) device.readParam(ParameterName.Address)).append("]").append(NEW_LINE);
                        sbInf.append(getString(R.string.dev_inf_serial_number)).append("[").append((String) device.readParam(ParameterName.SerialNumber)).append("]").append(NEW_LINE);
                        FirmwareVersion version = device.readParam(ParameterName.FirmwareVersion);
                        sbInf.append(getString(R.string.dev_inf_version)).append("[").append(version.version()).append(".").append(version.build()).append("]").append(NEW_LINE);
                        sbInf.append(getString(R.string.dev_inf_mode)).append("[").append(((FirmwareMode) device.readParam(ParameterName.FirmwareMode)).name()).append("]");

                        Parameter[] parameters = device.parameters();
                        if (parameters != null && parameters.length > 0) {
                            sbInf.append(NEW_LINE).append(NEW_LINE).append(getString(R.string.dev_inf_supported_params));
                            for (Parameter pIt : parameters) {
                                sbInf.append(NEW_LINE).append(getString(R.string.dev_inf_param_name)).append("[").append(pIt.getName()).append("] ");
                                sbInf.append(getString(R.string.dev_inf_param_type)).append("[").append(pIt.getType()).append("] ");
                                sbInf.append(getString(R.string.dev_inf_param_access)).append("[").append(pIt.getAccess()).append("]");
                            }
                        }

                        ChannelInfo[] channelInfos = device.channels();
                        if (channelInfos != null && channelInfos.length > 0) {
                            sbInf.append(NEW_LINE).append(NEW_LINE).append(getString(R.string.dev_inf_supported_channels));
                            for (ChannelInfo chIt : channelInfos) {
                                sbInf.append(NEW_LINE).append(getString(R.string.dev_inf_channel_name)).append("[").append(chIt.getName()).append("] ");
                                sbInf.append(getString(R.string.dev_inf_channel_type)).append("[").append(chIt.getType()).append("] ");
                                sbInf.append(getString(R.string.dev_inf_channel_index)).append("[").append(chIt.getIndex()).append("]");
                            }
                        }

                        Command[] commands = device.commands();
                        if (commands != null && commands.length > 0) {
                            sbInf.append(NEW_LINE).append(NEW_LINE).append(getString(R.string.dev_inf_supported_commands));
                            for (Command cmdIt : commands) {
                                sbInf.append(NEW_LINE).append(cmdIt);
                            }
                        }

                        updateDeviceInfoText(txtDevInfo, sbInf.toString());
                    } else {
                        CommonHelper.showMessage(DeviceInfoFragment.this, R.string.err_dev_inf_disconnected);
                    }
                } catch (Exception ex) {
                    Log.d(TAG, "Failed load device info", ex);
                    CommonHelper.showMessage(DeviceInfoFragment.this, R.string.err_dev_inf_failed);
                }
            }
        });
        /*execute(new Runnable() {
            @Override
            public void run() {

            }
        });
        try {

        } catch (Exception ex) {
            Log.d("[DeviceInfo]", "Failed load device info", ex);
        }
        */
    }

    @Override
    public void stopProcess() {
        try {
            if (!_future.isDone())
                _future.cancel(false);
        } catch (Exception ex) {
            Log.d("[DeviceInfo]", "Failed stop load device info", ex);
        } finally {
            _future = null;
        }

    }
}