package com.msuzuki.microservices.core.recommendation.services;

import static java.util.logging.Level.FINE;

import com.mongodb.DuplicateKeyException;
import com.msuzuki.api.core.recommendation.Recommendation;
import com.msuzuki.api.core.recommendation.RecommendationService;
import com.msuzuki.api.exceptions.InvalidInputException;
import com.msuzuki.microservices.core.recommendation.persistence.RecommendationEntity;
import com.msuzuki.microservices.core.recommendation.persistence.RecommendationRepository;
import com.msuzuki.util.http.ServiceUtil;
import io.micrometer.observation.ObservationRegistry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@AllArgsConstructor
@RestController
public class RecommendationServiceImpl implements RecommendationService {
    private final ObservationRegistry registry;
    private final RecommendationRepository repository;
    private final RecommendationMapper mapper;
    private final ServiceUtil serviceUtil;
    @Override
    public Flux<Recommendation> getRecommendations(int productId) {

        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        log.info("Will get recommendations for product with id={}", productId);

        return repository.findByProductId(productId)
                .log(log.getName(), FINE)
                .map(e -> mapper.entityToApi(e))
                .map(e -> setServiceAddress(e));
    }

    @Override
    public Mono<Recommendation> createRecommendation(Recommendation body) {

        if (body.getProductId() < 1) {
            throw new InvalidInputException("Invalid productId: " + body.getProductId());
        }

        RecommendationEntity entity = mapper.apiToEntity(body);
        Mono<Recommendation> newEntity = repository.save(entity)
                .log(log.getName(), FINE)
                .onErrorMap(
                        DuplicateKeyException.class,
                        ex -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId() + ", Recommendation Id: " + body.getRecommendationId())
                )
                .map(e -> mapper.entityToApi(e));
        return newEntity;
    }

    @Override
    public Mono<Void> deleteRecommendation(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        log.debug("deleteRecommendations: tries to delete recommendations for productId: {}", productId);
        return repository.deleteAll(repository.findByProductId(productId));
    }

    private Recommendation setServiceAddress(Recommendation e) {
        e.setServiceAddress(serviceUtil.getServiceAddress());
        return e;
    }
}
