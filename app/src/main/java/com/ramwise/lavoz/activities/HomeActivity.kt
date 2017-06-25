package com.ramwise.lavoz.activities

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.view.MenuItem
import com.ramwise.lavoz.R
import com.ramwise.lavoz.utils.rx.addDisposableTo
import com.ramwise.lavoz.viewmodels.activities.HomeActivityViewModel
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.toolbar.*
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.facebook.login.LoginManager
import com.jakewharton.rxbinding.support.design.widget.itemSelections
import com.jakewharton.rxbinding.view.clicks
import com.jakewharton.rxbinding.widget.text
import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.models.constants.MotionListViewType
import com.ramwise.lavoz.network.AuthenticationService
import com.ramwise.lavoz.utils.rx.ui
import rx.Observable
import java.util.*
import javax.inject.Inject
import com.google.firebase.iid.FirebaseInstanceId
import com.kobakei.ratethisapp.RateThisApp
import com.ramwise.lavoz.fragments.*


class HomeActivity : BaseActivity(), FragmentManager.OnBackStackChangedListener {

    @Inject lateinit var authService: AuthenticationService

    lateinit var viewModel: HomeActivityViewModel

    lateinit var drawerToggle: ActionBarDrawerToggle

    lateinit private var rootFragments: HashMap<String, BaseFragment>
    lateinit private var rootFragmentsBackstack: Stack<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)

        LavozApplication.networkComponent.inject(this)

        supportFragmentManager.addOnBackStackChangedListener(this)

        setSupportActionBar(toolbar)

        drawerToggle = object : ActionBarDrawerToggle(this, drawer_layout,
                R.string.cap_open, R.string.cap_close) {
            override fun onDrawerClosed(view: View?) {
                super.onDrawerClosed(view)
                actionBar?.title = title
            }

            override fun onDrawerOpened(drawerView: View?) {
                super.onDrawerOpened(drawerView)
                actionBar?.title = title
            }
        }

        drawer_layout.addDrawerListener(drawerToggle)

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        startRootFragments(
                Pair(HomeFragment.newInstance(), HomeFragment.fragmentTag()),
                Pair(AdviceAidFragment.newInstance(false), AdviceAidFragment.fragmentTag(false)),
                Pair(MotionListFragment.newInstance(MotionListViewType.RECENT),
                        MotionListFragment.fragmentTag(MotionListViewType.RECENT)),
                Pair(OverviewFragment.newInstance(), OverviewFragment.fragmentTag())
        )
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        drawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)

        drawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onBackStackChanged() {
        if (supportFragmentManager.backStackEntryCount == 0) {
            drawerToggle.isDrawerIndicatorEnabled = true
            drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        } else {
            drawerToggle.isDrawerIndicatorEnabled = false
            drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }

        drawerToggle.syncState()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                if (supportFragmentManager.backStackEntryCount == 0) {
                    drawer_layout.openDrawer(GravityCompat.START)
                } else {
                    onBackPressed()
                }

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)

            return
        } else if (supportFragmentManager.backStackEntryCount == 0) {
            // Ensure that there is not another root Fragment to be navigated to before closing
            // the application.
            if (navigateBetweenRootFragments(null, false)) {
                // Successfully navigated to another root Fragment, so do not continue up the call
                // stack.
                return
            }
        }

        super.onBackPressed()
    }

    override fun onStart() {
        super.onStart()

        RateThisApp.onStart(this)
    }

    override fun createViewModel() {
        viewModel = HomeActivityViewModel(
                Observable.defer {
                    (left_drawer.getHeaderView(0).findViewById(R.id.sign_out_button) as Button)
                            .clicks()
                }.share())
    }

    override fun configureObservers() {
        left_drawer
                .itemSelections()
                .subscribe {
                    when (it.itemId) {
                        R.id.home_item -> {
                            navigateBetweenRootFragments("home", true)
                        }
                        R.id.adviceaid_item -> {
                            navigateBetweenRootFragments("adviceaid_start", true)
                        }
                        R.id.motionlist_item -> {
                            navigateBetweenRootFragments("motionlist_recent", true)
                        }
                        R.id.overview_item -> {
                            navigateBetweenRootFragments("overview", true)
                        }
                    }
                    // Highlight the selected item has been done by NavigationView
                    it.isChecked = true

                    actionBar?.title = title

                    // Close the navigation drawer
                    drawer_layout.closeDrawers()
                }
                .addDisposableTo(disposeBag)
    }

    /** Initialize all root Fragments. Root Fragments in this context are Fragments that can be
     * accessed via the navigation drawer. For practical purposes, these Fragments should be
     * created during the Activity's onCreate() and then kept alive for the entire duration of
     * the Activity.
     *
     * @param roots Pairs of all Fragment objects that can be accessed via the navigation drawer
     *              along with the fragmentTag that belongs to its class.
     *
     * @param navigateToInitialRoot A boolean indicating whether the first Fragment passed in the
     *                              fragments parameter should be added to the Activity's view.
     *                              It is recommended that this value is set to true.
     */
    private fun startRootFragments(vararg roots: Pair<BaseFragment, String>,
                                   navigateToInitialRoot: Boolean = true) {
        // Initialize the required data structures. This method might be called multiple times,
        // and therefore this also has the function of clearing data upon recalling.
        rootFragments = HashMap<String, BaseFragment>()
        rootFragmentsBackstack = Stack<String>()

        var firstFragment: BaseFragment? = null
        var firstFragmentTag: String? = null
        for ((fragment, tag) in roots) {
            if (firstFragment == null) firstFragment = fragment
            if (firstFragmentTag == null) firstFragmentTag = tag

            // IMPORTANT: use the XML-defined "android:tag" property here instead of the
            // fragmentTag property defined on most BaseFragment subclasses.
            // This can and should only be done for root fragments.
            rootFragments[tag] = fragment
        }

        // Force an exception if there is no firstFragment, which should never happen.
        rootFragmentsBackstack.push(firstFragmentTag!!)

        if (navigateToInitialRoot) {
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

            navigateToFragment(firstFragment!!, tag = firstFragmentTag, replace = false,
                    animate = false, addToBackStack = false)

            left_drawer.setCheckedItem(R.id.home_item)
        }
    }

    /** Normally, navigating between Fragments with the navigation drawer and the regular Fragment
     * backstack means that Fragments get removed/recreated often. This behavior is undesirable,
     * so this method provides a custom way to navigate between root Fragments.
     *
     * Note that [onBackPressed] should be custom implemented to listen to hardware back arrow
     * calls. Whenever such calls are made while a root Fragment is displaying, this method should
     * be called with forwards=false. It will then attempt to display a previous root Fragment, or
     * return false.
     *
     * @param fragmentTag The tag of the Fragment to be displayed next. The Fragment must be a
     *                    root Fragment, i.e. a Fragment accessible from the navigation drawer and
     *                    included in the [startRootFragments] call during onCreate().
     *
     * @param forwards A boolean indicating whether to navigate forwards or backwards. Navigating
     *                 forwards happens whenever a new root Fragment should be displayed after
     *                 the user clicks an item in the navigation drawer. Navigating backwards
     *                 happens whenever a user presses the hardware back arrow when already on
     *                 another root Fragment.
     *
     * @param animate A boolean indicating whether to animate the transition. Defaults to true.
     *
     * @return A boolean indicating whether a different Fragment is being navigated to.
     *
     * @see [startRootFragments]
     */
    private fun navigateBetweenRootFragments(fragmentTag: String?, forwards: Boolean,
                                             animate: Boolean = true): Boolean {
        val currentFragment: BaseFragment?
        val newFragment: BaseFragment?

        if (forwards && fragmentTag != null) {
            val currentFragmentTag = rootFragmentsBackstack.peek()
            if (currentFragmentTag == fragmentTag) return false

            currentFragment = rootFragments[currentFragmentTag]
            newFragment = rootFragments[fragmentTag] ?: return false
        } else {
            if (rootFragmentsBackstack.size <= 1) return false

            currentFragment = rootFragments[rootFragmentsBackstack.pop()]
            newFragment = rootFragments[rootFragmentsBackstack.peek()] ?: return false
        }

        val transaction = supportFragmentManager.beginTransaction()
        transaction.detach(currentFragment)

        if (newFragment.isDetached) {
            transaction.attach(newFragment)
        } else {
            transaction.replace(R.id.fl_container, newFragment, fragmentTag)
        }

        if (animate) transaction.setCustomAnimations(
                R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit)

        transaction.commit()

        if (forwards) {
            rootFragmentsBackstack.remove(fragmentTag)
            rootFragmentsBackstack.push(fragmentTag)
        }

        _updateNavigationDrawerSelection()

        return true
    }

    /** Resets the highlighted navigation drawer item if necessary. Should be called after the
     * backstack changes. The decision to change the highlighted item is based on the custom
     * root fragment backstack.
     */
    private fun _updateNavigationDrawerSelection() {
        when(rootFragmentsBackstack.peek()) {
            "home" -> left_drawer.setCheckedItem(R.id.home_item)
            "adviceaid_start" -> left_drawer.setCheckedItem(R.id.adviceaid_item)
            "motionlist_recent" -> left_drawer.setCheckedItem(R.id.motionlist_item)
            "overview" -> left_drawer.setCheckedItem(R.id.overview_item)
        }
    }

    override fun configureUIObservers() {
        val headerView = left_drawer.getHeaderView(0)

        viewModel.userFullName.ui()
                .subscribe((headerView.findViewById(R.id.username_textview) as TextView).text())
                .addDisposableTo(ephemeralBag)

        viewModel.removeAuthToken
                .subscribe {
                    authService.removeAllTokens()
                }
                .addDisposableTo(ephemeralBag)

        viewModel.navigateToLoginActivity
                .subscribe { navigateToLoginActivity() }
                .addDisposableTo(ephemeralBag)

        viewModel.prepareForSignOut
                .ui()
                .subscribe {
                    drawer_layout.closeDrawer(GravityCompat.START)

                    fl_container.visibility = View.GONE
                }
                .addDisposableTo(ephemeralBag)

        viewModel.updatePushTokenIfNeeded.subscribe().addDisposableTo(ephemeralBag)
    }

    override fun setVisibilityOnLoadingOverlay(v: Boolean) {
        still_loading_overlay.visibility = if (v) View.VISIBLE else View.GONE
    }

    /** Navigates to the login activity. Should be called by either this activity or a fragment
     * within it.
     */
    fun navigateToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }
}
