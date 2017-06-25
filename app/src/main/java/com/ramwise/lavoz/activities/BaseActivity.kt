package com.ramwise.lavoz.activities

import android.content.res.Resources
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.annotation.IdRes
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.widget.Toast
import com.muddzdev.styleabletoastlibrary.StyleableToast
import com.ramwise.lavoz.R
import com.ramwise.lavoz.fragments.BaseFragment
import com.ramwise.lavoz.fragments.MotionFragment
import com.ramwise.lavoz.models.constants.ToastType
import com.ramwise.lavoz.utils.exceptions.NativeException
import com.ramwise.lavoz.viewmodels.BaseViewModel
import rx.subscriptions.CompositeSubscription
import java.util.*


/** This is the class that any Activity in this application should inherit from. It provides a
 * little bit of magic to avoid repetitive work, namely:
 *
 * - Creates CompositeSubscription objects ([ephemeralBag] and [disposeBag]).
 * - Calls `engage()`, `disengage()` and `onDestroy()` on the associated viewModel object (which
 *   should always exist by that name and be a subclass of [BaseViewModel]).
 * - Calls [createViewModel] on this object. This is where the viewModel property should be
 *   initialized, which is why it has to be a lateinit property. This is because it needs access
 *   to the View objects belonging to this Fragment, and those are not yet inflated at the time of
 *   creation of this viewModel object.
 *   Note that [createViewModel] will only be called once throughout the entire lifetime of the
 *   Activity: constructing a viewModel should therefore take into consideration that the event
 *   Observables might change over time. Something like flatMap is useful to circumvent this.
 *   This method is empty and can be overwritten by the subclass' implementation (i.e. super() need
 *   not be called).
 * - Calls [configureObsevers] and [configureUIObservers] at the appropriate times. These methods
 *   are empty and can be overwritten by the subclass' implementation
 *   (i.e. super() need not be called).
 */
abstract class BaseActivity : AppCompatActivity() {

    /** This bag of Subscriptions gets created on each [onStart()] and gets cleared after each
     * [onStop()].
     * Use this bag for quick subscriptions that should not last long.
     */
    protected var ephemeralBag = CompositeSubscription()

    /** This bag of Subscriptions gets created on each [onCreate()] and gets cleared after each
     * [onDestroy()].
     * Use this bag for subscriptions that are longer-lasting than those in [ephemeralBag].
     */
    protected var disposeBag = CompositeSubscription()

    /** A helper value to ensure that [createViewModel] only gets called once. */
    protected var _vmInitialized = false

    /** A helper value to ensure that [configureObservers] only gets called once. */
    protected var _configureObserversCalled = false

    /** A cache for _viewModel */
    protected var _viewModelCached: BaseViewModel? = null

    /** @return the viewModel property in the BaseActivity subclass, cast to [BaseViewModel]. */
    protected val _viewModel: BaseViewModel?
        get() {
            if (_viewModelCached == null) {
                try {
                    val field_ = this.javaClass.getDeclaredField("viewModel")
                    field_.isAccessible = true

                    _viewModelCached =  field_.get(this) as BaseViewModel
                } catch (e: NoSuchFieldException) {
                    throw NativeException("BaseActivity subclasses must have a viewModel property.")
                } catch (e: Exception) {
                    // In some cases, the viewModel object might not have been created yet when
                    // onDestroy gets called (i.e. when calling Activity.finish() in
                    // Activity.onCreate(). This is acceptable.
                    return null
                }
            }

            return _viewModelCached
        }

    /** Meant for use in conjunction with [toggleLoadingOverlay]. */
    private val _hudOverlayTimer = Handler()

    /** Meant for use in conjunction with [toggleLoadingOverlay]. */
    private var _hudOverlayTimerRunnable: Runnable? = null

    init {
        _hudOverlayTimerRunnable = object : Runnable {
            override fun run() { setVisibilityOnLoadingOverlay(true) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _resetForOnCreate()

        disposeBag = CompositeSubscription()
    }

    override fun onStart() {
        super.onStart()

        if (!_vmInitialized) {
            _vmInitialized = true

            createViewModel()
        }

        if (!_configureObserversCalled) {
            _configureObserversCalled = true

            configureObservers()
        }
    }

    override fun onResume() {
        super.onResume()

        ephemeralBag = CompositeSubscription()

        configureUIObservers()

        _viewModel?.engage()
    }

    override fun onPause() {
        super.onPause()

        _viewModel?.disengage()

        // Unlike BaseFragment, the ephemeralBag gets cleared in onPause(), because the activity
        // might come to the foreground after onPause(), so its reasonable to resubscribe.
        ephemeralBag.clear()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onRestart() {
        super.onRestart()
    }

    override fun onDestroy() {
        super.onDestroy()

        // It is true that onDestroy() might not be called if the process is killed instantly.
        // Still, we cannot easily move the cleanup to onStop(), because the whole point of having
        // the disposeBag is to make it persist even when the activity is invisible.
        disposeBag.clear()

        _viewModel?.destroy()
    }

    /** Due to the nature of the Activity lifecycle, the [onCreate] method might be called multiple
     * times. It should be treated as a fresh start, and so all resources should be initialized
     * again. This is an internal method that helps prepare the renewed lifecycle.
     *
     * It is not intended for use by any subclass.
     */
    private fun _resetForOnCreate() {
        _vmInitialized = false
        _configureObserversCalled = false
    }

    /** Can be used to set the ActionBar title. Mostly useful for Fragments to use. if there is no
     * ActionBar available, this will fail silently.
     *
     * This class can be overwritten by subclasses if needed.
     *
     * @param title The title to set.
     */
    open fun setActionBarTitle(title: String) {
        supportActionBar?.title = title
    }

    /** A convenience method to be used by any Fragment that is a part of this Activity to easily
     * navigate to another fragment.
     *
     * @param fragment A BaseFragment subclass to which to navigate.
     *
     * @param replace A boolean indicating whether to replace the current fragment instead of
     *                adding it. Defaults to true.
     *
     * @param addToBackStack A boolean indicating whether to add to the backstack. Defaults to true.
     *
     * @param tag The string to use as a tag in the backstack. If the replace parameter is set to
     *            true, then this tag will also be used to detect whether a Fragment with this tag
     *            is alraedy visible or in the backstack, and if so, that one will be used instead
     *            of using a new Fragment. If tag is set to null, the BaseFragment's tag property
     *            is used instead (this is often recommended).
     *
     * @param animate A boolean indicating whether to animate the transition. Defaults to true.
     *
     * @param clearStack A boolean indicating whether to forget all previous entries in the stack
     *                   or not. Defaults to false.
     *
     * @param container An integer representing the container to which to add this fragment.
     *                  Defaults to R.id.fl_container.
     */
    fun navigateToFragment(fragment: BaseFragment,
                           tag: String,
                           replace: Boolean = true,
                           addToBackStack: Boolean = true,
                           animate: Boolean = true,
                           clearStack: Boolean = false,
                           container: Int = R.id.fl_container) {
        if (replace) {
            val fragmentForTag = supportFragmentManager.findFragmentByTag(tag)
            if (fragmentForTag != null && fragmentForTag.isVisible) {
                return
            } else if (supportFragmentManager.popBackStackImmediate(tag, 0)) {
                return
            }
        }

        if (clearStack) {
            while (supportFragmentManager.backStackEntryCount > 0)
                supportFragmentManager.popBackStackImmediate()

            supportFragmentManager.executePendingTransactions()
        }

        val transaction = supportFragmentManager.beginTransaction()

        if (animate) transaction.setCustomAnimations(
                R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit)

        if (replace) transaction.replace(container, fragment, tag)
        else         transaction.add(container, fragment, tag)

        if (addToBackStack) transaction.addToBackStack(tag)

        transaction.commit()
    }

    /** The [viewModel] property on this object should be instantiated in this method. It will be
     * called at the appropriate time, and only once throughout the lifetime of the Activity.
     */
    open fun createViewModel() {

    }

    /** If you wish to subscribe() to a viewModel's Observable that is not related to the UI, then
     * you can do so here. All these subscriptions should be added to `disposeBag`.
     *
     * Note that by using this method instead of [configureUIObservers], all subscriptions will
     * only be defined once throughout the Activity lifecycle.
     */
    open fun configureObservers() {

    }

    /** Any subscribe() call to the viewModel's Observables should happen here if they are somehow
     * related to the UI (which is almost every subscription). All these subscriptions should be
     * added to [ephemeralBag] (not [disposeBag])
     */
    open fun configureUIObservers() {

    }

    /** Can be used by any Activity or Fragment to display a toast of various styles.
     *
     * This behavior can also be overwritten by any subclass, but that is not required.
     *
     * @param toastType A ToastType constant value
     *
     * @param message The message to display. This should be kept very short
     */
    open fun displayToast(toastType: Int, message: String) {
        val styleId: Int
        when(toastType) {
            ToastType.SUCCESS -> styleId = R.style.StyledToastSuccess
            ToastType.ERROR -> styleId = R.style.StyledToastError
            else -> styleId = R.style.StyledToastMessage
        }

        StyleableToast.makeText(this, message, Toast.LENGTH_LONG, styleId).show()
    }

    /** Should be implemented by any subclass that wishes to display a loader overlay whenever some
     * action takes longer than expected. This method should show or hide the overlay View.
     *
     * @param v The boolean indicating whether to show (true) or hide (false).
     *
     * @see [toggleLoadingOverlay]
     */
    open fun setVisibilityOnLoadingOverlay(v: Boolean) {

    }

    /** Should get called by any Fragment that wishes to start/end the loading spinner in
     * x milliseconds from now.
     *
     * @param v The boolean indicating whether to start (true) or end (false) the spinner.
     *
     * @param ms The delay (in milliseconds) before the spinner is shown. Defaults to 500 ms.
     *
     * @see [_hudOverlayTimer]
     * @see [_hudOverlayTimerRunnable]
     * @see [setVisibilityOnLoadingOverlay]
     */
    fun toggleLoadingOverlay(v: Boolean, ms: Long = 500) {
        if (v) {
            _hudOverlayTimer.postDelayed(_hudOverlayTimerRunnable, ms)
        } else {
            _hudOverlayTimer.removeCallbacks(_hudOverlayTimerRunnable)

            setVisibilityOnLoadingOverlay(false)
        }
    }
}