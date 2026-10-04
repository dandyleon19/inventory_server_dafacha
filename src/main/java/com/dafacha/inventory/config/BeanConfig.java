package com.dafacha.inventory.config;

import com.dafacha.inventory.application.service.AuditRecorder;
import com.dafacha.inventory.application.service.AuditServiceImpl;
import com.dafacha.inventory.application.service.AuthServiceImpl;
import com.dafacha.inventory.application.service.UserServiceImpl;
import com.dafacha.inventory.domain.ports.in.AuditUseCase;
import com.dafacha.inventory.domain.ports.in.UserUseCase;
import com.dafacha.inventory.domain.ports.out.AuditRepositoryPort;
import com.dafacha.inventory.domain.ports.out.UserAccessInvalidator;
import com.dafacha.inventory.infrastructure.adapter.out.repository.AuditRepositoryAdapter;
import com.dafacha.inventory.infrastructure.security.UserAccessResolver;
import com.dafacha.inventory.application.service.DashboardServiceImpl;
import com.dafacha.inventory.application.service.StockLedger;
import com.dafacha.inventory.domain.ports.in.DashboardUseCase;
import com.dafacha.inventory.domain.ports.out.CostLayerRepositoryPort;
import com.dafacha.inventory.domain.ports.out.StockLotRepositoryPort;
import com.dafacha.inventory.infrastructure.adapter.out.repository.CostLayerRepositoryAdapter;
import com.dafacha.inventory.infrastructure.adapter.out.repository.StockLotRepositoryAdapter;
import com.dafacha.inventory.application.service.PurchaseOrderServiceImpl;
import com.dafacha.inventory.application.service.SupplierServiceImpl;
import com.dafacha.inventory.domain.ports.in.PurchaseOrderUseCase;
import com.dafacha.inventory.domain.ports.in.SupplierUseCase;
import com.dafacha.inventory.domain.ports.out.DocumentSequencePort;
import com.dafacha.inventory.domain.ports.out.PurchaseOrderRepositoryPort;
import com.dafacha.inventory.domain.ports.out.SupplierRepositoryPort;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.PurchaseOrderMapper;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.SupplierMapper;
import com.dafacha.inventory.infrastructure.adapter.out.repository.DocumentSequenceAdapter;
import com.dafacha.inventory.infrastructure.adapter.out.repository.PurchaseOrderLineRepository;
import com.dafacha.inventory.infrastructure.adapter.out.repository.PurchaseOrderRepository;
import com.dafacha.inventory.infrastructure.adapter.out.repository.PurchaseOrderRepositoryAdapter;
import com.dafacha.inventory.infrastructure.adapter.out.repository.SupplierRepository;
import com.dafacha.inventory.infrastructure.adapter.out.repository.SupplierRepositoryAdapter;
import com.dafacha.inventory.application.service.StockServiceImpl;
import com.dafacha.inventory.application.service.WarehouseServiceImpl;
import com.dafacha.inventory.domain.ports.in.StockUseCase;
import com.dafacha.inventory.domain.ports.in.WarehouseUseCase;
import com.dafacha.inventory.domain.ports.out.StockMovementRepositoryPort;
import com.dafacha.inventory.domain.ports.out.StockRepositoryPort;
import com.dafacha.inventory.domain.ports.out.TransactionPort;
import com.dafacha.inventory.domain.ports.out.WarehouseRepositoryPort;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.StockMovementMapper;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.WarehouseMapper;
import com.dafacha.inventory.infrastructure.adapter.out.repository.StockMovementRepository;
import com.dafacha.inventory.infrastructure.adapter.out.repository.StockMovementRepositoryAdapter;
import com.dafacha.inventory.infrastructure.adapter.out.repository.StockRepositoryAdapter;
import com.dafacha.inventory.infrastructure.adapter.out.repository.TransactionAdapter;
import com.dafacha.inventory.infrastructure.adapter.out.repository.WarehouseRepository;
import com.dafacha.inventory.infrastructure.adapter.out.repository.WarehouseRepositoryAdapter;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import com.dafacha.inventory.application.service.CompanyServiceImpl;
import com.dafacha.inventory.domain.ports.in.CompanyUseCase;
import com.dafacha.inventory.domain.ports.out.CompanySettingsRepositoryPort;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.CompanySettingsMapper;
import com.dafacha.inventory.infrastructure.adapter.out.repository.CompanySettingsRepository;
import com.dafacha.inventory.infrastructure.adapter.out.repository.CompanySettingsRepositoryAdapter;
import com.dafacha.inventory.application.service.ProductCategoryServiceImpl;
import com.dafacha.inventory.application.service.ProductServiceImpl;
import com.dafacha.inventory.config.properties.JwtProperties;
import com.dafacha.inventory.domain.ports.in.AuthUseCase;
import com.dafacha.inventory.domain.ports.in.ProductCategoryUseCase;
import com.dafacha.inventory.domain.ports.in.ProductUseCase;
import com.dafacha.inventory.domain.ports.out.CompanyRepositoryPort;
import com.dafacha.inventory.domain.ports.out.ProductCategoryRepositoryPort;
import com.dafacha.inventory.domain.ports.out.ProductRepositoryPort;
import com.dafacha.inventory.domain.ports.out.RefreshTokenRepositoryPort;
import com.dafacha.inventory.domain.ports.out.UserRepositoryPort;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.CompanyMapper;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.ProductCategoryMapper;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.ProductMapper;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.RefreshTokenMapper;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.UserMapper;
import com.dafacha.inventory.infrastructure.adapter.out.repository.CompanyRepository;
import com.dafacha.inventory.infrastructure.adapter.out.repository.CompanyRepositoryAdapter;
import com.dafacha.inventory.infrastructure.adapter.out.repository.ProductCategoryRepository;
import com.dafacha.inventory.infrastructure.adapter.out.repository.ProductCategoryRepositoryAdapter;
import com.dafacha.inventory.infrastructure.adapter.out.repository.ProductRepository;
import com.dafacha.inventory.infrastructure.adapter.out.repository.ProductRepositoryAdapter;
import com.dafacha.inventory.infrastructure.adapter.out.repository.RefreshTokenRepository;
import com.dafacha.inventory.infrastructure.adapter.out.repository.RefreshTokenRepositoryAdapter;
import com.dafacha.inventory.infrastructure.adapter.out.repository.UserRepository;
import com.dafacha.inventory.infrastructure.adapter.out.repository.UserRepositoryAdapter;
import com.dafacha.inventory.infrastructure.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Hexagonal wiring: use cases and repository adapters are plain classes (no Spring stereotypes),
 * assembled here so the domain/application layers stay framework-free.
 */
@Configuration
public class BeanConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthUseCase authUseCase(
            UserRepositoryPort userRepositoryPort,
            CompanyRepositoryPort companyRepositoryPort,
            WarehouseRepositoryPort warehouseRepositoryPort,
            RefreshTokenRepositoryPort refreshTokenRepositoryPort,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            AuditRecorder auditRecorder
    ) {
        return new AuthServiceImpl(
                userRepositoryPort,
                companyRepositoryPort,
                warehouseRepositoryPort,
                refreshTokenRepositoryPort,
                passwordEncoder,
                jwtService,
                jwtProperties,
                auditRecorder
        );
    }

    @Bean
    public UserUseCase userUseCase(
            UserRepositoryPort userRepositoryPort,
            WarehouseRepositoryPort warehouseRepositoryPort,
            RefreshTokenRepositoryPort refreshTokenRepositoryPort,
            PasswordEncoder passwordEncoder,
            UserAccessInvalidator userAccessInvalidator,
            AuditRecorder auditRecorder
    ) {
        return new UserServiceImpl(
                userRepositoryPort,
                warehouseRepositoryPort,
                refreshTokenRepositoryPort,
                passwordEncoder,
                userAccessInvalidator,
                auditRecorder
        );
    }

    /** One instance is both what the request filter asks and what user management invalidates. */
    @Bean
    public UserAccessResolver userAccessResolver(UserRepositoryPort userRepositoryPort) {
        return new UserAccessResolver(userRepositoryPort);
    }

    @Bean
    public AuditRepositoryPort auditRepositoryPort(DatabaseClient databaseClient) {
        return new AuditRepositoryAdapter(databaseClient);
    }

    @Bean
    public AuditRecorder auditRecorder(AuditRepositoryPort auditRepositoryPort) {
        return new AuditRecorder(auditRepositoryPort);
    }

    @Bean
    public AuditUseCase auditUseCase(AuditRepositoryPort auditRepositoryPort) {
        return new AuditServiceImpl(auditRepositoryPort);
    }

    @Bean
    public ProductUseCase productUseCase(
            ProductRepositoryPort productRepositoryPort,
            ProductCategoryRepositoryPort productCategoryRepositoryPort,
            CompanySettingsRepositoryPort companySettingsRepositoryPort,
            StockMovementRepositoryPort stockMovementRepositoryPort,
            StockLotRepositoryPort stockLotRepositoryPort,
            StockRepositoryPort stockRepositoryPort,
            AuditRecorder auditRecorder
    ) {
        return new ProductServiceImpl(
                productRepositoryPort,
                productCategoryRepositoryPort,
                companySettingsRepositoryPort,
                stockMovementRepositoryPort,
                stockLotRepositoryPort,
                stockRepositoryPort,
                auditRecorder
        );
    }

    @Bean
    public WarehouseUseCase warehouseUseCase(
            WarehouseRepositoryPort warehouseRepositoryPort,
            StockMovementRepositoryPort stockMovementRepositoryPort,
            AuditRecorder auditRecorder
    ) {
        return new WarehouseServiceImpl(warehouseRepositoryPort, stockMovementRepositoryPort, auditRecorder);
    }

    @Bean
    public StockLedger stockLedger(
            StockRepositoryPort stockRepositoryPort,
            StockLotRepositoryPort stockLotRepositoryPort,
            StockMovementRepositoryPort stockMovementRepositoryPort,
            CostLayerRepositoryPort costLayerRepositoryPort
    ) {
        return new StockLedger(stockRepositoryPort, stockLotRepositoryPort, stockMovementRepositoryPort,
                costLayerRepositoryPort);
    }

    @Bean
    public CostLayerRepositoryPort costLayerRepositoryPort(DatabaseClient databaseClient) {
        return new CostLayerRepositoryAdapter(databaseClient);
    }

    @Bean
    public StockUseCase stockUseCase(
            StockRepositoryPort stockRepositoryPort,
            StockMovementRepositoryPort stockMovementRepositoryPort,
            StockLotRepositoryPort stockLotRepositoryPort,
            WarehouseRepositoryPort warehouseRepositoryPort,
            ProductRepositoryPort productRepositoryPort,
            CompanySettingsRepositoryPort companySettingsRepositoryPort,
            TransactionPort transactionPort,
            StockLedger stockLedger,
            CostLayerRepositoryPort costLayerRepositoryPort
    ) {
        return new StockServiceImpl(
                stockRepositoryPort,
                stockMovementRepositoryPort,
                stockLotRepositoryPort,
                warehouseRepositoryPort,
                productRepositoryPort,
                companySettingsRepositoryPort,
                transactionPort,
                stockLedger,
                costLayerRepositoryPort
        );
    }

    @Bean
    public DashboardUseCase dashboardUseCase(
            StockRepositoryPort stockRepositoryPort,
            StockLotRepositoryPort stockLotRepositoryPort,
            StockMovementRepositoryPort stockMovementRepositoryPort,
            PurchaseOrderRepositoryPort purchaseOrderRepositoryPort,
            ProductRepositoryPort productRepositoryPort,
            WarehouseRepositoryPort warehouseRepositoryPort,
            CompanySettingsRepositoryPort companySettingsRepositoryPort
    ) {
        return new DashboardServiceImpl(
                stockRepositoryPort,
                stockLotRepositoryPort,
                stockMovementRepositoryPort,
                purchaseOrderRepositoryPort,
                productRepositoryPort,
                warehouseRepositoryPort,
                companySettingsRepositoryPort
        );
    }

    @Bean
    public StockLotRepositoryPort stockLotRepositoryPort(DatabaseClient databaseClient) {
        return new StockLotRepositoryAdapter(databaseClient);
    }

    @Bean
    public SupplierUseCase supplierUseCase(
            SupplierRepositoryPort supplierRepositoryPort,
            PurchaseOrderRepositoryPort purchaseOrderRepositoryPort,
            AuditRecorder auditRecorder
    ) {
        return new SupplierServiceImpl(supplierRepositoryPort, purchaseOrderRepositoryPort, auditRecorder);
    }

    @Bean
    public PurchaseOrderUseCase purchaseOrderUseCase(
            PurchaseOrderRepositoryPort purchaseOrderRepositoryPort,
            SupplierRepositoryPort supplierRepositoryPort,
            WarehouseRepositoryPort warehouseRepositoryPort,
            ProductRepositoryPort productRepositoryPort,
            StockRepositoryPort stockRepositoryPort,
            StockLedger stockLedger,
            DocumentSequencePort documentSequencePort,
            TransactionPort transactionPort,
            AuditRecorder auditRecorder
    ) {
        return new PurchaseOrderServiceImpl(
                purchaseOrderRepositoryPort,
                supplierRepositoryPort,
                warehouseRepositoryPort,
                productRepositoryPort,
                stockRepositoryPort,
                stockLedger,
                documentSequencePort,
                transactionPort,
                auditRecorder
        );
    }

    @Bean
    public SupplierRepositoryPort supplierRepositoryPort(SupplierRepository repository, SupplierMapper supplierMapper) {
        return new SupplierRepositoryAdapter(repository, supplierMapper);
    }

    @Bean
    public PurchaseOrderRepositoryPort purchaseOrderRepositoryPort(
            PurchaseOrderRepository orderRepository,
            PurchaseOrderLineRepository lineRepository,
            PurchaseOrderMapper purchaseOrderMapper,
            DatabaseClient databaseClient
    ) {
        return new PurchaseOrderRepositoryAdapter(orderRepository, lineRepository, purchaseOrderMapper, databaseClient);
    }

    @Bean
    public DocumentSequencePort documentSequencePort(DatabaseClient databaseClient) {
        return new DocumentSequenceAdapter(databaseClient);
    }

    @Bean
    public WarehouseRepositoryPort warehouseRepositoryPort(WarehouseRepository repository, WarehouseMapper warehouseMapper) {
        return new WarehouseRepositoryAdapter(repository, warehouseMapper);
    }

    @Bean
    public StockRepositoryPort stockRepositoryPort(DatabaseClient databaseClient) {
        return new StockRepositoryAdapter(databaseClient);
    }

    @Bean
    public StockMovementRepositoryPort stockMovementRepositoryPort(
            StockMovementRepository repository,
            StockMovementMapper stockMovementMapper,
            DatabaseClient databaseClient
    ) {
        return new StockMovementRepositoryAdapter(repository, stockMovementMapper, databaseClient);
    }

    /** Declared here so the transaction support doesn't depend on a Boot auto-configuration detail. */
    @Bean
    public TransactionalOperator transactionalOperator(ReactiveTransactionManager transactionManager) {
        return TransactionalOperator.create(transactionManager);
    }

    @Bean
    public TransactionPort transactionPort(TransactionalOperator transactionalOperator) {
        return new TransactionAdapter(transactionalOperator);
    }

    @Bean
    public CompanyUseCase companyUseCase(
            CompanyRepositoryPort companyRepositoryPort,
            CompanySettingsRepositoryPort companySettingsRepositoryPort,
            AuditRecorder auditRecorder
    ) {
        return new CompanyServiceImpl(companyRepositoryPort, companySettingsRepositoryPort, auditRecorder);
    }

    @Bean
    public CompanySettingsRepositoryPort companySettingsRepositoryPort(
            CompanySettingsRepository repository,
            CompanySettingsMapper companySettingsMapper
    ) {
        return new CompanySettingsRepositoryAdapter(repository, companySettingsMapper);
    }

    @Bean
    public ProductCategoryUseCase productCategoryUseCase(
            ProductCategoryRepositoryPort productCategoryRepositoryPort,
            ProductRepositoryPort productRepositoryPort,
            AuditRecorder auditRecorder
    ) {
        return new ProductCategoryServiceImpl(productCategoryRepositoryPort, productRepositoryPort, auditRecorder);
    }

    @Bean
    public ProductRepositoryPort productRepositoryPort(ProductRepository repository, ProductMapper productMapper) {
        return new ProductRepositoryAdapter(repository, productMapper);
    }

    @Bean
    public ProductCategoryRepositoryPort productCategoryRepositoryPort(
            ProductCategoryRepository repository,
            ProductCategoryMapper productCategoryMapper
    ) {
        return new ProductCategoryRepositoryAdapter(repository, productCategoryMapper);
    }

    @Bean
    public UserRepositoryPort userRepositoryPort(UserRepository repository, UserMapper userMapper,
                                                 DatabaseClient databaseClient) {
        return new UserRepositoryAdapter(repository, userMapper, databaseClient);
    }

    @Bean
    public CompanyRepositoryPort companyRepositoryPort(CompanyRepository repository, CompanyMapper companyMapper) {
        return new CompanyRepositoryAdapter(repository, companyMapper);
    }

    @Bean
    public RefreshTokenRepositoryPort refreshTokenRepositoryPort(
            RefreshTokenRepository repository,
            RefreshTokenMapper refreshTokenMapper
    ) {
        return new RefreshTokenRepositoryAdapter(repository, refreshTokenMapper);
    }
}
