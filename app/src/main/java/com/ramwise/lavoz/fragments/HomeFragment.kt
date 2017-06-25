package com.ramwise.lavoz.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import com.jakewharton.rxbinding.view.clicks
import com.jakewharton.rxbinding.widget.text
import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.R
import com.ramwise.lavoz.activities.HomeActivity
import com.ramwise.lavoz.activities.LoginActivity
import com.ramwise.lavoz.models.Organization
import com.ramwise.lavoz.models.constants.ToastType
import com.ramwise.lavoz.models.constants.VoteOption
import com.ramwise.lavoz.network.AuthenticationService
import com.ramwise.lavoz.utils.exceptions.NativeException
import com.ramwise.lavoz.utils.rx.RxVariable
import com.ramwise.lavoz.utils.rx.addDisposableTo
import com.ramwise.lavoz.utils.rx.ui
import com.ramwise.lavoz.viewmodels.fragments.HomeFragmentViewModel
import kotlinx.android.synthetic.main.fragment_home.*
import rx.Observable
import java.util.*
import javax.inject.Inject
import android.content.DialogInterface
import com.facebook.share.ShareApi.share
import android.R.id.message
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.share.ShareApi
import com.facebook.share.Sharer
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.widget.ShareDialog
import com.ramwise.lavoz.models.constants.ShareType
import com.ramwise.lavoz.utils.sharing.ShareMotion


class HomeFragment : BaseFragment() {
    companion object {
        fun newInstance(forMotionId: Int? = null, asAdviceAid: Boolean = false): HomeFragment {
            val obj = HomeFragment()

            val args = Bundle()
            if (forMotionId != null) args.putSerializable("forMotionId", forMotionId)
            args.putBoolean("asAdviceAid", asAdviceAid)
            obj.arguments = args

            return obj
        }

        fun fragmentTag(forMotionId: Int) : String {
            return "home_" + forMotionId.toString()
        }

        fun fragmentTag(asAdviceAid: Boolean = false) : String {
            // Generate a random string
            if (asAdviceAid)
                return "home_" + (Math.random() * Int.MAX_VALUE).toString()
            return "home"
        }
    }

    @Inject lateinit var authService: AuthenticationService

    var forMotionId: Int? = null // Set the actual value in onCreate() from the Bundle.
    var asAdviceAid: Boolean = false // Set the actual value in onCreate() from the Bundle.

    lateinit var viewModel: HomeFragmentViewModel

    private val voteButtonsTapped = RxVariable(VoteOption.UNDEFINED)
    private val confirmVoteChange = RxVariable(VoteOption.UNDEFINED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LavozApplication.networkComponent.inject(this)

        val forMotionId_ = arguments.getInt("forMotionId")
        forMotionId = if (forMotionId_ == 0) null else forMotionId_

        asAdviceAid = arguments.getBoolean("asAdviceAid", false)

        // Purge the bundle to avoid a "Parcel: unable to marshal value" error when pausing the
        // fragment.
        arguments.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_home, container, false)

        return view
    }

    override fun createViewModel() {
        viewModel = HomeFragmentViewModel(
                forMotionId,
                asAdviceAid,
                Observable.defer { read_more_button.clicks() }.share(),
                Observable.defer { motivations_button.clicks() }.share(),
                Observable.defer { share_button.clicks() }.share(),
                voteButtonsTapped.asObservable().filter { it != VoteOption.UNDEFINED }.share(),
                confirmVoteChange.asObservable().filter { it != VoteOption.UNDEFINED }.share()
        )
    }

    override fun configureSharedViewClicks() {
        vote_agree_button.clicks().subscribe { voteButtonsTapped.value = VoteOption.AGREE }
                .addDisposableTo(ephemeralBag)
        vote_disagree_button.clicks().subscribe { voteButtonsTapped.value = VoteOption.DISAGREE }
                .addDisposableTo(ephemeralBag)
    }

    override fun configureUIObservers() {
        viewModel.motionIssue.ui().subscribe(issue_textview.text()).addDisposableTo(ephemeralBag)
        viewModel.motionLabelAgainst.ui().subscribe(vote_disagree_button.text()).addDisposableTo(ephemeralBag)
        viewModel.motionLabelFor.ui().subscribe(vote_agree_button.text()).addDisposableTo(ephemeralBag)
        viewModel.moreDetailsText.ui().subscribe(read_more_button.text()).addDisposableTo(ephemeralBag)
        viewModel.motionRemainingTimeText.ui().subscribe(timer_textview.text()).addDisposableTo(ephemeralBag)

        viewModel.readMoreButtonHidden
                .ui()
                .subscribe {
                    read_more_button.visibility = if (it) View.INVISIBLE else View.VISIBLE
                }
                .addDisposableTo(ephemeralBag)

        viewModel.motivationsuttonHidden
                .ui()
                .subscribe {
                    motivations_button.visibility = if (it) View.INVISIBLE else View.VISIBLE
                }
                .addDisposableTo(ephemeralBag)

        viewModel.shareButtonHidden
                .ui()
                .subscribe {
                    share_button.visibility = if (it) View.INVISIBLE else View.VISIBLE
                }
                .addDisposableTo(ephemeralBag)

        viewModel.voteButtonContainerHidden
                .ui()
                .subscribe {
                    vote_agree_layout.visibility = if (it) View.INVISIBLE else View.VISIBLE
                    vote_disagree_layout.visibility = if (it) View.INVISIBLE else View.VISIBLE
                }
                .addDisposableTo(ephemeralBag)

        viewModel.resultsContainerHidden
                .ui()
                .subscribe {
                    results_container_layout.visibility = if (it) View.INVISIBLE else View.VISIBLE
                }
                .addDisposableTo(ephemeralBag)

        viewModel.timerContainerHidden
                .ui()
                .subscribe {
                    timer_line_layout.visibility = if (it) View.INVISIBLE else View.VISIBLE
                    timer_textview.visibility = if (it) View.INVISIBLE else View.VISIBLE
                }
                .addDisposableTo(ephemeralBag)

        viewModel.adviceAidLogoHidden
                .ui()
                .subscribe {
                    advice_aid_logo.visibility = if (it) View.INVISIBLE else View.VISIBLE
                }
                .addDisposableTo(ephemeralBag)

        viewModel.resultsAgreeText.ui().subscribe(agree_results_textview.text()).addDisposableTo(ephemeralBag)
        viewModel.resultsDisagreeText.ui().subscribe(disagree_results_textview.text()).addDisposableTo(ephemeralBag)

        viewModel.resultsAgreeFraction
                .ui()
                .subscribe {
                    _setLineWeight(agree_line_remaining, it)
                    _setLineWeight(agree_line_gone, 1 - it)
                }
                .addDisposableTo(ephemeralBag)

        viewModel.resultsDisagreeFraction
                .ui()
                .subscribe {
                    _setLineWeight(disagree_line_remaining, it)
                    _setLineWeight(disagree_line_gone, 1 - it)
                }
                .addDisposableTo(ephemeralBag)

        viewModel.motionRemainingTimeFraction
                .ui()
                .subscribe {
                    _setLineWeight(timer_line_remaining, it)
                    _setLineWeight(timer_line_gone, 1 - it)
                }
                .addDisposableTo(ephemeralBag)

        viewModel.motionRemainingTimeLabelColor
                .ui()
                .subscribe { timer_textview.setTextColor(it) }
                .addDisposableTo(ephemeralBag)

        viewModel.navigateToMoreDetails
                .ui()
                .subscribe {
                    baseActivity?.navigateToFragment(
                            MotionFragment.newInstance(it),
                            MotionFragment.fragmentTag(it))
                }
                .addDisposableTo(ephemeralBag)

        viewModel.navigateToMotivations
                .ui()
                .subscribe {
                    baseActivity?.navigateToFragment(
                            MotivationsFragment.newInstance(it),
                            MotivationsFragment.fragmentTag(it))
                }
                .addDisposableTo(ephemeralBag)

        viewModel.redirectToAdviceAidNext
                .ui()
                .subscribe {
                    baseActivity?.navigateToFragment(
                            HomeFragment.newInstance(asAdviceAid = true),
                            HomeFragment.fragmentTag(asAdviceAid = true))
                }
                .addDisposableTo(ephemeralBag)

        viewModel.navigateToAdviceAidCompleted
                .ui()
                .subscribe {
                    baseActivity?.navigateToFragment(
                            AdviceAidFragment.newInstance(true),
                            AdviceAidFragment.fragmentTag(true))
                }
                .addDisposableTo(ephemeralBag)

        viewModel.showShareOptions
                .ui()
                .subscribe {
                    val builder = AlertDialog.Builder(activity)

                    builder.setTitle(resources.getString(R.string.question_how_to_share))

                    // Add more options later.
                    builder.setItems(arrayOf<CharSequence>("Facebook")
                    ) { dialog, which ->
                        if (which == 0) {
                            // Facebook
                            shareWithFacebook(it)
                        }
                    }
                    builder.show()
                }
                .addDisposableTo(ephemeralBag)

        viewModel.voteCastActivityIndicator
                .ui()
                .subscribe {
                    voting_now_overlay?.visibility = if (it) View.VISIBLE else View.GONE
                }
                .addDisposableTo(ephemeralBag)

        viewModel.voteRequiresChangeConfirmationFirst
                .ui()
                .subscribe {
                    val voteOption = it
                    AlertDialog.Builder(context)
                            .setTitle(R.string.cap_confirmation)
                            .setMessage(R.string.question_change_vote_question)
                            .setPositiveButton(R.string.cap_change, { dialog, which ->
                                confirmVoteChange.value = voteOption
                            })
                            .setNegativeButton(R.string.cap_cancel, null)
                            .show()
                }
                .addDisposableTo(ephemeralBag)

        viewModel.vote
                .ui()
                .subscribe {
                    if (it == VoteOption.AGREE) {
                        vote_agree_layout.background = ContextCompat.getDrawable(context,
                                R.drawable.shape_rounded_corners_vote_on_button)
                        vote_disagree_layout.background = ContextCompat.getDrawable(context,
                                R.drawable.shape_rounded_corners_vote_off_button)
                    } else if (it == VoteOption.DISAGREE) {
                        vote_agree_layout.background = ContextCompat.getDrawable(context,
                                R.drawable.shape_rounded_corners_vote_off_button)
                        vote_disagree_layout.background = ContextCompat.getDrawable(context,
                                R.drawable.shape_rounded_corners_vote_on_button)
                    } else {
                        vote_agree_layout.background = ContextCompat.getDrawable(context,
                                R.drawable.shape_rounded_corners_vote_off_button)
                        vote_disagree_layout.background = ContextCompat.getDrawable(context,
                                R.drawable.shape_rounded_corners_vote_off_button)
                    }
                }
                .addDisposableTo(ephemeralBag)

        viewModel.errorMotionClosed
                .ui()
                .subscribe {
                    baseActivity?.displayToast(ToastType.ERROR, it)
                }
                .addDisposableTo(ephemeralBag)

        viewModel.errorVoteCast
                .ui()
                .subscribe {
                    val (title, message) = it

                    baseActivity?.displayToast(ToastType.ERROR, message)
                }
                .addDisposableTo(ephemeralBag)

        viewModel.voteAlreadyCastMessage
                .ui()
                .subscribe {
                    baseActivity?.displayToast(ToastType.ERROR, it)
                }
                .addDisposableTo(ephemeralBag)

        viewModel.prepareLayoutOverlayHUDLoader
                .ui()
                .subscribe { toggleLoadingOverlay(it) }
                .addDisposableTo(ephemeralBag)

        viewModel.removeAuthToken
                .subscribe {
                    authService.removeAllTokens()
                }
                .addDisposableTo(ephemeralBag)

        viewModel.navigateToLoginActivity
                .ui()
                .subscribe {
                    (activity as HomeActivity).navigateToLoginActivity()
                }
                .addDisposableTo(ephemeralBag)
    }

    override fun setVisibilityOnLoadingOverlay(v: Boolean) {
        still_loading_overlay?.visibility = if (v) View.VISIBLE else View.GONE
    }

    private fun shareWithFacebook(shareMotion: ShareMotion) {
        val callbackManager = CallbackManager.Factory.create()
        val shareDialog = ShareDialog(this)

        shareDialog.registerCallback(callbackManager, object : FacebookCallback<Sharer.Result> {
            override fun onSuccess(result: Sharer.Result?) {
                baseActivity?.displayToast(ToastType.SUCCESS,
                        resources.getString(R.string.message_posted_to_facebook))
            }

            override fun onCancel() { }

            override fun onError(error: FacebookException?) {
                baseActivity?.displayToast(ToastType.ERROR,
                        resources.getString(R.string.error_not_posted_to_facebook))
            }
        })

        if (ShareDialog.canShow(ShareLinkContent::class.java)) {
            val content = ShareLinkContent.Builder()
                    .setContentUrl(shareMotion.contentUri)
                    .setContentDescription(shareMotion.contentDescription(ShareType.FACEBOOK))
                    .setContentTitle(shareMotion.contentTitle(ShareType.FACEBOOK))
                    .setImageUrl(shareMotion.contentImageUri(ShareType.FACEBOOK))
                    .build()

            shareDialog.show(content)
        }
    }

    /** Helper method to set the weight of a View (i.e. a line) */
    private fun _setLineWeight(v: View, weight: Double) {
        val p1 = v.layoutParams as LinearLayout.LayoutParams
        p1.weight = weight.toFloat()
        v.layoutParams = p1
    }
}