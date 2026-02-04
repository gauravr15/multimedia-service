package com.odin.multimedia.dto;

import java.util.Map;

import com.odin.multimedia.enums.NotificationChannel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO {
    private Long customerId;
    private Long notificationId;
    private NotificationChannel channel;
    private Map<String, Object> map;
    private String mobile;
    private String email;
}
