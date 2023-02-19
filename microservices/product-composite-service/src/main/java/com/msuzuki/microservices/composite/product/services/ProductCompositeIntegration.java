package com.msuzuki.microservices.composite.product.services;

import static com.msuzuki.api.event.Event.Type.CREATE;

import brave.Tracer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msuzuki.api.core.product.Product;
import com.msuzuki.api.core.product.ProductService;
import com.msuzuki.api.core.recommendation.Recommendation;
import com.msuzuki.api.core.recommendation.RecommendationService;
import com.msuzuki.api.core.review.Review;
import com.msuzuki.api.core.review.ReviewService;
import com.msuzuki.api.event.Event;
import com.msuzuki.api.exceptions.InvalidInputException;
import com.msuzuki.api.exceptions.NotFoundException;
import com.msuzuki.util.http.HttpErrorInfo;
import com.msuzuki.util.http.ServiceUtil;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatusCode;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.io.IOException;
import java.net.URI;

import static com.msuzuki.api.event.Event.Type.DELETE;
import static java.util.logging.Level.FINE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static reactor.core.publisher.Flux.empty;

@Slf4j
@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {

    private static final String PRODUCT_SERVICE_URL = "http://product";
    private static final String RECOMMENDATION_SERVICE_URL = "http://recommendation";
    private static final String REVIEW_SERVICE_URL = "http://review";

    private final Scheduler publishEventScheduler;
    private final WebClient webClient;
    private final ObjectMapper mapper;
    private final StreamBridge streamBridge;

    private final ServiceUtil serviceUtil;

    private final ObservationRegistry registry;


    @Autowired
    public ProductCompositeIntegration(
            @Qualifier("publishEventScheduler") Scheduler publishEventScheduler,
            WebClient.Builder webClient,
            ObjectMapper mapper,
            StreamBridge streamBridge,
            ServiceUtil serviceUtil,
            ObservationRegistry registry
    ) {
        this.webClient = webClient.build();
        this.mapper = mapper;
        this.streamBridge = streamBridge;
        this.publishEventScheduler = publishEventScheduler;
        this.serviceUtil = serviceUtil;
        this.registry = registry;
    }

    @Override
    public Mono<Product> createProduct(Product body) {
        return Mono.fromCallable(() -> {
            sendMessage("products-out-0", new Event(CREATE, body.getProductId(), body));
            return body;
        }).subscribeOn(publishEventScheduler);
    }

    @Override
    @Retry(name = "product")
    @TimeLimiter(name = "product")
    @CircuitBreaker(name = "product", fallbackMethod = "getProductFallbackValue")
    public Mono<Product> getProduct(int productId, int delay, int faultPercent) {

//        String url = PRODUCT_SERVICE_URL + "/product/" + productId;

        URI url = UriComponentsBuilder.fromUriString(PRODUCT_SERVICE_URL
        + "/product/{productId}?delay={delay}&faultPercent={faultPercent}").build(productId, delay, faultPercent);
        log.debug("Will call getProduct API on URL: {}", url);

        return webClient.get().uri(url).retrieve()
                .bodyToMono(Product.class)
                .log(log.getName(), FINE)
                .onErrorMap(
                        WebClientResponseException.class,
                        ex -> handleException(ex)
                );
    }

    private Mono<Product> getProductFallbackValue(int productId, int delay, int faultPercent, CallNotPermittedException ex) {

        log.warn("Creating a fail-fast fallback product for productId = {}, delay = {}, faultPercent = {} and exception = {}",
                productId, delay, faultPercent, ex.toString());

        if (productId == 13) {
            String errMsg = "Product Id: " + productId + " not found in fallback cache!";
            log.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        return Mono.just(new Product(productId, "Fallback product" + productId, productId, serviceUtil.getServiceAddress()));
    }

    @Override
    public Mono<Void> deleteProduct(int productId) {
        return Mono.fromRunnable(() ->
                    sendMessage("products-out-0", new Event(DELETE, productId, null)))
                .subscribeOn(publishEventScheduler)
                .then();
    }

    @Override
    public Mono<Recommendation> createRecommendation(Recommendation body) {
        return Mono.fromCallable(() -> {
            sendMessage("recommendations-out-0", new Event(CREATE, body.getProductId(), body));
            return body;
        }).subscribeOn(publishEventScheduler);
    }

    @Override
    public Flux<Recommendation> getRecommendations(int productId) {

        String url = RECOMMENDATION_SERVICE_URL+ "/recommendation?productId=" + productId;
        log.debug("Will call getRecommendations API on URL: {}", url);

        return webClient.get().uri(url).retrieve()
                .bodyToFlux(Recommendation.class)
                .log(log.getName(), FINE)
                .onErrorResume(error -> empty());
    }

    @Override
    public Mono<Void> deleteRecommendation(int productId) {
        return Mono.fromRunnable(() ->
            sendMessage("recommendations-out-0", new Event(DELETE, productId, null)))
        .subscribeOn(publishEventScheduler).then();
    }

    @Override
    public Mono<Review> createReview(Review body) {
        return Mono.fromCallable(() -> {
            sendMessage("reviews-out-0", new Event(CREATE, body.getProductId(),body));
            return body;
        }).subscribeOn(publishEventScheduler);
    }
    @Override
    public Flux<Review> getReviews(int productId) {
        String url = REVIEW_SERVICE_URL + "/review?productId=" +productId;
        log.debug("Will call getReviews API on URL: {}", url);

        return webClient.get().uri(url).retrieve().bodyToFlux(Review.class)
                .log(log.getName(), FINE)
                .onErrorResume(error -> empty());
    }

    @Override
    public Mono<Void> deleteReviews(int productId) {
        return Mono.fromRunnable(() ->
                sendMessage("reviews-out-0", new Event(DELETE, productId, null))
            ).subscribeOn(publishEventScheduler).then();
    }

    private void sendMessage(String bindingName, Event event) {
        log.debug("Sending a {} message to {}", event.getEventType(), bindingName);
        Message message = MessageBuilder.withPayload(event)
                .setHeader("partitionKey",event.getKey())
                .build();
        streamBridge.send(bindingName, message);
    }

    private Throwable handleException(Throwable ex) {

        if (!(ex instanceof WebClientResponseException)) {
            log.warn("Got an unexpected error: {}, will rethrow it", ex.toString());
            return ex;
        }

        WebClientResponseException wcre = (WebClientResponseException) ex;

        HttpStatusCode statusCode = wcre.getStatusCode();
        if (statusCode.equals(NOT_FOUND)) {
            return new NotFoundException(getErrorMessage(wcre));
        } else if (statusCode.equals(UNPROCESSABLE_ENTITY)) {
            return new InvalidInputException(getErrorMessage(wcre));
        } else {
            log.warn("Got an unexpected HTTP error: {}, will rethrow it", wcre.getStatusCode());
            log.warn("Error body: {}", wcre.getResponseBodyAsString());
            return ex;
        }
    }

    private String getErrorMessage(WebClientResponseException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ex.getMessage();
        }
    }
}
