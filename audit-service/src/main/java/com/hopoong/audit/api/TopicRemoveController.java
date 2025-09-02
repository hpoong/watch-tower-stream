package com.hopoong.audit.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
public class TopicRemoveController {

    private final TopicRemoveService topicRemoveService;


    // 모든 고정 토픽 메시지 비우기
    @GetMapping("/topic/purge-all")
    public ResponseEntity<Void> purgeAll() {
        topicRemoveService.purgeAll();
        return ResponseEntity.noContent().build(); // 204
    }

}

