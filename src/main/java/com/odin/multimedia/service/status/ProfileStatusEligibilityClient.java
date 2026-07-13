package com.odin.multimedia.service.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odin.multimedia.constants.ResponseCodes;
import com.odin.multimedia.dto.ProfileEligibilityBatchRequest;
import com.odin.multimedia.dto.ProfileEligibilityBatchResponse;
import com.odin.multimedia.dto.ProfileEligibilityResult;
import com.odin.multimedia.dto.ResponseDTO;
import com.odin.multimedia.dto.ProfileStatusReadiness;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ProfileStatusEligibilityClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final String endpoint;
	private final String readinessEndpoint;

    public ProfileStatusEligibilityClient(@Qualifier("statusProfileRestTemplate") RestTemplate restTemplate,
            ObjectMapper mapper, @Value("${profile.service.url}") String baseUrl,
            @Value("${status.profile.eligibility-path:status/eligibility/batch}") String path) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;
        this.endpoint = baseUrl + path;
		this.readinessEndpoint = baseUrl + "status/readiness";
    }

	public ProfileStatusReadiness readiness(String userId) {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.set("customerId", userId);
			ResponseEntity<ProfileStatusReadiness> response = restTemplate.exchange(readinessEndpoint,
					HttpMethod.GET, new HttpEntity<>(headers), ProfileStatusReadiness.class);
			if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null
					|| response.getBody().getState() == null) throw invalid("Profile readiness is malformed");
			return response.getBody();
		} catch (StatusAccessException exception) { throw exception; }
		catch (Exception exception) { throw new StatusAccessException(StatusAccessException.Category.DEPENDENCY_FAILURE,
				"Profile readiness is unavailable", exception); }
	}

	public void requireReady(String userId) {
		String state = readiness(userId).getState();
		if ("READY".equals(state)) return;
		if ("REPAIR_REQUIRED".equals(state)) throw new StatusAccessException(
				StatusAccessException.Category.REPAIR_REQUIRED, "Status profile repair required");
		throw new StatusAccessException(StatusAccessException.Category.DEPENDENCY_FAILURE,
				"Status profile readiness is indeterminate");
	}

    public Map<String, String> evaluate(String viewerId, Set<String> uploaderIds) {
        if (uploaderIds.isEmpty()) return new HashMap<>();
        long started = System.nanoTime();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("customerId", viewerId);
            HttpEntity<ProfileEligibilityBatchRequest> entity = new HttpEntity<>(
                    new ProfileEligibilityBatchRequest(new ArrayList<>(uploaderIds)), headers);
            ResponseEntity<ResponseDTO> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, ResponseDTO.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null
                    || !ResponseCodes.SUCCESS_CODE.equals(response.getBody().getStatusCode())) {
                throw invalid("Profile eligibility returned non-success");
            }
            ProfileEligibilityBatchResponse payload = mapper.convertValue(
                    response.getBody().getData(), ProfileEligibilityBatchResponse.class);
            if (payload == null || !viewerId.equals(payload.getViewerId()) || payload.getResults() == null) {
                throw invalid("Profile eligibility response is malformed");
            }
            Map<String, String> decisions = new HashMap<>();
            for (ProfileEligibilityResult result : payload.getResults()) {
                if (result == null || !uploaderIds.contains(result.getUploaderId())
                        || !("ALLOW".equals(result.getDecision()) || "DENY".equals(result.getDecision())
                        || "INDETERMINATE".equals(result.getDecision()))
                        || decisions.put(result.getUploaderId(), result.getDecision()) != null) {
                    throw invalid("Profile eligibility response contains invalid or duplicate results");
                }
            }
            if (!decisions.keySet().equals(new LinkedHashSet<>(uploaderIds))) {
                throw invalid("Profile eligibility response omitted an uploader");
            }
            log.info("Profile eligibility completed. viewerId={}, uploaderCount={}, latencyMs={}",
                    viewerId, uploaderIds.size(), (System.nanoTime() - started) / 1_000_000L);
            return decisions;
        } catch (StatusAccessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new StatusAccessException(StatusAccessException.Category.DEPENDENCY_FAILURE,
                    "Profile eligibility is unavailable", exception);
        }
    }

    private StatusAccessException invalid(String message) {
        return new StatusAccessException(StatusAccessException.Category.DEPENDENCY_FAILURE, message);
    }
}
