package com.kryptup.kryptup

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.kryptup.kryptup.BuildConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KryptupApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Point Firebase SDKs to the local emulator suite only when explicitly enabled in local.properties (USE_EMULATOR=true).
        // Android emulator uses 10.0.2.2 to reach localhost on the host machine.
        if (BuildConfig.DEBUG && BuildConfig.USE_EMULATOR) {
            FirebaseFunctions.getInstance("asia-south1").useEmulator("10.0.2.2", 5001)
            FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)
        }
    }
}
