# ADR-003 Convention Plugin 기반 멀티 모듈

Status: Accepted

## Context

앱은 차량 등록, 정비, 기록, 검사·리콜, 알림, AI 공유, 광고를 포함하고 Phone·Fold·Tablet Layout을 함께 지원한다. 기능 간 직접 의존이 생기면 빌드 시간과 변경 영향 범위가 커지고, 정비 Rule Engine처럼 Android와 무관해야 하는 로직이 Framework에 묶이기 쉽다.

## Decision

- 빌드 설정은 `build-logic`의 Convention Plugin으로 일원화하고 버전은 `gradle/libs.versions.toml`에서만 관리한다.
- 모듈은 다음 계층을 따른다.

  ```text
  app
  feature:onboarding · home · manage · history · vehicle · ai · settings
  data:vehicle · maintenance · history · backup · recall · ai
  core:model · domain · common · designsystem · ui · database · datastore · network
  core:security · notification · analytics · ads · testing
  ```

- `core:model`, `core:domain`, `core:common`은 Pure Kotlin(JVM) 모듈이다. 정비 Rule Engine과 UseCase는 `core:domain`에 둔다.
- Feature 모듈끼리는 서로 참조하지 않고 `core` 계약과 `app`의 Navigation 조립을 통해 연결한다.
- 모듈은 실제 코드가 들어가는 시점에 생성한다. 빈 모듈을 미리 만들지 않는다.

## Consequences

- 모듈 단위 빌드 캐시와 병렬 빌드가 가능하고 Rule Engine을 JVM Unit Test로만 검증할 수 있다.
- 새 모듈은 Convention Plugin 한 줄로 동일한 Kotlin·Compose·Lint·Detekt·Ktlint 설정을 받는다.
- 기획 모듈 목록에 없던 추가 모듈:
  - `core:domain`: Rule Engine을 Android 의존성 없이 유지
  - `core:security`: 차량번호·VIN 등 필드 암호화를 DB·Network와 분리
  - `core:ui`: Feature 간 직접 참조 없이 도메인 모델 표시 문구와 공용 Composable 공유
  - `core:datastore`: 테마·알림 같은 사용자 설정을 Room과 분리된 Key-Value로 저장
  - `data:backup`: 모든 테이블과 첨부 파일을 읽는 내보내기·전체 삭제를 기능 Repository와 분리

## Alternatives considered

- **단일 app 모듈**: 초기 속도는 빠르지만 계층 위반을 빌드가 막지 못한다.
- **data 단일 모듈**: 초기 설정 부담은 적지만 기능 간 결합이 생긴다. Domain별 모듈로 시작하되 코드가 적으면 해당 data 모듈 생성 시점을 늦춘다.
