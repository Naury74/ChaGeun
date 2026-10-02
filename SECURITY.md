# Security Policy

## 취약점 제보

공개 Issue 대신 GitHub [Private vulnerability reporting](https://github.com/Naury74/ChaGeun/security/advisories/new)으로 제보한다. 재현 절차와 영향 범위를 포함하고 실제 차량번호·VIN·소유주명은 첨부하지 않는다.

## 민감 데이터 원칙

| 데이터 | 원칙 |
|---|---|
| 소유주명 | 조회에만 사용, 저장·로그 금지 |
| 차량번호·VIN | Android Keystore 기반 암호화 저장, 화면·로그는 마스킹 |
| 공공 API Key | Cloudflare Worker Secret에만 보관, APK 포함 금지 |
| OAuth Token | Room 저장 금지 |
| 영수증·사진 | 로컬 우선, 외부 전송 시 위치 EXIF 제거 |
| AI 질문·메모 원문 | Analytics·Crash Report 전송 금지 |
| Analytics 이벤트 | `AnalyticsEvent` 타입으로만 기록하며 파라미터는 enum·Boolean·정수만 허용. Firebase Analytics로 보내되 광고 ID·SSAID는 수집하지 않고, 설정에서 끄면 수집을 멈춘다 |

## Secret 운영

- `app/google-services.json`은 Git에 올리지 않는다. 파일이 없으면 Firebase 없이 빌드되며, 배포 빌드는 CI Secret에서 파일을 복원한다.

- 로컬 개발 값은 `local.properties` 또는 환경변수에만 둔다.
- CI Secret은 GitHub Environment로 분리하고 Fork PR에 제공하지 않는다.
- 노출이 의심되면 즉시 폐기·회전하며 Git History 삭제만으로 종료하지 않는다.
