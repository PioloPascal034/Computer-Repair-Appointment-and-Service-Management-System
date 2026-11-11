import {createRequire} from 'node:module';
import {pathToFileURL} from 'node:url';
const mods='C:/Users/markk/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules';
const req=createRequire(mods+'/runtime.cjs');
console.log(req.resolve('@oai/artifact-tool'));
const canvas=req('@napi-rs/canvas');console.log('Fonts:',canvas.GlobalFonts.families.filter(x=>/Arial|Aptos|Calibri/.test(x.family)).map(x=>x.family));
