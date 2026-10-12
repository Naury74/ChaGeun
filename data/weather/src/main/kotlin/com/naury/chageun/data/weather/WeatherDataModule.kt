package com.naury.chageun.data.weather

import com.naury.chageun.core.domain.weather.ApproximateLocationProvider
import com.naury.chageun.core.domain.weather.AreaNameRepository
import com.naury.chageun.core.domain.weather.KoreaRegionRepository
import com.naury.chageun.core.domain.weather.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface WeatherDataModule {
    @Binds
    fun bindWeatherRepository(repository: MetNorwayWeatherRepository): WeatherRepository

    @Binds
    fun bindMetNorwayRemote(remote: HttpMetNorwayRemote): MetNorwayRemote

    @Binds
    fun bindApproximateLocationProvider(provider: PlatformApproximateLocationProvider): ApproximateLocationProvider

    @Binds
    fun bindAreaNameRepository(repository: GeocoderAreaNameRepository): AreaNameRepository

    @Binds
    fun bindKoreaRegionRepository(repository: AssetKoreaRegionRepository): KoreaRegionRepository

    @Binds
    fun bindAreaGeocoder(geocoder: PlatformAreaGeocoder): AreaGeocoder
}
