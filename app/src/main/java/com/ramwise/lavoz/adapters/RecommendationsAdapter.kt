package com.ramwise.lavoz.adapters

import com.ramwise.lavoz.models.Organization
import android.content.ClipData.Item
import android.content.Context
import android.database.Cursor
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.*
import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.R
import com.ramwise.lavoz.models.constants.MotionBodypartType
import com.ramwise.lavoz.viewmodels.adapters.*
import com.squareup.picasso.Picasso


class RecommendationsAdapter(val items: List<RecommendationAdapterViewModel>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        val inflater = parent?.context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
                as LayoutInflater

        return RecommendationsViewHolder(inflater.inflate(R.layout.adapter_recommendation, parent,
                false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        val viewHolder = holder as RecommendationsViewHolder
        val viewModel = items[position]

        viewHolder.pointsTextView.text = viewModel.pointsText
        viewHolder.nameTextView.text = viewModel.nameText

        if (viewModel.iconURL != null)
            Picasso.with(LavozApplication.context)
                    .load(viewModel.iconURL)
                    .into(viewHolder.iconImageView)
    }

    // MARK: - ViewHolders

    class RecommendationsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconImageView = itemView.findViewById(R.id.party_icon) as ImageView
        val pointsTextView = itemView.findViewById(R.id.party_points) as TextView
        val nameTextView = itemView.findViewById(R.id.party_name) as TextView
    }
}