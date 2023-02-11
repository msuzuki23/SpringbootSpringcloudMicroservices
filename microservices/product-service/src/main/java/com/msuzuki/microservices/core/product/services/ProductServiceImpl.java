package com.msuzuki.microservices.core.product.services;

import com.msuzuki.api.core.product.Product;
import com.msuzuki.api.core.product.ProductService;
import com.msuzuki.api.exceptions.InvalidInputException;
import com.msuzuki.api.exceptions.NotFoundException;
import com.msuzuki.microservices.core.product.persistence.ProductEntity;

import com.msuzuki.microservices.core.product.persistence.ProductRepository;
import com.msuzuki.util.http.ServiceUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.swing.text.html.parser.Entity;

import static java.util.logging.Level.FINE;

@Slf4j
@AllArgsConstructor
@RestController
public class ProductServiceImpl implements ProductService {
    private final ServiceUtil serviceUtil;
    private final ProductRepository repository;
    private final ProductMapper mapper;

    @Override
    public Mono<Product> getProduct(int productId) {

        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        log.info("Will get product info for id={}", productId);

        return repository.findByProductId(productId)
                .switchIfEmpty(Mono.error((new NotFoundException("No product found for productId: " + productId))))
                .log(log.getName(), FINE)
                .map(e -> mapper.entityToApi(e))
                .map(e -> setServiceAddress(e));
    }

    @Override
    public Mono<Product> createProduct(Product body) {

            if (body.getProductId() < 1) {
                throw new InvalidInputException("Invalid productId: " + body.getProductId());
            }

            ProductEntity entity = mapper.apiToEntity(body);
            Mono<Product> newEntity = repository.save(entity)
                    .log(log.getName(), FINE)
                    .onErrorMap(
                            DuplicateKeyException.class,
                            ex -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId())
                    )
                    .map(e -> mapper.entityToApi(e));
            return newEntity;
    }

    @Override
    public Mono<Void> deleteProduct(int productId) {

        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        log.debug("deleteProduct: tries to delete an entity with productId: {}", productId);
        return repository.findByProductId(productId)
                .log(log.getName(), FINE)
                .map(e -> repository.delete(e))
                .flatMap(e -> e);
    }

    private Product setServiceAddress(Product e) {
        e.setServiceAddress(serviceUtil.getServiceAddress());
        return e;
    }
}
