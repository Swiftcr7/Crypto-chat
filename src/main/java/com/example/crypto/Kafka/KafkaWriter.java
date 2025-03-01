package com.example.crypto.Kafka;

import org.springframework.stereotype.Service;

@Service
public interface KafkaWriter {
    public void sendMessage(byte[] message, String topic);

    public void close();
}
