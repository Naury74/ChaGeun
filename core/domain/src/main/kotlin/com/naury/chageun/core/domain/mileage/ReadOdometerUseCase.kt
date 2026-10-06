package com.naury.chageun.core.domain.mileage

import com.naury.chageun.core.model.MileageReading
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class ReadOdometerUseCase @Inject constructor(private val reader: DashboardTextReader, private val clock: Clock) {
    /** 글자는 읽었지만 맞는 숫자가 없으면 후보가 빈 [OdometerReadResult.Read]다. */
    suspend operator fun invoke(
        imageUri: String,
        previous: MileageReading?,
        onDownloadingModel: (Float?) -> Unit = {},
    ): OdometerReadResult = when (val result = reader.read(imageUri, onDownloadingModel)) {
        is TextReadResult.Read -> OdometerReadResult.Read(
            OdometerParser.parse(result.lines, previous, LocalDate.now(clock)),
        )
        TextReadResult.ModelUnavailable -> OdometerReadResult.ModelUnavailable
        TextReadResult.Unavailable -> OdometerReadResult.Unavailable
    }
}

sealed interface OdometerReadResult {
    data class Read(val candidates: OdometerCandidates) : OdometerReadResult

    data object ModelUnavailable : OdometerReadResult

    data object Unavailable : OdometerReadResult
}
