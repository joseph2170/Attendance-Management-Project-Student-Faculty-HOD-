package com.attendance.attendancebackend.repository;

import com.attendance.attendancebackend.model.ClassSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long> {
    
    List<ClassSchedule> findByFacultyId(Long facultyId);
    
    List<ClassSchedule> findByFacultyIdAndDayOfWeek(Long facultyId, String dayOfWeek);
    
    List<ClassSchedule> findByDepartmentAndSemester(String department, Integer semester);
    
    List<ClassSchedule> findBySubjectId(Long subjectId);
    
    long countByFacultyId(Long facultyId);
    
    @Query("SELECT cs FROM ClassSchedule cs JOIN FETCH cs.faculty JOIN FETCH cs.subject WHERE cs.faculty.id = :facultyId")
    List<ClassSchedule> findByFacultyIdWithDetails(@Param("facultyId") Long facultyId);
}