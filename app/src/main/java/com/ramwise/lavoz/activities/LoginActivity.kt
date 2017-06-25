package com.ramwise.lavoz.activities

import android.content.Intent
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.widget.TextView
import com.jakewharton.rxbinding.widget.text
import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.R
import com.ramwise.lavoz.fragments.HomeFragment
import com.ramwise.lavoz.fragments.LoginFragment
import com.ramwise.lavoz.network.AuthenticationService
import com.ramwise.lavoz.utils.rx.addDisposableTo
import com.ramwise.lavoz.utils.rx.ui
import com.ramwise.lavoz.viewmodels.activities.LoginActivityViewModel
import kotlinx.android.synthetic.main.activity_home.*
import javax.inject.Inject


class LoginActivity : BaseActivity() {

    lateinit var viewModel: LoginActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        supportFragmentManager
                .beginTransaction()
                .add(R.id.fl_container, LoginFragment(), "mainLoginFragment")
                .commit()
    }

    override fun createViewModel() {
        viewModel = LoginActivityViewModel()
    }

    override fun configureUIObservers() {
        viewModel.navigateToHomeActivity
                .subscribe {
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                }
                .addDisposableTo(ephemeralBag)
    }
}
