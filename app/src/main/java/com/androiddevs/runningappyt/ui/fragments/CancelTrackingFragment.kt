package com.androiddevs.runningappyt.ui.fragments

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.androiddevs.runningappyt.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CancelTrackingFragment:DialogFragment() {
    private var yesListener: (() -> Unit)? = null
    fun setListener(listener: () -> Unit){
        yesListener = listener
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        return MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("Cancel the Run?")
            .setMessage("Are you sure to cancel the current run and delete all its data?")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("Yes") { _, _ ->
                yesListener?.let { run->
                    run
                }
            }
            .setNegativeButton("No") { dialogInterface, _ ->
                dialogInterface.cancel()
            }
            .create()
    }
}