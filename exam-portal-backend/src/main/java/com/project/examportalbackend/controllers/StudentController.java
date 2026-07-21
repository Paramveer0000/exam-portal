package com.project.examportalbackend.controllers;

import com.project.examportalbackend.dto.ResetPasswordRequest;
import com.project.examportalbackend.dto.UpdateProfileRequest;
import com.project.examportalbackend.services.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Teacher (admin) management of their own students.
 */
@CrossOrigin
@RestController
@RequestMapping("/api/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @GetMapping("/")
    public ResponseEntity<?> getMyStudents() {
        return ResponseEntity.ok(studentService.getMyStudents());
    }

    @PutMapping("/{studentId}")
    public ResponseEntity<?> updateStudent(@PathVariable Long studentId,
                                           @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(studentService.updateStudent(studentId, request));
    }

    @PostMapping("/{studentId}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long studentId,
                                           @RequestBody ResetPasswordRequest request) {
        studentService.resetPassword(studentId, request.getNewPassword());
        return ResponseEntity.ok(true);
    }

    @PatchMapping("/{studentId}/status")
    public ResponseEntity<?> setActive(@PathVariable Long studentId, @RequestParam boolean active) {
        return ResponseEntity.ok(studentService.setActive(studentId, active));
    }

    @DeleteMapping("/{studentId}")
    public ResponseEntity<?> deleteStudent(@PathVariable Long studentId) {
        studentService.deleteStudent(studentId);
        return ResponseEntity.ok(true);
    }

    // Class (category) assignment — a school assigns classes to its own students.
    @GetMapping("/{studentId}/classes")
    public ResponseEntity<?> getAssignedClasses(@PathVariable Long studentId) {
        return ResponseEntity.ok(studentService.getAssignedClasses(studentId));
    }

    @PostMapping("/{studentId}/classes/{catId}")
    public ResponseEntity<?> assignClass(@PathVariable Long studentId, @PathVariable Long catId) {
        studentService.assignClass(studentId, catId);
        return ResponseEntity.ok(true);
    }

    @DeleteMapping("/{studentId}/classes/{catId}")
    public ResponseEntity<?> unassignClass(@PathVariable Long studentId, @PathVariable Long catId) {
        studentService.unassignClass(studentId, catId);
        return ResponseEntity.ok(true);
    }
}
