package kr.hhplus.be.server.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 상품 응답
@Schema(description = "상품 정보")
public class ProductResponse {
    
    @Schema(description = "상품 ID", example = "1")
    private Long id;
    
    @Schema(description = "상품명", example = "고성능 노트북")
    private String name;
    
    @Schema(description = "가격", example = "1500000.00")
    private BigDecimal price;
    
    @Schema(description = "재고 수량", example = "10")
    private Integer stockQuantity;
    
    @Schema(description = "생성 시간", example = "2025-07-16T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    // 기본 생성자
    public ProductResponse() {}
    
    public ProductResponse(Long id, String name, BigDecimal price, Integer stockQuantity, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public Integer getStockQuantity() {
        return stockQuantity;
    }
    
    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}