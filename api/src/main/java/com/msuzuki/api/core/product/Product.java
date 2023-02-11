package com.msuzuki.api.core.product;

import lombok.*;
import org.springframework.web.bind.annotation.GetMapping;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Product {
    private int productId;
    private String name;
    private int weight;
    private String serviceAddress;
}
