package orbital.com.foodsearch.Fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import orbital.com.foodsearch.R;

public class SettingFragment extends PreferenceFragment {

    public SettingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_preference);
    }

}