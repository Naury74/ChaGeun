package com.naury.chageun.feature.onboarding

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes

/** 온보딩에서 고를 수 있는 국내 판매량 상위 제조사와 대표 모델. 목록에 없으면 직접 입력한다. */
internal enum class VehicleMaker(@param:StringRes val nameRes: Int, @param:ArrayRes val modelsRes: Int) {
    Hyundai(R.string.maker_hyundai, R.array.models_hyundai),
    Kia(R.string.maker_kia, R.array.models_kia),
    Genesis(R.string.maker_genesis, R.array.models_genesis),
    KgMobility(R.string.maker_kgm, R.array.models_kgm),
    RenaultKorea(R.string.maker_renault, R.array.models_renault),
    Chevrolet(R.string.maker_chevrolet, R.array.models_chevrolet),
    Bmw(R.string.maker_bmw, R.array.models_bmw),
    MercedesBenz(R.string.maker_benz, R.array.models_benz),
    Tesla(R.string.maker_tesla, R.array.models_tesla),
}
