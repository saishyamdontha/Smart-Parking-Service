package com.parking.activity.dto;

public record ActivityEventRequest(String eventType, Long sessionId, Long spaceId, String userId) {
}
