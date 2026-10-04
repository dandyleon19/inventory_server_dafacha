package com.dafacha.inventory.config;

import com.dafacha.inventory.infrastructure.security.JwtAuthenticationFilter;
import com.dafacha.inventory.infrastructure.security.JwtService;
import com.dafacha.inventory.infrastructure.security.UserAccessResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

/**
 * The permission matrix. Reading is open to every authenticated user (a VIEWER can see everything); writing depends
 * on the role:
 * <pre>
 *                               ADMIN  WAREHOUSE  PURCHASING  VIEWER
 *   catalog, warehouses, company  x
 *   users, audit log              x
 *   stock adjustments             x
 *   entries, exits, transfers     x        x
 *   suppliers, purchase orders    x                  x
 *   receiving goods               x        x         x
 * </pre>
 * (ADMIN means ADMIN_USER and SUPER_ADMIN.) On top of the role, a user can be limited to some warehouses; that is
 * enforced in the use cases, not here.
 */
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String SUPER = "SUPER_ADMIN";
    private static final String ADMIN = "ADMIN_USER";
    private static final String WAREHOUSE = "WAREHOUSE_USER";
    private static final String PURCHASING = "PURCHASING_USER";

    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, UserAccessResolver userAccessResolver) {
        return new JwtAuthenticationFilter(jwtService, userAccessResolver);
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, JwtAuthenticationFilter jwtFilter) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/auth/bootstrap").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()

                        // Users and the audit log: admins only, for reading too.
                        .pathMatchers("/api/users", "/api/users/**", "/api/audit", "/api/audit/**")
                        .hasAnyRole(ADMIN, SUPER)

                        // Catalog, warehouses and company: any user reads; only admins change them.
                        .pathMatchers(HttpMethod.POST, "/api/products/**", "/api/product-categories/**", "/api/warehouses/**")
                        .hasAnyRole(ADMIN, SUPER)
                        .pathMatchers(HttpMethod.PUT, "/api/products/**", "/api/product-categories/**", "/api/warehouses/**")
                        .hasAnyRole(ADMIN, SUPER)
                        .pathMatchers(HttpMethod.DELETE, "/api/products/**", "/api/product-categories/**", "/api/warehouses/**")
                        .hasAnyRole(ADMIN, SUPER)
                        .pathMatchers(HttpMethod.PUT, "/api/company", "/api/company/**")
                        .hasAnyRole(ADMIN, SUPER)

                        // Suppliers: admins and purchasing manage them.
                        .pathMatchers(HttpMethod.POST, "/api/suppliers/**")
                        .hasAnyRole(ADMIN, SUPER, PURCHASING)
                        .pathMatchers(HttpMethod.PUT, "/api/suppliers/**")
                        .hasAnyRole(ADMIN, SUPER, PURCHASING)
                        .pathMatchers(HttpMethod.DELETE, "/api/suppliers/**")
                        .hasAnyRole(ADMIN, SUPER, PURCHASING)

                        // Purchase orders: receiving goods is also warehouse work (listed first so it wins over the
                        // rules below); creating, editing, ordering and cancelling is for admins and purchasing.
                        .pathMatchers(HttpMethod.POST, "/api/purchase-orders/*/receive")
                        .hasAnyRole(ADMIN, SUPER, WAREHOUSE, PURCHASING)
                        .pathMatchers(HttpMethod.POST, "/api/purchase-orders/**")
                        .hasAnyRole(ADMIN, SUPER, PURCHASING)
                        .pathMatchers(HttpMethod.PUT, "/api/purchase-orders/**")
                        .hasAnyRole(ADMIN, SUPER, PURCHASING)
                        .pathMatchers(HttpMethod.DELETE, "/api/purchase-orders/**")
                        .hasAnyRole(ADMIN, SUPER, PURCHASING)

                        // Stock: a physical-count adjustment rewrites the stock figure, so it is admin-only; entries,
                        // exits and transfers are day-to-day warehouse work.
                        .pathMatchers(HttpMethod.POST, "/api/stock/adjustments")
                        .hasAnyRole(ADMIN, SUPER)
                        .pathMatchers(HttpMethod.POST, "/api/stock/**")
                        .hasAnyRole(ADMIN, SUPER, WAREHOUSE)

                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().authenticated()
                )
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
