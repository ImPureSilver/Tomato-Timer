package org.pur3.tomatotimer

import android.app.Notification
import android.app.NotificationChannel
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat

private const val TAG = "MainActivity"

// TODO: Make a notification banner that displays the countdown when the user leaves the app
// TODO: Add persistence to save Work/Break time settings
var breakTimeHours: Long = 0L
var breakTimeMinutes: Long = 5L
var workTimeHours: Long = 0L
var workTimeMinutes: Long = 25L

lateinit var displayTimer: TextView

class MainActivity : AppCompatActivity() {

    lateinit var countDownTimer: CountDownTimer

    var isBreakTime: Boolean = false;
    var time_in_milli_seconds = (workTimeMinutes * 60_000L) + (workTimeHours * 5_000L)

    val baseTimerDuration: Long = 1L
    val baseBreakTimerDuration: Long = 1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        displayTimer = findViewById(R.id.timerDisplay)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnSetWorkDuration = findViewById<Button>(R.id.btn_work_duration)
        val btnSetBreakDuration = findViewById<Button>(R.id.btn_break_duration)

        displayTimer.setOnClickListener {
            // FOR DEBUGGING THE WORK/BREAK DURATION BUTTONS
            showValues()
            notificationAlert("TEST ALERT", "TEXT CONTENT")
        }

        displayTimer.text = presentSetTime(workTimeHours, workTimeMinutes)

        btnStart.setOnClickListener {
            it as Button

            when (it.text) {

                "Start" -> {
                    beginTimer(workTimeHours, workTimeMinutes)
                    it.text = "Stop"
                    it.setBackgroundColor(Color.rgb(244, 67, 54))
                    // Grey out the set Work/Break buttons
                    turnOffButtons(
                        btnSetWorkDuration,
                        btnSetBreakDuration,
                        true
                    )
                }

                "Stop" -> {
                    pauseTimer()
                    displayTimer.text = presentSetTime(workTimeHours, workTimeMinutes)
//                    displayTimer.text = "00:00:00"
                    it.text = "Start"
                    it.setBackgroundColor(Color.rgb(76, 175, 80))
                    // Re-color the set Work/Break buttons
                    turnOffButtons(
                        btnSetWorkDuration,
                        btnSetBreakDuration,
                        false
                    )
                }
            }
        }

        btnSetWorkDuration.setOnClickListener {
            timeSetupDialog(it)
        }

        btnSetBreakDuration.setOnClickListener {
            timeSetupDialog(it)
        }
    }

    private fun beginTimer(hours: Long, minutes: Long = baseTimerDuration) {

        val hourInMillis = hours * 3_600_000L
        val minInMillis = minutes * 60_000L
        val totalTime = hourInMillis + minInMillis

        countDownTimer = object : CountDownTimer(totalTime, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                time_in_milli_seconds = millisUntilFinished
                updateTextUI()
            }

            override fun onFinish() {
                soundTimer()
                isBreakTime = true
                breakTimer(breakTimeHours, breakTimeMinutes)
            }
        }

        countDownTimer.start()
    }

    private fun breakTimer(hours: Long, minutes: Long = baseBreakTimerDuration) {

        val hourInMillis = hours * 3_600_000L
        val minInMillis = minutes * 60_000L
        val totalTime = hourInMillis + minInMillis

        countDownTimer = object : CountDownTimer(totalTime, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                time_in_milli_seconds = millisUntilFinished
                updateTextUI()
            }

            override fun onFinish() {
                soundTimer()
                isBreakTime = false
                beginTimer(workTimeHours, workTimeMinutes)
            }
        }

        countDownTimer.start()
        isBreakTime = true
    }

    private fun pauseTimer() {
        countDownTimer.cancel()
        isBreakTime = false
    }

    private fun updateTextUI() {
        val hours = (time_in_milli_seconds / (1000 * 60 * 60)) % 24
        val minute = (time_in_milli_seconds / (1000 * 60)) % 60
        val seconds = (time_in_milli_seconds / 1000) % 60

        displayTimer.text = timeFormat(hours, minute, seconds)
    }

    fun soundTimer() {
        try {
            // TODO: make this more dynamic for user flexibility of getting to choose a tone
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, notification)
            ringtone.play()
        } catch (e: Exception) {
            Log.i(TAG, e.message.toString())
        }
    }

    fun turnOffButtons(button1: Button, button2: Button, turnThemOff: Boolean) {

        if (turnThemOff) {
            // make both buttons unclickable
            button1.isClickable = false
            button2.isClickable = false
            // reduce their alpha
            button1.alpha = .5F
            button2.alpha = .5F
        } else {
            // return buttons to their normal state
            button1.isClickable = true
            button2.isClickable = true
            // increase their alpha values
            button1.alpha = 1.0F
            button2.alpha = 1.0F
        }
    }

    fun timeSetupDialog(view: View) {
        val whichButton = view as Button
        val buttonText = whichButton.text.toString()

        if (buttonText == "Work Duration") {
            // send to dialog that this was the Work Duration set button
            val dialog = TimeSetterDialog(buttonText)
            dialog.show(supportFragmentManager, "$buttonText dialog")
        } else {
            // send to dialog that this was the Break Duration set button
            val dialog = TimeSetterDialog(buttonText)
            dialog.show(supportFragmentManager, "$buttonText dialog")
        }
    }

    fun showValues() {
        Log.d(
            TAG, """
            -----------------------------------
            Work time Hours: $workTimeHours
            Work time Minutes: $workTimeMinutes
            ------------------------------------
            Break time Hours: $breakTimeHours
            Break time Minutes: $breakTimeMinutes
            -----------------------------------
        """.trimIndent()
        )
    }

    fun timeFormat(hour: Long, minute: Long, second: Long): String {
        var hFormat = hour.toString()
        var mFormat = minute.toString()
        var sFormat = second.toString()

        if (hFormat.length == 1) hFormat = "0$hour"
        if (mFormat.length == 1) mFormat = "0$minute"
        if (sFormat.length == 1) sFormat = "0$second"

        val formattedTime = "$hFormat:$mFormat:$sFormat"

        return formattedTime
    }


    fun presentSetTime(hour: Long, minute: Long): String {
        var hFormat = hour.toString()
        var mFormat = minute.toString()

        if (hFormat.length == 1) hFormat = "0$hour"
        if (mFormat.length == 1) mFormat = "0$minute"

        val formattedTime = "$hFormat:$mFormat:00"

        return formattedTime
    }

    fun notificationAlert(textTitle: String, textContent: String) {
        val builder = NotificationCompat.Builder(this, NotificationChannel.DEFAULT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(textTitle)
            .setContentText(textContent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        builder
    }

}