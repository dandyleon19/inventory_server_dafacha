package com.dafacha.inventory.infrastructure.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class JwtAuthenticationFilter extends AuthenticationWebFilter {

    private final JwtService jwtService;
    private final UserAccessResolver userAccessResolver;

    public JwtAuthenticationFilter(JwtService jwtService, UserAccessResolver userAccessResolver) {

        super(new ReactiveAuthenticationManager() {
            @Override
            public Mono<Authentication> authenticate(Authentication authentication) {
                return Mono.just(authentication);
            }
        });

        this.jwtService = jwtService;
        this.userAccessResolver = userAccessResolver;

        this.setServerAuthenticationConverter(this::convert);
        this.setSecurityContextRepository(NoOpServerSecurityContextRepository.getInstance());
    }

    /**
     * The token only proves who the caller is. What they are allowed to do (role, active flag, company,
     * warehouses) comes from the database, so changes to a user apply without waiting for the token to expire.
     */
    private Mono<Authentication> convert(ServerWebExchange exchange) {

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.empty();
        }

        String token = authHeader.substring(7);

        if (!jwtService.isValid(token)) return Mono.empty();

        return userAccessResolver.resolve(jwtService.extractUserId(token))
                .<Authentication>map(authUser -> new UsernamePasswordAuthenticationToken(
                        authUser,
                        null,
                        AuthorityUtils.createAuthorityList("ROLE_" + authUser.getRole())));
    }
}
