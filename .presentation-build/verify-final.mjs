import fs from 'node:fs/promises';
import {createRequire} from 'node:module';
import {pathToFileURL} from 'node:url';
import {createHash} from 'node:crypto';
const require=createRequire('C:/Users/markk/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/runtime.cjs');
const {PresentationFile,FileBlob}=await import(pathToFileURL(require.resolve('@oai/artifact-tool')).href);
const final='documentation/presentations/CRSM_Project_Overview.pptx';
const p=await PresentationFile.importPptx(await FileBlob.load(final));
for(let i=0;i<p.slides.items.length;i++){
 const blob=await p.export({slide:p.slides.items[i],format:'png',scale:1});
 const bytes=new Uint8Array(await blob.arrayBuffer());
 await fs.writeFile(`.presentation-build/final-slide-${String(i+1).padStart(2,'0')}.png`,bytes);
}
console.log('Final file re-imported and rendered:',p.slides.items.length,'slides');
console.log('PPTX bytes:',(await fs.stat(final)).size);
process.exit(0);
