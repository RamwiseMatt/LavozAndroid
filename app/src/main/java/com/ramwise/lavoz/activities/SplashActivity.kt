package com.ramwise.lavoz.activities

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.R
import com.ramwise.lavoz.fragments.HomeFragment
import com.ramwise.lavoz.network.AuthenticationService
import javax.inject.Inject
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric


class SplashActivity : AppCompatActivity() {

    // The authService is needed here to determine which fragment to show. A viewModel has not been
    // created yet, so the decision cannot be made there.
    @Inject lateinit var authService: AuthenticationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)

        LavozApplication.networkComponent.inject(this)

        Fabric.with(this, Crashlytics())

        if (authService.isAuthenticated()) {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        } else {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}