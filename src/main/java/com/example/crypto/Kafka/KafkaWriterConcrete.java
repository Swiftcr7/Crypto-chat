package com.example.crypto.Kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
@Slf4j
public class KafkaWriterConcrete implements KafkaWriter {
    private static final String BootstrapServer = "localhost:9093";

    private static final String ClientId = "producerKafkaWriter";

    private final KafkaProducer<byte[], byte[]> kafkaProducer;

    public KafkaWriterConcrete() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaWriterConcrete.BootstrapServer);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, KafkaWriterConcrete.ClientId);
        properties.put("auto.create.topics.enable", "true");
        kafkaProducer = new KafkaProducer<>(properties, new ByteArraySerializer(), new ByteArraySerializer());
    }

    @Override
    public void sendMessage(byte[] message, String topic) {
        log.info("Send message in {}", topic);
        try {
            ProducerRecord<byte[], byte[]> producerRecord = new ProducerRecord<>(topic, message);
            kafkaProducer.send(producerRecord);
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }

    }

    @Override
    public void close() {
        kafkaProducer.close();

    }
}
