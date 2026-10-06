package com.naury.chageun.core.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VehicleBodyTypeTest {

    @Test
    fun recognizesKoreanAndEnglishNames_ignoringSpacesAndCase() {
        assertThat(vehicleBodyTypeOf("스포티지")).isEqualTo(VehicleBodyType.Suv)
        assertThat(vehicleBodyTypeOf("sportage")).isEqualTo(VehicleBodyType.Suv)
        assertThat(vehicleBodyTypeOf("아이오닉5")).isEqualTo(VehicleBodyType.Suv)
        assertThat(vehicleBodyTypeOf("Grand Koleos")).isEqualTo(VehicleBodyType.Suv)
        assertThat(vehicleBodyTypeOf("카니발")).isEqualTo(VehicleBodyType.Van)
        assertThat(vehicleBodyTypeOf("Porter")).isEqualTo(VehicleBodyType.Truck)
        assertThat(vehicleBodyTypeOf("캐스퍼")).isEqualTo(VehicleBodyType.Hatchback)
    }

    @Test
    fun unknownModel_fallsBackToSedan() {
        assertThat(vehicleBodyTypeOf("My custom car")).isEqualTo(VehicleBodyType.Sedan)
    }
}
