-- Initial schema: four tables with utf8mb4 charset. Seed data lives in
-- V3__seed_dev.sql.

-- ---------------------------------------------------------------------------
-- tbl_configuration
-- ---------------------------------------------------------------------------
CREATE TABLE tbl_configuration (
  name  VARCHAR(255) NOT NULL,
  value VARCHAR(255) NOT NULL,
  UNIQUE KEY uk_tbl_configuration_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- tbl_fights — one row per played game
-- ---------------------------------------------------------------------------
CREATE TABLE tbl_fights (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  player_red_1    VARCHAR(200) NOT NULL,
  player_red_2    VARCHAR(200) NOT NULL,
  player_blue_1   VARCHAR(200) NOT NULL,
  player_blue_2   VARCHAR(200) NOT NULL,
  `timestamp`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  match_winner    VARCHAR(20)  NOT NULL,
  points_at_stake INT(11)      NOT NULL,
  winning_table   INT(11)      NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- tbl_players — AUTO_INCREMENT seeded above the V3 dev seed's max id.
-- ---------------------------------------------------------------------------
CREATE TABLE tbl_players (
  id                BIGINT      NOT NULL AUTO_INCREMENT,
  name              VARCHAR(20) NOT NULL,
  playerReady       TINYINT(1)  NOT NULL DEFAULT 0,
  oprettet          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  registeredRFIDTag TEXT        NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tbl_players_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=87;

-- ---------------------------------------------------------------------------
-- tbl_timer
-- Same singleton pattern as tbl_latest_rfid_registration; promote unique id to PK.
-- ---------------------------------------------------------------------------
CREATE TABLE tbl_timer (
  id                      INT(11)   NOT NULL,
  lastRequestedTimerStart TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
