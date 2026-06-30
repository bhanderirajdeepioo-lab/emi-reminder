package io.helsy.emireminder.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Placeholder for app-level (non-database) bindings.
// Repository classes use constructor injection (@Inject) so no manual provides needed.
@Module
@InstallIn(SingletonComponent::class)
object AppModule
