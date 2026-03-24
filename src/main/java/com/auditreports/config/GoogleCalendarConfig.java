package com.auditreports.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleCalendarConfig {
    // Google Calendar configuration is handled via application.properties
    // and initialized in GoogleCalendarService @PostConstruct.
    //
    // To enable Google Calendar integration:
    // 1. Create a Google Cloud project and enable Calendar API
    // 2. Create a service account and download the JSON key file
    // 3. Set google.calendar.credentials-path in application.properties
    // 4. Share editor calendars with the service account email
}
