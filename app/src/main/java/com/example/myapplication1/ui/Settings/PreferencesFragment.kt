package com.example.myapplication1.ui.Settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.example.myapplication1.R

class PreferencesFragment: PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_screen, rootKey)
    }


}