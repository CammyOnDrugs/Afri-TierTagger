from pathlib import Path
import hashlib
import json
import os
import shutil
import subprocess
import urllib.error
import urllib.parse
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSIONS = ['1.21.11', '26.1', '26.1.1', '26.1.2', '26.2', '26.3']
BUILD_COMMIT = 'a2b15ea38f1c861035550d40a031491b015fb916'
BUILD_RUN = 40178936454
TAG = 'v2.8.1'


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def prepare():
    paths = subprocess.check_output(['git', 'ls-files', '-z'], cwd=ROOT).decode().split('\0')
    paths = [path for path in paths if path]
    if any(path.split('/')[0] in {'purpur', 'velocity', 'queue-bridge'} for path in paths):
        raise RuntimeError('Plugin files must not be included in this release source')
    diff = subprocess.check_output([
        'git', 'diff', '--name-only', BUILD_COMMIT, 'HEAD', '--',
        'src', 'modern', 'gradle', 'build.gradle', 'settings.gradle', 'gradle.properties',
        'gradlew', 'gradlew.bat'], cwd=ROOT).decode().strip()
    if diff:
        raise RuntimeError('Client source differs from the tested build: ' + diff)
    out = ROOT / 'release-dist'
    out.mkdir(exist_ok=True)
    jars = []
    for version in VERSIONS:
        name = f'Afri-TiersTagger-2.8.1+mc{version}.jar'
        matches = list((ROOT / 'release-builds').rglob(name))
        if len(matches) != 1:
            raise RuntimeError(f'Expected exactly one tested client artifact: {name}')
        dest = out / name
        shutil.copyfile(matches[0], dest)
        with zipfile.ZipFile(dest) as jar:
            metadata = json.loads(jar.read('fabric.mod.json'))
            if metadata['version'] != f'2.8.1+mc{version}':
                raise RuntimeError('Wrong client version in ' + name)
            if jar.testzip() is not None:
                raise RuntimeError('Corrupt client artifact: ' + name)
        jars.append(dest)
    common = ['RELEASE-NOTES.md', 'CHANGELOG.md', 'LICENSE', 'NOTICE', 'COPYRIGHT']
    for name in ['RELEASE-NOTES.md', 'CHANGELOG.md']:
        shutil.copyfile(ROOT / name, out / name)
    with zipfile.ZipFile(out / 'AfriTiers-2.8.1-client-source.zip', 'w', zipfile.ZIP_DEFLATED) as archive:
        for path in paths:
            archive.write(ROOT / path, path)
    with zipfile.ZipFile(out / 'AfriTiers-2.8.1-Minecraft-1.21.11-to-26.3.zip', 'w', zipfile.ZIP_DEFLATED) as archive:
        for path in jars:
            archive.write(path, path.name)
        for name in common:
            archive.write(ROOT / name, name)
    assets = jars + [out / name for name in [
        'AfriTiers-2.8.1-client-source.zip',
        'AfriTiers-2.8.1-Minecraft-1.21.11-to-26.3.zip', 'RELEASE-NOTES.md', 'CHANGELOG.md']]
    head = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT).decode().strip()
    manifest = {
        'version': '2.8.1', 'minecraftVersions': VERSIONS, 'sourceCommit': head,
        'testedBuildCommit': BUILD_COMMIT, 'testedBuildRun': BUILD_RUN,
        'pluginsIncluded': False,
        'files': [{'file': path.name, 'sha256': digest(path), 'bytes': path.stat().st_size}
                  for path in assets],
    }
    manifest_path = out / 'release-manifest.json'
    manifest_path.write_text(json.dumps(manifest, indent=2) + '\n')
    assets.append(manifest_path)
    checksums = out / 'SHA256SUMS'
    checksums.write_text(''.join(f'{digest(path)}  {path.name}\n' for path in assets))
    return assets + [checksums], head


def publish(assets, head):
    repository = os.environ['GITHUB_REPOSITORY']
    token = os.environ['GH_TOKEN']
    base = f'https://api.github.com/repos/{repository}'

    def request(url, method='GET', data=None, binary=False):
        headers = {'Authorization': 'Bearer ' + token, 'Accept': 'application/vnd.github+json',
                   'X-GitHub-Api-Version': '2022-11-28', 'User-Agent': 'Afri-Tiers-mod-release'}
        if data is not None:
            headers['Content-Type'] = 'application/octet-stream' if binary else 'application/json'
            if not binary:
                data = json.dumps(data).encode()
        with urllib.request.urlopen(urllib.request.Request(url, data=data, headers=headers, method=method), timeout=120) as response:
            return json.load(response)

    build = request(f'{base}/actions/runs/{BUILD_RUN}')
    if build['head_sha'] != BUILD_COMMIT or build['conclusion'] != 'success':
        raise RuntimeError('The pinned client build must be successful')
    body = (ROOT / 'RELEASE-NOTES.md').read_text()
    try:
        release = request(f'{base}/releases/tags/{TAG}')
    except urllib.error.HTTPError as error:
        if error.code != 404:
            raise
        release = request(f'{base}/releases', 'POST', {
            'tag_name': TAG, 'target_commitish': head,
            'name': 'Afri-TiersTagger 2.8.1 | Minecraft 1.21.11 - 26.3',
            'body': body, 'draft': True, 'prerelease': False})
    if not release['draft']:
        raise RuntimeError('Release is already published; refusing to replace its assets')
    if release['target_commitish'] != head:
        raise RuntimeError('Existing draft targets a different commit')
    existing = {entry['name']: entry for entry in request(release['assets_url'])}
    expected = {path.name for path in assets}
    if set(existing) - expected:
        raise RuntimeError('Unexpected files are present in the draft release')
    upload = release['upload_url'].split('{', 1)[0]
    for path in assets:
        if path.name in existing:
            if existing[path.name].get('digest') != 'sha256:' + digest(path):
                raise RuntimeError('Existing draft asset does not match ' + path.name)
            continue
        request(upload + '?' + urllib.parse.urlencode({'name': path.name}), 'POST', path.read_bytes(), True)
    uploaded = request(release['assets_url'])
    if {entry['name'] for entry in uploaded} != expected:
        raise RuntimeError('Uploaded release asset list is incomplete')
    for entry in uploaded:
        path = next(path for path in assets if path.name == entry['name'])
        if entry['size'] != path.stat().st_size or entry.get('digest') != 'sha256:' + digest(path):
            raise RuntimeError('Uploaded asset integrity check failed: ' + path.name)
    released = request(release['url'], 'PATCH', {'draft': False, 'make_latest': 'true'})
    print(released['html_url'])


if __name__ == '__main__':
    publish(*prepare())
