from pathlib import Path
import hashlib,json,sys,zipfile
root=Path(__file__).resolve().parent.parent
output=Path(sys.argv[1]) if len(sys.argv)>1 else root/'AfriTiers-server-icons-mc1.21.11.zip'
assets=root/'src/main/resources/assets'
font=assets/'minecraft/font/default.json'
providers=json.loads(font.read_text(encoding='utf-8'))['providers']
with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED) as pack:
    pack.writestr('pack.mcmeta',json.dumps({'pack':{'pack_format':75,'min_format':[75,0],'max_format':[75,0],'description':'Afri-Tiers gamemode and rank trophy icons for Minecraft 1.21.11'}}))
    pack.write(font,'assets/minecraft/font/default.json')
    for provider in providers:
        namespace,name=provider['file'].split(':',1)
        file=assets/namespace/'textures'/name
        pack.write(file,'assets/'+namespace+'/textures/'+name)
output.with_suffix('.sha1').write_text(hashlib.sha1(output.read_bytes()).hexdigest()+'  '+output.name+'\n')
print(output.name+' '+hashlib.sha1(output.read_bytes()).hexdigest())
