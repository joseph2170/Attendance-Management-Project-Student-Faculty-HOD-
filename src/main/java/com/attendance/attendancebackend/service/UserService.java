package com.attendance.attendancebackend.service;

import com.attendance.attendancebackend.model.User;
import com.attendance.attendancebackend.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public User saveUser(User user) {
        return userRepository.save(user);
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public List<User> getStudentsByDepartment(String department) {
        return userRepository.findStudentsByDepartment(department);
    }
    
    public List<User> getStudentsByDepartmentAndSemester(String department, Integer semester) {
        if (semester == null) {
            return getStudentsByDepartment(department);
        }
        return userRepository.findStudentsByDepartmentAndSemester(department, semester);
    }
    
    public List<User> getFacultyByDepartment(String department) {
        return userRepository.findFacultyByDepartment(department);
    }
    
    public boolean authenticate(String email, String password) {
        Optional<User> user = userRepository.findByEmail(email);
        return user.isPresent() && user.get().getPassword().equals(password);
    }
}