package com.ramwise.lavoz.di.modules

import android.app.Application
import android.provider.Settings
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ramwise.lavoz.BuildConfig
import com.ramwise.lavoz.LavozConstants
import com.ramwise.lavoz.network.AuthenticationService
import com.ramwise.lavoz.network.LavozService
import com.ramwise.lavoz.network.LavozServiceSkeleton
import dagger.Module
import dagger.Provides
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import rx.schedulers.Schedulers
import java.io.IOException
import java.util.*
import javax.inject.Singleton


@Module
class NetworkModule(baseUrl: String) {

    val mBaseUrl = baseUrl

    @Provides
    @Singleton
    fun providesGson() : Gson {
        val gson = GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create()

        return gson
    }

    @Provides
    @Singleton
    fun providesInterceptor(application: Application) : Interceptor {
        val interceptor = object : Interceptor {
            @Throws(IOException::class)
            override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
                val uuid = Settings.Secure.getString(application.applicationContext.contentResolver,
                        Settings.Secure.ANDROID_ID) ?: "nouuid_${Random().nextInt(Int.MAX_VALUE)}"

                val contentLength = (chain.request()?.body()?.contentLength() ?: 0).toString()

                val newRequest = chain.request().newBuilder()
                        .addHeader("Content-Type", "application/json")
                        .addHeader("User-Agent", "Android (${android.os.Build.VERSION.SDK_INT})")
                        .addHeader("X-User-App-UUID-String", uuid)
                        .addHeader("X-User-App-Bundle-Version", "${BuildConfig.VERSION_CODE}")
                        .addHeader("Content-Length", contentLength)
                        .build()


                return chain.proceed(newRequest)
            }
        }

        return interceptor
    }

    @Provides
    @Singleton
    fun providesOkHttpBuilder(interceptor: Interceptor): OkHttpClient.Builder {
        val builder = OkHttpClient.Builder()
        builder.interceptors().add(interceptor)

        return builder
    }

    @Provides
    @Singleton
    fun providesOkHttpClient(builder: OkHttpClient.Builder) : OkHttpClient {
        val client = builder.build()

        return client
    }

    @Provides
    @Singleton
    fun providesRetrofit(gson: Gson, client: OkHttpClient) : Retrofit {
        val retrofit = Retrofit.Builder()
                .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .baseUrl(mBaseUrl)
                .build()

        return retrofit
    }

    @Provides
    @Singleton
    fun providesLavozServiceSkeleton(retrofit: Retrofit): LavozServiceSkeleton {
        val skeleton = retrofit.create(LavozServiceSkeleton::class.java)

        return skeleton
    }

    @Provides
    @Singleton
    fun providesAuthenticationService(): AuthenticationService {
        val service = AuthenticationService()

        return service
    }

    @Provides
    @Singleton
    fun providesLavozService(skeleton: LavozServiceSkeleton,
                             authService: AuthenticationService): LavozService {
        val service = LavozService(skeleton, authService)

        return service
    }
}