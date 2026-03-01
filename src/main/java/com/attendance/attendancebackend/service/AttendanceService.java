package com.attendance.attendancebackend.service;

import com.attendance.attendancebackend.model.Attendance;
import com.attendance.attendancebackend.model.User;
import com.attendance.attendancebackend.model.Subject;
import com.attendance.attendancebackend.repository.AttendanceRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Service
public class AttendanceService {
    
    private final AttendanceRepository attendanceRepository;
    
    public AttendanceService(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }
    
    public Attendance markAttendance(Attendance attendance) {
        if (attendance.getDate() == null) {
            attendance.setDate(LocalDate.now());
        }
        attendance.setMarkedAt(LocalDateTime.now());
        return attendanceRepository.save(attendance);
    }
    
    public List<Attendance> markBulkAttendance(List<Attendance> attendances) {
        attendances.forEach(a -> {
            if (a.getDate() == null) {
                a.setDate(LocalDate.now());
            }
            a.setMarkedAt(LocalDateTime.now());
        });
        return attendanceRepository.saveAll(attendances);
    }
    
    public List<Attendance> getByStudent(Long studentId) {
        return attendanceRepository.findByStudentId(studentId);
    }
    
    public List<Attendance> getAll() {
        return attendanceRepository.findAll();
    }
    
    public List<Attendance> getByDate(LocalDate date) {
        return attendanceRepository.findByDate(date);
    }
    
    public List<Attendance> getAttendanceByDateRange(LocalDate startDate, LocalDate endDate) {
        return attendanceRepository.findByDateBetween(startDate, endDate);
    }
    
    public List<Attendance> getPendingVerifications() {
        return attendanceRepository.findByVerifiedFalse();
    }
    
    public List<Attendance> getPendingAttendance() {
        return getPendingVerifications();
    }
    
    public Attendance verifyAttendance(Long id) {
        Attendance attendance = attendanceRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Attendance not found"));
        attendance.setVerified(true);
        return attendanceRepository.save(attendance);
    }
    
    public List<Attendance> verifyAllPending() {
        List<Attendance> pending = attendanceRepository.findByVerifiedFalse();
        pending.forEach(a -> a.setVerified(true));
        return attendanceRepository.saveAll(pending);
    }
    
    public List<Attendance> getStudentAttendance(User student) {
        return attendanceRepository.findByStudentAndVerifiedTrue(student);
    }
    
    public Map<String, Object> getStudentAttendancePercentage(User student) {
        long totalClasses = attendanceRepository.countTotalAttendance(student);
        long presentClasses = attendanceRepository.countPresentAttendance(student);
        long lateClasses = attendanceRepository.countLateAttendance(student);
        long absentClasses = attendanceRepository.countAbsentAttendance(student);
        
        double percentage = totalClasses > 0 ? ((presentClasses + lateClasses) * 100.0) / totalClasses : 0.0;
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalClasses", totalClasses);
        stats.put("present", presentClasses);
        stats.put("late", lateClasses);
        stats.put("absent", absentClasses);
        stats.put("percentage", Math.round(percentage * 100.0) / 100.0);
        
        return stats;
    }
    
    public boolean hasAttendanceBeenMarked(Long studentId, Long classId, LocalDate date) {
        return attendanceRepository.existsByStudentIdAndClassScheduleIdAndDate(studentId, classId, date);
    }
    
    public List<Attendance> getByClassAndDate(Long classId, LocalDate date) {
        return attendanceRepository.findByClassScheduleIdAndDate(classId, date);
    }
}