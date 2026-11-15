import { createSign } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';

export const APPROVED_STATES = new Set([
  'ACCEPTED',
  'PENDING_APPLE_RELEASE',
  'PENDING_DEVELOPER_RELEASE',
  'PROCESSING_FOR_APP_STORE',
  'READY_FOR_DISTRIBUTION',
  'READY_FOR_SALE',
  'PREORDER_READY_FOR_SALE',
  'REPLACED_WITH_NEW_VERSION',
  'REMOVED_FROM_SALE',
  'DEVELOPER_REMOVED_FROM_SALE',
]);

export function compareVersions(a, b) {
  const parts = (version) => String(version).split('.').map((part) => Number.parseInt(part, 10) || 0);
  const [x, y] = [parts(a), parts(b)];
  for (let i = 0; i < Math.max(x.length, y.length); i++) {
    const difference = (x[i] ?? 0) - (y[i] ?? 0);
    if (difference !== 0) return Math.sign(difference);
  }
  return 0;
}

export function checkVersion(version, storeVersions) {
  const approved = storeVersions.filter((v) => APPROVED_STATES.has(v.state)).map((v) => v.version);
  const latest = approved.sort(compareVersions).at(-1) ?? null;
  return { ok: latest === null || compareVersions(version, latest) > 0, latest };
}

export function buildSituation(builds) {
  if (builds.length === 0) return 'missing';
  const states = builds.map((build) => build.state);
  if (states.includes('VALID')) return 'valid';
  if (states.some((state) => state === 'INVALID' || state === 'FAILED')) return 'invalid';
  return 'processing';
}

const base64url = (data) => Buffer.from(data).toString('base64url');

function apiToken({ keyId, issuerId, key }) {
  const now = Math.floor(Date.now() / 1000);
  const header = base64url(JSON.stringify({ alg: 'ES256', kid: keyId, typ: 'JWT' }));
  const payload = base64url(JSON.stringify({ iss: issuerId, iat: now, exp: now + 1200, aud: 'appstoreconnect-v1' }));
  const signature = createSign('SHA256')
    .update(`${header}.${payload}`)
    .sign({ key: key, dsaEncoding: 'ieee-p1363' })
    .toString('base64url');
  return `${header}.${payload}.${signature}`;
}

async function request(path, credentials) {
  const response = await fetch(`https://api.appstoreconnect.apple.com${path}`, {
    headers: { Authorization: `Bearer ${apiToken(credentials)}` },
  });
  if (!response.ok) throw new Error(`App Store Connect answered ${response.status} on ${path.split('?')[0]}`);
  return response.json();
}

async function storeVersions(appId, credentials) {
  const body = await request(
    `/v1/apps/${appId}/appStoreVersions?limit=200&fields[appStoreVersions]=versionString,appStoreState`,
    credentials
  );
  return body.data.map((v) => ({ version: v.attributes.versionString, state: v.attributes.appStoreState }));
}

async function versionBuilds(appId, version, number, credentials) {
  const filter = `filter[app]=${appId}&filter[version]=${encodeURIComponent(number)}&filter[preReleaseVersion.version]=${encodeURIComponent(version)}`;
  const body = await request(`/v1/builds?${filter}&fields[builds]=version,processingState`, credentials);
  return body.data.map((b) => ({ number: b.attributes.version, state: b.attributes.processingState }));
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function main([command, appId, version, number, minutes = '45']) {
  const credentials = {
    keyId: process.env.APP_STORE_CONNECT_API_KEY_ID,
    issuerId: process.env.APP_STORE_CONNECT_API_ISSUER_ID,
    key: readFileSync(process.env.ASC_KEY_PATH, 'utf8'),
  };

  if (command === 'check-version') {
    const { ok, latest } = checkVersion(version, await storeVersions(appId, credentials));
    if (!ok) {
      console.log(`::error::Version ${version} is not above the latest one approved on the App Store (${latest}). TestFlight would reject the build. Raise the app version.`);
      process.exit(1);
    }
    console.log(`Version ${version} is above the latest approved one (${latest ?? 'none'}).`);
    return;
  }

  if (command === 'wait-for-build') {
    if (!/^\d+(\.\d+)*$/.test(number ?? '')) {
      console.log(`::error::Invalid build number: "${number}".`);
      process.exit(1);
    }
    const deadline = Date.now() + Number(minutes) * 60_000;
    let situation = 'missing';
    while (Date.now() < deadline) {
      situation = buildSituation(await versionBuilds(appId, version, number, credentials));
      console.log(`${new Date().toISOString()} build ${version} (${number}): ${situation}`);
      if (situation === 'valid') return;
      if (situation === 'invalid') break;
      await sleep(30_000);
    }
    console.log(`::error::Build ${version} (${number}) did not become VALID on App Store Connect (situation: ${situation}).`);
    process.exit(1);
  }

  console.log('::error::Usage: app-store-connect.mjs check-version <appId> <version> | wait-for-build <appId> <version> <build> [minutes]');
  process.exit(2);
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  await main(process.argv.slice(2));
}
