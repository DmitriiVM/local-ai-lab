package com.dmitriim.localaiplayground.core.di

import dev.zacsweers.metro.Qualifier

abstract class AppScope private constructor()

@Qualifier
annotation class ApplicationCoroutineScope
