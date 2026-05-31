package com.foosball.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * One-to-one with {@link Player} via {@code player_id}. Kept in a
 * separate table so blob bytes don't ride along on every
 * {@code GET /players} query.
 */
@Entity
@Table(name = "tbl_player_photo")
public class PlayerPhoto extends PanacheEntityBase {

    @Id
    @Column(name = "player_id")
    public Long playerId;

    @NotNull
    @Column(name = "photo", columnDefinition = "MEDIUMBLOB", nullable = false)
    public byte[] photo;

    @NotNull
    @Size(max = 64)
    @Column(name = "photo_mime", length = 64, nullable = false)
    public String photoMime;
}
