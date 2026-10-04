package com.dafacha.inventory.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Supplier {
    private Long id;
    private Long companyId;
    private String name;
    private String rucNumber;
    private String contactName;
    private String phone;
    private String email;
    private String address;
    private String notes;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
