package com.attendance.attendancebackend.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "faculty_attendance")
public class FacultyAttendance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "faculty_id")
    private User faculty;
    
    @Column(name = "attendance_date")
    private LocalDate attendanceDate;
    
    private String status; // PRESENT, ABSENT, LEAVE, LATE
    
    @ManyToOne
    @JoinColumn(name = "marked_by")
    private User markedBy;
    
    private String remarks;
    
    @Column(name = "marked_at")
    private LocalDateTime markedAt;
    
    // Constructors
    public FacultyAttendance() {
        this.markedAt = LocalDateTime.now();
    }
    
    public FacultyAttendance(User faculty, LocalDate attendanceDate, String status, User markedBy, String remarks) {
        this.faculty = faculty;
        this.attendanceDate = attendanceDate;
        this.status = status;
        this.markedBy = markedBy;
        this.remarks = remarks;
        this.markedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getFaculty() { return faculty; }
    public void setFaculty(User faculty) { this.faculty = faculty; }
    
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public User getMarkedBy() { return markedBy; }
    public void setMarkedBy(User markedBy) { this.markedBy = markedBy; }
    
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    
    public LocalDateTime getMarkedAt() { return markedAt; }
    public void setMarkedAt(LocalDateTime markedAt) { this.markedAt = markedAt; }
}