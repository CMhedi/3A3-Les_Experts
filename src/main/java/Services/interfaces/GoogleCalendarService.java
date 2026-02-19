package Services.interfaces;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;

public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "PiDev Calendar";
    private static final GsonFactory JSON_FACTORY =
            GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private static final java.util.List<String> SCOPES =
            Collections.singletonList(
                    "https://www.googleapis.com/auth/calendar"
            );

    // ================= AUTH =================
    private static Credential getCredentials(
            final NetHttpTransport HTTP_TRANSPORT) throws Exception {

        InputStream in = GoogleCalendarService.class
                .getResourceAsStream("/credentials.json");

        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(
                        JSON_FACTORY,
                        new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT,
                        JSON_FACTORY,
                        clientSecrets,
                        SCOPES)
                        .setDataStoreFactory(
                                new FileDataStoreFactory(
                                        new java.io.File(TOKENS_DIRECTORY_PATH)))
                        .setAccessType("offline")
                        .build();

        LocalServerReceiver receiver =
                new LocalServerReceiver.Builder()
                        .setPort(8888)
                        .build();

        return new AuthorizationCodeInstalledApp(flow, receiver)
                .authorize("user");
    }

    private static Calendar getService() throws Exception {
        final NetHttpTransport HTTP_TRANSPORT =
                GoogleNetHttpTransport.newTrustedTransport();

        return new Calendar.Builder(
                HTTP_TRANSPORT,
                JSON_FACTORY,
                getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    // ================= ADD EVENT =================
    public static String addEvent(
            String summary,
            String description,
            LocalDateTime start,
            LocalDateTime end) throws Exception {

        Calendar service = getService();

        Event event = new Event()
                .setSummary(summary)
                .setDescription(description);

        EventDateTime startDateTime = new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(
                        Date.from(start.atZone(
                                ZoneId.systemDefault()).toInstant())))
                .setTimeZone(
                        ZoneId.systemDefault().toString());

        EventDateTime endDateTime = new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(
                        Date.from(end.atZone(
                                ZoneId.systemDefault()).toInstant())))
                .setTimeZone(
                        ZoneId.systemDefault().toString());

        event.setStart(startDateTime);
        event.setEnd(endDateTime);

        // 🔔 Notification 30 minutes
        Event.Reminders reminders =
                new Event.Reminders()
                        .setUseDefault(false)
                        .setOverrides(Collections.singletonList(
                                new EventReminder()
                                        .setMethod("popup")
                                        .setMinutes(30)
                        ));

        event.setReminders(reminders);

        // ✅ Google génère l'ID
        Event createdEvent =
                service.events()
                        .insert("primary", event)
                        .execute();

        System.out.println("Event ajouté : " + createdEvent.getId());

        return createdEvent.getId(); // 🔥 IMPORTANT
    }

    // ================= DELETE EVENT =================
    public static void deleteEvent(String eventId) {

        try {

            Calendar service = getService();

            service.events()
                    .delete("primary", eventId)
                    .execute();

            System.out.println("Event supprimé.");

        } catch (Exception e) {
            System.out.println("Event non trouvé.");
        }
    }
}
