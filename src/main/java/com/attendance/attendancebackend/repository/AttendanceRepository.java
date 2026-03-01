package com.attendance.attendancebackend.repository;

import com.attendance.attendancebackend.model.Attendance;
import com.attendance.attendancebackend.model.User;
import com.attendance.attendancebackend.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    
    List<Attendance> findByStudentId(Long studentId);
    
    List<Attendance> findByStudent(User student);
    
    List<Attendance> findByStudentAndVerifiedTrue(User student);
    
    List<Attendance> findByStudentAndSubjectAndVerifiedTrue(User student, Subject subject);
    
    List<Attendance> findByClassScheduleIdAndDate(Long classScheduleId, LocalDate date);
    
    Optional<Attendance> findByStudentIdAndClassScheduleIdAndDate(Long studentId, Long classScheduleId, LocalDate date);
    
    boolean existsByClassScheduleIdAndDate(Long classScheduleId, LocalDate date);
    
    List<Attendance> findByVerifiedFalse();
    
    List<Attendance> findByDate(LocalDate date);
    
    List<Attendance> findByDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<Attendance> findByMarkedByIdAndDateBetween(Long markedById, LocalDate startDate, LocalDate endDate);
    
    boolean existsByStudentAndSubjectAndDate(User student, Subject subject, LocalDate date);
    
    boolean existsByStudentIdAndClassScheduleIdAndDate(Long studentId, Long classScheduleId, LocalDate date);
    
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student = :student")
    long countTotalAttendance(@Param("student") User student);
    
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student = :student AND a.status = 'PRESENT'")
    long countPresentAttendance(@Param("student") User student);
    
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student = :student AND a.status = 'LATE'")
    long countLateAttendance(@Param("student") User student);
    
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student = :student AND a.status = 'ABSENT'")
    long countAbsentAttendance(@Param("student") User student);
}