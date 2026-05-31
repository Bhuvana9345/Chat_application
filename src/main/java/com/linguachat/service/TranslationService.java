package com.linguachat.service;

import com.linguachat.entity.Language;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TranslationService {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.translation.url}")
    private String translationUrl;

    public String translate(String text, Language source, Language target) {
        if (source == target || text == null || text.isBlank()) {
            return text;
        }
        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = translationUrl
                    .replace("{text}", encoded)
                    .replace("{source}", source.getCode())
                    .replace("{target}", target.getCode());
            Map<?, ?> response = restTemplate.getForObject(URI.create(url), Map.class);
            if (response == null || response.get("responseData") == null) {
                return text;
            }
            Map<?, ?> responseData = (Map<?, ?>) response.get("responseData");
            Object translated = responseData.get("translatedText");
            return translated == null || translated.toString().isBlank() ? text : translated.toString();
        } catch (Exception ex) {
            return text;
        }
    }
}
