package com.knatware.aiworkforce.voice;

import com.knatware.aiworkforce.model.Staff;
import org.springframework.stereotype.Service;

/**
 * Entry point for turning an AI staff member's reply into speech. Picks a
 * provider based on the staff member's configuration and falls back to the
 * simulated provider when no real provider is configured or a call fails — so
 * voice always "works" in a demo, and upgrades cleanly to real TTS in production.
 *
 * Flexible by design: any number of {@link VoiceProvider}s can be added. The
 * {@link HttpVoiceProvider} alone covers most HTTP TTS services (ElevenLabs,
 * Azure, Google, Polly, OpenAI, self-hosted) via configuration.
 */
@Service
public class VoiceService {

    private final HttpVoiceProvider httpProvider;
    private final SimulatedVoiceProvider simulated;

    public VoiceService(HttpVoiceProvider httpProvider, SimulatedVoiceProvider simulated) {
        this.httpProvider = httpProvider;
        this.simulated = simulated;
    }

    /**
     * Produce a voice clip for the given staff member and text. Returns null if
     * the staff member doesn't have voice enabled.
     */
    public VoiceProvider.VoiceClip speak(Staff staff, String text) {
        if (staff == null || !staff.isVoiceEnabled()) return null;

        // If a real provider is configured (staff has a voiceProvider set and the
        // HTTP provider is configured), try it first.
        String configured = staff.getVoiceProvider();
        if (configured != null && !configured.isBlank() && !configured.equalsIgnoreCase("simulated")) {
            VoiceProvider.VoiceClip clip = httpProvider.synthesize(staff, text);
            if (clip != null) return clip;
            // fall through to simulation if the real call wasn't configured/failed
        }
        return simulated.synthesize(staff, text);
    }
}
