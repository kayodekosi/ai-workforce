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
  connectWs();
  loadAll();
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
  ['staff','company','organogram','hr','chat','calendar','connectors','settings'].forEach(v=>{
    el('view-'+v).classList.toggle('hidden', v!==view);
  });
  document.querySelectorAll('.nav').forEach(n=>n.classList.toggle('active', n.dataset.view===view));
  if(view==='chat') loadChannels();
  if(view==='calendar') loadMeetings();
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
