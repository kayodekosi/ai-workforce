package com.knatware.aiworkforce.voice;

import com.knatware.aiworkforce.model.Staff;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * A flexible HTTP-based TTS provider that works with ANY service exposing an
 * HTTP endpoint that accepts text and returns an audio URL (or audio bytes via
 * a URL). Rather than hardcoding ElevenLabs/Azure/Google individually, this one
 * generic provider is configured per deployment:
 *
 *   app.voice.http.endpoint  = https://api.your-tts.com/synthesize
 *   app.voice.http.api-key   = <key>           (sent as Authorization: Bearer)
 *   app.voice.http.url-field = audio_url        (JSON field holding the result URL)
 *
 * This means ElevenLabs, Azure, Google, Amazon Polly, OpenAI TTS, or a
 * self-hosted engine can all be used by pointing the endpoint at them (or at a
 * thin adapter), with no code change. For providers with bespoke auth/response
 * shapes, implement a dedicated {@link VoiceProvider} instead — the registry
 * supports both.
 */
@Component
public class HttpVoiceProvider implements VoiceProvider {

    private final RestClient http = RestClient.create();
    private final String endpoint;
    private final String apiKey;
    private final String urlField;

    public HttpVoiceProvider(
            @Value("${app.voice.http.endpoint:}") String endpoint,
            @Value("${app.voice.http.api-key:}") String apiKey,
            @Value("${app.voice.http.url-field:audio_url}") String urlField) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.urlField = urlField;
    }

    /** Matches any configured external provider name; selected by VoiceService. */
    @Override
    public String id() { return "http"; }

    @Override
    public VoiceClip synthesize(Staff staff, String text) {
        if (endpoint == null || endpoint.isBlank()) {
            // Not configured — signal caller to fall back to simulation.
            return null;
        }
        try {
            var req = http.post().uri(endpoint)
                    .header("Content-Type", "application/json");
            if (apiKey != null && !apiKey.isBlank()) {
                req = req.header("Authorization", "Bearer " + apiKey);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = req.body(Map.of(
                    "text", text == null ? "" : text,
                    "voice", staff.getVoiceId() == null ? "" : staff.getVoiceId(),
                    "provider", staff.getVoiceProvider() == null ? "" : staff.getVoiceProvider()
            )).retrieve().body(Map.class);

            Object url = resp == null ? null : resp.get(urlField);
            if (url == null) return null;
            return new VoiceClip(url.toString(), staff.getVoiceProvider(), false);
        } catch (Exception e) {
            // On any failure, return null so the caller can fall back gracefully.
            return null;
        }
    }
}
