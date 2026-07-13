package com.odin.multimedia.service.status;
import java.io.InputStream;
import com.odin.multimedia.enums.StatusMediaType;
public final class StatusMediaAccess {
    private final InputStream stream; private final StatusMediaType mediaType;
    public StatusMediaAccess(InputStream stream, StatusMediaType mediaType){this.stream=stream;this.mediaType=mediaType;}
    public InputStream getStream(){return stream;} public StatusMediaType getMediaType(){return mediaType;}
}
