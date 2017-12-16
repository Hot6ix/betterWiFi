package com.simples.j.betterwifi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log.i
import kotlinx.android.synthetic.main.main.*

class Main : AppCompatActivity() {

    private val request = 0
    public var isServiceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), request)
        }
        else {
            i(applicationContext.packageName, "Permission was granted")
        }

        button1.setOnClickListener {
            if(isServiceRunning) {
                stopService(Intent(this, BackgroundService::class.java))
                isServiceRunning = false
            }
            else {
                startService(Intent(this, BackgroundService::class.java))
                isServiceRunning = true
            }

            button1.text = isServiceRunning.toString()
        }
        button1.text = isServiceRunning.toString()

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            request -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted
                    i(applicationContext.packageName, "Permission was granted")
                }
                else {
                    // Permission denied
                    i(applicationContext.packageName, "Permission denied")
                }
                return
            }
        }
    }
}
