package com.msuzuki.microservices.core.review.services;

import com.msuzuki.api.core.review.Review;
import com.msuzuki.api.core.review.ReviewService;
import com.msuzuki.api.exceptions.InvalidInputException;
import com.msuzuki.microservices.core.review.persistence.ReviewEntity;
import com.msuzuki.microservices.core.review.persistence.ReviewRepository;
import com.msuzuki.util.http.ServiceUtil;
import io.micrometer.observation.ObservationRegistry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static java.util.logging.Level.FINE;

@Slf4j
@AllArgsConstructor
@RestController
public class ReviewServiceImpl implements ReviewService {

    private final ObservationRegistry registry;

    private final ReviewRepository repository;
    private final ReviewMapper mapper;
    private final ServiceUtil serviceUtil;
    @Qualifier("jdbcScheduler")
    private final Scheduler jdbcScheduler;

    @Override
    public Mono<Review> createReview(Review body) {
        if (body.getReviewId() < -1) {
            throw  new InvalidInputException("Invalid productId: " + body.getProductId());
        }
        return Mono.fromCallable(() -> internalCreateReview(body))
                .subscribeOn(jdbcScheduler);
    }

    private Review internalCreateReview(Review body) {
        try {
            ReviewEntity entity = mapper.apiToEntity(body);
            ReviewEntity newEntity = repository.save(entity);

            log.debug("internalCreateReview: created a review entity: {}/{}", body.getProductId(), body.getReviewId());

            return mapper.entityToApi(newEntity);

        } catch (DataIntegrityViolationException dive) {
            String exceptionMessage = "Duplicate key, Product Id: " + body.getProductId() + ", Review Id: " + body.getReviewId();
            throw new InvalidInputException(exceptionMessage);
        }
    }
    @Override
    public Flux<Review> getReviews(int productId) {

        if (productId < -1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        log.info("Will get reviews for product with id={}", productId);

        return Mono.fromCallable(() -> internalGetReviews(productId))
                .flatMapMany(Flux::fromIterable)
                .log(log.getName(), FINE)
                .subscribeOn(jdbcScheduler);
    }

    private List<Review> internalGetReviews(int productId) {
        List<ReviewEntity> entityList = repository.findByProductId(productId);
        List<Review> reviews = mapper.entityListToApiList(entityList);
        reviews.forEach(e -> e.setServiceAddress(serviceUtil.getServiceAddress()));

        log.debug("internalGetReviews: response size: {}", reviews.size());

        return reviews;
    }

    @Override
    public Mono<Void> deleteReviews(int productId) {

        if (productId < -1) {
            throw new InvalidInputException("Invalid productId: " +  productId);
        }

        return Mono.fromRunnable(() -> internalDeleteReviews(productId))
                .subscribeOn(jdbcScheduler)
                .then();
    }

    private void internalDeleteReviews(int productId) {
        log.debug("deleteReviews: tries to delete reviews for the product with productId: {}", productId);
        repository.deleteAll(repository.findByProductId(productId));
    }
}
