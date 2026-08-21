package com.inventory.sync.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String name;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = true)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    public Product(String sku, String name, BigDecimal price) {
        this.sku = sku;
        this.name = name;
        this.price = price;
    }
}