package com.knatware.aiworkforce.voice;

import com.knatware.aiworkforce.model.Staff;

/**
 * Abstraction over a text-to-speech provider. Implementations call out to a
 * specific TTS service (ElevenLabs, Azure, Google, Amazon Polly, OpenAI, a
 * self-hosted engine, etc.) — the platform stays provider-agnostic.
 *
 * To add a provider: implement this interface, give it a unique {@link #id()},
 * and register it in {@link VoiceService}. The staff member's configured
 * {@code voiceProvider} selects which one is used.
 */
public interface VoiceProvider {

    /** Unique lowercase id, e.g. "elevenlabs", "azure", "google", "polly", "openai". */
    String id();

    /**
     * Synthesize speech for the given text using the staff member's voice config.
     *
     * @return a {@link VoiceClip} with a URL/locator to the produced audio.
     */
    VoiceClip synthesize(Staff staff, String text);

    /** Result of synthesis: a URL to the audio plus the provider that made it. */
    record VoiceClip(String url, String provider, boolean simulated) { }
}
