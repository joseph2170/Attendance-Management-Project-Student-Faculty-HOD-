package com.attendance.attendancebackend.service;

import com.attendance.attendancebackend.model.*;
import com.attendance.attendancebackend.repository.*;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final SubjectRepository subjectRepository;
    private final ClassScheduleRepository classScheduleRepository;
    
    public DashboardService(
            UserRepository userRepository,
            AttendanceRepository attendanceRepository,
            SubjectRepository subjectRepository,
            ClassScheduleRepository classScheduleRepository) {
        this.userRepository = userRepository;
        this.attendanceRepository = attendanceRepository;
        this.subjectRepository = subjectRepository;
        this.classScheduleRepository = classScheduleRepository;
    }
    
    public Map<String, Object> getStudentDashboardData(Long studentId) {
        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found"));
        
        Map<String, Object> dashboardData = new HashMap<>();
        dashboardData.put("student", student);
        
        long totalClasses = attendanceRepository.countTotalAttendance(student);
        long presentClasses = attendanceRepository.countPresentAttendance(student);
        long lateClasses = attendanceRepository.countLateAttendance(student);
        
        double attendancePercentage = totalClasses > 0 
            ? ((presentClasses + lateClasses) * 100.0) / totalClasses 
            : 0.0;
        
        Map<String, Object> attendanceStats = new HashMap<>();
        attendanceStats.put("total", totalClasses);
        attendanceStats.put("present", presentClasses);
        attendanceStats.put("late", lateClasses);
        attendanceStats.put("absent", totalClasses - presentClasses - lateClasses);
        attendanceStats.put("percentage", Math.round(attendancePercentage * 100.0) / 100.0);
        
        dashboardData.put("attendanceStats", attendanceStats);
        
        List<Subject> subjects = subjectRepository.findByDepartmentAndSemester(
            student.getDepartment(), student.getSemester());
        
        List<Map<String, Object>> subjectWiseAttendance = new ArrayList<>();
        for (Subject subject : subjects) {
            List<Attendance> subjectAttendance = attendanceRepository
                .findByStudentAndSubjectAndVerifiedTrue(student, subject);
            
            long subjectTotal = subjectAttendance.size();
            long subjectPresent = subjectAttendance.stream()
                .filter(a -> "PRESENT".equals(a.getStatus())).count();
            long subjectLate = subjectAttendance.stream()
                .filter(a -> "LATE".equals(a.getStatus())).count();
            
            double subjectPercentage = subjectTotal > 0 
                ? ((subjectPresent + subjectLate) * 100.0) / subjectTotal 
                : 0.0;
            
            Map<String, Object> subjectData = new HashMap<>();
            subjectData.put("subject", subject);
            subjectData.put("subjectName", subject.getSubjectName());
            subjectData.put("subjectCode", subject.getSubjectCode());
            subjectData.put("total", subjectTotal);
            subjectData.put("present", subjectPresent);
            subjectData.put("late", subjectLate);
            subjectData.put("absent", subjectTotal - subjectPresent - subjectLate);
            subjectData.put("percentage", Math.round(subjectPercentage * 100.0) / 100.0);
            
            subjectWiseAttendance.add(subjectData);
        }
        
        dashboardData.put("subjectWiseAttendance", subjectWiseAttendance);
        
        List<Attendance> recentAttendance = attendanceRepository
            .findByStudentAndVerifiedTrue(student)
            .stream()
            .sorted((a1, a2) -> a2.getDate().compareTo(a1.getDate()))
            .limit(10)
            .collect(Collectors.toList());
        
        dashboardData.put("recentAttendance", recentAttendance);
        
        return dashboardData;
    }
    
    public Map<String, Object> getFacultyDashboardData(Long facultyId) {
        User faculty = userRepository.findById(facultyId)
            .orElseThrow(() -> new RuntimeException("Faculty not found"));
        
        Map<String, Object> dashboardData = new HashMap<>();
        dashboardData.put("faculty", faculty);
        
        String today = LocalDate.now().getDayOfWeek().toString().toUpperCase();
        List<ClassSchedule> todaySchedule = classScheduleRepository
            .findByFacultyIdAndDayOfWeek(facultyId, today);
        
        dashboardData.put("todaySchedule", todaySchedule);
        dashboardData.put("todayClassesCount", todaySchedule.size());
        
        List<ClassSchedule> weeklySchedule = classScheduleRepository.findByFacultyId(facultyId);
        dashboardData.put("weeklySchedule", weeklySchedule);
        
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
        List<Attendance> recentMarkings = attendanceRepository
            .findByMarkedByIdAndDateBetween(facultyId, sevenDaysAgo, LocalDate.now());
        
        dashboardData.put("recentMarkings", recentMarkings);
        
        return dashboardData;
    }
    
    public Map<String, Object> getHODDashboardData(Long hodId, String department) {
        Map<String, Object> dashboardData = new HashMap<>();
        
        List<User> students = userRepository.findStudentsByDepartment(department);
        List<User> faculty = userRepository.findFacultyByDepartment(department);
        List<Subject> subjects = subjectRepository.findByDepartment(department);
        
        dashboardData.put("totalStudents", students != null ? students.size() : 0);
        dashboardData.put("totalFaculty", faculty != null ? faculty.size() : 0);
        dashboardData.put("totalSubjects", subjects != null ? subjects.size() : 0);
        
        List<Attendance> pendingVerifications = attendanceRepository.findByVerifiedFalse();
        dashboardData.put("pendingVerifications", pendingVerifications != null ? pendingVerifications.size() : 0);
        
        LocalDate today = LocalDate.now();
        List<Attendance> todayAttendance = attendanceRepository.findByDate(today);
        
        long todayPresent = todayAttendance.stream()
            .filter(a -> "PRESENT".equals(a.getStatus())).count();
        long todayAbsent = todayAttendance.stream()
            .filter(a -> "ABSENT".equals(a.getStatus())).count();
        long todayLate = todayAttendance.stream()
            .filter(a -> "LATE".equals(a.getStatus())).count();
        
        Map<String, Object> todayStats = new HashMap<>();
        todayStats.put("total", todayAttendance.size());
        todayStats.put("present", todayPresent);
        todayStats.put("absent", todayAbsent);
        todayStats.put("late", todayLate);
        
        dashboardData.put("todayAttendance", todayStats);
        
        return dashboardData;
    }
}