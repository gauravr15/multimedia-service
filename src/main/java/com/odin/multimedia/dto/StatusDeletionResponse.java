package com.odin.multimedia.dto;
import com.odin.multimedia.enums.StatusLifecycleState;
import lombok.AllArgsConstructor;
import lombok.Getter;
@Getter @AllArgsConstructor
public class StatusDeletionResponse {
    private final String statusId;
    private final StatusLifecycleState state;
}
