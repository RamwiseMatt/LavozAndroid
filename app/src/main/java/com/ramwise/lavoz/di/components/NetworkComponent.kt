package com.ramwise.lavoz.di.components

import com.ramwise.lavoz.activities.HomeActivity
import com.ramwise.lavoz.activities.SplashActivity
import com.ramwise.lavoz.di.modules.ApplicationModule
import com.ramwise.lavoz.di.modules.NetworkModule
import com.ramwise.lavoz.fragments.HomeFragment
import com.ramwise.lavoz.fragments.LoginFragment
import com.ramwise.lavoz.utils.factories.ObservableFactory
import com.ramwise.lavoz.viewmodels.activities.HomeActivityViewModel
import com.ramwise.lavoz.viewmodels.activities.LoginActivityViewModel
import com.ramwise.lavoz.viewmodels.fragments.*
import dagger.Component
import javax.inject.Singleton


@Singleton
@Component(modules = arrayOf(
        ApplicationModule::class,
        NetworkModule::class
))
interface NetworkComponent {
    fun inject(viewModel: LoginActivityViewModel)
    fun inject(viewModel: HomeActivityViewModel)

    fun inject(viewModel: LoginFragmentViewModel)
    fun inject(viewModel: HomeFragmentViewModel)
    fun inject(viewModel: MotionFragmentViewModel)
    fun inject(viewModel: MotionListFragmentViewModel)
    fun inject(viewModel: MotivationsFragmentViewModel)
    fun inject(viewModel: AddMotivationFragmentViewModel)
    fun inject(viewModel: OverviewFragmentViewModel)
    fun inject(viewModel: RecommendationsFragmentViewModel)
    fun inject(viewModel: AdviceAidFragmentViewModel)

    fun inject(activity: SplashActivity)
    fun inject(activity: HomeActivity)

    fun inject(fragment: LoginFragment)
    fun inject(fragment: HomeFragment)

    fun inject(observableFactory: ObservableFactory)
}