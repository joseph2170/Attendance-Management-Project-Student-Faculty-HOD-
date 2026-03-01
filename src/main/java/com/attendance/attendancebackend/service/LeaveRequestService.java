package com.attendance.attendancebackend.service;

import com.attendance.attendancebackend.model.LeaveRequest;
import com.attendance.attendancebackend.model.User;
import com.attendance.attendancebackend.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class LeaveRequestService {
    
    private final LeaveRequestRepository leaveRequestRepository;
    
    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
    }
    
    public LeaveRequest createLeaveRequest(LeaveRequest leaveRequest) {
        leaveRequest.setAppliedAt(LocalDateTime.now());
        leaveRequest.setStatus("PENDING");
        return leaveRequestRepository.save(leaveRequest);
    }
    
    public Optional<LeaveRequest> getLeaveRequestById(Long id) {
        return leaveRequestRepository.findById(id);
    }
    
    public List<LeaveRequest> getLeaveRequestsByFaculty(User faculty) {
        return leaveRequestRepository.findByFaculty(faculty);
    }
    
    public List<LeaveRequest> getLeaveRequestsByFacultyId(Long facultyId) {
        return leaveRequestRepository.findByFacultyId(facultyId);
    }
    
    public List<LeaveRequest> getPendingLeaveRequestsByDepartment(String department) {
        return leaveRequestRepository.findPendingByDepartment(department);
    }
    
    public LeaveRequest approveLeaveRequest(Long id, User hod, String remarks) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Leave request not found"));
        
        leaveRequest.setStatus("APPROVED");
        leaveRequest.setApprovedBy(hod);
        leaveRequest.setApprovedAt(LocalDateTime.now());
        leaveRequest.setRemarks(remarks);
        
        return leaveRequestRepository.save(leaveRequest);
    }
    
    public LeaveRequest rejectLeaveRequest(Long id, User hod, String remarks) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Leave request not found"));
        
        leaveRequest.setStatus("REJECTED");
        leaveRequest.setApprovedBy(hod);
        leaveRequest.setApprovedAt(LocalDateTime.now());
        leaveRequest.setRemarks(remarks);
        
        return leaveRequestRepository.save(leaveRequest);
    }
    
    public void cancelLeaveRequest(Long id, User faculty) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Leave request not found"));
        
        if (!leaveRequest.getFaculty().getId().equals(faculty.getId())) {
            throw new RuntimeException("You can only cancel your own leave requests");
        }
        
        if (!"PENDING".equals(leaveRequest.getStatus())) {
            throw new RuntimeException("Can only cancel pending requests");
        }
        
        leaveRequestRepository.delete(leaveRequest);
    }
    
    public int getLeaveBalance(User faculty, int year) {
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 31);
        
        List<LeaveRequest> approvedLeaves = leaveRequestRepository
            .findByFacultyAndStatus(faculty, "APPROVED");
        
        int totalDays = 0;
        for (LeaveRequest lr : approvedLeaves) {
            if (!lr.getFromDate().isBefore(startOfYear) && !lr.getToDate().isAfter(endOfYear)) {
                long days = ChronoUnit.DAYS.between(lr.getFromDate(), lr.getToDate()) + 1;
                totalDays += (int) days;
            }
        }
        
        return Math.max(0, 20 - totalDays);
    }
}