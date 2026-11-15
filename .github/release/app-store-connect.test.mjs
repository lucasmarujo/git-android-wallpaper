import assert from 'node:assert/strict';
import { test } from 'node:test';
import { compareVersions, checkVersion, buildSituation } from './app-store-connect.mjs';

test('compares versions numerically, padding with zeros', () => {
  assert.equal(compareVersions('1.0.1', '1.0'), 1);
  assert.equal(compareVersions('1.0', '1.0.0'), 0);
  assert.equal(compareVersions('1.10.0', '1.9.9'), 1);
  assert.equal(compareVersions('1.2.0', '2.0'), -1);
});

test('a version equal to the approved one is rejected, like the closed Apple train', () => {
  const result = checkVersion('1.0.0', [{ version: '1.0', state: 'READY_FOR_SALE' }]);
  assert.deepEqual(result, { ok: false, latest: '1.0' });
});

test('a version above the latest approved one passes', () => {
  const store = [
    { version: '1.0', state: 'READY_FOR_SALE' },
    { version: '1.0.2', state: 'REPLACED_WITH_NEW_VERSION' },
  ];
  assert.deepEqual(checkVersion('1.0.3', store), { ok: true, latest: '1.0.2' });
  assert.equal(checkVersion('1.0.2', store).ok, false);
});

test('a version in review or rejected does not close the train', () => {
  const store = [
    { version: '1.0', state: 'WAITING_FOR_REVIEW' },
    { version: '0.9', state: 'REJECTED' },
  ];
  assert.deepEqual(checkVersion('1.0.0', store), { ok: true, latest: null });
});

test('an app without store versions accepts any version', () => {
  assert.deepEqual(checkVersion('1.0.0', []), { ok: true, latest: null });
});

test('build situation on App Store Connect', () => {
  assert.equal(buildSituation([]), 'missing');
  assert.equal(buildSituation([{ state: 'PROCESSING' }]), 'processing');
  assert.equal(buildSituation([{ state: 'VALID' }]), 'valid');
  assert.equal(buildSituation([{ state: 'INVALID' }]), 'invalid');
  assert.equal(buildSituation([{ state: 'FAILED' }]), 'invalid');
});
