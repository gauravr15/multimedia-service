package com.odin.multimedia.dto;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccessRights {

    private Long id;

    private String userType;

    private String moduleName;

    private Integer isAllowed;

    private Timestamp creationTimestamp;

    private Timestamp updateTimestamp;

}
