package com.auditreports.controller;

import com.auditreports.model.Editor;
import com.auditreports.service.EditorService;
import com.auditreports.service.GoogleCalendarService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final GoogleCalendarService calendarService;
    private final EditorService editorService;

    public CalendarController(GoogleCalendarService calendarService, EditorService editorService) {
        this.calendarService = calendarService;
        this.editorService = editorService;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("configured", calendarService.isConfigured());
        if (!calendarService.isConfigured()) {
            status.put("message", "Google Calendar is not configured. Set google.calendar.credentials-path in application.properties.");
        }
        return status;
    }

    @GetMapping("/availability")
    public Map<String, Map<String, Object>> getAvailability(
            @RequestParam List<Long> editorIds,
            @RequestParam(defaultValue = "7") int days) {

        Map<String, Map<String, Object>> results = new LinkedHashMap<>();
        for (Long editorId : editorIds) {
            Editor editor = editorService.getEditor(editorId);
            String calendarId = editor.getCalendarId() != null ? editor.getCalendarId() : editor.getEmail();
            Map<String, Object> availability = calendarService.getAvailability(calendarId, days);
            availability.put("editorName", editor.getName());
            results.put(editor.getName(), availability);
        }
        return results;
    }
}
