package com.odin.multimedia.dto;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Getter @NoArgsConstructor
public class ProfileEligibilityBatchResponse { private String viewerId; private List<ProfileEligibilityResult> results; }
