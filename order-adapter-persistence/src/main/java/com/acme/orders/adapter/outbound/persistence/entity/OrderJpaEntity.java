package com.acme.orders.adapter.outbound.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The order's database row. A persistence detail, not the model.
 *
 * <p>This is the whole reason the aggregate has no JPA annotations on it: the table can grow columns,
 * change types and denormalise for query speed without any of that showing up in
 * {@link com.acme.orders.domain.model.order.Order}. It therefore has the things JPA needs and DDD
 * dislikes — a no-arg constructor, setters, mutable collections — and none of that escapes this
 * package.
 */
@Entity
@Table(name = "orders")
public class OrderJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "street", nullable = false, length = 255)
    private String street;

    @Column(name = "city", nullable = false, length = 128)
    private String city;

    @Column(name = "postal_code", nullable = false, length = 32)
    private String postalCode;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountAmount;

    /**
     * Denormalised so the list query can sort and filter on it without joining the lines. The
     * aggregate still derives the total from its lines; this column is written from that derivation
     * and read only by the query side.
     */
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "placed_at")
    private Instant placedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", length = 512)
    private String cancellationReason;

    @Column(name = "payment_reference", length = 128)
    private String paymentReference;

    @Column(name = "tracking_number", length = 128)
    private String trackingNumber;

    /**
     * Optimistic locking: two concurrent writers to the same order cannot both win.
     *
     * <p>Boxed rather than primitive so that Spring Data sees {@code null} on a never-persisted
     * entity and chooses {@code persist} over {@code merge} — with assigned UUID identifiers a
     * primitive {@code 0} would make every insert look like an update and cost a pointless SELECT.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private List<OrderLineJpaEntity> lines = new ArrayList<>();

    /**
     * Required by JPA, and by the mapper in the sibling package that builds these rows.
     *
     * <p>A public no-arg constructor would be a design smell on an aggregate; on a row mapping it is
     * harmless, because this class is the shape of a table and is never handed to a caller.
     */
    public OrderJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public void setPlacedAt(Instant placedAt) {
        this.placedAt = placedAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public Instant getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(Instant shippedAt) {
        this.shippedAt = shippedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public Long getVersion() {
        return version;
    }

    /** Current version, or 0 for an entity that has never been written. */
    public long versionOrZero() {
        return version == null ? 0L : version;
    }

    public List<OrderLineJpaEntity> getLines() {
        return lines;
    }

    public void setLines(List<OrderLineJpaEntity> lines) {
        this.lines = lines;
    }
}
