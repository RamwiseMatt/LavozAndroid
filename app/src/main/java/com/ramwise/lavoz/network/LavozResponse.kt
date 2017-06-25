package com.ramwise.lavoz.network

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.ramwise.lavoz.models.*


/**
 * This object represents a parsed JSON response from the Lavoz API, or a LavozError in case the
 * response was not HTTP status 200.
 *
 * This object is needed because the API always envelops its root objects in a keyword (e.g.
 * {'motion': {'title': 'title here, etc'}}, which GSON has a hard time dealing with.
 *
 * The generic type <T: Any> is simply there as an indicator for the programmer as to the expected
 * type of parsed object. Since Java dismisses generic type information at runtime, this cannot be
 * used to auto-cast or such things (see: type erasure).
 */
class LavozResponse<T: Any, LavozError>(error: Error? = null) {
    /** Should never be filled by the Gson deserializer, but set by a LavozResponseFactory.
     * If possible, this object will contain the HTTP status code or other details about the error.
     */
    var err: LavozError? = null

    @SerializedName("auth_token")
    @Expose
    var authToken: AuthToken? = null

    @Expose
    var channel: Channel? = null

    @Expose
    var channels: List<Channel>? = null

    @Expose
    var city: City? = null

    @Expose
    var cities: List<City>? = null

    @Expose
    var comment: Comment? = null

    @Expose
    var comments: List<Comment>? = null

    @SerializedName("comment_vote")
    @Expose
    var commentVote: CommentVote? = null

    @SerializedName("comment_votes")
    @Expose
    var commentVotes: List<CommentVote>? = null

    @Expose
    var file: File? = null

    @Expose
    var files: List<File>? = null

    @Expose
    var motion: Motion? = null

    @Expose
    var motions: List<Motion>? = null

    @Expose
    var organization: Organization? = null

    @Expose
    var organizations: List<Organization>? = null

    @Expose
    var overview: Overview? = null

    @Expose
    var user: User? = null

    @Expose
    var users: List<User>? = null

    @Expose
    var vote: Vote? = null

    @Expose
    var votes: List<Vote>? = null
}
