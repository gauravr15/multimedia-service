package com.odin.multimedia.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.odin.multimedia.dto.AccessRights;
import com.odin.multimedia.repository.AccessRightsRepository;


@Configuration
public class CachingConfig {
	
//	@Autowired
//	private AccessRightsRepository accessRightsRepo;
//
//	private static final Map<String, Map<String, Integer>> CACHED_RIGHTS = new HashMap<>();
//	
//	@PostConstruct
//	public void cacheAccessRights() {
//		List<AccessRights> accessRightsList = accessRightsRepo.findAllAccessRights();
//
//		Map<String, Map<String, Integer>> cachedRights = accessRightsList.stream()
//				.collect(Collectors.groupingBy(AccessRights::getUserType,
//						Collectors.toMap(AccessRights::getModuleName, AccessRights::getIsAllowed)));
//
//		CACHED_RIGHTS.putAll(cachedRights);
//	}
//	
//	public static Map<String, Map<String, Integer>> getCachedRights() {
//        return CACHED_RIGHTS;
//    }

}
