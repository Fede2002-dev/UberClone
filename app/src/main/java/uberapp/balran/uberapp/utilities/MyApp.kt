package uberapp.balran.uberapp.utilities

import android.app.Application
import android.content.Context

class MyApp : Application() {
    override fun onCreate() {
        instance = this
        super.onCreate()
    }

    companion object {
        var instance: MyApp? = null
            private set

        @JvmStatic
        val context: Context?
            get() = instance
    }
}