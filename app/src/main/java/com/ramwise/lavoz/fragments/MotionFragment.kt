package com.ramwise.lavoz.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import com.jakewharton.rxbinding.view.clicks
import com.jakewharton.rxbinding.widget.text
import com.ramwise.lavoz.R
import com.ramwise.lavoz.activities.HomeActivity
import com.ramwise.lavoz.models.constants.ToastType
import com.ramwise.lavoz.viewmodels.fragments.MotionFragmentViewModel
import com.ramwise.lavoz.utils.rx.addDisposableTo
import com.ramwise.lavoz.utils.rx.ui
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_motion.*
import rx.Observable


/**
 * @param motionId The motion id of the motion to show.
 */
class MotionFragment : BaseFragment() {
    companion object {
        fun newInstance(forMotionId: Int): MotionFragment {
            val obj = MotionFragment()

            val args = Bundle()
            args.putSerializable("forMotionId", forMotionId)
            obj.arguments = args

            return obj
        }

        fun fragmentTag(forMotionId: Int) : String { return "motion_" + forMotionId.toString() }
    }

    var forMotionId: Int = 0 // Set the actual value in onCreate() from the Bundle.

    lateinit var viewModel: MotionFragmentViewModel

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        title = resources.getString(R.string.cap_motion)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        forMotionId = arguments.getInt("forMotionId")

        // Purge the bundle to avoid a "Parcel: unable to marshal value" error when pausing the
        // fragment.
        arguments.remove("forMotionId")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_motion, container, false)

        _setupWebView(view.findViewById(R.id.webview) as WebView)

        return view
    }

    override fun onPause() {
        super.onPause()

        webview.onPause()
    }

    override fun onResume() {
        super.onResume()

        webview.onResume()
    }

    override fun createViewModel() {
        viewModel = MotionFragmentViewModel(forMotionId,
                Observable.defer { motivations_button.clicks() }.share())
    }

    override fun configureUIObservers() {

        viewModel.motionIssue.ui().subscribe(issue_textview.text()).addDisposableTo(ephemeralBag)
        viewModel.motionTimeAgoText.ui().subscribe(timer_textview.text()).addDisposableTo(ephemeralBag)
        viewModel.motionHeaderAcknowledgement.ui().subscribe(headerimage_description_textview.text()).addDisposableTo(ephemeralBag)

        viewModel.motionHeaderImageURL
                .ui()
                .subscribe {
                    Picasso.with(context)
                            .load(it)
                            .placeholder(R.drawable.banner_placeholder)
                            .into(header_imageview)
                }
                .addDisposableTo(ephemeralBag)

        viewModel.resultsContainerHidden
                .ui()
                .subscribe {
                    results_container_layout?.visibility = if (it) View.GONE else View.VISIBLE
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

        viewModel.webviewHTML
                .ui()
                .subscribe {
                    webview.loadDataWithBaseURL(null, it, "text/html", "UTF-8", null)
                }
                .addDisposableTo(ephemeralBag)

        viewModel.prepareLayoutOverlayHUDLoader
                .ui()
                .subscribe { toggleLoadingOverlay(it) }
                .addDisposableTo(ephemeralBag)

        viewModel.errorMotionNotFound
                .ui()
                .subscribe {
                    baseActivity?.displayToast(ToastType.ERROR, it)
                }
                .addDisposableTo(ephemeralBag)

        viewModel.navigateToPreviousFragment
                .ui()
                .subscribe {
                    activity.onBackPressed()
                }
                .addDisposableTo(ephemeralBag)
    }

    override fun setVisibilityOnLoadingOverlay(v: Boolean) {
        still_loading_overlay?.visibility = if (v) View.VISIBLE else View.GONE
    }

    /** Helper method to set the weight of a View (i.e. a line) */
    private fun _setLineWeight(v: View, weight: Double) {
        val p1 = v.layoutParams as LinearLayout.LayoutParams
        p1.weight = weight.toFloat()
        v.layoutParams = p1
    }

    /** Prepares the WebView and its settings. */
    private fun _setupWebView(webView: WebView) {
        webView.settings.loadWithOverviewMode = true
        webView.settings.javaScriptEnabled = true
        webView.settings.useWideViewPort = true
        webView.setBackgroundColor(Color.argb(1, 0, 0, 0))

        webView.setWebViewClient(object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
                if (url != null && url.startsWith("http")) {
                    view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    return true
                } else {
                    return super.shouldOverrideUrlLoading(view, url)
                }
            }
        })
    }
}