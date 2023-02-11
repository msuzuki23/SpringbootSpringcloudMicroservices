package com.msuzuki.api.composite.product;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ReviewSummary {
    private int reviewId;
    private String author;
    private String subject;
    private String content;
}
