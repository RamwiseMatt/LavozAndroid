package com.ramwise.lavoz.utils.exceptions


/** A simple Exception that can be used anywhere in the application to raise awareness of
 * malpractice. Should not be used silently.
 */
class NativeException(override val message: String?) : Throwable()