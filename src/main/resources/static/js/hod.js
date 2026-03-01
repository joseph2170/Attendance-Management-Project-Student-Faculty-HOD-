// HOD Dashboard JavaScript
let hodData = null;
let apiBaseUrl = 'http://localhost:8080';
let allStudents = [];

document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 HOD dashboard loading...');
    
    // Get user data from session storage
    const userStr = sessionStorage.getItem('user');
    
    if (!userStr) {
        console.log('No user data found, redirecting to login');
        window.location.href = 'login.html';
        return;
    }
    
    try {
        hodData = JSON.parse(userStr);
        console.log('✅ HOD data:', hodData);
        
        // Verify user is HOD
        if (hodData.role !== 'HOD') {
            console.log('User is not HOD, redirecting');
            window.location.href = 'login.html';
            return;
        }
        
        // Update UI with HOD info
        document.getElementById('hodName').textContent = hodData.name || 'HOD';
        document.getElementById('hodDept').textContent = hodData.department || 'Department';
        
        // Initialize date pickers with default values
        initializeDatePickers();
        
        // Load initial data
        loadDashboardData();
        
        // Update date time
        updateDateTime();
        setInterval(updateDateTime, 1000);
        
    } catch (error) {
        console.error('Error parsing user data:', error);
        window.location.href = 'login.html';
    }
});

function initializeDatePickers() {
    const today = new Date().toISOString().split('T')[0];
    const firstDay = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().split('T')[0];
    
    // Report date pickers
    const reportStart = document.getElementById('reportStartDate');
    const reportEnd = document.getElementById('reportEndDate');
    
    if (reportStart) reportStart.value = firstDay;
    if (reportEnd) reportEnd.value = today;
    
    // Faculty attendance date picker
    const facultyDate = document.getElementById('facultyAttendanceDate');
    if (facultyDate) {
        facultyDate.value = today;
        facultyDate.max = today;
    }
    
    // Filter date picker
    const filterDate = document.getElementById('filterDate');
    if (filterDate) filterDate.value = today;
}

function updateDateTime() {
    const now = new Date();
    const dateTimeElement = document.getElementById('currentDateTime');
    if (dateTimeElement) {
        dateTimeElement.textContent = now.toLocaleString();
    }
}

function showSection(sectionId) {
    console.log('Showing section:', sectionId);
    
    // Hide all sections
    document.querySelectorAll('.section').forEach(section => {
        section.style.display = 'none';
    });
    
    // Show selected section
    const selectedSection = document.getElementById(sectionId + '-section');
    if (selectedSection) {
        selectedSection.style.display = 'block';
    } else {
        console.error('Section not found:', sectionId + '-section');
        return;
    }
    
    // Update active link
    document.querySelectorAll('.sidebar-nav a').forEach(link => {
        link.classList.remove('active');
    });
    
    // Find and activate the clicked link
    const activeLink = Array.from(document.querySelectorAll('.sidebar-nav a')).find(
        link => link.getAttribute('onclick')?.includes(sectionId)
    );
    if (activeLink) {
        activeLink.classList.add('active');
    }
    
    // Load section specific data
    switch(sectionId) {
        case 'dashboard':
            loadDashboardData();
            break;
        case 'verify-attendance':
            loadAllPendingVerifications();
            break;
        case 'faculty-attendance':
            loadFacultyAttendance();
            break;
        case 'reports':
            generateReport();
            break;
        case 'faculty':
            loadFacultyList();
            break;
        case 'students':
            loadStudentList();
            break;
        case 'leave-requests':
            loadLeaveRequests();
            break;
        case 'profile':
            loadProfile();
            break;
    }
}

// ========== DASHBOARD FUNCTIONS ==========

async function loadDashboardData() {
    try {
        console.log('Loading dashboard data...');
        
        const statsGrid = document.getElementById('deptStats');
        const pendingBody = document.getElementById('pendingVerificationsBody');
        
        if (!statsGrid || !pendingBody) {
            console.error('Required elements not found');
            return;
        }
        
        statsGrid.innerHTML = `
            <div class="loading-spinner">
                <i class="fas fa-spinner fa-spin"></i>
                <div>Loading dashboard data...</div>
            </div>
        `;
        
        pendingBody.innerHTML = `
            <tr>
                <td colspan="6" class="loading-spinner">
                    <i class="fas fa-spinner fa-spin"></i>
                    <div>Loading pending verifications...</div>
                </td>
            </tr>
        `;
        
        // Fetch pending verifications
        const [pendingRes, studentsRes, facultyRes] = await Promise.all([
            fetch(`${apiBaseUrl}/api/hod/${hodData.userId}/pending-verifications`),
            fetch(`${apiBaseUrl}/api/hod/${hodData.userId}/department/${hodData.department}/students`),
            fetch(`${apiBaseUrl}/api/hod/${hodData.userId}/department/${hodData.department}/faculty`)
        ]);
        
        const pending = await pendingRes.json();
        const students = await studentsRes.json();
        const faculty = await facultyRes.json();
        
        console.log('Pending verifications:', pending);
        
        // Update stats
        statsGrid.innerHTML = `
            <div class="stat-card">
                <div class="stat-icon" style="background: #667eea;">
                    <i class="fas fa-clock"></i>
                </div>
                <div class="stat-info">
                    <h3>Pending Verifications</h3>
                    <p>${pending.total || 0}</p>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon" style="background: #28a745;">
                    <i class="fas fa-users"></i>
                </div>
                <div class="stat-info">
                    <h3>Total Students</h3>
                    <p>${students.total || 0}</p>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon" style="background: #ffc107;">
                    <i class="fas fa-chalkboard-teacher"></i>
                </div>
                <div class="stat-info">
                    <h3>Total Faculty</h3>
                    <p>${faculty.total || 0}</p>
                </div>
            </div>
        `;
        
        // Update pending verifications table
        pendingBody.innerHTML = '';
        
        if (!pending.pendingVerifications || pending.pendingVerifications.length === 0) {
            pendingBody.innerHTML = '<tr><td colspan="6" class="no-data">No pending verifications</td></tr>';
            return;
        }
        
        // Show only first 5 for dashboard
        pending.pendingVerifications.slice(0, 5).forEach(att => {
            const row = document.createElement('tr');
            const statusClass = att.status === 'PRESENT' ? 'status-present' : 
                              att.status === 'LATE' ? 'status-late' : 'status-absent';
            
            row.innerHTML = `
                <td>${att.student?.name || 'N/A'}</td>
                <td>${att.subject?.name || 'N/A'}</td>
                <td>${formatDate(att.date)}</td>
                <td>
                    <span class="status-badge ${statusClass}">
                        ${att.status}
                    </span>
                </td>
                <td>${att.markedBy?.name || 'N/A'}</td>
                <td>
                    <button class="btn btn-primary btn-sm" onclick="verifyAttendance(${att.id})">
                        <i class="fas fa-check"></i> Verify
                    </button>
                </td>
            `;
            pendingBody.appendChild(row);
        });
        
    } catch (error) {
        console.error('Error loading dashboard data:', error);
        showError('Failed to load dashboard data. Please refresh.');
    }
}

// ========== VERIFY ATTENDANCE FUNCTIONS ==========

async function loadAllPendingVerifications() {
    try {
        console.log('Loading all pending verifications...');
        
        const tbody = document.getElementById('allPendingVerificationsBody');
        if (!tbody) return;
        
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="loading-spinner">
                    <i class="fas fa-spinner fa-spin"></i>
                    <div>Loading pending verifications...</div>
                </td>
            </tr>
        `;
        
        const response = await fetch(`${apiBaseUrl}/api/hod/${hodData.userId}/pending-verifications`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        console.log('All pending verifications:', data);
        
        tbody.innerHTML = '';
        
        if (!data.pendingVerifications || data.pendingVerifications.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="no-data">No pending verifications</td></tr>';
            return;
        }
        
        // Group by date
        const groupedByDate = {};
        data.pendingVerifications.forEach(att => {
            const date = att.date;
            if (!groupedByDate[date]) {
                groupedByDate[date] = [];
            }
            groupedByDate[date].push(att);
        });
        
        // Sort dates in descending order
        const sortedDates = Object.keys(groupedByDate).sort((a, b) => new Date(b) - new Date(a));
        
        for (const date of sortedDates) {
            // Add date header
            const dateHeader = document.createElement('tr');
            dateHeader.className = 'date-header';
            dateHeader.innerHTML = `<td colspan="7"><strong>${formatDate(date)}</strong> (${groupedByDate[date].length} records)</td>`;
            tbody.appendChild(dateHeader);
            
            // Add attendance records for this date
            groupedByDate[date].forEach(att => {
                const row = document.createElement('tr');
                const statusClass = att.status === 'PRESENT' ? 'status-present' : 
                                  att.status === 'LATE' ? 'status-late' : 'status-absent';
                
                row.innerHTML = `
                    <td class="checkbox-col">
                        <input type="checkbox" class="verify-checkbox" value="${att.id}">
                    </td>
                    <td>${att.student?.name || 'N/A'} (${att.student?.rollNumber || ''})</td>
                    <td>${att.subject?.name || 'N/A'}</td>
                    <td>${formatDate(att.date)}</td>
                    <td>
                        <span class="status-badge ${statusClass}">
                            ${att.status}
                        </span>
                    </td>
                    <td>${att.markedBy?.name || 'N/A'}</td>
                    <td>
                        <button class="btn btn-primary btn-sm" onclick="verifyAttendance(${att.id})">
                            <i class="fas fa-check"></i>
                        </button>
                    </td>
                `;
                tbody.appendChild(row);
            });
        }
        
        // Add controls row
        const controlsRow = document.createElement('tr');
        controlsRow.className = 'controls-row';
        controlsRow.innerHTML = `
            <td colspan="7">
                <div style="display: flex; gap: 10px; padding: 10px; flex-wrap: wrap;">
                    <button class="btn btn-secondary" onclick="selectAll()">
                        <i class="fas fa-check-double"></i> Select All
                    </button>
                    <button class="btn btn-secondary" onclick="deselectAll()">
                        <i class="fas fa-times"></i> Deselect All
                    </button>
                    <button class="btn btn-success" onclick="verifySelected()">
                        <i class="fas fa-check-circle"></i> Verify Selected
                    </button>
                    <button class="btn btn-primary" onclick="verifyAll()">
                        <i class="fas fa-check-double"></i> Verify All
                    </button>
                </div>
            </td>
        `;
        tbody.appendChild(controlsRow);
        
    } catch (error) {
        console.error('Error loading pending verifications:', error);
        const tbody = document.getElementById('allPendingVerificationsBody');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="7" class="error-message">Error loading data. Please refresh.</td></tr>';
        }
    }
}

async function verifyAttendance(attendanceId) {
    try {
        console.log('Verifying attendance:', attendanceId);
        
        const response = await fetch(`${apiBaseUrl}/api/hod/attendance/verify/${attendanceId}`, {
            method: 'POST'
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const result = await response.json();
        
        if (result.success) {
            showNotification('✅ Attendance verified successfully', 'success');
            // Reload both dashboard and verification page
            loadDashboardData();
            if (document.getElementById('verify-attendance-section').style.display !== 'none') {
                loadAllPendingVerifications();
            }
        } else {
            showNotification('❌ Error: ' + result.message, 'error');
        }
        
    } catch (error) {
        console.error('Error verifying attendance:', error);
        showNotification('❌ Error verifying attendance', 'error');
    }
}

function selectAll() {
    document.querySelectorAll('.verify-checkbox').forEach(cb => {
        cb.checked = true;
    });
}

function deselectAll() {
    document.querySelectorAll('.verify-checkbox').forEach(cb => {
        cb.checked = false;
    });
}

async function verifySelected() {
    const checkboxes = document.querySelectorAll('.verify-checkbox:checked');
    
    if (checkboxes.length === 0) {
        showNotification('Please select at least one record', 'warning');
        return;
    }
    
    const attendanceIds = Array.from(checkboxes).map(cb => parseInt(cb.value));
    
    if (!confirm(`Verify ${attendanceIds.length} selected attendance records?`)) {
        return;
    }
    
    try {
        let successCount = 0;
        let failCount = 0;
        
        for (const id of attendanceIds) {
            try {
                const response = await fetch(`${apiBaseUrl}/api/hod/attendance/verify/${id}`, {
                    method: 'POST'
                });
                const result = await response.json();
                if (result.success) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (e) {
                failCount++;
            }
        }
        
        showNotification(`✅ Verified: ${successCount}, Failed: ${failCount}`, 'success');
        loadAllPendingVerifications();
        loadDashboardData();
        
    } catch (error) {
        console.error('Error verifying selected:', error);
        showNotification('❌ Error verifying selected records', 'error');
    }
}

async function verifyAll() {
    if (!confirm('Verify ALL pending attendance records?')) {
        return;
    }
    
    try {
        const response = await fetch(`${apiBaseUrl}/api/hod/attendance/verify-all`, {
            method: 'POST'
        });
        
        const result = await response.json();
        
        if (result.success) {
            showNotification(`✅ ${result.message}`, 'success');
            loadAllPendingVerifications();
            loadDashboardData();
        } else {
            showNotification('❌ Error: ' + result.message, 'error');
        }
        
    } catch (error) {
        console.error('Error verifying all:', error);
        showNotification('❌ Error verifying all attendance', 'error');
    }
}

function applyFilters() {
    const filterDate = document.getElementById('filterDate')?.value;
    const filterStatus = document.getElementById('filterStatus')?.value;
    
    showNotification(`Filtering by Date: ${filterDate || 'All'}, Status: ${filterStatus}`, 'info');
    // Implement actual filter logic here if needed
}

function toggleAll() {
    const selectAllCheckbox = document.getElementById('selectAll');
    const checkboxes = document.querySelectorAll('.verify-checkbox');
    checkboxes.forEach(cb => {
        cb.checked = selectAllCheckbox.checked;
    });
}

// ========== FACULTY ATTENDANCE FUNCTIONS ==========

async function loadFacultyAttendance() {
    try {
        console.log('Loading faculty attendance...');
        
        const container = document.getElementById('facultyAttendanceContainer');
        if (!container) return;
        
        const date = document.getElementById('facultyAttendanceDate')?.value || new Date().toISOString().split('T')[0];
        
        container.innerHTML = `
            <div class="loading-spinner">
                <i class="fas fa-spinner fa-spin"></i>
                <div>Loading faculty list...</div>
            </div>
        `;
        
        const response = await fetch(`${apiBaseUrl}/api/hod/${hodData.userId}/department/${hodData.department}/faculty-attendance-list?date=${date}`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        console.log('Faculty attendance data:', data);
        
        let html = `
            <div class="summary-card">
                <h3>Faculty Attendance - ${formatDate(date)}</h3>
                <div class="summary-stats">
                    <div class="summary-stat">
                        <h4>Total Faculty</h4>
                        <p>${data.total || 0}</p>
                    </div>
                    <div class="summary-stat">
                        <h4>Marked</h4>
                        <p>${data.markedCount || 0}</p>
                    </div>
                    <div class="summary-stat">
                        <h4>Pending</h4>
                        <p>${data.pendingCount || 0}</p>
                    </div>
                </div>
            </div>
            
            <div class="mark-all-buttons">
                <button class="btn-mark btn-mark-present" onclick="markAllFacultyPresent()">
                    <i class="fas fa-check-circle"></i> Mark All Present
                </button>
                <button class="btn-mark btn-mark-absent" onclick="markAllFacultyAbsent()">
                    <i class="fas fa-times-circle"></i> Mark All Absent
                </button>
                <button class="btn-mark btn-mark-leave" onclick="markAllFacultyLeave()">
                    <i class="fas fa-calendar-minus"></i> Mark All Leave
                </button>
                <button class="btn-mark btn-mark-late" onclick="markAllFacultyLate()">
                    <i class="fas fa-clock"></i> Mark All Late
                </button>
            </div>
            
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Designation</th>
                            <th>Status</th>
                            <th>Remarks</th>
                        </tr>
                    </thead>
                    <tbody>
        `;
        
        if (!data.faculty || data.faculty.length === 0) {
            html += `
                <tr>
                    <td colspan="4" class="no-data">No faculty found</td>
                </tr>
            `;
        } else {
            data.faculty.forEach(faculty => {
                const statusClass = faculty.status === 'PRESENT' ? 'status-present' :
                                  faculty.status === 'ABSENT' ? 'status-absent' :
                                  faculty.status === 'LEAVE' ? 'status-leave' : 'status-late';
                
                html += `
                    <tr>
                        <td><strong>${faculty.name}</strong><br><small>${faculty.email}</small></td>
                        <td>${faculty.designation}</td>
                        <td>
                            <select class="status-select ${statusClass}" data-faculty-id="${faculty.id}">
                                <option value="PRESENT" ${faculty.status === 'PRESENT' ? 'selected' : ''}>✅ PRESENT</option>
                                <option value="ABSENT" ${faculty.status === 'ABSENT' ? 'selected' : ''}>❌ ABSENT</option>
                                <option value="LEAVE" ${faculty.status === 'LEAVE' ? 'selected' : ''}>📅 LEAVE</option>
                                <option value="LATE" ${faculty.status === 'LATE' ? 'selected' : ''}>⏰ LATE</option>
                            </select>
                        </td>
                        <td>
                            <input type="text" class="remarks-input" data-faculty-id="${faculty.id}" 
                                   placeholder="Remarks (optional)" value="${faculty.remarks || ''}">
                        </td>
                    </tr>
                `;
            });
        }
        
        html += `
                    </tbody>
                </table>
            </div>
            
            <button class="btn-submit" onclick="submitFacultyAttendance()" style="margin-top: 20px; width: 100%;">
                <i class="fas fa-save"></i> SUBMIT FACULTY ATTENDANCE
            </button>
        `;
        
        container.innerHTML = html;
        
    } catch (error) {
        console.error('Error loading faculty attendance:', error);
        const container = document.getElementById('facultyAttendanceContainer');
        if (container) {
            container.innerHTML = '<div class="error-message">Error loading faculty attendance. Please refresh.</div>';
        }
    }
}

function markAllFacultyPresent() {
    document.querySelectorAll('.status-select').forEach(select => {
        select.value = 'PRESENT';
        select.classList.remove('status-absent', 'status-leave', 'status-late');
        select.classList.add('status-present');
    });
    showNotification('All faculty marked as PRESENT', 'success');
}

function markAllFacultyAbsent() {
    document.querySelectorAll('.status-select').forEach(select => {
        select.value = 'ABSENT';
        select.classList.remove('status-present', 'status-leave', 'status-late');
        select.classList.add('status-absent');
    });
    showNotification('All faculty marked as ABSENT', 'warning');
}

function markAllFacultyLeave() {
    document.querySelectorAll('.status-select').forEach(select => {
        select.value = 'LEAVE';
        select.classList.remove('status-present', 'status-absent', 'status-late');
        select.classList.add('status-leave');
    });
    showNotification('All faculty marked as LEAVE', 'info');
}

function markAllFacultyLate() {
    document.querySelectorAll('.status-select').forEach(select => {
        select.value = 'LATE';
        select.classList.remove('status-present', 'status-absent', 'status-leave');
        select.classList.add('status-late');
    });
    showNotification('All faculty marked as LATE', 'info');
}

async function submitFacultyAttendance() {
    const date = document.getElementById('facultyAttendanceDate')?.value || new Date().toISOString().split('T')[0];
    
    const statusSelects = document.querySelectorAll('.status-select');
    const remarksInputs = document.querySelectorAll('.remarks-input');
    
    const attendanceData = [];
    
    statusSelects.forEach(select => {
        const facultyId = parseInt(select.dataset.facultyId);
        const status = select.value;
        
        let remarks = '';
        remarksInputs.forEach(input => {
            if (parseInt(input.dataset.facultyId) === facultyId) {
                remarks = input.value;
            }
        });
        
        attendanceData.push({
            facultyId: facultyId,
            status: status,
            remarks: remarks
        });
    });
    
    if (attendanceData.length === 0) {
        showNotification('No faculty to mark attendance for', 'warning');
        return;
    }
    
    const presentCount = attendanceData.filter(a => a.status === 'PRESENT').length;
    const absentCount = attendanceData.filter(a => a.status === 'ABSENT').length;
    const leaveCount = attendanceData.filter(a => a.status === 'LEAVE').length;
    const lateCount = attendanceData.filter(a => a.status === 'LATE').length;
    
    const confirmMessage = `Faculty Attendance Summary for ${formatDate(date)}:
    
✅ PRESENT: ${presentCount}
❌ ABSENT: ${absentCount}
📅 LEAVE: ${leaveCount}
⏰ LATE: ${lateCount}
📝 TOTAL: ${attendanceData.length}

Do you want to submit?`;
    
    if (!confirm(confirmMessage)) {
        return;
    }
    
    try {
        const submitBtn = document.querySelector('.btn-submit');
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Submitting...';
        }
        
        const response = await fetch(`${apiBaseUrl}/api/hod/faculty-attendance/mark-bulk`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                hodId: hodData.userId,
                date: date,
                attendance: attendanceData
            })
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const result = await response.json();
        
        if (result.success) {
            showNotification(`✅ Faculty attendance submitted!\nMarked: ${result.marked}, Updated: ${result.updated}`, 'success');
            loadFacultyAttendance(); // Reload to show updated status
        } else {
            showNotification('❌ Error: ' + result.message, 'error');
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<i class="fas fa-save"></i> SUBMIT FACULTY ATTENDANCE';
            }
        }
        
    } catch (error) {
        console.error('Error submitting faculty attendance:', error);
        showNotification('❌ Error submitting attendance', 'error');
        
        const submitBtn = document.querySelector('.btn-submit');
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<i class="fas fa-save"></i> SUBMIT FACULTY ATTENDANCE';
        }
    }
}

function viewFacultyAttendanceReport() {
    showSection('reports');
    document.getElementById('reportType').value = 'faculty';
    generateReport();
}

// ========== REPORT FUNCTIONS ==========

async function generateReport() {
    const startDate = document.getElementById('reportStartDate')?.value;
    const endDate = document.getElementById('reportEndDate')?.value;
    const reportType = document.getElementById('reportType')?.value;
    
    if (!startDate || !endDate) {
        showNotification('Please select date range', 'warning');
        return;
    }
    
    try {
        const tbody = document.getElementById('reportTableBody');
        const summaryDiv = document.getElementById('reportSummary');
        const tableHeader = document.getElementById('reportTableHeader');
        
        if (!tbody || !summaryDiv || !tableHeader) return;
        
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="loading-spinner">
                    <i class="fas fa-spinner fa-spin"></i>
                    <div>Generating report...</div>
                </td>
            </tr>
        `;
        
        summaryDiv.style.display = 'none';
        
        let url;
        if (reportType === 'faculty') {
            url = `${apiBaseUrl}/api/hod/${hodData.userId}/reports/faculty?startDate=${startDate}&endDate=${endDate}`;
            tableHeader.innerHTML = `
                <tr>
                    <th>Date</th>
                    <th>Faculty</th>
                    <th>Status</th>
                    <th>Remarks</th>
                </tr>
            `;
        } else if (reportType === 'student') {
            url = `${apiBaseUrl}/api/hod/${hodData.userId}/reports/student?startDate=${startDate}&endDate=${endDate}`;
            tableHeader.innerHTML = `
                <tr>
                    <th>Date</th>
                    <th>Student</th>
                    <th>Subject</th>
                    <th>Status</th>
                    <th>Verified</th>
                </tr>
            `;
        } else if (reportType === 'department') {
            url = `${apiBaseUrl}/api/hod/${hodData.userId}/reports/department-summary?startDate=${startDate}&endDate=${endDate}`;
            tableHeader.innerHTML = `
                <tr>
                    <th>Semester</th>
                    <th>Students</th>
                    <th>Total Attendance</th>
                    <th>Present</th>
                    <th>Absent</th>
                    <th>Late</th>
                    <th>Percentage</th>
                </tr>
            `;
        }
        
        console.log('Fetching report:', url);
        
        const response = await fetch(url);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        console.log('Report data:', data);
        
        if (!data.success) {
            tbody.innerHTML = `<tr><td colspan="7" class="error-message">${data.message}</td></tr>`;
            return;
        }
        
        summaryDiv.style.display = 'block';
        
        if (reportType === 'student') {
            displayStudentReport(data);
        } else if (reportType === 'faculty') {
            displayFacultyReport(data);
        } else if (reportType === 'department') {
            displayDepartmentReport(data);
        }
        
    } catch (error) {
        console.error('Error generating report:', error);
        showNotification('Error generating report: ' + error.message, 'error');
        
        const tbody = document.getElementById('reportTableBody');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="7" class="error-message">Error generating report</td></tr>';
        }
    }
}

function displayStudentReport(data) {
    const tbody = document.getElementById('reportTableBody');
    const summaryDiv = document.getElementById('reportSummary');
    
    summaryDiv.innerHTML = `
        <h3>Student Attendance Report</h3>
        <p><i class="fas fa-calendar"></i> ${formatDate(data.startDate)} to ${formatDate(data.endDate)}</p>
        <div class="summary-stats">
            <div class="summary-stat">
                <h4>Total Records</h4>
                <p>${data.summary.total}</p>
            </div>
            <div class="summary-stat">
                <h4>Present</h4>
                <p style="color: #28a745;">${data.summary.present}</p>
            </div>
            <div class="summary-stat">
                <h4>Absent</h4>
                <p style="color: #dc3545;">${data.summary.absent}</p>
            </div>
            <div class="summary-stat">
                <h4>Late</h4>
                <p style="color: #ffc107;">${data.summary.late}</p>
            </div>
            <div class="summary-stat">
                <h4>Verified</h4>
                <p style="color: #17a2b8;">${data.summary.verified}</p>
            </div>
            <div class="summary-stat">
                <h4>Pending</h4>
                <p style="color: #fd7e14;">${data.summary.pending}</p>
            </div>
        </div>
    `;
    
    tbody.innerHTML = '';
    
    if (!data.records || data.records.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="no-data">No data found for selected period</td></tr>';
        return;
    }
    
    data.records.forEach(record => {
        const row = document.createElement('tr');
        const statusClass = record.status === 'PRESENT' ? 'status-present' :
                          record.status === 'LATE' ? 'status-late' : 'status-absent';
        
        row.innerHTML = `
            <td>${formatDate(record.date)}</td>
            <td>
                <strong>${record.student.name}</strong><br>
                <small>${record.student.rollNumber} | Sem ${record.student.semester}</small>
            </td>
            <td>${record.subject?.name || 'N/A'}</td>
            <td>
                <span class="status-badge ${statusClass}">
                    ${record.status}
                </span>
            </td>
            <td>
                <span class="status-badge ${record.verified ? 'status-present' : 'status-absent'}">
                    ${record.verified ? '✓ Verified' : '⏳ Pending'}
                </span>
            </td>
        `;
        tbody.appendChild(row);
    });
}

function displayFacultyReport(data) {
    const tbody = document.getElementById('reportTableBody');
    const summaryDiv = document.getElementById('reportSummary');
    
    summaryDiv.innerHTML = `
        <h3>Faculty Attendance Report</h3>
        <p><i class="fas fa-calendar"></i> ${formatDate(data.startDate)} to ${formatDate(data.endDate)}</p>
        <div class="summary-stats">
            <div class="summary-stat">
                <h4>Total Records</h4>
                <p>${data.summary.total}</p>
            </div>
            <div class="summary-stat">
                <h4>Present</h4>
                <p style="color: #28a745;">${data.summary.present}</p>
            </div>
            <div class="summary-stat">
                <h4>Absent</h4>
                <p style="color: #dc3545;">${data.summary.absent}</p>
            </div>
            <div class="summary-stat">
                <h4>Leave</h4>
                <p style="color: #17a2b8;">${data.summary.leave}</p>
            </div>
            <div class="summary-stat">
                <h4>Late</h4>
                <p style="color: #ffc107;">${data.summary.late}</p>
            </div>
        </div>
        
        <h4 style="margin-top: 20px; margin-bottom: 10px;">Faculty-wise Summary</h4>
    `;
    
    if (data.facultySummary && data.facultySummary.length > 0) {
        let facultyCards = '<div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 15px; margin-top: 15px;">';
        
        data.facultySummary.forEach(fac => {
            let percentClass = 'status-present';
            if (fac.percentage < 75) percentClass = 'status-late';
            if (fac.percentage < 60) percentClass = 'status-absent';
            
            facultyCards += `
                <div style="background: white; padding: 15px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                    <strong>${fac.facultyName}</strong><br>
                    <small style="color: #666;">${fac.designation}</small>
                    <div style="display: flex; justify-content: space-between; margin: 10px 0;">
                        <span>Present: <strong style="color: #28a745;">${fac.present}</strong></span>
                        <span>Absent: <strong style="color: #dc3545;">${fac.absent}</strong></span>
                        <span>Leave: <strong style="color: #17a2b8;">${fac.leave}</strong></span>
                        <span>Late: <strong style="color: #ffc107;">${fac.late}</strong></span>
                    </div>
                    <div class="progress-bar" style="margin: 10px 0;">
                        <div class="progress-fill ${percentClass}" style="width: ${fac.percentage}%"></div>
                    </div>
                    <div style="display: flex; justify-content: space-between;">
                        <span>Total Classes: ${fac.total}</span>
                        <span class="status-badge ${percentClass}">${fac.percentage}%</span>
                    </div>
                </div>
            `;
        });
        
        facultyCards += '</div>';
        summaryDiv.innerHTML += facultyCards;
    }
    
    tbody.innerHTML = '';
    
    if (!data.records || data.records.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="no-data">No data found for selected period</td></tr>';
        return;
    }
    
    data.records.forEach(record => {
        const row = document.createElement('tr');
        const statusClass = record.status === 'PRESENT' ? 'status-present' :
                          record.status === 'ABSENT' ? 'status-absent' :
                          record.status === 'LEAVE' ? 'status-leave' : 'status-late';
        
        row.innerHTML = `
            <td>${formatDate(record.date)}</td>
            <td>
                <strong>${record.faculty.name}</strong><br>
                <small>${record.faculty.designation}</small>
            </td>
            <td>
                <span class="status-badge ${statusClass}">
                    ${record.status}
                </span>
            </td>
            <td>${record.remarks || '-'}</td>
        `;
        tbody.appendChild(row);
    });
}

function displayDepartmentReport(data) {
    const tbody = document.getElementById('reportTableBody');
    const summaryDiv = document.getElementById('reportSummary');
    
    summaryDiv.innerHTML = `
        <h3>Department Summary Report</h3>
        <p><i class="fas fa-calendar"></i> ${formatDate(data.startDate)} to ${formatDate(data.endDate)}</p>
        <p><i class="fas fa-building"></i> Department: ${data.department}</p>
        <div class="summary-stats">
            <div class="summary-stat">
                <h4>Total Students</h4>
                <p>${data.totalStudents}</p>
            </div>
            <div class="summary-stat">
                <h4>Total Attendance</h4>
                <p>${data.totalAttendance}</p>
            </div>
        </div>
    `;
    
    tbody.innerHTML = '';
    
    if (!data.semesterSummary || Object.keys(data.semesterSummary).length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="no-data">No data found for selected period</td></tr>';
        return;
    }
    
    const sortedSemesters = Object.keys(data.semesterSummary).sort((a, b) => parseInt(a) - parseInt(b));
    
    sortedSemesters.forEach(sem => {
        const semData = data.semesterSummary[sem];
        const percentage = semData.percentage || 0;
        let percentClass = 'status-present';
        
        if (percentage < 75) percentClass = 'status-late';
        if (percentage < 60) percentClass = 'status-absent';
        
        const row = document.createElement('tr');
        row.innerHTML = `
            <td><strong>Semester ${sem}</strong></td>
            <td>${semData.totalStudents}</td>
            <td>${semData.totalAttendance}</td>
            <td>${semData.present}</td>
            <td>${semData.absent}</td>
            <td>${semData.late}</td>
            <td>
                <span class="status-badge ${percentClass}">
                    ${percentage}%
                </span>
            </td>
        `;
        tbody.appendChild(row);
    });
}

async function exportReport() {
    const startDate = document.getElementById('reportStartDate')?.value;
    const endDate = document.getElementById('reportEndDate')?.value;
    const reportType = document.getElementById('reportType')?.value;
    
    try {
        showNotification('Preparing export...', 'info');
        
        const response = await fetch(`${apiBaseUrl}/api/hod/${hodData.userId}/reports/export?type=${reportType}&startDate=${startDate}&endDate=${endDate}`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        
        if (data.success) {
            const blob = new Blob([data.data], { type: 'text/csv' });
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = data.filename;
            a.click();
            
            showNotification('Report exported successfully', 'success');
        } else {
            showNotification('Error exporting report: ' + data.message, 'error');
        }
        
    } catch (error) {
        console.error('Error exporting report:', error);
        showNotification('Error exporting report', 'error');
    }
}

// ========== FACULTY LIST FUNCTIONS ==========

async function loadFacultyList() {
    try {
        console.log('Loading faculty list...');
        
        const tbody = document.getElementById('facultyListBody');
        if (!tbody) return;
        
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="loading-spinner">
                    <i class="fas fa-spinner fa-spin"></i>
                    <div>Loading faculty...</div>
                </td>
            </tr>
        `;
        
        const response = await fetch(`${apiBaseUrl}/api/hod/${hodData.userId}/department/${hodData.department}/faculty`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        console.log('Faculty list:', data);
        
        tbody.innerHTML = '';
        
        if (!data.faculty || data.faculty.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="no-data">No faculty found</td></tr>';
            return;
        }
        
        data.faculty.forEach(f => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td><strong>${f.name}</strong></td>
                <td>${f.email}</td>
                <td>${f.designation}</td>
                <td>${f.phoneNumber || 'N/A'}</td>
                <td>
                    <span class="badge badge-info">${f.classCount || 0} classes</span>
                </td>
            `;
            tbody.appendChild(row);
        });
        
    } catch (error) {
        console.error('Error loading faculty list:', error);
        const tbody = document.getElementById('facultyListBody');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="5" class="error-message">Error loading faculty</td></tr>';
        }
    }
}

// ========== STUDENT LIST FUNCTIONS ==========

async function loadStudentList() {
    try {
        console.log('Loading student list...');
        
        const tbody = document.getElementById('studentsListBody');
        if (!tbody) return;
        
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="loading-spinner">
                    <i class="fas fa-spinner fa-spin"></i>
                    <div>Loading students...</div>
                </td>
            </tr>
        `;
        
        const response = await fetch(`${apiBaseUrl}/api/hod/${hodData.userId}/department/${hodData.department}/students`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        console.log('Student list:', data);
        
        allStudents = data.students || [];
        
        displayFilteredStudents(allStudents);
        
    } catch (error) {
        console.error('Error loading student list:', error);
        const tbody = document.getElementById('studentsListBody');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="6" class="error-message">Error loading students</td></tr>';
        }
    }
}

function displayFilteredStudents(students) {
    const tbody = document.getElementById('studentsListBody');
    if (!tbody) return;
    
    tbody.innerHTML = '';
    
    if (students.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="no-data">No students found</td></tr>';
        return;
    }
    
    students.forEach(student => {
        const row = document.createElement('tr');
        const percentage = student.attendancePercentage || 0;
        let statusClass = 'badge-success';
        let statusText = 'Good';
        
        if (percentage < 75) {
            statusClass = 'badge-warning';
            statusText = 'Warning';
        }
        if (percentage < 60) {
            statusClass = 'badge-danger';
            statusText = 'Critical';
        }
        
        row.innerHTML = `
            <td>${student.rollNumber}</td>
            <td><strong>${student.name}</strong></td>
            <td>${student.email}</td>
            <td>Semester ${student.semester}</td>
            <td>${student.phoneNumber || 'N/A'}</td>
            <td>
                <span class="badge ${statusClass}">${statusText}</span>
                <small style="display: block; font-size: 11px;">${percentage}%</small>
            </td>
        `;
        tbody.appendChild(row);
    });
}

function filterStudents() {
    const semesterFilter = document.getElementById('studentSemesterFilter')?.value;
    const searchFilter = document.getElementById('studentSearchFilter')?.value?.toLowerCase() || '';
    
    let filtered = allStudents;
    
    if (semesterFilter) {
        filtered = filtered.filter(s => s.semester == semesterFilter);
    }
    
    if (searchFilter) {
        filtered = filtered.filter(s => 
            s.name?.toLowerCase().includes(searchFilter) ||
            s.rollNumber?.toLowerCase().includes(searchFilter)
        );
    }
    
    displayFilteredStudents(filtered);
}

// ========== LEAVE REQUESTS FUNCTIONS ==========

async function loadLeaveRequests() {
    try {
        console.log('Loading leave requests...');
        
        const tbody = document.getElementById('leaveRequestsBody');
        if (!tbody) return;
        
        tbody.innerHTML = `
            <tr>
                <td colspan="9" class="loading-spinner">
                    <i class="fas fa-spinner fa-spin"></i>
                    <div>Loading leave requests...</div>
                </td>
            </tr>
        `;
        
        const response = await fetch(`${apiBaseUrl}/api/hod/${hodData.userId}/leave-requests/pending`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        console.log('Leave requests:', data);
        
        tbody.innerHTML = '';
        
        if (!data.leaveRequests || data.leaveRequests.length === 0) {
            tbody.innerHTML = '<tr><td colspan="9" class="no-data">No pending leave requests</td></tr>';
            return;
        }
        
        data.leaveRequests.forEach(request => {
            const row = document.createElement('tr');
            
            row.innerHTML = `
                <td><strong>${request.facultyName}</strong><br><small>${request.facultyEmail}</small></td>
                <td>${formatDate(request.fromDate)}</td>
                <td>${formatDate(request.toDate)}</td>
                <td>${request.days}</td>
                <td>${request.leaveType}</td>
                <td>${request.reason}</td>
                <td>${formatDate(request.appliedAt)}</td>
                <td>
                    <span class="status-badge status-pending">PENDING</span>
                </td>
                <td>
                    <button class="btn btn-success btn-sm" onclick="approveLeave(${request.id}, '${request.facultyName}')">
                        <i class="fas fa-check"></i> Approve
                    </button>
                    <button class="btn btn-danger btn-sm" onclick="rejectLeave(${request.id}, '${request.facultyName}')">
                        <i class="fas fa-times"></i> Reject
                    </button>
                </td>
            `;
            tbody.appendChild(row);
        });
        
    } catch (error) {
        console.error('Error loading leave requests:', error);
        const tbody = document.getElementById('leaveRequestsBody');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="9" class="error-message">Error loading leave requests</td></tr>';
        }
    }
}

async function approveLeave(leaveId, facultyName) {
    const remarks = prompt(`Enter remarks for approving ${facultyName}'s leave request:`, "Approved");
    if (remarks === null) return;
    
    try {
        const response = await fetch(`${apiBaseUrl}/api/hod/leave-requests/approve/${leaveId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                hodId: hodData.userId,
                remarks: remarks
            })
        });
        
        const result = await response.json();
        
        if (result.success) {
            alert('✅ Leave request approved');
            loadLeaveRequests();
        } else {
            alert('❌ Error: ' + result.message);
        }
        
    } catch (error) {
        console.error('Error approving leave:', error);
        alert('❌ Error approving leave');
    }
}

async function rejectLeave(leaveId, facultyName) {
    const remarks = prompt(`Enter reason for rejecting ${facultyName}'s leave request:`, "Request denied");
    if (remarks === null) return;
    
    try {
        const response = await fetch(`${apiBaseUrl}/api/hod/leave-requests/reject/${leaveId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                hodId: hodData.userId,
                remarks: remarks
            })
        });
        
        const result = await response.json();
        
        if (result.success) {
            alert('✅ Leave request rejected');
            loadLeaveRequests();
        } else {
            alert('❌ Error: ' + result.message);
        }
        
    } catch (error) {
        console.error('Error rejecting leave:', error);
        alert('❌ Error rejecting leave');
    }
}

// ========== PROFILE FUNCTIONS ==========

function loadProfile() {
    const profileDiv = document.getElementById('profileDetails');
    if (!profileDiv) return;
    
    profileDiv.innerHTML = `
        <div class="profile-card">
            <div class="profile-header">
                <i class="fas fa-user-tie"></i>
                <h3>${hodData.name}</h3>
                <p>Head of Department</p>
            </div>
            <div class="profile-details">
                <p><i class="fas fa-envelope"></i> <strong>Email:</strong> ${hodData.email}</p>
                <p><i class="fas fa-building"></i> <strong>Department:</strong> ${hodData.department || 'N/A'}</p>
                <p><i class="fas fa-phone"></i> <strong>Phone:</strong> ${hodData.phoneNumber || 'Not provided'}</p>
                <p><i class="fas fa-id-card"></i> <strong>Employee ID:</strong> ${hodData.userId || 'N/A'}</p>
            </div>
        </div>
    `;
}

// ========== UTILITY FUNCTIONS ==========

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    try {
        const date = new Date(dateString);
        if (isNaN(date.getTime())) return dateString;
        
        const options = { year: 'numeric', month: 'short', day: 'numeric' };
        return date.toLocaleDateString(undefined, options);
    } catch (error) {
        return dateString;
    }
}

function showNotification(message, type = 'info') {
    console.log('Notification:', message, type);
    
    const notification = document.createElement('div');
    notification.className = type === 'success' ? 'success-message' : 
                             type === 'error' ? 'error-message' : 
                             type === 'warning' ? 'warning-message' : 'info-message';
    
    notification.style.position = 'fixed';
    notification.style.top = '20px';
    notification.style.right = '20px';
    notification.style.zIndex = '1000';
    notification.style.maxWidth = '400px';
    notification.style.padding = '15px 20px';
    notification.style.borderRadius = '5px';
    notification.style.boxShadow = '0 4px 6px rgba(0,0,0,0.1)';
    notification.style.animation = 'slideIn 0.3s ease';
    
    notification.innerHTML = message.replace(/\n/g, '<br>');
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => {
            notification.remove();
        }, 300);
    }, 5000);
}

function showError(message) {
    showNotification(message, 'error');
}

function logout() {
    if (confirm('Are you sure you want to logout?')) {
        sessionStorage.removeItem('user');
        sessionStorage.removeItem('token');
        window.location.href = 'login.html';
    }
}

// Add CSS animations if not already present
if (!document.getElementById('hod-dashboard-styles')) {
    const style = document.createElement('style');
    style.id = 'hod-dashboard-styles';
    style.textContent = `
        @keyframes slideIn {
            from {
                transform: translateX(100%);
                opacity: 0;
            }
            to {
                transform: translateX(0);
                opacity: 1;
            }
        }
        
        @keyframes slideOut {
            from {
                transform: translateX(0);
                opacity: 1;
            }
            to {
                transform: translateX(100%);
                opacity: 0;
            }
        }
        
        .badge-success {
            background: #d4edda;
            color: #155724;
        }
        
        .badge-warning {
            background: #fff3cd;
            color: #856404;
        }
        
        .badge-danger {
            background: #f8d7da;
            color: #721c24;
        }
        
        .badge-info {
            background: #d1ecf1;
            color: #0c5460;
        }
        
        .status-leave {
            background: #cce5ff;
            color: #004085;
        }
        
        .btn-mark-leave {
            background: #cce5ff;
            color: #004085;
            border: 1px solid #b8daff;
        }
        
        .btn-mark-leave:hover {
            background: #b8daff;
        }
        
        .checkbox-col {
            width: 40px;
            text-align: center;
        }
        
        .date-header {
            background: #e9ecef;
            font-weight: bold;
        }
        
        .controls-row {
            background: #f8f9fa;
        }
        
        .summary-card {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 20px;
            border-radius: 10px;
            margin-bottom: 20px;
        }
        
        .summary-stats {
            display: flex;
            gap: 15px;
            margin-top: 15px;
            flex-wrap: wrap;
        }
        
        .summary-stat {
            background: rgba(255,255,255,0.2);
            padding: 10px 15px;
            border-radius: 5px;
            text-align: center;
            flex: 1;
            min-width: 80px;
        }
        
        .summary-stat h4 {
            margin: 0;
            font-size: 12px;
            opacity: 0.9;
        }
        
        .summary-stat p {
            margin: 5px 0 0;
            font-size: 20px;
            font-weight: bold;
        }
    `;
    document.head.appendChild(style);
}