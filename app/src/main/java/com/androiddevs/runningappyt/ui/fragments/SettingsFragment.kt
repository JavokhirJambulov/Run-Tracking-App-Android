package com.androiddevs.runningappyt.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.androiddevs.runningappyt.R
import com.androiddevs.runningappyt.databinding.FragmentSettingsBinding
import com.androiddevs.runningappyt.other.Constants.KEY_NAME
import com.androiddevs.runningappyt.other.Constants.KEY_WEIGHT
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    @Inject
    lateinit var sharedPreferences: SharedPreferences
    private lateinit var settingsFragment: FragmentSettingsBinding
    //private lateinit var tvToolbarTitle:MaterialTextView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        settingsFragment = DataBindingUtil.inflate(inflater, R.layout.fragment_settings,container,false)
        //tvToolbarTitle = view?.findViewById(R.id.tvToolbarTitle)!!
        return settingsFragment.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadFieldsFromSharedPref()

        settingsFragment.btnApplyChanges.setOnClickListener {
            val success = applyChangesToSharedPref()
            if(success) {
                Snackbar.make(view, "Saved changes", Snackbar.LENGTH_LONG).show()
            } else {
                Snackbar.make(view, "Please fill out all the fields", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun loadFieldsFromSharedPref() {
        val name = sharedPreferences.getString(KEY_NAME, "")
        val weight = sharedPreferences.getFloat(KEY_WEIGHT, 80f)
        settingsFragment.etName.setText(name)
        settingsFragment.etWeight.setText(weight.toString())
    }

    private fun applyChangesToSharedPref(): Boolean {
        val nameText = settingsFragment.etName.text.toString()
        val weightText = settingsFragment.etWeight.text.toString()
        if(nameText.isEmpty() || weightText.isEmpty()) {
            return false
        }
        sharedPreferences.edit()
            .putString(KEY_NAME, nameText)
            .putFloat(KEY_WEIGHT, weightText.toFloat())
            .apply()
        val toolbarText = "Let's go $nameText"
        //tvToolbarTitle.text = toolbarText
        return true
    }
}