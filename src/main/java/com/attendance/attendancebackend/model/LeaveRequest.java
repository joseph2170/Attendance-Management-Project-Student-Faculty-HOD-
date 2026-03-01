package com.attendance.attendancebackend.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests")
public class LeaveRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "faculty_id")
    private User faculty;
    
    @Column(name = "from_date")
    private LocalDate fromDate;
    
    @Column(name = "to_date")
    private LocalDate toDate;
    
    private String reason;
    
    private String status; // PENDING, APPROVED, REJECTED
    
    @Column(name = "leave_type")
    private String leaveType; // SICK, CASUAL, EMERGENCY, VACATION, OTHER
    
    @ManyToOne
    @JoinColumn(name = "approved_by")
    private User approvedBy;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "applied_at")
    private LocalDateTime appliedAt;
    
    private String remarks;
    
    // Constructors
    public LeaveRequest() {
        this.appliedAt = LocalDateTime.now();
        this.status = "PENDING";
    }
    
    public LeaveRequest(User faculty, LocalDate fromDate, LocalDate toDate, String reason, String leaveType) {
        this.faculty = faculty;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.reason = reason;
        this.leaveType = leaveType;
        this.appliedAt = LocalDateTime.now();
        this.status = "PENDING";
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getFaculty() { return faculty; }
    public void setFaculty(User faculty) { this.faculty = faculty; }
    
    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }
    
    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }
    
    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }
    
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}