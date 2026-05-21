-- V1__init_schema.sql
-- Initial schema port from legacy TableSoccerREST (db-init/01-schema.sql).
-- Sources:
--   - phpMyAdmin dump:        TableSoccerREST/db-init/01-schema.sql
--   - Runtime CREATE TABLE IF NOT EXISTS in MoreUtil.groovy lines 39-52
--     (tbl_configuration, tbl_timer) -- now owned by Flyway.
--
-- Deviations from the legacy dump:
--   1. Charset bumped from latin1 to utf8mb4 / utf8mb4_unicode_ci.
--      Legacy latin1 mojibakes Danish names (Søren, Müller, ...).
--   2. The legacy column tbl_fights.points_at_steake (sic) is renamed to
--      points_at_stake. The Java entity uses the corrected name; see
--      legacy Model/Game.groovy:49 for the original typo.
--   3. Inline indices/PKs/AUTO_INCREMENT instead of trailing ALTER TABLEs.
--   4. Seed data lives in V3__seed_dev.sql, not here.
--   5. tbl_latest_rfid_registration is intentionally NOT created. The
--      RFID/Registration feature is dropped per FRONTEND-USAGE.md
--      (frontend never calls /registration). tbl_players.registeredRFIDTag
--      is preserved because the frontend round-trips it on PUT.

-- ---------------------------------------------------------------------------
-- tbl_configuration
-- ---------------------------------------------------------------------------
CREATE TABLE tbl_configuration (
  name  VARCHAR(255) NOT NULL,
  value VARCHAR(255) NOT NULL,
  UNIQUE KEY uk_tbl_configuration_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- tbl_fights
-- NOTE: legacy column `points_at_steake` (typo) is corrected to `points_at_stake`.
-- ---------------------------------------------------------------------------
CREATE TABLE tbl_fights (
  id              BIGINT       NOT NULL AUTO_INCREMENT, -- widened from legacy INT(11) to match entity Long
  player_red_1    VARCHAR(200) NOT NULL,
  player_red_2    VARCHAR(200) NOT NULL,
  player_blue_1   VARCHAR(200) NOT NULL,
  player_blue_2   VARCHAR(200) NOT NULL,
  `timestamp`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  match_winner    VARCHAR(20)  NOT NULL,
  points_at_stake INT(11)      NOT NULL, -- legacy column was misspelled `points_at_steake`
  winning_table   INT(11)      NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- tbl_players
-- AUTO_INCREMENT starts at 87 to match the legacy dump's high-water mark
-- (relevant only when V3 seed data is applied).
-- ---------------------------------------------------------------------------
CREATE TABLE tbl_players (
  id                BIGINT      NOT NULL AUTO_INCREMENT, -- widened from legacy INT(11) to match entity Long
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
