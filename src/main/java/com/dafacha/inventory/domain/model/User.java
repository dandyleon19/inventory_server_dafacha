package com.dafacha.inventory.domain.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private Boolean isActive;
    private Role role;
    private Long companyId;
    private Company company;
    /** Warehouses the user is limited to; null or empty means all of them. Not a column of users. */
    private List<Long> warehouseIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
