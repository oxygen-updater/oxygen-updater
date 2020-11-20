package com.oxygenupdater

import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import androidx.work.WorkManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.oxygenupdater.apis.DownloadApi
import com.oxygenupdater.apis.ServerApi
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.repositories.BillingRepository
import com.oxygenupdater.repositories.ServerRepository
import com.oxygenupdater.utils.DatabaseBuilders.buildLocalAppDatabase
import com.oxygenupdater.utils.DatabaseBuilders.buildLocalBillingDatabase
import com.oxygenupdater.utils.NotificationUtils
import com.oxygenupdater.utils.createDownloadClient
import com.oxygenupdater.utils.createNetworkClient
import com.oxygenupdater.utils.createOkHttpCache
import com.oxygenupdater.viewmodels.BillingViewModel
import com.oxygenupdater.viewmodels.FaqViewModel
import com.oxygenupdater.viewmodels.InstallViewModel
import com.oxygenupdater.viewmodels.MainViewModel
import com.oxygenupdater.viewmodels.NewsViewModel
import com.oxygenupdater.viewmodels.OnboardingViewModel
import com.oxygenupdater.viewmodels.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.StringQualifier
import org.koin.dsl.module
import retrofit2.Retrofit

private const val QUALIFIER_SERVER = "SERVER"
private const val QUALIFIER_DOWNLOAD = "DOWNLOAD"

private val retrofitModule = module {
    single(StringQualifier(QUALIFIER_SERVER)) { createNetworkClient(createOkHttpCache(androidContext())) }
    single(StringQualifier(QUALIFIER_DOWNLOAD)) { createDownloadClient() }
}

private val networkModule = module {
    single { get<Retrofit>(StringQualifier(QUALIFIER_SERVER)).create(ServerApi::class.java) }
    single { get<Retrofit>(StringQualifier(QUALIFIER_DOWNLOAD)).create(DownloadApi::class.java) }
}

private val preferencesModule = module {
    single { PreferenceManager.getDefaultSharedPreferences(androidContext()) }
}

private val repositoryModule = module {
    single { ServerRepository(get(), get(), get()) }
    single { BillingRepository(get(), get()) }
}

private val viewModelModule = module {
    viewModel { BillingViewModel(get(), get(), get()) }
    viewModel { OnboardingViewModel(get(), get()) }
    viewModel { MainViewModel(get()) }
    viewModel { InstallViewModel(get()) }
    viewModel { NewsViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { FaqViewModel(get()) }

}

private val databaseModule = module {
    single { buildLocalAppDatabase(androidContext()) }
    single { buildLocalBillingDatabase(androidContext()) }
}

private val notificationModule = module {
    single { NotificationManagerCompat.from(androidContext()) }
    single { NotificationUtils(androidContext()) }
}

private val miscellaneousSingletonModule = module {
    /**
     * A singleton [SystemVersionProperties] helps avoid unnecessary calls to the native `getprop` command.
     */
    single { SystemVersionProperties() }

    single { AppUpdateManagerFactory.create(androidContext()) }
    single { Firebase.analytics }
    single { Firebase.crashlytics }
    single { WorkManager.getInstance(androidContext()) }
}

val allModules = listOf(
    retrofitModule,
    networkModule,
    preferencesModule,
    repositoryModule,
    viewModelModule,
    databaseModule,
    notificationModule,
    miscellaneousSingletonModule
)
