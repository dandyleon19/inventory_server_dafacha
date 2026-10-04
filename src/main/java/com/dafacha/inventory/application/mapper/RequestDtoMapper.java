package com.dafacha.inventory.application.mapper;

import com.dafacha.inventory.application.dto.request.CreateCompanyRequest;
import com.dafacha.inventory.application.dto.request.CreateUserRequest;
import com.dafacha.inventory.application.dto.request.ProductCategoryRequest;
import com.dafacha.inventory.application.dto.request.ProductRequest;
import com.dafacha.inventory.application.dto.request.LayerPickRequest;
import com.dafacha.inventory.application.dto.request.LotCountRequest;
import com.dafacha.inventory.application.dto.request.LotPickRequest;
import com.dafacha.inventory.application.dto.request.NewLotRequest;
import com.dafacha.inventory.application.dto.request.PurchaseOrderRequest;
import com.dafacha.inventory.application.dto.request.ReceivePurchaseOrderRequest;
import com.dafacha.inventory.application.dto.request.SaveUserRequest;
import com.dafacha.inventory.application.dto.request.StockAdjustmentRequest;
import com.dafacha.inventory.application.dto.request.StockMovementRequest;
import com.dafacha.inventory.application.dto.request.StockTransferRequest;
import com.dafacha.inventory.application.dto.request.SupplierRequest;
import com.dafacha.inventory.application.dto.request.UpdateCompanyRequest;
import com.dafacha.inventory.application.dto.request.WarehouseRequest;
import com.dafacha.inventory.application.dto.request.UpdateCompanySettingsRequest;
import com.dafacha.inventory.application.support.SearchSupport;
import com.dafacha.inventory.domain.model.Company;
import com.dafacha.inventory.domain.model.LayerPick;
import com.dafacha.inventory.domain.model.LotCount;
import com.dafacha.inventory.domain.model.LotPick;
import com.dafacha.inventory.domain.model.NewLot;
import com.dafacha.inventory.domain.model.CompanySettings;
import com.dafacha.inventory.domain.model.Product;
import com.dafacha.inventory.domain.model.ProductCategory;
import com.dafacha.inventory.domain.model.PurchaseOrder;
import com.dafacha.inventory.domain.model.PurchaseOrderLine;
import com.dafacha.inventory.domain.model.ReceiptItem;
import com.dafacha.inventory.domain.model.StockOperation;
import com.dafacha.inventory.domain.model.Supplier;
import com.dafacha.inventory.domain.model.User;
import com.dafacha.inventory.domain.model.Warehouse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RequestDtoMapper {

    public User toUser(CreateUserRequest request) {
        return User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(request.getPassword())
                .role(request.getRole())
                .companyId(request.getCompanyId())
                .build();
    }

    public User toUser(SaveUserRequest request) {
        return User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(request.getPassword())
                .role(request.getRole())
                .isActive(request.getIsActive())
                .warehouseIds(request.getWarehouseIds())
                .build();
    }

    public Supplier toSupplier(SupplierRequest request) {
        return Supplier.builder()
                .name(request.getName())
                .rucNumber(request.getRucNumber())
                .contactName(request.getContactName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .notes(request.getNotes())
                .isActive(request.getIsActive())
                .build();
    }

    public PurchaseOrder toPurchaseOrder(PurchaseOrderRequest request) {
        List<PurchaseOrderLine> lines = request.getLines().stream()
                .map(line -> PurchaseOrderLine.builder()
                        .productId(line.getProductId())
                        .quantity(line.getQuantity())
                        .unitCost(line.getUnitCost())
                        .packaging(SearchSupport.blankToNull(line.getPackaging()))
                        .entryInput(SearchSupport.blankToNull(line.getEntryInput()))
                        .build())
                .toList();

        return PurchaseOrder.builder()
                .supplierId(request.getSupplierId())
                .warehouseId(request.getWarehouseId())
                .expectedDate(request.getExpectedDate())
                .notes(request.getNotes())
                .lines(lines)
                .build();
    }

    public List<ReceiptItem> toReceiptItems(ReceivePurchaseOrderRequest request) {
        return request.getItems().stream()
                .map(item -> ReceiptItem.builder()
                        .lineId(item.getLineId())
                        .quantity(item.getQuantity())
                        .expiryDate(item.getExpiryDate())
                        .lotNumber(item.getLotNumber())
                        .build())
                .toList();
    }

    public Warehouse toWarehouse(WarehouseRequest request) {
        return Warehouse.builder()
                .name(request.getName())
                .address(request.getAddress())
                .isActive(request.getIsActive())
                .build();
    }

    public StockOperation toStockOperation(StockMovementRequest request) {
        return StockOperation.builder()
                .warehouseId(request.getWarehouseId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .expiryDate(request.getExpiryDate())
                .lotNumber(request.getLotNumber())
                .notes(request.getNotes())
                .lots(toLotPicks(request.getLots()))
                .unitCost(request.getUnitCost())
                .layerPicks(toLayerPicks(request.getLayers()))
                .build();
    }

    private List<LayerPick> toLayerPicks(List<LayerPickRequest> picks) {
        if (picks == null) {
            return null;
        }
        return picks.stream().map(pick -> new LayerPick(pick.getLayerId(), pick.getQuantity())).toList();
    }

    private List<LotPick> toLotPicks(List<LotPickRequest> picks) {
        if (picks == null) {
            return null;
        }
        return picks.stream().map(pick -> new LotPick(pick.getLotId(), pick.getQuantity())).toList();
    }

    public StockOperation toStockOperation(StockAdjustmentRequest request) {
        return StockOperation.builder()
                .warehouseId(request.getWarehouseId())
                .productId(request.getProductId())
                .countedQuantity(request.getCountedQuantity())
                .expiryDate(request.getExpiryDate())
                .lotNumber(request.getLotNumber())
                .notes(request.getNotes())
                .lotCounts(request.getLotCounts() == null ? null : request.getLotCounts().stream()
                        .map((LotCountRequest count) -> new LotCount(count.getLotId(), count.getCountedQuantity()))
                        .toList())
                .newLots(request.getNewLots() == null ? null : request.getNewLots().stream()
                        .map((NewLotRequest lot) -> new NewLot(lot.getExpiryDate(), lot.getLotNumber(), lot.getQuantity()))
                        .toList())
                .build();
    }

    public StockOperation toStockOperation(StockTransferRequest request) {
        return StockOperation.builder()
                .warehouseId(request.getFromWarehouseId())
                .toWarehouseId(request.getToWarehouseId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .notes(request.getNotes())
                .lots(toLotPicks(request.getLots()))
                .layerPicks(toLayerPicks(request.getLayers()))
                .build();
    }

    public Company toCompany(UpdateCompanyRequest request) {
        return Company.builder()
                .name(request.getName())
                .socialReason(request.getSocialReason())
                .fiscalAddress(request.getFiscalAddress())
                .rucNumber(request.getRucNumber())
                .phone(request.getPhone())
                .build();
    }

    public CompanySettings toCompanySettings(UpdateCompanySettingsRequest request) {
        return CompanySettings.builder()
                .requireProductSku(request.getRequireProductSku())
                .requireProductCategory(request.getRequireProductCategory())
                .requireProductBarcode(request.getRequireProductBarcode())
                .allowNegativeStock(request.getAllowNegativeStock())
                .expiryAlertDays(request.getExpiryAlertDays())
                .purchaseArrivalAlertDays(request.getPurchaseArrivalAlertDays())
                .costMethod(request.getCostMethod())
                .build();
    }

    public Product toProduct(ProductRequest request) {
        return Product.builder()
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .description(request.getDescription())
                .sku(request.getSku())
                .barcode(request.getBarcode())
                .unit(request.getUnit())
                .costPrice(request.getCostPrice())
                .salePrice(request.getSalePrice())
                .minStock(request.getMinStock())
                .tracksExpiry(request.getTracksExpiry())
                .isActive(request.getIsActive())
                .build();
    }

    public ProductCategory toProductCategory(ProductCategoryRequest request) {
        return ProductCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    public Company toCompany(CreateCompanyRequest request) {
        return Company.builder()
                .name(request.getName())
                .socialReason(request.getSocialReason())
                .fiscalAddress(request.getFiscalAddress())
                .rucNumber(request.getRucNumber())
                .phone(request.getPhone())
                .build();
    }
}
