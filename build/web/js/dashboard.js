document.addEventListener('DOMContentLoaded', () => {
    const tbody = document.querySelector('#recent-projects-table tbody');

    // ---- Zone counts (Red = High risk, Yellow = Medium, Green = Low) ----
    // Same source and rule as the Red / Yellow / Green Zone pages (mplads_ml_results.risk_level),
    // so these numbers always equal "Total results" on those pages.
    function setText(id, text) {
        const el = document.getElementById(id);
        if (el) el.textContent = text;
    }

    function loadZoneCounts() {
        fetch('/MPLADs/api/zone-counts')
            .then(res => {
                if (!res.ok) throw new Error('Bad response: ' + res.status);
                return res.json();
            })
            .then(c => {
                const red = Number(c.high) || 0;
                const yellow = Number(c.medium) || 0;
                const green = Number(c.low) || 0;
                const sum = red + yellow + green;
                const pct = n => sum ? ((n / sum) * 100).toFixed(1) : '0.0';

                // KPI cards
                setText('kpi-red-zone', red.toLocaleString());
                setText('kpi-yellow-zone', yellow.toLocaleString());
                setText('kpi-green-zone', green.toLocaleString());

                // Donut legend
                setText('donut-red', red.toLocaleString() + ' (' + pct(red) + '%)');
                setText('donut-yellow', yellow.toLocaleString() + ' (' + pct(yellow) + '%)');
                setText('donut-green', green.toLocaleString() + ' (' + pct(green) + '%)');

                // Donut ring drawn from the same numbers
                const ring = document.querySelector('.v2-donut');
                if (ring && sum > 0) {
                    const r = (red / sum) * 100;
                    const y = r + (yellow / sum) * 100;
                    ring.style.background = 'conic-gradient(#ef4444 0% ' + r + '%, #eab308 ' + r + '% ' + y + '%, #22c55e ' + y + '% 100%)';
                }
            })
            .catch(err => {
                console.error('Failed to load zone counts:', err);
                ['kpi-red-zone', 'kpi-yellow-zone', 'kpi-green-zone'].forEach(id => setText(id, '—'));
                ['donut-red', 'donut-yellow', 'donut-green'].forEach(id => setText(id, '—'));
            });
    }
    loadZoneCounts();

    // ---- Total Projects: every record in the projects table ----
    fetch('/MPLADs/api/projects')
        .then(res => res.json())
        .then(projects => {
            setText('total-projects', projects.length.toLocaleString());
            setText('donut-total-projects', projects.length.toLocaleString());
        })
        .catch(err => console.error('Failed to load total projects:', err));

    // ---- Priority Review Queue: RED ZONE (High risk) projects only ----
    if (tbody) {
        fetch('/MPLADs/api/zone-projects?level=High')
            .then(res => {
                if (!res.ok) throw new Error('Bad response: ' + res.status);
                return res.json();
            })
            .then(redProjects => {
                // Show ONE row for records that share the same MP name + work + allocation
                // amount (lowest project_id is kept).
                const norm = v => String(v == null ? '' : v).trim();
                const seen = new Set();
                const uniqueProjects = redProjects
                    .slice()
                    .sort((a, b) => Number(a.project_id) - Number(b.project_id))
                    .filter(p => {
                        const key = JSON.stringify([norm(p.mp_name), norm(p.work_), p.allocation_amount]);
                        if (seen.has(key)) return false;
                        seen.add(key);
                        return true;
                    });

                if (uniqueProjects.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;">No Red Zone projects found.</td></tr>';
                    return;
                }

                // Pagination Logic (30 per page)
                const itemsPerPage = 30;
                let currentPage = 1;

                function displayPage(page) {
                    tbody.innerHTML = '';
                    const start = (page - 1) * itemsPerPage;
                    const end = start + itemsPerPage;
                    const paginatedItems = uniqueProjects.slice(start, end);

                    paginatedItems.forEach(project => {
                        const tr = document.createElement('tr');
                        const work = project.work_ || 'N/A';
                        // project_id is 0 when no matching record was found in the projects table
                        const workCell = project.project_id
                            ? `<a href="project-details.html?id=${project.project_id}&level=High" style="color: var(--accent-color); text-decoration: none;">${work}</a>`
                            : work;
                        tr.innerHTML = `
                            <td>${workCell}</td>
                            <td>${project.mp_name || 'N/A'}</td>
                            <td>${project.constituency || 'N/A'}</td>
                            <td>₹${(project.allocation_amount || 0).toLocaleString('en-IN')}</td>
                        `;
                        tbody.appendChild(tr);
                    });

                    const totalPages = Math.ceil(uniqueProjects.length / itemsPerPage) || 1;

                    // Pagination controls
                    const paginationRow = document.createElement('tr');
                    paginationRow.innerHTML = `
                        <td colspan="4" style="text-align: center; padding: 25px;">
                            <div style="display:inline-flex; align-items:center; gap: 20px; background: rgba(255,255,255,0.03); padding: 10px 24px; border-radius: 30px; border: 1px solid var(--border-light);">
                                <button id="prevBtn" ${page === 1 ? 'disabled' : ''} style="background:transparent; border:none; color: ${page === 1 ? '#4b5563' : '#f3f4f6'}; cursor: ${page === 1 ? 'not-allowed' : 'pointer'}; font-weight:600; font-size: 0.9rem; transition: color 0.2s;">← Previous</button>
                                <span style="font-weight: 600; color: #d4af37; font-size: 0.95rem; padding: 0 10px;">Page ${page} of ${totalPages}</span>
                                <button id="nextBtn" ${end >= uniqueProjects.length ? 'disabled' : ''} style="background:transparent; border:none; color: ${end >= uniqueProjects.length ? '#4b5563' : '#f3f4f6'}; cursor: ${end >= uniqueProjects.length ? 'not-allowed' : 'pointer'}; font-weight:600; font-size: 0.9rem; transition: color 0.2s;">Next →</button>
                            </div>
                        </td>
                    `;
                    tbody.appendChild(paginationRow);

                    if (document.getElementById('prevBtn')) {
                        document.getElementById('prevBtn').addEventListener('click', () => { currentPage--; displayPage(currentPage); });
                    }
                    if (document.getElementById('nextBtn')) {
                        document.getElementById('nextBtn').addEventListener('click', () => { currentPage++; displayPage(currentPage); });
                    }
                }

                displayPage(currentPage);
            })
            .catch(e => {
                console.error("Failed to load Red Zone projects:", e);
                tbody.innerHTML = '<tr><td colspan="4" style="color:red;">Error loading data from backend</td></tr>';
            });
    }
});