package com.odin.multimedia.dto;
import java.util.List;
import lombok.Data;
@Data public class ProfileStatusReadiness {
    private String state;
    private List<String> reasons;
    private List<String> repairActions;
    private boolean retryable;
    private int readinessVersion;
}
