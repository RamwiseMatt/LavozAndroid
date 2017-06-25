package com.ramwise.lavoz

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.view.WindowManager
import com.ramwise.lavoz.di.components.DaggerNetworkComponent
import com.ramwise.lavoz.di.components.NetworkComponent
import com.ramwise.lavoz.di.modules.ApplicationModule
import com.ramwise.lavoz.di.modules.NetworkModule
import com.ramwise.lavoz.LavozConstants
import net.danlew.android.joda.JodaTimeAndroid
import org.joda.time.DateTimeZone
import com.kobakei.ratethisapp.RateThisApp





class LavozApplication : Application() {
    companion object {
        lateinit var networkComponent: NetworkComponent

        lateinit var app: LavozApplication
        lateinit var windowManager: WindowManager
        lateinit var context: Context
        lateinit var shared: SharedPreferences
    }

    override fun onCreate() {
        super.onCreate()

        app = this
        context = app.applicationContext
        shared = PreferenceManager.getDefaultSharedPreferences(context)
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        JodaTimeAndroid.init(this)
        DateTimeZone.setDefault(DateTimeZone.UTC)

        networkComponent = DaggerNetworkComponent.builder()
                .applicationModule(ApplicationModule(this))
                .networkModule(NetworkModule(LavozConstants.LAVOZ_API_BASE_URL))
                .build()

        val rateThisAppConfig = RateThisApp.Config(7, 7)
        rateThisAppConfig.setMessage(R.string.message_rate_us_now)
        RateThisApp.init(rateThisAppConfig)
    }
}