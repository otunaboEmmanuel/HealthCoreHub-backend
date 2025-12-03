package com.hc.onboardingservice.entity;
import com.hc.onboardingservice.resolver.FeaturesConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String description;
    private double price;
    private int durationDays;

    @JdbcTypeCode(SqlTypes.JSON)  // This tells Hibernate to use JSON type
    @Column(columnDefinition = "jsonb")
    private List<String> features;
    private LocalDateTime createdAt;
}

