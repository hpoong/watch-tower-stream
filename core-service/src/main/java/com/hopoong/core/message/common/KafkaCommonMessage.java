package com.hopoong.core.message.common;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class KafkaCommonMessage<T> {
    private Header header;
    private T body;

    @Getter
    @Builder
    public static class Header {
        private String type;
        @Builder.Default private String source = "resource-service";
        private String traceId;
        private String timestamp;
        @Builder.Default private String version = "v1";
    }
}


