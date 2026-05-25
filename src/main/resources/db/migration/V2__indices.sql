-- Indices supporting the period-filter and 4-way player-OR queries on
-- tbl_fights. Four single-column indices on the player slots let MariaDB
-- index-merge them.

CREATE INDEX idx_tbl_fights_timestamp    ON tbl_fights (`timestamp`);
CREATE INDEX idx_tbl_fights_player_red_1 ON tbl_fights (player_red_1);
CREATE INDEX idx_tbl_fights_player_red_2 ON tbl_fights (player_red_2);
CREATE INDEX idx_tbl_fights_player_blue_1 ON tbl_fights (player_blue_1);
CREATE INDEX idx_tbl_fights_player_blue_2 ON tbl_fights (player_blue_2);
