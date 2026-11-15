from pathlib import Path
import hashlib,json,shutil,urllib.request
root=Path(__file__).resolve().parents[1]
for item in json.loads((root/'tools/dependencies.json').read_text()):
    target=root/'libs'/item['file'];target.parent.mkdir(exist_ok=True)
    if not target.exists() or hashlib.sha512(target.read_bytes()).hexdigest()!=item['sha512']:
        request=urllib.request.Request(item['url'],headers={'User-Agent':'Afri-TierTagger/2.8.6 build'})
        with urllib.request.urlopen(request,timeout=60) as response:data=response.read(8*1024*1024+1)
        if len(data)>8*1024*1024 or hashlib.sha512(data).hexdigest()!=item['sha512']:raise RuntimeError('Dependency hash mismatch: '+item['file'])
        target.write_bytes(data)
    destination=root/'modern/libs'/item['file'];destination.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(target,destination)
print('Verified pinned WalksyLib and compile-only Mod Menu dependencies.')
