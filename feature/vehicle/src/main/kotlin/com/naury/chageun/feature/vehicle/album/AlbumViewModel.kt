package com.naury.chageun.feature.vehicle.album

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.album.AlbumRepository
import com.naury.chageun.core.domain.vehicle.VehiclePhotoRepository
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.model.MileageEntry
import com.naury.chageun.core.model.Vehicle
import com.naury.chageun.core.model.VehicleId
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 같은 날 찍은 사진 묶음. */
data class AlbumDay(val date: LocalDate, val photos: List<AlbumPhoto>)

/**
 * 방금 지워 아직 되돌릴 수 있는 사진·동영상. 안내 문구에 쓸 수를 종류별로 들고 있다.
 * [vehicleId]를 함께 들고 있어 화면을 떠나며 마무리할 때 차를 다시 찾지 않는다.
 */
data class AlbumUndo(val vehicleId: VehicleId, val ids: List<String>, val photoCount: Int, val videoCount: Int)

data class AlbumUiState(
    val isLoading: Boolean = true,
    val days: List<AlbumDay> = emptyList(),
    val selected: AlbumPhoto? = null,
    /** 방금 한 장을 넣었거나 수정을 눌러 날짜·코멘트를 입력받는 사진. */
    val editing: AlbumPhoto? = null,
    val failedCount: Int = 0,
    /** 너무 길거나 커서 넣지 않은 동영상 수. */
    val tooLargeCount: Int = 0,
    val isCoverUpdated: Boolean = false,
    /** 편집한 사진·동영상을 그 자리에 저장하지 못했다. 사진 보기에 잠깐 알린다. */
    val isEditFailed: Boolean = false,
    /** 길게 눌러 여러 장을 고르는 중인 사진들. 비어 있으면 고르는 중이 아니다. */
    val picked: Set<String> = emptySet(),
    /** 그리드를 두 손가락으로 오므리고 벌려 고른 칸 크기 단계. 클수록 칸이 크고 한 줄에 적게 들어간다. */
    val gridStep: Int = DEFAULT_GRID_STEP,
    /** 공유할 때 워터마크 추천 문구에 차종을 쓴다. 번호판은 쓰지 않는다. */
    val vehicle: Vehicle? = null,
    /** 공유할 때 워터마크 추천 문구에 사진 찍은 날의 주행거리를 쓴다. 최신순. */
    val mileageLog: List<MileageEntry> = emptyList(),
    /** 방금 지워 되돌리기를 기다리는 사진. 없으면 null. */
    val undo: AlbumUndo? = null,
) {
    val photos: List<AlbumPhoto> get() = days.flatMap { it.photos }
    val isPicking: Boolean get() = picked.isNotEmpty()
}

const val GRID_STEP_COUNT = 5
const val DEFAULT_GRID_STEP = 3

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val albumRepository: AlbumRepository,
    private val photoRepository: VehiclePhotoRepository,
    private val clock: Clock,
) : ViewModel() {

    private val selectedId = savedStateHandle.getStateFlow<String?>(KEY_SELECTED, null)
    private val editingId = savedStateHandle.getStateFlow<String?>(KEY_EDITING, null)

    /** 여러 장 고르기. 화면을 다시 만들어도 남도록 SavedStateHandle에 둔다. */
    val picks = PickedPhotos(savedStateHandle)
    private val gridStep = savedStateHandle.getStateFlow(KEY_GRID_STEP, DEFAULT_GRID_STEP)
    private val messages = MutableStateFlow(Messages())

    /** 지우고 되돌리기. 화면이 사라질 때 기다리던 사진을 마저 지우도록 ViewModel과 함께 닫는다. */
    val deletes = PendingAlbumDelete(viewModelScope, albumRepository).also { addCloseable(it) }
    private val view = combine(
        selectedId,
        editingId,
        picks.ids,
        gridStep,
        deletes.pending,
    ) { selected, editing, picked, step, undo ->
        ViewState(selected, editing, picked.toSet(), step, undo)
    }

    val uiState: StateFlow<AlbumUiState> = vehicleRepository.observePrimaryVehicle()
        .filterNotNull()
        .flatMapLatest { vehicle ->
            combine(
                albumRepository.observe(vehicle.id),
                vehicleRepository.observeMileageLog(vehicle.id),
                view,
                messages,
            ) { photos, mileage, view, message ->
                val ids = photos.mapTo(HashSet()) { it.id }
                AlbumUiState(
                    isLoading = false,
                    days = photos.groupBy { it.takenOn }.map { (date, items) -> AlbumDay(date, items) },
                    selected = photos.firstOrNull { it.id == view.selected },
                    editing = photos.firstOrNull { it.id == view.editing },
                    failedCount = message.failed,
                    tooLargeCount = message.tooLarge,
                    isCoverUpdated = message.coverUpdated,
                    isEditFailed = message.editFailed,
                    picked = view.picked.filterTo(LinkedHashSet()) { it in ids },
                    gridStep = view.gridStep,
                    vehicle = vehicle,
                    mileageLog = mileage,
                    undo = view.undo,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AlbumUiState())

    fun select(id: String?) {
        savedStateHandle[KEY_SELECTED] = id
    }

    /**
     * 사진·동영상을 넣는다. 한 장만 넣었으면 바로 날짜·코멘트를 입력받는다.
     *
     * @param takenOn 찍은 날을 정해 넣을 때. 고른 사진으로 만든 슬라이드쇼처럼 편집기가 만든 MP4에는 찍은 날이 없거나
     *   만든 날로 적혀 있어, 고른 사진 중 가장 늦은 날로 맞춘다. null이면 파일의 날짜, 없으면 오늘이다.
     */
    fun add(sourceUris: List<String>, highQuality: Boolean, takenOn: LocalDate? = null) {
        if (sourceUris.isEmpty()) return
        viewModelScope.launch {
            val vehicleId = vehicleId()
            val result = albumRepository.add(vehicleId, sourceUris, takenOn ?: LocalDate.now(clock), highQuality)
            messages.value = Messages(failed = result.failed, tooLarge = result.tooLarge)
            if (takenOn != null) result.addedIds.forEach { albumRepository.updateDetails(vehicleId, it, takenOn, null) }
            result.addedIds.singleOrNull()?.let { savedStateHandle[KEY_EDITING] = it }
        }
    }

    /** 편집기에서 고친 파일을 같은 자리에 넣는다. 날짜·메모·순서는 그대로다. */
    fun replaceMedia(id: String, sourceUri: String) {
        viewModelScope.launch {
            val replaced = albumRepository.replace(vehicleId(), id, sourceUri)
            if (!replaced) messages.value = Messages(editFailed = true)
        }
    }

    /** [id]의 날짜·메모 입력 창을 연다. null이면 닫는다. */
    fun editDetails(id: String?) {
        savedStateHandle[KEY_EDITING] = id
    }

    fun saveDetails(id: String, takenOn: LocalDate, comment: String) {
        viewModelScope.launch {
            albumRepository.updateDetails(vehicleId(), id, takenOn, comment)
            editDetails(null)
        }
    }

    /**
     * 사진을 바로 목록에서 빼고 되돌리기 안내를 띄운다. 파일은 안내가 끝나거나 화면을 떠날 때 [deletes]가 지운다.
     * 열어 둔 사진을 지우면 옆 사진으로 넘어가고, 고르던 사진에서도 뺀다.
     */
    fun delete(ids: List<String>) {
        if (ids.isEmpty()) return
        val photos = uiState.value.photos
        val removed = photos.filter { it.id in ids }
        // 목록에서 빠지기 전에 옆 사진으로 넘겨야 사진 보기가 잠깐 닫혔다 열리지 않는다.
        if (selectedId.value in ids) select(nextAfterDelete(photos, selectedId.value.orEmpty(), ids.toSet()))
        picks.remove(ids)
        viewModelScope.launch { deletes.start(vehicleId(), ids, removed) }
    }

    /** 그날 사진을 모두 고른다. 이미 모두 골라져 있으면 그날 사진을 모두 뺀다. */
    fun pickDay(date: LocalDate) {
        val day = uiState.value.days.firstOrNull { it.date == date }?.photos?.map { it.id } ?: return
        picks.toggleAll(day)
    }

    /** [delta]가 양수면 칸을 키우고 음수면 줄인다. 범위를 넘으면 그대로 둔다. */
    fun zoomGrid(delta: Int) {
        savedStateHandle[KEY_GRID_STEP] = (gridStep.value + delta).coerceIn(0, GRID_STEP_COUNT - 1)
    }

    /** 앨범 사진을 내 차 대표 사진으로 쓴다. 대표 사진은 배경 제거를 거쳐 따로 저장된다. */
    fun setAsCover(photo: AlbumPhoto) {
        viewModelScope.launch {
            val updated = photoRepository.replace(vehicleId(), Uri.fromFile(File(photo.filePath)).toString())
            messages.value = Messages(coverUpdated = updated)
        }
    }

    fun dismissMessage() {
        messages.value = Messages()
    }

    private suspend fun vehicleId() = vehicleRepository.observePrimaryVehicle().filterNotNull().first().id

    private data class Messages(
        val failed: Int = 0,
        val tooLarge: Int = 0,
        val coverUpdated: Boolean = false,
        val editFailed: Boolean = false,
    )

    private data class ViewState(
        val selected: String?,
        val editing: String?,
        val picked: Set<String>,
        val gridStep: Int,
        val undo: AlbumUndo?,
    )

    private companion object {
        const val KEY_SELECTED = "album_selected"
        const val KEY_EDITING = "album_editing"
        const val KEY_GRID_STEP = "album_grid_step"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/**
 * 되돌릴 수 있게 지운 사진 하나 묶음을 들고 있다. 새로 지우면 앞 묶음은 마저 지운다.
 * [finish]는 화면이 사라지는 중에도 불릴 수 있어 코루틴이 이미 취소됐더라도 시작은 하게 하고, 저장소가 끝까지 지운다.
 */
class PendingAlbumDelete(private val scope: CoroutineScope, private val repository: AlbumRepository) : AutoCloseable {
    private val current = MutableStateFlow<AlbumUndo?>(null)
    val pending: StateFlow<AlbumUndo?> = current

    suspend fun start(vehicleId: VehicleId, ids: List<String>, removed: List<AlbumPhoto>) {
        finish()
        repository.markDeleted(vehicleId, ids)
        current.value = AlbumUndo(
            vehicleId = vehicleId,
            ids = ids,
            photoCount = removed.count { !it.isVideo },
            videoCount = removed.count { it.isVideo },
        )
    }

    /** 방금 지운 사진을 되살린다. */
    fun undo() {
        val last = current.getAndUpdate { null } ?: return
        scope.launch { repository.restoreDeleted(last.vehicleId, last.ids) }
    }

    /** 안내 시간이 끝났을 때, 화면을 떠나거나 앱이 뒤로 갈 때 기다리던 사진을 정말 지운다. */
    fun finish() {
        val last = current.getAndUpdate { null } ?: return
        scope.launch(start = CoroutineStart.ATOMIC) { repository.purgeDeleted(last.vehicleId, last.ids) }
    }

    override fun close() = finish()
}

/** 지운 사진 [removedId] 다음 사진. 뒤에 남은 사진이 없으면 앞 사진이고, 모두 지웠으면 null이다. */
internal fun nextAfterDelete(photos: List<AlbumPhoto>, removedId: String, removed: Set<String>): String? {
    val index = photos.indexOfFirst { it.id == removedId }
    if (index < 0) return null
    val after = photos.drop(index + 1).firstOrNull { it.id !in removed }
    return (after ?: photos.take(index).lastOrNull { it.id !in removed })?.id
}

/** 앨범에서 길게 눌러 고른 사진 id. 고른 순서를 지킨다. */
class PickedPhotos(private val savedStateHandle: SavedStateHandle) {
    val ids: StateFlow<ArrayList<String>> = savedStateHandle.getStateFlow(KEY_PICKED, arrayListOf())

    fun toggle(id: String) {
        val current = ids.value
        set(if (id in current) current - id else current + id)
    }

    /** 모두 골라져 있으면 모두 빼고, 아니면 빠진 것을 더한다. */
    fun toggleAll(group: List<String>) {
        val current = ids.value
        set(if (current.containsAll(group)) current - group.toSet() else (current + group).distinct())
    }

    fun remove(group: List<String>) = set(ids.value - group.toSet())

    fun clear() = set(emptyList())

    private fun set(value: List<String>) {
        savedStateHandle[KEY_PICKED] = ArrayList(value)
    }

    private companion object {
        const val KEY_PICKED = "album_picked"
    }
}
