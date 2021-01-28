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

// TODO: Add persistence to save Work/Break time settings

// TODO: Prevent the app from crashing when the user taps the back button while on MainActivity.
//       When the app is started and then the back button is pressed, the app crashes due to the
//       displayTimer not being assigned it's corresponding Object.

var breakTimeHours: Long = 0L
var breakTimeMinutes: Long = 5L
var workTimeHours: Long = 0L
var workTimeMinutes: Long = 25L

lateinit var displayTimer: TextView

class MainActivity : AppCompatActivity() {

    lateinit var countDownTimer: CountDownTimer
    private val CHANNEL_ID = "tomato_timer_channel"

    private var isBreakTime: Boolean = false
    private var hasLeftApp: Boolean = false
    private var isTimerCounting: Boolean = false
    private var timeRemaining: Long = 0L

    private lateinit var infoText: TextView

    lateinit var builder: NotificationCompat.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        displayTimer = findViewById(R.id.timerDisplay)
        infoText = findViewById(R.id.info_text)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnSetWorkDuration = findViewById<Button>(R.id.btn_work_duration)
        val btnSetBreakDuration = findViewById<Button>(R.id.btn_break_duration)

        displayTimer.text = presentSetTime(workTimeHours, workTimeMinutes)

        btnStart.setOnClickListener {
            it as Button

            when (it.text) {

                "Start" -> {
                    countDownTimer = beginTimer(workTimeHours, workTimeMinutes)
                    countDownTimer.start()
                    isTimerCounting = true
                    it.text = "Stop"
                    infoText.text = getString(R.string.text_info_timer_working)
                    // Upon tapping the button when it says "Start" turn the button red
                    it.setBackgroundColor(Color.rgb(244, 67, 54))
                    // Change textInfo to let user know how to pause the timer.
                    infoText.text = getText(R.string.text_info_timer_working)
                    // Grey out the set Work/Break buttons
                    turnOffButtons(
                        btnSetWorkDuration,
                        btnSetBreakDuration,
                        true
                    )
                    if (hasLeftApp) hasLeftApp = false
                }

                "Stop" -> {
                    stopTimer()
                    isTimerCounting = false
                    // "Zero" the time back to the set time
                    displayTimer.text = presentSetTime(workTimeHours, workTimeMinutes)
                    // Set the button text to "Start"
                    it.text = "Start"
                    // Set textInfo
                    infoText.text = getString(R.string.text_info_ready_to_start)
                    // Change the background color of the button to Green
                    it.setBackgroundColor(Color.rgb(76, 175, 80))
                    // Re-color the set Work/Break buttons
                    turnOffButtons(
                        btnSetWorkDuration,
                        btnSetBreakDuration,
                        false
                    )
                    if (hasLeftApp) hasLeftApp = false
                }

                "Resume" -> {
                    // change the button text to "Stop"
                    it.text = "Stop"
                    infoText.text = getString(R.string.text_info_timer_working)
                    // Upon tapping the button when it says "Start" turn the button red
                    it.setBackgroundColor(Color.rgb(244, 67, 54))
                    // Call the resumeTimer()
                    countDownTimer = resumeTimer(timeRemaining)
                    countDownTimer.start()
                    isTimerCounting = true
                    // turn off the setter buttons
                    turnOffButtons(
                        btnSetWorkDuration,
                        btnSetBreakDuration,
                        true
                    )
                    if (hasLeftApp) hasLeftApp = false
                }
            }
        }

        btnStart.setOnLongClickListener {
            it as Button
            // Pause button feature
            when (it.text) {
                "Stop" -> {
                    if (isTimerCounting) {
                        countDownTimer.cancel()
                        isTimerCounting = false
                        it.text = "Resume"
                        it.setBackgroundColor(Color.BLUE)
                        infoText.text = getString(R.string.text_info_ready_to_resume)
                        Log.i(TAG, "Stopped timer at will.")
                    }
                }
            }

            true
        }

        btnSetWorkDuration.setOnClickListener {
            timeSetupDialog(it)
        }

        btnSetBreakDuration.setOnClickListener {
            timeSetupDialog(it)
        }

        createNotificationChannel()
        builder = notificationAlert()
    }

    override fun onBackPressed() {
        // Do nothing, this is to prevent the app from crashing due to displayTimer not having
        // a TextView reference when android call onPause().
    }

    override fun onResume() {
        super.onResume()

        if (hasLeftApp  && isBreakTime == false) {
            // Set the button text to "Resume", onCreate has the logic for resuming time.
            val button = findViewById<Button>(R.id.btnStart)
            button.text = "Resume"
            // Set the button color to Blue
            button.setBackgroundColor(Color.BLUE)
            // User is back in the app
            hasLeftApp = false
            Log.i(
                TAG,
                "onResume(): Has set button text to 'Resume' and remaining time is: $timeRemaining"
            )
        }

        Log.i(TAG, "onResume(): hasLeftApp = false")
    }

    override fun onPause() {
        super.onPause()

        hasLeftApp = true
        Log.i(TAG, "onPause(): hasLeftApp = true ")

        // If user leaves app while on break, spawn notification with break-timer continuing
        if (isBreakTime && isTimerCounting) {
            // TODO: Make a notification showing the break-timer countdown
            Log.i(TAG, "onPause(): Spawning remaining time in a notification")
        }

        // User is working and then leaves app, remind them to comeback with a dropdown Notification
        if (isBreakTime == false && isTimerCounting) {
            stopTimer()
            isTimerCounting = false
            val notificationId: Int = Random.nextInt()
            // Works every now and then, make notificationId more reliable
            with(NotificationManagerCompat.from(this)) {
                // notificationId is a unique int for each notification that you must define
                notify(notificationId, builder.build())
            }
        }

        Log.i(TAG, "onPause(): Stopped timer and is showing notification")
    }

    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
    //                         START/STOP/RESUME TIMER METHODS                            //
    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
    private fun beginTimer(hours: Long, minutes: Long): CountDownTimer {

        if (hours == 0L && minutes == 0L) {
            // return the base work timer
            return beginTimer(0L, 25L)
        }

        val hourInMillis = hours * 3_600_000L
        val minInMillis = minutes * 60_000L
        val totalTime = hourInMillis + minInMillis

        val timer = object : CountDownTimer(totalTime, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemaining = millisUntilFinished
                Log.i(
                    TAG, """
                    beginTimer()
                    workTimeRemaining: $timeRemaining
                    ________________________________________________
                """.trimIndent()
                )
                updateTextUI()

            }

            override fun onFinish() {
                soundTimer()
                isBreakTime = true
                infoText.text = getString(R.string.text_info_break)
                countDownTimer = breakTimer(breakTimeHours, breakTimeMinutes)
                countDownTimer.start()
                Log.i(TAG, "Break time has started")
            }
        }

        return timer
    }

    private fun resumeTimer(timeLeft: Long): CountDownTimer {

        val timer = object : CountDownTimer(timeLeft, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                this@MainActivity.timeRemaining = millisUntilFinished
                Log.i(
                    TAG, """
                    resumeTimer()
                    workTimeRemaining: ${this@MainActivity.timeRemaining}
                    ________________________________________________
                """.trimIndent()
                )
                timeRemaining = millisUntilFinished
                updateTextUI()
            }

            override fun onFinish() {
                soundTimer()
                if (isBreakTime) {
                    isBreakTime = false
                    infoText.text = getString(R.string.text_info_timer_working)
                    // Start the break timer
                    countDownTimer = beginTimer(workTimeHours, workTimeMinutes)
                    countDownTimer.start()
                    beginTimer(workTimeHours, workTimeMinutes)
                } else {
                    isBreakTime = true
                    infoText.text = getString(R.string.text_info_break)
                    // Start the break timer
                    countDownTimer = breakTimer(breakTimeHours, breakTimeMinutes)
                    countDownTimer.start()
                }
            }
        }
        return timer
    }

    private fun breakTimer(hours: Long, minutes: Long): CountDownTimer {

        if(hours == 0L && minutes == 0L) {
            return breakTimer(0L, 5L)
        }

        val hourInMillis = hours * 3_600_000L
        val minInMillis = minutes * 60_000L
        val totalTime = hourInMillis + minInMillis

        val timer = object : CountDownTimer(totalTime, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemaining = millisUntilFinished
                Log.i(
                    TAG, """
                    breakTimer()
                    Time left: $millisUntilFinished ms
                    ________________________________________________
                """.trimIndent()
                )
                updateTextUI()
            }

            override fun onFinish() {
                soundTimer()
                isBreakTime = false
                // Start working timer
                infoText.text = getString(R.string.text_info_timer_working)
                countDownTimer = beginTimer(workTimeHours, workTimeMinutes)
                countDownTimer.start()
            }
        }
        return timer
    }

    // made for stopping the timer and checking if there is a current break happening to reset it
    private fun stopTimer() {

        // TODO: Work on this logic
        if(isBreakTime && hasLeftApp) {
            // Let the timer keep counting, it's their break
        }

        if (isBreakTime) {
            countDownTimer.cancel()
            isTimerCounting = false
            isBreakTime = false
        } else {
            countDownTimer.cancel()
            isTimerCounting = false
        }
    }

    private fun turnOffButtons(button1: Button, button2: Button, turnThemOff: Boolean) {

        if (turnThemOff) {
            // make both buttons un-clickable
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

        if (isBreakTime) {
            val hours = (timeRemaining / (1000 * 60 * 60)) % 24
            val minute = (timeRemaining / (1000 * 60)) % 60
            val seconds = (timeRemaining / 1000) % 60

            displayTimer.text = timeFormat(hours, minute, seconds)
        } else {
            val hours = (timeRemaining / (1000 * 60 * 60)) % 24
            val minute = (timeRemaining / (1000 * 60)) % 60
            val seconds = (timeRemaining / 1000) % 60

            displayTimer.text = timeFormat(hours, minute, seconds)
        }
    }

    private fun timeFormat(hour: Long, minute: Long, second: Long): String {
        var hFormat = hour.toString()
        var mFormat = minute.toString()
        var sFormat = second.toString()

        if (hFormat.length == 1) hFormat = "0$hour"
        if (mFormat.length == 1) mFormat = "0$minute"
        if (sFormat.length == 1) sFormat = "0$second"

        return "$hFormat:$mFormat:$sFormat"
    }

    private fun presentSetTime(hour: Long, minute: Long): String {
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

    /*
    * Get this to bring the user back to the app with it's state the same as it was
    * so  that the onResume method can work as intended.
    */
    fun notificationAlert(): NotificationCompat.Builder {

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("Tomato Timer")
            .setContentText("Come back and work!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // Notification removes itself upon user interaction.
            .setAutoCancel(true)
        return builder
    }

    /* TODO: Make the notification take the user back into the app,
             then make the notification go away after the press.
             Also, make the notification announce the break starting and ending.
    */
    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
    //                              CUSTOM DIALOG SETUP                                   //
    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
    private fun timeSetupDialog(view: View) {
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

}