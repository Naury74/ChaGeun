package com.naury.chageun.core.ui

import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.naury.chageun.core.designsystem.component.ChoiceCard
import com.naury.chageun.core.designsystem.component.ChoicePill
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.FuelType
import java.time.Year
import java.util.Locale

/** 고를 수 있는 국내 판매량 상위 제조사와 대표 모델. 목록에 없으면 직접 입력한다. */
enum class VehicleMaker(@param:StringRes val nameRes: Int, @param:ArrayRes val modelsRes: Int) {
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

enum class VehicleInfoField { Maker, Model, ModelYear, FuelType }

/**
 * 제조사·모델·연식·연료 선택 영역. 온보딩과 차량 정보 수정이 함께 쓴다.
 *
 * 제조사와 모델은 등록할 때의 언어로 저장된다. 앱 언어가 바뀌어도 같은 칩이 선택되도록 지원하는 모든 언어의
 * 이름과 비교하고, 어느 언어와도 맞지 않을 때만 직접 입력 값으로 보여 준다.
 * 오류 문구는 화면마다 형식이 달라 [error]로 받아 각 칸 아래에 그린다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VehicleInfoFields(
    maker: String,
    model: String,
    modelYear: String,
    fuelType: FuelType?,
    onMakerChanged: (String) -> Unit,
    onModelChanged: (String) -> Unit,
    onModelYearChanged: (String) -> Unit,
    onFuelTypeSelected: (FuelType) -> Unit,
    modifier: Modifier = Modifier,
    error: @Composable (VehicleInfoField) -> Unit = {},
) {
    val makerNames = VehicleMaker.entries.associateWith { stringResource(it.nameRes) }
    val catalogs = rememberCatalogResources()
    val selectedMaker = VehicleMaker.entries.firstOrNull { entry ->
        maker == makerNames.getValue(entry) || catalogs.any { it.getString(entry.nameRes) == maker }
    }
    var isCustomMaker by rememberSaveable { mutableStateOf(maker.isNotEmpty() && selectedMaker == null) }

    // 온보딩 단계 본문과 같은 간격으로 칸을 나눈다.
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.md)) {
        FieldLabel(R.string.vehicle_field_maker)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
        ) {
            VehicleMaker.entries.forEach { entry ->
                ChoicePill(
                    label = makerNames.getValue(entry),
                    selected = entry == selectedMaker && !isCustomMaker,
                    onClick = {
                        isCustomMaker = false
                        if (entry != selectedMaker) {
                            onMakerChanged(makerNames.getValue(entry))
                            onModelChanged("")
                        }
                    },
                )
            }
            ChoicePill(
                label = stringResource(R.string.vehicle_field_other_maker),
                selected = isCustomMaker,
                onClick = {
                    if (!isCustomMaker) {
                        isCustomMaker = true
                        onMakerChanged("")
                        onModelChanged("")
                    }
                },
            )
        }
        error(VehicleInfoField.Maker)
        AnimatedVisibility(visible = isCustomMaker) {
            PlainField(maker, onMakerChanged, R.string.vehicle_field_maker)
        }

        if (selectedMaker != null || isCustomMaker) {
            ModelPicker(
                models = selectedMaker?.let { stringArrayResource(it.modelsRes).toList() }.orEmpty(),
                otherLanguageModels = selectedMaker
                    ?.let { entry -> catalogs.map { it.getStringArray(entry.modelsRes).toList() } }
                    .orEmpty(),
                model = model,
                onModelChanged = onModelChanged,
            )
            error(VehicleInfoField.Model)
        }

        YearPicker(modelYear, onModelYearChanged)
        error(VehicleInfoField.ModelYear)

        FieldLabel(R.string.vehicle_field_fuel)
        FuelPicker(fuelType, onFuelTypeSelected)
        error(VehicleInfoField.FuelType)
    }
}

@Composable
private fun FuelPicker(selected: FuelType?, onSelected: (FuelType) -> Unit) {
    // 마지막 줄이 한 칸만 남아도 다른 카드와 너비를 맞추도록 빈 칸을 채우고, 한 줄의 카드 높이를 같게 둔다.
    Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        FuelType.entries.chunked(FUEL_COLUMNS).forEach { row ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
            ) {
                row.forEach { fuel ->
                    ChoiceCard(
                        label = stringResource(fuel.labelRes),
                        icon = fuel.icon,
                        selected = selected == fuel,
                        onClick = { onSelected(fuel) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                repeat(FUEL_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModelPicker(
    models: List<String>,
    otherLanguageModels: List<List<String>>,
    model: String,
    onModelChanged: (String) -> Unit,
) {
    // 모델 목록은 언어마다 같은 순서로 두므로 위치로 같은 모델을 찾는다.
    val selectedIndex = (listOf(models) + otherLanguageModels)
        .firstNotNullOfOrNull { names -> names.indexOf(model).takeIf { it >= 0 } }
    var isCustom by rememberSaveable {
        mutableStateOf(models.isEmpty() || (model.isNotEmpty() && selectedIndex == null))
    }
    FieldLabel(R.string.vehicle_field_model)
    if (models.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
        ) {
            models.forEachIndexed { index, name ->
                ChoicePill(
                    label = name,
                    selected = !isCustom && index == selectedIndex,
                    onClick = {
                        isCustom = false
                        onModelChanged(name)
                    },
                )
            }
            ChoicePill(
                label = stringResource(R.string.vehicle_field_custom),
                selected = isCustom,
                onClick = {
                    isCustom = true
                    onModelChanged("")
                },
            )
        }
    }
    AnimatedVisibility(visible = isCustom || models.isEmpty()) {
        PlainField(model, onModelChanged, R.string.vehicle_field_model)
    }
}

@Composable
private fun YearPicker(modelYear: String, onYearChanged: (String) -> Unit) {
    val newest = remember { Year.now().value + 1 }
    val years = remember(newest) { (newest downTo newest - RECENT_YEARS).map(Int::toString) }
    var isCustom by rememberSaveable { mutableStateOf(modelYear.isNotEmpty() && modelYear !in years) }
    FieldLabel(R.string.vehicle_field_year)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        items(years, key = { it }) { year ->
            ChoicePill(
                label = year,
                selected = !isCustom && modelYear == year,
                onClick = {
                    isCustom = false
                    onYearChanged(year)
                },
            )
        }
        item(key = "older") {
            ChoicePill(
                label = stringResource(R.string.vehicle_field_older),
                selected = isCustom,
                onClick = {
                    isCustom = true
                    onYearChanged("")
                },
            )
        }
    }
    AnimatedVisibility(visible = isCustom) {
        PlainField(modelYear, onYearChanged, R.string.vehicle_field_year, KeyboardType.Number)
    }
}

/** 지원하는 언어별 리소스. 저장된 이름이 다른 언어여도 같은 제조사·모델로 알아보는 데 쓴다. */
@Composable
private fun rememberCatalogResources(): List<Resources> {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(context, configuration) {
        CATALOG_LOCALES.map { locale ->
            context.createConfigurationContext(Configuration(configuration).apply { setLocale(locale) }).resources
        }
    }
}

@Composable
private fun FieldLabel(@StringRes titleRes: Int) {
    Text(
        stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = ChageunTheme.spacing.xs),
    )
}

@Composable
private fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )
}

private val CATALOG_LOCALES = listOf(Locale.ENGLISH, Locale.KOREAN)
private const val FUEL_COLUMNS = 3
private const val RECENT_YEARS = 15
