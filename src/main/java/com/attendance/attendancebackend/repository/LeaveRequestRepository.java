package com.attendance.attendancebackend.repository;

import com.attendance.attendancebackend.model.LeaveRequest;
import com.attendance.attendancebackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    
    List<LeaveRequest> findByFacultyId(Long facultyId);
    
    List<LeaveRequest> findByFaculty(User faculty);
    
    List<LeaveRequest> findByStatus(String status);
    
    List<LeaveRequest> findByFacultyAndStatus(User faculty, String status);
    
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.faculty.department = :department AND lr.status = 'PENDING' ORDER BY lr.appliedAt DESC")
    List<LeaveRequest> findPendingByDepartment(@Param("department") String department);
    
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.faculty.department = :department AND lr.status = 'APPROVED' AND lr.fromDate <= :date AND lr.toDate >= :date")
    List<LeaveRequest> findApprovedLeavesOnDate(@Param("department") String department, @Param("date") LocalDate date);
    
    List<LeaveRequest> findByFromDateGreaterThanEqualAndToDateLessThanEqual(LocalDate startDate, LocalDate endDate);
    
    long countByFacultyAndStatus(User faculty, String status);
}