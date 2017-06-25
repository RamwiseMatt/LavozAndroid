package com.ramwise.lavoz.fragments

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding.view.clicks
import com.jakewharton.rxbinding.widget.checkedChanges
import com.jakewharton.rxbinding.widget.text
import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.R
import com.ramwise.lavoz.models.Organization
import com.ramwise.lavoz.models.constants.VoteOption
import com.ramwise.lavoz.utils.rx.addDisposableTo
import com.ramwise.lavoz.utils.rx.ui
import com.ramwise.lavoz.viewmodels.fragments.HomeFragmentViewModel
import com.ramwise.lavoz.viewmodels.fragments.OverviewFragmentViewModel
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_overview.*
import rx.Observable
import java.util.*
import android.content.Intent
import android.net.Uri
import com.jakewharton.rxbinding.widget.checked
import com.kobakei.ratethisapp.RateThisApp
import com.ramwise.lavoz.models.constants.OptionalBool
import com.ramwise.lavoz.models.constants.ToastType
import com.ramwise.lavoz.utils.rx.RxVariable
import com.ramwise.lavoz.utils.rx.unwrap
import java.util.concurrent.TimeUnit


class OverviewFragment : BaseFragment() {
    companion object {
        fun newInstance(asAdviceAid: Boolean = false): OverviewFragment {
            val obj = OverviewFragment()

            val args = Bundle()
            args.putBoolean("asAdviceAid", asAdviceAid)
            obj.arguments = args

            return obj
        }

        fun fragmentTag(asAdviceAid: Boolean = false) : String {
            return if (asAdviceAid) "overview_asadviceaid" else "overview"
        }
    }

    lateinit var viewModel: OverviewFragmentViewModel

    /** The ViewModel requires an Observable that represents user-activated Switch changes. However,
     * the code itself might also make Switch changes, but those should not be included in the
     * Observable, else it might lead to an infinite loop.
     * This RxVariable should emit a simple Unit value whenever the Switch was just updated by the
     * code. The Observable given to the ViewModel should filter out all check-change events within
     * x milliseconds from this RxVariable being updated, e.g. 100ms.
     *
     * The emitted value is an OptionalBool constant and should be mapped to a Boolean after
     * filtering out the .UNDEFINED values.
     */
    private val displayNameSwitchChangedByCode = RxVariable(OptionalBool.UNDEFINED)

    /** There is a retain() call in the ViewModel, this RxVariable works in cooperation with
     * [configureSharedViewClicks].
     *
     * The emitted value is an OptionalBool constant and should be mapped to a Boolean after
     * filtering out the .UNDEFINED values.
     */
    private val displayNameSwitchChangedByHand = RxVariable(OptionalBool.UNDEFINED)

    var asAdviceAid: Boolean = false // Set the actual value in onCreate() from the Bundle.

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        title = resources.getString(R.string.cap_you)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        asAdviceAid = arguments.getBoolean("asAdviceAid", false)

        // Purge the bundle to avoid a "Parcel: unable to marshal value" error when pausing the
        // fragment.
        arguments.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_overview, container, false)

        return view
    }

    override fun onStart() {
        super.onStart()

        RateThisApp.showRateDialogIfNeeded(activity)
    }

    override fun createViewModel() {
        viewModel = OverviewFragmentViewModel(
                asAdviceAid,
                Observable.defer { recommendations_button.clicks() }.share(),
                Observable.defer { banner_imageview.clicks() }.share(),
                Observable.defer { partypoints_explanation_button.clicks() }.share(),
                Observable.defer { majority_votes_button.clicks() }.share(),
                Observable.defer { minority_votes_button.clicks() }.share(),
                Observable.defer { motivated_votes_button.clicks() }.share(),
                Observable
                        .merge(
                                displayNameSwitchChangedByHand.asObservable(),
                                // The code ones should always get filtered out after throttling
                                displayNameSwitchChangedByCode.asObservable().map { OptionalBool.UNDEFINED }
                        )
                        .throttleFirst(100, TimeUnit.MILLISECONDS)
                        .filter { it != OptionalBool.UNDEFINED }
                        .map { OptionalBool.toBoolean(it) }
                        .share()
        )
    }

    override fun configureSharedViewClicks() {
        preference_display_name_switch
                .checkedChanges()
                .skip(1)
                .subscribe { displayNameSwitchChangedByHand.value = OptionalBool.fromBoolean(it) }
                .addDisposableTo(ephemeralBag)
    }

    override fun prepareConfigureUIObservers() {
        displayNameSwitchChangedByCode.value = OptionalBool.UNDEFINED
        displayNameSwitchChangedByHand.value = OptionalBool.UNDEFINED
    }

    override fun configureUIObservers() {
        viewModel.bannerImageURL
                .ui()
                .subscribe {
                    Picasso.with(LavozApplication.context)
                            .load(it)
                            .into(banner_imageview)
                }
                .addDisposableTo(ephemeralBag)

        viewModel.navigateToBrowser
                .ui()
                .subscribe {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                    startActivity(intent)
                }
                .addDisposableTo(ephemeralBag)

        viewModel.bannerMessageText.ui().subscribe(banner_textview.text()).addDisposableTo(ephemeralBag)

        viewModel.deviceMessageAlerts
                .ui()
                .subscribe {
                    for (alert in it) {
                        val builder = AlertDialog.Builder(context)
                        if (alert.title != null)
                            builder.setTitle(alert.title)

                        builder.setMessage(alert.text)
                                .setPositiveButton(alert.okText, { dialog, which ->
                                    if (alert.url != null) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(alert.url))
                                        startActivity(intent)
                                    }
                                })
                                .setNegativeButton(alert.cancelText, null)
                                .show()
                    }
                }
                .addDisposableTo(ephemeralBag)

        viewModel.forceShowRatePrompt
                .ui()
                .subscribe {
                    RateThisApp.showRateDialog(activity)
                }
                .addDisposableTo(ephemeralBag)

        viewModel.partyPointsOne.ui().subscribe(party_points_textview_one.text()).addDisposableTo(ephemeralBag)
        viewModel.partyPointsTwo.ui().subscribe(party_points_textview_two.text()).addDisposableTo(ephemeralBag)
        viewModel.partyPointsThree.ui().subscribe(party_points_textview_three.text()).addDisposableTo(ephemeralBag)

        viewModel.partyNameOne.ui().subscribe(party_name_textview_one.text()).addDisposableTo(ephemeralBag)
        viewModel.partyNameTwo.ui().subscribe(party_name_textview_two.text()).addDisposableTo(ephemeralBag)
        viewModel.partyNameThree.ui().subscribe(party_name_textview_three.text()).addDisposableTo(ephemeralBag)

        viewModel.partyIconURLOne
                .ui()
                .subscribe {
                    Picasso.with(LavozApplication.context)
                            .load(it)
                            .into(party_icon_one)
                }
                .addDisposableTo(ephemeralBag)

        viewModel.partyIconURLTwo
                .ui()
                .subscribe {
                    Picasso.with(LavozApplication.context)
                            .load(it)
                            .into(party_icon_two)
                }
                .addDisposableTo(ephemeralBag)

        viewModel.partyIconURLThree
                .ui()
                .subscribe {
                    Picasso.with(LavozApplication.context)
                            .load(it)
                            .into(party_icon_three)
                }
                .addDisposableTo(ephemeralBag)

        viewModel.showPartyPointsInfoDialog
                .ui()
                .subscribe {
                    val (title, message) = it
                    AlertDialog.Builder(context)
                            .setTitle(title)
                            .setMessage(message)
                            .setNegativeButton(R.string.cap_go_back, null)
                            .show()
                }
                .addDisposableTo(ephemeralBag)

        viewModel.navigateToRecommendations
                .ui()
                .subscribe {
                    baseActivity?.navigateToFragment(
                            RecommendationsFragment.newInstance(it),
                            RecommendationsFragment.fragmentTag())
                }
                .addDisposableTo(ephemeralBag)

        viewModel.navigateToMotionList
                .ui()
                .subscribe {
                    baseActivity?.navigateToFragment(
                            MotionListFragment.newInstance(it),
                            MotionListFragment.fragmentTag(it))
                }
                .addDisposableTo(ephemeralBag)

        viewModel.votesReceivedCountText.ui().subscribe(num_hearts_received_textview.text()).addDisposableTo(ephemeralBag)
        viewModel.majorityCountText.ui().subscribe(num_majority_votes_textview.text()).addDisposableTo(ephemeralBag)
        viewModel.minorityCountText.ui().subscribe(num_minority_votes_textview.text()).addDisposableTo(ephemeralBag)
        viewModel.motivatedCountText.ui().subscribe(num_motivated_votes_textview.text()).addDisposableTo(ephemeralBag)

        viewModel.userDisplayNameSwitchValue
                .ui()
                .subscribe {
                    displayNameSwitchChangedByCode.value = OptionalBool.fromBoolean(it)

                    preference_display_name_switch.isChecked = it
                }
                .addDisposableTo(ephemeralBag)

        viewModel.preferenceChangeFailed
                .ui()
                .subscribe {
                    baseActivity?.displayToast(ToastType.ERROR, it)
                }
                .addDisposableTo(ephemeralBag)

        viewModel.prepareLayoutOverlayHUDLoader
                .ui()
                .subscribe { toggleLoadingOverlay(it) }
                .addDisposableTo(ephemeralBag)
    }

    override fun setVisibilityOnLoadingOverlay(v: Boolean) {
        still_loading_overlay?.visibility = if (v) View.VISIBLE else View.GONE
    }

}