package com.ramwise.lavoz.network

import android.util.Base64
import com.google.gson.GsonBuilder
import com.ramwise.lavoz.network.LavozResponse
import rx.Observable
import com.ramwise.lavoz.models.*
import com.ramwise.lavoz.models.constants.CommentVoteOption
import com.ramwise.lavoz.network.LavozError
import com.ramwise.lavoz.models.constants.Gender
import com.ramwise.lavoz.models.constants.MotionListViewType
import com.ramwise.lavoz.models.constants.VoteOption
import okhttp3.MediaType
import okhttp3.RequestBody
import java.util.*


class LavozService(skeleton: LavozServiceSkeleton,
                   authService: AuthenticationService) {

    val skeleton = skeleton
    val authService = authService

    /** A HashMap that serves as a HeaderMap for the often-used X-Auth-Token. Unlike some headers,
     * this header is not required for every API request and therefore is not included in the
     * OKHttp Interceptor builder.
     */
    private val authTokenHeaderMap = HashMap<String, String>()

    init {
        authService.authTokenAsObservable().subscribe { it: AuthToken? ->
            if (it != null) authTokenHeaderMap.put("X-Auth-Token", it.textual)
            else            authTokenHeaderMap.remove("X-Auth-Token")
        }
    }

    fun registerUser(email: String, name: String, password: String): Observable<LavozResponse<User, LavozError>> {
        val requestBody = HashMap<String, String>()
        requestBody.put("email", email)
        requestBody.put("name", name)
        requestBody.put("password", password)

        return skeleton.registerUser(requestBody)
    }

    /* @param gender A Gender constant */
    fun setUserGender(gender: Int): Observable<LavozResponse<Unit, LavozError>> {
        val requestBody = HashMap<String, String>()
        requestBody.put("gender", Gender.toString(gender))

        return skeleton.completeUserRegistration(authTokenHeaderMap, requestBody)
    }

    fun setUserCity(cityId: Int): Observable<LavozResponse<Unit, LavozError>> {
        val requestBody = HashMap<String, String>()
        requestBody.put("city_id", cityId.toString())

        return skeleton.completeUserRegistration(authTokenHeaderMap, requestBody)
    }

    fun setUserAgeRange(ageRangeMin: Int, ageRangeMax: Int): Observable<LavozResponse<Unit, LavozError>> {
        val requestBody = HashMap<String, String>()
        requestBody.put("age_range_min", ageRangeMin.toString())
        requestBody.put("age_range_max", ageRangeMax.toString())

        return skeleton.completeUserRegistration(authTokenHeaderMap, requestBody)
    }

    /** Changes the political party preferences of the user. These political parties are sometimes
     * also refered to as organizations (they are a subset of all organizations in the Lavoz db)
     *
     * @param parties A list of organization IDs that the user prefers. These organizations should
     *                all be political parties.
     *
     */
    fun updateUserPoliticalParties(parties: List<Int>): Observable<LavozResponse<Unit, LavozError>> {
        // Due to the more complicated nature of the data, the only way to PUT the required data
        // via Retrofit is to post raw JSON (which is serialized via JSON).
        val data = HashMap<String, List<Map<String, String>>>()
        data.put("user_political_parties", parties.map {
            hashMapOf("organization_id" to it.toString())
        })

        val json = GsonBuilder().create().toJson(data)
        val requestBody = RequestBody.create(MediaType.parse("application/json"), json)

        return skeleton.updateUserPoliticalParties(authTokenHeaderMap, requestBody)
    }

    fun loginNativeUser(email: String, password: String): Observable<LavozResponse<AuthToken, LavozError>> {
        val encoded = Base64.encode((email + ":" + password).toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        val headerMap: HashMap<String, String> = HashMap()
        headerMap.put("Authorization", "Basic ${encoded}}")

        return skeleton.loginNativeUser(headerMap)
    }

    fun loginFacebookUser(accessToken: String): Observable<LavozResponse<AuthToken, LavozError>> {
        val headerMap: HashMap<String, String> = HashMap()
        headerMap.put("X-FB-Access-Token", accessToken)

        return skeleton.loginFacebookUser(headerMap)
    }

    fun loginGoogleUser(authCode: String): Observable<LavozResponse<AuthToken, LavozError>> {
        val headerMap: HashMap<String, String> = HashMap()
        headerMap.put("X-Google-Auth-Code", authCode)

        return skeleton.loginGoogleUser(headerMap)
    }

    fun signOut(): Observable<LavozResponse<Unit, LavozError>> {
        return skeleton.signOut(authTokenHeaderMap)
    }

    fun enablePushNotifications(appPushToken: String): Observable<LavozResponse<Unit, LavozError>> {
        val headerMap: HashMap<String, String> = HashMap()
        headerMap.put("X-User-App-Push-Token", appPushToken)
        headerMap.putAll(authTokenHeaderMap)

        return skeleton.enablePushNotifications(headerMap)
    }

    fun getUser(userId: Int): Observable<LavozResponse<User, LavozError>> {
        return skeleton.getUser(authTokenHeaderMap, userId)
    }

    fun setUserAnonymous(value: Boolean): Observable<LavozResponse<User, LavozError>> {
        return skeleton.setUserAnonymous(authTokenHeaderMap, if (value) "true" else "false")
    }

    fun getOverview(forceRefresh: Boolean = false): Observable<LavozResponse<Overview, LavozError>> {
        if (forceRefresh) return skeleton.getOverviewWithForceRefresh(authTokenHeaderMap)
        else              return skeleton.getOverview(authTokenHeaderMap)
    }

    fun getMotion(motionId: Int, withComments: Boolean = false, asModerator: Boolean = false):
            Observable<LavozResponse<Motion, LavozError>> {
        val headerMap = authTokenHeaderMap

        if (withComments) {
            if (asModerator) return skeleton.getMotionWithCommentsAsModerator(headerMap, motionId)
            else             return skeleton.getMotionWithComments(headerMap, motionId)
        } else {
            if (asModerator) return skeleton.getMotionAsModerator(headerMap, motionId)
            else             return skeleton.getMotion(headerMap, motionId)
        }
    }

    fun getMotionNow(): Observable<LavozResponse<Motion, LavozError>> {
        return skeleton.getMotionNow(authTokenHeaderMap)
    }

    fun getMotionForAdviceAid(): Observable<LavozResponse<Motion, LavozError>> {
        return skeleton.getMotionForAdviceAid(authTokenHeaderMap)
    }

    /**
     * @param viewType A MotionListViewType constant.
     *
     * @param offset The offset of the API request. Defaults to 0.
     *
     * @param limit The number of motions to return in a single API request. Defaults to 7.
     *
     * @param asModerator Indicates whether the query should be perfoemd as moderator or not.
     *                    Should only happen for admin accounts.
     */
    fun getMotions(viewType: Int, offset: Int = 0, limit: Int = 7, asModerator: Boolean = false):
            Observable<LavozResponse<List<Motion>, LavozError>> {
        val headerMap = authTokenHeaderMap

        var actualOffset = offset
        if (viewType == MotionListViewType.RECENT && !asModerator) {
            // Skip the first item since the home page already shows that one.
            actualOffset += 1
        }

        if (asModerator) return skeleton.getMotionsAsModerator(headerMap,
                MotionListViewType.toString(viewType), actualOffset, limit)
        else return skeleton.getMotions(headerMap,
                MotionListViewType.toString(viewType), actualOffset, limit)
    }

    /**
     * @param motionId The id of the motion to vote on.
     *
     * @param voteOption A VoteOption constant.
     */
    fun castVote(motionId: Int, voteOption: Int): Observable<LavozResponse<Vote, LavozError>> {
        val requestBody = HashMap<String, String>()
        requestBody.put("ballot", VoteOption.toString(voteOption))

        return skeleton.castVote(authTokenHeaderMap, motionId, requestBody)
    }

    fun searchCities(query: String): Observable<LavozResponse<List<City>, LavozError>> {
        return skeleton.searchCities(authTokenHeaderMap, query)
    }

    /**
     * @param motionId The id of the motion to vote on.
     *
     * @param body The body of text written by the user.
     *
     * @param voteOption A VoteOption constant of the ROOT comment of this comment tree. The whold
     *                   comment tree therefore ends up with the same VoteOption set.
     */
    fun commentWrite(motionId: Int, body: String, voteOption: Int, replyToCommentId: Int?):
            Observable<LavozResponse<Comment, LavozError>> {
        val requestBody: HashMap<String, String> = HashMap()
        requestBody.put("body", body)
        requestBody.put("ballot_type", VoteOption.toString(voteOption))

        if (replyToCommentId != null) {
            requestBody.put("reply_to", replyToCommentId.toString())
        }

        return skeleton.commentWrite(authTokenHeaderMap, motionId, requestBody)
    }

    fun commentRemove(commentId: Int): Observable<LavozResponse<Unit, LavozError>> {
        return skeleton.commentRemove(authTokenHeaderMap, commentId)
    }

    /**
     * @param commentId The id of the comment to vote on.
     *
     * @param commentVoteOption A CommentVoteOption constant.
     */
    fun commentVote(commentId: Int, commentVoteOption: Int): Observable<LavozResponse<CommentVote, LavozError>> {
        return skeleton.commentVote(authTokenHeaderMap, commentId, CommentVoteOption.toString(commentVoteOption))
    }

    fun commentVoteRemove(commentVoteId: Int): Observable<LavozResponse<Unit, LavozError>> {
        return skeleton.commentVoteRemove(authTokenHeaderMap, commentVoteId)
    }

    fun commentVoteRemoveByCommentId(commentId: Int, motionId: Int): Observable<LavozResponse<Unit, LavozError>> {
        return skeleton.commentVoteRemoveByCommentId(authTokenHeaderMap, commentId, motionId)
    }
}