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

/**
 * JPA entity for the legacy {@code tbl_fights} table.
 *
 * <p>Note: the entity is named {@code Game} but the underlying table is
 * historically {@code tbl_fights}. The legacy column {@code points_at_steake}
 * (typo) is renamed to {@code points_at_stake} by the Flyway migration.
 */
@Entity
@Table(name = "tbl_fights")
public class Game extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @NotNull
    @Size(max = 200)
    @Column(name = "player_red_1", length = 200, nullable = false)
    public String playerRed1;

    @NotNull
    @Size(max = 200)
    @Column(name = "player_red_2", length = 200, nullable = false)
    public String playerRed2;

    @NotNull
    @Size(max = 200)
    @Column(name = "player_blue_1", length = 200, nullable = false)
    public String playerBlue1;

    @NotNull
    @Size(max = 200)
    @Column(name = "player_blue_2", length = 200, nullable = false)
    public String playerBlue2;

    @Column(name = "timestamp", nullable = false)
    public LocalDateTime timestamp;

    @Size(max = 20)
    @Column(name = "match_winner", length = 20, nullable = false)
    public String matchWinner;

    @Column(name = "points_at_stake", nullable = false)
    public int pointsAtStake;

    @Column(name = "winning_table", nullable = false)
    public int winningTable;
}
