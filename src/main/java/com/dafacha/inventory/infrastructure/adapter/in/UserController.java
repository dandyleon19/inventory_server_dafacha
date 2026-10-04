package com.dafacha.inventory.infrastructure.adapter.in;

import com.dafacha.inventory.application.dto.request.ResetPasswordRequest;
import com.dafacha.inventory.application.dto.request.SaveUserRequest;
import com.dafacha.inventory.application.dto.response.PageResponse;
import com.dafacha.inventory.application.dto.response.UserResponse;
import com.dafacha.inventory.application.mapper.RequestDtoMapper;
import com.dafacha.inventory.domain.model.Role;
import com.dafacha.inventory.domain.ports.in.UserUseCase;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.UserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/** Users of the caller's company. Admins only (see SecurityConfig). */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserUseCase userUseCase;
    private final RequestDtoMapper requestDtoMapper;
    private final UserMapper userMapper;

    @GetMapping
    public Mono<ResponseEntity<PageResponse<UserResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean isActive
    ) {
        return userUseCase.findAll(page, size, search, role, isActive)
                .map(result -> result.map(userMapper::toResponse))
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<UserResponse>> findById(@PathVariable Long id) {
        return userUseCase.findById(id)
                .map(userMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @PostMapping
    public Mono<ResponseEntity<UserResponse>> create(@Valid @RequestBody SaveUserRequest request) {
        return userUseCase.create(requestDtoMapper.toUser(request))
                .map(userMapper::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<UserResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody SaveUserRequest request
    ) {
        return userUseCase.update(id, requestDtoMapper.toUser(request))
                .map(userMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @PutMapping("/{id}/password")
    public Mono<ResponseEntity<Void>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        return userUseCase.resetPassword(id, request.getPassword())
                .thenReturn(ResponseEntity.noContent().build());
    }
}
