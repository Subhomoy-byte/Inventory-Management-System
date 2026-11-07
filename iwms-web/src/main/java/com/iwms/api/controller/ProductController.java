package com.iwms.api.controller;

import com.iwms.api.dto.ProductDto;
import com.iwms.api.filter.CustomAuthFilter;
import com.iwms.model.Permission;
import com.iwms.model.Product;
import com.iwms.model.User;
import com.iwms.repository.Repository;
import com.iwms.service.InventoryService;
import com.iwms.storage.AuditLogger;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final InventoryService inventoryService;
    private final Repository<Product, String> productRepository;
    private final AuditLogger auditLogger;

    public ProductController(InventoryService inventoryService,
                             Repository<Product, String> productRepository,
                             AuditLogger auditLogger) {
        this.inventoryService = inventoryService;
        this.productRepository = productRepository;
        this.auditLogger = auditLogger;
    }

    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts(HttpServletRequest request) {
        User user = (User) request.getAttribute(CustomAuthFilter.AUTH_USER_ATTR);
        boolean canViewCosts = user != null && user.hasPermission(Permission.VIEW_COST_PRICES);

        List<ProductDto> dtoList = inventoryService.getAllProducts().stream()
                .map(p -> toDto(p, canViewCosts))
                .toList();

        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/{sku}")
    public ResponseEntity<?> getProductBySku(@PathVariable String sku, HttpServletRequest request) {
        User user = (User) request.getAttribute(CustomAuthFilter.AUTH_USER_ATTR);
        boolean canViewCosts = user != null && user.hasPermission(Permission.VIEW_COST_PRICES);

        Product p = inventoryService.getProduct(sku);
        if (p == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Product not found: " + sku));
        }

        inventoryService.recordProductView(sku);
        return ResponseEntity.ok(toDto(p, canViewCosts));
    }

    @DeleteMapping("/{sku}")
    public ResponseEntity<?> deleteProduct(@PathVariable String sku, HttpServletRequest request) throws Exception {
        User user = (User) request.getAttribute(CustomAuthFilter.AUTH_USER_ATTR);
        if (user == null || !user.hasPermission(Permission.DELETE_PRODUCTS)) {
            auditLogger.log(user != null ? user.getUsername() : "anonymous", "DELETE_PRODUCT", sku, "FORBIDDEN", "Insufficient permissions");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Permission denied: DELETE_PRODUCTS required"));
        }

        productRepository.deleteById(sku);
        auditLogger.log(user.getUsername(), "DELETE_PRODUCT", sku, "SUCCESS", "Product deleted by " + user.getUsername());
        return ResponseEntity.ok(Map.of("message", "Product deleted successfully: " + sku));
    }

    private ProductDto toDto(Product p, boolean canViewCosts) {
        int currentStock = inventoryService.getCurrentStock(p.getSku());
        Double costPrice = canViewCosts ? p.getCostPrice() : null;
        return new ProductDto(
                p.getSku(),
                p.getName(),
                p.getCategory().name(),
                p.getUnitPrice(),
                costPrice,
                p.getMinStockLevel(),
                currentStock,
                p.calculateStorageCost(),
                p.getHandlingRules()
        );
    }
}
