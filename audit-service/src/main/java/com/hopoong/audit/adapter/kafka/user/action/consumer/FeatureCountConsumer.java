package com.hopoong.audit.adapter.kafka.user.action.consumer;

import com.carroti.um.FeatureCount5mRecord;
import com.hopoong.core.topic.KafkaTopicManager;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeatureCountConsumer {

    private final RedisTemplate<String, Object> redisTemplate;


    @KafkaListener(
            topics = KafkaTopicManager.USER_FEATURE_COUNT_5M,
            groupId = "user-feature-count-5m-group",
            containerFactory = "avroKafkaListenerContainerFactory",
            concurrency = "1"
    )
    public void userFeatureCountConsume(ConsumerRecord<String, FeatureCount5mRecord> record) {
        String key = record.key();
        FeatureCount5mRecord event = record.value();

        System.out.println("USER_FEATURE_COUNT_5M ==============");
        System.out.println(key);
        System.out.println(key);
        System.out.println(key);
        System.out.println(key);


//        var rec = reca.value();
//        String redisKey = String.format("rt:tenant:%s:feature:%s:count:5m:%d",
//                rec.getTenantId(), rec.getFeature(), rec.getWindowStart());
//
//        System.out.println("==================");
//        System.out.println(redisKey);
//        System.out.println(rec.getCount());
//
//        // 업서트 + TTL
////        redisTemplate.opsForValue().set(redisKey, String.valueOf(rec.getCount()), Duration.ofHours(6));
    }


}