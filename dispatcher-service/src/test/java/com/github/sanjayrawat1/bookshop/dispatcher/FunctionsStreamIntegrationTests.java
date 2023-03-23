package com.github.sanjayrawat1.bookshop.dispatcher;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

/**
 * @author Sanjay Singh Rawat
 */
@SpringBootTest
public class FunctionsStreamIntegrationTests {

    /**
     * Represents the input binding packlabel-in-0
     */
    @Autowired
    private InputDestination input;

    /**
     * Represents the output binding packlabel-out-0
     */
    @Autowired
    private OutputDestination output;

    /**
     * Message brokers like RabbitMQ deal with binary data, so any data flowing through them is mapped to byte[] in Java.
     * The conversion between bytes and DTOs is handled by Spring Cloud Stream transparently. But just like for messages,
     * we need to handle that explicitly in this test scenario when asserting the content of the message received from the output channel.
     */
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void whenOrderAcceptedThenDispatched() throws IOException {
        long orderId = 121;
        Message<OrderAcceptedMessage> inputMessage = MessageBuilder.withPayload(new OrderAcceptedMessage(orderId)).build();
        Message<OrderDispatchedMessage> expectedOutputMessage = MessageBuilder.withPayload(new OrderDispatchedMessage(orderId)).build();

        this.input.send(inputMessage);
        assertThat(objectMapper.readValue(output.receive().getPayload(), OrderDispatchedMessage.class)).isEqualTo(expectedOutputMessage.getPayload());
    }
}
