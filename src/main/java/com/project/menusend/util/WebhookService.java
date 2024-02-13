package com.project.menusend.util;

import com.slack.api.Slack;
import com.slack.api.webhook.WebhookResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    @Value("${menu.menu-slack-url}")
    private String menuSlackUrl;

    @Value("${menu.error-slack-url}")
    private String errorSlackUrl;

    public void sendMenuMessage(String content) {
        Slack slack = Slack.getInstance();
        String payload = "{\"text\": \"" + content + "\"}";
        try {
            WebhookResponse response = slack.send(menuSlackUrl, payload);
            log.info("webhookResponse: " + response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendErrorMessage(String content) {
        Slack slack = Slack.getInstance();
        String payload = "{\"text\": \"" + content + "\"}";
        try {
            WebhookResponse response = slack.send(errorSlackUrl, payload);
            log.info("webhookResponse: " + response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
