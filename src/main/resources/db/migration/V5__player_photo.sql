CREATE TABLE tbl_player_photo (
  player_id  BIGINT       NOT NULL,
  photo      MEDIUMBLOB   NOT NULL,
  photo_mime VARCHAR(64)  NOT NULL,
  PRIMARY KEY (player_id),
  CONSTRAINT fk_player_photo_player
    FOREIGN KEY (player_id) REFERENCES tbl_players (id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
