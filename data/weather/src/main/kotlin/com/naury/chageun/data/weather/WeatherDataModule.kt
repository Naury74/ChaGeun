package com.naury.chageun.data.weather

import com.naury.chageun.core.domain.weather.ApproximateLocationProvider
import com.naury.chageun.core.domain.weather.AreaNameRepository
import com.naury.chageun.core.domain.weather.KoreaRegionRepository
import com.naury.chageun.core.domain.weather.KoreaRegionUpdates
import com.naury.chageun.core.domain.weather.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface WeatherDataModule {
    @Binds
    fun bindMetNorwayRemote(remote: HttpMetNorwayRemote): MetNorwayRemote

    @Binds
    fun bindApproximateLocationProvider(provider: PlatformApproximateLocationProvider): ApproximateLocationProvider

    @Binds
    fun bindAreaNameRepository(repository: GeocoderAreaNameRepository): AreaNameRepository

    @Binds
    fun bindKoreaRegionRepository(repository: AssetKoreaRegionRepository): KoreaRegionRepository

    @Binds
    fun bindKoreaRegionUpdates(updater: KoreaRegionListUpdater): KoreaRegionUpdates

    @Binds
    fun bindKoreaRegionRemote(remote: HttpKoreaRegionRemote): KoreaRegionRemote

    @Binds
    fun bindAreaGeocoder(geocoder: PlatformAreaGeocoder): AreaGeocoder

    @Binds
    fun bindKoreaRegionUpdateScheduler(scheduler: PeriodicKoreaRegionUpdateScheduler): KoreaRegionUpdateScheduler
}

/**
 * 날씨 저장소만 따로 묶는다. 계측 테스트는 이 모듈만 가짜 날씨로 바꿔(@TestInstallIn) MET Norway에 묻지 않고,
 * 지역 목록·위치 같은 나머지 바인딩은 실제 것을 그대로 쓴다.
 */
@Module
@InstallIn(SingletonComponent::class)
interface WeatherRepositoryModule {
    @Binds
    fun bindWeatherRepository(repository: MetNorwayWeatherRepository): WeatherRepository
}
