import fs from 'node:fs/promises';
import path from 'node:path';
import {createRequire} from 'node:module';
import {pathToFileURL} from 'node:url';
const workspace=process.cwd();
const build=path.join(workspace,'.presentation-build');
const runtime='C:/Users/markk/.cache/codex-runtimes/codex-primary-runtime/dependencies';
process.env.RUNTIME_NODE_MODULES=runtime+'/node/node_modules';
const require=createRequire(runtime+'/node/node_modules/runtime.cjs');
const {Presentation,PresentationFile}=await import(pathToFileURL(require.resolve('@oai/artifact-tool')).href);
const sharp=require('sharp');
const skill='C:/Users/markk/.codex/plugins/cache/openai-primary-runtime/presentations/26.1105.12137/skills/presentations';
const {applyPresentationChartFont,finalizePresentation}=await import(pathToFileURL(skill+'/container_tools/artifact_tool_utils.mjs').href);
const deck=Presentation.create({slideSize:{width:1280,height:720}});
const C={navy:'#071F3B',blue:'#1769E8',muted:'#507090',pale:'#EEF5FB',gold:'#FFB83D',ink:'#12233F',white:'#FFFFFF'};
function text(s,value,x,y,w,h,size=28,color=C.ink,bold=false){
 const box=s.shapes.add({geometry:'textbox',position:{left:x,top:y,width:w,height:h},fill:'none',line:{fill:'none',width:0}});
 box.text=value;box.text.style={fontFamily:'Arial',fontSize:size,color,bold,verticalAlignment:'top',insets:0,autoFit:'none'};return box;
}
function slide(title,{dark=false,note=''}={}){
 const s=deck.slides.add();s.background.fill=dark?C.navy:C.pale;
 text(s,title,70,55,1140,90,46,dark?C.white:C.ink,true);
 text(s,'CRSM  |  '+String(deck.slides.items.length).padStart(2,'0'),70,674,1135,22,15,dark?'#A4BDDB':C.muted);
 s.speakerNotes.text=note;return s;
}
function pair(s,heading,body,x,y,w,{dark=false}={}){text(s,heading,x,y,w,48,29,dark?C.gold:C.blue,true);text(s,body,x,y+59,w,145,26,dark?'#D9E6F6':C.ink);}
async function image(s,relative,x,y,w,h){const file=path.join(workspace,relative);const bytes=await fs.readFile(file);const meta=await sharp(bytes).metadata();const ratio=Math.min(w/meta.width,h/meta.height);const iw=meta.width*ratio,ih=meta.height*ratio;s.images.add({blob:bytes.buffer.slice(bytes.byteOffset,bytes.byteOffset+bytes.byteLength),contentType:relative.endsWith('.png')?'image/png':'image/jpeg',alt:relative,fit:'contain',position:{left:x+(w-iw)/2,top:y+(h-ih)/2,width:iw,height:ih}});}
function caption(s,value,x=70,y=623,w=1140){text(s,value,x,y,w,36,18,C.muted);}

// 01
{
 const s=deck.slides.add();s.background.fill=C.navy;
 await image(s,'src/crsm/assets/repair-workbench.jpg',720,0,560,720);
 text(s,'CRSM',70,65,570,70,74,C.gold,true);
 text(s,'Computer Repair\nAppointment and\nService Management\nSystem',70,190,630,300,55,C.white,true);
 text(s,'Project overview',73,555,580,50,30,'#D9E6F6');
 text(s,'November 2026',73,625,580,30,20,'#A4BDDB');
 s.speakerNotes.text='Project title and current application entry point: README.md; src/Main.java; src/crsm/fx/FxApplication.java. Cover image: src/crsm/assets/repair-workbench.jpg (supplied project asset). This overview describes source code and prepared SQL; it does not certify a live deployment.';
}
// 02
{
 const s=slide('Project purpose',{note:'Sources: README.md; src/crsm/fx/WorkspaceController.java; src/crsm/CustomerWorkflowService.java. Problems here describe the workflow the project is designed to address, not measured findings from a real repair business. Design image: docs/design/SIGN IN.png; not a current application screenshot.'});
 text(s,'Keep each repair connected\nto the right customer, device\nand payment record.',70,175,560,170,39,C.blue,true);
 text(s,'The system brings appointment intake, technical work, estimate decisions and payment tracking into one desktop application.',70,405,535,150,28);
 await image(s,'docs/design/SIGN IN.png',665,150,540,435);
 caption(s,'Supplied sign-in design reference',665,601,540);
}
// 03
{
 const s=slide('Three account roles',{dark:true,note:'Sources: src/crsm/fx/WorkspaceController.java role/navigation branches; src/crsm/UserSession.java; docs/profile_and_photo_access_migration.sql; docs/availability_and_payment_migration.sql. Technician accounts use the STAFF role. Admin assigns technicians; assigned technicians and Admin may record shop payments and confirm pickup.'});
 pair(s,'Customer','Registers devices, requests repairs, tracks progress, reviews estimates and views receipts.',70,210,330,{dark:true});
 pair(s,'Technician','Handles assigned work, confirms the final cost, records received payments and confirms pickup.',475,210,330,{dark:true});
 pair(s,'Admin','Manages records and accounts, assigns technicians, reviews reports and handles refunds.',880,210,330,{dark:true});
 text(s,'All roles have Profile access. Role and activation controls remain with Admin.',70,560,1100,55,27,'#D9E6F6');
}
// 04
{
 const s=slide('Customer repair intake',{note:'Sources: src/crsm/fx/WorkspaceController.java customerRequest(), customerComputers(), customerAppointments(), myRepairs(); src/crsm/SupabaseStorageService.java validateImage(). Image: docs/design/user/3.jpg. This is a supplied design reference: its 5 MB label differs from the current Java upload limit of 2 MB per PNG/JPEG and up to three files.'});
 pair(s,'Device and problem','Record the device details, reported issue and requested service.',70,180,470);
 pair(s,'Appointment and photos','Book a drop-off and attach up to three readable PNG/JPEG photos, each up to 2 MB.',70,390,470);
 await image(s,'docs/design/user/3.jpg',605,150,590,455);caption(s,'Design reference; current upload limit is 2 MB per photo',605,620,590);
}
// 05
{
 const s=slide('Repair lifecycle',{note:'Sources: docs/customer_workflow_migration.sql estimate and work RPCs; docs/availability_and_payment_migration.sql assignment, finish, payment, pickup RPCs. The sequence summarizes the main successful path, not every permitted transition. Customer estimate decline returns the request for follow-up; parts/testing can add intermediate steps.'});
 const rows=[['01','Request and assignment','Customer submits the repair; Admin assigns an eligible technician.'],['02','Diagnosis and estimate','Technician diagnoses the issue; Customer approves or declines the estimate.'],['03','Technical work','Repair work and parts are recorded; the final bill is confirmed.'],['04','Payment and pickup','Received payments produce receipts; a settled repair can be collected.']];
 rows.forEach((r,i)=>{const y=175+i*109;text(s,r[0],70,y,80,65,43,C.blue,true);text(s,r[1],180,y,1000,40,29,C.ink,true);text(s,r[2],180,y+47,1000,52,25,C.muted);});
}
// 06
{
 const s=slide('Technician availability',{note:'Source: docs/availability_and_payment_migration.sql crsm_technical_status(), guard_crsm_technician_capacity(), list_crsm_technician_availability(). Finishing technical work releases capacity while Awaiting Payment or Ready for Pickup do not consume a technical slot. Existing multiple assignments are not deleted by the migration. Image: docs/design/technician.png is a design reference with illustrative values, not current capacity rules or live counts.'});
 text(s,'Availability combines duty\nstatus and technical workload.',70,170,510,115,35,C.blue,true);
 text(s,'One active technical repair per technician.\n\nBusy, Unavailable and Off Duty block new assignments.\n\nFinished technical work frees the slot before payment or pickup.',70,330,485,250,27);
 await image(s,'docs/design/technician.png',610,180,595,405);caption(s,'Design reference; names and counts are illustrative',610,609,595);
}
// 07
{
 const s=slide('Shop payments and digital receipts',{note:'Sources: docs/availability_and_payment_migration.sql record_crsm_payment(), confirm_crsm_pickup(); src/crsm/PaymentReceipt.java; src/crsm/fx/PaymentDialogs.java. The PHP 500 bill and PHP 200 payment shown here are an illustrative arithmetic example, not business performance data. A later PHP 300 payment settles that example. Customers pay at the shop; assigned Technician or Admin records received funds. Refunds remain Admin-only. The QR is a non-scannable demo visual and transfers no money.'});
 text(s,'PHP 500 final bill\nPHP 200 received\nPHP 300 still due',70,190,530,200,40,C.blue,true);
 text(s,'Each received payment gets a receipt.\nCustomers can view and save it as text.\n\nPickup requires the balance to be settled.',70,420,530,170,27);
 const chart=s.charts.add('bar',{position:{left:670,top:190,width:535,height:330},title:'Illustrative partial payment (PHP)',categories:['Received','Remaining'],series:[{name:'PHP',values:[200,300],fill:C.blue}],hasLegend:false,barOptions:{direction:'column',varyColors:false},titleTextStyle:{fontSize:24,fill:C.ink},dataLabels:{showValue:true,position:'outEnd',textStyle:{fontSize:24,bold:true,fill:C.ink}},xAxis:{textStyle:{fontSize:23,fill:C.muted}},yAxis:{min:0,max:350,majorUnit:100,textStyle:{fontSize:20,fill:C.muted}},chartFill:C.pale});applyPresentationChartFont(chart,{fontFamily:'Arial'});
 caption(s,'Illustrative example | Demo QR does not process real payments',670,563,535);
}
// 08
{
 const s=slide('Profiles and attached photos',{note:'Sources: src/crsm/fx/RepairPhotoViewer.java; src/crsm/SupabaseAuthService.java; docs/profile_and_photo_access_migration.sql; src/crsm/fx/WorkspaceController.java profile(). Repair-photo permissions cover owning Customer, assigned Technician and active Admin. Profile save updates supported identity/contact fields and preserves account links. Email is read only in the current JavaFX screen. Image: docs/design/profile info.png is a supplied design reference, not an exact screenshot of all supported current fields.'});
 pair(s,'Account information','Edit name, username and contact details. Change passwords through the authentication service.',70,180,490);
 pair(s,'Damage-photo viewer','Open stored thumbnails, preview photos and navigate between images. Loading failures can be retried.',70,395,490);
 await image(s,'docs/design/profile info.png',610,155,590,440);caption(s,'Supplied profile design reference',610,615,590);
}
// 09
{
 const s=slide('Dashboard charts and statistics',{note:'Sources: src/crsm/fx/WorkspaceController.java dashboard(); docs/dashboard_statistics_migration.sql. Current native JavaFX charts: repair-status pie chart and six-month net-payment bar chart. Metrics are scoped to owner/assignment/Admin. Payments subtract refunds and group by the original payment month. Image: docs/design/dashboard.png is a design reference. All visible values in that image are illustrative; its extra modules do not imply implementation.'});
 text(s,'Repair status\nNet payment trends\nCurrent balances',70,190,390,190,34,C.blue,true);
 text(s,'Customer: owned repairs\nTechnician: assigned repairs\nAdmin: shop overview',70,438,390,145,26);
 await image(s,'docs/design/dashboard.png',490,145,715,460);caption(s,'Design reference; values illustrative and extra modules may differ',490,620,715);
}
// 10
{
 const s=slide('Application architecture',{dark:true,note:'Sources: src/Main.java; src/crsm/fx/FxApplication.java; src/crsm/fx/WorkspaceController.java; src/crsm/SupabaseClient.java; src/crsm/SupabaseAuthService.java; src/crsm/SupabaseStorageService.java; src/crsm/CustomerWorkflowService.java; src/crsm/RepairRepository.java. Legacy Swing classes remain. JDBC still uses a configured shared database credential; it does not inherit the signed-in Supabase JWT identity.'});
 pair(s,'Presentation','JavaFX controllers, FXML layouts and CSS. The downloaded Figma project supplies visual references.',70,185,1100,{dark:true});
 pair(s,'Application services','Authentication, workflow RPCs, private image storage, receipt formatting and supplier links.',70,355,1100,{dark:true});
 text(s,'Persistence',70,535,1100,40,29,C.gold,true);text(s,'Supabase Auth / Storage / PostgreSQL, plus existing JDBC repository paths.',70,591,1100,50,26,'#D9E6F6');
}
// 11
{
 const s=slide('Core database records',{note:'Sources: docs/supabase_fresh_setup.sql; docs/customer_workflow_migration.sql; docs/repair_request_pricing_migration.sql. This editable table summarizes the seven core business entities; additional tables hold profiles, history, estimates, refunds, catalog and settings. It is not a full ERD.'});
 const values=[['Entity','Primary responsibility','Main relationship'],['Customers','Contact and ownership','Owns computers'],['Computers','Device identifiers and details','Belongs to a customer'],['Appointments','Drop-off schedule','Links customer and computer'],['Repair requests','Issue, status and costs','Links device, technician and service'],['Technicians','Skills, duty and account link','Assigned to repairs'],['Repair services','Service descriptions and fees','Referenced by repairs and part prices'],['Payments','Received amount and receipt','Belongs to a repair request']];
 const table=s.tables.add({rows:8,columns:3,left:70,top:175,width:1140,height:432,columnWidths:[245,405,490],values});
 for(let r=0;r<8;r++)for(let c=0;c<3;c++){const cell=table.getCell(r,c);cell.fill=r===0?C.navy:r%2?C.white:'#E4EEF9';cell.text.style={fontFamily:'Arial',fontSize:23,color:r===0?C.white:C.ink,bold:r===0,verticalAlignment:'middle',insets:10};}
 caption(s,'Profiles, history, estimates, refunds and catalog tables extend these records');
}
// 12
{
 const s=slide('Parts prices and supplier browsing',{note:'Sources: src/crsm/fx/WorkspaceController.java partPrices(); src/crsm/SupplierLinks.java; src/crsm/fx/FxApplication.java openLink(); docs/repair_request_pricing_migration.sql. Parts prices are stored shop prices, not scraped live supplier data. Shopee opens in the external browser on explicit user click. The application does not import supplier photos/prices or purchase parts. Image: docs/design/services.png is a design reference for service browsing, not a screenshot of the new parts browser.'});
 text(s,'Search compatible parts\nby brand, model and specification.',70,175,550,125,37,C.blue,true);
 text(s,'Shop part prices are stored separately from labor.\n\nCheck on Shopee opens the selected part search in the user\'s browser.\n\nCurrent product photos and prices are viewed on Shopee.',70,355,535,235,27);
 await image(s,'docs/design/services.png',670,170,525,405);caption(s,'Service-catalog design reference',670,611,525);
}
// 13
{
 const s=slide('Security boundaries',{note:'Sources: docs/profile_and_photo_access_migration.sql; docs/availability_and_payment_migration.sql; docs/dashboard_statistics_migration.sql; src/crsm/RepairRepository.java; docs/workflow_implementation_notes.md. These are source-level controls requiring deployed migrations. The remaining shared-credential JDBC path is a material deployment limitation, and role-based UI visibility alone is not database authorization.'});
 pair(s,'Account and repair access','Active-account checks, Customer ownership and Technician assignment protect guarded workflow and photo operations.',70,175,1120);
 pair(s,'Restricted management','Admin controls account role/status and refunds. Payment recording is limited to the assigned Technician or Admin.',70,355,1120);
 text(s,'Deployment concern',70,535,1120,40,29,C.blue,true);text(s,'Legacy JDBC uses a shared database credential. Migrate or isolate those paths before distributing the app to untrusted users.',70,587,1120,65,26);
}
// 14
{
 const s=slide('Verification and deployment status',{note:'Sources: build-javafx.ps1; test-workflows.ps1; tests/WorkflowChecks.java; tests/AccountPhotoChecks.java; tests/workflow_regression.sql; docs/javafx-feature-checklist.md. Local service tests were rerun successfully for this inspection. The suites use a localhost HTTP server and do not validate deployed SQL. No app launch or sign-in was performed. Migration application and database regression execution remain unverified.'});
 pair(s,'Automated local checks','Service contracts, validation, payment roles, attachment requests, receipt totals, statistics requests and supplier-link encoding.',70,180,1100);
 pair(s,'Database and sign-in checks','Apply ordered migrations, then verify persistence, role restrictions, concurrent assignment/payment and private storage access.',70,370,1100);
 text(s,'Local checks passed; live database and signed-in UI behavior still require verification.',70,585,1100,65,28,C.blue,true);
}
// 15
{
 const s=slide('Suggested demonstration',{dark:true,note:'Sources: docs/javafx-feature-checklist.md; src/crsm/fx/WorkspaceController.java; src/crsm/fx/PaymentDialogs.java; src/crsm/SupplierLinks.java. Prepare linked test accounts and deployed migrations first. Do not use real customer records or real funds for a classroom demonstration. The presentation uses design reference images rather than captured runtime screens.'});
 text(s,'1  Submit a repair as Customer\n2  Assign and complete technical work\n3  Record a partial payment, then settle the bill\n4  View the receipt and confirm pickup\n5  Review charts and open Shopee search',70,190,1110,330,34,C.white);
 text(s,'Prepare linked test accounts and apply the required migrations before the demonstration.',70,570,1100,70,26,'#D9E6F6');
}

await fs.mkdir(build,{recursive:true});
const draft=path.join(build,'CRSM_Project_Overview.v2.draft.pptx');
await (await PresentationFile.exportPptx(deck)).save(draft);console.log('Draft exported:',deck.slides.items.length,'slides');
for(let i=0;i<deck.slides.items.length;i++){
 const png=await deck.export({slide:deck.slides.items[i],format:'png',scale:1});await fs.writeFile(path.join(build,`slide-${String(i+1).padStart(2,'0')}.png`),new Uint8Array(await png.arrayBuffer()));console.log('Rendered',i+1);
}
const finalPath=path.join(workspace,'documentation/presentations/CRSM_Project_Overview.pptx');
const result=await finalizePresentation({workspaceDir:workspace,candidatePath:draft,finalPath,pythonExecutable:runtime+'/python/python.exe',integrityValidatorPath:skill+'/container_tools/inspect_presentation_package_integrity.py',layoutValidatorPath:skill+'/container_tools/inspect_presentation_layout_geometry.py',layoutArgs:['--expected-slide-size-emu','12192000,6858000','--validate-heading-fit','--require-native-table-slide','11'],explicitTotalSlideCount:15,requiredNativeTableOwnerSlides:[11],requiredNativeChartOwnerSlides:[7],materializeLiteralChartWorkbooks:true,nativeChartTargetApplication:'powerpoint',fontPolicy:{basis:'design',families:['Arial']},verifyArtifactToolImport:true,receiptPath:path.join(build,'validation.json')});
console.log(JSON.stringify(result));
