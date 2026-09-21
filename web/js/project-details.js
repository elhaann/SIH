document.addEventListener('DOMContentLoaded', () => {
    const container = document.getElementById('project-details-container');
    const urlParams = new URLSearchParams(window.location.search);
    const projectId = urlParams.get('id');

    // Where the "Back" link goes: the zone page the user came from (?level=High|Medium|Low)
    const BACK_LINKS = {
        high:   { href: 'red-zone.html',    label: '← Back to Red Zone Projects' },
        medium: { href: 'yellow-zone.html', label: '← Back to Yellow Zone Projects' },
        low:    { href: 'green-zone.html',  label: '← Back to Green Zone Projects' }
    };
    const back = BACK_LINKS[(urlParams.get('level') || '').trim().toLowerCase()]
        || { href: 'projects.html', label: '← Back to Projects' };

    if (!projectId) {
        showError("No Project ID provided in URL.");
        return;
    }

    // Fetch all projects and find the matching one
    fetch('/MPLADs/api/projects')
        .then(res => res.json())
        .then(projects => {
            const p = projects.find(proj => String(proj.project_id) === String(projectId));
            
            if (p) {
                renderDetails(p);
            } else {
                showError("Project not found in database.");
            }
        })
        .catch(e => {
            console.error(e);
            showError("Failed to fetch project details from backend.");
        });

    function showError(msg) {
        container.innerHTML = `
            <div style="text-align: center; padding: 80px;">
                <h2 style="margin-bottom: 1rem;">Error</h2>
                <p style="margin-bottom: 2rem;">${msg}</p>
                <a href="${back.href}" class="btn-ai" style="display: inline-block; width: auto; padding: 12px 30px;">${back.label}</a>
            </div>
        `;
    }

    function getStatusPillClass(status) {
        if (status === 'Completed') return 'completed';
        if (status === 'Ongoing' || status === 'In Progress') return 'in-progress';
        if (status === 'Unsanctioned') return 'unsanctioned';
        if (status === 'Delayed') return 'delayed';
        return 'in-progress';
    }

    function getStatusTextClass(status) {
        if (status === 'Completed') return 'green';
        if (status === 'Ongoing' || status === 'In Progress') return 'blue';
        if (status === 'Unsanctioned') return 'red';
        if (status === 'Delayed') return 'yellow';
        return 'blue';
    }

    function renderDetails(p) {
        const status = p.project_status || 'Ongoing';
        const pillClass = getStatusPillClass(status);
        const statusTextClass = getStatusTextClass(status);
        const allocationCr = ((p.allocation_amount || 0) / 10000000).toFixed(2);

        container.innerHTML = `
            <!-- Header -->
            <div class="detail-header">
                <a href="${back.href}" class="back-link">${back.label}</a>
                <div class="detail-status">
                    <span class="status-pill ${pillClass}">${status}</span>
                </div>
                <h1>${p.work_ || 'Project Details'}</h1>
                <p class="breadcrumb">${p.state || 'N/A'}<span>•</span>${p.constituency || 'N/A'}<span>•</span>${p.house || 'General'}</p>
            </div>

            <!-- Main Grid -->
            <div class="detail-grid">
                <!-- PROJECT OVERVIEW -->
                <div class="info-card">
                    <h3>Project Overview</h3>
                    <div class="info-row">
                        <div>
                            <div class="info-label">MP Name</div>
                            <div class="info-value">${p.mp_name || 'N/A'}</div>
                        </div>
                        <div>
                            <div class="info-label">Category</div>
                            <div class="info-value">${p.house || 'General'}</div>
                        </div>
                    </div>
                    <div class="info-row">
                        <div>
                            <div class="info-label">State</div>
                            <div class="info-value">${p.state || 'N/A'}</div>
                        </div>
                        <div>
                            <div class="info-label">Constituency</div>
                            <div class="info-value">${p.constituency || 'N/A'}</div>
                        </div>
                    </div>
                </div>

                <!-- LOCATION -->
                <div class="info-card">
                    <h3>Location</h3>
                    <div class="info-row">
                        <div>
                            <div class="info-label">State</div>
                            <div class="info-value">${p.state || 'N/A'}</div>
                        </div>
                        <div>
                            <div class="info-label">Constituency</div>
                            <div class="info-value">${p.constituency || 'N/A'}</div>
                        </div>
                    </div>
                    <div class="info-row">
                        <div>
                            <div class="info-label">IDA</div>
                            <div class="info-value">${p.ida || 'N/A'}</div>
                        </div>
                    </div>
                </div>

                <!-- AI RISK ASSESSMENT -->
                <div class="ai-card">
                    <h3>AI Risk Assessment</h3>
                    <button class="btn-ai" id="run-analysis-btn" onclick="runAnalysis(${p.project_id})">Run Analysis →</button>
                    <div id="analysis-result"></div>
                </div>
            </div>

            <!-- Bottom Row -->
            <div class="detail-bottom">
                <!-- FINANCIAL INFORMATION -->
                <div class="info-card">
                    <h3>Financial Information</h3>
                    <div class="info-row">
                        <div>
                            <div class="info-label">Allocation Amount</div>
                            <div class="info-value">₹${allocationCr} Cr</div>
                        </div>
                    </div>
                </div>

                <!-- APPROVAL & STATUS -->
                <div class="info-card">
                    <h3>Approval & Status</h3>
                    <div class="info-row">
                        <div>
                            <div class="info-label">Date</div>
                            <div class="info-value">${p.Date_ || 'N/A'}</div>
                        </div>
                        <div>
                            <div class="info-label">Approval</div>
                            <div class="info-value">${p.ida_approval || 'N/A'}</div>
                        </div>
                    </div>
                    <div class="info-row">
                        <div>
                            <div class="info-label">Status</div>
                            <div class="info-value status-text ${statusTextClass}">${status}</div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }
});

// ---------- AI Analysis ----------
function escapeHtml(v) {
    return String(v == null ? '' : v)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;')
        .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function showAnalysisError(msg) {
    const box = document.getElementById('analysis-result');
    if (box) {
        box.innerHTML = '<p style="margin-top:20px; padding:12px 14px; border-radius:8px; ' +
            'background:rgba(239,68,68,0.12); color:#fca5a5; font-size:0.95rem;">' +
            escapeHtml(msg) + '</p>';
    }
}

function showAnalysisResult(a) {
    const box = document.getElementById('analysis-result');
    if (!box) return;

    const score = Number(a.risk_score);
    const anomaly = Number(a.anomaly_score);
    const level = String(a.risk_level || 'N/A');
    const colors = {
        High:   { fg: '#f87171', bg: 'rgba(239,68,68,0.15)' },
        Medium: { fg: '#facc15', bg: 'rgba(234,179,8,0.15)' },
        Low:    { fg: '#4ade80', bg: 'rgba(34,197,94,0.15)' }
    };
    const c = colors[level] || { fg: '#e5e7eb', bg: 'rgba(255,255,255,0.08)' };

    const rows = [
        ['Risk Score', isNaN(score) ? 'N/A' : score.toFixed(1) + ' / 100'],
        ['Risk Level', '<span style="display:inline-block; padding:3px 12px; border-radius:20px; font-weight:700; ' +
            'color:' + c.fg + '; background:' + c.bg + ';">' + escapeHtml(level) + '</span>'],
        ['Anomaly Score', isNaN(anomaly) ? 'N/A' : anomaly.toFixed(3)],
        ['Recommendation', escapeHtml(a.recommendation || 'No anomalies detected.')]
    ];
    if (a.model_version) rows.push(['Model Version', escapeHtml(a.model_version)]);

    const cell = 'padding:10px 8px; border-bottom:1px solid rgba(255,255,255,0.08); vertical-align:top;';
    box.innerHTML =
        '<table style="width:100%; border-collapse:collapse; margin-top:22px; font-size:0.95rem;">' +
        '<thead><tr><th colspan="2" style="text-align:left; padding:0 8px 10px; font-size:0.75rem; ' +
        'letter-spacing:0.08em; text-transform:uppercase; color:#d4af37;">Analysis Result</th></tr></thead><tbody>' +
        rows.map(r => '<tr><td style="' + cell + ' color:#9ca3af; width:38%;">' + r[0] + '</td>' +
                      '<td style="' + cell + ' color:#f3f4f6; font-weight:600;">' + r[1] + '</td></tr>').join('') +
        '</tbody></table>';
}

function runAnalysis(projectId) {
    const btn = document.getElementById('run-analysis-btn');
    const box = document.getElementById('analysis-result');
    if (box) box.innerHTML = '';
    if (btn) { btn.textContent = 'Analyzing...'; btn.disabled = true; }

    // zone the user came from (red/yellow/green page adds &level=High|Medium|Low)
    const level = new URLSearchParams(window.location.search).get('level');
    fetch('/MPLADs/api/analyze?id=' + encodeURIComponent(projectId) + (level ? '&level=' + encodeURIComponent(level) : ''))
        .then(r => r.json().catch(() => ({ error: 'Invalid response from server.' }))
            .then(data => ({ ok: r.ok, data })))
        .then(({ ok, data }) => {
            if (!ok || data.error) {
                showAnalysisError(data.error || 'Analysis failed. Please try again.');
            } else {
                showAnalysisResult(data);
            }
        })
        .catch(e => {
            console.log('ML Service unavailable:', e);
            showAnalysisError('Could not load the risk result. Make sure the Tomcat server is running.');
        })
        .finally(() => {
            if (btn) { btn.textContent = 'Run Analysis →'; btn.disabled = false; }
        });
}