package com.ramwise.lavoz.viewmodels.adapters

import com.ramwise.lavoz.R
import com.ramwise.lavoz.models.Organization
import com.ramwise.lavoz.models.constants.ImageSizeClass
import com.ramwise.lavoz.viewmodels.BaseAdapterViewModel


class RecommendationAdapterViewModel(organization: Organization) : BaseAdapterViewModel() {

    private val points = organization.score?.points ?: 0

    val pointsText = points.toString() + " " + resources.getString(R.string.nocap_points)
    val nameText = organization.name
    var iconURL = organization.icon?.urlByImageSizeClass(ImageSizeClass.MEDIUM)
}