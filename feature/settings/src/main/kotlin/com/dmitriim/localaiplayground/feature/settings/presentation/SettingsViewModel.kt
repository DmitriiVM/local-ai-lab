package com.dmitriim.localaiplayground.feature.settings.presentation

import androidx.lifecycle.ViewModel
import com.dmitriim.localaiplayground.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class SettingsViewModel : ViewModel()
