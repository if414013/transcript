package com.gopay.transcript;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

@JsonAutoDetect(
        fieldVisibility = Visibility.ANY
)
public class Transcript {
    private static final Logger logger = LoggerFactory.getLogger(Transcript.class);
    private static final Logger transcriptLogger = LoggerFactory.getLogger("transcriptLogger");
    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    private final String transactionId;
    private final TranscriptType transcriptType;
    private final String body;
    private final TranscriptBodyType bodyType;
    private final long transcriptTime ;

    public Transcript(final TranscriptType transcriptType, final String body, final TranscriptBodyType bodyType, final String transactionId) {
        this.transcriptType = transcriptType;
        this.transactionId = transactionId;
        this.bodyType = bodyType;
        this.body = body;
        this.transcriptTime = Instant.now().toEpochMilli();
    }

    public static void logRequest(TranscriptType transcriptType, String transactionId, String request) {
        Transcript transcript = new Transcript(transcriptType, request, TranscriptBodyType.REQUEST, transactionId);
        transcript.log();
    }


    public static void logResponse(TranscriptType transcriptType, String transactionId, String response) {
        Transcript transcript = new Transcript(transcriptType, response, TranscriptBodyType.RESPONSE, transactionId);
        transcript.log();
    }

    public static void logEventPublished(TranscriptType transcriptType, String transactionId, String response) {
        Transcript transcript = new Transcript(transcriptType, response, TranscriptBodyType.EVENT_PUBLISH, transactionId);
        transcript.log();
    }

    public static void logEventConsumed(TranscriptType transcriptType, String transactionId, String response) {
        Transcript transcript = new Transcript(transcriptType, response, TranscriptBodyType.EVENT_CONSUME, transactionId);
        transcript.log();
    }

    protected String toJson() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            logger.warn("unexpected exception when converting transcript data to json", e);
            return "";
        }
    }

    public void log() {
        try {
            transcriptLogger.info(this.toJson());
        } catch (Exception e) {
            logger.warn("unexpected exception when logging to transcript", e);
        }

    }
}
