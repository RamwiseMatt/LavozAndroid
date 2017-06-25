package com.ramwise.lavoz.utils.jodatime

import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.R
import org.joda.time.DateTime
import org.joda.time.Seconds

fun DateTime.differenceToString(futureDate: DateTime, shortened: Boolean = false): String {
    val seconds = Seconds.secondsBetween(this, futureDate)?.seconds
    val resources = LavozApplication.context.resources

    if (seconds == null) return ""
    else if (seconds > 31536000) {
        val years = Math.round(seconds / 31536000.0).toInt()

        if (shortened) return years.toString() + resources.getString(R.string.shortname_y)
        else return years.toString() + " " + resources.getString(
                if (years == 1) R.string.nocap_year else R.string.nocap_years)
    }
    else if (seconds > 604800) {
        val weeks = Math.round(seconds / 604800.0).toInt()

        if (shortened) return weeks.toString() + resources.getString(R.string.shortname_w)
        else return weeks.toString() + " " + resources.getString(
                if (weeks == 1) R.string.nocap_week else R.string.nocap_weeks)
    }
    else if (seconds > 86400) {
        val days = Math.round(seconds / 86400.0).toInt()

        if (shortened) return days.toString() + resources.getString(R.string.shortname_d)
        else return days.toString() + " " + resources.getString(
                if (days == 1) R.string.nocap_day else R.string.nocap_days)
    }
    else if (seconds > 3600) {
        val hours = Math.round(seconds / 3600.0).toInt()

        if (shortened) return hours.toString() + resources.getString(R.string.shortname_h)
        else return hours.toString() + " " + resources.getString(
                if (hours == 1) R.string.nocap_hour else R.string.nocap_hours)
    }
    else if (seconds > 60) {
        val minutes = Math.round(seconds / 60.0).toInt()

        if (shortened) return minutes.toString() + resources.getString(R.string.shortname_m)
        else return minutes.toString() + " " + resources.getString(
                if (minutes == 1) R.string.nocap_minute else R.string.nocap_minutes)
    }
    else if (seconds > 0) {
        if (shortened) return resources.getString(R.string.nocap_just_nopw)
        else return resources.getString(R.string.nocap_less_than) + " " +
                resources.getString(R.string.nocap_one_minute)
    }
    else {
        if (shortened) return "0" + resources.getString(R.string.shortname_s)
        else return "0" + resources.getString(R.string.nocap_seconds)
    }
}