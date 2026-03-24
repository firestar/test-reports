package com.auditreports.controller;

import com.auditreports.model.ReportState;
import com.auditreports.model.StateTransition;
import com.auditreports.service.StateConfigService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StateConfigController {

    private final StateConfigService stateConfigService;

    public StateConfigController(StateConfigService stateConfigService) {
        this.stateConfigService = stateConfigService;
    }

    @GetMapping("/states")
    public List<ReportState> getAllStates() {
        return stateConfigService.getAllStates();
    }

    @GetMapping("/states/{id}")
    public ReportState getState(@PathVariable Long id) {
        return stateConfigService.getState(id);
    }

    @PostMapping("/states")
    public ResponseEntity<ReportState> createState(@Valid @RequestBody ReportState state) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stateConfigService.createState(state));
    }

    @PutMapping("/states/{id}")
    public ReportState updateState(@PathVariable Long id, @Valid @RequestBody ReportState state) {
        return stateConfigService.updateState(id, state);
    }

    @DeleteMapping("/states/{id}")
    public ResponseEntity<Void> deleteState(@PathVariable Long id) {
        stateConfigService.deleteState(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/transitions")
    public List<StateTransition> getAllTransitions() {
        return stateConfigService.getAllTransitions();
    }

    @PostMapping("/transitions")
    public ResponseEntity<StateTransition> createTransition(@RequestBody Map<String, Long> body) {
        Long fromStateId = body.get("fromStateId");
        Long toStateId = body.get("toStateId");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(stateConfigService.createTransition(fromStateId, toStateId));
    }

    @DeleteMapping("/transitions/{id}")
    public ResponseEntity<Void> deleteTransition(@PathVariable Long id) {
        stateConfigService.deleteTransition(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }
}
