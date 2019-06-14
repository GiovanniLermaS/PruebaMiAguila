package co.com.appmovil.Broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import co.com.prueba.Global.GlobalApp

class GpsReceiver : BroadcastReceiver() {

    companion object {
        var gpsReceiverListener: GpsReceiverListener? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (gpsReceiverListener != null) {
            gpsReceiverListener!!.onGpsConnectionChanged(isEnabled)
        }
    }

    fun isEnabled(): Boolean {
        val manager =
            GlobalApp().getInstance()!!.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    interface GpsReceiverListener {
        fun onGpsConnectionChanged(isEnabled: Boolean)
    }
}


