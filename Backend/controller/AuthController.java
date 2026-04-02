package com.attendance.attendancebackend.controller;

import com.attendance.attendancebackend.model.User;
import com.attendance.attendancebackend.repository.UserRepository;
import com.attendance.attendancebackend.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    private final UserRepository userRepository;
    private final DashboardService dashboardService;
    
    public AuthController(UserRepository userRepository, DashboardService dashboardService) {
        this.userRepository = userRepository;
        this.dashboardService = dashboardService;
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            String email = credentials.get("email");
            String password = credentials.get("password");
            
            System.out.println("Login attempt - Email: " + email);
            
            Optional<User> userOpt = userRepository.findByEmail(email);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                System.out.println("User found: " + user.getName() + ", Role: " + user.getRole());
                
                if (user.getPassword().equals(password)) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Login successful");
                    response.put("userId", user.getId());
                    response.put("name", user.getName());
                    response.put("email", user.getEmail());
                    response.put("role", user.getRole());
                    response.put("department", user.getDepartment());
                    
                    if ("STUDENT".equals(user.getRole())) {
                        response.put("rollNumber", user.getRollNumber());
                        response.put("semester", user.getSemester());
                    } else if ("FACULTY".equals(user.getRole())) {
                        response.put("designation", user.getDesignation());
                    }
                    
                    return ResponseEntity.ok(response);
                } else {
                    System.out.println("Password mismatch");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Invalid password"));
                }
            } else {
                System.out.println("User not found with email: " + email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "User not found"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Server error: " + e.getMessage()));
        }
    }
    
    @GetMapping("/check-user/{email}")
    public ResponseEntity<?> checkUser(@PathVariable String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            return ResponseEntity.ok(Map.of(
                "exists", true,
                "name", user.get().getName(),
                "role", user.get().getRole()
            ));
        } else {
            return ResponseEntity.ok(Map.of("exists", false));
        }
    }
}