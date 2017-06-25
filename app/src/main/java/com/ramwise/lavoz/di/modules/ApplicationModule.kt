package com.ramwise.lavoz.di.modules

import android.app.Application
import dagger.Module
import dagger.Provides
import javax.inject.Singleton


@Module
class ApplicationModule(application: Application) {

    val mApplication = application

    @Provides
    @Singleton
    fun providesApplication() : Application {
        return mApplication
    }
}