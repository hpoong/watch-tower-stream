package com.hopoong.audit.adapter.kafka.resourcemetric.store;

import com.hopoong.core.topic.KafkaStoreManager;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class SystemMetricsStore {

    private final StreamsBuilderFactoryBean factoryBean;


    public Map<String, Long> errorCountStoreQueryAllStore() {
        KafkaStreams streams = factoryBean.getKafkaStreams();

        ReadOnlyKeyValueStore<String, Long> store = streams.store(
            StoreQueryParameters.fromNameAndType(
                    KafkaStoreManager.SYSTEM_RESOURCE_METRICS_ERROR_COUNT_STORE,
                QueryableStoreTypes.keyValueStore()
            )
        );

        Map<String, Long> result = new HashMap<>();

        KeyValueIterator<String, Long> all = store.all();

        while (all.hasNext()) {
            KeyValue<String, Long> next = all.next();
            System.out.println("Key = " + next.key + ", Value = " + next.value);
            result.put(next.key, next.value);
        }

        all.close();
        return result;
    }

}
