package com.ramwise.lavoz.utils.factories

import com.ramwise.lavoz.models.Comment
import java.util.*

class CommentFactory {
    /**
     * A helper method. This method takes a list of comments and flattens it, such that
     * all nested responses are also included in the list that is output.
     *
     * @param list The Realm List to be flattened.
     *
     * @param include A list of integers that represent the identifiers of the comments whose child responses should be included.
     *
     * @param updateRootVoteOption A boolean indicating whether the rootVoteOption parameter should
     *                             be set to a comment's parent's value.
     *
     * @return A swift list of Comment objects.
     */
    fun flatResponses(list: List<Comment>?, include: List<Int>, updateRootVoteOption: Boolean = true) : List<Comment>? {
        if (list == null) return null

        val stackAsList = list.reversed().map { Pair(it, 0) }
        val stack: Stack<Pair<Comment, Int>> = Stack()
        for (item in stackAsList) stack.push(item)

        val output = ArrayList<Comment>()

        while (stack.isNotEmpty()) {
            val (iterator, iteratorDepth) = stack.pop()
            iterator.responseDepth = iteratorDepth
            output.add(iterator)

            if (include.contains(iterator.id)) {
                iterator.isExpanded = true
                if (iterator.responses != null) {
                    for (child in iterator.responses!!) {
                        if (updateRootVoteOption)
                            child.rootVoteOption = iterator.rootVoteOption

                        stack.push(Pair(child, iteratorDepth + 1))
                    }
                }
            } else {
                iterator.isExpanded = false
            }
        }
        return output
    }
}