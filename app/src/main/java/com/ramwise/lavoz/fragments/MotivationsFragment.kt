package com.ramwise.lavoz.fragments

import android.content.Context
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding.view.clicks
import com.ramwise.lavoz.R
import com.ramwise.lavoz.adapters.MotivationsAdapter
import com.ramwise.lavoz.models.constants.CommentVoteOption
import com.ramwise.lavoz.models.constants.ToastType
import com.ramwise.lavoz.utils.rx.RxVariable
import com.ramwise.lavoz.utils.rx.addDisposableTo
import com.ramwise.lavoz.utils.rx.ui
import com.ramwise.lavoz.viewmodels.adapters.interfaces.MotivationsGenericAdapterViewModel
import com.ramwise.lavoz.viewmodels.fragments.MotivationsFragmentViewModel
import kotlinx.android.synthetic.main.fragment_motivations.*
import rx.Observable
import java.lang.ref.WeakReference
import java.util.*


class MotivationsFragment : BaseFragment() {
    companion object {
        fun newInstance(forMotionId: Int): MotivationsFragment {
            val obj = MotivationsFragment()

            val args = Bundle()
            args.putSerializable("forMotionId", forMotionId)
            obj.arguments = args

            return obj
        }

        fun fragmentTag(forMotionId: Int) : String { return "motivations_" + forMotionId.toString() }
    }

    var forMotionId: Int = 0 // Set the actual value in onCreate() from the Bundle.

    lateinit var viewModel: MotivationsFragmentViewModel

    /** Emits a signal whenever the recyclerView is clicked. The emitted
     * value is the section position (i.e. the cell position - 1 for the header).
     * The initial value can be ignored.
     */
    private val recyclerViewClicked = RxVariable(-1)

    /** Emits a signal whenever the recyclerView's reply button is clicked. The emitted
     * value is the section position (i.e. the cell position - 1 for the header).
     * The initial value can be ignored.
     */
    private val recyclerViewReplyClicked = RxVariable(-1)

    /** Emits a signal whenever the recyclerView's heart/trash button is clicked. The emitted
     * value is a Pair with the section position (i.e. the cell position - 1 for the header) and a
     * CommentVoteOption constant that indicates what action should be taken.
     *
     * The initial value can be ignored.
     */
    private val recyclerViewHeartOrTrashClicked = RxVariable(Pair(-1, CommentVoteOption.UNDEFINED))

    /** Emits a signal whenever the user confirms that they wish to delete a certain comment.
     * The emitted value is the motivation id to be deleted.
     *
     * The initial value can be ignored.
     */
    private val confirmTrashClicked = RxVariable(-1)

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

        title = resources.getString(R.string.cap_motivations)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        forMotionId = arguments.getInt("forMotionId")

        // Purge the bundle to avoid a "Parcel: unable to marshal value" error when pausing the
        // fragment.
        arguments.remove("forMotionId")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_motivations, container, false)

        layoutManager = LinearLayoutManager(activity.applicationContext)

        val recyclerView = view.findViewById(R.id.recycler_view) as RecyclerView
        recyclerView.layoutManager = layoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.isNestedScrollingEnabled = false

        val fab = view.findViewById(R.id.floating_action_button) as FloatingActionButton

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 || (dy < 0 && fab.isShown)) fab.hide()
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) fab.show()
                super.onScrollStateChanged(recyclerView, newState)
            }
        })

        // Set an adapter on an empty ArrayList.
        recyclerView.adapter = object :
                MotivationsAdapter(ArrayList<MotivationsGenericAdapterViewModel>()) {
            override fun onCellClicked(position: Int, sectionPosition: Int) {
                weakThis.get().recyclerViewClicked.value = sectionPosition
            }
            override fun onReplyClicked(position: Int, sectionPosition: Int) {
                weakThis.get().recyclerViewReplyClicked.value = sectionPosition
            }
            override fun onHeartOrTrashClicked(position: Int, sectionPosition: Int,
                                               commentVoteOption: Int) {
                weakThis.get().recyclerViewHeartOrTrashClicked.value =
                        Pair(sectionPosition, commentVoteOption)
            }
        }

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
        viewModel = MotivationsFragmentViewModel(
                forMotionId,
                recyclerViewClicked.asObservable().filter { it > - 1}.share(),
                Observable.merge(
                        Observable
                                .defer { floating_action_button.clicks() }
                                .map { null as Int? }
                                .share(),
                        recyclerViewReplyClicked.asObservable()
                                .filter { it > - 1}
                                .share()
                ),
                recyclerViewHeartOrTrashClicked.asObservable().filter { it.first > - 1}.share(),
                confirmTrashClicked.asObservable().filter { it > - 1}.share()
        )
    }

    override fun prepareConfigureUIObservers() {
        recyclerViewClicked.value = -1
        recyclerViewReplyClicked.value = -1
        recyclerViewHeartOrTrashClicked.value = Pair(-1, CommentVoteOption.UNDEFINED)
        confirmTrashClicked.value = -1
    }

    override fun configureUIObservers() {
        viewModel.adapterViewModels
                .ui()
                .subscribe {
                    (recycler_view.adapter as MotivationsAdapter).swap(it)

                    if (savedLayoutPosition != null) {
                        layoutManager?.scrollToPosition(savedLayoutPosition ?: 0)

                        savedLayoutPosition = null
                    }
                }
                .addDisposableTo(ephemeralBag)


        viewModel.showConfirmTrashDialog
                .ui()
                .subscribe {
                    AlertDialog.Builder(context)
                            .setTitle(R.string.cap_confirmation)
                            .setMessage(R.string.question_delete_motivation)
                            .setPositiveButton(R.string.cap_delete, { dialog, which ->
                                confirmTrashClicked.value = it
                            })
                            .setNegativeButton(R.string.cap_cancel, null)
                            .show()
                }
                .addDisposableTo(ephemeralBag)

        viewModel.prepareLayoutOverlayHUDLoader
                .ui()
                .subscribe { toggleLoadingOverlay(it) }
                .addDisposableTo(ephemeralBag)

        viewModel.deletingMotivationOverlayHUDLoader
                .ui()
                .subscribe {
                    if (it)
                        action_now_text.text = resources.getString(
                                R.string.message_motivation_being_deleted)

                    action_now_overlay?.visibility = if (it) View.VISIBLE else View.GONE
                }
                .addDisposableTo(ephemeralBag)

        viewModel.upvoteToAPI.subscribe().addDisposableTo(ephemeralBag)
        viewModel.upvoteRemoveToAPI.subscribe().addDisposableTo(ephemeralBag)
        viewModel.deleteMotivationToAPI.subscribe().addDisposableTo(ephemeralBag)

        viewModel.addButtonHidden
                .ui()
                .subscribe { floating_action_button_container?.visibility =
                        if (it) View.GONE else View.VISIBLE }
                .addDisposableTo(ephemeralBag)

        viewModel.errorCannotAddMotivation
                .ui()
                .subscribe {
                    baseActivity?.displayToast(ToastType.ERROR,
                            resources.getString(R.string.error_not_voted_before_motivation))
                }
                .addDisposableTo(ephemeralBag)

        viewModel.navigateToAddMotivation
                .ui()
                .subscribe {
                    baseActivity?.navigateToFragment(
                            AddMotivationFragment.newInstance(
                                    it.forMotionId,
                                    it.motionLabelFor,
                                    it.motionLabelAgainst,
                                    it.rootVoteOption,
                                    it.replyToCommentId,
                                    it.replyToAuthorName),
                            AddMotivationFragment.fragmentTag(it.forMotionId))
                }
                .addDisposableTo(ephemeralBag)

        viewModel.scrollToRowInSection
                .ui()
                .subscribe {
                    val (section, relativePosition) = it
                    val position = if (section == 0) 0 else relativePosition + 1

                    layoutManager?.scrollToPositionWithOffset(position, 0)
                }
                .addDisposableTo(ephemeralBag)
    }

    override fun setVisibilityOnLoadingOverlay(v: Boolean) {
        still_loading_overlay?.visibility = if (v) View.VISIBLE else View.GONE
    }
}