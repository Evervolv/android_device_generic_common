/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.usb.testing.debuggertool.datasignal

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.hardware.usb.UsbManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val USB_PORT_CLASS_NAME = "android.hardware.usb.UsbPort"
const val USB_PORT_STATUS_CLASS_NAME = "android.hardware.usb.UsbPortStatus"
const val USB_PORT_STATUS_GET_DATA_STATUS_METHOD = "getUsbDataStatus"
const val POLLING_MILLIS = 1000L
const val GET_USB_HAL_METHOD = "getUsbHalVersion"
const val ENABLE_USB_DATA_SIGNAL_METHOD = "enableUsbDataSignal"
const val IS_PORT_DISABLED_METHOD = "isPortDisabled"
const val GET_PORTS_METHOD = "getPorts"
const val ACTION_USB_PORT_CHANGED = "android.hardware.usb.action.USB_PORT_CHANGED"
const val USB_EXTRA_PORT_STATUS = "portStatus"
const val DATA_STATUS_DISABLED_FORCE = 8

val REQUIRED_METHODS =
    setOf(
        GET_USB_HAL_METHOD,
        ENABLE_USB_DATA_SIGNAL_METHOD,
        IS_PORT_DISABLED_METHOD,
        GET_PORTS_METHOD,
    )
const val TAG = "USB_DEBUG_TEST"

class MainActivity : AppCompatActivity() {
    private val usbPortStatuses = mutableMapOf<String, Boolean>()
    private val logDivider = "\n" + "-".repeat(25) + "\n"

    private val mBroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (ACTION_USB_PORT_CHANGED == intent.action) {
                    val portStatus =
                        intent.getParcelableExtra(
                            USB_EXTRA_PORT_STATUS,
                            Class.forName(USB_PORT_STATUS_CLASS_NAME),
                        )
                    // Validation for case
                    val portStatusString =
                        Class.forName(USB_PORT_STATUS_CLASS_NAME)
                            .getDeclaredMethod("toString")
                            .invoke(portStatus) as String
                    val portStatusDataStatus =
                        Class.forName(USB_PORT_STATUS_CLASS_NAME)
                            .getDeclaredMethod(USB_PORT_STATUS_GET_DATA_STATUS_METHOD)
                            .invoke(portStatus) as Int
                    // temp for now to only test against devices with 1 USB
                    if (
                        DATA_STATUS_DISABLED_FORCE == portStatusDataStatus &&
                            usbPortStatuses.size == 1 &&
                            usbPortStatuses.any { it.value }
                    ) {
                        usbPortEventEvenWhenDataDisabledOccurred = true
                        mStatusText.setTextColor(Color.GREEN)
                        log("Usb event detected when data disabled. Confirmed behavior.", true)
                    }
                    log(portStatusString, true)
                } else if (
                    intent.action in
                        setOf(
                            UsbManager.ACTION_USB_DEVICE_ATTACHED,
                            UsbManager.ACTION_USB_ACCESSORY_ATTACHED,
                        )
                ) {
                    log(intent.action + " event caught", true)
                }
            }
        }

    private lateinit var mStatusText: TextView
    private lateinit var mBatteryCurrentText: TextView
    private lateinit var mUsbManager: UsbManager
    private lateinit var mBatteryManager: BatteryManager
    private lateinit var mErrorTextView: TextView

    private var job: Job? = null
    private var portStatusString: CharSequence = ""
    private var usbPortEventEvenWhenDataDisabledOccurred = false

    @SuppressLint("SetTextI18n")
    private fun log(message: String, debug: Boolean = false) {
        val logType = if (debug) "DEBUG:" else "EXCEPTION:"
        mErrorTextView.text = mErrorTextView.text.toString() + logType + message + logDivider
        Log.d(TAG, message)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        mUsbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        mBatteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        try {
            mErrorTextView = findViewById<View>(R.id.error_text) as TextView
            mErrorTextView.movementMethod = ScrollingMovementMethod()
            mStatusText = findViewById<View>(R.id.usb_data_status_text) as TextView
            mBatteryCurrentText = findViewById<View>(R.id.battery_current_text) as TextView
            val usbHalVersionTextview = findViewById<View>(R.id.hal_version_text) as TextView

            checkApiAvailability()
            usbHalVersionTextview.text = getUsbHalVersion().toString()
            job =
                CoroutineScope(Job() + Handler(Looper.getMainLooper()).asCoroutineDispatcher())
                    .launch {
                        try {
                            while (true) {
                                mStatusText.text = formattedUsbDataStatus()
                                if (mStatusText.text.equals(portStatusString)) {
                                    log(
                                        "usbPortStatus changed: mStatusText: ${mStatusText.text}",
                                        true,
                                    )
                                }
                                mBatteryCurrentText.text =
                                    (mBatteryManager.getIntProperty(
                                            BatteryManager.BATTERY_PROPERTY_CURRENT_NOW
                                        ) / 1000)
                                        .toString()
                                delay(POLLING_MILLIS)
                            }
                        } catch (ex: Exception) {
                            log("${ex.message} ${ex.stackTraceToString()}")
                        }
                    }
            val intentFilter = IntentFilter()
            intentFilter.addAction(ACTION_USB_PORT_CHANGED)
            intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            intentFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
            registerReceiver(mBroadcastReceiver, intentFilter)
        } catch (ex: Exception) {
            log("${ex.message} ${ex.stackTraceToString()}")
        }
    }

    override fun onStop() {
        super.onStop()
    }

    fun toggleUsbOn(v: View) {
        try {
            setUsbDataSignal(true)
        } catch (ex: Exception) {
            log("${ex.message} ${ex.stackTraceToString()}")
        }
        log("Attempt to turn on USB occurred", true)
        Toast.makeText(this, "Attempting to turn ON USB data", Toast.LENGTH_LONG).show()
    }

    fun toggleUsbOff(v: View) {
        try {
            setUsbDataSignal(false)
        } catch (ex: Exception) {
            log("${ex.message} ${ex.stackTraceToString()}")
        }
        log("Attempt to turn off USB occurred", true)
        Toast.makeText(this, "Attempting to turn OFF USB data", Toast.LENGTH_LONG).show()
    }

    private fun getUsbHalVersion(): Int {
        val halVersion =
            UsbManager::class.java.getMethod(GET_USB_HAL_METHOD).invoke(mUsbManager) as Int
        return halVersion
    }

    private fun checkApiAvailability() {
        val declaredMethodNames = UsbManager::class.java.declaredMethods.map { it.name }
        val methodNames = UsbManager::class.java.methods.map { it.name }
        val missingMethods =
            REQUIRED_METHODS.filterNot { method ->
                declaredMethodNames.any { it == method } or methodNames.any { it == method }
            }
        if (missingMethods.isNotEmpty()) {
            val sb = StringBuilder()
            sb.append("Not all required found: Missing methods: $missingMethods \n")
            sb.append("Current API methods:\n")
            UsbManager::class.java.methods.map { sb.append(it.name).append("\n") }
            sb.append("Current API declared methods:\n")
            UsbManager::class.java.declaredMethods.map { sb.append(it.name).append("\n") }
            throw Exception(sb.toString())
        }
    }

    private fun setUsbDataSignal(enable: Boolean): Boolean {
        val result =
            UsbManager::class
                .java
                .getMethod(ENABLE_USB_DATA_SIGNAL_METHOD, Boolean::class.java)
                .invoke(mUsbManager, enable) as Boolean
        return result
    }

    @SuppressLint("PrivateApi", "SoonBlockedPrivateApi")
    private fun formattedUsbDataStatus(): String {
        val sb = StringBuilder()
        val ports =
            UsbManager::class.java.getMethod(GET_PORTS_METHOD).invoke(mUsbManager) as List<*>

        for (port in ports) {
            val portId =
                Class.forName(USB_PORT_CLASS_NAME).getDeclaredMethod("getId").invoke(port) as String
            sb.append(portId).append(": ")
            val method =
                UsbManager::class
                    .java
                    .getDeclaredMethod(IS_PORT_DISABLED_METHOD, Class.forName(USB_PORT_CLASS_NAME))
            method.isAccessible = true
            val enabled = if (method.invoke(mUsbManager, port) as Boolean) "disabled" else "enabled"
            usbPortStatuses[portId] = enabled == "enabled"
            sb.append(enabled)
            sb.append("\n")
        }
        return sb.toString()
    }
}
