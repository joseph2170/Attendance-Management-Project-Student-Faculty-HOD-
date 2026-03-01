package com.attendance.attendancebackend.controller;

import com.attendance.attendancebackend.model.*;
import com.attendance.attendancebackend.service.*;
import com.attendance.attendancebackend.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/faculty")
@CrossOrigin(origins = "*")
public class FacultyController {
    
    private final UserService userService;
    private final AttendanceService attendanceService;
    private final ClassScheduleRepository classScheduleRepository;
    private final AttendanceRepository attendanceRepository;
    
    public FacultyController(
            UserService userService,
            AttendanceService attendanceService,
            ClassScheduleRepository classScheduleRepository,
            AttendanceRepository attendanceRepository) {
        this.userService = userService;
        this.attendanceService = attendanceService;
        this.classScheduleRepository = classScheduleRepository;
        this.attendanceRepository = attendanceRepository;
    }
    
    @GetMapping("/{facultyId}/classes/today")
    public ResponseEntity<?> getTodayClasses(@PathVariable Long facultyId) {
        try {
            LocalDate today = LocalDate.now();
            String dayOfWeek = today.getDayOfWeek().toString().toUpperCase();
            
            List<ClassSchedule> todayClasses = classScheduleRepository
                .findByFacultyIdAndDayOfWeek(facultyId, dayOfWeek);
            
            List<Map<String, Object>> classList = new ArrayList<>();
            
            for (ClassSchedule cs : todayClasses) {
                Map<String, Object> classMap = new HashMap<>();
                classMap.put("id", cs.getId());
                classMap.put("startTime", cs.getStartTime().toString());
                classMap.put("endTime", cs.getEndTime().toString());
                classMap.put("roomNumber", cs.getRoomNumber());
                classMap.put("semester", cs.getSemester());
                classMap.put("department", cs.getDepartment());
                
                if (cs.getSubject() != null) {
                    Map<String, Object> subjectMap = new HashMap<>();
                    subjectMap.put("id", cs.getSubject().getId());
                    subjectMap.put("subjectName", cs.getSubject().getSubjectName());
                    subjectMap.put("subjectCode", cs.getSubject().getSubjectCode());
                    classMap.put("subject", subjectMap);
                }
                
                boolean attendanceMarked = attendanceRepository
                    .existsByClassScheduleIdAndDate(cs.getId(), today);
                classMap.put("attendanceMarked", attendanceMarked);
                
                classList.add(classMap);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("date", today.toString());
            response.put("day", dayOfWeek);
            response.put("classes", classList);
            response.put("totalClasses", classList.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching today's classes: " + e.getMessage());
        }
    }
    
    @GetMapping("/{facultyId}/class/{classId}/students")
    public ResponseEntity<?> getClassStudents(
            @PathVariable Long facultyId,
            @PathVariable Long classId,
            @RequestParam(required = false) LocalDate date) {
        try {
            if (date == null) {
                date = LocalDate.now();
            }
            
            ClassSchedule classSchedule = classScheduleRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
            
            List<User> students = userService.getStudentsByDepartmentAndSemester(
                classSchedule.getDepartment(), classSchedule.getSemester());
            
            List<Attendance> todayAttendance = attendanceRepository
                .findByClassScheduleIdAndDate(classId, date);
            
            Map<Long, Boolean> attendanceMarked = new HashMap<>();
            Map<Long, String> attendanceStatus = new HashMap<>();
            
            for (Attendance att : todayAttendance) {
                if (att.getStudent() != null) {
                    attendanceMarked.put(att.getStudent().getId(), true);
                    attendanceStatus.put(att.getStudent().getId(), att.getStatus());
                }
            }
            
            List<Map<String, Object>> studentList = new ArrayList<>();
            for (User student : students) {
                Map<String, Object> studentMap = new HashMap<>();
                studentMap.put("id", student.getId());
                studentMap.put("name", student.getName());
                studentMap.put("email", student.getEmail());
                studentMap.put("rollNumber", student.getRollNumber());
                studentMap.put("semester", student.getSemester());
                studentMap.put("attendanceMarked", attendanceMarked.getOrDefault(student.getId(), false));
                studentMap.put("attendanceStatus", attendanceStatus.getOrDefault(student.getId(), "PRESENT"));
                
                studentList.add(studentMap);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("classId", classId);
            response.put("date", date.toString());
            response.put("subject", classSchedule.getSubject());
            response.put("totalStudents", students.size());
            response.put("markedCount", todayAttendance.size());
            response.put("students", studentList);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching class students: " + e.getMessage());
        }
    }
    
    @PostMapping("/attendance/mark")
    public ResponseEntity<?> markAttendance(@RequestBody Map<String, Object> request) {
        try {
            Long facultyId = Long.parseLong(request.get("facultyId").toString());
            Long classId = Long.parseLong(request.get("classId").toString());
            String dateStr = request.get("date").toString();
            LocalDate date = LocalDate.parse(dateStr);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> attendanceList = (List<Map<String, Object>>) request.get("attendance");
            
            User faculty = userService.getUserById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
            
            ClassSchedule classSchedule = classScheduleRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
            
            List<Attendance> savedAttendances = new ArrayList<>();
            int markedCount = 0;
            int updatedCount = 0;
            
            for (Map<String, Object> attData : attendanceList) {
                Long studentId = Long.parseLong(attData.get("studentId").toString());
                String status = attData.get("status").toString();
                
                Optional<Attendance> existingAtt = attendanceRepository
                    .findByStudentIdAndClassScheduleIdAndDate(studentId, classId, date);
                
                User student = userService.getUserById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found"));
                
                if (existingAtt.isPresent()) {
                    Attendance attendance = existingAtt.get();
                    attendance.setStatus(status);
                    attendance.setMarkedBy(faculty);
                    attendance.setMarkedAt(LocalDateTime.now());
                    Attendance saved = attendanceRepository.save(attendance);
                    savedAttendances.add(saved);
                    updatedCount++;
                } else {
                    Attendance attendance = new Attendance();
                    attendance.setStudent(student);
                    attendance.setSubject(classSchedule.getSubject());
                    attendance.setClassSchedule(classSchedule);
                    attendance.setDate(date);
                    attendance.setStatus(status);
                    attendance.setVerified(false);
                    attendance.setMarkedBy(faculty);
                    attendance.setMarkedAt(LocalDateTime.now());
                    
                    Attendance saved = attendanceRepository.save(attendance);
                    savedAttendances.add(saved);
                    markedCount++;
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Attendance marked successfully");
            response.put("marked", markedCount);
            response.put("updated", updatedCount);
            response.put("total", attendanceList.size());
            response.put("date", date.toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error marking attendance: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/{facultyId}/attendance/recent")
    public ResponseEntity<?> getRecentAttendance(@PathVariable Long facultyId) {
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(7);
            
            List<Attendance> recentAttendance = attendanceRepository
                .findByMarkedByIdAndDateBetween(facultyId, startDate, endDate);
            
            List<Map<String, Object>> formattedList = new ArrayList<>();
            for (Attendance att : recentAttendance) {
                Map<String, Object> attMap = new HashMap<>();
                attMap.put("id", att.getId());
                attMap.put("date", att.getDate().toString());
                attMap.put("status", att.getStatus());
                attMap.put("verified", att.isVerified());
                
                if (att.getStudent() != null) {
                    attMap.put("studentName", att.getStudent().getName());
                    attMap.put("rollNumber", att.getStudent().getRollNumber());
                }
                
                if (att.getSubject() != null) {
                    attMap.put("subjectName", att.getSubject().getSubjectName());
                }
                
                formattedList.add(attMap);
            }
            
            formattedList.sort((a, b) -> b.get("date").toString().compareTo(a.get("date").toString()));
            
            return ResponseEntity.ok(formattedList);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching recent attendance: " + e.getMessage());
        }
    }
    
    @GetMapping("/{facultyId}/schedule/weekly")
    public ResponseEntity<?> getWeeklySchedule(@PathVariable Long facultyId) {
        try {
            List<ClassSchedule> allClasses = classScheduleRepository.findByFacultyId(facultyId);
            
            Map<String, List<Map<String, Object>>> scheduleByDay = new LinkedHashMap<>();
            List<String> dayOrder = Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY");
            
            for (String day : dayOrder) {
                scheduleByDay.put(day, new ArrayList<>());
            }
            
            for (ClassSchedule cs : allClasses) {
                Map<String, Object> classMap = new HashMap<>();
                classMap.put("id", cs.getId());
                classMap.put("startTime", cs.getStartTime().toString());
                classMap.put("endTime", cs.getEndTime().toString());
                classMap.put("roomNumber", cs.getRoomNumber());
                classMap.put("semester", cs.getSemester());
                
                if (cs.getSubject() != null) {
                    classMap.put("subjectName", cs.getSubject().getSubjectName());
                    classMap.put("subjectCode", cs.getSubject().getSubjectCode());
                }
                
                String day = cs.getDayOfWeek();
                if (scheduleByDay.containsKey(day)) {
                    scheduleByDay.get(day).add(classMap);
                }
            }
            
            scheduleByDay.entrySet().removeIf(entry -> entry.getValue().isEmpty());
            
            return ResponseEntity.ok(scheduleByDay);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching weekly schedule: " + e.getMessage());
        }
    }
}