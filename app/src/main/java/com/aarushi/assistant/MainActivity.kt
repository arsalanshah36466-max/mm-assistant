package com.aarushi.assistant

import android.accessibilityservice.AccessibilityService
import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this).apply {
            text = "आरुषि वॉइस असिस्टेंट एक्टिव है।\n\n1. Accessibility ऑन रखें\n2. 'No Restrictions' बैटरी परमिशन दें ताकि होम या लॉक स्क्रीन पर भी सुने।"
            textSize = 18f
            setPadding(40, 80, 40, 40)
        }
        val btn = Button(this).apply {
            text = "सर्विस स्टार्ट करें"
            setOnClickListener {
                val serviceIntent = Intent(this@MainActivity, AarushiBackgroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            }
        }
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(tv)
            addView(btn)
        }
        setContentView(layout)
    }
}

class AarushiBackgroundService : Service(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        startForegroundNotification()
        initListener()
    }

    private fun startForegroundNotification() {
        val channelId = "aarushi_voice"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, "Aarushi Voice Listener", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(chan)
        }
        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Aarushi Assistant Active")
            .setContentText("आरुषि आपकी आवाज़ सुन रही है...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
        startForeground(101, notif)
    }

    private fun initListener() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
        }
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val cmd = matches[0].lowercase()
                    handleCommand(cmd)
                }
                initListener()
            }
            override fun onError(error: Int) { initListener() }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    private fun handleCommand(cmd: String) {
        if (cmd.contains("आरुषि") || cmd.contains("aarushi")) {
            speak("जी, कहिए")
        }
        if (cmd.contains("instagram") || cmd.contains("इंस्टाग्राम")) {
            val launchIntent = packageManager.getLaunchIntentForPackage("com.instagram.android")
            launchIntent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(it)
            }
            speak("इंस्टाग्राम ओपन कर दिया है")
        } else if (cmd.contains("कॉल") || cmd.contains("call") || cmd.contains("पापा") || cmd.contains("father")) {
            speak("पापा को कॉल लगा रही हूँ")
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(dialIntent)
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("hi", "IN")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

class AarushiAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val rootNode = rootInActiveWindow ?: return
        clickButtonByText(rootNode, "Like")
        clickButtonByText(rootNode, "लाइक")
        clickButtonByText(rootNode, "Comment")
    }

    private fun clickButtonByText(node: AccessibilityNodeInfo, text: String) {
        val nodes = node.findAccessibilityNodeInfosByText(text)
        for (n in nodes) {
            n.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    override fun onInterrupt() {}
}
