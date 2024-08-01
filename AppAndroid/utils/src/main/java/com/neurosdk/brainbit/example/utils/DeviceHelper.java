package com.neurosdk.brainbit.example.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.neuromd.neurosdk.Device;
import com.neuromd.neurosdk.DeviceEnumerator;
import com.neuromd.neurosdk.DeviceInfo;
import com.neuromd.neurosdk.DeviceType;
import com.neuromd.neurosdk.INotificationCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class DeviceHelper {
    private final AppCompatActivity _context;
    private boolean _started;
    private final ReentrantLock _searchLock = new ReentrantLock();
    private final DeviceType _devType;
    private DeviceEnumerator _deviceEnum;
    private final List<DeviceInfo> _deviceInfoList = new ArrayList<>();
    private IDeviceEvent _devEvent;


    public DeviceHelper(AppCompatActivity context, DeviceType devType) {
        _context = context;
        _devType = devType;
    }

    public DeviceHelper(AppCompatActivity context) {
        this(context, DeviceType.Callibri);
    }

    private void invokeSearchState(boolean searchState) {
        final IDeviceEvent ev = _devEvent;
        if (ev != null)
            ev.searchStateChanged(searchState);
    }

    private void invokeDeviceListChanged() {
        final IDeviceEvent ev = _devEvent;
        if (ev != null) {
            _context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ev.deviceListChanged();
                }
            });
        }
    }

    private void updateDevInfo() {
        if (_searchLock.tryLock()) {
            boolean hasChanged = false;
            try {
                // Checking the change of the device info list
                // This is a slow solution
                List<DeviceInfo> deviceInfoList = _deviceEnum.devices();
                if (deviceInfoList.size() != _deviceInfoList.size()) {
                    hasChanged = true;
                } else {
                    boolean founded;
                    for (DeviceInfo it : _deviceInfoList) {
                        founded = false;
                        for (DeviceInfo itIn : deviceInfoList) {
                            if (TextUtils.equals(itIn.address(), it.address())) {
                                founded = true;
                                break;
                            }
                        }
                        if (!founded) {
                            hasChanged = true;
                            break;
                        }
                    }
                }
                if (hasChanged) {
                    _deviceInfoList.clear();
                    if (deviceInfoList.size() > 0)
                        _deviceInfoList.addAll(deviceInfoList);
                }
            } finally {
                _searchLock.unlock();
            }
            if (hasChanged) {
                // fire event
                invokeDeviceListChanged();
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void startSearch() {
        if (_searchLock.tryLock()) {
            try {
                if (_started)
                    return;
                if (_deviceEnum == null)
                    _deviceEnum = new DeviceEnumerator(_context, _devType);
                _deviceInfoList.clear();
                _deviceEnum.deviceListChanged.subscribe(new INotificationCallback() {
                    @Override
                    public void onNotify(Object o, Object o2) {
                        updateDevInfo();
                    }
                });
                _started = true;
            } finally {
                _searchLock.unlock();
            }
            invokeSearchState(true);
        } else {
            invokeSearchState(isSearchStarted());
        }
    }

    public void stopSearch() {
        if (_searchLock.tryLock()) {
            try {
                if (!_started)
                    return;
                _started = false;
                if (_deviceEnum != null)
                    _deviceEnum.deviceListChanged.unsubscribe();
            } finally {
                _searchLock.unlock();
            }
            invokeSearchState(false);
        }
    }

    public boolean isSearchStarted() {
        try {
            if (_searchLock.tryLock(100, TimeUnit.MILLISECONDS)) {
                try {
                    return _started;
                } finally {
                    _searchLock.unlock();
                }
            }
            return false;
        } catch (InterruptedException e) {
            return false;
        }
    }

    public void setDeviceEvent(IDeviceEvent devEvent) {
        _devEvent = devEvent;
    }

    public List<DeviceInfo> getDeviceInfoList() {
        try {
            if (_searchLock.tryLock(100, TimeUnit.MILLISECONDS)) {
                try {
                    return Collections.unmodifiableList(_deviceInfoList);
                } finally {
                    _searchLock.unlock();
                }
            }
            return Collections.emptyList();
        } catch (InterruptedException e) {
            return Collections.emptyList();
        }
    }

    private boolean boundTo(String devAddress) {
        BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> devBounded = bAdapter.getBondedDevices();
        boolean ready = false;
        for (BluetoothDevice it : devBounded) {
            if (it.getAddress().equals(devAddress)) {
                ready = true;
                break;
            }
        }
        if (!ready) {
            BluetoothDevice dev = bAdapter.getRemoteDevice(devAddress);
            if (dev != null) {
                final ConditionVariable cv = new ConditionVariable();
                final AtomicBoolean boundedReady = new AtomicBoolean(false);
                BroadcastReceiver receiverBound = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent != null) {
                            Bundle extras = intent.getExtras();
                            if (extras != null) {
                                int state = extras.getInt(BluetoothDevice.EXTRA_BOND_STATE);
                                if (state == BluetoothDevice.BOND_BONDED) {
                                    boundedReady.set(true);
                                    cv.open();
                                } else if (state == BluetoothDevice.BOND_NONE) {
                                    cv.open();
                                }
                            }
                        }
                    }
                };
                _context.registerReceiver(receiverBound, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
                if (dev.createBond()) {
                    cv.block();
                    ready = boundedReady.get();
                }
                _context.unregisterReceiver(receiverBound);
            }
        }
        return ready;
    }

    public Device createDevice(DeviceInfo deviceInfo) {
        if (deviceInfo == null || TextUtils.isEmpty(deviceInfo.address()))
            return null;
        try {
            if (_searchLock.tryLock(100, TimeUnit.MILLISECONDS)) {
                try {
                    if (_deviceEnum == null)
                        _deviceEnum = new DeviceEnumerator(_context, _devType);
                    if (deviceInfo.name().toLowerCase().contains("black")) {
                        if (boundTo(deviceInfo.address())) {
                            return _deviceEnum.createDevice(deviceInfo);
                        }
                    } else {
                        return _deviceEnum.createDevice(deviceInfo);
                    }
                } finally {
                    _searchLock.unlock();
                }
            }
            return null;
        } catch (InterruptedException e) {
            return null;
        }
    }

    public Device createDevice(String address) {
        if (!TextUtils.isEmpty(address)) {
            List<DeviceInfo> devLst = getDeviceInfoList();
            for (DeviceInfo it : devLst) {
                if (TextUtils.equals(it.address(), address)) {
                    return createDevice(it);
                }
            }
        }
        return null;
    }

    public interface IDeviceEvent {
        void searchStateChanged(boolean searchState);

        void deviceListChanged();
    }
}
