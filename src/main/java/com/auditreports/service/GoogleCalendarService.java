package com.auditreports.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class GoogleCalendarService {

    @Value("${google.calendar.credentials-path:}")
    private String credentialsPath;

    @Value("${google.calendar.application-name:Audit Reports}")
    private String applicationName;

    private Calendar calendarService;
    private boolean configured = false;

    @PostConstruct
    public void init() {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            configured = false;
            return;
        }
        try {
            GoogleCredentials credentials = ServiceAccountCredentials
                    .fromStream(new FileInputStream(credentialsPath))
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/calendar.readonly"));

            calendarService = new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName(applicationName)
                    .build();
            configured = true;
        } catch (IOException | GeneralSecurityException e) {
            configured = false;
        }
    }

    public boolean isConfigured() {
        return configured;
    }

    public Map<String, Object> getAvailability(String calendarId, int days) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("calendarId", calendarId);

        if (!configured) {
            result.put("configured", false);
            result.put("message", "Google Calendar is not configured. Set google.calendar.credentials-path in application.properties.");
            return result;
        }

        try {
            java.time.LocalDateTime now = LocalDateTime.now();
            java.time.LocalDateTime end = now.plusDays(days);

            DateTime timeMin = new DateTime(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()));
            DateTime timeMax = new DateTime(Date.from(end.atZone(ZoneId.systemDefault()).toInstant()));

            FreeBusyRequest request = new FreeBusyRequest()
                    .setTimeMin(timeMin)
                    .setTimeMax(timeMax)
                    .setItems(Collections.singletonList(new FreeBusyRequestItem().setId(calendarId)));

            FreeBusyResponse response = calendarService.freebusy().query(request).execute();
            FreeBusyCalendar calendar = response.getCalendars().get(calendarId);

            List<Map<String, String>> busySlots = new ArrayList<>();
            if (calendar != null && calendar.getBusy() != null) {
                for (TimePeriod period : calendar.getBusy()) {
                    Map<String, String> slot = new LinkedHashMap<>();
                    slot.put("start", period.getStart().toStringRfc3339());
                    slot.put("end", period.getEnd().toStringRfc3339());
                    busySlots.add(slot);
                }
            }

            result.put("configured", true);
            result.put("busySlots", busySlots);
            result.put("totalBusySlots", busySlots.size());
            result.put("queryStart", now.toString());
            result.put("queryEnd", end.toString());
        } catch (IOException e) {
            result.put("configured", true);
            result.put("error", "Failed to query calendar: " + e.getMessage());
        }

        return result;
    }

    public Map<String, Map<String, Object>> getBatchAvailability(List<String> calendarIds, int days) {
        Map<String, Map<String, Object>> results = new LinkedHashMap<>();
        for (String calendarId : calendarIds) {
            results.put(calendarId, getAvailability(calendarId, days));
        }
        return results;
    }
}
