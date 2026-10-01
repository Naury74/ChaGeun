# ADR-001 Room 중심 Offline First

Status: Accepted

## Context

차량 관리 데이터의 대부분은 사용자가 직접 입력하는 기록이고, 공공 API는 승인 조건과 장애 가능성이 있다. 계정 없이 첫 실행부터 사용할 수 있어야 하며, 네트워크가 없어도 차량 상태와 기록을 확인할 수 있어야 한다.

## Decision

- Room을 앱의 단일 진실 공급원(SSOT)으로 사용한다. UI는 Room을 관찰하고 네트워크 결과를 직접 기다리지 않는다.
- 공공 데이터 갱신은 Background에서 수행하고 성공 시 Transaction으로 반영한다. 실패해도 기존 데이터를 유지하고 최신성 정보만 갱신한다.
- 모든 표시 데이터는 출처 메타데이터(`sourceType`, `fetchedAt`, `effectiveAt`, `confidence`)를 가진다.
- Timeline은 단일 이력 테이블이 아니라 Domain 테이블의 Query Projection으로 구성한다.
- Schema는 Export하여 Repository에 보관하고, 모든 Migration은 테스트한다. Destructive Migration은 Debug 외 금지한다.
- 시각은 UTC `Instant`, 정비일처럼 시간대가 불필요한 값은 `LocalDate`, 금액은 정수 원, 거리는 km로 저장한다.

## Consequences

- 로컬 저장 완료가 곧 성공이므로 V1에서 동기화 충돌 처리가 필요 없다.
- V1.5 Cloud Sync는 Outbox 패턴과 `version`/`updatedAt`/`deletedAt` 필드를 추가해야 한다.
- 기기 분실 시 복구는 명시적 Export/Import에 의존한다.

## Alternatives considered

- **Server-first**: 계정 강제와 서버 비용이 발생하고 오프라인 사용이 불가하다.
- **DataStore/파일 저장**: 관계형 Query, Migration 검증, Timeline Projection이 어렵다.
