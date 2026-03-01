package com.attendance.attendancebackend.config;

import com.attendance.attendancebackend.model.User;
import com.attendance.attendancebackend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    
    private final UserRepository userRepository;
    
    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            
            // Create HOD
            User hod = new User();
            hod.setName("Dr. Rajesh Kumar");
            hod.setEmail("hod.cse@example.com");
            hod.setPassword("1234");
            hod.setRole("HOD");
            hod.setDepartment("Computer Science");
            hod.setDesignation("Professor & HOD");
            hod.setPhoneNumber("9876543210");
            userRepository.save(hod);
            
            // Create Faculty
            User faculty1 = new User();
            faculty1.setName("Prof. Amit Sharma");
            faculty1.setEmail("amit.sharma@example.com");
            faculty1.setPassword("1234");
            faculty1.setRole("FACULTY");
            faculty1.setDepartment("Computer Science");
            faculty1.setDesignation("Associate Professor");
            faculty1.setPhoneNumber("9876543211");
            userRepository.save(faculty1);
            
            User faculty2 = new User();
            faculty2.setName("Prof. Priya Patel");
            faculty2.setEmail("priya.patel@example.com");
            faculty2.setPassword("1234");
            faculty2.setRole("FACULTY");
            faculty2.setDepartment("Computer Science");
            faculty2.setDesignation("Assistant Professor");
            faculty2.setPhoneNumber("9876543212");
            userRepository.save(faculty2);
            
            // Create Students
            for (int i = 1; i <= 10; i++) {
                User student = new User();
                student.setName("Student " + i);
                student.setEmail("student" + i + "@example.com");
                student.setPassword("1234");
                student.setRole("STUDENT");
                student.setDepartment("Computer Science");
                student.setSemester(1);
                student.setRollNumber("CS202400" + i);
                student.setPhoneNumber("998877660" + i);
                userRepository.save(student);
            }
        }
    }
}