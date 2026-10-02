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
| Analytics 이벤트 | `AnalyticsEvent` 타입으로만 기록하며 파라미터는 enum·Boolean·정수만 허용. 외부 SDK 연결 전까지 Debug Logcat에만 남는다 |

## Secret 운영

- 로컬 개발 값은 `local.properties` 또는 환경변수에만 둔다.
- CI Secret은 GitHub Environment로 분리하고 Fork PR에 제공하지 않는다.
- 노출이 의심되면 즉시 폐기·회전하며 Git History 삭제만으로 종료하지 않는다.
