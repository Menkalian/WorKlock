package de.menkalian.worklock.database

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DataAccessModule {
    @Provides
    fun dataAccess(
        impl: DataAccessImpl
    ): DataAccess = impl
}