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

function logout(){ TOKEN=null; ROLE=null; location.reload(); }

function enterApp(){
  el('login').classList.add('hidden');
  el('shell').classList.remove('hidden');
  el('whoami').textContent = USER;
  el('sideRole').textContent = ROLE + ' PORTAL';
  loadAll();
}

// ---------- NAV ----------
function nav(view){
  ['staff','company','chat'].forEach(v=>{
    el('view-'+v).classList.toggle('hidden', v!==view);
  });
  document.querySelectorAll('.nav').forEach(n=>n.classList.toggle('active', n.dataset.view===view));
  if(view==='chat') loadChannels();
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
}
function renderMessages(msgs){
  el('chatMessages').innerHTML = msgs.map(m=>{
    const mine = m.sender && USER && m.sender.fullName===USER;
    const fn = m.sender ? m.sender.fullName : '?';
    const role = m.sender && m.sender.type==='AI' ? `<small>AI · ${m.sender.function||m.sender.position||''}</small>` : '';
    return `<div class="bubble ${mine?'me':''}">
      <div class="who">${fn} ${role} ${m.voiceClipUrl?'🔊':''}</div>
      <div class="txt">${(m.content||'').replace(/</g,'&lt;')}</div></div>`;
  }).join('') || `<div style="color:var(--muted2)">No messages yet — say hello below.</div>`;
  el('chatMessages').scrollTop = el('chatMessages').scrollHeight;
}
async function sendMsg(){
  if(!CURRENT_CHANNEL) return;
  const input = el('msgInput'); const content = input.value.trim();
  if(!content) return;
  // pick first member as the "sender" identity for the simulation
  const senderId = (CURRENT_CHANNEL.members && CURRENT_CHANNEL.members[0] && CURRENT_CHANNEL.members[0].id) || (STAFF[0]&&STAFF[0].id);
  input.value='';
  try{
    await api(`/chat/channels/${CURRENT_CHANNEL.id}/messages`, {method:'POST',
      body:JSON.stringify({senderId, content})});
    const msgs = await api(`/chat/channels/${CURRENT_CHANNEL.id}/messages`);
    renderMessages(msgs);
  }catch(e){ alert('Send failed: '+e.message); }
}

// AI fields toggle
document.addEventListener('change', e=>{
  if(e.target.id==='sType'){
    el('aiFields').style.display = e.target.value==='AI' ? 'block' : 'none';
  }
});
