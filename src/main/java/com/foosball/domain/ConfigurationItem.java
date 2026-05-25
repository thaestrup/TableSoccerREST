package com.foosball.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** JPA entity for {@code tbl_configuration}. {@code name} is the primary key. */
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
