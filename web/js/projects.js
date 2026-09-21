document.addEventListener('DOMContentLoaded', () => {
    const tbody = document.getElementById('project-rows');
    const searchInput = document.getElementById('search-input');
    const filterStatus = document.getElementById('filter-status');
    const filterCategory = document.getElementById('filter-category');
    const totalResults = document.getElementById('total-results');
    const pageIndicator = document.getElementById('page-indicator');
    const paginationControls = document.getElementById('pagination-controls');

    // Registry page (projects.html) has no data-zone.
    // Zone pages: red -> High risk, yellow -> Medium risk, green -> Low risk (from mplads_ml_results)
    const zone = document.body.getAttribute('data-zone');
    const isRegistry = !zone;
    const ZONE_LEVELS = { red: 'High', yellow: 'Medium', green: 'Low' };
    const DATA_URL = ZONE_LEVELS[zone]
        ? '/MPLADs/api/zone-projects?level=' + ZONE_LEVELS[zone]
        : '/MPLADs/api/projects';
    const COLS = 10;   // table columns (Project ID column is not shown)

    let allProjects = [];
    let filteredProjects = [];   // every record matching search/filters (used for "Total results")
    let displayProjects = [];    // what is shown in the table (duplicates collapsed on registry)
    let currentPage = 1;
    const itemsPerPage = 50;

    // Fetch projects from the Java Servlet backend
    fetch(DATA_URL)
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.json();
        })
        .then(data => {
            // Map backend fields to frontend format
            allProjects = data.map(p => ({
                id: p.project_id,
                name: p.work_ || 'Unknown Work',
                category: p.house || 'General', 
                status: p.project_status || 'Ongoing',
                cost: '₹' + (p.allocation_amount || 0).toLocaleString('en-IN'),
                start: p.Date_ || 'N/A', 
                mp_name: p.mp_name,
                state: p.state,
                district: p.constituency,
                ida_approval: p.ida_approval || 'N/A'
            }));
            
            filteredProjects = allProjects;
            applyDedupe();
            renderProjects();
        })
        .catch(error => {
            console.error('Error fetching projects:', error);
            if (tbody) tbody.innerHTML = '<tr><td colspan="' + COLS + '" style="color:red; text-align:center;">Failed to load projects from the database. Make sure the Tomcat server is running.</td></tr>';
        });

    // Show only ONE row for records that share the same MP name + work + allocation amount.
    // The record with the lowest Project ID is the one shown. Project ID is used only
    // internally (for choosing the record and for the "View" link), never displayed here.
    function applyDedupe() {
        if (!isRegistry) {
            displayProjects = filteredProjects;
            return;
        }
        const norm = v => String(v == null ? '' : v).trim();
        const sorted = filteredProjects.slice().sort((a, b) => Number(a.id) - Number(b.id));
        const seen = new Set();
        displayProjects = [];
        sorted.forEach(p => {
            const key = JSON.stringify([norm(p.mp_name), norm(p.name), p.cost]);
            if (!seen.has(key)) {
                seen.add(key);
                displayProjects.push(p);
            }
        });
    }

    function renderProjects() {
        if (!tbody) return;
        
        tbody.innerHTML = '';
        
        if (totalResults) totalResults.textContent = filteredProjects.length;

        if (filteredProjects.length === 0) {
            tbody.innerHTML = '<tr><td colspan="' + COLS + '" style="text-align:center;">No projects found matching the criteria.</td></tr>';
            if(paginationControls) paginationControls.innerHTML = '';
            if(pageIndicator) pageIndicator.textContent = '0 / 0';
            return;
        }

        const totalPages = Math.ceil(displayProjects.length / itemsPerPage) || 1;
        if (currentPage > totalPages) currentPage = totalPages;
        
        if(pageIndicator) pageIndicator.textContent = `${currentPage} / ${totalPages}`;

        const start = (currentPage - 1) * itemsPerPage;
        const end = start + itemsPerPage;
        const paginatedItems = displayProjects.slice(start, end);

        paginatedItems.forEach(project => {
            const tr = document.createElement('tr');
            
            // formatting status badge
            let badgeClass = 'status-badge ';
            if (project.status === 'Completed') badgeClass += 'completed';
            else if (project.status === 'Ongoing' || project.status === 'In Progress') badgeClass += 'in-progress';
            else if (project.status === 'Unsanctioned') badgeClass += 'unsanctioned';
            else badgeClass += 'tendered';

            // "View" needs a project id (used only for the link, never displayed)
            const viewCell = project.id
                ? `<a class="view-link" style="color: #3b82f6; text-decoration: none; font-weight: bold;" href="project-details.html?id=${project.id}${ZONE_LEVELS[zone] ? '&level=' + ZONE_LEVELS[zone] : ''}">View →</a>`
                : '—';

            tr.innerHTML = `
                <td>${project.mp_name || 'N/A'}</td>
                <td class="work">${project.name}</td>
                <td>${project.category}</td>
                <td>${project.state || 'N/A'}</td>
                <td>${project.district || 'N/A'}</td>
                <td>${project.start}</td>
                <td class="amount">${project.cost}</td>
                <td>${project.ida_approval}</td>
                <td><span class="${badgeClass}" style="padding: 4px 8px; border-radius: 4px; font-size: 0.85em; background: rgba(255,255,255,0.1);">${project.status}</span></td>
                <td>${viewCell}</td>
            `;
            tbody.appendChild(tr);
        });

        renderPagination(totalPages);
    }

    function renderPagination(totalPages) {
        if (!paginationControls) return;
        
        paginationControls.innerHTML = `
            <div style="display:inline-flex; align-items:center; gap: 20px; background: rgba(255,255,255,0.03); padding: 10px 24px; border-radius: 30px; border: 1px solid var(--border-light);">
                <button id="projPrevBtn" ${currentPage === 1 ? 'disabled' : ''} style="background:transparent; border:none; color: ${currentPage === 1 ? '#4b5563' : '#f3f4f6'}; cursor: ${currentPage === 1 ? 'not-allowed' : 'pointer'}; font-weight:600; font-size: 0.9rem; transition: color 0.2s;">← Previous</button>
                <span style="font-weight: 600; color: #d4af37; font-size: 0.95rem; padding: 0 10px;">Page ${currentPage} of ${totalPages}</span>
                <button id="projNextBtn" ${currentPage === totalPages ? 'disabled' : ''} style="background:transparent; border:none; color: ${currentPage === totalPages ? '#4b5563' : '#f3f4f6'}; cursor: ${currentPage === totalPages ? 'not-allowed' : 'pointer'}; font-weight:600; font-size: 0.9rem; transition: color 0.2s;">Next →</button>
            </div>
        `;

        if(document.getElementById('projPrevBtn')) {
            document.getElementById('projPrevBtn').addEventListener('click', () => { 
                currentPage--; 
                renderProjects(); 
                window.scrollTo(0,0);
            });
        }
        if(document.getElementById('projNextBtn')) {
            document.getElementById('projNextBtn').addEventListener('click', () => { 
                currentPage++; 
                renderProjects(); 
                window.scrollTo(0,0);
            });
        }
    }

    function filterData() {
        const query = searchInput ? searchInput.value.toLowerCase() : '';
        const status = filterStatus ? filterStatus.value : 'All';
        const category = filterCategory ? filterCategory.value : 'All';

        filteredProjects = allProjects.filter(p => {
            const matchQuery = p.name.toLowerCase().includes(query) || 
                               (p.mp_name && p.mp_name.toLowerCase().includes(query)) ||
                               (p.state && p.state.toLowerCase().includes(query));
            const matchStatus = status === 'All' || p.status === status;
            const matchCategory = category === 'All' || p.category === category;
            return matchQuery && matchStatus && matchCategory;
        });
        
        currentPage = 1;
        applyDedupe();
        renderProjects();
    }

    if(searchInput) searchInput.addEventListener('input', filterData);
    if(filterStatus) filterStatus.addEventListener('change', filterData);
    if(filterCategory) filterCategory.addEventListener('change', filterData);
});