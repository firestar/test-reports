package com.auditreports.controller;

import com.auditreports.model.Editor;
import com.auditreports.service.EditorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/editors")
public class EditorController {

    private final EditorService editorService;

    public EditorController(EditorService editorService) {
        this.editorService = editorService;
    }

    @GetMapping
    public List<Editor> getAllEditors() {
        return editorService.getAllEditors();
    }

    @GetMapping("/{id}")
    public Map<String, Object> getEditor(@PathVariable Long id) {
        return editorService.getEditorWorkload(id);
    }

    @PostMapping
    public ResponseEntity<Editor> createEditor(@Valid @RequestBody Editor editor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(editorService.createEditor(editor));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
