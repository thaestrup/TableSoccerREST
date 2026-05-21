package com.foosball.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * JPA entity for the legacy {@code tbl_timer} table.
 *
 * <p>Single-row mailbox table: holds the timestamp of the last requested
 * timer start.
 */
@Entity
@Table(name = "tbl_timer")
public class TimerState extends PanacheEntityBase {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    public Integer id;

    @NotNull
    @Column(name = "lastRequestedTimerStart", nullable = false)
    public LocalDateTime lastRequestedTimerStart;
}
