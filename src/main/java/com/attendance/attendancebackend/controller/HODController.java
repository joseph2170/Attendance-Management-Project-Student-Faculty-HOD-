package com.attendance.attendancebackend.controller;

import com.attendance.attendancebackend.model.*;
import com.attendance.attendancebackend.service.*;
import com.attendance.attendancebackend.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hod")
@CrossOrigin(origins = "*")
public class HODController {
    
    private final UserService userService;
    private final AttendanceService attendanceService;
    private final SubjectRepository subjectRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final AttendanceRepository attendanceRepository;
    private final FacultyAttendanceService facultyAttendanceService;
    private final FacultyAttendanceRepository facultyAttendanceRepository;
    private final LeaveRequestService leaveRequestService;
    private final LeaveRequestRepository leaveRequestRepository;
    
    public HODController(
            UserService userService,
            AttendanceService attendanceService,
            SubjectRepository subjectRepository,
            ClassScheduleRepository classScheduleRepository,
            AttendanceRepository attendanceRepository,
            FacultyAttendanceService facultyAttendanceService,
            FacultyAttendanceRepository facultyAttendanceRepository,
            LeaveRequestService leaveRequestService,
            LeaveRequestRepository leaveRequestRepository) {
        this.userService = userService;
        this.attendanceService = attendanceService;
        this.subjectRepository = subjectRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.attendanceRepository = attendanceRepository;
        this.facultyAttendanceService = facultyAttendanceService;
        this.facultyAttendanceRepository = facultyAttendanceRepository;
        this.leaveRequestService = leaveRequestService;
        this.leaveRequestRepository = leaveRequestRepository;
    }
    
    @GetMapping("/{hodId}/pending-verifications")
    public ResponseEntity<?> getPendingVerifications(@PathVariable Long hodId) {
        try {
            User hod = userService.getUserById(hodId)
                .orElseThrow(() -> new RuntimeException("HOD not found"));
            
            String department = hod.getDepartment();
            List<Attendance> allPending = attendanceRepository.findByVerifiedFalse();
            
            List<Map<String, Object>> pendingList = new ArrayList<>();
            for (Attendance att : allPending) {
                if (att.getStudent() != null && department.equals(att.getStudent().getDepartment())) {
                    Map<String, Object> attMap = new HashMap<>();
                    attMap.put("id", att.getId());
                    attMap.put("date", att.getDate().toString());
                    attMap.put("status", att.getStatus());
                    
                    Map<String, Object> studentMap = new HashMap<>();
                    studentMap.put("id", att.getStudent().getId());
                    studentMap.put("name", att.getStudent().getName());
                    studentMap.put("rollNumber", att.getStudent().getRollNumber());
                    studentMap.put("semester", att.getStudent().getSemester());
                    attMap.put("student", studentMap);
                    
                    if (att.getSubject() != null) {
                        Map<String, Object> subjectMap = new HashMap<>();
                        subjectMap.put("id", att.getSubject().getId());
                        subjectMap.put("name", att.getSubject().getSubjectName());
                        subjectMap.put("code", att.getSubject().getSubjectCode());
                        attMap.put("subject", subjectMap);
                    }
                    
                    if (att.getMarkedBy() != null) {
                        Map<String, Object> facultyMap = new HashMap<>();
                        facultyMap.put("id", att.getMarkedBy().getId());
                        facultyMap.put("name", att.getMarkedBy().getName());
                        attMap.put("markedBy", facultyMap);
                    }
                    
                    pendingList.add(attMap);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("total", pendingList.size());
            response.put("pendingVerifications", pendingList);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching pending verifications: " + e.getMessage());
        }
    }
    
    @PostMapping("/attendance/verify/{attendanceId}")
    public ResponseEntity<?> verifyAttendance(@PathVariable Long attendanceId) {
        try {
            Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance not found"));
            
            attendance.setVerified(true);
            Attendance verified = attendanceRepository.save(attendance);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Attendance verified successfully");
            response.put("attendanceId", verified.getId());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error verifying attendance: " + e.getMessage());
        }
    }
    
    @PostMapping("/attendance/verify-all")
    public ResponseEntity<?> verifyAllAttendance() {
        try {
            List<Attendance> pending = attendanceRepository.findByVerifiedFalse();
            
            for (Attendance att : pending) {
                att.setVerified(true);
            }
            
            List<Attendance> verified = attendanceRepository.saveAll(pending);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All pending attendance verified");
            response.put("count", verified.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error verifying all attendance: " + e.getMessage());
        }
    }
    
    @GetMapping("/{hodId}/department/{department}/students")
    public ResponseEntity<?> getDepartmentStudents(
            @PathVariable Long hodId,
            @PathVariable String department,
            @RequestParam(required = false) Integer semester) {
        try {
            List<User> students;
            if (semester != null) {
                students = userService.getStudentsByDepartmentAndSemester(department, semester);
            } else {
                students = userService.getStudentsByDepartment(department);
            }
            
            List<Map<String, Object>> formattedStudents = new ArrayList<>();
            for (User student : students) {
                Map<String, Object> studentMap = new HashMap<>();
                studentMap.put("id", student.getId());
                studentMap.put("name", student.getName());
                studentMap.put("email", student.getEmail());
                studentMap.put("rollNumber", student.getRollNumber());
                studentMap.put("semester", student.getSemester());
                studentMap.put("phoneNumber", student.getPhoneNumber());
                
                long total = attendanceRepository.countTotalAttendance(student);
                long present = attendanceRepository.countPresentAttendance(student);
                double percentage = total > 0 ? (present * 100.0 / total) : 0;
                
                studentMap.put("totalClasses", total);
                studentMap.put("present", present);
                studentMap.put("attendancePercentage", Math.round(percentage * 100.0) / 100.0);
                
                formattedStudents.add(studentMap);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("total", formattedStudents.size());
            response.put("students", formattedStudents);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching students: " + e.getMessage());
        }
    }
    
    @GetMapping("/{hodId}/department/{department}/faculty")
    public ResponseEntity<?> getDepartmentFaculty(
            @PathVariable Long hodId,
            @PathVariable String department) {
        try {
            List<User> faculty = userService.getFacultyByDepartment(department);
            
            List<Map<String, Object>> formattedFaculty = new ArrayList<>();
            for (User f : faculty) {
                Map<String, Object> facultyMap = new HashMap<>();
                facultyMap.put("id", f.getId());
                facultyMap.put("name", f.getName());
                facultyMap.put("email", f.getEmail());
                facultyMap.put("designation", f.getDesignation());
                facultyMap.put("phoneNumber", f.getPhoneNumber());
                
                long classCount = classScheduleRepository.countByFacultyId(f.getId());
                facultyMap.put("classCount", classCount);
                
                formattedFaculty.add(facultyMap);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("total", formattedFaculty.size());
            response.put("faculty", formattedFaculty);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching faculty: " + e.getMessage());
        }
    }
    
    @GetMapping("/{hodId}/department/{department}/faculty-attendance-list")
    public ResponseEntity<?> getFacultyAttendanceList(
            @PathVariable Long hodId,
            @PathVariable String department,
            @RequestParam(required = false) LocalDate date) {
        try {
            if (date == null) {
                date = LocalDate.now();
            }
            
            List<User> faculty = userService.getFacultyByDepartment(department);
            List<FacultyAttendance> existingAttendance = facultyAttendanceRepository
                .findByDepartmentAndDate(department, date);
            
            Map<Long, FacultyAttendance> attendanceMap = new HashMap<>();
            for (FacultyAttendance fa : existingAttendance) {
                attendanceMap.put(fa.getFaculty().getId(), fa);
            }
            
            List<Map<String, Object>> facultyList = new ArrayList<>();
            for (User f : faculty) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", f.getId());
                map.put("name", f.getName());
                map.put("email", f.getEmail());
                map.put("designation", f.getDesignation());
                map.put("phone", f.getPhoneNumber());
                
                FacultyAttendance existing = attendanceMap.get(f.getId());
                if (existing != null) {
                    map.put("attendanceMarked", true);
                    map.put("status", existing.getStatus());
                    map.put("remarks", existing.getRemarks());
                } else {
                    map.put("attendanceMarked", false);
                    map.put("status", "PRESENT");
                    map.put("remarks", "");
                }
                
                facultyList.add(map);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("date", date.toString());
            response.put("total", facultyList.size());
            response.put("markedCount", existingAttendance.size());
            response.put("pendingCount", facultyList.size() - existingAttendance.size());
            response.put("faculty", facultyList);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching faculty attendance list: " + e.getMessage());
        }
    }
    
    @PostMapping("/faculty-attendance/mark-bulk")
    public ResponseEntity<?> markBulkFacultyAttendance(@RequestBody Map<String, Object> request) {
        try {
            Long hodId = Long.parseLong(request.get("hodId").toString());
            String dateStr = request.get("date").toString();
            LocalDate date = LocalDate.parse(dateStr);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> attendanceList = (List<Map<String, Object>>) request.get("attendance");
            
            User hod = userService.getUserById(hodId)
                .orElseThrow(() -> new RuntimeException("HOD not found"));
            
            List<FacultyAttendance> savedAttendances = new ArrayList<>();
            int marked = 0;
            int updated = 0;
            
            for (Map<String, Object> attData : attendanceList) {
                Long facultyId = Long.parseLong(attData.get("facultyId").toString());
                String status = attData.get("status").toString();
                String remarks = attData.get("remarks") != null ? attData.get("remarks").toString() : "";
                
                User faculty = userService.getUserById(facultyId)
                    .orElseThrow(() -> new RuntimeException("Faculty not found"));
                
                Optional<FacultyAttendance> existing = facultyAttendanceRepository
                    .findByFacultyIdAndAttendanceDate(facultyId, date);
                
                FacultyAttendance attendance;
                if (existing.isPresent()) {
                    attendance = existing.get();
                    attendance.setStatus(status);
                    attendance.setRemarks(remarks);
                    attendance.setMarkedBy(hod);
                    attendance.setMarkedAt(LocalDateTime.now());
                    updated++;
                } else {
                    attendance = new FacultyAttendance();
                    attendance.setFaculty(faculty);
                    attendance.setAttendanceDate(date);
                    attendance.setStatus(status);
                    attendance.setRemarks(remarks);
                    attendance.setMarkedBy(hod);
                    attendance.setMarkedAt(LocalDateTime.now());
                    marked++;
                }
                
                savedAttendances.add(facultyAttendanceRepository.save(attendance));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Faculty attendance processed successfully");
            response.put("marked", marked);
            response.put("updated", updated);
            response.put("total", savedAttendances.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error marking faculty attendance: " + e.getMessage());
        }
    }
    
    @GetMapping("/{hodId}/leave-requests/pending")
    public ResponseEntity<?> getPendingLeaveRequests(@PathVariable Long hodId) {
        try {
            User hod = userService.getUserById(hodId)
                .orElseThrow(() -> new RuntimeException("HOD not found"));
            
            List<LeaveRequest> pendingLeaves = leaveRequestService
                .getPendingLeaveRequestsByDepartment(hod.getDepartment());
            
            List<Map<String, Object>> leaveList = new ArrayList<>();
            for (LeaveRequest lr : pendingLeaves) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", lr.getId());
                map.put("facultyId", lr.getFaculty().getId());
                map.put("facultyName", lr.getFaculty().getName());
                map.put("facultyEmail", lr.getFaculty().getEmail());
                map.put("fromDate", lr.getFromDate().toString());
                map.put("toDate", lr.getToDate().toString());
                map.put("reason", lr.getReason());
                map.put("leaveType", lr.getLeaveType());
                map.put("appliedAt", lr.getAppliedAt().toString());
                
                long days = ChronoUnit.DAYS.between(lr.getFromDate(), lr.getToDate()) + 1;
                map.put("days", days);
                
                leaveList.add(map);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("total", leaveList.size());
            response.put("leaveRequests", leaveList);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching leave requests: " + e.getMessage());
        }
    }
    
    @PostMapping("/leave-requests/approve/{leaveId}")
    public ResponseEntity<?> approveLeaveRequest(
            @PathVariable Long leaveId,
            @RequestBody Map<String, Object> request) {
        try {
            Long hodId = Long.parseLong(request.get("hodId").toString());
            String remarks = request.get("remarks") != null ? request.get("remarks").toString() : "Approved";
            
            User hod = userService.getUserById(hodId)
                .orElseThrow(() -> new RuntimeException("HOD not found"));
            
            LeaveRequest approved = leaveRequestService.approveLeaveRequest(leaveId, hod, remarks);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Leave request approved successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error approving leave: " + e.getMessage());
        }
    }
    
    @PostMapping("/leave-requests/reject/{leaveId}")
    public ResponseEntity<?> rejectLeaveRequest(
            @PathVariable Long leaveId,
            @RequestBody Map<String, Object> request) {
        try {
            Long hodId = Long.parseLong(request.get("hodId").toString());
            String remarks = request.get("remarks") != null ? request.get("remarks").toString() : "Rejected";
            
            User hod = userService.getUserById(hodId)
                .orElseThrow(() -> new RuntimeException("HOD not found"));
            
            LeaveRequest rejected = leaveRequestService.rejectLeaveRequest(leaveId, hod, remarks);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Leave request rejected");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error rejecting leave: " + e.getMessage());
        }
    }
}