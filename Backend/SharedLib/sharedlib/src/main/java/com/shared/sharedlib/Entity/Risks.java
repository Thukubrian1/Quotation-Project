package com.shared.sharedlib.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Table(schema = "risks", name = "risks")
public class Risks {

    @Id
    @Column(name = "risk_id")
    private BigDecimal riskId;

    @Column(name = "risk_name")
    private String riskName;

    @Column(name = "risk_description")
    private String riskDescription;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
