package com.ramwise.lavoz.fragments

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding.support.v7.widget.RxRecyclerView
import com.jakewharton.rxbinding.view.clicks
import com.ramwise.lavoz.R
import com.ramwise.lavoz.adapters.MotionListAdapter
import com.ramwise.lavoz.models.constants.MotionListViewType
import com.ramwise.lavoz.models.constants.MotionStatus
import com.ramwise.lavoz.models.constants.VoteOption
import com.ramwise.lavoz.utils.rx.RxVariable
import com.ramwise.lavoz.utils.rx.addDisposableTo
import com.ramwise.lavoz.utils.rx.ui
import com.ramwise.lavoz.viewmodels.adapters.MotionListConcludedAdapterViewModel
import com.ramwise.lavoz.viewmodels.adapters.MotionListOpenAdapterViewModel
import com.ramwise.lavoz.viewmodels.adapters.interfaces.MotionListGenericAdapterViewModel
import com.ramwise.lavoz.viewmodels.fragments.MotionListFragmentViewModel
import kotlinx.android.synthetic.main.fragment_motionlist.*
import rx.Observable
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.TimeUnit


class MotionListFragment : BaseFragment() {
    companion object {
        fun newInstance(viewType: Int): MotionListFragment {
            val obj = MotionListFragment()

            val args = Bundle()
            args.putSerializable("viewType", viewType)
            obj.arguments = args

            return obj
        }

        fun fragmentTag(viewType: Int) : String {
            return "motionlist_" + MotionListViewType.toString(viewType)
        }
    }

    var viewType: Int = MotionListViewType.RECENT // Set the value in onCreate() from the Bundle.

    lateinit var viewModel: MotionListFragmentViewModel

    /** Emits a signal whenever the recyclerView is clicked. The emitted value is a Pair of
     * the position of the cell that was clicked, and the MotionStatus of it.
     *
     * The initial value can be ignored.
     */
    private val recyclerViewClicked = RxVariable(Pair(-1, MotionStatus.UNDEFINED))

    /** Emits a signal whenever the recyclerView has reached the bottom.
     */
    private val recyclerViewBottomReached = RxVariable(false)

    /** A WeakReference to the current object. */
    private val weakThis = WeakReference(this)

    /** A LinearLayoutManager to be created in [onCreateView]. */
    private var layoutManager: LinearLayoutManager? = null

    /** Holds the position of the recyclerView's top visible item before navigating away from the
     * fragment. This is useful when re-setting the position after the fragment comes into view
     * again.
     *
     * This should be set to null if there is no prefered position to navigate to.
     */
    private var savedLayoutPosition: Int? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        title = resources.getString(R.string.cap_motions)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewType_ = arguments.getInt("viewType")

        // Coincidentally, .RECENT is of value 0, but still perform this check for clarity's sake.
        viewType = if (viewType_ == 0) MotionListViewType.RECENT else viewType_

        // Purge the bundle to avoid a "Parcel: unable to marshal value" error when pausing the
        // fragment.
        arguments.remove("viewType")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_motionlist, container, false)

        layoutManager = LinearLayoutManager(activity.applicationContext)

        val recyclerView = view.findViewById(R.id.recycler_view) as RecyclerView
        recyclerView.layoutManager = layoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.isNestedScrollingEnabled = false

        // Set an adapter on an empty ArrayList.
        recyclerView.adapter = object :
                MotionListAdapter(ArrayList<MotionListGenericAdapterViewModel>()) {
            override fun onCellClicked(layoutPosition: Int, motionStatus: Int) {
                weakThis.get().recyclerViewClicked.value = Pair(layoutPosition, motionStatus)
            }
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy > 0) {
                    if ((layoutManager?.childCount ?: 0) +
                            (layoutManager?.findFirstVisibleItemPosition() ?: 0) >=
                            (layoutManager?.itemCount ?: 0)) {
                        weakThis.get().recyclerViewBottomReached.value = true
                    }
                }
            }
        })

        return view
    }

    override fun onPause() {
        super.onPause()

        val position = layoutManager?.findFirstVisibleItemPosition() ?: 0

        // Add 1 to the position because that is likely the one that was in primary focus, unless
        // the given position is 0, which is just the top of the recycler.
        savedLayoutPosition = if (position > 0) position + 1 else position

    }

    override fun createViewModel() {
        viewModel = MotionListFragmentViewModel(
                viewType,
                recyclerViewClicked.asObservable().filter { it.first > - 1}.share(),
                recyclerViewBottomReached.asObservable().filter { it }
                        .debounce(300, TimeUnit.MILLISECONDS).map { (Unit) }.share()
        )
    }

    override fun prepareConfigureUIObservers() {
        recyclerViewClicked.value = Pair(-1, MotionStatus.UNDEFINED)
        recyclerViewBottomReached.value = false
    }

    override fun configureUIObservers() {
        viewModel.adapterViewModels
                .ui()
                .subscribe {
                    (recycler_view.adapter as MotionListAdapter).swap(it)

                    if (savedLayoutPosition != null) {
                        layoutManager?.scrollToPosition(savedLayoutPosition ?: 0)

                        savedLayoutPosition = null
                    }
                }
                .addDisposableTo(ephemeralBag)

        viewModel.navigateToHome
                .ui()
                .subscribe {
                    baseActivity?.navigateToFragment(
                            HomeFragment.newInstance(forMotionId = it),
                            HomeFragment.fragmentTag(forMotionId = it))
                }
                .addDisposableTo(ephemeralBag)

        viewModel.navigateToMoreDetails
                .ui()
                .subscribe {
                    baseActivity?.navigateToFragment(
                            MotionFragment.newInstance(it),
                            MotionFragment.fragmentTag(it))
                }
                .addDisposableTo(ephemeralBag)

        viewModel.prepareLayoutOverlayHUDLoader
                .ui()
                .subscribe { toggleLoadingOverlay(it) }
                .addDisposableTo(ephemeralBag)
    }

    override fun configureObservers() {
        viewModel.bottomPageHUDLoader
                .ui()
                .subscribe {
                    bottom_loading_icon?.visibility = if (it) View.VISIBLE else View.INVISIBLE
                }
                .addDisposableTo(disposeBag)
    }

    override fun setVisibilityOnLoadingOverlay(v: Boolean) {
        still_loading_overlay?.visibility = if (v) View.VISIBLE else View.GONE
    }

}