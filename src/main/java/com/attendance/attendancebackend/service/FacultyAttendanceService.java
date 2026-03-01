package com.attendance.attendancebackend.service;

import com.attendance.attendancebackend.model.FacultyAttendance;
import com.attendance.attendancebackend.model.User;
import com.attendance.attendancebackend.repository.FacultyAttendanceRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Service
public class FacultyAttendanceService {
    
    private final FacultyAttendanceRepository facultyAttendanceRepository;
    
    public FacultyAttendanceService(FacultyAttendanceRepository facultyAttendanceRepository) {
        this.facultyAttendanceRepository = facultyAttendanceRepository;
    }
    
    public FacultyAttendance markAttendance(FacultyAttendance attendance) {
        if (attendance.getAttendanceDate() == null) {
            attendance.setAttendanceDate(LocalDate.now());
        }
        attendance.setMarkedAt(LocalDateTime.now());
        return facultyAttendanceRepository.save(attendance);
    }
    
    public List<FacultyAttendance> markBulkAttendance(List<FacultyAttendance> attendances) {
        attendances.forEach(a -> {
            if (a.getAttendanceDate() == null) {
                a.setAttendanceDate(LocalDate.now());
            }
            a.setMarkedAt(LocalDateTime.now());
        });
        return facultyAttendanceRepository.saveAll(attendances);
    }
    
    public Optional<FacultyAttendance> getByFacultyIdAndDate(Long facultyId, LocalDate date) {
        return facultyAttendanceRepository.findByFacultyIdAndAttendanceDate(facultyId, date);
    }
    
    public List<FacultyAttendance> getByFacultyId(Long facultyId) {
        return facultyAttendanceRepository.findByFacultyId(facultyId);
    }
    
    public List<FacultyAttendance> getByDate(LocalDate date) {
        return facultyAttendanceRepository.findByAttendanceDate(date);
    }
    
    public List<FacultyAttendance> getByDateRange(LocalDate startDate, LocalDate endDate) {
        return facultyAttendanceRepository.findByAttendanceDateBetween(startDate, endDate);
    }
    
    public List<FacultyAttendance> getByDepartmentAndDate(String department, LocalDate date) {
        return facultyAttendanceRepository.findByDepartmentAndDate(department, date);
    }
    
    public boolean isAttendanceMarked(Long facultyId, LocalDate date) {
        return facultyAttendanceRepository.existsByFacultyIdAndAttendanceDate(facultyId, date);
    }
    
    public Map<String, Object> getFacultyAttendanceStats(Long facultyId, LocalDate startDate, LocalDate endDate) {
        List<FacultyAttendance> attendances = facultyAttendanceRepository.findByFacultyId(facultyId)
            .stream()
            .filter(a -> !a.getAttendanceDate().isBefore(startDate) && !a.getAttendanceDate().isAfter(endDate))
            .toList();
        
        long total = attendances.size();
        long present = attendances.stream().filter(a -> "PRESENT".equals(a.getStatus())).count();
        long absent = attendances.stream().filter(a -> "ABSENT".equals(a.getStatus())).count();
        long leave = attendances.stream().filter(a -> "LEAVE".equals(a.getStatus())).count();
        long late = attendances.stream().filter(a -> "LATE".equals(a.getStatus())).count();
        
        double percentage = total > 0 ? (present * 100.0) / total : 0.0;
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("present", present);
        stats.put("absent", absent);
        stats.put("leave", leave);
        stats.put("late", late);
        stats.put("percentage", Math.round(percentage * 100.0) / 100.0);
        
        return stats;
    }
}