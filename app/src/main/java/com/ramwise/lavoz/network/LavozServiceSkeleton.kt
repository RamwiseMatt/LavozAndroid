package com.ramwise.lavoz.network

import com.ramwise.lavoz.models.*
import com.ramwise.lavoz.network.LavozError
import com.ramwise.lavoz.network.LavozResponse

import retrofit2.http.*
import okhttp3.RequestBody
import rx.Observable

interface LavozServiceSkeleton {
    @POST("users")
    fun registerUser(@Body body: Map<String, String>): Observable<LavozResponse<User, LavozError>>

    @PUT("users/complete_registration")
    fun completeUserRegistration(@HeaderMap headers: Map<String, String>,
                                 @Body body: Map<String, String>): Observable<LavozResponse<Unit, LavozError>>

    @PUT("organizations/user/political_parties")
    fun updateUserPoliticalParties(@HeaderMap headers: Map<String, String>,
                                   @Body body: RequestBody):
            Observable<LavozResponse<Unit, LavozError>>

    @GET("users/token")
    fun loginNativeUser(@HeaderMap headers: Map<String, String>):
            Observable<LavozResponse<AuthToken, LavozError>>

    @GET("users/token/fb")
    fun loginFacebookUser(@HeaderMap headers: Map<String, String>):
            Observable<LavozResponse<AuthToken, LavozError>>

    @GET("users/token/google")
    fun loginGoogleUser(@HeaderMap headers: Map<String, String>):
            Observable<LavozResponse<AuthToken, LavozError>>

    @DELETE("users/token")
    fun signOut(@HeaderMap headers: Map<String, String>):
            Observable<LavozResponse<Unit, LavozError>>

    @PUT("users/enable_push")
    fun enablePushNotifications(@HeaderMap headers: Map<String, String>):
            Observable<LavozResponse<Unit, LavozError>>

    @GET("users/{userId}")
    fun getUser(@HeaderMap headers: Map<String, String>,
                @Path("userId") userId: Int): Observable<LavozResponse<User, LavozError>>

    @PUT("users/preferences/anonymous/{boolAsString}")
    fun setUserAnonymous(@HeaderMap headers: Map<String, String>,
                         @Path("boolAsString") boolAsString: String): Observable<LavozResponse<User, LavozError>>

    @GET("overview/personal")
    fun getOverview(@HeaderMap headers: Map<String, String>): Observable<LavozResponse<Overview, LavozError>>

    @GET("overview/personal/force_refresh")
    fun getOverviewWithForceRefresh(@HeaderMap headers: Map<String, String>
                                    ): Observable<LavozResponse<Overview, LavozError>>

    @GET("motions/{motionId}")
    fun getMotion(@HeaderMap headers: Map<String, String>,
                  @Path("motionId") motionId: Int): Observable<LavozResponse<Motion, LavozError>>

    @GET("motions/{motionId}/as_moderator")
    fun getMotionAsModerator(@HeaderMap headers: Map<String, String>,
                             @Path("motionId") motionId: Int): Observable<LavozResponse<Motion, LavozError>>

    @GET("motions/{motionId}/comments/replies")
    fun getMotionWithComments(@HeaderMap headers: Map<String, String>,
                              @Path("motionId") motionId: Int): Observable<LavozResponse<Motion, LavozError>>

    @GET("motions/{motionId}/comments/replies/as_moderator")
    fun getMotionWithCommentsAsModerator(@HeaderMap headers: Map<String, String>,
                                         @Path("motionId") motionId: Int): Observable<LavozResponse<Motion, LavozError>>

    @GET("motions/now")
    fun getMotionNow(@HeaderMap headers: Map<String, String>): Observable<LavozResponse<Motion, LavozError>>

    @GET("motions/advice_aid/next")
    fun getMotionForAdviceAid(@HeaderMap headers: Map<String, String>): Observable<LavozResponse<Motion, LavozError>>

    @GET("motions/{viewType}/offset/{offset}/limit/{limit}")
    fun getMotions(@HeaderMap headers: Map<String, String>,
                   @Path("viewType") viewType: String,
                   @Path("offset") offset: Int,
                   @Path("limit") limit: Int): Observable<LavozResponse<List<Motion>, LavozError>>

    @GET("motions/{viewType}/offset/{offset}/limit/{limit}/as_moderator")
    fun getMotionsAsModerator(@HeaderMap headers: Map<String, String>,
                              @Path("viewType") viewType: String,
                              @Path("offset") offset: Int,
                              @Path("limit") limit: Int): Observable<LavozResponse<List<Motion>, LavozError>>

    @PUT("motions/{motionId}/vote")
    fun castVote(@HeaderMap headers: Map<String, String>,
                 @Path("motionId") motionId: Int,
                 @Body body: Map<String, String>): Observable<LavozResponse<Vote, LavozError>>

    @GET("cities/search/{query}")
    fun searchCities(@HeaderMap headers: Map<String, String>,
                     @Path("query") query: String): Observable<LavozResponse<List<City>, LavozError>>

    @POST("comments/motion/{motionId}")
    fun commentWrite(@HeaderMap headers: Map<String, String>,
                     @Path("motionId") motionId: Int,
                     @Body body: Map<String, String>): Observable<LavozResponse<Comment, LavozError>>

    @DELETE("comments/{commentId}")
    fun commentRemove(@HeaderMap headers: Map<String, String>,
                      @Path("commentId") commentId: Int): Observable<LavozResponse<Unit, LavozError>>

    /** Vote on a comment.
     *
     * @param commentId The ID of the comment to vote on
     * @param choice A string, either "agree" or "disagree" (without quotes).
     */
    @PUT("comments/{commentId}/{choice}")
    fun commentVote(@HeaderMap headers: Map<String, String>,
                    @Path("commentId") commentId: Int,
                    @Path("choice") choice: String): Observable<LavozResponse<CommentVote, LavozError>>

    @DELETE("comments/vote/{commentVoteId}")
    fun commentVoteRemove(@HeaderMap headers: Map<String, String>,
                          @Path("commentVoteId") commentUpvoteId: Int):
            Observable<LavozResponse<Unit, LavozError>>

    @DELETE("comments/{commentVoteId}/motion/{motionId}")
    fun commentVoteRemoveByCommentId(@HeaderMap headers: Map<String, String>,
                                     @Path("commentVoteId") commentUpvoteId: Int,
                                     @Path("motionId") motionId: Int):
            Observable<LavozResponse<Unit, LavozError>>
}