-- V3__seed_dev.sql
-- Development / test seed data ported verbatim from
-- TableSoccerREST/db-init/01-schema.sql (configuration: lines 38-42,
-- players: lines 92-133, timer: lines 150-151).
--
-- Profile handling
-- ----------------
-- This is a regular versioned migration rather than a repeatable R__
-- migration on purpose. A repeatable migration would re-run on every
-- checksum change, but the seed is static and we want one-shot insertion
-- semantics on a fresh DB. Idempotency is achieved with INSERT IGNORE on
-- the unique-keyed rows.
--
-- Production deployments that should NOT include this seed must point
-- Flyway at a directory that excludes V3, e.g.:
--
--   # application.properties (prod profile)
--   %prod.quarkus.flyway.locations=classpath:db/migration/prod
--
-- and arrange V1/V2 to live under db/migration/prod, OR set
-- quarkus.flyway.baseline-on-migrate=true + baseline-version=3 on prod
-- so V3 is treated as already-applied.
--
-- Dev/test profiles (the default %dev and %test) use the standard
-- classpath:db/migration location and pick this file up automatically.

-- ---------------------------------------------------------------------------
-- tbl_configuration (4 rows)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO tbl_configuration (name, value) VALUES
  ('nameTable1',     'Fort Nordjylland, Nikolaj Arena'),
  ('nameTable2',     'John og Nikolaj Stadion'),
  ('nameTable3',     'Henrik Park'),
  ('numberOfTables', '3');

-- ---------------------------------------------------------------------------
-- tbl_players (40 rows; original IDs and timestamps preserved)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO tbl_players (id, name, playerReady, oprettet, registeredRFIDTag) VALUES
  (1,  'Lars',        1, '2016-10-10 19:46:36', '9E5D046000'),
  (2,  'Joan',        1, '2016-10-10 19:46:36', '7E8F0C6000'),
  (3,  'Michael',     1, '2016-10-10 19:46:36', ''),
  (4,  'John',        1, '2016-10-10 19:46:36', ''),
  (5,  'Nikolaj',     1, '2016-10-10 19:46:36', 'EE39046000'),
  (6,  'Jens',        1, '2016-10-10 19:46:36', ''),
  (7,  'Daniel',      1, '2016-10-10 19:46:36', ''),
  (9,  'Frank',       1, '2016-10-10 19:46:36', '0EEE4BCD00'),
  (10, 'Kristine',    1, '2016-10-10 19:46:36', '0020B35A00'),
  (11, 'Casper',      1, '2016-10-10 19:46:36', 'FEF7096000'),
  (12, 'Carsten',     1, '2016-10-10 19:46:36', ''),
  (13, 'Morten',      1, '2016-10-10 19:46:36', ''),
  (14, 'Henrik',      1, '2016-10-10 19:46:36', 'BEFB096000'),
  (15, 'Thomas',      1, '2016-10-10 19:46:36', ''),
  (17, 'Rasmus',      1, '2016-10-10 19:46:36', ''),
  (19, 'Peter',       1, '2016-10-10 19:46:36', '2EDB3ECD00'),
  (20, 'Rune',        1, '2016-10-10 19:46:36', '6272B35A00'),
  (21, 'Christian',   1, '2016-10-10 19:46:36', ''),
  (22, 'Per',         1, '2016-10-10 19:46:36', ''),
  (24, 'Frederik',    1, '2016-10-10 19:46:36', ''),
  (25, 'Allan',       1, '2016-10-10 19:46:36', ''),
  (34, 'ThomasBo',    1, '2016-10-10 19:46:36', '9EFA096000'),
  (37, 'Tina',        1, '2016-10-10 19:46:36', ''),
  (38, 'Kenneth',     1, '2016-10-10 19:46:36', ''),
  (39, 'Rene',        1, '2016-10-10 19:46:36', ''),
  (46, 'Kasper',      1, '2016-10-10 19:46:36', ''),
  (47, 'MortenMOHI',  1, '2016-10-10 19:46:36', ''),
  (48, 'LarsLAJE',    1, '2016-10-10 19:46:36', ''),
  (49, 'Erik',        1, '2016-10-10 19:46:36', ''),
  (54, 'Ole',         1, '2016-10-10 19:46:36', ''),
  (55, 'LarsLAKR',    1, '2016-10-10 19:46:36', '9E37046000'),
  (58, 'MortenMONC',  1, '2016-10-10 19:46:36', ''),
  (75, 'MortenMONN',  1, '2016-10-10 19:46:36', ''),
  (76, 'METP',        1, '2016-10-10 19:46:36', ''),
  (77, 'Laila',       1, '2016-10-10 19:46:36', ''),
  (78, 'ThomasQ1M7',  1, '2016-10-10 19:46:36', ''),
  (79, 'Elin',        1, '2016-10-10 19:46:36', ''),
  (81, 'KasperM',     1, '2016-10-10 19:46:36', ''),
  (83, 'DOKI',        1, '2016-10-10 19:46:36', ''),
  (84, 'Mikkel',      1, '2016-10-10 19:46:36', ''),
  (85, 'MHWH',        1, '2016-10-10 19:46:36', '');

-- ---------------------------------------------------------------------------
-- tbl_timer (1 row)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO tbl_timer (id, lastRequestedTimerStart) VALUES
  (1, '2017-04-24 12:37:51');
