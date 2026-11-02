package com.parking.session.client;

import com.parking.session.diagnostics.Http;
import com.parking.session.exception.ConflictException;
import com.parking.session.exception.NotFoundException;
import com.parking.session.exception.UpstreamServiceException;
import com.parking.session.exception.UpstreamServiceException.Kind;
import com.parking.session.exception.UpstreamServiceException.Target;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ParkingSpaceClient {

    private final RestClient restClient;

    public ParkingSpaceClient(@Value("${parking.service.base-url}") String baseUrl,
                              @Value("${parking.service.timeout-ms:2000}") int timeoutMs) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(Http.factory(timeoutMs))
                .requestInterceptor(Http.correlation())
                .build();
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
            int status = e.getStatusCode().value();
            if (status == 404) {
                throw new NotFoundException("Parking space " + spaceId + " not found");
            }
            if (status == 409) {
                throw new ConflictException("Parking space " + spaceId + " cannot be "
                        + (action.equals("occupy") ? "occupied" : "released") + ": " + e.getResponseBodyAsString());
            }
            Kind kind = status >= 500 ? Kind.HTTP_5XX : Kind.HTTP_4XX;
            throw new UpstreamServiceException(Target.PARKING, kind, status,
                    "parking-service returned " + status + ": " + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            Kind kind = Http.isTimeout(e) ? Kind.TIMEOUT : Kind.UNREACHABLE;
            throw new UpstreamServiceException(Target.PARKING, kind, null,
                    "parking-service " + kind.name().toLowerCase() + ": " + e.getMessage());
        }
    }
}
