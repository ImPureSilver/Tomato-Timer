package org.pur3.tomatotimer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.random.Random

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
    private val CHANNEL_ID = "tomato_timer_channel"

    private var workTimeRemaining: Long = 0L

    var builder = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_tomato)
        .setContentTitle("Tomato Timer")
        .setContentText("Come back and work!")
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    private var isBreakTime: Boolean = false
    private var hasLeftApp: Boolean = false
    private var time_in_milli_seconds = (workTimeMinutes * 60_000L) + (workTimeHours * 5_000L)

    private val baseTimerDuration: Long = 1L
    private val baseBreakTimerDuration: Long = 1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        displayTimer = findViewById(R.id.timerDisplay)
        // Views of the MainActivity
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnSetWorkDuration = findViewById<Button>(R.id.btn_work_duration)
        val btnSetBreakDuration = findViewById<Button>(R.id.btn_break_duration)

        displayTimer.setOnClickListener {
            // FOR DEBUGGING THE WORK/BREAK DURATION BUTTONS
            showValues()
        }

        displayTimer.text = presentSetTime(workTimeHours, workTimeMinutes)

        btnStart.setOnClickListener {
            it as Button

            when (it.text) {

                "Start" -> {
                    countDownTimer = beginTimer(workTimeHours, workTimeMinutes)
                    countDownTimer.start()
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
                    stopTimer()
                    // "Zero" the time back to the user set time
                    displayTimer.text = presentSetTime(workTimeHours, workTimeMinutes)
                    // Set the button text to "Start"
                    it.text = "Start"
                    // Change the background color of the button to Green
                    it.setBackgroundColor(Color.rgb(76, 175, 80))
                    // Re-color the set Work/Break buttons
                    turnOffButtons(
                        btnSetWorkDuration,
                        btnSetBreakDuration,
                        false
                    )
                }

                "Resume" -> {
                    // Show the time left from time in milli seconds
                    // Call the resumeTimer()
                    countDownTimer = resumeTimer(workTimeRemaining)
                    countDownTimer.start()
                    // change the button text to "Stop"
                    it.text = "Stop"
                    // turn off the setter buttons
                    turnOffButtons(
                        btnSetWorkDuration,
                        btnSetBreakDuration,
                        true
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

        createNotificationChannel()
    }

    override fun onResume() {
        super.onResume()

        if (hasLeftApp == true && isBreakTime == false) {
            // Set the button text to "Resume", onCreate has the logic for resuming time.
            val button = findViewById<Button>(R.id.btnStart)
            button.text = "Resume"
            // Set the button color to Blue
            button.setBackgroundColor(Color.BLUE)
        }

        Log.i(TAG, "onResume(): Has set button text to 'Resume' and remaining time is: $workTimeRemaining")
    }

    override fun onPause() {
        super.onPause()

        hasLeftApp = true
        stopTimer()
        val notificationId: Int = Random.nextInt()
        // Works every now and then, make notificationId more reliable
        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(notificationId, builder.build())
        }

        Log.i(TAG, "onPause(): Stopped timer and is showing notification")
    }

    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
    //                         START/STOP/RESUME TIMER METHODS                            //
    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
    private fun beginTimer(hours: Long, minutes: Long = baseTimerDuration): CountDownTimer {

        val hourInMillis = hours * 3_600_000L
        val minInMillis = minutes * 60_000L
        val totalTime = hourInMillis + minInMillis

        val timer = object : CountDownTimer(totalTime, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                time_in_milli_seconds = millisUntilFinished
                workTimeRemaining = millisUntilFinished
                Log.i(TAG, """
                    beginTimer()
                    time_in_milli_seconds left: $time_in_milli_seconds
                    workTimeRemaining: $workTimeRemaining
                    ________________________________________________
                """.trimIndent())
                updateTextUI()

            }

            override fun onFinish() {
                soundTimer()
                isBreakTime = true
                countDownTimer = breakTimer(breakTimeHours, breakTimeMinutes)
                countDownTimer.start()
                Log.i(TAG, "Break time has started")
            }
        }

        return timer
    }

    private fun resumeTimer(timeRemainingInMilliSecs: Long): CountDownTimer {

        val hours = (timeRemainingInMilliSecs / (1000 * 60 * 60)) % 24
        val minute = (timeRemainingInMilliSecs / (1000 * 60)) % 60
        val seconds = (timeRemainingInMilliSecs / 1000) % 60

        val totalTime = hours + minute + seconds

        val timer = object : CountDownTimer(totalTime, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                time_in_milli_seconds = millisUntilFinished
                workTimeRemaining = millisUntilFinished
                Log.i(TAG, """
                    resumeTimer()
                    time_in_milli_seconds left: $time_in_milli_seconds
                    workTimeRemaining: $workTimeRemaining
                    ________________________________________________
                """.trimIndent())
                updateTextUI()
            }

            override fun onFinish() {
                soundTimer()
                isBreakTime = true
                countDownTimer = beginTimer(workTimeHours, workTimeMinutes)
                countDownTimer.start()
            }
        }
        return timer
    }

    private fun breakTimer(hours: Long, minutes: Long = baseBreakTimerDuration): CountDownTimer {

        val hourInMillis = hours * 3_600_000L
        val minInMillis = minutes * 60_000L
        val totalTime = hourInMillis + minInMillis

        val timer = object : CountDownTimer(totalTime, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                time_in_milli_seconds = millisUntilFinished
                Log.i(TAG, """
                    breakTimer()
                    time_in_milli_seconds left: $time_in_milli_seconds
                    ________________________________________________
                """.trimIndent())
                updateTextUI()
            }

            override fun onFinish() {
                soundTimer()
                isBreakTime = false
                countDownTimer = beginTimer(workTimeHours, workTimeMinutes)
                countDownTimer.start()
            }
        }
        // i forgot why this worked, ill leave it i guess...
//        isBreakTime = true
        return timer
    }

    // made for stopping the timer while checking if there is a current break happening
    private fun stopTimer() {

        if (isBreakTime) {
            countDownTimer.cancel()
            isBreakTime = false
        } else {
            countDownTimer.cancel()
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

    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
    //                          DISPLAY CLOCK FORMAT METHODS                              //
    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
    private fun updateTextUI() {
        val hours = (time_in_milli_seconds / (1000 * 60 * 60)) % 24
        val minute = (time_in_milli_seconds / (1000 * 60)) % 60
        val seconds = (time_in_milli_seconds / 1000) % 60

        displayTimer.text = timeFormat(hours, minute, seconds)
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

    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
    //                               NOTIFICATION METHODS                                 //
    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
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

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun notificationAlert(textTitle: String, textContent: String) {
        val notificationBuilder =
            NotificationCompat.Builder(this, NotificationChannel.DEFAULT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(textTitle)
                .setContentText(textContent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notifiId = Random.nextInt()

        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(notifiId, notificationBuilder.build())
        }
    }

    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
    //                              CUSTOM DIALOG SETUP                                   //
    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
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
}