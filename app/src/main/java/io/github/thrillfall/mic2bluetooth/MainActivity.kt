package io.github.thrillfall.mic2bluetooth

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.github.thrillfall.mic2bluetooth.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val loopback by lazy { AudioLoopback(applicationContext) }
    private var pendingMode: LoopbackMode? = null
    private var pttHoldMode = false

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = updateOutputLabel()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = updateOutputLabel()
    }

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val mode = pendingMode
        pendingMode = null
        if (results.values.all { it } && mode != null) {
            startLoopback(mode)
        } else {
            setStatus(getString(R.string.status_permission_denied))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.modeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val newHold = (checkedId == R.id.modeHold)
            if (newHold != pttHoldMode) {
                pttHoldMode = newHold
                if (loopback.isRunning) stopLoopback()
                applyButtonMode()
                updateUi()
            }
        }
        binding.lowLatencySwitch.setOnCheckedChangeListener { _, _ ->
            if (loopback.isRunning) stopLoopback()
            updateUi()
        }
        applyButtonMode()
        updateUi()
    }

    override fun onResume() {
        super.onResume()
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        am.registerAudioDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
        updateOutputLabel()
    }

    override fun onPause() {
        super.onPause()
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        am.unregisterAudioDeviceCallback(deviceCallback)
    }

    override fun onStop() {
        super.onStop()
        stopLoopback()
    }

    private fun applyButtonMode() {
        if (pttHoldMode) {
            binding.micButton.setOnClickListener(null)
            binding.micButton.setOnTouchListener { v, ev ->
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        v.performClick()
                        v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(80).start()
                        ensurePermissionAndStart()
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                        stopLoopback()
                        true
                    }
                    else -> false
                }
            }
        } else {
            binding.micButton.setOnTouchListener(null)
            binding.micButton.setOnClickListener {
                if (loopback.isRunning) stopLoopback() else ensurePermissionAndStart()
            }
        }
    }

    private fun selectedMode(): LoopbackMode =
        if (binding.lowLatencySwitch.isChecked) LoopbackMode.SCO_LOW_LATENCY
        else LoopbackMode.A2DP_HIGH_QUALITY

    private fun ensurePermissionAndStart() {
        val mode = selectedMode()
        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (mode == LoopbackMode.SCO_LOW_LATENCY) {
            needed += Manifest.permission.BLUETOOTH_CONNECT
        }
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            startLoopback(mode)
        } else {
            pendingMode = mode
            requestPermissions.launch(missing.toTypedArray())
        }
    }

    private fun startLoopback(mode: LoopbackMode) {
        loopback.start(mode) { msg ->
            runOnUiThread {
                setStatus(getString(R.string.status_error, msg))
                updateUi()
            }
        }
        updateUi()
    }

    private fun stopLoopback() {
        loopback.stop()
        updateUi()
    }

    private fun updateUi() {
        val running = loopback.isRunning
        val tint = ContextCompat.getColor(
            this, if (running) R.color.mic_red_active else R.color.mic_red_idle
        )
        binding.micButton.backgroundTintList = ColorStateList.valueOf(tint)

        binding.hintText.setText(
            when {
                running && pttHoldMode -> R.string.hint_active_hold
                running -> R.string.hint_active
                pttHoldMode -> R.string.hint_hold
                else -> R.string.hint_tap
            }
        )

        setStatus(
            if (running) getString(R.string.status_running)
            else getString(R.string.status_idle)
        )
        updateOutputLabel()
    }

    private fun updateOutputLabel() {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val mode = selectedMode()
        val match = if (mode == LoopbackMode.SCO_LOW_LATENCY) {
            devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
        } else {
            devices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
            }
        }
        val label = if (match != null) {
            val profile = if (mode == LoopbackMode.SCO_LOW_LATENCY) "SCO" else "A2DP"
            getString(R.string.output_bluetooth, "${match.productName ?: "Bluetooth"} ($profile)")
        } else if (mode == LoopbackMode.SCO_LOW_LATENCY) {
            getString(R.string.output_none_sco)
        } else {
            getString(R.string.output_none)
        }
        binding.outputLabel.text = label
    }

    private fun setStatus(text: String) {
        binding.statusLabel.text = text
    }
}
