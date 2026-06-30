package com.emireminder.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Repository classes use constructor injection (@Inject) so no manual provides needed.
@Module
@InstallIn(SingletonComponent::class)
object AppModule
