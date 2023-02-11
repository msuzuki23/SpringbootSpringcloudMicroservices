package com.msuzuki.api.core.recommendation;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface RecommendationService {

    /**
     * Sample usage: "curl $HOST:$PORT/recommendation?productId=1"
     *
     * @param productId, Id of the product
     * @return the recommendation of the product
     */
    @GetMapping(
            value = "recommendation",
            produces = "application/json"
    )
    Flux<Recommendation> getRecommendations(
            @RequestParam(value = "productId", required = true) int productId
    );

    Mono<Recommendation> createRecommendation(@RequestBody Recommendation body);

    Mono<Void> deleteRecommendation(@RequestParam(value = "productId", required = true) int productId);
}
