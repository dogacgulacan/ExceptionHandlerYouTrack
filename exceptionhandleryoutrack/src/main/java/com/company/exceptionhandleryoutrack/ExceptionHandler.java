package com.company.projectsystem.exceptions;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.exception.AbstractUiExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.MediaType;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.atomic.AtomicBoolean;

@Component("project_ProjectExceptionHandler")
public class ExceptionHandler extends AbstractUiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandler.class);

    @Value("${youtrack.url}")
    String youtrackUrl = "https://dogacgulacan.youtrack.cloud/api/issues";
    @Value("${youtrack.apiToken}")
    String apiToken = "perm:YWRtaW4=.NDgtMQ==.tEok8alVuCn4cm5zqjqfmQlZfzC06M";

    @Autowired
    private ApplicationContext applicationContext;

    // Kullanıcı onayını depolamak için instance değişkeni

    public ExceptionHandler() {
        super(CustomException.class.getName());
    }

    @Override
    protected void doHandle(String className, String message, @Nullable Throwable throwable) {
        Notifications notifications = applicationContext.getBean(Notifications.class);

        notifications.create(message)
                .withType(Notifications.Type.ERROR)
                .withPosition(Notification.Position.MIDDLE)
                .withThemeVariant(NotificationVariant.LUMO_ERROR)
                .withClassName("notification-align-center")
                .show();

        // Kullanıcı onayını al
        getUserConsent(className, message, throwable);

        // Eğer kullanıcı onayı verdiyse, YouTrack maddesi oluştur

    }

    // Kullanıcı onayını almak için metot
    private void getUserConsent(String className, String message, @Nullable Throwable throwable) {
        Dialog consentDialog = new Dialog();
        AtomicBoolean  userConsent = new AtomicBoolean(false);
        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.add(new Text("Bir hata oluştu. Bu hatayı destek ekibimize bildirmek ister misiniz?"));


        Button yesButton = new Button("Evet", event -> {
            userConsent.set(true);  // Kullanıcı onayı true olarak ayarlanır
            consentDialog.close();
            createYouTrackIssue(className, message, throwable);
        });

        Button noButton = new Button("Hayır", event -> {
            userConsent.set(false);  // Kullanıcı onayı false olarak ayarlanır
            consentDialog.close();
        });

        dialogLayout.add(new HorizontalLayout(yesButton, noButton));
        consentDialog.add(dialogLayout);

        consentDialog.open();
    }

    private void createYouTrackIssue(String className, String message, @Nullable Throwable throwable) {
        RestTemplate restTemplate = new RestTemplate();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiToken);

            String issueTitle = "Exception in " + className;
            String issueDescription = message + (throwable != null ? ("\n" + throwable.toString()) : "");

            String safeIssueTitle = issueTitle.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
            String safeIssueDescription = issueDescription.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");

            String requestBody = "{"
                    + "\"summary\": \"" + safeIssueTitle + "\","
                    + "\"description\": \"" + safeIssueDescription + "\","
                    + "\"project\": {\"id\": \"0-3\"}"
                    + "}";

            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            restTemplate.postForEntity(youtrackUrl, request, String.class);
        } catch (Exception e) {
            log.error("YouTrack API çağrısında bir hata oluştu: ", e);
        }
    }
}
