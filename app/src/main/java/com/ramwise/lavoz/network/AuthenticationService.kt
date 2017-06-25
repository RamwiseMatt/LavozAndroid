package com.ramwise.lavoz.network

import com.facebook.login.LoginManager
import com.google.gson.Gson
import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.LavozConstants
import com.ramwise.lavoz.models.AuthToken
import com.ramwise.lavoz.models.User
import com.ramwise.lavoz.utils.rx.RxVariable
import rx.Observable
import org.joda.time.DateTime


class AuthenticationService {
    private val _authToken: RxVariable<AuthToken?>

    val gson = Gson()

    init {
        _authToken = RxVariable(_storedFromSharedPreferences())
    }

    var authToken: AuthToken?
        get() {
            return _authToken.value
        }
        set(value) {
            _storeInSharedPreferences(value)

            _authToken.value = value
        }

    /** @return A boolean indicating whether the user is authenticated. This does not necessarily
     * mean that the token is still valid, since that should always be determined server-side.
     */
    fun isAuthenticated(): Boolean {
        return _authToken.value?.isValid() ?: false
    }

    /** Removes both native tokens and any token associated with external services such as Facebook.
     *
     * Note: Google users do not stay signed in to Google with Lavoz after signing in, and therefore
     * no Google tokens need or will be deleted.
     */
    fun removeAllTokens() {
        LoginManager.getInstance().logOut()

        this.authToken = null
    }

    fun authTokenAsObservable(): Observable<AuthToken?> {
        return _authToken.asObservable()
    }

    private fun _storeInSharedPreferences(obj: AuthToken?) {
        LavozApplication.shared
                .edit()
                .putString(LavozConstants.SHARED_KEY_AUTH_TOKEN, gson.toJson(obj))
                .apply()
    }

    private fun _storedFromSharedPreferences() : AuthToken? {
        return gson.fromJson(
                LavozApplication.shared.getString(LavozConstants.SHARED_KEY_AUTH_TOKEN, null),
                AuthToken::class.java)
    }
}