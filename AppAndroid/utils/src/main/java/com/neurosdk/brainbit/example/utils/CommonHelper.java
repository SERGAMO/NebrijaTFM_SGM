package com.neurosdk.brainbit.example.utils;

import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public final class CommonHelper {
    public static void showMessage(final Fragment fragment, final CharSequence msg) {
        if (fragment == null || TextUtils.isEmpty(msg))
            return;
        final FragmentActivity activity = fragment.getActivity();
        if (activity != null && fragment.isAdded()) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (fragment.isAdded())
                        Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public static void showMessage(final Fragment fragment, @StringRes int msgId) {
        if (fragment != null)
            showMessage(fragment, fragment.getString(msgId));
    }
}
