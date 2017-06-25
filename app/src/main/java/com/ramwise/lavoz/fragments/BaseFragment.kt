package com.ramwise.lavoz.fragments

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.R
import com.ramwise.lavoz.activities.BaseActivity
import com.ramwise.lavoz.utils.exceptions.NativeException
import com.ramwise.lavoz.viewmodels.BaseViewModel
import rx.subscriptions.CompositeSubscription
import java.lang.ref.WeakReference
import java.util.*


/** This is the class that any Fragment in this application should inherit from. It provides a
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
 *   Fragment: constructing a viewModel should therefore take into consideration that the event
 *   Observables might change over time. Something like flatMap is useful to circumvent this.
 *   This method is empty and can be overwritten by the subclass' implementation (i.e. super() need
 *   not be called).
 * - Calls [configureObsevers] and [configureUIObservers] at the appropriate times. These methods
 *   are empty and can be overwritten by the subclass' implementation
 *   (i.e. super() need not be called).
 */
abstract class BaseFragment : Fragment() {
    companion object {
        fun fragmentTag() : String {
            return "IMPLEMENT_IN_SUBCLASS"
        }
    }

    /** The title of the Fragment as it should appear in the action bar. Can be overwritten by
     * subclasses.
     *
     * Important: This is a lateinit variable because often you will want to use
     * resources.getString() to set the title, but this is not available at init-time for fragments.
     * So the value of this title must be set somewhere like in [onAttach].
     */
    lateinit var title: String

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

    /** @return the viewModel property in the BaseFragment subclass, cast to [BaseViewModel]. */
    protected val _viewModel: BaseViewModel?
        get() {
            if (_viewModelCached == null) {
                try {
                    val field_ = this.javaClass.getDeclaredField("viewModel")
                    field_.isAccessible = true

                    _viewModelCached =  field_.get(this) as BaseViewModel
                } catch (e: NoSuchFieldException) {
                    throw NativeException("BaseFragment subclasses must have a viewModel property.")
                } catch (e: Exception) {
                    // In some cases, the viewModel object might not have been created yet (i.e.
                    // early in the Fragment lifecycle).
                    return null
                }
            }

            return _viewModelCached
        }

    /** Should be set to true when [onCreate] is called for the first time */
    protected var _onCreateCalled = false

    /** Meant for use in conjunction with [toggleLoadingOverlay]. */
    private val _hudOverlayTimer = Handler()

    /** Meant for use in conjunction with [toggleLoadingOverlay]. */
    private var _hudOverlayTimerRunnable: Runnable? = null

    /** @return The Activity to which this Fragment belongs (via [getActivity]), cast to a
     * [BaseActivity] object if possible. Returns null otherwise.
     */
    val baseActivity: BaseActivity?
        get() {
            return if (activity is BaseActivity) activity as BaseActivity
                    else null
        }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        title = resources.getString(R.string.app_name)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        disposeBag = CompositeSubscription()

        _onCreateCalled = true
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (!_vmInitialized) {
            _vmInitialized = true

            createViewModel()
        }

        if (!_configureObserversCalled) {
            _configureObserversCalled = true

            configureObservers()
        }
    }

    override fun onStart() {
        super.onStart()

        ephemeralBag = CompositeSubscription()

        configureSharedViewClicks()

        prepareConfigureUIObservers()

        configureUIObservers()
    }

    override fun onResume() {
        super.onResume()

        _viewModel?.engage()

        baseActivity?.setActionBarTitle(title)
    }

    override fun onPause() {
        super.onPause()

        _viewModel?.disengage()
    }

    override fun onStop() {
        super.onStop()

        ephemeralBag.clear()
    }

    override fun onDestroy() {
        super.onDestroy()

        disposeBag.clear()

        _viewModel?.destroy()
    }

    /** The [viewModel] property on this object should be instantiated in this method. It will be
     * called at the appropriate time, and only once throughout the lifetime of the Fragment.
     */
    open fun createViewModel() {

    }

    /** If you wish to subscribe() to a viewModel's Observable that is not related to the UI, then
     * you can do so here. All these subscriptions should be added to `disposeBag`.
     *
     * Note that by using this method instead of [configureUIObservers], all subscriptions will
     * only be defined once throughout the Fragment lifecycle.
     */
    open fun configureObservers() {

    }

    /** Due to the nature of sharing between Observables within the ViewModel, there is no
     * way to share button-clicks with multiple Observers if there is a retain() call
     * downstream, because then the clicks-Observable is kept alive even after a Fragment recreates
     * its views, thus leaving an Observable of clicks on non-existing views.
     * To solve this, simply register the clicks here and write them to an RxVariable.
     *
     * This method is always called right before [prepareConfigureUIObservers].
     */
    open fun configureSharedViewClicks() {

    }

    /** In some cases, there is a need to do some preparation work before [configureUIObservers]
     * is called. A common example is resetting the values of RxVariables to a value that is
     * ignored, such that resubscribing to its Observable does not retrigger a previous action.
     *
     * This method is always called right before [configureUIObservers].
     */
    open fun prepareConfigureUIObservers() {

    }

    /** Any subscribe() call to the viewModel's Observables should happen here if they are somehow
     * related to the UI (which is almost every subscription). All these subscriptions should be
     * added to [ephemeralBag] (not [disposeBag])
     */
    open fun configureUIObservers() {

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
     * @param ms The delay (in milliseconds) before the spinner is shown. If set to -1 (default),
     *           the delay for showing is 500ms and for hiding is 0ms.
     *
     * @see [_hudOverlayTimer]
     * @see [_hudOverlayTimerRunnable]
     * @see [setVisibilityOnLoadingOverlay]
     */
    fun toggleLoadingOverlay(v: Boolean, ms: Long = -1) {
        if (_hudOverlayTimerRunnable != null)
            _hudOverlayTimer.removeCallbacks(_hudOverlayTimerRunnable)

        val ms_: Long
        if (ms == -1L)  ms_ = if (v) 500L else 0L
        else            ms_ = ms

        if (ms_ > 0) {
            _hudOverlayTimerRunnable = object : Runnable {
                override fun run() { setVisibilityOnLoadingOverlay(v) }
            }
            _hudOverlayTimer.postDelayed(_hudOverlayTimerRunnable, ms_)
        } else {
            setVisibilityOnLoadingOverlay(v)
        }
    }
}