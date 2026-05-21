-- V2__indices.sql
-- Indices the legacy schema was missing. Code review flagged each of these
-- as causing full table scans on tbl_fights.
--
-- Rationale per index:
--   - idx_tbl_fights_timestamp:
--       Every period filter query in the legacy code is shaped like
--       `WHERE timestamp > DATE_SUB(NOW(), INTERVAL ? HOUR)`. Without an
--       index, that scans the whole tbl_fights table.
--   - idx_tbl_fights_player_red_1 / _red_2 / _blue_1 / _blue_2:
--       Games.getGamesForName and MoreUtil.getNewestMatchForPlayer issue
--       4-way OR predicates over the four player columns. MariaDB's
--       optimizer can index-merge four single-column indices, so we keep
--       them simple rather than a single composite.

CREATE INDEX idx_tbl_fights_timestamp    ON tbl_fights (`timestamp`);
CREATE INDEX idx_tbl_fights_player_red_1 ON tbl_fights (player_red_1);
CREATE INDEX idx_tbl_fights_player_red_2 ON tbl_fights (player_red_2);
CREATE INDEX idx_tbl_fights_player_blue_1 ON tbl_fights (player_blue_1);
CREATE INDEX idx_tbl_fights_player_blue_2 ON tbl_fights (player_blue_2);
