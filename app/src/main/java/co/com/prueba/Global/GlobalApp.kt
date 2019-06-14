package co.com.prueba.Global

import android.app.Application
import android.content.Context
import android.support.multidex.MultiDex
import co.com.appmovil.Broadcast.GpsReceiver
import co.com.prueba.Broadcast.ConnectivityReceiver

class GlobalApp : Application() {

    companion object {
        var mInstance: GlobalApp? = null
    }

    override fun onCreate() {
        super.onCreate()
        mInstance = this
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    @Synchronized
    fun getInstance(): GlobalApp? {
        return mInstance
    }

    fun setConnectivityListener(listener: ConnectivityReceiver.ConnectivityReceiverListener) {
        ConnectivityReceiver.connectivityReceiverListener = listener
    }

    fun setGpsListener(listener: GpsReceiver.GpsReceiverListener) {
        GpsReceiver.gpsReceiverListener = listener
    }
}