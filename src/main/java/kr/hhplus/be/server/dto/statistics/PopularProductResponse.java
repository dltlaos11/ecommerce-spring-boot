package kr.hhplus.be.server.dto.statistics;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "인기 상품 정보")
public class PopularProductResponse {
    
    @Schema(description = "순위", example = "1")
    private Integer rank;
    
    @Schema(description = "상품 ID", example = "1")
    private Long productId;
    
    @Schema(description = "상품명", example = "고성능 노트북")
    private String productName;
    
    @Schema(description = "상품 가격", example = "1500000.00")
    private BigDecimal price;
    
    @Schema(description = "총 판매 수량", example = "150")
    private Integer totalSalesQuantity;
    
    @Schema(description = "총 판매 금액", example = "225000000.00")
    private BigDecimal totalSalesAmount;
    
    // 기본 생성자
    public PopularProductResponse() {}
    
    public PopularProductResponse(Integer rank, Long productId, String productName, BigDecimal price,
                                Integer totalSalesQuantity, BigDecimal totalSalesAmount) {
        this.rank = rank;
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.totalSalesQuantity = totalSalesQuantity;
        this.totalSalesAmount = totalSalesAmount;
    }
    
    // Getters and Setters
    public Integer getRank() {
        return rank;
    }
    
    public void setRank(Integer rank) {
        this.rank = rank;
    }
    
    public Long getProductId() {
        return productId;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public Integer getTotalSalesQuantity() {
        return totalSalesQuantity;
    }
    
    public void setTotalSalesQuantity(Integer totalSalesQuantity) {
        this.totalSalesQuantity = totalSalesQuantity;
    }
    
    public BigDecimal getTotalSalesAmount() {
        return totalSalesAmount;
    }
    
    public void setTotalSalesAmount(BigDecimal totalSalesAmount) {
        this.totalSalesAmount = totalSalesAmount;
    }
}