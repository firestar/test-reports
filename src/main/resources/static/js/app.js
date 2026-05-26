const API = '';

let allStates = [];
let allEditors = [];

// Tab switching
document.querySelectorAll('.nav-tab').forEach(tab => {
    tab.addEventListener('click', () => {
        document.querySelectorAll('.nav-tab').forEach(t => t.classList.remove('active'));
        document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
        tab.classList.add('active');
        document.getElementById('tab-' + tab.dataset.tab).classList.add('active');
        loadTabData(tab.dataset.tab);
    });
});

function loadTabData(tab) {
    switch (tab) {
        case 'queue': loadQueue(); break;
        case 'reports': loadReports(); break;
        case 'editors': loadEditors(); break;
        case 'settings': loadSettings(); break;
    }
}

// ========== Queue ==========
async function loadQueue() {
    const resp = await fetch(API + '/api/reports/queue');
    const reports = await resp.json();
    const tbody = document.querySelector('#queue-table tbody');
    tbody.innerHTML = reports.map(r => `
        <tr>
            <td>${r.id}</td>
            <td>${esc(r.title)}</td>
            <td>${esc(r.region)}</td>
            <td>${esc(r.auditType || '-')}</td>
            <td>${esc(r.fiscalYear || '-')}</td>
            <td><small>${esc(r.reportPath)}</small></td>
            <td>${formatDate(r.createdAt)}</td>
            <td><button class="btn btn-success btn-sm" onclick="showAssignModal(${r.id})">Assign</button></td>
        </tr>
    `).join('');
}

// ========== Reports ==========
async function loadReports() {
    await loadStatesCache();
    populateStateFilter();
    const resp = await fetch(API + '/api/reports');
    let reports = await resp.json();
    const filterState = document.getElementById('filter-state').value;
    if (filterState) {
        reports = reports.filter(r => r.currentState && r.currentState.id == filterState);
    }
    const tbody = document.querySelector('#reports-table tbody');
    tbody.innerHTML = reports.map(r => {
        const days = r.assignedAt ? daysSince(r.assignedAt) : '-';
        const isTerminal = r.currentState && r.currentState.isTerminal;
        return `
        <tr>
            <td>${r.id}</td>
            <td>${esc(r.title)}</td>
            <td>${esc(r.region)}</td>
            <td><span class="state-badge${isTerminal ? ' terminal' : ''}">${esc(r.currentState ? r.currentState.name : '-')}</span></td>
            <td>${r.editor ? esc(r.editor.name) : '<em>Unassigned</em>'}</td>
            <td>${days}</td>
            <td>${formatDate(r.createdAt)}</td>
            <td><button class="btn btn-primary btn-sm" onclick="showReportDetail(${r.id})">View</button></td>
        </tr>`;
    }).join('');
}

async function showReportDetail(id) {
    const resp = await fetch(API + '/api/reports/' + id);
    const data = await resp.json();
    const report = data.report;
    const history = data.history || [];
    const metrics = data.metrics || {};
    const transitions = data.allowedTransitions || [];

    let html = `
        <div style="margin-bottom:16px">
            <strong>Title:</strong> ${esc(report.title)}<br>
            <strong>Region:</strong> ${esc(report.region)}<br>
            <strong>Type:</strong> ${esc(report.auditType || '-')}<br>
            <strong>Fiscal Year:</strong> ${esc(report.fiscalYear || '-')}<br>
            <strong>Path:</strong> <code>${esc(report.reportPath)}</code><br>
            <strong>Editor:</strong> ${report.editor ? esc(report.editor.name) : 'Unassigned'}<br>
            <strong>Current State:</strong> <span class="state-badge">${esc(report.currentState.name)}</span><br>
            ${metrics.daysWithEditor !== undefined ? `<strong>Days with Editor:</strong> ${metrics.daysWithEditor}<br>` : ''}
            <strong>Total Hours Elapsed:</strong> ${metrics.totalHoursElapsed || 0}
        </div>`;

    if (report.notes) {
        html += `<div style="margin-bottom:16px"><strong>Notes:</strong><br>${esc(report.notes)}</div>`;
    }

    // State history timeline
    html += '<h3>State History</h3><div class="timeline">';
    history.forEach(h => {
        const from = h.fromState ? h.fromState.name : 'Created';
        html += `
            <div class="timeline-item">
                <span class="time">${formatDateTime(h.changedAt)}</span>
                <span class="description">${esc(from)} &rarr; <strong>${esc(h.toState.name)}</strong> by ${esc(h.changedBy)}</span>
            </div>`;
    });
    html += '</div>';

    // State timings
    if (metrics.stateTimings && metrics.stateTimings.length > 0) {
        html += '<h3 style="margin-top:12px">Time per State</h3><table class="data-table"><thead><tr><th>State</th><th>Hours</th></tr></thead><tbody>';
        metrics.stateTimings.forEach(t => {
            html += `<tr><td>${esc(t.state)}${t.current ? ' (current)' : ''}</td><td>${t.hoursInState}h</td></tr>`;
        });
        html += '</tbody></table>';
    }

    // Allowed transitions
    if (transitions.length > 0) {
        html += '<h3 style="margin-top:16px">Transition</h3>';
        html += '<div class="form-group" style="margin-bottom:8px"><label>Transition Date (optional)</label><input type="datetime-local" id="transition-date" class="transition-date-input"></div>';
        html += '<div style="display:flex;gap:8px;flex-wrap:wrap">';
        transitions.forEach(t => {
            html += `<button class="btn btn-primary btn-sm" onclick="doTransition(${report.id}, ${t.toState.id})">&rarr; ${esc(t.toState.name)}</button>`;
        });
        html += '</div>';
    }

    openModal('Report #' + report.id, html);
}

async function doTransition(reportId, toStateId) {
    const dateInput = document.getElementById('transition-date');
    const payload = {toStateId: toStateId, changedBy: 'admin'};
    if (dateInput && dateInput.value) {
        payload.changedAt = dateInput.value;
    }
    const resp = await fetch(API + '/api/reports/' + reportId + '/transition', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(payload)
    });
    if (resp.ok) {
        closeModal();
        loadReports();
        loadQueue();
    } else {
        const err = await resp.json();
        alert(err.error || 'Transition failed');
    }
}

// ========== Editors ==========
async function loadEditors() {
    const resp = await fetch(API + '/api/editors');
    allEditors = await resp.json();

    // Check calendar status
    let calendarConfigured = false;
    try {
        const calResp = await fetch(API + '/api/calendar/status');
        const calStatus = await calResp.json();
        calendarConfigured = calStatus.configured;
    } catch (e) { /* ignore */ }

    const container = document.getElementById('editors-list');
    let html = '';
    for (const editor of allEditors) {
        let workloadResp = await fetch(API + '/api/editors/' + editor.id);
        let workload = await workloadResp.json();
        let reports = workload.reports || [];

        html += `
        <div class="card">
            <h3>${esc(editor.name)}</h3>
            <div class="meta">${esc(editor.email)}</div>
            <div class="stats">
                <div class="stat">
                    <div class="stat-value">${reports.length}</div>
                    <div class="stat-label">Reports</div>
                </div>
            </div>`;

        if (reports.length > 0) {
            html += '<table class="data-table"><thead><tr><th>Report</th><th>State</th><th>Days</th></tr></thead><tbody>';
            reports.forEach(r => {
                html += `<tr><td>${esc(r.title)}</td><td>${esc(r.currentState)}</td><td>${r.daysAssigned !== undefined ? r.daysAssigned : '-'}</td></tr>`;
            });
            html += '</tbody></table>';
        }

        // Calendar availability
        if (calendarConfigured) {
            html += `<button class="btn btn-secondary btn-sm" style="margin-top:8px" onclick="checkCalendar(${editor.id})">Check Calendar</button>`;
            html += `<div id="calendar-${editor.id}"></div>`;
        } else {
            html += `<div class="calendar-status calendar-unknown">Calendar not configured</div>`;
        }

        html += '</div>';
    }
    container.innerHTML = html;
}

async function checkCalendar(editorId) {
    const resp = await fetch(API + '/api/calendar/availability?editorIds=' + editorId + '&days=7');
    const data = await resp.json();
    const container = document.getElementById('calendar-' + editorId);
    const entries = Object.values(data);
    if (entries.length > 0) {
        const entry = entries[0];
        if (entry.error) {
            container.innerHTML = `<div class="calendar-status calendar-unknown">${esc(entry.error)}</div>`;
        } else if (entry.configured === false) {
            container.innerHTML = `<div class="calendar-status calendar-unknown">${esc(entry.message)}</div>`;
        } else {
            const busy = entry.totalBusySlots || 0;
            const cls = busy > 5 ? 'calendar-busy' : 'calendar-available';
            const label = busy > 5 ? 'Busy (' + busy + ' events)' : (busy === 0 ? 'Available' : busy + ' events');
            container.innerHTML = `<div class="calendar-status ${cls}">${label} (next 7 days)</div>`;
        }
    }
}

// ========== Settings ==========
async function loadSettings() {
    await loadStatesCache();
    loadStatesTable();
    loadTransitionsTable();
}

async function loadStatesCache() {
    const resp = await fetch(API + '/api/states');
    allStates = await resp.json();
}

function loadStatesTable() {
    const tbody = document.querySelector('#states-table tbody');
    tbody.innerHTML = allStates.map(s => `
        <tr>
            <td>${s.displayOrder}</td>
            <td>${esc(s.name)}</td>
            <td>${s.isInitial ? 'Yes' : ''}</td>
            <td>${s.isTerminal ? 'Yes' : ''}</td>
            <td><button class="btn btn-danger btn-sm" onclick="deleteState(${s.id})">Delete</button></td>
        </tr>
    `).join('');
}

async function loadTransitionsTable() {
    const resp = await fetch(API + '/api/transitions');
    const transitions = await resp.json();
    const tbody = document.querySelector('#transitions-table tbody');
    tbody.innerHTML = transitions.map(t => `
        <tr>
            <td>${esc(t.fromState.name)}</td>
            <td>${esc(t.toState.name)}</td>
            <td><button class="btn btn-danger btn-sm" onclick="deleteTransition(${t.id})">Delete</button></td>
        </tr>
    `).join('');
}

// ========== Modals ==========
function openModal(title, bodyHtml) {
    document.getElementById('modal-title').textContent = title;
    document.getElementById('modal-body').innerHTML = bodyHtml;
    document.getElementById('modal-overlay').classList.add('active');
}

function closeModal() {
    document.getElementById('modal-overlay').classList.remove('active');
}

function showCreateReportModal() {
    const html = `
        <form onsubmit="createReport(event)">
            <div class="form-group"><label>Title *</label><input id="cr-title" required></div>
            <div class="form-group"><label>Region *</label><input id="cr-region" required></div>
            <div class="form-group"><label>Audit Type</label><input id="cr-type" placeholder="e.g. Financial, Compliance"></div>
            <div class="form-group"><label>Fiscal Year</label><input id="cr-year" placeholder="e.g. 2026"></div>
            <div class="form-group"><label>Report Path *</label><input id="cr-path" required placeholder="\\\\server\\share\\report.docx"></div>
            <div class="form-group"><label>Notes</label><textarea id="cr-notes"></textarea></div>
            <div class="form-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                <button type="submit" class="btn btn-primary">Create Report</button>
            </div>
        </form>`;
    openModal('New Report', html);
}

async function createReport(e) {
    e.preventDefault();
    const body = {
        title: document.getElementById('cr-title').value,
        region: document.getElementById('cr-region').value,
        auditType: document.getElementById('cr-type').value || null,
        fiscalYear: document.getElementById('cr-year').value || null,
        reportPath: document.getElementById('cr-path').value,
        notes: document.getElementById('cr-notes').value || null
    };
    const resp = await fetch(API + '/api/reports', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(body)
    });
    if (resp.ok) {
        closeModal();
        loadQueue();
    } else {
        const err = await resp.json();
        alert(err.error || 'Failed to create report');
    }
}

async function showAssignModal(reportId) {
    const resp = await fetch(API + '/api/editors');
    const editors = await resp.json();
    let html = `<form onsubmit="assignEditor(event, ${reportId})">
        <div class="form-group">
            <label>Select Editor</label>
            <select id="assign-editor" required>
                <option value="">-- Choose editor --</option>
                ${editors.map(e => `<option value="${e.id}">${esc(e.name)} (${esc(e.email)})</option>`).join('')}
            </select>
        </div>
        <div class="form-actions">
            <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
            <button type="submit" class="btn btn-success">Assign</button>
        </div>
    </form>`;
    openModal('Assign Editor to Report #' + reportId, html);
}

async function assignEditor(e, reportId) {
    e.preventDefault();
    const editorId = document.getElementById('assign-editor').value;
    const resp = await fetch(API + '/api/reports/' + reportId + '/assign', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({editorId: parseInt(editorId)})
    });
    if (resp.ok) {
        closeModal();
        loadQueue();
    } else {
        const err = await resp.json();
        alert(err.error || 'Assignment failed');
    }
}

function showCreateEditorModal() {
    const html = `
        <form onsubmit="createEditor(event)">
            <div class="form-group"><label>Name *</label><input id="ce-name" required></div>
            <div class="form-group"><label>Email *</label><input id="ce-email" type="email" required></div>
            <div class="form-group"><label>Google Calendar ID</label><input id="ce-calendar" placeholder="Usually same as email"></div>
            <div class="form-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                <button type="submit" class="btn btn-primary">Create Editor</button>
            </div>
        </form>`;
    openModal('New Editor', html);
}

async function createEditor(e) {
    e.preventDefault();
    const body = {
        name: document.getElementById('ce-name').value,
        email: document.getElementById('ce-email').value,
        calendarId: document.getElementById('ce-calendar').value || null
    };
    const resp = await fetch(API + '/api/editors', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(body)
    });
    if (resp.ok) {
        closeModal();
        loadEditors();
    } else {
        const err = await resp.json();
        alert(err.error || 'Failed to create editor');
    }
}

function showCreateStateModal() {
    const html = `
        <form onsubmit="createState(event)">
            <div class="form-group"><label>Name *</label><input id="cs-name" required></div>
            <div class="form-group"><label>Display Order</label><input id="cs-order" type="number" value="0"></div>
            <div class="form-group"><label><input type="checkbox" id="cs-initial"> Initial State</label></div>
            <div class="form-group"><label><input type="checkbox" id="cs-terminal"> Terminal State</label></div>
            <div class="form-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                <button type="submit" class="btn btn-primary">Create State</button>
            </div>
        </form>`;
    openModal('New State', html);
}

async function createState(e) {
    e.preventDefault();
    const body = {
        name: document.getElementById('cs-name').value,
        displayOrder: parseInt(document.getElementById('cs-order').value),
        isInitial: document.getElementById('cs-initial').checked,
        isTerminal: document.getElementById('cs-terminal').checked
    };
    const resp = await fetch(API + '/api/states', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(body)
    });
    if (resp.ok) {
        closeModal();
        loadSettings();
    } else {
        const err = await resp.json();
        alert(err.error || 'Failed to create state');
    }
}

async function deleteState(id) {
    if (!confirm('Delete this state?')) return;
    const resp = await fetch(API + '/api/states/' + id, {method: 'DELETE'});
    if (resp.ok) {
        loadSettings();
    } else {
        const err = await resp.json();
        alert(err.error || 'Failed to delete state');
    }
}

function showCreateTransitionModal() {
    const options = allStates.map(s => `<option value="${s.id}">${esc(s.name)}</option>`).join('');
    const html = `
        <form onsubmit="createTransition(event)">
            <div class="form-group"><label>From State</label><select id="ct-from" required>${options}</select></div>
            <div class="form-group"><label>To State</label><select id="ct-to" required>${options}</select></div>
            <div class="form-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                <button type="submit" class="btn btn-primary">Add Transition</button>
            </div>
        </form>`;
    openModal('New Transition', html);
}

async function createTransition(e) {
    e.preventDefault();
    const body = {
        fromStateId: parseInt(document.getElementById('ct-from').value),
        toStateId: parseInt(document.getElementById('ct-to').value)
    };
    const resp = await fetch(API + '/api/transitions', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(body)
    });
    if (resp.ok) {
        closeModal();
        loadSettings();
    } else {
        const err = await resp.json();
        alert(err.error || 'Failed to create transition');
    }
}

async function deleteTransition(id) {
    if (!confirm('Delete this transition?')) return;
    const resp = await fetch(API + '/api/transitions/' + id, {method: 'DELETE'});
    if (resp.ok) {
        loadSettings();
    } else {
        const err = await resp.json();
        alert(err.error || 'Failed to delete transition');
    }
}

// ========== State filter ==========
function populateStateFilter() {
    const select = document.getElementById('filter-state');
    const currentVal = select.value;
    select.innerHTML = '<option value="">All States</option>' +
        allStates.map(s => `<option value="${s.id}">${esc(s.name)}</option>`).join('');
    select.value = currentVal;
}

// ========== Utilities ==========
function esc(str) {
    if (str == null) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

function formatDate(dt) {
    if (!dt) return '-';
    return new Date(dt).toLocaleDateString();
}

function formatDateTime(dt) {
    if (!dt) return '-';
    const d = new Date(dt);
    return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'});
}

function daysSince(dt) {
    if (!dt) return '-';
    const diff = Date.now() - new Date(dt).getTime();
    return Math.floor(diff / (1000 * 60 * 60 * 24));
}

// ========== Init ==========
document.addEventListener('DOMContentLoaded', () => {
    loadQueue();
});
