package com.attendance.attendancebackend.repository;

import com.attendance.attendancebackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    List<User> findByRole(String role);
    
    @Query("SELECT u FROM User u WHERE u.role = 'STUDENT'")
    List<User> findAllStudents();
    
    @Query("SELECT u FROM User u WHERE u.role = 'FACULTY'")
    List<User> findAllFaculty();
    
    @Query("SELECT u FROM User u WHERE u.role = 'STUDENT' AND u.department = :department")
    List<User> findStudentsByDepartment(@Param("department") String department);
    
    @Query("SELECT u FROM User u WHERE u.role = 'STUDENT' AND u.department = :department AND u.semester = :semester")
    List<User> findStudentsByDepartmentAndSemester(@Param("department") String department, @Param("semester") Integer semester);
    
    @Query("SELECT u FROM User u WHERE u.role = 'FACULTY' AND u.department = :department")
    List<User> findFacultyByDepartment(@Param("department") String department);
    
    boolean existsByEmail(String email);
}