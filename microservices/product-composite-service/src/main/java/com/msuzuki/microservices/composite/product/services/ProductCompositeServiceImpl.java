package com.msuzuki.microservices.composite.product.services;

import com.msuzuki.api.composite.product.*;
import com.msuzuki.api.core.product.Product;
import com.msuzuki.api.core.recommendation.Recommendation;
import com.msuzuki.api.core.review.Review;
import com.msuzuki.util.http.ServiceUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.logging.Level.FINE;

@Slf4j
@AllArgsConstructor
@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {

    private final ServiceUtil serviceUtil;
    private ProductCompositeIntegration integration;

    @Override
    public Mono<Void> createProduct(ProductAggregate body) {
        try {
            List<Mono> monoList = new ArrayList<>();

            log.debug("createCompositeProduct: creates a new composite entity fro productId: {}", body.getProductId());

            Product product = new Product(body.getProductId(), body.getName(), body.getWeight(), null);
            monoList.add(integration.createProduct(product));

            if (body.getRecommendations() != null) {
                body.getRecommendations().forEach(r -> {
                    Recommendation recommendation = new Recommendation(body.getProductId(), r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent(), null);
                    monoList.add(integration.createRecommendation(recommendation));
                });
            }

            if (body.getReviews() != null) {
                body.getReviews().forEach((r -> {
                    Review review = new Review(body.getProductId(), r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent(), null);
                    monoList.add(integration.createReview(review));
                }));
            }

            log.debug("createCompositeProduct: composite entities created for productId: {}", body.getProductId());

            return Mono.zip(r -> "", monoList.toArray(new Mono[0]))
                    .doOnError(ex -> log.warn("createCompositeProduct failed: {}", ex.toString()))
                    .then();

        } catch (RuntimeException re) {
            log.warn("createCompositeProduct failed", re);
            throw re;
        }
    }

    @Override
    public Mono<ProductAggregate> getProduct(int productId, int delay, int faultPercent) {

        log.info("Will get composite product info for productId: {}", productId);

        return Mono.zip(
                        values -> createProductAggregate((Product) values[0], (List<Recommendation>) values[1], (List<Review>) values[2], serviceUtil.getServiceAddress()),
                        integration.getProduct(productId, delay, faultPercent),
                        integration.getRecommendations(productId).collectList(),
                        integration.getReviews(productId).collectList())
                .doOnError(ex -> log.warn("getCompositeProduct failed: {}", ex.toString()))
                .log(log.getName(), FINE);
    }

    @Override
    public Mono<Void> deleteProduct(int productId) {
        try {
            log.debug("deleteCompositeProduct: Deletes a product aggregate for productId: {}", productId);

            return Mono.zip(
                        r -> "",
                        integration.deleteProduct(productId),
                        integration.deleteRecommendation(productId),
                        integration.deleteReviews(productId))
                    .doOnError(ex -> log.warn("delete failed: {}", ex.toString()))
                    .log(log.getName(), FINE).then();
        } catch (RuntimeException re) {
            log.warn("deleteCompositeProduct failed: {}", re.toString());
            throw re;
        }
    }

    private ProductAggregate createProductAggregate(
            Product product,
            List<Recommendation> recommendations,
            List<Review> reviews,
            String serviceAddress
    ) {

        // 1. Setup product info
        int productId = product.getProductId();
        String name = product.getName();
        int weight = product.getWeight();

        // 2. Copy summary recommendation info, if available
        List<RecommendationSummary> recommendationSummaries =
                (recommendations == null) ? null : recommendations.stream()
                        .map(r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent()))
                        .collect(Collectors.toList());

        // 3. Copy summary review info, if available
        List<ReviewSummary> reviewSummaries =(reviews == null) ? null : reviews.stream()
                .map(r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent()))
                .collect(Collectors.toList());

        // 4. Create info regarding the involved microservices addresses
        String productAddress = product.getServiceAddress();
        String reviewAddress = (reviews != null && reviews.size() > 0) ? reviews.get(0).getServiceAddress() : "";
        String recommendationAddress = (recommendations != null && recommendations.size() > 0) ? recommendations.get(0).getServiceAddress() : "";
        ServiceAddresses serviceAddresses = new ServiceAddresses(serviceAddress, productAddress, reviewAddress, recommendationAddress);

        return new ProductAggregate(productId, name, weight, recommendationSummaries, reviewSummaries, serviceAddresses);
    }

    // TODO: In Chapter 12 there is an extra code/function related to Security and Authorization
    // Double-check that to make sure that is related to the authorization server
    // Double-check if this is needed when using Keycloak
}
