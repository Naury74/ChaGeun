package com.naury.chageun.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.component.PressStyle
import com.naury.chageun.core.designsystem.component.pressable
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.weather.KoreaRegionCatalog
import com.naury.chageun.core.model.KoreaArea
import com.naury.kinetica.ui.KIconButton
import com.naury.kinetica.ui.KLoadingIndicator
import com.naury.kinetica.ui.KTone
import com.naury.kinetica.ui.KVariant

/**
 * 시·도 → 시·군·구 → 읍·면·동으로 내려가거나 이름으로 찾아 날씨 볼 지역을 고르는 창.
 * 폰에서는 아래 시트, 넓은 창에서는 가운데 카드다(목록은 [DialogFrame] 안에서 남은 높이만 쓰고 스크롤한다).
 *
 * 뒤로 가기는 검색어 지우기 → 한 단계 위로 → 창 닫기 순서다. 지역 목록은 창을 열 때 [loadRegions]로 처음 읽는다.
 */
@Composable
internal fun RegionPickerDialog(
    loadRegions: suspend () -> KoreaRegionCatalog,
    recentCodes: List<String>,
    message: String?,
    onPicked: (KoreaArea) -> Unit,
    onDismiss: () -> Unit,
) {
    val catalog by produceState<KoreaRegionCatalog?>(null) { value = loadRegions() }
    var query by rememberSaveable { mutableStateOf("") }
    var savedDrill by rememberSaveable { mutableStateOf("") }
    val drill = RegionDrill.decode(savedDrill)
    val isSearching = query.isNotBlank()
    val dialogTitle = stringResource(R.string.weather_location_regions_title)
    DialogFrame(
        paneTitle = dialogTitle,
        onDismiss = onDismiss,
        dismissible = true,
        horizontalPadding = ChageunTheme.spacing.lg,
    ) { close ->
        // 창 안에서 등록해야 시스템 Dialog의 뒤로 가기보다 먼저 받는다.
        BackHandler(enabled = query.isNotEmpty() || !drill.isTop) {
            if (query.isNotEmpty()) query = "" else savedDrill = drill.back().encode()
        }
        RegionPickerHeader(
            title = catalog?.let { drill.current(it)?.path }.takeUnless { isSearching } ?: dialogTitle,
            onBack = { savedDrill = drill.back().encode() }.takeUnless { drill.isTop || isSearching },
        )
        message?.let { RegionPickerMessage(it) }
        Spacer(Modifier.height(ChageunTheme.spacing.md))
        RegionSearchField(query, onQueryChange = { query = it })
        Spacer(Modifier.height(ChageunTheme.spacing.sm))
        RegionPickerBody(
            catalog = catalog,
            drill = drill,
            query = query,
            recentCodes = recentCodes,
            onOpen = { area ->
                query = ""
                savedDrill = RegionDrill.at(area).encode()
            },
            onPick = onPicked,
            modifier = Modifier
                .weight(1f)
                .heightIn(max = LIST_MAX_HEIGHT),
        )
        Spacer(Modifier.height(ChageunTheme.spacing.md))
        DialogButton(
            DialogAction(
                stringResource(R.string.weather_location_cancel),
                DialogActionStyle.Secondary,
                onClick = close,
            ),
            Modifier.fillMaxWidth(),
        )
    }
}

/** 제목과 뒤로 가기. 제목은 지금 보는 단계의 이름이라, 바뀌면 TalkBack이 새 이름을 읽는다. */
@Composable
private fun RegionPickerHeader(title: String, onBack: (() -> Unit)?) {
    Box(Modifier.fillMaxWidth().heightIn(min = HEADER_MIN_HEIGHT), contentAlignment = Alignment.Center) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = HEADER_SIDE)
                .semantics {
                    heading()
                    liveRegion = LiveRegionMode.Polite
                },
        )
        if (onBack != null) {
            KIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.weather_region_back),
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
                variant = KVariant.Text,
                tone = KTone.Neutral,
                size = ChageunTheme.spacing.minTouchTarget,
            )
        }
    }
}

@Composable
private fun RegionPickerMessage(message: String) {
    Spacer(Modifier.height(ChageunTheme.spacing.xs))
    Text(
        message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun RegionSearchField(query: String, onQueryChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    ChageunTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(R.string.weather_region_search),
        // 지우기 버튼은 입력란이 글자가 있을 때 띄운다.
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
    )
}

@Composable
private fun RegionPickerBody(
    catalog: KoreaRegionCatalog?,
    drill: RegionDrill,
    query: String,
    recentCodes: List<String>,
    onOpen: (KoreaArea) -> Unit,
    onPick: (KoreaArea) -> Unit,
    modifier: Modifier,
) {
    if (catalog == null) {
        RegionLoading(modifier)
        return
    }
    val rows = remember(catalog, drill, query, recentCodes) { regionRows(catalog, drill, query, recentCodes) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        regionSearchSummary(query, rows)?.let { RegionSearchSummaryText(it) }
        if (rows.isNotEmpty()) RegionList(rows, drill, query, onOpen, onPick)
    }
}

@Composable
private fun RegionLoading(modifier: Modifier) {
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm, Alignment.CenterVertically),
    ) {
        KLoadingIndicator(label = stringResource(R.string.weather_region_loading))
        Text(
            stringResource(R.string.weather_region_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 찾은 수를 알린다. 입력할 때마다 TalkBack이 조용히 읽는다. */
@Composable
private fun RegionSearchSummaryText(summary: RegionSearchSummary) {
    val text = when {
        summary.count == 0 -> stringResource(R.string.weather_region_no_results)
        summary.isTruncated -> stringResource(R.string.weather_region_result_many, summary.count)
        else -> pluralStringResource(R.plurals.weather_region_result_count, summary.count, summary.count)
    }
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ChageunTheme.spacing.xs)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun RegionList(
    rows: List<RegionRow>,
    drill: RegionDrill,
    query: String,
    onOpen: (KoreaArea) -> Unit,
    onPick: (KoreaArea) -> Unit,
) {
    val listState = rememberLazyListState()
    // 단계를 옮기거나 검색어가 바뀌면 목록 맨 위부터 보여 준다.
    LaunchedEffect(drill, query) { listState.scrollToItem(0) }
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.large)
            .background(ChageunTheme.colors.inputFill),
    ) {
        itemsIndexed(rows, key = { _, row -> row.key }) { index, row ->
            if (index > 0 && row !is RegionRow.Header && rows[index - 1] !is RegionRow.Header) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = ChageunTheme.spacing.md),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            when (row) {
                is RegionRow.Header -> RegionHeaderRow(stringResource(row.textRes))
                is RegionRow.Whole -> RegionWholeRow(row.area, onPick)
                is RegionRow.Place -> RegionPlaceRow(row, onOpen, onPick)
            }
        }
    }
}

@Composable
private fun RegionHeaderRow(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = ChageunTheme.spacing.md,
                end = ChageunTheme.spacing.md,
                top = ChageunTheme.spacing.md,
                bottom = ChageunTheme.spacing.xs,
            )
            .semantics { heading() },
    )
}

/** "강남구 전체". 내려가지 않고 지금 단계 전체를 고른다. */
@Composable
private fun RegionWholeRow(area: KoreaArea, onPick: (KoreaArea) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = ROW_MIN_HEIGHT)
            .pressable(
                onClick = { onPick(area) },
                style = PressStyle.Highlight,
                onClickLabel = stringResource(R.string.weather_region_choose),
            )
            .padding(horizontal = ChageunTheme.spacing.md, vertical = ChageunTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            stringResource(R.string.weather_region_whole, area.name),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 지역 한 줄. 검색 결과와 최근 지역은 "서울 강남구 역삼1동"처럼 전체 이름을 쓰고 자기 이름만 굵게 한다. */
@Composable
private fun RegionPlaceRow(row: RegionRow.Place, onOpen: (KoreaArea) -> Unit, onPick: (KoreaArea) -> Unit) {
    val area = row.area
    val text = buildAnnotatedString {
        val parent = area.path.removeSuffix(area.name).trimEnd()
        if (row.showsPath && parent.isNotEmpty()) append("$parent ")
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(area.name) }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = ROW_MIN_HEIGHT)
            .pressable(
                onClick = { if (row.opens) onOpen(area) else onPick(area) },
                style = PressStyle.Highlight,
                onClickLabel = stringResource(
                    if (row.opens) R.string.weather_region_open else R.string.weather_region_choose,
                ),
            )
            .padding(horizontal = ChageunTheme.spacing.md, vertical = ChageunTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (row.opens) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val LIST_MAX_HEIGHT = 520.dp
private val ROW_MIN_HEIGHT = 56.dp
private val HEADER_MIN_HEIGHT = 48.dp

// 뒤로 가기 버튼(48dp)과 겹치지 않도록 제목 양옆을 비운다.
private val HEADER_SIDE = 48.dp
