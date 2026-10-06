# Firebase 규칙

클라우드 백업(에픽 #148)의 Firestore·Storage 보안 규칙이다. 앱 빌드와 CI에는 포함되지 않는다.

| 파일 | 내용 |
|---|---|
| `firestore.rules` | `users/{uid}`, `users/{uid}/backups/{backupId}` 본인만. 백업 문서는 만들기·읽기·삭제만 되고 필드 형식을 검사한다 |
| `storage.rules` | `users/{uid}/backups/{backupId}.zip` 본인만. ZIP, 200MB 미만, 덮어쓰기 불가 |
| `test/` | 에뮬레이터 기반 규칙 테스트 |

## 백업 문서 필드

`createdAt`(timestamp), `sizeBytes`(int, 200MB 미만), `schemaVersion`(int), `appVersion`(string, 32자 이하), `recordCount`, `photoCount`(int, 0 이상)는 필수다. `deviceModel`, `vehicleLabel`(string, 64자 이하)은 선택이다. 이 밖의 필드는 거부한다.

## 테스트 실행

Node 20 이상과 Java 11 이상이 필요하다. 실제 프로젝트가 아닌 `demo-chageun` 에뮬레이터에서만 돈다.

```sh
cd firebase/test
npm install
npm test
```

## 배포

Firebase CLI로 로그인한 뒤 저장소의 `firebase/` 폴더에서 실행한다.

```sh
npx firebase-tools login
npx firebase-tools deploy --only firestore:rules,storage
```
