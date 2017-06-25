package com.ramwise.lavoz.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding.view.clicks
import com.jakewharton.rxbinding.widget.text
import com.ramwise.lavoz.R
import com.ramwise.lavoz.models.constants.VoteOption
import com.ramwise.lavoz.utils.rx.RxVariable
import com.ramwise.lavoz.utils.rx.addDisposableTo
import com.ramwise.lavoz.utils.rx.ui
import rx.Observable
import com.ramwise.lavoz.viewmodels.fragments.AdviceAidFragmentViewModel
import kotlinx.android.synthetic.main.fragment_adviceaid.*


class AdviceAidFragment : BaseFragment() {
    companion object {
        fun newInstance(isCompleted: Boolean): AdviceAidFragment {
            val obj = AdviceAidFragment()

            val args = Bundle()
            args.putBoolean("isCompleted", isCompleted)
            obj.arguments = args

            return obj
        }

        fun fragmentTag(isCompleted: Boolean) : String {
            return "adviceaid_" + (if (isCompleted) "completed" else "start")
        }
    }

    var isCompleted: Boolean = false // Set the actual value in onCreate() from the Bundle.

    lateinit var viewModel: AdviceAidFragmentViewModel

    private val voteButtonsTapped = RxVariable(VoteOption.UNDEFINED)
    private val confirmVoteChange = RxVariable(VoteOption.UNDEFINED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isCompleted = arguments.getBoolean("isCompleted", false)

        // Purge the bundle to avoid a "Parcel: unable to marshal value" error when pausing the
        // fragment.
        arguments.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_adviceaid, container, false)

        return view
    }

    override fun createViewModel() {
        viewModel = AdviceAidFragmentViewModel(
                isCompleted,
                Observable.defer { main_button.clicks() }.share()
        )
    }

    override fun configureUIObservers() {

        viewModel.mainText.ui().subscribe(main_textview.text()).addDisposableTo(ephemeralBag)
        viewModel.buttonText.ui().subscribe(main_button.text()).addDisposableTo(ephemeralBag)

        viewModel.navigateToNext
                .ui()
                .subscribe {
                    if (it) {
                        baseActivity?.navigateToFragment(
                                OverviewFragment.newInstance(asAdviceAid = true),
                                OverviewFragment.fragmentTag(asAdviceAid = true),
                                clearStack = true)
                    } else {
                        baseActivity?.navigateToFragment(
                                HomeFragment.newInstance(forMotionId = null, asAdviceAid = true),
                                HomeFragment.fragmentTag(asAdviceAid = true))
                    }

                }
                .addDisposableTo(ephemeralBag)
    }
}