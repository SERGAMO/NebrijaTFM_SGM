package com.neurosdk.brainbit.demo;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import androidx.fragment.app.Fragment;

import com.neuromd.neurosdk.DeviceInfo;
import com.neurosdk.brainbit.example.utils.CommonHelper;
import com.neurosdk.brainbit.example.utils.DeviceHelper;
import com.neurosdk.brainbit.example.utils.SensorHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DevSearchFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DevSearchFragment extends Fragment implements IBrainbitFragment {
    private final static String TAG = "[DevSearch]";

    private final String DEV_NAME_KEY = "name";
    private final String DEV_ADDRESS_KEY = "address";

    private Button btnSearch;
    private ListView lvDevices;

    private BaseAdapter _lvDevicesAdapter;
    private final ArrayList<HashMap<String, String>> _deviceInfoList = new ArrayList<>();

    private final ExecutorService _es = Executors.newFixedThreadPool(1);

    public DevSearchFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment
     *
     * @return A new instance of fragment DevSearchFragment.
     */
    public static DevSearchFragment newInstance() {
        return new DevSearchFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_dev_search, container, false);
        btnSearch = rootView.findViewById(R.id.btn_search);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (DevHolder.inst().isSearchStarted()) {
                    stopSearch();
                } else {
                    startSearch();
                }
            }
        });
        initDevicesListView(rootView);
        DevHolder.inst().setDeviceEvent(new DeviceHelper.IDeviceEvent() {
            @Override
            public void searchStateChanged(final boolean searchState) {
                btnSearch.post(new Runnable() {
                    @Override
                    public void run() {
                        btnSearch.setText(searchState ? R.string.btn_stop_search_title : R.string.btn_start_search_title);
                    }
                });
                unlockView();
            }

            @Override
            public void deviceListChanged() {
                updateDevicesListView();
            }
        });
        return rootView;
    }

    private void initDevicesListView(View rootView) {
        lvDevices = rootView.findViewById(R.id.lv_devices);
        _lvDevicesAdapter = new SimpleAdapter(getContext(),
                _deviceInfoList,
                android.R.layout.simple_list_item_2,
                new String[]{DEV_NAME_KEY, DEV_ADDRESS_KEY},
                new int[]{android.R.id.text1, android.R.id.text2});
        lvDevices.setAdapter(_lvDevicesAdapter);
        lvDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            @SuppressWarnings("unchecked")
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Map<String, String> item = (Map<String, String>) _lvDevicesAdapter.getItem(position);
                if (item != null) {
                    stopSearch();
                    connectToDevice(item.get(DEV_ADDRESS_KEY));
                }
            }
        });
    }

    private void updateDevicesListView() {
        _deviceInfoList.clear();
        for (DeviceInfo it : DevHolder.inst().getDeviceInfoList()) {
            HashMap<String, String> map = new HashMap<>();
            map.put(DEV_NAME_KEY, it.name());
            map.put(DEV_ADDRESS_KEY, it.address());
            _deviceInfoList.add(map);
        }
        _lvDevicesAdapter.notifyDataSetInvalidated();
    }

    private void clearDevicesListView() {
        if (!_deviceInfoList.isEmpty()) {
            _deviceInfoList.clear();
            _lvDevicesAdapter.notifyDataSetInvalidated();
        }
    }

    private void stopSearch() {
        DevHolder.inst().stopSearch();
    }

    private void startSearch() {
        lockView();
        DevHolder.inst().disconnect();
        clearDevicesListView();
        DevHolder.inst().enabledSensor(new SensorHelper.ISensorEvent() {
            @Override
            public void ready() {
                DevHolder.inst().startSearch();
            }

            @Override
            public void cancel(String message, Exception error) {
                unlockView();
                CommonHelper.showMessage(DevSearchFragment.this, message);
            }
        });
    }

    private void invokeDeviceConnected() {
        CommonHelper.showMessage(DevSearchFragment.this, R.string.device_search_connected);
        unlockView();
    }

    private void lockView() {
        btnSearch.setEnabled(false);
        lvDevices.setEnabled(false);
    }

    private void unlockView() {
        btnSearch.post(new Runnable() {
            @Override
            public void run() {
                btnSearch.setEnabled(true);
                lvDevices.setEnabled(true);
            }
        });
    }

    private void connectToDevice(final String address) {
        lockView();
        _es.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    DevHolder.inst().connect(address);
                    invokeDeviceConnected();
                } catch (Exception ex) {
                    Log.d(TAG, "Failed connect to device", ex);
                    CommonHelper.showMessage(DevSearchFragment.this, R.string.device_search_connection_failed);
                    unlockView();
                }
            }
        });
    }

    @Override
    public void stopProcess() {
        stopSearch();
    }
}