package com.ramwise.lavoz.adapters

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.R
import com.ramwise.lavoz.models.constants.CommentVoteOption
import com.ramwise.lavoz.models.constants.MotionStatus
import com.ramwise.lavoz.models.constants.MotivationsViewType
import com.ramwise.lavoz.viewmodels.adapters.MotivationsHeaderAdapterViewModel
import com.ramwise.lavoz.viewmodels.adapters.MotivationsMotivationAdapterViewModel
import com.ramwise.lavoz.viewmodels.adapters.interfaces.MotivationsGenericAdapterViewModel
import com.squareup.picasso.Picasso

import java.lang.ref.WeakReference
import java.util.*


/**
 * An adapter for a list of Motivation objects.
 *
 * @param items A list of [MotivationsGenericAdapterViewModel]s
 */
open class MotivationsAdapter(val items: ArrayList<MotivationsGenericAdapterViewModel>) :
        RecyclerView.Adapter<MotivationsAdapter.MotivationsGenericViewHolder>() {

    private val displayMetrics = LavozApplication.context.resources.displayMetrics

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

    fun swap(newItems: ArrayList<MotivationsGenericAdapterViewModel>) {
        items.clear()
        items.addAll(newItems)

        notifyDataSetChanged()
    }

    /** Should be implemented by an (anonymous) subclass in a Fragment or Activity that wishes to
     * handle cell clicks.
     *
     * @param position The position in the RecyclerView of the cell that was clicked.
     * @param sectionPosition The position of the item within its section of the recyclerView.
     */
    open fun onCellClicked(position: Int, sectionPosition: Int) {}

    /** Should be implemented by an (anonymous) subclass in a Fragment or Activity that wishes to
     * handle reply button clicks.
     *
     * @param position The position in the RecyclerView of the cell that was clicked.
     * @param sectionPosition The position of the item within its section of the recyclerView.
     */
    open fun onReplyClicked(position: Int, sectionPosition: Int) {}

    /** Should be implemented by an (anonymous) subclass in a Fragment or Activity that wishes to
     * handle heart/trash button clicks.
     *
     * @param position The position in the RecyclerView of the cell that was clicked.
     * @param sectionPosition The position of the item within its section of the recyclerView.
     * @param commentVoteOption A CommentVoteOption constant indicating whether this was an upvote
     *                          or a removal of an upvote. If the icon is a trash button, then
     *                          CommentVoteOption.UNDEFINED is used here.
     */
    open fun onHeartOrTrashClicked(position: Int, sectionPosition: Int, commentVoteOption: Int) {}

    /** Internal method that should be called from the viewHolder. This method determines the
     * second parameter for [onHeartOrTrashClicked]. It also changes the viewModel slightly and
     * calls [notifyDataSetChanged] to reflect the new heart icon.
     */
    private fun onHeartOrTrashClicked(position: Int, sectionPosition: Int) {
        val viewModel = items[position]

        if (viewModel is MotivationsMotivationAdapterViewModel) {
            if (viewModel.showDeleteButton) {
                onHeartOrTrashClicked(position, sectionPosition, CommentVoteOption.UNDEFINED)
            } else {
                onHeartOrTrashClicked(position, sectionPosition,
                        if (viewModel.heartIsFilled) CommentVoteOption.CLEAR
                        else CommentVoteOption.UPVOTE)

                // The heart icon and heart count just changed. This is not reflected by the
                // stored API data, but we can at least temporarily update the cell's viewmodel.
                viewModel.heartIsFilled = !viewModel.heartIsFilled
                viewModel.upvoteCount += if (viewModel.heartIsFilled) 1 else -1
            }

            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): MotivationsGenericViewHolder {
        val inflater = parent?.context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
                as LayoutInflater

        if (MotivationsViewType.HEADER == viewType) {
            return MotivationsHeaderViewHolder(
                        inflater.inflate(R.layout.adapter_motivations_header, parent, false),
                        WeakReference(this))
        }
        else {
            return MotivationsMotivationViewHolder(
                        inflater.inflate(R.layout.adapter_motivations_motivation, parent, false),
                        WeakReference(this))
        }
    }

    override fun onBindViewHolder(holder: MotivationsGenericViewHolder?, position: Int) {
        val viewType = getItemViewType(position)

        if (MotivationsViewType.HEADER == viewType) {
            val viewHolder = holder as MotivationsHeaderViewHolder
            val viewModel = items[position] as MotivationsHeaderAdapterViewModel

            viewHolder.issueTextView.text = viewModel.issueText
            viewHolder.headerImageAcknowledgementTextView.text = viewModel.acknowledgementText

            if (viewModel.headerImageURL != null)
                Picasso.with(viewHolder.headerImageView.context)
                        .load(viewModel.headerImageURL)
                        .placeholder(R.drawable.banner_placeholder)
                        .into(viewHolder.headerImageView)
            else
                viewHolder.headerImageView.setImageResource(R.drawable.banner_placeholder)
        }
        else if (MotivationsViewType.MOTIVATION == viewType) {
            val viewHolder = holder as MotivationsMotivationViewHolder
            val viewModel = items[position] as MotivationsMotivationAdapterViewModel

            val constraintSet = ConstraintSet()
            constraintSet.clone(viewHolder.constraintLayout)
            constraintSet.setMargin(R.id.motivation_container_layout, ConstraintSet.START,
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                            viewModel.insetByPoints.toFloat(), displayMetrics).toInt())
            constraintSet.applyTo(viewHolder.constraintLayout)

            viewHolder.authorTextView.text = viewModel.authorText
            viewHolder.motivationTextView.text = viewModel.motivationText
            viewHolder.authorTextView.setTextColor(viewModel.authorTextColor)
            viewHolder.moreCommentsContainerLayout.visibility = if (viewModel.isExpandable)
                View.VISIBLE else View.GONE
            viewHolder.expandCollapseIconImageView.setImageResource(if (viewModel.isExpanded)
                R.drawable.icon_minus_white else R.drawable.icon_plus_white)
            viewHolder.responsesCountTextView.text = viewModel.countResponsesText
            viewHolder.heartsCountTextView.text = viewModel.upvoteCountText

            viewHolder.verifiedIconImageView.visibility = if (viewModel.verifiedIconHidden)
                View.GONE else View.VISIBLE

            viewHolder.replyButton.visibility = if (viewModel.actionButtonHidden)
                View.GONE else View.VISIBLE

            if (viewModel.showDeleteButton)
                viewHolder.heartOrTrashButton.setImageResource(R.drawable.icon_trash_cf000f)
            else
                viewHolder.heartOrTrashButton.setImageResource(
                        if (viewModel.heartIsFilled) R.drawable.icon_heartfilled_cf000f
                        else R.drawable.icon_heart_cf000f)
        }
    }

    // MARK: - ViewHolders

    /** Any subclass of this GenericViewHolder should still set the onClickListener in their init
     * method. This cannot be done in this generic superclass because it is unwise to reference
     * 'this' in a non-final constructor.
     */
    open class MotivationsGenericViewHolder(itemView: View,
                                           val weakAdapter: WeakReference<MotivationsAdapter>) :
            RecyclerView.ViewHolder(itemView) {
        open val type = MotivationsViewType.UNDEFINED
    }

    class MotivationsHeaderViewHolder(itemView: View,
                                   weakAdapter: WeakReference<MotivationsAdapter>) :
            MotivationsGenericViewHolder(itemView, weakAdapter){
        override val type = MotivationsViewType.HEADER

        val headerImageView = itemView.findViewById(R.id.header_imageview) as ImageView
        val headerImageAcknowledgementTextView = itemView.findViewById(
                R.id.headerimage_acknowledgement_textview) as TextView
        val issueTextView = itemView.findViewById(R.id.issue_textview) as TextView
    }

    class MotivationsMotivationViewHolder(itemView: View,
                                        weakAdapter: WeakReference<MotivationsAdapter>) :
            MotivationsGenericViewHolder(itemView, weakAdapter), View.OnClickListener  {
        override val type = MotivationsViewType.MOTIVATION

        val constraintLayout = itemView as ConstraintLayout
        val motivationLayout = itemView.findViewById(R.id.motivation_container_layout)
                as ConstraintLayout
        val authorTextView = itemView.findViewById(R.id.author_textview) as TextView
        val motivationTextView = itemView.findViewById(R.id.motivation_textview) as TextView

        val moreCommentsContainerLayout = itemView.findViewById(R.id.more_comments_container_layout)
                as ViewGroup

        val expandCollapseIconImageView = itemView.findViewById(R.id.expand_collapse_icon_imageview)
                as ImageView
        val verifiedIconImageView = itemView.findViewById(R.id.verified_icon_imageview)
                as ImageView

        val responsesCountTextView = itemView.findViewById(R.id.responses_count_textview)
                as TextView
        val heartsCountTextView = itemView.findViewById(R.id.hearts_count_textview)
                as TextView

        val replyButton = itemView.findViewById(R.id.reply_button) as ImageButton
        val heartOrTrashButton = itemView.findViewById(R.id.heart_or_trash_button) as ImageButton

        init {
            itemView.setOnClickListener(this)
            replyButton.setOnClickListener(this)
            heartOrTrashButton.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            if (layoutPosition == 0)
                // Do not handle click events on the header.
                return

            // Make sure that setOnClickListener is called in the subclass!
            when(p0?.id) {
                itemView.id -> weakAdapter.get().onCellClicked(layoutPosition,
                        // - 1 is because there is 1 header item
                        layoutPosition - 1)
                replyButton.id -> weakAdapter.get().onReplyClicked(layoutPosition,
                        // - 1 is because there is 1 header item
                        layoutPosition - 1)
                heartOrTrashButton.id -> weakAdapter.get().onHeartOrTrashClicked(layoutPosition,
                        // - 1 is because there is 1 header item
                        layoutPosition - 1)
            }
        }
    }
}
