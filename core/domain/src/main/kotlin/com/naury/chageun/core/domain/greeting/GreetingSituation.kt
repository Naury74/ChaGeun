package com.naury.chageun.core.domain.greeting

/** 홈 인사말을 고른 상황. 상황마다 화면에 여러 문구가 있고, 그중 하나를 날짜로 고른다. */
enum class GreetingSituation {
    // 안전과 관련된 차 상태
    Recall,
    InspectionOverdue,
    MaintenanceOverdue,
    InspectionSoon,

    // 처음 왔거나 오랜만에 온 사용자
    FirstVisit,
    WelcomeBack,

    // 공휴일·기념일
    NewYear,
    IndependenceMovementDay,
    ChildrensDay,
    MemorialDay,
    LiberationDay,
    NationalFoundationDay,
    HangulDay,
    Christmas,

    // 차와 함께한 날
    CarAnniversary,
    MileageMilestone,
    RecordedRecently,

    // 요일·출퇴근
    MondayMorning,
    FridayEvening,
    CommuteMorning,
    CommuteEvening,
    Weekend,
    MileageStale,

    // 계절 오후
    SpringAfternoon,
    SummerAfternoon,
    AutumnAfternoon,
    WinterAfternoon,

    // 시간대
    Dawn,
    Morning,
    Lunch,
    Afternoon,
    Evening,
    Night,
}
