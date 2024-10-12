package com.flywire.exercise.controller;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flywire.exercise.model.Employee;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

  private static final String DATA_FILE_PATH = "src/main/resources/json/data.json";
  private final ObjectMapper objectMapper = new ObjectMapper();

  private List<Employee> readEmployeesFromFile() throws IOException {
    return objectMapper.readValue(new File(DATA_FILE_PATH), new TypeReference<List<Employee>>() {
    });
  }

  @GetMapping("/active")
  public ResponseEntity<List<Employee>> getActiveEmployees() throws IOException {
    List<Employee> employees = readEmployeesFromFile();
    List<Employee> activeEmployees = employees.stream()
        .filter(Employee::isActive)
        .sorted(Comparator.comparing(e -> e.getName().split(" ")[1])) // Sort by last name
        .collect(Collectors.toList());
    return ResponseEntity.ok(activeEmployees);
  }

  @GetMapping("/{id}")
  public ResponseEntity<Map<String, Object>> getEmployeeById(@PathVariable int id) throws IOException {
    List<Employee> employees = readEmployeesFromFile();
    Employee employee = employees.stream().filter(e -> e.getId() == id).findFirst().orElse(null);

    if (employee == null) {
      return ResponseEntity.notFound().build();
    }

    // Get direct reports
    List<Employee> directReports = employees.stream()
        .filter(e -> employee.getDirectReports().contains(e.getId()))
        .collect(Collectors.toList());

    // Create a response with the employee and direct reports
    Map<String, Object> response = new HashMap<>();
    response.put("employee", employee);
    response.put("directReports", directReports);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/hired")
  public ResponseEntity<List<Employee>> getEmployeesByHireDateRange(@RequestParam String startDate,
      @RequestParam String endDate) {
    try {
      List<Employee> employees = readEmployeesFromFile();

      // Convert date strings to Date objects (assuming the format is MM/dd/yyyy)
      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
      Date start = sdf.parse(startDate);
      Date end = sdf.parse(endDate);

      List<Employee> filteredEmployees = employees.stream()
          .filter(e -> {
            try {
              Date hireDate = sdf.parse(e.getHireDate());
              return hireDate.compareTo(start) >= 0 && hireDate.compareTo(end) <= 0;
            } catch (ParseException ex) {
              return false; // Handle parse error gracefully
            }
          })
          .sorted(Comparator.comparing(Employee::getHireDate).reversed()) // Sort by hire date descending
          .collect(Collectors.toList());

      return ResponseEntity.ok(filteredEmployees);
    } catch (ParseException e) {
      return ResponseEntity.badRequest().body(Collections.emptyList()); // Handle bad request
    } catch (IOException e) {
      return ResponseEntity.status(500).body(Collections.emptyList()); // Handle internal server error
    }
  }

  @PostMapping
  public ResponseEntity<String> createEmployee(@RequestBody Employee newEmployee) throws IOException {
    List<Employee> employees = readEmployeesFromFile();

    // Validate employee ID
    if (employees.stream().anyMatch(e -> e.getId() == newEmployee.getId())) {
      return ResponseEntity.badRequest().body("Employee ID already exists.");
    }

    employees.add(newEmployee);
    objectMapper.writeValue(new File(DATA_FILE_PATH), employees); // Write updated list back to JSON

    return ResponseEntity.status(201).body("Employee created successfully.");
  }

  @PatchMapping("/{id}/deactivate")
  public ResponseEntity<String> deactivateEmployee(@PathVariable int id) throws IOException {
    List<Employee> employees = readEmployeesFromFile();
    Employee employee = employees.stream().filter(e -> e.getId() == id).findFirst().orElse(null);

    if (employee == null) {
      return ResponseEntity.notFound().build();
    }

    employee.setActive(false);
    objectMapper.writeValue(new File(DATA_FILE_PATH), employees); // Write updated list back to JSON

    return ResponseEntity.ok("Employee deactivated successfully.");
  }
}
