# Exemple concret : Architecture Hexagonale Pure

Ce document présente un exemple complet d'implémentation de l'architecture hexagonale pure, avec séparation totale entre le domain et l'infrastructure.

## Cas d'usage : Gestion de produits avec règles métier complexes

### Règles métier

1. Un produit doit avoir un prix positif
2. Une réduction ne peut pas dépasser 70%
3. Un produit en rupture de stock ne peut pas être activé
4. Le prix final ne peut jamais être négatif
5. Un produit avec des commandes actives ne peut pas être supprimé

Ces règles métier complexes justifient une séparation complète pour faciliter les tests unitaires sans framework.

## Structure des fichiers

```
backend/src/main/java/com/enterprise/app/
├── domain/
│   ├── model/
│   │   ├── Product.java              # Domain pur
│   │   ├── ProductCategory.java      # Value Object
│   │   └── Money.java                # Value Object
│   ├── port/
│   │   ├── in/
│   │   │   ├── CreateProductUseCase.java
│   │   │   └── UpdateProductPriceUseCase.java
│   │   └── out/
│   │       └── ProductRepository.java
│   ├── service/
│   │   └── ProductDomainService.java
│   └── exception/
│       ├── ProductBusinessException.java
│       └── InsufficientStockException.java
│
├── application/
│   ├── service/
│   │   └── ProductApplicationService.java
│   └── dto/
│       ├── CreateProductCommand.java
│       └── ProductResponse.java
│
├── infrastructure/
│   ├── persistence/
│   │   ├── entity/
│   │   │   └── ProductEntity.java
│   │   ├── jpa/
│   │   │   └── JpaProductRepository.java
│   │   ├── mapper/
│   │   │   └── ProductMapper.java
│   │   └── adapter/
│   │       └── ProductRepositoryAdapter.java
│   └── config/
│       └── PersistenceConfig.java
│
└── presentation/
    └── controller/
        └── ProductController.java
```

## Implémentation complète

### 1. Domain Layer (Pur Java - ZÉRO dépendance)

#### 1.1 Value Objects

```java
// domain/model/Money.java
package com.enterprise.app.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value Object representing money with currency.
 * Immutable and self-validating.
 */
public final class Money {
    private final BigDecimal amount;
    private final Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency;
    }

    public Money(BigDecimal amount, String currencyCode) {
        this(amount, Currency.getInstance(currencyCode));
    }

    public static Money euro(BigDecimal amount) {
        return new Money(amount, "EUR");
    }

    public static Money euro(double amount) {
        return euro(BigDecimal.valueOf(amount));
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot subtract different currencies");
        }
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier), this.currency);
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money)) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0 && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return amount + " " + currency.getCurrencyCode();
    }
}
```

```java
// domain/model/ProductCategory.java
package com.enterprise.app.domain.model;

/**
 * Product category enum - part of domain.
 */
public enum ProductCategory {
    ELECTRONICS,
    CLOTHING,
    FOOD,
    BOOKS,
    HOME,
    SPORTS
}
```

#### 1.2 Domain Model

```java
// domain/model/Product.java
package com.enterprise.app.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pure domain model - NO framework dependencies.
 * Contains ALL business logic and invariants.
 */
public class Product {
    private static final BigDecimal MAX_DISCOUNT_PERCENTAGE = new BigDecimal("70.00");
    private static final int MIN_STOCK_FOR_ACTIVATION = 1;

    private UUID id;
    private String name;
    private String description;
    private Money price;
    private BigDecimal discountPercentage;
    private int stockQuantity;
    private ProductCategory category;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long version;

    // Private constructor to enforce creation through factory methods
    private Product() {
        this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.discountPercentage = BigDecimal.ZERO;
        this.active = false;
        this.version = 0L;
    }

    /**
     * Factory method to create a new product.
     * Enforces business rules at creation.
     */
    public static Product create(String name, String description, Money price,
                                  ProductCategory category, int initialStock) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.category = category;
        product.setStockQuantity(initialStock);
        return product;
    }

    /**
     * Business rule: Update price with validation.
     */
    public void updatePrice(Money newPrice) {
        if (newPrice == null) {
            throw new IllegalArgumentException("Price cannot be null");
        }
        if (!newPrice.isPositive()) {
            throw new ProductBusinessException("Price must be positive");
        }
        this.price = newPrice;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Business rule: Apply discount with maximum limit.
     */
    public void applyDiscount(BigDecimal discountPercentage) {
        if (discountPercentage == null) {
            throw new IllegalArgumentException("Discount percentage cannot be null");
        }
        if (discountPercentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new ProductBusinessException("Discount percentage cannot be negative");
        }
        if (discountPercentage.compareTo(MAX_DISCOUNT_PERCENTAGE) > 0) {
            throw new ProductBusinessException(
                "Discount percentage cannot exceed " + MAX_DISCOUNT_PERCENTAGE + "%"
            );
        }
        this.discountPercentage = discountPercentage;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Business rule: Remove discount.
     */
    public void removeDiscount() {
        this.discountPercentage = BigDecimal.ZERO;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Calculate final price after discount.
     */
    public Money getFinalPrice() {
        if (discountPercentage.compareTo(BigDecimal.ZERO) == 0) {
            return price;
        }
        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
            discountPercentage.divide(new BigDecimal("100"), 4, BigDecimal.ROUND_HALF_UP)
        );
        Money finalPrice = price.multiply(discountMultiplier);

        if (finalPrice.isNegative()) {
            throw new ProductBusinessException("Final price cannot be negative");
        }
        return finalPrice;
    }

    /**
     * Business rule: Activate product only if stock available.
     */
    public void activate() {
        if (this.active) {
            throw new ProductBusinessException("Product is already active");
        }
        if (stockQuantity < MIN_STOCK_FOR_ACTIVATION) {
            throw new InsufficientStockException(
                "Cannot activate product with insufficient stock. Minimum: " + MIN_STOCK_FOR_ACTIVATION
            );
        }
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Business rule: Deactivate product.
     */
    public void deactivate() {
        if (!this.active) {
            throw new ProductBusinessException("Product is already inactive");
        }
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Business rule: Update stock quantity.
     */
    public void updateStock(int quantity) {
        if (quantity < 0) {
            throw new ProductBusinessException("Stock quantity cannot be negative");
        }
        this.stockQuantity = quantity;
        this.updatedAt = LocalDateTime.now();

        // Auto-deactivate if stock runs out
        if (this.stockQuantity == 0 && this.active) {
            this.deactivate();
        }
    }

    /**
     * Business rule: Decrease stock (e.g., when order is placed).
     */
    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (quantity > this.stockQuantity) {
            throw new InsufficientStockException(
                "Insufficient stock. Available: " + this.stockQuantity + ", Requested: " + quantity
            );
        }
        updateStock(this.stockQuantity - quantity);
    }

    /**
     * Business rule: Increase stock (e.g., when restocked).
     */
    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        updateStock(this.stockQuantity + quantity);
    }

    /**
     * Check if product is available for purchase.
     */
    public boolean isAvailableForPurchase() {
        return active && stockQuantity > 0;
    }

    /**
     * Check if product is out of stock.
     */
    public boolean isOutOfStock() {
        return stockQuantity == 0;
    }

    // Getters
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Money getPrice() { return price; }
    public BigDecimal getDiscountPercentage() { return discountPercentage; }
    public int getStockQuantity() { return stockQuantity; }
    public ProductCategory getCategory() { return category; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    // Package-private setters for infrastructure (reconstitution from DB)
    void setId(UUID id) { this.id = id; }
    void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    void setVersion(long version) { this.version = version; }

    private void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }
        if (name.length() > 200) {
            throw new IllegalArgumentException("Product name cannot exceed 200 characters");
        }
        this.name = name.trim();
    }

    private void setDescription(String description) {
        this.description = description != null ? description.trim() : null;
    }

    private void setPrice(Money price) {
        if (price == null) {
            throw new IllegalArgumentException("Price cannot be null");
        }
        if (!price.isPositive()) {
            throw new ProductBusinessException("Price must be positive");
        }
        this.price = price;
    }

    private void setStockQuantity(int stockQuantity) {
        if (stockQuantity < 0) {
            throw new ProductBusinessException("Stock quantity cannot be negative");
        }
        this.stockQuantity = stockQuantity;
    }
}
```

#### 1.3 Domain Exceptions

```java
// domain/exception/ProductBusinessException.java
package com.enterprise.app.domain.exception;

/**
 * Base exception for product business rule violations.
 */
public class ProductBusinessException extends RuntimeException {
    public ProductBusinessException(String message) {
        super(message);
    }

    public ProductBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

```java
// domain/exception/InsufficientStockException.java
package com.enterprise.app.domain.exception;

/**
 * Thrown when stock is insufficient for an operation.
 */
public class InsufficientStockException extends ProductBusinessException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
```

#### 1.4 Domain Ports

```java
// domain/port/out/ProductRepository.java
package com.enterprise.app.domain.port.out;

import com.enterprise.app.domain.model.Product;
import com.enterprise.app.domain.model.ProductCategory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for product persistence.
 * Defined in domain, implemented in infrastructure.
 */
public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(UUID id);
    List<Product> findByCategory(ProductCategory category);
    List<Product> findActiveProducts();
    void deleteById(UUID id);
    boolean existsById(UUID id);
}
```

```java
// domain/port/in/CreateProductUseCase.java
package com.enterprise.app.domain.port.in;

import com.enterprise.app.domain.model.Product;
import com.enterprise.app.domain.model.ProductCategory;
import com.enterprise.app.domain.model.Money;

/**
 * Input port for creating products.
 */
public interface CreateProductUseCase {
    Product createProduct(String name, String description, Money price,
                          ProductCategory category, int initialStock);
}
```

### 2. Infrastructure Layer (JPA + Spring)

#### 2.1 JPA Entity

```java
// infrastructure/persistence/entity/ProductEntity.java
package com.enterprise.app.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for product persistence.
 * Contains ONLY persistence annotations, NO business logic.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal priceAmount;

    @Column(nullable = false, length = 3)
    private String priceCurrency;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}
```

#### 2.2 Spring Data JPA Repository

```java
// infrastructure/persistence/jpa/JpaProductRepository.java
package com.enterprise.app.infrastructure.persistence.jpa;

import com.enterprise.app.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository - infrastructure concern.
 */
public interface JpaProductRepository extends JpaRepository<ProductEntity, UUID> {

    List<ProductEntity> findByCategory(String category);

    @Query("SELECT p FROM ProductEntity p WHERE p.active = true")
    List<ProductEntity> findActiveProducts();
}
```

#### 2.3 Mapper

```java
// infrastructure/persistence/mapper/ProductMapper.java
package com.enterprise.app.infrastructure.persistence.mapper;

import com.enterprise.app.domain.model.Money;
import com.enterprise.app.domain.model.Product;
import com.enterprise.app.domain.model.ProductCategory;
import com.enterprise.app.infrastructure.persistence.entity.ProductEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Maps between pure domain objects and JPA entities.
 */
@Component
public class ProductMapper {

    public ProductEntity toEntity(Product domain) {
        return ProductEntity.builder()
            .id(domain.getId())
            .name(domain.getName())
            .description(domain.getDescription())
            .priceAmount(domain.getPrice().getAmount())
            .priceCurrency(domain.getPrice().getCurrency().getCurrencyCode())
            .discountPercentage(domain.getDiscountPercentage())
            .stockQuantity(domain.getStockQuantity())
            .category(domain.getCategory().name())
            .active(domain.isActive())
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .version(domain.getVersion())
            .build();
    }

    public Product toDomain(ProductEntity entity) {
        Money price = new Money(
            entity.getPriceAmount(),
            Currency.getInstance(entity.getPriceCurrency())
        );

        Product product = Product.create(
            entity.getName(),
            entity.getDescription(),
            price,
            ProductCategory.valueOf(entity.getCategory()),
            entity.getStockQuantity()
        );

        // Reconstitute state using package-private setters
        product.setId(entity.getId());
        product.setCreatedAt(entity.getCreatedAt());
        product.setVersion(entity.getVersion());

        // Apply discount if any
        if (entity.getDiscountPercentage().compareTo(BigDecimal.ZERO) > 0) {
            product.applyDiscount(entity.getDiscountPercentage());
        }

        // Activate if needed
        if (entity.getActive()) {
            product.activate();
        }

        return product;
    }
}
```

#### 2.4 Repository Adapter

```java
// infrastructure/persistence/adapter/ProductRepositoryAdapter.java
package com.enterprise.app.infrastructure.persistence.adapter;

import com.enterprise.app.domain.model.Product;
import com.enterprise.app.domain.model.ProductCategory;
import com.enterprise.app.domain.port.out.ProductRepository;
import com.enterprise.app.infrastructure.persistence.entity.ProductEntity;
import com.enterprise.app.infrastructure.persistence.jpa.JpaProductRepository;
import com.enterprise.app.infrastructure.persistence.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapts Spring Data JPA repository to domain repository port.
 * Implements the interface defined in the domain.
 */
@Repository
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepository {

    private final JpaProductRepository jpaRepository;
    private final ProductMapper mapper;

    @Override
    public Product save(Product product) {
        ProductEntity entity = mapper.toEntity(product);
        ProductEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public List<Product> findByCategory(ProductCategory category) {
        return jpaRepository.findByCategory(category.name())
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Product> findActiveProducts() {
        return jpaRepository.findActiveProducts()
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }
}
```

### 3. Tests unitaires du domain (SANS Spring)

```java
// domain/model/ProductTest.java
package com.enterprise.app.domain.model;

import com.enterprise.app.domain.exception.InsufficientStockException;
import com.enterprise.app.domain.exception.ProductBusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests - NO Spring context needed!
 * Tests run in milliseconds.
 */
class ProductTest {

    @Test
    void should_create_product_with_valid_data() {
        // Given
        Money price = Money.euro(99.99);

        // When
        Product product = Product.create("Laptop", "Gaming laptop", price,
            ProductCategory.ELECTRONICS, 10);

        // Then
        assertNotNull(product.getId());
        assertEquals("Laptop", product.getName());
        assertEquals(price, product.getPrice());
        assertEquals(10, product.getStockQuantity());
        assertFalse(product.isActive()); // Starts inactive
    }

    @Test
    void should_throw_exception_when_price_is_negative() {
        // Given
        Money negativePrice = Money.euro(-10.00);

        // When & Then
        assertThrows(ProductBusinessException.class, () ->
            Product.create("Product", "Description", negativePrice,
                ProductCategory.ELECTRONICS, 5)
        );
    }

    @Test
    void should_apply_discount_within_limit() {
        // Given
        Product product = Product.create("Product", "Desc", Money.euro(100),
            ProductCategory.ELECTRONICS, 10);

        // When
        product.applyDiscount(new BigDecimal("20.00")); // 20% discount

        // Then
        assertEquals(new BigDecimal("20.00"), product.getDiscountPercentage());
        assertEquals(Money.euro(80.00), product.getFinalPrice());
    }

    @Test
    void should_throw_exception_when_discount_exceeds_limit() {
        // Given
        Product product = Product.create("Product", "Desc", Money.euro(100),
            ProductCategory.ELECTRONICS, 10);

        // When & Then
        assertThrows(ProductBusinessException.class, () ->
            product.applyDiscount(new BigDecimal("80.00")) // 80% > 70% max
        );
    }

    @Test
    void should_activate_product_with_sufficient_stock() {
        // Given
        Product product = Product.create("Product", "Desc", Money.euro(100),
            ProductCategory.ELECTRONICS, 5);

        // When
        product.activate();

        // Then
        assertTrue(product.isActive());
        assertTrue(product.isAvailableForPurchase());
    }

    @Test
    void should_throw_exception_when_activating_without_stock() {
        // Given
        Product product = Product.create("Product", "Desc", Money.euro(100),
            ProductCategory.ELECTRONICS, 0);

        // When & Then
        assertThrows(InsufficientStockException.class, product::activate);
    }

    @Test
    void should_decrease_stock_correctly() {
        // Given
        Product product = Product.create("Product", "Desc", Money.euro(100),
            ProductCategory.ELECTRONICS, 10);
        product.activate();

        // When
        product.decreaseStock(3);

        // Then
        assertEquals(7, product.getStockQuantity());
        assertTrue(product.isActive());
    }

    @Test
    void should_auto_deactivate_when_stock_reaches_zero() {
        // Given
        Product product = Product.create("Product", "Desc", Money.euro(100),
            ProductCategory.ELECTRONICS, 1);
        product.activate();

        // When
        product.decreaseStock(1);

        // Then
        assertEquals(0, product.getStockQuantity());
        assertFalse(product.isActive()); // Auto-deactivated
        assertFalse(product.isAvailableForPurchase());
    }

    @Test
    void should_throw_exception_when_decreasing_more_than_available() {
        // Given
        Product product = Product.create("Product", "Desc", Money.euro(100),
            ProductCategory.ELECTRONICS, 5);

        // When & Then
        assertThrows(InsufficientStockException.class, () ->
            product.decreaseStock(10) // Requesting more than available
        );
    }
}
```

## Comparaison : Avant vs Après

### Avant (Annotations JPA dans domain)

```java
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private BigDecimal price;

    // Business logic mixed with persistence
    public void applyDiscount(BigDecimal discount) {
        this.price = price.multiply(BigDecimal.ONE.subtract(discount));
    }
}
```

**Tests :**
```java
@SpringBootTest // ❌ Requires full Spring context
class ProductTest {
    @Autowired
    private ProductRepository repository;

    @Test
    void test() { // Slow (500ms+)
        // ...
    }
}
```

### Après (Séparation complète)

```java
// Domain pur
public class Product {
    private UUID id;
    private Money price; // Value Object

    // Pure business logic, NO annotations
    public void applyDiscount(BigDecimal discount) {
        if (discount.compareTo(MAX_DISCOUNT) > 0) {
            throw new ProductBusinessException("Discount too high");
        }
        this.price = price.multiply(BigDecimal.ONE.subtract(discount));
    }
}
```

**Tests :**
```java
// NO Spring annotations! ✅
class ProductTest {
    @Test
    void test() { // Fast (<10ms)
        Product product = Product.create(...);
        product.applyDiscount(new BigDecimal("0.2"));
        assertEquals(expected, product.getFinalPrice());
    }
}
```

## Avantages mesurables

| Critère | Avec JPA dans domain | Séparation complète |
|---------|---------------------|---------------------|
| Temps de tests domain | 500-2000ms | 5-20ms |
| Dépendances pour tester | Spring Boot + H2 | Aucune (Java pur) |
| Complexité cyclomatique | Élevée (mélange concerns) | Faible (séparation) |
| Changement de DB | Difficile | Facile |
| Testabilité | Moyenne (integration tests) | Excellente (unit tests) |
| Courbe d'apprentissage | Facile | Moyenne |

## Conclusion

Cette approche est recommandée pour :
- ✅ Domain avec logique métier complexe
- ✅ Règles métier critiques nécessitant des tests exhaustifs
- ✅ Applications à longue durée de vie
- ✅ Équipes expérimentées en DDD

L'investissement initial en code (mappers, séparation) est compensé par :
- Tests ultra-rapides du domain
- Facilité de maintenance
- Indépendance technologique
- Qualité et fiabilité du code métier
