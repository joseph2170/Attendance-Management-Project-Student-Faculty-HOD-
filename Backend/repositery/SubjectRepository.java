package com.attendance.attendancebackend.repository;

import com.attendance.attendancebackend.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    
    List<Subject> findByDepartment(String department);
    
    List<Subject> findByDepartmentAndSemester(String department, Integer semester);
    
    List<Subject> findBySemester(Integer semester);
    
    @Query("SELECT s FROM Subject s WHERE s.department = :department AND s.semester = :semester")
    List<Subject> findSubjectsByDepartmentAndSemester(@Param("department") String department, @Param("semester") Integer semester);
    
    Subject findBySubjectCode(String subjectCode);
}