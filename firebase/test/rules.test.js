// 보안 규칙 단위 테스트. 에뮬레이터 위에서만 돈다(실행 방법은 ../README.md).
import { after, before, beforeEach, test } from 'node:test';
import { readFileSync } from 'node:fs';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { Timestamp, deleteDoc, doc, getDoc, setDoc, updateDoc } from 'firebase/firestore';
import { deleteObject, getBytes, ref, uploadBytes } from 'firebase/storage';

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-chageun',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
    storage: { rules: readFileSync(new URL('../storage.rules', import.meta.url), 'utf8') },
  });
});

beforeEach(async () => {
  await env.clearFirestore();
  await env.clearStorage();
});

after(async () => env.cleanup());

const backup = (overrides = {}) => ({
  createdAt: Timestamp.now(),
  sizeBytes: 1024,
  schemaVersion: 6,
  appVersion: '1.0.0',
  deviceModel: 'Pixel 9 Pro Fold',
  vehicleLabel: 'Kia Sportage',
  recordCount: 12,
  photoCount: 3,
  ...overrides,
});

const zip = new Uint8Array([0x50, 0x4b, 0x03, 0x04]);
const zipMeta = { contentType: 'application/zip' };

test('본인은 백업 문서를 만들고 읽고 지울 수 있다', async () => {
  const db = env.authenticatedContext('alice').firestore();
  const path = doc(db, 'users/alice/backups/b1');
  await assertSucceeds(setDoc(path, backup()));
  await assertSucceeds(getDoc(path));
  await assertSucceeds(deleteDoc(path));
});

test('다른 사용자의 백업은 읽거나 쓸 수 없다', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'users/alice/backups/b1'), backup());
  });
  const bob = env.authenticatedContext('bob').firestore();
  await assertFails(getDoc(doc(bob, 'users/alice/backups/b1')));
  await assertFails(setDoc(doc(bob, 'users/alice/backups/b2'), backup()));
  await assertFails(deleteDoc(doc(bob, 'users/alice/backups/b1')));
});

test('로그인하지 않으면 아무것도 할 수 없다', async () => {
  const anon = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(anon, 'users/alice')));
  await assertFails(setDoc(doc(anon, 'users/alice/backups/b1'), backup()));
});

test('백업 문서는 수정할 수 없고 형식이 맞아야 만들 수 있다', async () => {
  const db = env.authenticatedContext('alice').firestore();
  const path = doc(db, 'users/alice/backups/b1');
  await assertSucceeds(setDoc(path, backup()));
  await assertFails(updateDoc(path, { recordCount: 99 }));
  await assertFails(setDoc(doc(db, 'users/alice/backups/b2'), backup({ sizeBytes: 300 * 1024 * 1024 })));
  await assertFails(setDoc(doc(db, 'users/alice/backups/b3'), backup({ plate: '123가4567' })));
  await assertFails(setDoc(doc(db, 'users/alice/backups/b4'), backup({ recordCount: -1 })));
});

test('사용자 문서는 정해진 필드만 쓸 수 있다', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertSucceeds(setDoc(doc(db, 'users/alice'), { createdAt: Timestamp.now() }));
  await assertSucceeds(updateDoc(doc(db, 'users/alice'), { lastBackupAt: Timestamp.now() }));
  await assertFails(updateDoc(doc(db, 'users/alice'), { email: 'a@example.com' }));
});

test('Storage: 본인은 ZIP을 올리고 받고 지울 수 있다', async () => {
  const storage = env.authenticatedContext('alice').storage();
  const file = ref(storage, 'users/alice/backups/b1.zip');
  await assertSucceeds(uploadBytes(file, zip, zipMeta));
  await assertSucceeds(getBytes(file));
  await assertSucceeds(deleteObject(file));
});

test('Storage: 다른 사용자 경로, ZIP이 아닌 파일, 덮어쓰기는 거부한다', async () => {
  const alice = env.authenticatedContext('alice').storage();
  const bob = env.authenticatedContext('bob').storage();
  await assertSucceeds(uploadBytes(ref(alice, 'users/alice/backups/b1.zip'), zip, zipMeta));
  await assertFails(getBytes(ref(bob, 'users/alice/backups/b1.zip')));
  await assertFails(uploadBytes(ref(bob, 'users/alice/backups/b2.zip'), zip, zipMeta));
  await assertFails(uploadBytes(ref(alice, 'users/alice/backups/b3.zip'), zip, { contentType: 'image/jpeg' }));
  await assertFails(uploadBytes(ref(alice, 'users/alice/backups/b4.jpg'), zip, zipMeta));
  await assertFails(uploadBytes(ref(alice, 'users/alice/backups/b1.zip'), zip, zipMeta));
  await assertFails(uploadBytes(ref(alice, 'users/alice/other.zip'), zip, zipMeta));
});
