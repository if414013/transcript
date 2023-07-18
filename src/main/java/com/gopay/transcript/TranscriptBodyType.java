package com.gopay.transcript;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TranscriptBodyType {
    REQUEST("Request"),
    RESPONSE("Response"),
    EVENT_PUBLISH("EventPublish"),
    EVENT_CONSUME("EventConsume");

    private final String type;

    TranscriptBodyType(String type) {
        this.type = type;
    }

    @JsonValue
    public String jsonValue() {
        return this.type;
    }
}
