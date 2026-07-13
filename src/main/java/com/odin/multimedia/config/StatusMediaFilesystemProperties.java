package com.odin.multimedia.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "status.media.filesystem")
public class StatusMediaFilesystemProperties {

    private String root = Paths.get(System.getProperty("java.io.tmpdir"), "odin-status-media").toString();

    public Path rootPath() {
        return Paths.get(root).toAbsolutePath().normalize();
    }
}
