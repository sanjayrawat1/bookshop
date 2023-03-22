package com.github.sanjayrawat1.bookshop.dispatcher;

import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Functions to perform actions as part of dispatching an order.
 * <p>
 * Spring Cloud Function is capable of managing functions defined in different ways, as long as they adhere to the standard Java interfaces Function, Supplier,
 * and Consumer. You can make Spring Cloud Function aware of your functions by registering them as beans.
 * <p>
 * Functions registered as beans are enhanced with extra features by the Spring Cloud Function framework. The beauty of this is that the business logic itself
 * is not aware of the surrounding framework. You can evolve it independently and test it without being concerned about framework-related issues.
 *
 * @author Sanjay Singh Rawat
 */
@Slf4j
@Configuration
public class DispatchingFunctions {

    /**
     * Functions defined as beans can be discovered and managed by Spring Cloud Function.
     */
    @Bean
    public Function<OrderAcceptedMessage, Long> pack() {
        return orderAcceptedMessage -> {
            log.info("The order with id : {} is packed.", orderAcceptedMessage.orderId());
            return orderAcceptedMessage.orderId();
        };
    }
}
