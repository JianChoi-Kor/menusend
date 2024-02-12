package com.project.menusend.util;

import com.slack.api.Slack;
import com.slack.api.webhook.WebhookResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class WebhookService {

    @Value("${menu.menu-slack-url}")
    private String menuSlackUrl;

    @Value("${menu.error-slack-url}")
    private String errorSlackUrl;

    public boolean sendMenuMessage(String content) {
        Slack slack = Slack.getInstance();
        String payload = "{\"text\": \"" + content + "\"}";
        try {
            WebhookResponse response = slack.send(menuSlackUrl, payload);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    public boolean sendErrorMessage(String content) {
        Slack slack = Slack.getInstance();
        String payload = "{\"text\": \"" + content + "\"}";
        try {
            WebhookResponse response = slack.send(errorSlackUrl, payload);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }
}
