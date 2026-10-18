// SPDX-License-Identifier: GPL-3.0-or-later
package com.serwylo.lexica;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

/** Single-use device-credential authorization; never persists an external intent. */
public final class ParentalExternalAction {
    private static final String TAG = "wigo-parent-external";
    private ParentalExternalAction() { }

    public static void start(Context context, Intent externalIntent) {
        Context owner = context;
        while (owner instanceof ContextWrapper && !(owner instanceof Activity)) {
            Context next = ((ContextWrapper) owner).getBaseContext();
            if (next == owner) break;
            owner = next;
        }
        if (!(owner instanceof FragmentActivity)) return;
        FragmentActivity activity = (FragmentActivity) owner;
        if (activity.isFinishing() || activity.isDestroyed()) return;
        FragmentManager manager = activity.getSupportFragmentManager();
        if (manager.isStateSaved() || manager.findFragmentByTag(TAG) != null) return;
        KeyguardManager keyguard = (KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE);
        if (Build.VERSION.SDK_INT < 23 || keyguard == null || !keyguard.isDeviceSecure()) {
            Toast.makeText(activity, R.string.wigo_parent_blocked, Toast.LENGTH_LONG).show();
            return;
        }
        Intent confirmation = keyguard.createConfirmDeviceCredentialIntent(
                activity.getString(R.string.wigo_parent_title), activity.getString(R.string.wigo_parent_message));
        if (confirmation == null) return;
        GateFragment gate = new GateFragment();
        gate.pending = new Intent(externalIntent);
        manager.beginTransaction().add(gate, TAG).commitNow();
        try {
            gate.startActivityForResult(confirmation, GateFragment.REQUEST);
        } catch (RuntimeException unavailable) {
            gate.clearAndRemove();
            Toast.makeText(activity, R.string.wigo_parent_blocked, Toast.LENGTH_LONG).show();
        }
    }

    public static class GateFragment extends Fragment {
        private static final int REQUEST = 7901;
        private Intent pending;

        @Override public void onCreate(Bundle state) {
            super.onCreate(state);
            // Restored fragments have no pending action: authorization is not transferable.
            if (state != null) clearAndRemove();
        }

        @Override public void onActivityResult(int request, int result, Intent data) {
            super.onActivityResult(request, result, data);
            if (request != REQUEST) return;
            Intent action = pending;
            FragmentActivity activity = getActivity();
            clearAndRemove();
            if (result != Activity.RESULT_OK || action == null || activity == null
                    || activity.isFinishing() || activity.isDestroyed()) return;
            try {
                activity.startActivity(action);
            } catch (RuntimeException unavailable) {
                Toast.makeText(activity, R.string.wigo_external_unavailable, Toast.LENGTH_LONG).show();
            }
        }

        private void clearAndRemove() {
            pending = null;
            if (isAdded()) getParentFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
        }

        @Override public void onDestroy() {
            pending = null;
            super.onDestroy();
        }
    }
}
