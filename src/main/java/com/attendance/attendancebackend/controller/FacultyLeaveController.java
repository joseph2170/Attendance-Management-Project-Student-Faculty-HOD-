package com.attendance.attendancebackend.controller;

import com.attendance.attendancebackend.model.LeaveRequest;
import com.attendance.attendancebackend.model.User;
import com.attendance.attendancebackend.service.LeaveRequestService;
import com.attendance.attendancebackend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/faculty/leave")
@CrossOrigin(origins = "*")
public class FacultyLeaveController {
    
    private final LeaveRequestService leaveRequestService;
    private final UserService userService;
    
    public FacultyLeaveController(LeaveRequestService leaveRequestService, UserService userService) {
        this.leaveRequestService = leaveRequestService;
        this.userService = userService;
    }
    
    @GetMapping("/{facultyId}")
    public ResponseEntity<?> getFacultyLeaveRequests(@PathVariable Long facultyId) {
        try {
            User faculty = userService.getUserById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
            
            List<LeaveRequest> leaveRequests = leaveRequestService.getLeaveRequestsByFaculty(faculty);
            
            List<Map<String, Object>> formattedRequests = leaveRequests.stream()
                .map(lr -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", lr.getId());
                    map.put("fromDate", lr.getFromDate().toString());
                    map.put("toDate", lr.getToDate().toString());
                    map.put("reason", lr.getReason());
                    map.put("leaveType", lr.getLeaveType());
                    map.put("status", lr.getStatus());
                    map.put("appliedAt", lr.getAppliedAt().toString());
                    
                    long days = ChronoUnit.DAYS.between(lr.getFromDate(), lr.getToDate()) + 1;
                    map.put("days", days);
                    
                    if (lr.getApprovedBy() != null) {
                        map.put("approvedBy", lr.getApprovedBy().getName());
                        map.put("approvedAt", lr.getApprovedAt() != null ? lr.getApprovedAt().toString() : null);
                        map.put("remarks", lr.getRemarks());
                    }
                    return map;
                })
                .collect(Collectors.toList());
            
            int leaveBalance = leaveRequestService.getLeaveBalance(faculty, LocalDate.now().getYear());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("leaveRequests", formattedRequests);
            response.put("totalRequests", formattedRequests.size());
            response.put("pendingCount", leaveRequests.stream().filter(lr -> "PENDING".equals(lr.getStatus())).count());
            response.put("approvedCount", leaveRequests.stream().filter(lr -> "APPROVED".equals(lr.getStatus())).count());
            response.put("rejectedCount", leaveRequests.stream().filter(lr -> "REJECTED".equals(lr.getStatus())).count());
            response.put("leaveBalance", leaveBalance);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error fetching leave requests: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping("/request")
    public ResponseEntity<?> createLeaveRequest(@RequestBody Map<String, Object> request) {
        try {
            Long facultyId = Long.parseLong(request.get("facultyId").toString());
            String fromDateStr = request.get("fromDate").toString();
            String toDateStr = request.get("toDate").toString();
            String reason = request.get("reason").toString();
            String leaveType = request.get("leaveType").toString();
            
            User faculty = userService.getUserById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
            
            LocalDate fromDate = LocalDate.parse(fromDateStr);
            LocalDate toDate = LocalDate.parse(toDateStr);
            
            if (fromDate.isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest().body("From date cannot be in the past");
            }
            
            if (toDate.isBefore(fromDate)) {
                return ResponseEntity.badRequest().body("To date must be after from date");
            }
            
            int leaveBalance = leaveRequestService.getLeaveBalance(faculty, LocalDate.now().getYear());
            long requestedDays = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
            
            if (requestedDays > leaveBalance) {
                return ResponseEntity.badRequest()
                    .body("Insufficient leave balance. Available: " + leaveBalance + " days");
            }
            
            LeaveRequest leaveRequest = new LeaveRequest();
            leaveRequest.setFaculty(faculty);
            leaveRequest.setFromDate(fromDate);
            leaveRequest.setToDate(toDate);
            leaveRequest.setReason(reason);
            leaveRequest.setLeaveType(leaveType);
            
            LeaveRequest saved = leaveRequestService.createLeaveRequest(leaveRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Leave request submitted successfully");
            response.put("leaveRequest", saved);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error creating leave request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @DeleteMapping("/cancel/{leaveId}")
    public ResponseEntity<?> cancelLeaveRequest(@PathVariable Long leaveId, @RequestParam Long facultyId) {
        try {
            User faculty = userService.getUserById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
            
            leaveRequestService.cancelLeaveRequest(leaveId, faculty);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Leave request cancelled successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error cancelling leave request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}