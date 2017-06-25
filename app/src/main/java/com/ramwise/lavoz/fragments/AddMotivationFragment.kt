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
import com.squareup.picasso.Picasso
import rx.Observable
import java.util.*
import android.content.Intent
import android.net.Uri
import android.view.inputmethod.InputMethodManager
import com.jakewharton.rxbinding.widget.checked
import com.ramwise.lavoz.models.constants.ToastType
import com.ramwise.lavoz.utils.rx.RxVariable
import com.ramwise.lavoz.utils.rx.unwrap
import com.ramwise.lavoz.viewmodels.fragments.AddMotivationFragmentViewModel
import kotlinx.android.synthetic.main.fragment_addmotivation.*
import java.util.concurrent.TimeUnit
import android.view.inputmethod.InputMethodManager.HIDE_IMPLICIT_ONLY
import android.content.Context.INPUT_METHOD_SERVICE
import android.support.v4.content.ContextCompat
import com.jakewharton.rxbinding.widget.textChanges


class AddMotivationFragment : BaseFragment() {
    companion object {
        fun newInstance(forMotionId: Int, motionLabelFor: String, motionLabelAgainst: String,
                        rootVoteOption: Int, replyToCommentId: Int?, replyToAuthorName: String?):
                AddMotivationFragment {
            val obj = AddMotivationFragment()

            val args = Bundle()
            args.putSerializable("forMotionId", forMotionId)
            args.putSerializable("motionLabelFor", motionLabelFor)
            args.putSerializable("motionLabelAgainst", motionLabelAgainst)
            args.putSerializable("rootVoteOption", rootVoteOption)

            if (replyToCommentId != null) args.putSerializable("replyToCommentId", replyToCommentId)
            if (replyToAuthorName != null) args.putSerializable("replyToAuthorName", replyToAuthorName)
            obj.arguments = args

            return obj
        }

        fun fragmentTag(forMotionId: Int) : String {
            return "add_motivation_" + forMotionId.toString()
        }
    }

    // Set the actual values of these in onCreate() from the Bundle.
    var forMotionId: Int = 0
    var motionLabelFor: String = ""
    var motionLabelAgainst: String = ""
    var rootVoteOption: Int = VoteOption.UNDEFINED
    var replyToCommentId: Int? = null
    var replyToAuthorName: String? = null

    lateinit var viewModel: AddMotivationFragmentViewModel

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        title = resources.getString(R.string.cap_leave_a_thought)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Required parameters
        forMotionId = arguments.getInt("forMotionId")
        motionLabelFor = arguments.getString("motionLabelFor")
        motionLabelAgainst = arguments.getString("motionLabelAgainst")
        rootVoteOption = arguments.getInt("rootVoteOption")

        // Optional parameters
        val replyToCommentId_ = arguments.getInt("replyToCommentId")
        val replyToAuthorName_: String? = arguments.getString("replyToAuthorName")

        replyToCommentId = if (replyToCommentId_ == 0) null else replyToCommentId_
        replyToAuthorName = replyToAuthorName_

        // Purge the bundle to avoid a "Parcel: unable to marshal value" error when pausing the
        // fragment.
        arguments.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_addmotivation, container, false)

        return view
    }

    override fun onResume() {
        super.onResume()

        motivation_input_field.requestFocus()
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    override fun onPause() {
        super.onPause()

        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(motivation_input_field.windowToken, 0)
    }

    override fun createViewModel() {
        viewModel = AddMotivationFragmentViewModel(
                forMotionId,
                motionLabelFor,
                motionLabelAgainst,
                rootVoteOption,
                replyToCommentId,
                replyToAuthorName,
                Observable.defer { motivation_input_field.textChanges() }.share(),
                Observable.defer { cancel_button.clicks() }.share(),
                Observable.defer { add_button.clicks() }.share())
    }

    override fun configureUIObservers() {

        viewModel.tagLine.ui().subscribe(share_thought_tag_textview.text()).addDisposableTo(ephemeralBag)
        viewModel.charactersLeft.ui().subscribe(remaining_count_textview.text()).addDisposableTo(ephemeralBag)

        viewModel.addButtonEnabled
                .ui()
                .subscribe {
                    add_button.isClickable = it
                    add_button.setBackgroundResource(
                            if (it) R.drawable.round_accent_button
                            else R.drawable.round_disabled_button)
                }
                .addDisposableTo(ephemeralBag)

        viewModel.navigateToMotivations
                .ui()
                .subscribe { activity.onBackPressed() }
                .addDisposableTo(ephemeralBag)

        viewModel.errorCannotAddMotivation
                .ui()
                .subscribe {
                    baseActivity?.displayToast(ToastType.ERROR, it)
                }
                .addDisposableTo(ephemeralBag)

        viewModel.addingMotivationOverlayHUDLoader
                .ui()
                .subscribe {
                    if (it)
                        action_now_text.text = resources.getString(
                                R.string.message_motivation_being_deleted)

                    action_now_overlay?.visibility = if (it) View.VISIBLE else View.GONE
                }
                .addDisposableTo(ephemeralBag)
    }

    override fun setVisibilityOnLoadingOverlay(v: Boolean) {
        still_loading_overlay?.visibility = if (v) View.VISIBLE else View.GONE
    }

}