package com.naury.chageun.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.PropaneTank
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TireRepair
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.ToneColors
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.MaintenanceCategory
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem

val MaintenanceItem.icon: ImageVector
    get() = when (this) {
        MaintenanceItem.EngineOil -> Icons.Filled.OilBarrel
        MaintenanceItem.OilFilter -> Icons.Filled.FilterAlt
        MaintenanceItem.AirFilter -> Icons.Filled.Air
        MaintenanceItem.SparkPlug -> Icons.Filled.FlashOn
        MaintenanceItem.CabinFilter -> Icons.Filled.AcUnit
        MaintenanceItem.BrakePad -> Icons.Filled.RadioButtonChecked
        MaintenanceItem.BrakeFluid -> Icons.Filled.Opacity
        MaintenanceItem.Coolant -> Icons.Filled.DeviceThermostat
        MaintenanceItem.TransmissionOil -> Icons.Filled.Settings
        MaintenanceItem.Battery -> Icons.Filled.BatteryChargingFull
        MaintenanceItem.Tire -> Icons.Filled.TireRepair
        MaintenanceItem.Wiper -> Icons.Filled.Umbrella
    }

val FuelType.icon: ImageVector
    get() = when (this) {
        FuelType.Gasoline -> Icons.Filled.LocalGasStation
        FuelType.Diesel -> Icons.Filled.OilBarrel
        FuelType.Lpg -> Icons.Filled.PropaneTank
        FuelType.Hybrid -> Icons.Filled.Eco
        FuelType.PlugInHybrid -> Icons.Filled.EvStation
        FuelType.Electric -> Icons.Filled.ElectricBolt
        FuelType.Hydrogen -> Icons.Filled.WaterDrop
    }

val TimelineEventType.icon: ImageVector
    get() = when (this) {
        TimelineEventType.Maintenance -> Icons.Filled.Build
        TimelineEventType.Fuel -> Icons.Filled.LocalGasStation
        TimelineEventType.Inspection -> Icons.Filled.FactCheck
        TimelineEventType.Repair -> Icons.Filled.Build
        TimelineEventType.Note -> Icons.Filled.NoteAlt
    }

/** 분류별 색. 같은 분류의 항목은 같은 색이라 목록에서 한눈에 묶여 보인다. */
@Composable
fun MaintenanceCategory.tone(): ToneColors {
    val colors = ChageunTheme.colors
    return when (this) {
        MaintenanceCategory.Engine -> colors.upcoming
        MaintenanceCategory.Brake -> colors.critical
        MaintenanceCategory.Electrical -> colors.good
        MaintenanceCategory.Drivetrain -> colors.ai
        MaintenanceCategory.Chassis, MaintenanceCategory.Climate, MaintenanceCategory.Visibility -> colors.unknown
    }
}

/** 항목 이름 옆에 두는 장식 아이콘. 이름이 함께 읽히므로 TalkBack 설명은 두지 않는다. */
@Composable
fun ItemIconBadge(icon: ImageVector, tone: ToneColors, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .background(tone.container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tone.content, modifier = Modifier.size(size * ICON_RATIO))
    }
}

@Composable
fun MaintenanceItemIcon(item: MaintenanceItem, modifier: Modifier = Modifier, size: Dp = 44.dp) =
    ItemIconBadge(item.icon, item.category.tone(), modifier, size)

/** 정비 기록은 항목 아이콘을, 나머지는 기록 종류 아이콘을 쓴다. 주유는 정비와 구분되도록 색을 따로 둔다. */
@Composable
fun TimelineItemIcon(record: TimelineItem, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val item = record.maintenanceItem
    if (item != null) {
        MaintenanceItemIcon(item, modifier, size)
    } else {
        ItemIconBadge(record.ref.type.icon, record.ref.type.tone(), modifier, size)
    }
}

@Composable
fun TimelineEventType.tone(): ToneColors {
    val colors = ChageunTheme.colors
    return when (this) {
        TimelineEventType.Fuel -> colors.good
        TimelineEventType.Inspection -> colors.ai
        TimelineEventType.Repair -> colors.critical
        TimelineEventType.Maintenance, TimelineEventType.Note -> colors.unknown
    }
}

private const val ICON_RATIO = 0.5f
