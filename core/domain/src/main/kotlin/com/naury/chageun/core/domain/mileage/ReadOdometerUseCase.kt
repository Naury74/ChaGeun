package com.naury.chageun.core.domain.mileage

import com.naury.chageun.core.model.MileageReading
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class ReadOdometerUseCase @Inject constructor(private val reader: DashboardTextReader, private val clock: Clock) {
    /** @return 인식기를 쓸 수 없으면 null. 글자는 읽었지만 맞는 숫자가 없으면 후보가 빈 결과. */
    suspend operator fun invoke(imageUri: String, previous: MileageReading?): OdometerCandidates? {
        val lines = reader.read(imageUri) ?: return null
        return OdometerParser.parse(lines, previous, LocalDate.now(clock))
    }
}
