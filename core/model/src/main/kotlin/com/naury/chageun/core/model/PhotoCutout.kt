package com.naury.chageun.core.model

/** 내 차 사진에서 배경을 지우는 진행 상태. Hero 아래에 안내로 보여 준다. */
sealed interface CutoutStatus {
    /** 진행 중인 작업이 없다. */
    data object Idle : CutoutStatus

    /** 배경 지우기 모델을 처음 한 번 내려받는 중이다. [progress]는 0~1이며 크기를 아직 모르면 null이다. */
    data class DownloadingModel(val progress: Float?) : CutoutStatus

    data object Processing : CutoutStatus

    data class Failed(val reason: CutoutFailure) : CutoutStatus
}

enum class CutoutFailure {
    /** 모델을 내려받지 못했다. 대개 네트워크 문제라 다시 시도하면 된다. */
    ModelUnavailable,

    /** 사진에서 차를 찾지 못했다. 같은 사진으로 다시 해도 결과가 같다. */
    NoSubject,

    /** 처리 중 프로세스가 죽었거나 너무 오래 걸렸다. */
    Error,
}
