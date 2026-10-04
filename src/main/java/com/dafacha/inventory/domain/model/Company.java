package com.dafacha.inventory.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Company {
    private Long id;
    private String name;
    private String socialReason;
    private String fiscalAddress;
    private String rucNumber;
    private String phone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
