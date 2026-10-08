# 출시 절차

`release-android.yml`이 태그부터 Production 5%까지 진행한다. 순서는 CICD §11.4를 따른다.

1. `v*.*.*` 태그가 `main` 커밋인지 확인
2. 태그와 `versionName`이 같은지 확인
3. 전체 검사 다시 실행
4. 서명된 Release AAB 빌드(`release-build` Environment)
5. 16KB 페이지 크기 정렬 검사(`scripts/check-16kb-alignment.sh`)
6. 서명과 R8 mapping 확인
7. Internal 트랙 업로드와 mapping 등록(`play-internal` Environment)
8. Required Reviewer 승인 뒤 Production 5% 단계적 배포(`production` Environment)

## 버전 올리기

- 출시 PR에서 `app/build.gradle.kts`의 `versionName`과 `versionCode`를 함께 올린다. `versionCode`는 이전에 Play에 올린 값보다 커야 하고 CI 번호로 만들지 않는다.
- 합친 뒤 그 `main` 커밋에 `v<versionName>` 태그를 단다: `git tag v0.2.0 && git push origin v0.2.0`.
- 0.1.0(versionCode 1)은 로컬에서 만든 AAB가 있다. 다음 출시는 versionCode 2부터다.

## 처음 한 번 설정

GitHub 저장소 Settings › Environments에 세 Environment를 만든다.

| Environment | 보호 규칙 | Secret / Variable |
|---|---|---|
| `release-build` | `v*.*.*` 태그만 배포 허용 | Secret: `UPLOAD_KEYSTORE_BASE64`(업로드 키 `base64 -i chageun-upload.jks`), `UPLOAD_STORE_PASSWORD`, `UPLOAD_KEY_ALIAS`, `UPLOAD_KEY_PASSWORD`, `GOOGLE_SERVICES_JSON_BASE64`, `ADMOB_APP_ID`, `ADMOB_NATIVE_UNIT_ID` |
| `play-internal` | `v*.*.*` 태그만 배포 허용 | Variable: `GCP_WORKLOAD_IDENTITY_PROVIDER`, `GCP_PLAY_SERVICE_ACCOUNT` |
| `production` | Required reviewer: 본인, 태그만 허용 | Variable: `GCP_WORKLOAD_IDENTITY_PROVIDER`, `GCP_PLAY_SERVICE_ACCOUNT`, `IN_APP_UPDATE_PRIORITY`(평소 0, 강제 업데이트 때 4~5) |

- Play 업로드는 장기 JSON 키 대신 GitHub OIDC와 Google Cloud Workload Identity를 쓴다. 서비스 계정에는 Play Console에서 차근 앱의 출시 권한만 준다.
- 키스토어와 비밀번호는 저장소, 이슈, PR에 절대 쓰지 않는다. 반기마다 담당자와 Secret을 점검한다.

## 출시 체크리스트

출시마다 이 목록을 출시 PR 설명에 복사해 채운다.

- [ ] 담당: (이름)
- [ ] `versionName` / `versionCode` 올림, 태그 생성
- [ ] DB 버전이 바뀌었다면 Migration 테스트 통과와 이전 버전 백업 가져오기 확인
- [ ] 개인정보처리방침·Data Safety(`data-safety.md`)가 이번 변경과 맞는지 확인
- [ ] 폴드 에뮬레이터(접기·펴기)에서 홈·관리·기록·내 차·AI 주요 흐름 확인
- [ ] Internal 트랙에서 설치·업데이트·첫 실행 스모크 테스트
- [ ] 강제 업데이트가 필요한 버전이면 `IN_APP_UPDATE_PRIORITY` 설정
- [ ] Production 승인

## 단계적 배포와 중단 기준

5% → 20% → 50% → 100%로 Play Console에서 올린다. 단계마다 최소 24시간 지켜본다.

다음 중 하나라도 해당하면 배포를 멈추고(Play Console › 출시 중단) 원인을 이슈로 남긴다.

- Crash-free 사용자 비율이 직전 버전보다 0.5%p 이상 낮음
- 사용자 체감 ANR 비율 0.47% 이상(Play 기준선)
- 정비 기록 저장, 주행거리 갱신, 백업 가져오기 관련 오류가 새로 보고됨
- 광고 관련 비정상 종료 증가

## 되돌리기

Play는 versionCode를 낮출 수 없다. 이전 커밋을 되돌린 코드로 `versionCode`를 더 높여 다시 출시한다. DB 버전을 올린 출시를 되돌릴 때는 스키마를 내리지 않고 새 Migration으로 무시하는 방향으로 처리한다. 각 출시의 AAB와 mapping은 워크플로 아티팩트(90일)와 로컬 `~/Documents/ChaGeun-releases/<versionName>-<versionCode>/`에 보관한다.
