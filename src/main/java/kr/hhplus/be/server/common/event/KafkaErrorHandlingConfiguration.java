package kr.hhplus.be.server.common.event;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import lombok.extern.slf4j.Slf4j;

/**
 * Kafka 에러 처리 및 DLQ 설정
 * 
 * - Consumer 실패 시 자동 재시도
 * - 최종 실패 시 DLQ(Dead Letter Queue)로 메시지 이동
 * - 재시도 전략 및 백오프 설정
 * 
 * Consumer 내부 로직 최적화를 우선하고, DLQ는 최종 안전망으로 사용
 */
@Slf4j
@Configuration
@EnableKafka
public class KafkaErrorHandlingConfiguration {

    @Value("${app.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /**
     * 에러 핸들러 설정
     * - 3회 재시도 후 DLQ로 이동
     * - 백오프: 1초 간격 재시도
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        // 재시도 전략: 3회 시도, 1초 간격
        FixedBackOff fixedBackOff = new FixedBackOff(1000L, 3L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(fixedBackOff);

        // DLQ로 보낼 예외 타입들 설정
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);

        log.info("🔧 Kafka Error Handler 설정 완료 - 재시도 3회, 백오프 1초");
        return errorHandler;
    }

    /**
     * Consumer 동시성 설정 - 병렬 처리 최적화
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(kafkaErrorHandler());

        // Consumer 동시성 설정 - 파티션 수만큼 동시 처리 가능
        factory.setConcurrency(3);

        // 기본 자동 커밋 사용 (Spring Boot 권장 방식)
        log.info("🚀 Kafka Consumer Factory 설정 완료 - 동시성: 3, 자동 커밋");
        return factory;
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "ecommerce-consumer-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // Consumer 안정성 설정 - application.yml의 자동 커밋 설정 사용
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "kr.hhplus.be.server.*.event");

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Producer 안정성 설정
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // 중복 방지
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3); // 재시도
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // 모든 복제본 확인

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}