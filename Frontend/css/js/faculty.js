let facultyData = null;
let apiBaseUrl = 'http://localhost:8080';
let currentClassId = null;
let currentDate = new Date().toISOString().split('T')[0];

document.addEventListener('DOMContentLoaded', function() {
    const userStr = sessionStorage.getItem('user');
    
    if (!userStr) {
        window.location.href = 'login.html';
        return;
    }
    
    try {
        facultyData = JSON.parse(userStr);
        
        if (facultyData.role !== 'FACULTY') {
            window.location.href = 'login.html';
            return;
        }
        
        document.getElementById('facultyName').textContent = facultyData.name || 'Faculty';
        document.getElementById('facultyDept').textContent = facultyData.department || 'Department';
        
        const dateInput = document.getElementById('attendanceDate');
        if (dateInput) {
            dateInput.value = currentDate;
            dateInput.max = currentDate;
        }
        
        loadTodaySchedule();
        loadClasses();
        loadAttendanceHistory();
        loadLeaveRequests();
        
        updateDateTime();
        setInterval(updateDateTime, 1000);
    } catch (error) {
        console.error('Error:', error);
        window.location.href = 'login.html';
    }
});

function updateDateTime() {
    const now = new Date();
    const elem = document.getElementById('currentDateTime');
    if (elem) elem.textContent = now.toLocaleString();
}

function showSection(sectionId) {
    document.querySelectorAll('.section').forEach(s => s.style.display = 'none');
    document.getElementById(sectionId + '-section').style.display = 'block';
    
    document.querySelectorAll('.sidebar-nav a').forEach(l => l.classList.remove('active'));
    if (event?.target) event.target.classList.add('active');
    
    switch(sectionId) {
        case 'dashboard': loadTodaySchedule(); break;
        case 'mark-attendance': loadClasses(); break;
        case 'my-classes': loadWeeklySchedule(); break;
        case 'attendance-history': loadAttendanceHistory(); break;
        case 'leave': loadLeaveRequests(); break;
        case 'profile': loadProfile(); break;
    }
}

async function loadTodaySchedule() {
    const container = document.getElementById('todayClasses');
    container.innerHTML = '<div class="loading-spinner"><i class="fas fa-spinner fa-spin"></i> Loading...</div>';
    
    try {
        const res = await fetch(`${apiBaseUrl}/api/faculty/${facultyData.userId}/classes/today`);
        const data = await res.json();
        
        container.innerHTML = '';
        
        if (!data.classes?.length) {
            container.innerHTML = '<div class="error-message">No classes scheduled for today</div>';
            return;
        }
        
        data.classes.forEach(cls => {
            container.innerHTML += `
                <div class="class-card">
                    <div class="class-header">
                        <span class="class-time">${cls.startTime} - ${cls.endTime}</span>
                        <span class="badge ${cls.attendanceMarked ? 'badge-success' : 'badge-warning'}">
                            ${cls.attendanceMarked ? '✓ Marked' : '⏳ Pending'}
                        </span>
                    </div>
                    <div class="class-subject">${cls.subject?.subjectName || 'N/A'}</div>
                    <div class="class-info">
                        <span><i class="fas fa-layer-group"></i> Semester ${cls.semester}</span>
                        <span><i class="fas fa-door-open"></i> Room: ${cls.roomNumber}</span>
                    </div>
                    <button class="btn-primary" onclick="prepareMarkAttendance(${cls.id})" style="width:100%;">
                        <i class="fas fa-check-circle"></i> Mark Attendance
                    </button>
                </div>
            `;
        });
    } catch (error) {
        container.innerHTML = '<div class="error-message">Error loading classes</div>';
    }
}

async function loadClasses() {
    const select = document.getElementById('classSelect');
    select.innerHTML = '<option value="">Loading...</option>';
    
    try {
        const res = await fetch(`${apiBaseUrl}/api/faculty/${facultyData.userId}/classes/today`);
        const data = await res.json();
        
        select.innerHTML = '<option value="">Select a class</option>';
        if (data.classes?.length) {
            data.classes.forEach(cls => {
                select.innerHTML += `<option value="${cls.id}">${cls.subject?.subjectName} - Sem ${cls.semester} (${cls.startTime})</option>`;
            });
        } else {
            select.innerHTML = '<option value="">No classes today</option>';
        }
    } catch (error) {
        select.innerHTML = '<option value="">Error loading classes</option>';
    }
}

function prepareMarkAttendance(classId) {
    showSection('mark-attendance');
    document.getElementById('classSelect').value = classId;
    loadStudentsForClass();
}

async function loadStudentsForClass() {
    const classId = document.getElementById('classSelect').value;
    const date = document.getElementById('attendanceDate').value;
    
    if (!classId) return;
    
    const container = document.getElementById('studentsList');
    container.innerHTML = '<div class="loading-spinner"><i class="fas fa-spinner fa-spin"></i> Loading students...</div>';
    
    try {
        const res = await fetch(`${apiBaseUrl}/api/faculty/${facultyData.userId}/class/${classId}/students?date=${date}`);
        const data = await res.json();
        
        if (!data.students?.length) {
            container.innerHTML = '<div class="error-message">No students found</div>';
            return;
        }
        
        const markedCount = data.students.filter(s => s.attendanceMarked).length;
        
        let html = `
            <div class="summary-card">
                <h3>${data.subject?.subjectName} - ${new Date(date).toLocaleDateString()}</h3>
                <div class="summary-stats">
                    <div class="summary-stat"><h4>Total</h4><p>${data.students.length}</p></div>
                    <div class="summary-stat"><h4>Marked</h4><p>${markedCount}</p></div>
                    <div class="summary-stat"><h4>Pending</h4><p>${data.students.length - markedCount}</p></div>
                </div>
            </div>
            <div class="mark-all-buttons">
                <button class="btn-mark btn-mark-present" onclick="markAllPresent()"><i class="fas fa-check-circle"></i> All Present</button>
                <button class="btn-mark btn-mark-absent" onclick="markAllAbsent()"><i class="fas fa-times-circle"></i> All Absent</button>
                <button class="btn-mark btn-mark-late" onclick="markAllLate()"><i class="fas fa-clock"></i> All Late</button>
            </div>
            <div class="table-container"><table><thead><tr><th>Roll No</th><th>Name</th><th>Status</th></tr></thead><tbody>
        `;
        
        data.students.forEach(s => {
            const status = s.attendanceStatus || 'PRESENT';
            const statusClass = status === 'PRESENT' ? 'status-present' : status === 'LATE' ? 'status-late' : 'status-absent';
            html += `
                <tr>
                    <td>${s.rollNumber}</td>
                    <td>${s.name}</td>
                    <td>
                        <select class="status-select ${statusClass}" data-student-id="${s.id}" ${s.attendanceMarked ? 'disabled' : ''}>
                            <option value="PRESENT" ${status === 'PRESENT' ? 'selected' : ''}>✅ PRESENT</option>
                            <option value="ABSENT" ${status === 'ABSENT' ? 'selected' : ''}>❌ ABSENT</option>
                            <option value="LATE" ${status === 'LATE' ? 'selected' : ''}>⏰ LATE</option>
                        </select>
                        ${s.attendanceMarked ? '<span class="badge-success"><i class="fas fa-check"></i> Marked</span>' : ''}
                    </td>
                </tr>
            `;
        });
        
        html += `</tbody></table></div>
            <button class="btn-submit" onclick="submitAttendance()" ${markedCount === data.students.length ? 'disabled' : ''}>
                <i class="fas fa-save"></i> SUBMIT ATTENDANCE
            </button>`;
        
        container.innerHTML = html;
    } catch (error) {
        container.innerHTML = '<div class="error-message">Error loading students</div>';
    }
}

function markAllPresent() {
    document.querySelectorAll('.status-select:not([disabled])').forEach(s => {
        s.value = 'PRESENT';
        s.className = 'status-select status-present';
    });
}

function markAllAbsent() {
    document.querySelectorAll('.status-select:not([disabled])').forEach(s => {
        s.value = 'ABSENT';
        s.className = 'status-select status-absent';
    });
}

function markAllLate() {
    document.querySelectorAll('.status-select:not([disabled])').forEach(s => {
        s.value = 'LATE';
        s.className = 'status-select status-late';
    });
}

async function submitAttendance() {
    const classId = document.getElementById('classSelect').value;
    const date = document.getElementById('attendanceDate').value;
    
    if (!classId || !date) return alert('Select class and date');
    
    const pending = document.querySelectorAll('.status-select:not([disabled])');
    if (!pending.length) return alert('No pending students');
    
    const attendance = Array.from(pending).map(s => ({
        studentId: parseInt(s.dataset.studentId),
        status: s.value
    }));
    
    if (!confirm(`Submit attendance for ${attendance.length} students?`)) return;
    
    try {
        const res = await fetch(`${apiBaseUrl}/api/faculty/attendance/mark`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                facultyId: facultyData.userId,
                classId: parseInt(classId),
                date,
                attendance
            })
        });
        
        const result = await res.json();
        if (result.success) {
            alert('✅ Attendance submitted');
            loadStudentsForClass();
            loadTodaySchedule();
            loadAttendanceHistory();
        }
    } catch (error) {
        alert('❌ Error submitting attendance');
    }
}

async function loadWeeklySchedule() {
    const container = document.getElementById('weeklySchedule');
    container.innerHTML = '<div class="loading-spinner"><i class="fas fa-spinner fa-spin"></i> Loading...</div>';
    
    try {
        const res = await fetch(`${apiBaseUrl}/api/faculty/${facultyData.userId}/schedule/weekly`);
        const schedule = await res.json();
        
        container.innerHTML = '';
        const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
        
        for (const day of days) {
            if (schedule[day]?.length) {
                let html = `<div class="class-card"><h3>${day}</h3>`;
                schedule[day].forEach(c => {
                    html += `<div><strong>${c.startTime}-${c.endTime}</strong> ${c.subjectName} (Sem ${c.semester}) Room:${c.roomNumber}</div>`;
                });
                html += '</div>';
                container.innerHTML += html;
            }
        }
        
        if (!container.innerHTML) container.innerHTML = '<div class="error-message">No schedule found</div>';
    } catch (error) {
        container.innerHTML = '<div class="error-message">Error loading schedule</div>';
    }
}

async function loadAttendanceHistory() {
    const tbody = document.getElementById('attendanceHistoryBody');
    tbody.innerHTML = '<tr><td colspan="5" class="loading-spinner"><i class="fas fa-spinner fa-spin"></i> Loading...</td></tr>';
    
    try {
        const res = await fetch(`${apiBaseUrl}/api/faculty/${facultyData.userId}/attendance/recent`);
        const data = await res.json();
        
        if (!data.length) {
            tbody.innerHTML = '<tr><td colspan="5" class="no-data">No history found</td></tr>';
            return;
        }
        
        tbody.innerHTML = data.map(a => `
            <tr>
                <td>${new Date(a.date).toLocaleDateString()}</td>
                <td>${a.subjectName || 'N/A'}</td>
                <td>${a.studentName || 'N/A'}</td>
                <td><span class="status-badge ${a.status === 'PRESENT' ? 'status-present' : a.status === 'LATE' ? 'status-late' : 'status-absent'}">${a.status}</span></td>
                <td><span class="status-badge ${a.verified ? 'status-present' : 'status-absent'}">${a.verified ? 'Verified' : 'Pending'}</span></td>
            </tr>
        `).join('');
    } catch (error) {
        tbody.innerHTML = '<tr><td colspan="5" class="error-message">Error loading history</td></tr>';
    }
}

async function loadLeaveRequests() {
    const tbody = document.getElementById('leaveRequestsBody');
    tbody.innerHTML = '<tr><td colspan="9" class="loading-spinner"><i class="fas fa-spinner fa-spin"></i> Loading...</td></tr>';
    
    try {
        const res = await fetch(`${apiBaseUrl}/api/faculty/leave/${facultyData.userId}`);
        const data = await res.json();
        
        if (data.success) {
            document.getElementById('pendingLeaveCount').textContent = data.pendingCount || 0;
            document.getElementById('approvedLeaveCount').textContent = data.approvedCount || 0;
            document.getElementById('rejectedLeaveCount').textContent = data.rejectedCount || 0;
            document.getElementById('leaveBalance').textContent = data.leaveBalance || 20;
            
            if (!data.leaveRequests?.length) {
                tbody.innerHTML = '<tr><td colspan="9" class="no-data">No leave requests</td></tr>';
                return;
            }
            
            tbody.innerHTML = data.leaveRequests.map(l => `
                <tr>
                    <td>${new Date(l.fromDate).toLocaleDateString()}</td>
                    <td>${new Date(l.toDate).toLocaleDateString()}</td>
                    <td>${l.days}</td>
                    <td>${l.leaveType}</td>
                    <td>${l.reason}</td>
                    <td><span class="status-badge ${l.status === 'PENDING' ? 'status-pending' : l.status === 'APPROVED' ? 'status-present' : 'status-absent'}">${l.status}</span></td>
                    <td>${new Date(l.appliedAt).toLocaleDateString()}</td>
                    <td>${l.remarks || '-'}</td>
                    <td>${l.status === 'PENDING' ? `<button class="btn-danger btn-sm" onclick="cancelLeaveRequest(${l.id})">Cancel</button>` : '-'}</td>
                </tr>
            `).join('');
        }
    } catch (error) {
        tbody.innerHTML = '<tr><td colspan="9" class="error-message">Error loading leave requests</td></tr>';
    }
}

function showLeaveRequestForm() {
    const form = document.getElementById('leaveRequestForm');
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('leaveFromDate').value = today;
    document.getElementById('leaveToDate').value = today;
    document.getElementById('leaveDuration').value = '1 day(s)';
    form.style.display = 'block';
}

function hideLeaveRequestForm() {
    document.getElementById('leaveRequestForm').style.display = 'none';
}

function calculateLeaveDays() {
    const from = document.getElementById('leaveFromDate').value;
    const to = document.getElementById('leaveToDate').value;
    if (from && to) {
        const days = Math.ceil((new Date(to) - new Date(from)) / (1000*60*60*24)) + 1;
        document.getElementById('leaveDuration').value = days + ' day(s)';
    }
}

async function submitLeaveRequest() {
    const fromDate = document.getElementById('leaveFromDate').value;
    const toDate = document.getElementById('leaveToDate').value;
    const leaveType = document.getElementById('leaveType').value;
    const reason = document.getElementById('leaveReason').value;
    
    if (!fromDate || !toDate || !reason) return alert('Fill all fields');
    
    try {
        const res = await fetch(`${apiBaseUrl}/api/faculty/leave/request`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                facultyId: facultyData.userId,
                fromDate, toDate, reason, leaveType
            })
        });
        
        const result = await res.json();
        if (result.success) {
            alert('✅ Leave request submitted');
            hideLeaveRequestForm();
            loadLeaveRequests();
        }
    } catch (error) {
        alert('❌ Error submitting request');
    }
}

async function cancelLeaveRequest(leaveId) {
    if (!confirm('Cancel this leave request?')) return;
    try {
        await fetch(`${apiBaseUrl}/api/faculty/leave/cancel/${leaveId}?facultyId=${facultyData.userId}`, { method: 'DELETE' });
        alert('✅ Request cancelled');
        loadLeaveRequests();
    } catch (error) {
        alert('❌ Error cancelling request');
    }
}

function loadProfile() {
    document.getElementById('profileDetails').innerHTML = `
        <div class="profile-card">
            <div class="profile-header"><i class="fas fa-chalkboard-teacher"></i><h3>${facultyData.name}</h3><p>${facultyData.designation || 'Faculty'}</p></div>
            <div class="profile-details">
                <p><strong>Email:</strong> ${facultyData.email}</p>
                <p><strong>Department:</strong> ${facultyData.department}</p>
                <p><strong>Phone:</strong> ${facultyData.phoneNumber || 'N/A'}</p>
            </div>
        </div>
    `;
}

function logout() {
    if (confirm('Logout?')) {
        sessionStorage.clear();
        window.location.href = 'login.html';
    }
}