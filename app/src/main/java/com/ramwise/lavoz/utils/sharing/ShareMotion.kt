package com.ramwise.lavoz.utils.sharing

import android.content.res.Resources
import android.net.Uri
import com.ramwise.lavoz.LavozConstants
import com.ramwise.lavoz.R
import com.ramwise.lavoz.models.Motion
import com.ramwise.lavoz.models.constants.ShareType


class ShareMotion(val motion: Motion, val resources: Resources) {
    val contentURLAsString : String
        get() {
            return LavozConstants.WEB_BASE_LINK_FOR_MOTION + motion.id.toString()
        }

    val contentUri : Uri
        get() {
            return Uri.parse(contentURLAsString)
        }

    fun contentTitle(shareType: Int) : String {
        when(shareType) {
            ShareType.FACEBOOK -> return _facebookContentTitle()
            ShareType.TWITTER -> return _twitterContentTitle()
            ShareType.LINKEDIN -> return _linkedInContentTitle()
        }
        return ""
    }

    fun contentDescription(shareType: Int) : String {
        when(shareType) {
            ShareType.FACEBOOK -> return _facebookContentDescription()
            ShareType.LINKEDIN -> return _linkedInContentDescription()
        }
        return ""
    }

    fun contentImageUri(shareType: Int) : Uri? {
        when(shareType) {
            ShareType.FACEBOOK -> return _facebookImageUri()
        }
        return null
    }

    fun linkedInTargetURL() : String {
        try {
            return String.format("%s&url=%s&title=%s&summary=%s&source=Lavoz",
                    "https://www.linkedin.com/shareArticle?mini=true",
                    Uri.encode(contentURLAsString),
                    Uri.encode(contentTitle(ShareType.LINKEDIN)),
                    Uri.encode(contentDescription(ShareType.LINKEDIN)))
        } catch (e: Exception) {
            return ""
        }
    }

    fun emailSubject() : String {
        return resources.getString(R.string.cap_question_on_lavoz)
    }

    fun emailBody() : String {
        return resources.getString(R.string.cap_look_at_question_colon) + contentURLAsString
    }

    private fun _facebookContentTitle() : String {
        return resources.getString(R.string.cap_question_colon) + " " + motion.issue
    }

    private fun _facebookContentDescription() : String {
        if (motion.conclusion != null) {
            val percFor = motion.conclusion!!.percentageFor
            val percAgainst = motion.conclusion!!.percentageAgainst

            val p1 = if (percFor >= percAgainst) percFor else percAgainst
            val p2 = (if (percFor >= percAgainst) motion.labelFor else motion.labelAgainst) ?: ""
            val p3 = if (percFor < percAgainst) percFor else percAgainst
            val p4 = (if (percFor < percAgainst) motion.labelFor else motion.labelAgainst) ?: ""

            return String.format(resources.getString(R.string.results_formatted),
                    p1, p2.toLowerCase(), p3, p4.toLowerCase())
        }
        return resources.getString(R.string.message_be_heard)
    }

    private fun _facebookImageUri() : Uri {
        return Uri.parse(LavozConstants.IMAGE_LOGO_LINK)
    }

    private fun _twitterContentTitle() : String {
        var contentTitle = resources.getString(R.string.cap_question_on_lavoz)
        var contentTitleBaseLength = 26 // url length

        if (contentTitle.length > 140 - contentTitleBaseLength) {
            contentTitleBaseLength += 3 // ellipsis
            contentTitle = contentTitle.substring(0, 140 - contentTitleBaseLength) + "..."
        }

        return contentTitle
    }

    private fun _linkedInContentTitle() : String {
        // TO-DO: implement this.
        return ""
    }

    private fun _linkedInContentDescription() : String {
        // TO-DO: implement this.
        return ""
    }
}