package com.foosball.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** JPA entity for {@code tbl_players}. */
@Entity
@Table(name = "tbl_players")
public class Player extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @NotNull
    @Size(max = 20)
    @Column(name = "name", length = 20, unique = true, nullable = false)
    public String name;

    @Column(name = "playerReady", nullable = false)
    public boolean playerReady;

    @Column(name = "oprettet", nullable = false)
    public LocalDateTime oprettet;

    @Column(name = "registeredRFIDTag", columnDefinition = "TEXT", nullable = false)
    public String registeredRFIDTag;
}
