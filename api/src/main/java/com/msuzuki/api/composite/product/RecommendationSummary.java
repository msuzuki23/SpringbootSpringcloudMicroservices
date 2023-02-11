package com.msuzuki.api.composite.product;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class RecommendationSummary {
    private int recommendationId;
    private String author;
    private int rate;
    private String content;
}
