-- Soft-delete column for tbl_fights. Live rows have deleted_at IS NULL.

ALTER TABLE tbl_fights
  ADD COLUMN deleted_at TIMESTAMP NULL DEFAULT NULL
  AFTER winning_table;

CREATE INDEX idx_tbl_fights_deleted_at ON tbl_fights (deleted_at);
