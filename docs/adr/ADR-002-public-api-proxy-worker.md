# ADR-002 공공 API는 Cloudflare Worker를 경유한다

Status: Accepted (공식 데이터 연동 시 적용. V1 범위는 ADR-004)

## Context

국토교통부 자동차종합정보 API는 서비스 Key가 필요하고 XML 응답을 제공하며, 차량등록번호·소유주명과 제3자 제공 동의를 요구하는 항목이 있다. APK에 포함된 Key는 추출이 가능하고, Provider 응답 구조가 바뀌면 배포된 앱을 즉시 수정할 수 없다.

## Decision

- 앱은 공공 API를 직접 호출하지 않고 Cloudflare Worker의 `/v1/*` Endpoint만 호출한다.
- Worker가 Key 보관, Rate Limit, XML → Canonical JSON 변환, 오류 정규화(`INVALID_PLATE`, `OWNER_CONSENT_REQUIRED`, `PROVIDER_UNAVAILABLE` 등), 개인정보 없는 로그를 담당한다.
- Request/Response에 `schema_version`을 둔다.
- 앱의 `VehicleRepository`는 Lookup Adapter Interface에 의존하며, 자동 조회가 실패하거나 Feature Flag로 꺼지면 수동 등록으로 동일한 기능을 제공한다.

## Consequences

- Key가 APK에 존재하지 않는다.
- Provider 변경을 Worker 배포만으로 흡수할 수 있다.
- Worker 장애가 자동 조회 장애가 되므로 Circuit Breaker와 수동 등록 경로가 필수다.
- Worker 무료 한도를 넘지 않도록 사용량 알림과 Hard Limit을 설정한다.

## Alternatives considered

- **앱에서 직접 호출**: Key 노출과 XML 파싱 코드의 앱 내 고착.
- **자체 VM 서버**: 운영 비용과 관리 부담이 MVP 규모에 비해 크다.

## 예외: 키가 없는 공개 API (2026-10-12)

- 홈 날씨는 MET Norway Locationforecast를 앱에서 직접 호출한다. Key가 없고 상업적 이용이 허용되며(CC BY 4.0, 출처 표시), 숨길 비밀이 없어 Worker를 거칠 이유가 없다.
- 대신 좌표를 소수 첫째 자리(약 10km)로 반올림해 보내고, 좌표는 저장하거나 로그에 남기지 않는다.
- Key가 필요한 공공 API(기상청 등)로 바꾸면 이 결정의 원칙대로 Worker를 거친다.
