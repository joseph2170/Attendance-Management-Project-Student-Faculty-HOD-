// Student Dashboard JavaScript
let studentData = null;
let apiBaseUrl = 'http://localhost:8080';
let allAttendanceRecords = [];

document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 Student dashboard loading...');
    
    // Get user data from session storage
    const userStr = sessionStorage.getItem('user');
    console.log('User data from session:', userStr);
    
    if (!userStr) {
        console.log('No user data found, redirecting to login');
        window.location.href = 'login.html';
        return;
    }
    
    try {
        studentData = JSON.parse(userStr);
        console.log('Parsed student data:', studentData);
        
        // Verify user is student
        if (studentData.role !== 'STUDENT') {
            console.log('User is not student, redirecting');
            window.location.href = 'login.html';
            return;
        }
        
        // Update UI with student info
        updateStudentInfo();
        
        // Load dashboard data
        loadDashboardData();
        
        // Update date time
        updateDateTime();
        setInterval(updateDateTime, 1000);
        
    } catch (error) {
        console.error('Error parsing user data:', error);
        window.location.href = 'login.html';
    }
});

function updateDateTime() {
    const now = new Date();
    const dateTimeElement = document.getElementById('currentDateTime');
    if (dateTimeElement) {
        dateTimeElement.textContent = now.toLocaleString();
    }
}

function updateStudentInfo() {
    try {
        const studentNameElem = document.getElementById('studentName');
        const studentInfoElem = document.getElementById('studentInfo');
        const bannerElem = document.getElementById('studentInfoBanner');
        
        if (studentNameElem) {
            studentNameElem.textContent = studentData.name || 'Student';
        }
        
        if (studentInfoElem) {
            studentInfoElem.textContent = 
                `${studentData.rollNumber || 'Roll No'} | Sem ${studentData.semester || 'N/A'}`;
        }
        
        if (bannerElem) {
            bannerElem.innerHTML = `
                <h2>Welcome, ${studentData.name}!</h2>
                <p><i class="fas fa-id-card"></i> Roll Number: ${studentData.rollNumber || 'N/A'} | 
                   <i class="fas fa-layer-group"></i> Semester: ${studentData.semester || 'N/A'} | 
                   <i class="fas fa-building"></i> Department: ${studentData.department || 'Computer Science'}</p>
            `;
        }
    } catch (error) {
        console.error('Error updating student info:', error);
    }
}

// Global function for showing sections
window.showSection = function(sectionId) {
    console.log('Showing section:', sectionId);
    
    // Hide all sections
    document.querySelectorAll('.section').forEach(section => {
        section.style.display = 'none';
    });
    
    // Show selected section
    const selectedSection = document.getElementById(sectionId + '-section');
    if (selectedSection) {
        selectedSection.style.display = 'block';
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
        case 'attendance':
            loadAttendanceHistory();
            break;
        case 'subjects':
            loadSubjects();
            break;
        case 'profile':
            loadProfile();
            break;
    }
};

// Global function for logout
window.logout = function() {
    if (confirm('Are you sure you want to logout?')) {
        sessionStorage.removeItem('user');
        sessionStorage.removeItem('token');
        window.location.href = 'login.html';
    }
};

// Global functions for attendance filters
window.filterAttendance = function() {
    const filterValue = document.getElementById('attendanceFilter')?.value;
    const monthValue = document.getElementById('monthFilter')?.value;
    
    let filtered = [...allAttendanceRecords];
    
    // Filter by status
    if (filterValue && filterValue !== 'ALL') {
        if (filterValue === 'VERIFIED') {
            filtered = filtered.filter(a => a.verified === true);
        } else if (filterValue === 'PENDING') {
            filtered = filtered.filter(a => a.verified === false);
        } else {
            filtered = filtered.filter(a => a.status === filterValue);
        }
    }
    
    // Filter by month
    if (monthValue) {
        const [year, month] = monthValue.split('-');
        filtered = filtered.filter(a => {
            if (!a.date) return false;
            const date = new Date(a.date);
            return date.getFullYear() === parseInt(year) && 
                   (date.getMonth() + 1) === parseInt(month);
        });
    }
    
    displayAttendanceRecords(filtered);
};

window.resetFilters = function() {
    const filterSelect = document.getElementById('attendanceFilter');
    const monthInput = document.getElementById('monthFilter');
    
    if (filterSelect) filterSelect.value = 'ALL';
    if (monthInput) monthInput.value = '';
    
    displayAttendanceRecords(allAttendanceRecords);
};

window.exportAttendance = function() {
    if (!allAttendanceRecords || allAttendanceRecords.length === 0) {
        alert('No attendance data to export');
        return;
    }
    
    // Create CSV content
    let csv = 'Date,Subject,Status,Verified\n';
    
    allAttendanceRecords.forEach(att => {
        const subjectName = att.subject?.subjectName || att.subject?.name || 'N/A';
        const date = att.date || 'N/A';
        const status = att.status || 'N/A';
        const verified = att.verified ? 'Yes' : 'No';
        
        csv += `${date},${subjectName},${status},${verified}\n`;
    });
    
    // Download CSV file
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `attendance_${studentData.name || 'student'}_${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    
    window.URL.revokeObjectURL(url);
};

async function loadDashboardData() {
    try {
        console.log('Loading dashboard data for student ID:', studentData.userId);
        
        // Show loading states
        document.getElementById('attendanceStats').innerHTML = 
            '<div class="loading-spinner"><i class="fas fa-spinner fa-spin"></i> Loading...</div>';
        document.getElementById('subjectWiseAttendance').innerHTML = 
            '<div class="loading-spinner"><i class="fas fa-spinner fa-spin"></i> Loading...</div>';
        document.getElementById('recentAttendanceBody').innerHTML = 
            '<tr><td colspan="3" class="loading-spinner">Loading...</td></tr>';
        
        // Fetch stats
        const statsRes = await fetch(`${apiBaseUrl}/api/student/${studentData.userId}/attendance/stats`);
        if (statsRes.ok) {
            const stats = await statsRes.json();
            updateStatsCards(stats);
        } else {
            console.error('Failed to fetch stats');
            useMockStatsData();
        }
        
        // Fetch subject-wise attendance
        const subjectsRes = await fetch(`${apiBaseUrl}/api/student/${studentData.userId}/attendance/subject-wise`);
        if (subjectsRes.ok) {
            const subjects = await subjectsRes.json();
            updateSubjectWiseAttendance(subjects);
        } else {
            console.error('Failed to fetch subjects');
            useMockSubjectData();
        }
        
        // Fetch recent attendance
        const attendanceRes = await fetch(`${apiBaseUrl}/api/student/${studentData.userId}/attendance`);
        if (attendanceRes.ok) {
            const attendance = await attendanceRes.json();
            allAttendanceRecords = attendance;
            updateRecentAttendance(attendance.slice(0, 10));
        } else {
            console.error('Failed to fetch attendance');
            useMockAttendanceData();
        }
        
    } catch (error) {
        console.error('Error loading dashboard data:', error);
        useMockStatsData();
        useMockSubjectData();
        useMockAttendanceData();
    }
}

// Mock data functions for testing
function useMockStatsData() {
    const mockStats = {
        totalClasses: 45,
        present: 38,
        late: 2,
        absent: 5,
        percentage: 88.89
    };
    updateStatsCards(mockStats);
}

function useMockSubjectData() {
    const mockSubjects = [
        {
            subjectCode: 'CS101',
            subjectName: 'Programming Fundamentals',
            credits: 4,
            present: 12,
            late: 1,
            absent: 2,
            total: 15,
            percentage: 86.67,
            requiredToMaintain75: 0
        },
        {
            subjectCode: 'CS102',
            subjectName: 'Mathematics',
            credits: 3,
            present: 10,
            late: 2,
            absent: 3,
            total: 15,
            percentage: 80.0,
            requiredToMaintain75: 0
        },
        {
            subjectCode: 'CS103',
            subjectName: 'Digital Logic',
            credits: 3,
            present: 8,
            late: 1,
            absent: 6,
            total: 15,
            percentage: 60.0,
            requiredToMaintain75: 3
        }
    ];
    updateSubjectWiseAttendance(mockSubjects);
}

function useMockAttendanceData() {
    const mockAttendance = [
        { date: '2026-02-26', subject: { subjectName: 'Programming Fundamentals' }, status: 'PRESENT', verified: true },
        { date: '2026-02-25', subject: { subjectName: 'Mathematics' }, status: 'PRESENT', verified: true },
        { date: '2026-02-24', subject: { subjectName: 'Digital Logic' }, status: 'LATE', verified: true },
        { date: '2026-02-23', subject: { subjectName: 'Programming Fundamentals' }, status: 'PRESENT', verified: true },
        { date: '2026-02-22', subject: { subjectName: 'Mathematics' }, status: 'ABSENT', verified: false },
        { date: '2026-02-21', subject: { subjectName: 'Digital Logic' }, status: 'PRESENT', verified: true },
        { date: '2026-02-20', subject: { subjectName: 'Programming Fundamentals' }, status: 'PRESENT', verified: true },
        { date: '2026-02-19', subject: { subjectName: 'Mathematics' }, status: 'PRESENT', verified: true },
        { date: '2026-02-18', subject: { subjectName: 'Digital Logic' }, status: 'PRESENT', verified: true },
        { date: '2026-02-17', subject: { subjectName: 'Programming Fundamentals' }, status: 'LATE', verified: false }
    ];
    allAttendanceRecords = mockAttendance;
    updateRecentAttendance(mockAttendance.slice(0, 10));
}

function updateStatsCards(stats) {
    const statsGrid = document.getElementById('attendanceStats');
    if (!statsGrid) return;
    
    const percentage = stats.percentage || 0;
    let color = percentage >= 75 ? '#28a745' : percentage >= 60 ? '#ffc107' : '#dc3545';
    
    statsGrid.innerHTML = `
        <div class="stat-card">
            <div class="stat-icon" style="background: #667eea;"><i class="fas fa-chart-line"></i></div>
            <div class="stat-info"><h3>Attendance</h3><p style="color: ${color};">${percentage}%</p></div>
        </div>
        <div class="stat-card">
            <div class="stat-icon" style="background: #28a745;"><i class="fas fa-check-circle"></i></div>
            <div class="stat-info"><h3>Present</h3><p>${stats.present || 0}</p></div>
        </div>
        <div class="stat-card">
            <div class="stat-icon" style="background: #ffc107;"><i class="fas fa-clock"></i></div>
            <div class="stat-info"><h3>Late</h3><p>${stats.late || 0}</p></div>
        </div>
        <div class="stat-card">
            <div class="stat-icon" style="background: #dc3545;"><i class="fas fa-times-circle"></i></div>
            <div class="stat-info"><h3>Absent</h3><p>${stats.absent || 0}</p></div>
        </div>
    `;
    
    if (percentage < 75) {
        const warning = document.createElement('div');
        warning.className = 'warning-message';
        warning.innerHTML = `<i class="fas fa-exclamation-triangle"></i> Your attendance is below 75%!`;
        statsGrid.appendChild(warning);
    }
}

function updateSubjectWiseAttendance(subjects) {
    const container = document.getElementById('subjectWiseAttendance');
    if (!container) return;
    
    if (!subjects || subjects.length === 0) {
        container.innerHTML = '<p class="no-data">No subject data available</p>';
        return;
    }
    
    let html = '';
    subjects.forEach(subject => {
        const percentage = subject.percentage || 0;
        let color = percentage >= 75 ? '#28a745' : percentage >= 60 ? '#ffc107' : '#dc3545';
        
        html += `
            <div class="subject-card">
                <div class="subject-header">
                    <span class="subject-name">${subject.subjectName}</span>
                    <span class="subject-code">${subject.subjectCode}</span>
                </div>
                <div class="subject-stats">
                    <div class="stat-item"><span class="stat-value" style="color:#28a745;">${subject.present || 0}</span> <span class="stat-label">Present</span></div>
                    <div class="stat-item"><span class="stat-value" style="color:#ffc107;">${subject.late || 0}</span> <span class="stat-label">Late</span></div>
                    <div class="stat-item"><span class="stat-value" style="color:#dc3545;">${subject.absent || 0}</span> <span class="stat-label">Absent</span></div>
                </div>
                <div class="progress-container">
                    <div class="progress-bar"><div class="progress-fill" style="width:${percentage}%; background:${color};"></div></div>
                    <span class="percentage-text" style="color:${color};">${percentage}%</span>
                </div>
                ${subject.requiredToMaintain75 > 0 ? 
                    `<div class="required-classes">Need ${subject.requiredToMaintain75} more to maintain 75%</div>` : 
                    percentage >= 75 ? 
                    `<div class="good-standing"><i class="fas fa-check-circle"></i> Good standing</div>` : ''}
            </div>
        `;
    });
    container.innerHTML = html;
}

function updateRecentAttendance(attendance) {
    const tbody = document.getElementById('recentAttendanceBody');
    if (!tbody) return;
    
    if (!attendance || attendance.length === 0) {
        tbody.innerHTML = '<tr><td colspan="3" class="no-data">No recent attendance</td></tr>';
        return;
    }
    
    tbody.innerHTML = '';
    attendance.forEach(att => {
        const row = document.createElement('tr');
        const statusClass = att.status === 'PRESENT' ? 'status-present' : 
                          att.status === 'LATE' ? 'status-late' : 'status-absent';
        
        const subjectName = att.subject?.subjectName || att.subject?.name || 'N/A';
        const formattedDate = formatDate(att.date);
        
        row.innerHTML = `
            <td>${formattedDate}</td>
            <td>${subjectName}</td>
            <td><span class="status-badge ${statusClass}">${att.status}</span></td>
        `;
        tbody.appendChild(row);
    });
}

async function loadAttendanceHistory() {
    const tbody = document.getElementById('attendanceHistoryBody');
    if (!tbody) return;
    
    tbody.innerHTML = '<tr><td colspan="4" class="loading-spinner"><i class="fas fa-spinner fa-spin"></i> Loading...</td></tr>';
    
    try {
        const response = await fetch(`${apiBaseUrl}/api/student/${studentData.userId}/attendance`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const attendance = await response.json();
        allAttendanceRecords = attendance;
        displayAttendanceRecords(attendance);
        
    } catch (error) {
        console.error('Error loading attendance history:', error);
        tbody.innerHTML = '<tr><td colspan="4" class="error-message">Error loading history</td></tr>';
        useMockAttendanceData();
    }
}

function displayAttendanceRecords(records) {
    const tbody = document.getElementById('attendanceHistoryBody');
    if (!tbody) return;
    
    tbody.innerHTML = '';
    
    if (!records || records.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="no-data">No records found</td></tr>';
        return;
    }
    
    records.sort((a, b) => new Date(b.date) - new Date(a.date));
    
    records.forEach(att => {
        const row = document.createElement('tr');
        const statusClass = att.status === 'PRESENT' ? 'status-present' : 
                          att.status === 'LATE' ? 'status-late' : 'status-absent';
        
        const subjectName = att.subject?.subjectName || att.subject?.name || 'N/A';
        const formattedDate = formatDate(att.date);
        
        row.innerHTML = `
            <td>${formattedDate}</td>
            <td>${subjectName}</td>
            <td><span class="status-badge ${statusClass}">${att.status}</span></td>
            <td><span class="status-badge ${att.verified ? 'status-present' : 'status-absent'}">${att.verified ? 'Verified' : 'Pending'}</span></td>
        `;
        tbody.appendChild(row);
    });
}

async function loadSubjects() {
    const tbody = document.getElementById('subjectsListBody');
    if (!tbody) return;
    
    tbody.innerHTML = '<tr><td colspan="4" class="loading-spinner"><i class="fas fa-spinner fa-spin"></i> Loading...</td></tr>';
    
    try {
        const response = await fetch(`${apiBaseUrl}/api/student/${studentData.userId}/attendance/subject-wise`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const subjects = await response.json();
        
        tbody.innerHTML = '';
        
        if (!subjects || subjects.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="no-data">No subjects found</td></tr>';
            return;
        }
        
        subjects.forEach(subject => {
            const row = document.createElement('tr');
            const percentage = subject.percentage || 0;
            let color = percentage >= 75 ? '#28a745' : percentage >= 60 ? '#ffc107' : '#dc3545';
            
            row.innerHTML = `
                <td>${subject.subjectCode || 'N/A'}</td>
                <td>${subject.subjectName || 'N/A'}</td>
                <td>${subject.credits || 'N/A'}</td>
                <td>
                    <div style="display: flex; align-items: center; gap: 10px;">
                        <div class="progress-bar" style="width: 100px;"><div class="progress-fill" style="width:${percentage}%; background:${color};"></div></div>
                        <span style="color:${color};">${percentage}%</span>
                    </div>
                    <small>P:${subject.present || 0} L:${subject.late || 0} A:${subject.absent || 0}</small>
                </td>
            `;
            tbody.appendChild(row);
        });
        
    } catch (error) {
        console.error('Error loading subjects:', error);
        tbody.innerHTML = '<tr><td colspan="4" class="error-message">Error loading subjects</td></tr>';
        useMockSubjectData();
    }
}

async function loadProfile() {
    const profileDiv = document.getElementById('profileDetails');
    if (!profileDiv) return;
    
    profileDiv.innerHTML = '<div class="loading-spinner"><i class="fas fa-spinner fa-spin"></i> Loading...</div>';
    
    try {
        const response = await fetch(`${apiBaseUrl}/api/student/${studentData.userId}/profile`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const profile = await response.json();
        
        const percentage = profile.attendanceStats?.percentage || 0;
        const color = percentage >= 75 ? '#28a745' : percentage >= 60 ? '#ffc107' : '#dc3545';
        
        profileDiv.innerHTML = `
            <div class="profile-card">
                <div class="profile-header">
                    <i class="fas fa-user-graduate"></i>
                    <h3>${profile.name}</h3>
                    <p>${profile.rollNumber}</p>
                </div>
                <div class="profile-details">
                    <p><i class="fas fa-envelope"></i> <strong>Email:</strong> ${profile.email}</p>
                    <p><i class="fas fa-building"></i> <strong>Department:</strong> ${profile.department}</p>
                    <p><i class="fas fa-layer-group"></i> <strong>Semester:</strong> ${profile.semester}</p>
                    <p><i class="fas fa-phone"></i> <strong>Phone:</strong> ${profile.phoneNumber || 'N/A'}</p>
                </div>
                <div class="profile-stats">
                    <h4>Attendance Summary</h4>
                    <div class="stat-row"><span>Total Classes:</span> <strong>${profile.attendanceStats?.totalClasses || 0}</strong></div>
                    <div class="stat-row"><span>Present:</span> <strong style="color:#28a745;">${profile.attendanceStats?.present || 0}</strong></div>
                    <div class="stat-row"><span>Late:</span> <strong style="color:#ffc107;">${profile.attendanceStats?.late || 0}</strong></div>
                    <div class="stat-row"><span>Absent:</span> <strong style="color:#dc3545;">${profile.attendanceStats?.absent || 0}</strong></div>
                    <div class="stat-row"><span>Percentage:</span> <strong style="color:${color};">${percentage}%</strong></div>
                </div>
            </div>
        `;
        
    } catch (error) {
        console.error('Error loading profile:', error);
        profileDiv.innerHTML = '<div class="error-message">Error loading profile</div>';
    }
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    try {
        const date = new Date(dateString);
        if (isNaN(date.getTime())) return dateString;
        return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
    } catch (error) {
        return dateString;
    }
}

// Add CSS for dashboard if not already present
const style = document.createElement('style');
style.textContent = `
    .loading-spinner {
        text-align: center;
        padding: 30px;
        color: #666;
    }
    .loading-spinner i {
        font-size: 30px;
        margin-bottom: 10px;
        color: #667eea;
    }
    .error-message {
        background: #f8d7da;
        color: #721c24;
        padding: 15px;
        border-radius: 5px;
        text-align: center;
        margin: 20px 0;
    }
    .no-data {
        text-align: center;
        padding: 30px;
        color: #999;
        font-style: italic;
    }
    .warning-message {
        background: #fff3cd;
        color: #856404;
        padding: 15px;
        border-radius: 5px;
        margin: 20px 0;
        display: flex;
        align-items: center;
        gap: 10px;
    }
    .required-classes {
        font-size: 12px;
        color: #856404;
        margin-top: 5px;
        padding: 5px;
        background: #fff3cd;
        border-radius: 3px;
    }
    .good-standing {
        margin-top: 15px;
        padding: 10px;
        background: #d4edda;
        color: #155724;
        border-radius: 5px;
        font-size: 14px;
        display: flex;
        align-items: center;
        gap: 10px;
    }
    .subject-card {
        background: white;
        border-radius: 10px;
        padding: 15px;
        margin-bottom: 15px;
        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }
    .subject-header {
        display: flex;
        justify-content: space-between;
        margin-bottom: 10px;
        padding-bottom: 5px;
        border-bottom: 1px solid #eee;
    }
    .subject-name {
        font-size: 16px;
        font-weight: 600;
        color: #333;
    }
    .subject-code {
        font-size: 12px;
        color: #666;
        background: #f0f0f0;
        padding: 2px 8px;
        border-radius: 3px;
    }
    .subject-stats {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 10px;
        margin-bottom: 10px;
        text-align: center;
    }
    .stat-item {
        text-align: center;
    }
    .stat-label {
        color: #666;
        font-size: 12px;
    }
    .stat-value {
        font-weight: bold;
        font-size: 16px;
    }
    .progress-container {
        margin-top: 10px;
        display: flex;
        align-items: center;
        gap: 10px;
    }
    .progress-bar {
        flex: 1;
        height: 8px;
        background: #e9ecef;
        border-radius: 4px;
        overflow: hidden;
    }
    .progress-fill {
        height: 100%;
        transition: width 0.3s;
    }
    .percentage-text {
        font-weight: bold;
        min-width: 50px;
    }
    .status-badge {
        padding: 4px 8px;
        border-radius: 4px;
        font-size: 12px;
        font-weight: 600;
    }
    .status-present {
        background: #d4edda;
        color: #155724;
    }
    .status-absent {
        background: #f8d7da;
        color: #721c24;
    }
    .status-late {
        background: #fff3cd;
        color: #856404;
    }
    .profile-card {
        background: white;
        border-radius: 10px;
        padding: 30px;
        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
    }
    .profile-header {
        text-align: center;
        margin-bottom: 30px;
    }
    .profile-header i {
        font-size: 60px;
        color: #667eea;
        margin-bottom: 15px;
    }
    .profile-header h3 {
        margin: 10px 0 5px;
        color: #333;
    }
    .profile-details {
        max-width: 500px;
        margin: 0 auto;
    }
    .profile-details p {
        padding: 10px 0;
        border-bottom: 1px solid #e9ecef;
        margin: 0;
    }
    .profile-stats {
        margin-top: 20px;
        padding: 15px;
        background: #f8f9fa;
        border-radius: 5px;
    }
    .stat-row {
        display: flex;
        justify-content: space-between;
        padding: 5px 0;
    }
    .filter-section {
        background: white;
        padding: 15px;
        border-radius: 8px;
        margin-bottom: 20px;
        display: flex;
        gap: 10px;
        flex-wrap: wrap;
    }
    .filter-select, .filter-input {
        padding: 8px 12px;
        border: 1px solid #ddd;
        border-radius: 4px;
        font-size: 14px;
    }
    .btn-secondary {
        background: #6c757d;
        color: white;
        border: none;
        padding: 8px 16px;
        border-radius: 4px;
        cursor: pointer;
    }
    .btn-secondary:hover {
        background: #5a6268;
    }
    .btn-primary {
        background: #007bff;
        color: white;
        border: none;
        padding: 8px 16px;
        border-radius: 4px;
        cursor: pointer;
    }
    .btn-primary:hover {
        background: #0069d9;
    }
`;
document.head.appendChild(style);