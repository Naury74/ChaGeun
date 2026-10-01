package com.naury.chageun.feature.ai

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.naury.chageun.core.domain.ai.AiContextFacts
import com.naury.chageun.core.domain.ai.SharedRecord
import com.naury.chageun.core.model.RuleSource
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.VehicleHealthLevel
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.core.ui.remainingText

/** Renders shared facts in the user's language; only fields present in [AiContextFacts] can appear. */
@Composable
internal fun aiPromptText(facts: AiContextFacts, question: String): String = buildList {
    add(stringResource(R.string.ai_prompt_header, formatDate(facts.asOf)))
    val vehicle = listOfNotNull(
        "${facts.maker} ${facts.model}",
        facts.modelYear?.toString(),
        facts.fuelType?.let { stringResource(it.labelRes) },
    ).joinToString(" · ")
    add(stringResource(R.string.ai_prompt_vehicle, vehicle))
    add(
        facts.mileage?.let {
            stringResource(R.string.ai_prompt_mileage, formatNumber(it.mileage.value), formatDate(it.date))
        }
            ?: stringResource(R.string.ai_prompt_mileage_unknown),
    )
    add(stringResource(R.string.ai_prompt_health, stringResource(facts.health.labelRes)))
    if (facts.maintenance.isNotEmpty()) {
        add(stringResource(R.string.ai_prompt_maintenance))
        facts.maintenance.forEach { status ->
            val detail = listOfNotNull(
                stringResource(status.state.labelRes),
                remainingText(status),
                stringResource(R.string.ai_prompt_user_rule).takeIf { status.ruleSource == RuleSource.User },
            ).joinToString(", ")
            add(stringResource(R.string.ai_prompt_item, stringResource(status.item.labelRes), detail))
        }
    }
    if (facts.missingInfo.isNotEmpty()) {
        add(
            stringResource(
                R.string.ai_prompt_missing,
                facts.missingInfo.map {
                    stringResource(it.labelRes)
                }.joinToString(", "),
            ),
        )
    }
    if (facts.recentRecords.isNotEmpty()) {
        add(stringResource(R.string.ai_prompt_records))
        facts.recentRecords.forEach { add(stringResource(R.string.ai_prompt_item, recordTitle(it), recordDetail(it))) }
    }
    add(stringResource(R.string.ai_prompt_footer))
    add("")
    add(stringResource(R.string.ai_prompt_question, question.trim()))
}.joinToString("\n")

@Composable
private fun recordTitle(record: SharedRecord): String {
    val item = record.maintenanceItem
    return when {
        item != null -> stringResource(item.labelRes)
        record.type == TimelineEventType.Fuel -> stringResource(record.type.labelRes)
        else -> record.title ?: stringResource(record.type.labelRes)
    }
}

@Composable
private fun recordDetail(record: SharedRecord): String = listOfNotNull(
    record.date?.let { formatDate(it) },
    record.mileage?.let { stringResource(R.string.ai_km, formatNumber(it.value)) },
    record.costWon?.let { stringResource(R.string.ai_won, formatNumber(it)) },
).joinToString(", ")

private val VehicleHealthLevel.labelRes: Int
    get() = when (this) {
        VehicleHealthLevel.Good -> R.string.ai_health_good
        VehicleHealthLevel.Upcoming -> R.string.ai_health_upcoming
        VehicleHealthLevel.NeedsAttention -> R.string.ai_health_attention
        VehicleHealthLevel.InsufficientData -> R.string.ai_health_insufficient
    }
