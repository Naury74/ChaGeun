# ADR-005 선택 계정과 Firebase 클라우드 백업

Status: Accepted (저장소 부분은 ADR-006으로 대체)

## Context

기기를 바꾸거나 잃어버리면 기록을 되살릴 방법이 ZIP 내보내기·가져오기뿐이다. 사용자는 차근 계정(이메일)이나 Google 계정으로 로그인해 백업하고 내려받기를 원한다.

기획서는 계정을 "첫 실행 때 없음, 클라우드 백업이나 MCP가 필요할 때 생성"으로 정했고, 서버는 Cloudflare Worker와 D1 동기화(V1.5)를 가정했다. 이미 Firebase(Analytics, Crashlytics)를 쓰고 있어서 인증과 저장소를 Firebase에 두면 서버 코드 없이 시작할 수 있다.

## Decision

- 계정은 선택이다. 로그인하지 않아도 모든 기능을 쓸 수 있고, 로컬 Room이 계속 단일 진실 공급원이다(ADR-001 유지).
- 인증은 Firebase Authentication을 쓴다.
  - 차근 계정: 이메일·비밀번호. 이메일 인증과 비밀번호 재설정을 제공한다.
  - Google 계정: Credential Manager(Sign in with Google)로 받은 ID 토큰을 Firebase에 넘긴다.
  - 같은 이메일로 두 방식을 쓰면 하나의 Firebase 사용자로 연결한다.
- 백업은 실시간 동기화가 아니라 **스냅숏**이다. 기존 내보내기 ZIP(`data.json`과 첨부 이미지)을 그대로 올린다.
  - 파일은 Cloud Storage `users/{uid}/backups/{backupId}.zip`, 목록과 요약 정보는 Firestore `users/{uid}/backups/{backupId}`에 둔다.
  - 보안 규칙은 본인 `uid` 경로만 읽고 쓰게 한다. 규칙 파일은 저장소에 두고 배포한다.
  - 차량번호는 지금처럼 마스킹한 값만 ZIP에 들어간다. 전체 번호는 기기 밖으로 나가지 않는다.
  - 최근 백업만 보관한다(기본 5개). 자동 백업은 사용자가 켤 때만, Wi-Fi·충전 중에 주 1회 한다.
- 내려받기는 기존 가져오기 흐름(미리보기 → 확인 → 전체 교체)을 그대로 쓴다. 새 기기 온보딩 첫 화면에서 "백업에서 복원"을 고를 수 있다.
- 계정 삭제는 앱 안(설정)과 웹 요청 페이지 모두에서 할 수 있다. 삭제하면 Firebase 사용자, Firestore 문서, Storage 백업을 모두 지운다. 기기 안의 기록은 남긴다.
- 실시간 동기화, 여러 기기 동시 편집, MCP는 범위 밖이다. 필요해지면 기획서 23장의 Outbox와 `version`/`deletedAt` 방식으로 따로 결정한다.

## Consequences

- 앱에 네트워크 요청과 개인정보(이메일, Firebase UID, 클라우드에 저장된 기록·사진)가 생긴다. 개인정보처리방침, Play 데이터 보안, 계정 삭제 안내를 함께 갱신해야 한다.
- Cloud Storage는 Blaze(종량제) 요금제가 필요하다. 무료 사용량 안에서는 비용이 없지만 결제 계정을 연결해야 하고, 예산 알림을 설정해 둔다.
- Google 로그인에는 debug, upload, Play 앱 서명 키의 SHA 지문을 Firebase에 등록해야 한다.
- 백업은 덮어쓰기 복원이라 두 기기에서 따로 기록하면 마지막 복원 기준으로 정리된다. 화면에서 이 점을 분명히 알린다.

## Alternatives considered

- **Cloudflare Worker + D1 (기획서 원안)**: 서버 코드, 인증 구현, 운영 부담이 크다. 동기화를 만들 때 다시 검토한다.
- **Google Drive 앱 데이터 폴더**: 계정 없이 Google 로그인만으로 가능하지만, 차근 이메일 계정을 지원할 수 없다.
- **Firestore에만 저장**: 문서 크기 제한(1MB) 때문에 사진을 담을 수 없다.
- **실시간 동기화부터 구현**: 충돌 처리와 스키마 변경이 커서 백업 요구에 비해 과하다.
