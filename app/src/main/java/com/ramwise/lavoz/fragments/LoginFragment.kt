package com.ramwise.lavoz.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ramwise.lavoz.R
import com.ramwise.lavoz.viewmodels.fragments.LoginFragmentViewModel
import kotlinx.android.synthetic.main.fragment_login.*
import com.facebook.login.LoginResult
import android.content.Intent
import android.preference.PreferenceManager
import com.facebook.*
import com.facebook.login.widget.LoginButton
import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.LavozConstants
import com.ramwise.lavoz.network.AuthenticationService
import com.ramwise.lavoz.utils.rx.RxVariable
import com.ramwise.lavoz.utils.rx.addDisposableTo
import com.ramwise.lavoz.utils.rx.ui
import javax.inject.Inject
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.ConnectionResult
import android.support.annotation.NonNull
import android.support.v4.app.FragmentActivity
import android.widget.Toast
import android.R.attr.data
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.SignInButton


class LoginFragment : BaseFragment() {
    companion object {
        fun newInstance(): LoginFragment { return LoginFragment() }

        fun fragmentTag() : String { return "login" }

        /** Value used by Google Signin to start an activity */
        val RC_SIGN_IN = 1839
    }

    @Inject lateinit var authService: AuthenticationService

    lateinit var viewModel: LoginFragmentViewModel

    lateinit var googleApiClient: GoogleApiClient

    private var _fbAccessTokenString: RxVariable<String?>? = null
    private var _googleAuthCodeString: RxVariable<String?>? = null

    private var fbCallbackManager = CallbackManager.Factory.create()

    private var _repeatedOnResume = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LavozApplication.networkComponent.inject(this)

        if (savedInstanceState == null) {
            // Only create the googleApiClient once, else the following exception is thrown:
            // java.lang.IllegalStateException: Already managing a GoogleApiClient with id 0

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestServerAuthCode(getString(R.string.google_server_client_id), false)
                    .build()

            googleApiClient = GoogleApiClient
                    .Builder(activity)
                    .enableAutoManage(activity) {
                        // onConnectionFailedListener
                    }
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_login, container, false)

        val fbLoginButton = view.findViewById(R.id.fb_login_button) as LoginButton
        fbLoginButton.setReadPermissions("email")
        fbLoginButton.setReadPermissions("public_profile")
        fbLoginButton.fragment = this
        fbLoginButton.registerCallback(fbCallbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                // The RxVariable might not exist yet, because the view model itself
                // does not get created until onActivityCreated(), which is called AFTER
                // onCreateView()
                // It it does not exist, that is fine, because it will be instantiated with the
                // token later.
                _fbAccessTokenString?.value = AccessToken.getCurrentAccessToken()?.token
            }

            override fun onCancel() {
            }

            override fun onError(exception: FacebookException) {
            }
        })

        val googleLoginButton = view.findViewById(R.id.google_login_button) as SignInButton
        googleLoginButton.setOnClickListener { signIn() }

        return view
    }

    override fun onPause() {
        super.onPause()

        // Show the grey overlay in preparation for returning here after signing in with FB/Google
        toggleLoadingOverlay(true, 0)
    }

    override fun onResume() {
        super.onResume()

        if (AccessToken.getCurrentAccessToken() == null) {
            // Hide the overlay. Wait 3 seconds if this is a repeat load, because there is a chance
            // google sign in is about to redirect us to the homeactivity.
            toggleLoadingOverlay(false, if (!_repeatedOnResume) 0 else 3000)
        }

        if (_repeatedOnResume == false)
            _repeatedOnResume = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            // Google
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)

            handleSignInResult(result)
        } else {
            // Facebook or other
            fbCallbackManager.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun createViewModel() {
        _fbAccessTokenString = RxVariable(AccessToken.getCurrentAccessToken()?.token)
        _googleAuthCodeString = RxVariable(null)

        viewModel = LoginFragmentViewModel(
                _fbAccessTokenString!!.asObservable(),
                // Skip the first value (null), since that the placeholder value.
                _googleAuthCodeString!!.asObservable().skip(1))
    }

    override fun configureUIObservers() {
        viewModel.authToken
                .ui()
                .subscribe {
                    // Update the authToken
                    authService.authToken = it
                }
                .addDisposableTo(ephemeralBag)
    }

    private fun signIn() {
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient)

        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun handleSignInResult(result: GoogleSignInResult) {
        if (result.isSuccess) {
            // It worked, so show the overlay again. This cancels out the hide overlay action
            // done in onResume(). This construction is necessary because there is no way of knowing
            // whether the google sign in was successful upon onResume().
            toggleLoadingOverlay(true, 0)

            _googleAuthCodeString?.value = result.signInAccount?.serverAuthCode
        } else {
            toggleLoadingOverlay(false, 0)

            _googleAuthCodeString?.value = null
        }
    }

    override fun setVisibilityOnLoadingOverlay(v: Boolean) {
        still_loading_overlay?.visibility = if (v) View.VISIBLE else View.GONE
    }
}