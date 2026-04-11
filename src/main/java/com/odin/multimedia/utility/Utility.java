package com.odin.multimedia.utility;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odin.multimedia.dto.ResponseDTO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Utility {
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@Autowired
	private RestTemplate restTemplate;

	public <D, E> E getAnInstance(D dto, Class<E> entityClass) {
	    try {
	        if (dto == null) {
	            log.warn("DTO is null, cannot map to {}", entityClass.getSimpleName());
	            return null;
	        }

	        // Handle if dto is a collection or an array
	        if (dto instanceof Collection || dto.getClass().isArray()) {
	            // Convert to a list of the specified entity type
	            JavaType javaType = objectMapper.getTypeFactory().constructCollectionType(List.class, entityClass);
	            List<E> list = objectMapper.convertValue(dto, javaType);

	            // If only one element exists, return it as a single object
	            if (list.size() == 1) {
	                return list.get(0);
	            }

	            // Otherwise, return the entire list (cast to E)
	            @SuppressWarnings("unchecked")
	            E castedList = (E) list;
	            return castedList;
	        }

	        // Default case: Map dto to the specified entity type
	        return objectMapper.convertValue(dto, entityClass);
	    } catch (Exception e) {
	        log.error("Error occurred while converting to class {}: {}", entityClass.getSimpleName(), ExceptionUtils.getStackTrace(e));
	        return null;
	    }
	}



	public <T> List<T> getInstances(ResponseDTO response, Class<T> clazz) {
		if (response == null) {
			log.warn("[UTIL-INSTANCES] response object is null for class={}", clazz.getSimpleName());
			return Collections.emptyList();
		}
		log.info("[UTIL-INSTANCES] statusCode={} hasData={} dataType={} for class={}",
				response.getStatusCode(),
				response.getData() != null,
				response.getData() != null ? response.getData().getClass().getSimpleName() : "null",
				clazz.getSimpleName());
		if (ObjectUtils.isEmpty(response.getData())) {
			log.warn("[UTIL-INSTANCES] data is null/empty for statusCode={} class={}",
					response.getStatusCode(), clazz.getSimpleName());
			return Collections.emptyList();
		}
	    try {
	    	ObjectMapper objectMapper = new ObjectMapper();
	        List<?> rawData = (List<?>) response.getData();
	        log.info("[UTIL-INSTANCES] rawData size={} elementType={} for class={}",
	        		rawData.size(),
	        		rawData.isEmpty() ? "empty" : rawData.get(0).getClass().getSimpleName(),
	        		clazz.getSimpleName());
	        if (rawData.size() == 0) {
	        	log.warn("[UTIL-INSTANCES] rawData list is empty for class={}", clazz.getSimpleName());
	        	return Collections.emptyList();
	        }
	        // Map each LinkedHashMap to the desired type
	        List<T> result = rawData.stream()
	            .map(item -> objectMapper.convertValue(item, clazz))
	            .collect(Collectors.toList());
	        log.info("[UTIL-INSTANCES] Converted {} instances of {}", result.size(), clazz.getSimpleName());
	        return result;
	    } catch (Exception e) {
	        log.error("[UTIL-INSTANCES] Error converting to {}: {}", clazz.getSimpleName(), ExceptionUtils.getStackTrace(e));
	        return Collections.emptyList(); // Return empty list in case of an error
	    }
	}

    public <T, R> ResponseDTO makeRestCall(String url, T requestBody, HttpMethod httpMethod, Class<R> responseType) {
        try {
        	HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            // Add correlation ID to the headers
            String correlationId = CorrelationIdUtil.getCorrelationId();
            if (correlationId == null) {
                // Generate a new correlation ID if not present
                correlationId = CorrelationIdUtil.generateCorrelationId();
                CorrelationIdUtil.setCorrelationId(correlationId); // Optionally set it to MDC
            }
            headers.set("X-Correlation-ID", correlationId);
            headers.set("Content-Type", "application/json");
            HttpEntity<T> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<ResponseDTO> response = restTemplate.exchange(url, httpMethod, entity, ResponseDTO.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while making REST call", e);
        }
    }
    
    public <D, E> E dtoToEntity(D dto, Class<E> entityClass) {
        return objectMapper.convertValue(dto, entityClass);
    }
    
    public String[] getNullPropertyNames(Object source) {
        final BeanWrapperImpl wrapper = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = wrapper.getPropertyDescriptors();
        java.util.List<String> nullPropertyNames = new java.util.ArrayList<>();
        for (java.beans.PropertyDescriptor pd : pds) {
            if (wrapper.getPropertyValue(pd.getName()) == null) {
                nullPropertyNames.add(pd.getName());
            }
        }
        return nullPropertyNames.toArray(new String[0]);
    }

}
