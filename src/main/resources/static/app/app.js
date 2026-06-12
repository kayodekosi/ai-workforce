// ===== AI-Workforce admin SPA =====
let TOKEN = null, ROLE = null, USER = null;
let STAFF = [], DEPTS = [], LEVELS = [], CHANNELS = [], CURRENT_CHANNEL = null;
const COLORS = ['#6ea8fe','#a78bfa','#f472b6','#34d399','#fbbf24','#fb923c'];

const api = async (path, opts={}) => {
  const headers = {'Content-Type':'application/json', ...(opts.headers||{})};
  if (TOKEN) headers['Authorization'] = 'Bearer ' + TOKEN;
  const res = await fetch('/api' + path, {...opts, headers});
  if (res.status === 204) return null;
  const text = await res.text();
  let body; try { body = text ? JSON.parse(text) : null; } catch { body = text; }
  if (!res.ok) throw new Error((body && body.message) || ('HTTP ' + res.status));
  return body;
};
const initials = (name) => (name||'?').split(/[\s(]/).filter(Boolean).slice(0,2).map(w=>w[0]).join('').toUpperCase();
const colorFor = (id) => COLORS[(id||0) % COLORS.length];
const el = (id) => document.getElementById(id);

// ---------- AUTH ----------
async function doLogin(){
  const msg = el('loginMsg'); msg.innerHTML='';
  try{
    const r = await api('/auth/login', {method:'POST', body:JSON.stringify({username:el('lUser').value, password:el('lPass').value})});
    if(!r.ok){ msg.innerHTML = `<div class="msg err">${r.message}</div>`; return; }
    USER = el('lUser').value;
    if(r.mustChangePassword){
      el('loginForm').classList.add('hidden');
      el('changeForm').classList.remove('hidden');
      msg.innerHTML = `<div class="msg ok">${r.message}</div>`;
      return;
    }
    TOKEN = r.token; ROLE = r.role; enterApp();
  }catch(e){ msg.innerHTML = `<div class="msg err">${e.message}</div>`; }
}

async function doChange(){
  const msg = el('loginMsg');
  const a = el('cNew').value, b = el('cNew2').value;
  if(a!==b){ msg.innerHTML = `<div class="msg err">Passwords don't match.</div>`; return; }
  try{
    const r = await api('/auth/change-password', {method:'POST',
      body:JSON.stringify({username:USER, oldPassword:el('lPass').value, newPassword:a})});
    if(!r.ok){ msg.innerHTML = `<div class="msg err">${r.message}</div>`; return; }
    TOKEN = r.token; ROLE = r.role; enterApp();
  }catch(e){ msg.innerHTML = `<div class="msg err">${e.message}</div>`; }
}

function logout(){
  TOKEN=null; ROLE=null; USER=null;
  try{ if(SUB) SUB.unsubscribe(); }catch(_){}
  try{ if(STOMP){ STOMP.deactivate ? STOMP.deactivate() : STOMP.disconnect(); } }catch(_){}
  location.reload();
}

function enterApp(){
  el('login').classList.add('hidden');
  el('shell').classList.remove('hidden');
  el('whoami').textContent = USER;
  el('sideRole').textContent = ROLE + ' PORTAL';
  applyBranding();
  connectWs();
  loadAll();
}

// ---------- BRANDING / THEME ----------
const ACCENTS = ['blue','teal','green','purple','pink','amber','red'];
const ACCENT_HEX = {blue:'#3b82f6',teal:'#0ea5e9',green:'#10b981',purple:'#8b5cf6',pink:'#ec4899',amber:'#f59e0b',red:'#ef4444'};
let BRANDING = {portalName:'AI-Workforce',logoUrl:'',theme:'midnight',accent:'blue'};

// Apply branding at page load (before login) — the GET is public.
async function brandLoginPage(){
  try{
    const res = await fetch('/api/portal/settings');
    if(!res.ok) return;
    const s = await res.json();
    BRANDING = s;
    applyThemeAttrs(s.theme, s.accent);
    const lt = document.getElementById('loginTitle');
    if(lt) lt.textContent = (s.logoUrl ? '' : '🤖 ') + (s.portalName||'AI-Workforce');
    if(s.logoUrl && lt){
      let img = document.getElementById('loginLogo');
      if(!img){ img=document.createElement('img'); img.id='loginLogo'; img.style.cssText='max-height:46px;margin-bottom:10px;display:block;'; lt.parentNode.insertBefore(img, lt); }
      img.src = s.logoUrl;
    }
  }catch(_){}
}
brandLoginPage();
function applyThemeAttrs(theme, accent){
  const root = document.documentElement;
  if(theme && theme!=='midnight') root.setAttribute('data-theme',theme); else root.removeAttribute('data-theme');
  if(accent) root.setAttribute('data-accent',accent); else root.removeAttribute('data-accent');
}
async function applyBranding(){
  try{
    const s = await api('/portal/settings');
    BRANDING = s;
    applyThemeAttrs(s.theme, s.accent);
    // apply portal name + logo to sidebar
    const h2 = document.querySelector('.side h2');
    if(h2) h2.textContent = s.portalName || 'AI-Workforce';
    if(s.logoUrl){
      let img = document.getElementById('sideLogo');
      if(!img){ img=document.createElement('img'); img.id='sideLogo'; img.style.cssText='max-height:30px;margin-bottom:8px;display:block;'; h2.parentNode.insertBefore(img,h2); }
      img.src = s.logoUrl;
    }
  }catch(_){}
}

// ---------- WEBSOCKET (real-time chat) ----------
let STOMP = null, SUB = null, WS_READY = false;
function connectWs(){
  try{
    const sock = new SockJS('/ws');
    STOMP = window.StompJs ? new window.StompJs.Client({webSocketFactory:()=>sock})
                           : Stomp.over(sock);
    if(STOMP.activate){ // stompjs v7 Client API
      STOMP.onConnect = ()=>{ WS_READY=true; if(CURRENT_CHANNEL) subscribeChannel(CURRENT_CHANNEL.id); };
      STOMP.activate();
    } else { // fallback older API
      STOMP.connect({}, ()=>{ WS_READY=true; if(CURRENT_CHANNEL) subscribeChannel(CURRENT_CHANNEL.id); });
    }
  }catch(e){ console.warn('WebSocket unavailable, falling back to fetch:', e); }
}
function subscribeChannel(id){
  if(!STOMP || !WS_READY) return;
  if(SUB){ try{ SUB.unsubscribe(); }catch(_){} SUB=null; }
  const topic = '/topic/channels/'+id;
  const handler = (frame)=>{ try{ appendLiveMessage(JSON.parse(frame.body)); }catch(_){} };
  SUB = STOMP.subscribe ? STOMP.subscribe(topic, handler)
                        : STOMP.subscribe(topic, handler);
}
function appendLiveMessage(m){
  if(!CURRENT_CHANNEL) return;
  const wrap = el('chatMessages');
  const mine = m.sender && USER && m.sender.fullName===USER;
  const fn = m.sender ? m.sender.fullName : '?';
  const role = m.sender && m.sender.type==='AI' ? `<small>AI · ${m.sender.function||m.sender.position||''}</small>` : '';
  const div = document.createElement('div');
  div.className = 'bubble' + (mine?' me':'');
  div.innerHTML = `<div class="who">${fn} ${role} ${m.voiceClipUrl?'🔊':''}</div>
    <div class="txt">${(m.content||'').replace(/</g,'&lt;')}</div>`;
  // avoid duplicating the just-sent echo if already present by id
  if(m.id && wrap.querySelector(`[data-mid="${m.id}"]`)) return;
  div.setAttribute('data-mid', m.id||'');
  wrap.appendChild(div);
  wrap.scrollTop = wrap.scrollHeight;
}

// ---------- NAV ----------
function nav(view){
  ['staff','company','organogram','hr','chat','calendar','email','audit','connectors','settings'].forEach(v=>{
    el('view-'+v).classList.toggle('hidden', v!==view);
  });
  document.querySelectorAll('.nav').forEach(n=>n.classList.toggle('active', n.dataset.view===view));
  if(view==='chat') loadChannels();
  if(view==='calendar') loadMeetings();
  if(view==='email') loadEmails();
  if(view==='audit') loadAudit();
  if(view==='connectors') loadConnectors();
  if(view==='settings') loadSettings();
  if(view==='organogram') renderOrganogram();
  if(view==='hr') loadCandidates();
}

async function loadAll(){ await Promise.all([loadStaff(), loadCompany()]); }

// ---------- STAFF ----------
async function loadStaff(){
  STAFF = await api('/staff');
  const ai = STAFF.filter(s=>s.type==='AI').length;
  el('staffSub').textContent = `${STAFF.length} staff · ${ai} AI · ${STAFF.length-ai} human`;
  el('staffRows').innerHTML = STAFF.map(s=>{
    const st = s.status==='ACTIVE' ? ['#34d399','Online'] :
               s.status==='ON_LEAVE' ? ['#fbbf24','On Leave'] :
               s.status==='OFFBOARDED' ? ['#64748b','Offboarded'] : ['#64748b', s.status];
    return `<tr>
      <td><span class="avatar" style="background:${colorFor(s.id)}">${initials(s.fullName)}</span>${s.fullName||''}<br>
          <small style="color:var(--muted2);margin-left:39px;">${s.email||''}</small></td>
      <td>${s.position||'—'}<br><small style="color:var(--muted2)">${(s.department&&s.department.name)||''}</small></td>
      <td><span class="pill ${s.type==='AI'?'ai':'human'}">${s.type}</span></td>
      <td>${s.phoneExtension||'—'}</td>
      <td><span class="status"><span class="dot" style="background:${st[0]}"></span>${st[1]}</span></td>
      <td style="text-align:right;">
        ${s.status==='ON_LEAVE'
          ? `<button class="btn ghost sm" onclick="staffAction(${s.id},'activate')">Reactivate</button>`
          : `<button class="btn ghost sm" onclick="staffAction(${s.id},'leave')">Leave</button>`}
      </td></tr>`;
  }).join('');
}

async function staffAction(id, action){
  await api(`/staff/${id}/${action}`, {method:'POST'});
  loadStaff();
}

function openStaffModal(){
  el('sDept').innerHTML = '<option value="">—</option>' + DEPTS.map(d=>`<option value="${d.id}">${d.name}</option>`).join('');
  ['sName','sEmail','sPosition','sFunction','sExt','sDuties','sPrompt','sModel','sEndpoint'].forEach(i=>el(i).value='');
  el('staffModal').classList.remove('hidden');
}
function closeModal(id){ el(id).classList.add('hidden'); }

async function saveStaff(){
  const body = {
    fullName:el('sName').value, email:el('sEmail').value, position:el('sPosition').value,
    function:el('sFunction').value, phoneExtension:el('sExt').value, duties:el('sDuties').value,
    type:el('sType').value, systemPrompt:el('sPrompt').value, model:el('sModel').value,
    connector:el('sConnector').value, connectorEndpoint:el('sEndpoint').value,
    voiceEnabled:el('sVoice').value==='true'
  };
  const deptId = el('sDept').value;
  if(deptId) body.department = DEPTS.find(d=>d.id==deptId);
  try{
    await api('/staff', {method:'POST', body:JSON.stringify(body)});
    closeModal('staffModal'); loadStaff();
  }catch(e){ alert('Save failed: ' + e.message + (ROLE!=='ADMIN' ? ' (only ADMIN can add staff)' : '')); }
}

// ---------- COMPANY ----------
async function loadCompany(){
  const list = await api('/company');
  const c = (list && list[0]) || {};
  el('companyCard').innerHTML = `
    <div class="grid2">
      <div class="field"><label>Company name</label><input id="cName" value="${c.name||''}"></div>
      <div class="field"><label>Industry</label><input id="cIndustry" value="${c.industry||''}"></div>
    </div>
    <div class="field"><label>Description</label><input id="cDesc" value="${c.description||''}"></div>
    <div class="grid2">
      <div class="field"><label>URL</label><input id="cUrl" value="${c.url||''}"></div>
      <div class="field"><label>Address</label><input id="cAddr" value="${c.address||''}"></div>
    </div>
    <button class="btn" onclick="saveCompany(${c.id||0})">Save company</button>`;
  DEPTS = await api('/company/departments');
  LEVELS = await api('/company/levels');
  el('deptList').innerHTML = DEPTS.map(d=>`<div style="padding:7px 0;border-top:1px solid var(--line);">${d.name}</div>`).join('')
    + `<div style="margin-top:10px;display:flex;gap:8px;"><input id="newDept" placeholder="New department"><button class="btn sm" onclick="addDept()">Add</button></div>`;
  el('levelList').innerHTML = LEVELS.sort((a,b)=>a.rank-b.rank).map(l=>`<div style="padding:7px 0;border-top:1px solid var(--line);">${l.name} <small style="color:var(--muted2)">rank ${l.rank}</small></div>`).join('')
    + `<div style="margin-top:10px;display:flex;gap:8px;"><input id="newLevel" placeholder="New level"><input id="newRank" type="number" placeholder="rank" style="width:80px;"><button class="btn sm" onclick="addLevel()">Add</button></div>`;
}
async function saveCompany(id){
  const body={name:el('cName').value,industry:el('cIndustry').value,description:el('cDesc').value,url:el('cUrl').value,address:el('cAddr').value};
  try{ await api('/company/'+id,{method:'PUT',body:JSON.stringify(body)}); alert('Company saved.'); }
  catch(e){ alert('Save failed: '+e.message+(ROLE!=='ADMIN'?' (ADMIN only)':'')); }
}
async function addDept(){
  try{ await api('/company/departments',{method:'POST',body:JSON.stringify({name:el('newDept').value})}); loadCompany(); }
  catch(e){ alert('Failed: '+e.message); }
}
async function addLevel(){
  try{ await api('/company/levels',{method:'POST',body:JSON.stringify({name:el('newLevel').value,rank:parseInt(el('newRank').value)||1})}); loadCompany(); }
  catch(e){ alert('Failed: '+e.message); }
}

// ---------- CHAT ----------
async function loadChannels(){
  CHANNELS = await api('/chat/channels');
  el('channelList').innerHTML = CHANNELS.length ? CHANNELS.map(c=>
    `<button class="nav ${CURRENT_CHANNEL&&CURRENT_CHANNEL.id===c.id?'active':''}" onclick="openChannel(${c.id})">${c.type==='CONFERENCE'?'📣':'#'} ${c.name}</button>`
  ).join('') : `<div style="color:var(--muted2);font-size:12.5px;">No channels yet. Create one →</div>`;
}
function openChannelModal(){
  el('chMembers').innerHTML = STAFF.map(s=>
    `<label style="text-transform:none;display:flex;align-items:center;gap:8px;margin-bottom:6px;color:var(--ink);font-size:13px;">
      <input type="checkbox" value="${s.id}" style="width:auto;"> ${s.fullName} <span class="pill ${s.type==='AI'?'ai':'human'}">${s.type}</span></label>`
  ).join('');
  el('channelModal').classList.remove('hidden');
}
async function saveChannel(){
  const ids = [...document.querySelectorAll('#chMembers input:checked')].map(c=>parseInt(c.value));
  try{
    const ch = await api('/chat/channels', {method:'POST',
      body:JSON.stringify({name:el('chName').value, type:el('chType').value, memberIds:ids})});
    CHANNELS.push(ch); closeModal('channelModal'); loadChannels(); openChannel(ch.id);
  }catch(e){ alert('Failed: '+e.message); }
}
async function openChannel(id){
  CURRENT_CHANNEL = CHANNELS.find(c=>c.id===id);
  loadChannels();
  el('composer').style.display='flex';
  const msgs = await api(`/chat/channels/${id}/messages`);
  renderMessages(msgs);
  subscribeChannel(id);
}
function renderMessages(msgs){
  el('chatMessages').innerHTML = msgs.map(m=>{
    const mine = m.sender && USER && m.sender.fullName===USER;
    const fn = m.sender ? m.sender.fullName : '?';
    const role = m.sender && m.sender.type==='AI' ? `<small>AI · ${m.sender.function||m.sender.position||''}</small>` : '';
    return `<div class="bubble ${mine?'me':''}" data-mid="${m.id||''}">
      <div class="who">${fn} ${role} ${m.voiceClipUrl?'🔊':''}</div>
      <div class="txt">${(m.content||'').replace(/</g,'&lt;')}</div></div>`;
  }).join('') || `<div style="color:var(--muted2)">No messages yet — say hello below.</div>`;
  el('chatMessages').scrollTop = el('chatMessages').scrollHeight;
}
async function sendMsg(){
  if(!CURRENT_CHANNEL) return;
  const input = el('msgInput'); const content = input.value.trim();
  if(!content) return;
  const senderId = (CURRENT_CHANNEL.members && CURRENT_CHANNEL.members[0] && CURRENT_CHANNEL.members[0].id) || (STAFF[0]&&STAFF[0].id);
  input.value='';
  try{
    // Messages arrive via the WebSocket broadcast; if WS is down, fall back to refetch.
    await api(`/chat/channels/${CURRENT_CHANNEL.id}/messages`, {method:'POST',
      body:JSON.stringify({senderId, content})});
    if(!WS_READY){
      const msgs = await api(`/chat/channels/${CURRENT_CHANNEL.id}/messages`);
      renderMessages(msgs);
    }
  }catch(e){ alert('Send failed: '+e.message); }
}

// AI fields toggle
document.addEventListener('change', e=>{
  if(e.target.id==='sType'){
    el('aiFields').style.display = e.target.value==='AI' ? 'block' : 'none';
  }
});

// ---------- AUDIT ----------
async function loadAudit(){
  const list = await api('/audit?limit=200');
  el('auditRows').innerHTML = list.length ? list.map(e=>{
    const when = e.at ? new Date(e.at).toLocaleString(undefined,{month:'short',day:'numeric',hour:'2-digit',minute:'2-digit',second:'2-digit'}) : '';
    return `<tr>
      <td style="color:var(--muted2);font-size:12px;white-space:nowrap;">${when}</td>
      <td>${e.actor||'system'}</td>
      <td><span class="pill ai">${e.action||''}</span></td>
      <td style="color:var(--ink);font-size:13px;">${(e.detail||'').replace(/</g,'&lt;')}</td>
    </tr>`;
  }).join('') : `<tr><td colspan="4" style="color:var(--muted2)">No activity recorded yet.</td></tr>`;
}

// ---------- STAFF EXCEL IMPORT ----------
function downloadSample(){
  // fetch with auth then trigger a download (the endpoint requires a token)
  fetch('/api/staff-import/sample', {headers:{'Authorization':'Bearer '+TOKEN}})
    .then(r=>r.blob()).then(b=>{
      const url=URL.createObjectURL(b); const a=document.createElement('a');
      a.href=url; a.download='staff-import-template.xlsx'; a.click(); URL.revokeObjectURL(url);
    }).catch(e=>alert('Download failed: '+e.message));
}
async function uploadStaff(input){
  const file = input.files[0]; if(!file) return;
  const fd = new FormData(); fd.append('file', file);
  el('importMsg').innerHTML = `<div class="msg" style="background:#11203b;color:var(--muted2)">Importing…</div>`;
  try{
    const res = await fetch('/api/staff-import', {method:'POST',
      headers:{'Authorization':'Bearer '+TOKEN}, body:fd});
    const r = await res.json();
    const errs = (r.errors&&r.errors.length) ? ` (${r.errors.length} skipped: ${r.errors.slice(0,3).join('; ')}${r.errors.length>3?'…':''})` : '';
    el('importMsg').innerHTML = `<div class="msg ok">Imported ${r.created||0} staff${errs}</div>`;
    loadStaff();
  }catch(e){ el('importMsg').innerHTML = `<div class="msg err">${e.message}</div>`; }
  input.value='';
}

// ---------- EMAIL ----------
const EMAIL_STATUS_COL = {PENDING:'#fbbf24',SENT:'#34d399',FAILED:'#ef4444'};
async function loadEmails(){
  const list = await api('/emails');
  // infer mode from whether anything is SENT vs PENDING (display hint only)
  const anySent = list.some(e=>e.status==='SENT');
  el('mailMode').textContent = anySent ? 'SMTP ON' : 'OUTBOX (preview)';
  el('mailMode').style.background = anySent ? '#11314a' : '#3a3320';
  el('mailMode').style.color = anySent ? '#34d399' : '#fbbf24';
  el('emailRows').innerHTML = list.length ? list.map(e=>{
    const col = EMAIL_STATUS_COL[e.status]||'#64748b';
    const when = e.createdAt ? new Date(e.createdAt).toLocaleString(undefined,{month:'short',day:'numeric',hour:'2-digit',minute:'2-digit'}) : '';
    return `<tr>
      <td>${(e.toAddress||'—')}</td>
      <td><b style="color:#fff">${(e.subject||'(no subject)').replace(/</g,'&lt;')}</b></td>
      <td><span class="pill ai">${e.trigger||'manual'}</span></td>
      <td><span class="pill" style="background:${col}22;color:${col}">${e.status}</span></td>
      <td style="color:var(--muted2);font-size:12px;">${when}</td>
    </tr>`;
  }).join('') : `<tr><td colspan="5" style="color:var(--muted2)">Outbox is empty. Compose one, or trigger events in HR/Calendar.</td></tr>`;
}
function openComposeModal(){
  el('composeMsg').innerHTML='';
  ['emTo','emSubject','emBody'].forEach(i=>el(i).value='');
  el('composeModal').classList.remove('hidden');
}
async function sendEmail(){
  const body={to:el('emTo').value,subject:el('emSubject').value,body:el('emBody').value};
  if(!body.to){ el('composeMsg').innerHTML=`<div class="msg err">Recipient required.</div>`; return; }
  try{
    await api('/emails',{method:'POST',body:JSON.stringify(body)});
    closeModal('composeModal'); loadEmails();
  }catch(e){ el('composeMsg').innerHTML=`<div class="msg err">${e.message}</div>`; }
}

// ---------- HR ----------
const STATUS_COLORS = {APPLIED:'#64748b',SCREENING:'#6ea8fe',AI_INTERVIEW:'#a78bfa',
  SHORTLISTED:'#34d399',OFFER:'#fbbf24',HIRED:'#22c55e',REJECTED:'#ef4444'};
async function loadCandidates(){
  const list = await api('/hr/candidates');
  el('candidateRows').innerHTML = list.length ? list.map(c=>{
    const col = STATUS_COLORS[c.status]||'#64748b';
    return `<tr>
      <td><b style="color:#fff">${(c.fullName||'').replace(/</g,'&lt;')}</b><br><small style="color:var(--muted2)">${c.email||''}</small></td>
      <td>${c.roleAppliedFor||'—'}</td>
      <td><span class="pill" style="background:${col}22;color:${col}">${c.status}</span></td>
      <td>${c.grade!=null?`<b style="color:${c.grade>=70?'#34d399':'#fbbf24'}">${c.grade}</b>/100`:'—'}</td>
      <td style="text-align:right;white-space:nowrap;">
        <button class="btn ghost sm" onclick="openInterview(${c.id})">🎤 Interview</button>
        <button class="btn ghost sm" onclick="aiScreen(${c.id})">🤖 AI Screen</button>
        <select class="sm" style="width:auto;display:inline-block;padding:5px;" onchange="setCandStatus(${c.id},this.value)">
          ${Object.keys(STATUS_COLORS).map(s=>`<option ${s===c.status?'selected':''}>${s}</option>`).join('')}
        </select>
        <button class="btn ghost sm" onclick="removeCandidate(${c.id})">✕</button>
      </td></tr>`;
  }).join('') : `<tr><td colspan="5" style="color:var(--muted2)">No candidates yet. Add one to start the pipeline.</td></tr>`;
}
function openCandidateModal(){
  ['cdName','cdEmail','cdRole'].forEach(i=>el(i).value='');
  el('candidateModal').classList.remove('hidden');
}
async function saveCandidate(){
  const body={fullName:el('cdName').value,email:el('cdEmail').value,roleAppliedFor:el('cdRole').value};
  if(!body.fullName){ alert('Name required'); return; }
  try{ await api('/hr/candidates',{method:'POST',body:JSON.stringify(body)}); closeModal('candidateModal'); loadCandidates(); }
  catch(e){ alert('Failed: '+e.message); }
}
async function aiScreen(id){
  try{ await api(`/hr/candidates/${id}/ai-screen`,{method:'POST'}); loadCandidates(); }
  catch(e){ alert('Failed: '+e.message); }
}
async function setCandStatus(id,status){
  try{ await api(`/hr/candidates/${id}/status`,{method:'POST',body:JSON.stringify({status})}); loadCandidates(); }
  catch(e){ alert('Failed: '+e.message); }
}
async function removeCandidate(id){
  try{ await api(`/hr/candidates/${id}`,{method:'DELETE'}); loadCandidates(); }
  catch(e){ alert('Failed: '+e.message); }
}

// ---------- AI INTERVIEW ----------
let IV = null; // current interview
async function openInterview(candidateId){
  el('ivMsg').innerHTML=''; el('ivBody').innerHTML='Loading…';
  el('interviewModal').classList.remove('hidden');
  try{
    IV = await api('/hr/interviews/latest/'+candidateId);
  }catch(_){ IV = null; }
  if(!IV){
    el('ivBody').innerHTML = `<div style="color:var(--muted);font-size:13px;margin:14px 0;">
      No interview yet for this candidate. Generate one to begin.</div>`;
    el('ivActions').innerHTML = `<button class="btn ghost" onclick="closeModal('interviewModal')">Close</button>
      <button class="btn" onclick="createInterview(${candidateId})">Generate Interview</button>`;
  } else {
    renderInterview();
  }
}
async function createInterview(candidateId){
  try{
    IV = await api('/hr/interviews/create',{method:'POST',body:JSON.stringify({candidateId})});
    renderInterview();
  }catch(e){ el('ivMsg').innerHTML=`<div class="msg err">${e.message}</div>`; }
}
function renderInterview(){
  if(!IV || !Array.isArray(IV.questions)){
    el('ivBody').innerHTML = `<div class="msg err">Interview data didn't load correctly. Try regenerating.</div>`;
    el('ivActions').innerHTML = `<button class="btn ghost" onclick="closeModal('interviewModal')">Close</button>`;
    return;
  }
  const scored = IV.status==='SCORED';
  el('ivSub').textContent = scored
    ? `Scored via ${IV.connectorUsed||'simulation'} — overall ${IV.overallScore}/100`
    : 'Answer the questions, then score the interview';
  el('ivBody').innerHTML = `
    ${scored && IV.summary ? `<div class="msg ok">${IV.summary}</div>`:''}
    ${IV.questions.map((q,i)=>`
      <div class="card" style="margin-bottom:12px;">
        <div style="color:#fff;font-size:13.5px;margin-bottom:8px;">Q${i+1}. ${q.question}</div>
        ${scored
          ? `<div style="font-size:13px;color:var(--ink);background:#0f1c33;border-radius:7px;padding:9px 11px;margin-bottom:8px;">${(q.answer||'(no answer)').replace(/</g,'&lt;')}</div>
             <div style="font-size:12.5px;color:var(--muted);">Score: <b style="color:${q.score>=70?'#34d399':'#fbbf24'}">${q.score}</b>/100 — ${q.feedback||''}</div>`
          : `<textarea id="ivA${i}" rows="2" placeholder="Candidate's answer…">${q.answer||''}</textarea>`}
      </div>`).join('')}`;
  if(scored){
    el('ivActions').innerHTML = `<button class="btn ghost" onclick="closeModal('interviewModal');loadCandidates();">Close</button>`;
  } else {
    el('ivActions').innerHTML = `<button class="btn ghost" onclick="closeModal('interviewModal')">Cancel</button>
      <button class="btn ghost" onclick="saveAnswers()">Save Answers</button>
      <button class="btn" onclick="scoreInterview()">🤖 Score Interview</button>`;
  }
}
function collectAnswers(){
  return IV.questions.map((q,i)=>{ const t=el('ivA'+i); return t?t.value:''; });
}
async function saveAnswers(){
  try{
    IV = await api(`/hr/interviews/${IV.id}/answers`,{method:'POST',body:JSON.stringify({answers:collectAnswers()})});
    el('ivMsg').innerHTML = `<div class="msg ok">Answers saved.</div>`;
  }catch(e){ el('ivMsg').innerHTML=`<div class="msg err">${e.message}</div>`; }
}
async function scoreInterview(){
  try{
    await api(`/hr/interviews/${IV.id}/answers`,{method:'POST',body:JSON.stringify({answers:collectAnswers()})});
    IV = await api(`/hr/interviews/${IV.id}/score`,{method:'POST'});
    renderInterview();
  }catch(e){ el('ivMsg').innerHTML=`<div class="msg err">${e.message}</div>`; }
}

// ---------- ORGANOGRAM ----------
function renderOrganogram(){
  // build a tree from STAFF using reportsTo
  const byId = {}; STAFF.forEach(s=>byId[s.id]={...s, children:[]});
  const roots = [];
  STAFF.forEach(s=>{
    const node = byId[s.id];
    const parentId = s.reportsTo && s.reportsTo.id;
    if(parentId && byId[parentId]) byId[parentId].children.push(node);
    else roots.push(node);
  });
  const card = (n)=>{
    const ai = n.type==='AI';
    return `<div class="org-node">
      <div class="org-card">
        <span class="avatar" style="background:${colorFor(n.id)};width:26px;height:26px;font-size:11px;">${initials(n.fullName)}</span>
        <div><div style="color:#fff;font-size:13px;font-weight:600;">${n.fullName||''}</div>
        <div style="color:var(--muted2);font-size:11px;">${n.position||''} ${ai?'· <span style="color:var(--accent2)">AI</span>':'· <span style="color:var(--warn)">Human</span>'}</div></div>
      </div>
      ${n.children.length ? `<div class="org-children">${n.children.map(card).join('')}</div>` : ''}
    </div>`;
  };
  el('orgTree').innerHTML = roots.length
    ? `<div class="org-root">${roots.map(card).join('')}</div>`
    : `<div style="color:var(--muted2)">No staff yet.</div>`;
}

// ---------- CALENDAR ----------
const fmtWhen = (s,e)=>{
  if(!s) return '—';
  const d = new Date(s); const opts={month:'short',day:'numeric',hour:'2-digit',minute:'2-digit'};
  let out = d.toLocaleString(undefined,opts);
  if(e){ const ed=new Date(e); out += ' – ' + ed.toLocaleString(undefined,{hour:'2-digit',minute:'2-digit'}); }
  return out;
};
async function loadMeetings(){
  const list = await api('/meetings');
  el('meetingRows').innerHTML = list.length ? list.map(m=>`
    <tr>
      <td><b style="color:#fff">${(m.title||'(untitled)').replace(/</g,'&lt;')}</b>
          ${m.organiser?`<br><small style="color:var(--muted2)">by ${m.organiser.fullName}</small>`:''}</td>
      <td>${fmtWhen(m.startTime,m.endTime)}</td>
      <td>${(m.location||'—')}</td>
      <td>${(m.attendees||[]).map(a=>a.fullName).join(', ')||'—'}</td>
      <td style="text-align:right;"><button class="btn ghost sm" onclick="cancelMeeting(${m.id})">Cancel</button></td>
    </tr>`).join('') : `<tr><td colspan="5" style="color:var(--muted2)">No meetings scheduled.</td></tr>`;
}
function openMeetingModal(){
  el('meetingMsg').innerHTML='';
  ['mTitle','mStart','mEnd','mLoc'].forEach(i=>el(i).value='');
  el('mOrganiser').innerHTML = '<option value="">—</option>' + STAFF.map(s=>`<option value="${s.id}">${s.fullName}</option>`).join('');
  el('mAttendees').innerHTML = STAFF.map(s=>
    `<label style="text-transform:none;display:flex;align-items:center;gap:8px;margin-bottom:6px;color:var(--ink);font-size:13px;">
      <input type="checkbox" value="${s.id}" style="width:auto;"> ${s.fullName} <span class="pill ${s.type==='AI'?'ai':'human'}">${s.type}</span></label>`
  ).join('');
  el('meetingModal').classList.remove('hidden');
}
async function saveMeeting(){
  const msg = el('meetingMsg');
  const toIso = v => v ? new Date(v).toISOString() : null;
  const ids = [...document.querySelectorAll('#mAttendees input:checked')].map(c=>parseInt(c.value));
  const body = {
    title:el('mTitle').value, description:'', location:el('mLoc').value,
    startTime:toIso(el('mStart').value), endTime:toIso(el('mEnd').value),
    organiserId: el('mOrganiser').value ? parseInt(el('mOrganiser').value) : null,
    attendeeIds: ids
  };
  if(!body.title || !body.startTime || !body.endTime){
    msg.innerHTML = `<div class="msg err">Title, start and end are required.</div>`; return;
  }
  try{
    await api('/meetings', {method:'POST', body:JSON.stringify(body)});
    closeModal('meetingModal'); loadMeetings();
  }catch(e){
    // api() throws on non-2xx; surface conflict messages clearly
    msg.innerHTML = `<div class="msg err">${e.message}</div>`;
  }
}
async function cancelMeeting(id){
  await api('/meetings/'+id, {method:'DELETE'});
  loadMeetings();
}

// ---------- CONNECTORS ----------
function loadConnectors(){
  const ai = STAFF.filter(s=>s.type==='AI');
  el('connectorRows').innerHTML = ai.length ? ai.map(s=>`
    <tr>
      <td><span class="avatar" style="background:${colorFor(s.id)}">${initials(s.fullName)}</span>${s.fullName}</td>
      <td><span class="pill ai">${s.connector||'NONE'}</span></td>
      <td style="color:var(--muted2);font-size:12px;">${s.connectorEndpoint||'— (simulation)'}</td>
      <td>${s.model||'—'}</td>
      <td>${s.voiceEnabled ? '🔊 '+(s.voiceProvider||'on') : '—'}</td>
    </tr>`).join('') : `<tr><td colspan="5" style="color:var(--muted2)">No AI staff yet.</td></tr>`;
}

// ---------- SETTINGS ----------
function loadSettings(){
  el('setUser').textContent = USER;
  el('setRole').textContent = ROLE;
  el('settingsMsg').innerHTML = '';
  el('smtpMsg').innerHTML = '';
  loadBranding();
  loadSmtp();
}
function loadBranding(){
  el('brName').value = BRANDING.portalName||'AI-Workforce';
  el('brLogo').value = BRANDING.logoUrl||'';
  el('brTheme').value = BRANDING.theme||'midnight';
  // live theme preview on change
  el('brTheme').onchange = ()=>applyThemeAttrs(el('brTheme').value, currentAccent());
  el('accentSwatches').innerHTML = ACCENTS.map(a=>
    `<button onclick="pickAccent('${a}')" data-acc="${a}" title="${a}"
      style="width:26px;height:26px;border-radius:50%;border:2px solid ${a===(BRANDING.accent||'blue')?'#fff':'transparent'};background:${ACCENT_HEX[a]};cursor:pointer;"></button>`
  ).join('');
}
function currentAccent(){
  const sel = document.querySelector('#accentSwatches button[style*="2px solid rgb(255"]');
  return sel ? sel.dataset.acc : (BRANDING.accent||'blue');
}
function pickAccent(a){
  document.querySelectorAll('#accentSwatches button').forEach(b=>b.style.border='2px solid transparent');
  const btn = document.querySelector(`#accentSwatches button[data-acc="${a}"]`);
  if(btn) btn.style.border='2px solid #fff';
  applyThemeAttrs(el('brTheme').value, a);
}
async function saveBranding(){
  const body={portalName:el('brName').value,logoUrl:el('brLogo').value,theme:el('brTheme').value,accent:currentAccent()};
  try{
    BRANDING = await api('/portal/settings',{method:'PUT',body:JSON.stringify(body)});
    applyBranding();
    el('brandMsg').innerHTML=`<div class="msg ok">Branding saved &amp; applied.</div>`;
  }catch(e){ el('brandMsg').innerHTML=`<div class="msg err">${e.message}</div>`; }
}
async function loadSmtp(){
  try{
    const s = await api('/emails/settings');
    el('smtpEnabled').checked = !!s.enabled;
    el('smtpHost').value = s.host||'';
    el('smtpPort').value = s.port||587;
    el('smtpUser').value = s.username||'';
    el('smtpPass').value = '';
    el('smtpFrom').value = s.fromAddress||'';
    el('smtpTls').checked = s.startTls!==false;
    el('smtpAuth').checked = s.auth!==false;
  }catch(e){ el('smtpMsg').innerHTML=`<div class="msg err">${e.message}</div>`; }
}
async function saveSmtp(){
  const body={
    enabled:el('smtpEnabled').checked, host:el('smtpHost').value,
    port:parseInt(el('smtpPort').value)||587, username:el('smtpUser').value,
    password:el('smtpPass').value, fromAddress:el('smtpFrom').value,
    startTls:el('smtpTls').checked, auth:el('smtpAuth').checked
  };
  try{
    await api('/emails/settings',{method:'PUT',body:JSON.stringify(body)});
    el('smtpMsg').innerHTML=`<div class="msg ok">Settings saved.</div>`;
    el('smtpPass').value='';
  }catch(e){ el('smtpMsg').innerHTML=`<div class="msg err">${e.message}</div>`; }
}
async function testSmtp(){
  el('smtpMsg').innerHTML=`<div class="msg" style="background:#11203b;color:var(--muted2)">Testing connection…</div>`;
  // save first so the test uses the latest values
  await saveSmtp().catch(()=>{});
  try{
    const r = await api('/emails/settings/test',{method:'POST'});
    el('smtpMsg').innerHTML = r.ok
      ? `<div class="msg ok">✓ ${r.message}</div>`
      : `<div class="msg err">✕ ${r.message}</div>`;
  }catch(e){ el('smtpMsg').innerHTML=`<div class="msg err">${e.message}</div>`; }
}
async function changeOwnPassword(){
  const msg = el('settingsMsg');
  const a = el('setNew').value, b = el('setNew2').value;
  if(a!==b){ msg.innerHTML = `<div class="msg err">Passwords don't match.</div>`; return; }
  try{
    const r = await api('/auth/change-password', {method:'POST',
      body:JSON.stringify({username:USER, oldPassword:el('setOld').value, newPassword:a})});
    if(!r.ok){ msg.innerHTML = `<div class="msg err">${r.message}</div>`; return; }
    if(r.token) TOKEN = r.token;
    msg.innerHTML = `<div class="msg ok">${r.message}</div>`;
    el('setOld').value=el('setNew').value=el('setNew2').value='';
  }catch(e){ msg.innerHTML = `<div class="msg err">${e.message}</div>`; }
}
