package com.ramwise.lavoz.fragments

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import com.ramwise.lavoz.R
import com.ramwise.lavoz.adapters.RecommendationsAdapter
import com.ramwise.lavoz.models.Organization
import com.ramwise.lavoz.utils.factories.AdapterViewModelFactory
import com.ramwise.lavoz.viewmodels.fragments.RecommendationsFragmentViewModel
import kotlinx.android.synthetic.main.fragment_motion.*
import kotlinx.android.synthetic.main.fragment_recommendations.*
import java.util.*


class RecommendationsFragment : BaseFragment() {
    companion object {
        fun newInstance(recommendations: ArrayList<Organization>): RecommendationsFragment {
            val obj = RecommendationsFragment()

            val args = Bundle()
            args.putSerializable("recommendationsArray", recommendations)
            obj.arguments = args

            return obj
        }

        fun fragmentTag() : String { return "recommendations" }
    }

    /** This value should be passed via the savedInstanceState Bundle.
     *
     * @see [newInstance]
     */
    private var recommendationsArray: ArrayList<Organization>? = null

    lateinit var viewModel: RecommendationsFragmentViewModel


    override fun onAttach(context: Context?) {
        super.onAttach(context)

        title = resources.getString(R.string.cap_party_points)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("UNCHECKED_CAST")
        recommendationsArray = arguments.getSerializable("recommendationsArray")
                as ArrayList<Organization>?

        // Purge the bundle to avoid a "Parcel: unable to marshal value" error when pausing the
        // fragment.
        arguments.remove("recommendationsArray")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_recommendations, container, false)
        val adapter = RecommendationsAdapter(AdapterViewModelFactory().build(recommendationsArray))

        // Do not use Kotlin's synthetic view binding here, because the view hasn't been
        // set yet.
        val recyclerView = view.findViewById(R.id.recycler_view) as RecyclerView

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(activity.applicationContext)
        recyclerView.itemAnimator = DefaultItemAnimator()

        return view
    }

    override fun createViewModel() {
        viewModel = RecommendationsFragmentViewModel()
    }
}