package org.schabi.newpipe.settings;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import com.google.android.material.snackbar.Snackbar;

import org.schabi.newpipe.R;
import org.schabi.newpipe.util.PermissionHelper;

public class VideoAudioSettingsFragment extends BasePreferenceFragment {
    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private EditTextPreference bufferMinPref;
    private EditTextPreference bufferMaxPref;
    private String lastValidMin;
    private String lastValidMax;

    @Override
    public void onCreatePreferences(@Nullable final Bundle savedInstanceState,
                                    @Nullable final String rootKey) {
        addPreferencesFromResourceRegistry();

        bufferMinPref = (EditTextPreference) requirePreference(R.string.buffer_min_key);
        bufferMaxPref = (EditTextPreference) requirePreference(R.string.buffer_max_key);

        lastValidMin = defaultPreferences.getString(
                getString(R.string.buffer_min_key),
                getString(R.string.buffer_min_default_value));
        lastValidMax = defaultPreferences.getString(
                getString(R.string.buffer_max_key),
                getString(R.string.buffer_max_default_value));

        bufferMinPref.setOnPreferenceChangeListener(this::validateBufferMin);
        bufferMaxPref.setOnPreferenceChangeListener(this::validateBufferMax);

        listener = (sharedPreferences, s) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && s.equals(getString(R.string.minimize_on_exit_key))) {
                final String newSetting = sharedPreferences.getString(s, null);
                if (newSetting != null
                        && newSetting.equals(getString(R.string.minimize_on_exit_popup_key))
                        && !Settings.canDrawOverlays(getContext())) {

                    Snackbar.make(getListView(), R.string.permission_display_over_apps,
                                    Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.settings, view ->
                                    PermissionHelper.checkSystemAlertWindowPermission(getContext()))
                            .show();

                }
            }
        };

        updateBufferSummaries();
    }

    private boolean validateBufferMin(@NonNull final Preference preference,
                                      @NonNull final Object newValue) {
        final String strValue = newValue.toString().trim();
        try {
            final int value = Integer.parseInt(strValue);
            if (value < 20 || value > 570) {
                showBufferValidationError(
                        getString(R.string.buffer_min_title) + ": "
                                + value + " " + getString(R.string.buffer_range_error,
                                20, 570));
                return false;
            }

            // Auto-adjust max if gap would be < 30
            final int currentMax = Integer.parseInt(defaultPreferences.getString(
                    getString(R.string.buffer_max_key),
                    getString(R.string.buffer_max_default_value)));
            if (value + 30 > currentMax) {
                final int newMax = Math.min(value + 30, 600);
                bufferMaxPref.setText(String.valueOf(newMax));
                defaultPreferences.edit()
                        .putString(getString(R.string.buffer_max_key), String.valueOf(newMax))
                        .apply();
            }

            lastValidMin = strValue;
            updateBufferSummaries();
            return true;
        } catch (final NumberFormatException e) {
            showBufferValidationError(getString(R.string.buffer_invalid_number));
            return false;
        }
    }

    private boolean validateBufferMax(@NonNull final Preference preference,
                                      @NonNull final Object newValue) {
        final String strValue = newValue.toString().trim();
        try {
            final int value = Integer.parseInt(strValue);
            if (value > 600) {
                showBufferValidationError(
                        getString(R.string.buffer_max_title) + ": "
                                + value + " " + getString(R.string.buffer_range_error,
                                0, 600));
                return false;
            }

            // Auto-adjust min if gap would be < 30
            final int currentMin = Integer.parseInt(defaultPreferences.getString(
                    getString(R.string.buffer_min_key),
                    getString(R.string.buffer_min_default_value)));
            if (value < currentMin + 30) {
                final int newMin = Math.max(value - 30, 20);
                // If even at min=20 the gap is still < 30, reject
                if (value < 20 + 30) {
                    showBufferValidationError(
                            getString(R.string.buffer_max_title) + ": "
                                    + value + " " + getString(R.string.buffer_range_error,
                                    50, 600));
                    return false;
                }
                bufferMinPref.setText(String.valueOf(newMin));
                defaultPreferences.edit()
                        .putString(getString(R.string.buffer_min_key), String.valueOf(newMin))
                        .apply();
            }

            lastValidMax = strValue;
            updateBufferSummaries();
            return true;
        } catch (final NumberFormatException e) {
            showBufferValidationError(getString(R.string.buffer_invalid_number));
            return false;
        }
    }

    private void showBufferValidationError(final String message) {
        if (getView() != null) {
            Snackbar.make(getListView(), message, Snackbar.LENGTH_LONG).show();
        } else {
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
        }
    }

    private void updateBufferSummaries() {
        if (bufferMinPref != null) {
            bufferMinPref.setSummary(getString(R.string.buffer_min_summary)
                    + " (" + lastValidMin + " " + getString(R.string.seconds) + ")");
        }
        if (bufferMaxPref != null) {
            bufferMaxPref.setSummary(getString(R.string.buffer_max_summary)
                    + " (" + lastValidMax + " " + getString(R.string.seconds) + ")");
        }
    }

    @Override
    public boolean onPreferenceTreeClick(final Preference preference) {
        if (getString(R.string.caption_settings_key).equals(preference.getKey())) {
            try {
                startActivity(new Intent(Settings.ACTION_CAPTIONING_SETTINGS));
            } catch (final ActivityNotFoundException e) {
                Toast.makeText(getActivity(), R.string.general_error, Toast.LENGTH_SHORT).show();
            }
        }

        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(listener);
    }
}
