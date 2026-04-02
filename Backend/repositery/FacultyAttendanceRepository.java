package com.attendance.attendancebackend.repository;

import com.attendance.attendancebackend.model.FacultyAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacultyAttendanceRepository extends JpaRepository<FacultyAttendance, Long> {
    
    List<FacultyAttendance> findByFacultyId(Long facultyId);
    
    Optional<FacultyAttendance> findByFacultyIdAndAttendanceDate(Long facultyId, LocalDate date);
    
    List<FacultyAttendance> findByAttendanceDate(LocalDate date);
    
    List<FacultyAttendance> findByAttendanceDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<FacultyAttendance> findByStatus(String status);
    
    @Query("SELECT fa FROM FacultyAttendance fa WHERE fa.faculty.department = :department AND fa.attendanceDate = :date")
    List<FacultyAttendance> findByDepartmentAndDate(@Param("department") String department, @Param("date") LocalDate date);
    
    @Query("SELECT fa FROM FacultyAttendance fa WHERE fa.faculty.department = :department AND fa.attendanceDate BETWEEN :startDate AND :endDate")
    List<FacultyAttendance> findByDepartmentAndDateRange(
        @Param("department") String department,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    boolean existsByFacultyIdAndAttendanceDate(Long facultyId, LocalDate date);
    
    @Query("SELECT COUNT(fa) FROM FacultyAttendance fa WHERE fa.faculty.department = :department AND fa.attendanceDate = :date")
    long countByDepartmentAndDate(@Param("department") String department, @Param("date") LocalDate date);
}