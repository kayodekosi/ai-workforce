package com.knatware.aiworkforce.voice;

import com.knatware.aiworkforce.model.Staff;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * "Generate and attach a voice clip URL" mode — needs no external account.
 *
 * It returns a deterministic placeholder clip URL derived from the text and the
 * staff member's voice id, so the platform's voice flow works end-to-end in
 * demos. Swap a real provider in (set the staff member's voiceProvider) when you
 * have credentials.
 */
@Component
public class SimulatedVoiceProvider implements VoiceProvider {

    @Override
    public String id() { return "simulated"; }

    @Override
    public VoiceClip synthesize(Staff staff, String text) {
        String voice = staff.getVoiceId() == null ? "default" : staff.getVoiceId();
        String snippet = text == null ? "" : text.substring(0, Math.min(text.length(), 80));
        String url = "/media/voice/sim/"
                + URLEncoder.encode(voice, StandardCharsets.UTF_8) + "/"
                + Integer.toHexString(snippet.hashCode()) + ".mp3";
        return new VoiceClip(url, "simulated", true);
    }
}
