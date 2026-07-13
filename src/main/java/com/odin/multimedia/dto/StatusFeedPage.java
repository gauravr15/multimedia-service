package com.odin.multimedia.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StatusFeedPage {
    private final List<StatusFeedItem> statuses;
    private final String nextCursor;
}
