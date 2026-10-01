# Contributing

## 작업 흐름

1. Issue 생성 — 문제, 완료 조건, 범위, 테스트 포인트
2. `main`에서 Branch 생성
   ```text
   feature/CHGN-<issue>-<slug>
   fix/CHGN-<issue>-<slug>
   refactor/CHGN-<issue>-<slug>
   chore/CHGN-<issue>-<slug>
   ```
3. PR 생성 — Template의 모든 항목 작성, 본문에 `Closes #<issue>`
4. Required Check 통과 후 Squash Merge

`main` 직접 Push와 Force Push는 금지한다. 1인 개발 기간에는 Approval 0개를 허용하되 Required Check와 Self Review는 생략하지 않는다.

## Commit

[Conventional Commits](https://www.conventionalcommits.org/)를 사용한다. Squash Merge 시 PR 제목이 Commit 제목이 된다.

```text
feat(vehicle): add manual registration fallback
fix(maintenance): keep unknown state without mileage
build(ci): add pull request quality checks
docs(adr): record local-first storage decision
```

Scope는 Module 또는 Domain 이름을 사용한다. 하나의 PR은 하나의 목적만 가지며 생성 파일을 제외하고 400줄 안팎을 권장한다.

## 로컬 검증

```bash
./gradlew ktlintFormat                # 자동 수정
./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug
```

## 코드

- [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide), Wildcard Import 금지
- 식별자·Package·KDoc은 영어, 사용자 문구는 `strings.xml`
- `Util`, `Manager`, `Helper`, `Common`, `Temp`처럼 책임이 드러나지 않는 이름 금지
- UI는 Room Entity와 Network DTO를 직접 사용하지 않는다
- 날짜·금액·주행거리는 문자열이 아니라 Type으로 계산한다
- 로그는 `AppLogger`만 사용하고 차량번호·VIN·소유주명·질문/메모 원문·Token을 남기지 않는다

### 주석

코드가 설명하지 못하는 **이유와 제약**만 남긴다.

```kotlin
// A missing safety record must remain UNKNOWN; showing GOOD could mislead the user.
// SECURITY: Owner name is used for lookup only and must not be persisted or logged.
// WORKAROUND(CHGN-184): <원인>. Remove after <조건>.
// TODO(CHGN-231): <후속 작업>
```

Ticket 없는 `TODO`·`FIXME`, 동작을 반복 설명하는 주석, 주석 처리된 코드는 남기지 않는다.

## 의존성 추가

PR 본문에 해결하는 문제, 표준 기능 대체 가능성, APK 크기 영향, Native `.so` 포함 여부(16KB Page), 라이선스, 유지보수 상태를 적는다. 버전은 `gradle/libs.versions.toml`에서만 관리한다.

## Repository에 넣지 않는 것

Build 산출물, APK/AAB, Keystore, API Key, Test/Lint Report, Screenshot 결과, 작업일지·진행현황 문서. 결과물은 CI Artifact 또는 PR에 첨부한다.

## ADR

되돌리기 어렵고 여러 Module에 영향을 주는 결정만 `docs/adr/`에 기록한다.
