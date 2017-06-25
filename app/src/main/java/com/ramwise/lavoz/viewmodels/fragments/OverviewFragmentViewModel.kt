package com.ramwise.lavoz.viewmodels.fragments

import android.support.v4.content.ContextCompat
import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.LavozConstants
import com.ramwise.lavoz.utils.factories.LavozResponseFactory
import com.ramwise.lavoz.utils.factories.ObservableFactory

import com.ramwise.lavoz.network.AuthenticationService
import com.ramwise.lavoz.network.LavozService

import javax.inject.Inject
import com.ramwise.lavoz.R
import com.ramwise.lavoz.models.*
import com.ramwise.lavoz.models.constants.*
import com.ramwise.lavoz.network.LavozError
import com.ramwise.lavoz.network.LavozResponse
import com.ramwise.lavoz.utils.sharing.ShareMotion
import com.ramwise.lavoz.utils.*
import com.ramwise.lavoz.utils.jodatime.differenceToString
import com.ramwise.lavoz.utils.rx.*
import com.ramwise.lavoz.viewmodels.BaseViewModel

import org.joda.time.DateTime
import org.joda.time.Seconds
import rx.Observable
import rx.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit


class OverviewFragmentViewModel(
        asAdviceAid: Boolean,
        clickRecommendations: Observable<Unit>,
        clickBanner: Observable<Unit>,
        clickPartyPointsInfo: Observable<Unit>,
        clickMajorityButton: Observable<Unit>,
        clickMinorityButton: Observable<Unit>,
        clickMotivatedButton: Observable<Unit>,
        clickPreferenceDisplayNameSwitch: Observable<Boolean>): BaseViewModel() {

    @Inject lateinit var lavozService: LavozService
    @Inject lateinit var authService: AuthenticationService

    /** Emits a signal whenever the user should navigate to the recommendations page.
     * The value emitted is the array of organizations to be displayed.
     */
    val navigateToRecommendations: Observable<ArrayList<Organization>>

    /** Emits a signal whenever the user should navigate to the motionlist page.
     * The value emitted is a MotionListViewType constant.
     */
    val navigateToMotionList: Observable<Int>

    /** Emits a signal whenever the native browser should be opened. The emitted value is the URL
     * string.
     */
    val navigateToBrowser: Observable<String>

    /** Emits a signal whenever the explanation dialog for party points should be shown.
     * The value emitted is a Pair of strings that represent the title and message for the dialog.
     */
    val showPartyPointsInfoDialog: Observable<Pair<String, String>>

    /** Emits a boolean that indicates whether the Activity/Fragment should show/hide its loading
     * spinner in the overlay in x seconds.
     * Any false signal should cancel a timer controller by the Activity/Fragment, any true should
     * activate such a timer.
     */
    val prepareLayoutOverlayHUDLoader: Observable<Boolean>

    val bannerImageURL: Observable<String>

    /** The aspect ratio as a string that is compatible with a ConstraintSet */
    val bannerImageAspectRatio: Observable<String>

    val bannerMessageText: Observable<String>

    /** Emits all DeviceMessages that should be displayed as alerts. Each value in the array
     * represents a single alert, represented as DeviceAlert data objects.
     */
    val deviceMessageAlerts: Observable<List<DeviceAlert>>

    val partyPointsOne: Observable<String>
    val partyNameOne: Observable<String>
    val partyIconURLOne: Observable<String>

    val partyPointsTwo: Observable<String>
    val partyNameTwo: Observable<String>
    val partyIconURLTwo: Observable<String>

    val partyPointsThree: Observable<String>
    val partyNameThree: Observable<String>
    val partyIconURLThree: Observable<String>

    val votesReceivedCountText: Observable<String>
    val majorityCountText: Observable<String>
    val minorityCountText: Observable<String>
    val motivatedCountText: Observable<String>

    /** The emitted value is the boolean that the "display username" switch should be set to */
    val userDisplayNameSwitchValue: Observable<Boolean>

    /** Emits an error message when trying to change a user preference failed */
    val preferenceChangeFailed: Observable<String>

    private val responseFactory = LavozResponseFactory()

    /** Emits a signal when the Overview page should forcibly prompt the "rate us now" alert */
    val forceShowRatePrompt: Observable<Unit>

    init {
        LavozApplication.networkComponent.inject(this)

        val activityIndicator = RxActivityIndicator()

        prepareLayoutOverlayHUDLoader = activityIndicator.asObservable()

        // MARK: - Preference related

        val userDisplayNamePreferenceChangeToAPI = clickPreferenceDisplayNameSwitch
                .flatMap {
                    lavozService
                            .setUserAnonymous(!it)
                            .subscribeOn(Schedulers.io())
                            .trackActivity(activityIndicator)
                            .onErrorReturn { responseFactory.createError<User>(it) }
                }
                .share()

        val userDisplayNamePreferenceChangeFailed = userDisplayNamePreferenceChangeToAPI
                .map { it.err }
                .unwrap()

        val userDisplayNamePreferenceChangeSuccess = userDisplayNamePreferenceChangeToAPI
                .map { it.user }
                .unwrap()

        preferenceChangeFailed = userDisplayNamePreferenceChangeFailed
                .map { resources.getString(R.string.error_not_update_preference) }

        // MARK: - Main data related

        val overviewFromAPI = Observable
                .merge(
                        ObservableFactory().engagedInterval(engaged),
                        userDisplayNamePreferenceChangeSuccess
                )
                .flatMap {
                    lavozService
                            .getOverview()
                            .subscribeOn(Schedulers.io())
                            .trackActivity(activityIndicator)
                            .onErrorReturn { responseFactory.createError<Overview>(it) }
                }
                .shareReplay(1)
                .retain(retainBag)

        val overview = overviewFromAPI
                .map { it.overview }
                .unwrap()

        userDisplayNameSwitchValue = Observable
                .merge(
                        userDisplayNamePreferenceChangeToAPI
                            .withLatestFrom(clickPreferenceDisplayNameSwitch) {
                                response: LavozResponse<User, LavozError>, desiredValue: Boolean ->
                                if (response.err == null) desiredValue else !desiredValue
                            },
                        overview.map { !it.user.anonymous }
                )

        // MARK: - Device message related

        val deviceMessages = overview.map { it.messages }.unwrap()

        val deviceMessageBanners = deviceMessages
                .map {
                    it.filter {
                        it.type == DeviceMessageType.BANNER || it.type == DeviceMessageType.INLINE
                    }
                }

        deviceMessageAlerts = deviceMessages
                .map { it.filter { it.type == DeviceMessageType.ALERT } }
                .map { it
                        .filter { it.cancelButtonText != null && it.okButtonText != null}
                        .filter {
                            val sharedKey = "deviceMessageAlertId" + it.id.toString()
                            if (!LavozApplication.shared.getBoolean(sharedKey, false)) {
                                LavozApplication.shared
                                        .edit()
                                        .putBoolean(sharedKey, true)
                                        .apply()
                                true
                            } else {
                                false
                            }
                        }
                        .map {
                            DeviceAlert(it.title, it.text, it.cancelButtonText!!,
                                    it.okButtonText!!, it.externalURLString)
                        }
                }

        val deviceMessageFirstBanner = deviceMessageBanners
                .map { if (it.isNotEmpty()) it.first() else null }.unwrap()

        bannerImageURL = deviceMessageFirstBanner
                .map { it.file?.urlByImageSizeClass(ImageSizeClass.LARGE )}
                .unwrap()

        bannerImageAspectRatio = deviceMessageFirstBanner
                .map { "H," + it.file?.aspectRatio.toString() + ":" + "1" }
                .unwrap()

        bannerMessageText = deviceMessageFirstBanner
                .map { it.text }

        navigateToBrowser = clickBanner
                .withLatestFrom(deviceMessageFirstBanner) { ignore: Unit, dm: DeviceMessage ->
            dm.externalURLString
        }

        // MARK: - Recommendation points related

        val recommendations = overview.map { it.politicalScores }.unwrap()

        val recommendationOne = recommendations.map { if (it.size > 0) it[0] else null}.unwrap()
        val recommendationTwo = recommendations.map { if (it.size > 1) it[1] else null}.unwrap()
        val recommendationThree = recommendations.map { if (it.size > 2) it[2] else null}.unwrap()

        partyNameOne = recommendationOne.map { it.name }.unwrap()
        partyPointsOne = recommendationOne
                .map {
                    it.score?.points.toString() + " " + resources.getString(R.string.nocap_points)
                }.unwrap()
        partyIconURLOne = recommendationOne
                .map { it.icon?.urlByImageSizeClass(ImageSizeClass.SMALL) }
                .unwrap()

        partyNameTwo = recommendationTwo.map { it.name }.unwrap()
        partyPointsTwo = recommendationTwo
                .map {
                    it.score?.points.toString() + " " + resources.getString(R.string.nocap_points)
                }.unwrap()
        partyIconURLTwo = recommendationTwo
                .map { it.icon?.urlByImageSizeClass(ImageSizeClass.SMALL) }
                .unwrap()

        partyNameThree = recommendationThree.map { it.name }.unwrap()
        partyPointsThree = recommendationThree
                .map {
                    it.score?.points.toString() + " " + resources.getString(R.string.nocap_points)
                }.unwrap()
        partyIconURLThree = recommendationThree
                .map { it.icon?.urlByImageSizeClass(ImageSizeClass.SMALL) }
                .unwrap()

        navigateToRecommendations = clickRecommendations
                .withLatestFrom(recommendations, { click: (Unit),
                                                   recommendations: ArrayList<Organization> ->
                    recommendations
                })

        showPartyPointsInfoDialog = clickPartyPointsInfo.map {
            Pair(
                    resources.getString(R.string.cap_party_points_questionmark),
                    resources.getString(R.string.message_party_points_info)
            )
        }

        // MARK: - Activity related

        votesReceivedCountText = overview.map { it.countReceivedVotes.toString() }
        majorityCountText = overview.map { it.countMajority.toString() }
        minorityCountText = overview.map { it.countMinority.toString() }
        motivatedCountText = overview.map { it.countMotivated.toString() }

        navigateToMotionList = Observable.merge(
                clickMajorityButton.map { MotionListViewType.MAJORITY },
                clickMinorityButton.map { MotionListViewType.MINORITY },
                clickMotivatedButton.map { MotionListViewType.MOTIVATED }
        )

        // MARK: - Other

        forceShowRatePrompt = Observable
                .just(asAdviceAid)
                .filter { it }
                .map { (Unit) }
                .delay(8, TimeUnit.SECONDS)
    }

    /** A simple data class that represents all that should be displayed in a custom alert coming
     * from the Overview object.
     *
     * While this is closely related to the [DeviceMessage] model, the purpose of this data class
     * is to provide an abstraction that respects the MVVM pattern. Unlike [DeviceMessage],
     * we can expose DeviceAlert to the Fragment.
     */
    data class DeviceAlert(val title: String?, val text: String, val cancelText: String,
                           val okText: String, val url: String?)
}