package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.domain.model.Role;
import com.dafacha.inventory.domain.model.User;
import com.dafacha.inventory.domain.ports.out.UserRepositoryPort;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Users go through the Spring Data repository; the user -> warehouses link has no entity (a pure join table),
 * so it is read and written with DatabaseClient.
 */
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private static final String WAREHOUSES_SQL =
            "SELECT user_id, warehouse_id FROM user_warehouses WHERE user_id IN (:userIds)";
    private static final String DELETE_WAREHOUSES_SQL = "DELETE FROM user_warehouses WHERE user_id = :userId";
    private static final String INSERT_WAREHOUSE_SQL =
            "INSERT INTO user_warehouses (user_id, warehouse_id) VALUES (:userId, :warehouseId) ON CONFLICT DO NOTHING";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<User> save(User user) {
        return userRepository.save(userMapper.toEntity(user))
                .map(userMapper::toDomain);
    }

    @Override
    public Mono<User> findById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDomain);
    }

    @Override
    public Mono<User> findByIdAndCompanyId(Long id, Long companyId) {
        return userRepository.findByIdAndCompanyId(id, companyId)
                .map(userMapper::toDomain);
    }

    @Override
    public Mono<User> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByEmail(String email, Long excludeId) {
        return userRepository.existsByEmail(email, excludeId);
    }

    @Override
    public Mono<Long> countAll() {
        return userRepository.count();
    }

    @Override
    public Flux<User> findPage(Long companyId, Role role, Boolean isActive, String searchPattern, int page, int size) {
        return userRepository.findPage(companyId, role == null ? null : role.name(), isActive, searchPattern,
                        size, (long) page * size)
                .map(userMapper::toDomain);
    }

    @Override
    public Mono<Long> count(Long companyId, Role role, Boolean isActive, String searchPattern) {
        return userRepository.countFiltered(companyId, role == null ? null : role.name(), isActive, searchPattern);
    }

    @Override
    public Mono<Map<Long, List<Long>>> findWarehouseIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Mono.just(Map.of());
        }
        return databaseClient.sql(WAREHOUSES_SQL)
                .bind("userIds", new ArrayList<>(userIds))
                .map(row -> Map.entry(row.get("user_id", Long.class), row.get("warehouse_id", Long.class)))
                .all()
                .collectList()
                .map(entries -> {
                    Map<Long, List<Long>> byUser = new HashMap<>();
                    entries.forEach(entry ->
                            byUser.computeIfAbsent(entry.getKey(), key -> new ArrayList<>()).add(entry.getValue()));
                    return byUser;
                });
    }

    @Override
    public Mono<Void> replaceWarehouses(Long userId, Collection<Long> warehouseIds) {
        return databaseClient.sql(DELETE_WAREHOUSES_SQL)
                .bind("userId", userId)
                .fetch()
                .rowsUpdated()
                .thenMany(Flux.fromIterable(warehouseIds == null ? List.<Long>of() : warehouseIds)
                        .concatMap(warehouseId -> databaseClient.sql(INSERT_WAREHOUSE_SQL)
                                .bind("userId", userId)
                                .bind("warehouseId", warehouseId)
                                .fetch()
                                .rowsUpdated()))
                .then();
    }
}
