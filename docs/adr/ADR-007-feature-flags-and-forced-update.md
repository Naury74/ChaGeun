# ADR-007 기능 스위치는 앱 기본값으로, 강제 업데이트는 Play 인앱 업데이트로

Status: Accepted

## Context

기획 §36.2는 앱이 최소 지원 버전과 강제 업데이트 여부를 조회할 수 있어야 한다고 하고, §36.3은 6개의 기능 스위치를 두되 처음에는 상용 Remote Config 대신 Worker의 서명된 공개 설정이나 앱 기본값을 쓰라고 한다. V1은 수동 등록으로 출시해 Worker가 없다(ADR-004). 개발자 비용이 생기면 안 된다(ADR-006).

## Decision

- `FeatureFlags`(core:model)와 `FeatureFlagRepository`(core:domain)를 둔다. V1 구현은 앱 기본값만 내보낸다.
  - `native_ads_enabled`: `AdGate`에 묶어 끄면 광고 요청 자체를 하지 않는다.
  - `ai_share_enabled`: 홈 AI 카드, 관리 상세의 "AI로 설명", 기록 상세의 "이 기록을 AI에게 물어보기"를 감춘다.
  - `vehicle_auto_lookup_enabled`, `recall_refresh_enabled`, `mcp_connection_enabled`: V1에 없는 기능이라 기본 꺼짐. 기능을 만들 때 이 값으로 작업과 통신을 막는다.
  - `maintenance_prediction_enabled`: 핵심 기능이라 기본 켜짐. 예측을 따로 끄는 경로는 원격 설정이 생길 때 붙인다.
- 화면에는 `LocalFeatureFlags`로 넘긴다.
- 강제 업데이트는 Google Play 인앱 업데이트(app-update 2.1.0)를 쓴다. 출시할 때 업데이트 우선순위를 4 이상으로 주면 앱을 열거나 돌아올 때 전체 화면 업데이트를 띄운다. 우선순위는 Play Developer API로 출시할 때 정한다(`inAppUpdatePriority`).

## Consequences

- 서버 없이 강제 업데이트가 가능하고 비용이 없다. 다만 Play에서 설치한 앱에만 동작하고, 우선순위는 콘솔 화면이 아니라 API로 출시할 때만 정할 수 있다.
- 기능 스위치는 지금 앱 업데이트로만 바뀐다. 장애 때 원격으로 끄려면 Worker의 서명된 공개 설정이 필요하다. Worker를 만들 때 `FeatureFlagRepository` 구현만 바꾼다.

## Alternatives considered

- **Firebase Remote Config**: 무료지만 기획이 초기 의존을 피하라고 했고, 설정 서명 검증이 없다.
- **GitHub Pages의 정적 JSON**: 서버 없이 원격 값을 줄 수 있지만 서명·캐시·오프라인 기본값 설계가 필요해 Worker와 함께 정한다.
