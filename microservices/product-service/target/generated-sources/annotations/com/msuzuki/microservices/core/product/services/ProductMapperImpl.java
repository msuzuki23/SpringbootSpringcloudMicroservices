package com.msuzuki.microservices.core.product.services;

import com.msuzuki.api.core.product.Product;
import com.msuzuki.microservices.core.product.persistence.ProductEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2023-02-19T14:45:26-0500",
    comments = "version: 1.5.3.Final, compiler: javac, environment: Java 19.0.2 (Oracle Corporation)"
)
@Component
public class ProductMapperImpl implements ProductMapper {

    @Override
    public Product entityToApi(ProductEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Product product = new Product();

        product.setProductId( entity.getProductId() );
        product.setName( entity.getName() );
        product.setWeight( entity.getWeight() );

        return product;
    }

    @Override
    public ProductEntity apiToEntity(Product api) {
        if ( api == null ) {
            return null;
        }

        ProductEntity productEntity = new ProductEntity();

        productEntity.setProductId( api.getProductId() );
        productEntity.setName( api.getName() );
        productEntity.setWeight( api.getWeight() );

        return productEntity;
    }
}
