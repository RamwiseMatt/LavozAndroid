package com.ramwise.lavoz

import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter


class LavozStatic {
    companion object {
        /** Provides a Joda Time DateTimeFormat object that is configured to deal with Lavoz API
         * datetime strings.
         */
        val dateTimeFormat: DateTimeFormatter = DateTimeFormat.forPattern(LavozConstants.DATE_FORMAT)
    }
}