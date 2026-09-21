package com.parking.session.client;

import com.parking.session.exception.ConflictException;
import com.parking.session.exception.NotFoundException;
import com.parking.session.exception.UpstreamServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ParkingSpaceClient {

    private final RestClient restClient;

    public ParkingSpaceClient(@Value("${parking.service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void occupy(Long spaceId) {
        call(spaceId, "occupy");
    }

    public void release(Long spaceId) {
        call(spaceId, "release");
    }

    private void call(Long spaceId, String action) {
        try {
            restClient.post()
                    .uri("/api/spaces/{id}/{action}", spaceId, action)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            if (status.value() == 404) {
                throw new NotFoundException("Parking space " + spaceId + " not found");
            }
            if (status.value() == 409) {
                throw new ConflictException("Parking space " + spaceId + " cannot be " + (action.equals("occupy") ? "occupied" : "released") + ": " + e.getResponseBodyAsString());
            }
            throw new UpstreamServiceException("parking-service call failed: " + e.getMessage());
        } catch (Exception e) {
            throw new UpstreamServiceException("parking-service unreachable: " + e.getMessage());
        }
    }
}
