package com.msuzuki.microservices.core.product.services;

import com.msuzuki.api.core.product.Product;
import com.msuzuki.api.core.product.ProductService;
import com.msuzuki.api.event.Event;
import com.msuzuki.api.exceptions.EventProcessingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@AllArgsConstructor
@Slf4j
public class MessageProcessorConfig {

    private final ProductService productService;

    @Bean
    public Consumer<Event<Integer, Product>> messageProcessor() {
        return event -> {
            log.info("Process message created at{}...", event.getEventCreatedAt());

            switch (event.getEventType()) {
                case CREATE:
                    Product product = event.getData();
                    log.info("Create product with Id: {}", product.getProductId());
                    productService.createProduct(product).block();
                    break;
                case DELETE:
                    int productId = event.getKey();
                    log.info("Delete ProductId: {}", productId);
                    productService.deleteProduct(productId).block();
                    break;
                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a CREATE or DELETE event";
                    log.warn(errorMessage);
                    throw new EventProcessingException(errorMessage);
            }
            log.info("Message processing done!");
        };
    }
}
