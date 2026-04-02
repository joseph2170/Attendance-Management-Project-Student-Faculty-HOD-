package com.attendance.attendancebackend.controller;

import com.attendance.attendancebackend.model.*;
import com.attendance.attendancebackend.service.*;
import com.attendance.attendancebackend.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
@CrossOrigin(origins = "*")
public class StudentController {
    
    private final UserService userService;
    private final AttendanceService attendanceService;
    private final SubjectRepository subjectRepository;
    private final AttendanceRepository attendanceRepository;
    
    public StudentController(
            UserService userService,
            AttendanceService attendanceService,
            SubjectRepository subjectRepository,
            AttendanceRepository attendanceRepository) {
        this.userService = userService;
        this.attendanceService = attendanceService;
        this.subjectRepository = subjectRepository;
        this.attendanceRepository = attendanceRepository;
    }
    
    /**
     * Get student by ID
     * Endpoint: GET /api/student/{studentId}
     */
    @GetMapping("/{studentId}")
    public ResponseEntity<?> getStudent(@PathVariable Long studentId) {
        try {
            User student = userService.getUserById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", student.getId());
            response.put("name", student.getName());
            response.put("email", student.getEmail());
            response.put("rollNumber", student.getRollNumber());
            response.put("department", student.getDepartment());
            response.put("semester", student.getSemester());
            response.put("phoneNumber", student.getPhoneNumber());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "message", "Student not found"));
        }
    }
    
    /**
     * Get all attendance for a student
     * Endpoint: GET /api/student/{studentId}/attendance
     */
    @GetMapping("/{studentId}/attendance")
    public ResponseEntity<?> getStudentAttendance(@PathVariable Long studentId) {
        try {
            User student = userService.getUserById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));
            
            List<Attendance> attendance = attendanceRepository.findByStudentId(studentId);
            
            // Sort by date (most recent first)
            attendance.sort((a1, a2) -> a2.getDate().compareTo(a1.getDate()));
            
            List<Map<String, Object>> formattedAttendance = new ArrayList<>();
            
            for (Attendance att : attendance) {
                Map<String, Object> attMap = new HashMap<>();
                attMap.put("id", att.getId());
                attMap.put("date", att.getDate().toString());
                attMap.put("status", att.getStatus());
                attMap.put("verified", att.isVerified());
                
                // Add subject details
                if (att.getSubject() != null) {
                    Map<String, Object> subjectMap = new HashMap<>();
                    subjectMap.put("id", att.getSubject().getId());
                    subjectMap.put("subjectName", att.getSubject().getSubjectName());
                    subjectMap.put("subjectCode", att.getSubject().getSubjectCode());
                    attMap.put("subject", subjectMap);
                }
                
                formattedAttendance.add(attMap);
            }
            
            return ResponseEntity.ok(formattedAttendance);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error fetching attendance: " + e.getMessage()));
        }
    }
    
    /**
     * Get attendance statistics with percentage
     * Endpoint: GET /api/student/{studentId}/attendance/stats
     */
    @GetMapping("/{studentId}/attendance/stats")
    public ResponseEntity<?> getAttendanceStats(@PathVariable Long studentId) {
        try {
            User student = userService.getUserById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));
            
            // Get all attendance for this student
            List<Attendance> allAttendance = attendanceRepository.findByStudentId(studentId);
            
            long totalClasses = allAttendance.size();
            long present = allAttendance.stream()
                .filter(a -> "PRESENT".equals(a.getStatus()))
                .count();
            long late = allAttendance.stream()
                .filter(a -> "LATE".equals(a.getStatus()))
                .count();
            long absent = allAttendance.stream()
                .filter(a -> "ABSENT".equals(a.getStatus()))
                .count();
            
            double percentage = totalClasses > 0 
                ? ((present + late) * 100.0) / totalClasses 
                : 0.0;
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalClasses", totalClasses);
            stats.put("present", present);
            stats.put("late", late);
            stats.put("absent", absent);
            stats.put("percentage", Math.round(percentage * 100.0) / 100.0);
            stats.put("studentId", studentId);
            stats.put("studentName", student.getName());
            stats.put("rollNumber", student.getRollNumber());
            stats.put("semester", student.getSemester());
            stats.put("department", student.getDepartment());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error calculating stats: " + e.getMessage()));
        }
    }
    
    /**
     * Get subject-wise attendance
     * Endpoint: GET /api/student/{studentId}/attendance/subject-wise
     */
    @GetMapping("/{studentId}/attendance/subject-wise")
    public ResponseEntity<?> getSubjectWiseAttendance(@PathVariable Long studentId) {
        try {
            User student = userService.getUserById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));
            
            // Get subjects for student's department and semester
            List<Subject> subjects = subjectRepository
                .findByDepartmentAndSemester(student.getDepartment(), student.getSemester());
            
            // If no subjects found, return empty list
            if (subjects == null || subjects.isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>());
            }
            
            List<Attendance> allAttendance = attendanceRepository.findByStudentId(studentId);
            
            List<Map<String, Object>> subjectWiseData = new ArrayList<>();
            
            for (Subject subject : subjects) {
                List<Attendance> subjectAttendance = allAttendance.stream()
                    .filter(a -> a.getSubject() != null && 
                            a.getSubject().getId().equals(subject.getId()))
                    .collect(Collectors.toList());
                
                long total = subjectAttendance.size();
                long present = subjectAttendance.stream()
                    .filter(a -> "PRESENT".equals(a.getStatus()))
                    .count();
                long late = subjectAttendance.stream()
                    .filter(a -> "LATE".equals(a.getStatus()))
                    .count();
                long absent = total - present - late;
                
                double percentage = total > 0 
                    ? ((present + late) * 100.0) / total 
                    : 0.0;
                
                Map<String, Object> subjectData = new HashMap<>();
                subjectData.put("subjectId", subject.getId());
                subjectData.put("subjectCode", subject.getSubjectCode());
                subjectData.put("subjectName", subject.getSubjectName());
                subjectData.put("credits", subject.getCredits());
                subjectData.put("total", total);
                subjectData.put("present", present);
                subjectData.put("late", late);
                subjectData.put("absent", absent);
                subjectData.put("percentage", Math.round(percentage * 100.0) / 100.0);
                
                // Calculate required classes to maintain 75%
                if (total > 0 && percentage < 75.0) {
                    double currentEffective = present + late;
                    long currentTotal = total;
                    long required = 0;
                    
                    while (((currentEffective) * 100.0 / (currentTotal + required)) < 75.0) {
                        required++;
                        if (required > 100) break; // Safety limit
                    }
                    subjectData.put("requiredToMaintain75", required);
                } else {
                    subjectData.put("requiredToMaintain75", 0);
                }
                
                subjectWiseData.add(subjectData);
            }
            
            return ResponseEntity.ok(subjectWiseData);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error fetching subject-wise attendance: " + e.getMessage()));
        }
    }
    
    /**
     * Get student profile with attendance summary
     * Endpoint: GET /api/student/{studentId}/profile
     */
    @GetMapping("/{studentId}/profile")
    public ResponseEntity<?> getStudentProfile(@PathVariable Long studentId) {
        try {
            User student = userService.getUserById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));
            
            // Get attendance stats
            List<Attendance> allAttendance = attendanceRepository.findByStudentId(studentId);
            
            long totalClasses = allAttendance.size();
            long present = allAttendance.stream()
                .filter(a -> "PRESENT".equals(a.getStatus()))
                .count();
            long late = allAttendance.stream()
                .filter(a -> "LATE".equals(a.getStatus()))
                .count();
            long absent = allAttendance.stream()
                .filter(a -> "ABSENT".equals(a.getStatus()))
                .count();
            
            double percentage = totalClasses > 0 
                ? ((present + late) * 100.0) / totalClasses 
                : 0.0;
            
            Map<String, Object> attendanceStats = new HashMap<>();
            attendanceStats.put("totalClasses", totalClasses);
            attendanceStats.put("present", present);
            attendanceStats.put("late", late);
            attendanceStats.put("absent", absent);
            attendanceStats.put("percentage", Math.round(percentage * 100.0) / 100.0);
            
            Map<String, Object> profile = new HashMap<>();
            profile.put("id", student.getId());
            profile.put("name", student.getName());
            profile.put("email", student.getEmail());
            profile.put("rollNumber", student.getRollNumber() != null ? student.getRollNumber() : "N/A");
            profile.put("department", student.getDepartment() != null ? student.getDepartment() : "N/A");
            profile.put("semester", student.getSemester() != null ? student.getSemester() : "N/A");
            profile.put("phoneNumber", student.getPhoneNumber() != null ? student.getPhoneNumber() : "N/A");
            profile.put("attendanceStats", attendanceStats);
            
            return ResponseEntity.ok(profile);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error fetching profile: " + e.getMessage()));
        }
    }
    
    /**
     * Get attendance for a specific date range
     * Endpoint: GET /api/student/{studentId}/attendance/range?startDate=2026-01-01&endDate=2026-12-31
     */
    @GetMapping("/{studentId}/attendance/range")
    public ResponseEntity<?> getAttendanceByDateRange(
            @PathVariable Long studentId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            User student = userService.getUserById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));
            
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            
            List<Attendance> attendance = attendanceRepository.findByStudentId(studentId)
                .stream()
                .filter(a -> !a.getDate().isBefore(start) && !a.getDate().isAfter(end))
                .sorted((a1, a2) -> a2.getDate().compareTo(a1.getDate()))
                .collect(Collectors.toList());
            
            List<Map<String, Object>> formattedAttendance = new ArrayList<>();
            
            for (Attendance att : attendance) {
                Map<String, Object> attMap = new HashMap<>();
                attMap.put("id", att.getId());
                attMap.put("date", att.getDate().toString());
                attMap.put("status", att.getStatus());
                attMap.put("verified", att.isVerified());
                
                if (att.getSubject() != null) {
                    Map<String, Object> subjectMap = new HashMap<>();
                    subjectMap.put("id", att.getSubject().getId());
                    subjectMap.put("name", att.getSubject().getSubjectName());
                    attMap.put("subject", subjectMap);
                }
                
                formattedAttendance.add(attMap);
            }
            
            return ResponseEntity.ok(formattedAttendance);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error fetching attendance range: " + e.getMessage()));
        }
    }
    
    /**
     * Get monthly attendance summary
     * Endpoint: GET /api/student/{studentId}/attendance/monthly
     */
    @GetMapping("/{studentId}/attendance/monthly")
    public ResponseEntity<?> getMonthlyAttendance(@PathVariable Long studentId) {
        try {
            User student = userService.getUserById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));
            
            List<Attendance> allAttendance = attendanceRepository.findByStudentId(studentId);
            
            // Group by month (YYYY-MM)
            Map<String, List<Attendance>> byMonth = allAttendance.stream()
                .collect(Collectors.groupingBy(a -> 
                    a.getDate().getYear() + "-" + String.format("%02d", a.getDate().getMonthValue())
                ));
            
            List<Map<String, Object>> monthlyData = new ArrayList<>();
            
            for (Map.Entry<String, List<Attendance>> entry : byMonth.entrySet()) {
                List<Attendance> monthAtt = entry.getValue();
                long present = monthAtt.stream()
                    .filter(a -> "PRESENT".equals(a.getStatus()))
                    .count();
                long late = monthAtt.stream()
                    .filter(a -> "LATE".equals(a.getStatus()))
                    .count();
                long absent = monthAtt.stream()
                    .filter(a -> "ABSENT".equals(a.getStatus()))
                    .count();
                
                double percentage = monthAtt.size() > 0 
                    ? ((present + late) * 100.0 / monthAtt.size()) 
                    : 0;
                
                Map<String, Object> monthStats = new HashMap<>();
                monthStats.put("month", entry.getKey());
                monthStats.put("total", monthAtt.size());
                monthStats.put("present", present);
                monthStats.put("late", late);
                monthStats.put("absent", absent);
                monthStats.put("percentage", Math.round(percentage * 100.0) / 100.0);
                
                monthlyData.add(monthStats);
            }
            
            // Sort by month (most recent first)
            monthlyData.sort((m1, m2) -> m2.get("month").toString()
                .compareTo(m1.get("month").toString()));
            
            return ResponseEntity.ok(monthlyData);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error fetching monthly attendance: " + e.getMessage()));
        }
    }
    
    /**
     * Get today's attendance status
     * Endpoint: GET /api/student/{studentId}/attendance/today
     */
    @GetMapping("/{studentId}/attendance/today")
    public ResponseEntity<?> getTodayAttendance(@PathVariable Long studentId) {
        try {
            User student = userService.getUserById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));
            
            LocalDate today = LocalDate.now();
            
            List<Attendance> todayAttendance = attendanceRepository.findByStudentId(studentId)
                .stream()
                .filter(a -> a.getDate().equals(today))
                .collect(Collectors.toList());
            
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (Attendance att : todayAttendance) {
                Map<String, Object> attMap = new HashMap<>();
                attMap.put("id", att.getId());
                attMap.put("status", att.getStatus());
                attMap.put("verified", att.isVerified());
                
                if (att.getSubject() != null) {
                    attMap.put("subject", att.getSubject().getSubjectName());
                }
                
                result.add(attMap);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error fetching today's attendance: " + e.getMessage()));
        }
    }
}