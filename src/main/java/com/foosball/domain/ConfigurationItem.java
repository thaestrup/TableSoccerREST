package com.foosball.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * JPA entity for the legacy {@code tbl_configuration} table.
 *
 * <p>The legacy schema has no surrogate id; {@code name} is the unique
 * logical key so it acts as the JPA {@code @Id}.
 */
@Entity
@Table(name = "tbl_configuration")
public class ConfigurationItem extends PanacheEntityBase {

    @Id
    @NotNull
    @Size(max = 255)
    @Column(name = "name", length = 255, nullable = false, unique = true)
    public String name;

    @NotNull
    @Size(max = 255)
    @Column(name = "value", length = 255, nullable = false)
    public String value;
}
