package com.ramwise.lavoz.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.ramwise.lavoz.R
import com.squareup.picasso.Picasso
import com.ramwise.lavoz.models.constants.MotionStatus
import com.ramwise.lavoz.utils.rx.addDisposableTo
import com.ramwise.lavoz.utils.rx.ui
import com.ramwise.lavoz.viewmodels.adapters.MotionListConcludedAdapterViewModel
import com.ramwise.lavoz.viewmodels.adapters.MotionListOpenAdapterViewModel
import com.ramwise.lavoz.viewmodels.adapters.interfaces.MotionListGenericAdapterViewModel
//import kotlinx.android.synthetic.main.fragment_home.*
import rx.Observable
import rx.subscriptions.CompositeSubscription
import java.lang.ref.WeakReference
import java.util.*


/**
 * An adapter for a list of Motion objects.
 *
 * @param items A list of MotionListGenericAdapterViewModels
 *
 * @param disposeBag A bag to which all subscriptions can be added for disposal after completion.
 *                   Normal adapters don't subscribe to things, but this one does because it has
 *                   to regularly update the view based on the Observable.
 */
open class MotionListAdapter(val items: ArrayList<MotionListGenericAdapterViewModel>) :
        RecyclerView.Adapter<MotionListAdapter.MotionListGenericViewHolder>() {

    init {
        this.setHasStableIds(true)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemId(position: Int): Long {
        return items[position].id.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type
    }

    fun swap(newItems: ArrayList<MotionListGenericAdapterViewModel>) {
        items.clear()
        items.addAll(newItems)

        notifyDataSetChanged()
    }

    /** Should be implemented by an (anonymous) subclass in a Fragment or Activity that wishes to
     * handle cell clicks.
     *
     * @param layoutPosition The position in the RecyclerView of the cell that was clicked.
     *
     * @param motionStatus The MotionStatus value of the cell that was clicked.
     */
    open fun onCellClicked(layoutPosition: Int, motionStatus: Int) {}

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): MotionListGenericViewHolder {
        val inflater = parent?.context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
                as LayoutInflater

        if (MotionStatus.CONCLUDED == viewType) {
            return MotionListConcludedViewHolder(
                        inflater.inflate(R.layout.adapter_motionlist_concluded, parent, false),
                        WeakReference(this))
        }
        else {
            return MotionListOpenViewHolder(
                        inflater.inflate(R.layout.adapter_motionlist_open, parent, false),
                        WeakReference(this))
        }
    }

    override fun onBindViewHolder(holder: MotionListGenericViewHolder?, position: Int) {
        val viewType = getItemViewType(position)

        if (MotionStatus.OPEN == viewType) {
            val viewHolder = holder as MotionListOpenViewHolder
            val viewModel = items[position] as MotionListOpenAdapterViewModel

            viewHolder.disposeBag.clear()

            viewHolder.issueTextView.text = viewModel.issueText

            viewModel.timeAgoText
                    .ui()
                    .subscribe {
                        viewHolder.timeTextView.text = it
                    }
                    .addDisposableTo(viewHolder.disposeBag)

            viewModel.remainingTimeFraction
                    .ui()
                    .subscribe {
                        _setLineWeight(viewHolder.timerLineRemaining, it)
                        _setLineWeight(viewHolder.timerLineGone, 1 - it)
                    }
                    .addDisposableTo(viewHolder.disposeBag)

            if (viewModel.mainImageURL != null)
                Picasso.with(viewHolder.mainImageView.context)
                        .load(viewModel.mainImageURL)
                        .placeholder(R.drawable.banner_placeholder)
                        .into(viewHolder.mainImageView)
            else
                viewHolder.mainImageView.setImageResource(R.drawable.banner_placeholder)

        }
        else if (MotionStatus.CONCLUDED == viewType) {
                val viewHolder = holder as MotionListConcludedViewHolder
                val viewModel = items[position] as MotionListConcludedAdapterViewModel

            viewHolder.issueTextView.text = viewModel.issueText
            viewHolder.dateTextView.text = viewModel.dateText
            viewHolder.resultsAgreeTextView.text = viewModel.resultsAgreeText
            viewHolder.resultsDisagreeTextView.text = viewModel.resultsDisagreeText
            viewHolder.voteIndicatorTextView.text = viewModel.voteIndicatorText

            if (viewModel.mainImageURL != null)
                Picasso.with(viewHolder.mainImageView.context)
                        .load(viewModel.mainImageURL)
                        .placeholder(R.drawable.banner_placeholder)
                        .into(viewHolder.mainImageView)
            else
                viewHolder.mainImageView.setImageResource(R.drawable.banner_placeholder)
        }
    }

    // MARK: - ViewHolders

    /** Any subclass of this GenericViewHolder should still set the onClickListener in their init
     * method. This cannot be done in this generic superclass because it is unwise to reference
     * 'this' in a non-final constructor.
     */
    open class MotionListGenericViewHolder(itemView: View,
                                           val weakAdapter: WeakReference<MotionListAdapter>) :
            RecyclerView.ViewHolder(itemView) {
        open val type = MotionStatus.UNDEFINED
    }

    class MotionListOpenViewHolder(itemView: View,
                                   weakAdapter: WeakReference<MotionListAdapter>) :
            MotionListGenericViewHolder(itemView, weakAdapter), View.OnClickListener {
        override val type = MotionStatus.OPEN

        /** All Observable subscriptions in [onBindViewHolder] should be added to this bag. Whenever
         * [onBindViewHolder] gets called again, the bag should be cleared before adding new
         * subscriptions.
         */
        val disposeBag = CompositeSubscription()

        val mainImageView = itemView.findViewById(R.id.main_imageview) as ImageView
        val issueTextView = itemView.findViewById(R.id.issue_textview) as TextView
        val timeTextView = itemView.findViewById(R.id.timer_textview) as TextView
        val timerLineRemaining: View = itemView.findViewById(R.id.timer_line_remaining)
        val timerLineGone: View = itemView.findViewById(R.id.timer_line_gone)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            // Make sure that setOnClickListener is called in the subclass!
            weakAdapter.get().onCellClicked(layoutPosition, MotionStatus.OPEN)
        }
    }

    class MotionListConcludedViewHolder(itemView: View,
                                        weakAdapter: WeakReference<MotionListAdapter>) :
            MotionListGenericViewHolder(itemView, weakAdapter), View.OnClickListener  {
        override val type = MotionStatus.CONCLUDED

        val mainImageView = itemView.findViewById(R.id.main_imageview) as ImageView
        val issueTextView = itemView.findViewById(R.id.issue_textview) as TextView
        val dateTextView = itemView.findViewById(R.id.date_textview) as TextView
        val resultsDisagreeTextView = itemView.findViewById(R.id.results_disagree_textview)
                as TextView
        val resultsAgreeTextView = itemView.findViewById(R.id.results_agree_textview)
                as TextView
        val voteIndicatorTextView = itemView.findViewById(R.id.vote_indicator_textview)
                as TextView

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            // Make sure that setOnClickListener is called in the subclass!
            weakAdapter.get().onCellClicked(layoutPosition, MotionStatus.CONCLUDED)
        }
    }

    /** Helper method to set the weight of a View (i.e. a line) */
    private fun _setLineWeight(v: View, weight: Double) {
        val p1 = v.layoutParams as LinearLayout.LayoutParams
        p1.weight = weight.toFloat()
        v.layoutParams = p1
    }
}
