package com.oxygenupdater

import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import androidx.work.WorkManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics
import com.oxygenupdater.database.DatabaseBuilders.buildLocalAppDatabase
import com.oxygenupdater.repositories.BillingRepository
import com.oxygenupdater.repositories.ServerRepository
import com.oxygenupdater.ui.faq.FaqViewModel
import com.oxygenupdater.ui.install.InstallGuideViewModel
import com.oxygenupdater.ui.news.NewsItemViewModel
import com.oxygenupdater.ui.news.NewsListViewModel
import com.oxygenupdater.ui.settings.SettingsViewModel
import com.oxygenupdater.ui.update.UpdateInformationViewModel
import com.oxygenupdater.utils.createDownloadApi
import com.oxygenupdater.utils.createServerApi
import com.oxygenupdater.viewmodels.BillingViewModel
import com.oxygenupdater.viewmodels.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

private val retrofitModule = module {
    single { createServerApi(androidContext()) }
    single { createDownloadApi() }
}

val preferencesModule = module {
    single { PreferenceManager.getDefaultSharedPreferences(androidContext()) }
}

private val repositoryModule = module {
    single { ServerRepository(get(), get()) }
    single { BillingRepository(get()) }
}

private val viewModelModule = module {
    viewModel { BillingViewModel(get(), get()) }
    viewModel { MainViewModel(get(), get(), get()) }
    viewModel { UpdateInformationViewModel(get()) }
    viewModel { InstallGuideViewModel(get()) }
    viewModel { NewsListViewModel(get()) }
    viewModel { NewsItemViewModel(get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { FaqViewModel(get()) }
}

private val databaseModule = module {
    single { buildLocalAppDatabase(androidContext()) }
}

private val notificationModule = module {
    single { NotificationManagerCompat.from(androidContext()) }
}

private val miscellaneousSingletonModule = module {
    single { AppUpdateManagerFactory.create(androidContext()) }
    single { Firebase.analytics }
    single { Firebase.crashlytics }
    single { WorkManager.getInstance(androidContext()) }
}

val allModules = listOf(
    retrofitModule,
    preferencesModule,
    repositoryModule,
    viewModelModule,
    databaseModule,
    notificationModule,
    miscellaneousSingletonModule
)
