package org.pur3.tomatotimer

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment

class TimeSetterDialog(val buttonName: String) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            val mView = inflater.inflate(R.layout.activity_set_time, null)
            val etHours = mView.findViewById<EditText>(R.id.et_hours)
            val etMinutes = mView.findViewById<EditText>(R.id.et_minutes)

            builder.setView(mView)
                // Add action buttons
                .setPositiveButton(R.string.ok) { dialog, id ->
                    setTime(etHours, etMinutes)
                }
                .setNegativeButton(R.string.cancel) { dialog, id ->
                    getDialog()?.cancel()
                }
                .setTitle("Set $buttonName")
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun presentSetTime(hour: Int, minute: Int): String {
        var hFormat = hour.toString()
        var mFormat = minute.toString()

        if (hFormat.length == 1) hFormat = "0$hour"
        if (mFormat.length == 1) mFormat = "0$minute"

        val formattedTime = "$hFormat:$mFormat:00"

        return formattedTime
    }

    private fun setTime(hourBox: EditText, minuteBox: EditText) {

        val hoursText = hourBox.text.toString()
        val minutesText = minuteBox.text.toString()

        if (buttonName == "Work Duration") {
            if (hoursText.isBlank() && minutesText.isNotBlank()) {
                // if the user only is setting the minutes, just change the minutes and erase the hours
                workTimeHours = 0
                workTimeMinutes = minutesText.toInt()
                displayTimer.text = presentSetTime(workTimeHours, workTimeMinutes)
                saveWorkTimeSettings()
            } else if (hoursText.isNotBlank() && minutesText.isBlank()) {
                // if the user is only setting the hours and not the minutes, change the hours and erase the minutes
                workTimeHours = hoursText.toInt()
                workTimeMinutes = 0
                displayTimer.text = presentSetTime(workTimeHours, workTimeMinutes)
                saveWorkTimeSettings()
            } else if (hoursText.isNotBlank() && minutesText.isNotBlank()) {
                // If both the spots are filled, set them
                workTimeHours = hoursText.toInt()
                workTimeMinutes = minutesText.toInt()
                displayTimer.text = presentSetTime(workTimeHours, workTimeMinutes)
                saveWorkTimeSettings()
            } else {
                // "Ok" was tapped and nothing has been set in the boxes, don't do anything
            }
        } else {

            if (hoursText.isBlank() && minutesText.isNotBlank()) {
                // if the user only is setting the minutes, just change the minutes and erase the hours
                breakTimeHours = 0
                breakTimeMinutes = minutesText.toInt()
                Toast.makeText(context, "Break timer set", Toast.LENGTH_LONG).show()
                saveBreakTimeSettings()
            } else if (hoursText.isNotBlank() && minutesText.isBlank()) {
                // if the user is only setting the hours and not the minutes, change the hours and erase the minutes
                breakTimeHours = hoursText.toInt()
                breakTimeMinutes = 0
                Toast.makeText(context, "Break timer set", Toast.LENGTH_LONG).show()
                saveBreakTimeSettings()
            } else if (hoursText.isNotBlank() && minutesText.isNotBlank()) {
                // If both the spots are filled, set them
                breakTimeHours = hoursText.toInt()
                breakTimeMinutes = minutesText.toInt()
                Toast.makeText(context, "Break timer set", Toast.LENGTH_LONG).show()
                saveBreakTimeSettings()
            } else {
                // "Ok" was tapped and nothing has been set in the boxes, don't do anything
            }
        }
    }

    private fun saveWorkTimeSettings() {
        val sharedPref = activity?.getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE
        )

        with(sharedPref?.edit()) {
            this?.putInt(getString(R.string.saved_worktime_hours), workTimeHours)
            this?.putInt(getString(R.string.saved_worktime_minutes), workTimeMinutes)
            this?.apply()
        }
    }

    private fun saveBreakTimeSettings() {
        val sharedPref = activity?.getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE
        )

        with(sharedPref?.edit()) {
            this?.putInt(getString(R.string.saved_breaktime_hours), breakTimeHours)
            this?.putInt(getString(R.string.saved_breaktime_minutes), breakTimeMinutes)
            this?.apply()
        }
    }
}
