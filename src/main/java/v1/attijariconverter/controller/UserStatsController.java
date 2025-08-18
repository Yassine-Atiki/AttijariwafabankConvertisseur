package v1.attijariconverter.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import v1.attijariconverter.service.ConversionService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserStatsController {

    @Autowired
    private ConversionService conversionService;

    @GetMapping("/list")
    public ResponseEntity<List<String>> getAllUsers() {
        List<String> users = conversionService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{username}/stats")
    public ResponseEntity<Map<String, Object>> getUserStatistics(@PathVariable String username) {
        Map<String, Object> stats = conversionService.getUserStatistics(username);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{username}/stats-by-date")
    public ResponseEntity<Map<String, Object>> getUserStatsByDate(
            @PathVariable String username,
            @RequestParam String date) {
        Map<String, Object> stats = conversionService.getUserStatsByDate(username, date);
        return ResponseEntity.ok(stats);
    }
}
