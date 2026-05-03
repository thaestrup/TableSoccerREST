CREATE USER IF NOT EXISTS 'football'@'%' IDENTIFIED BY '';
CREATE USER IF NOT EXISTS 'football'@'localhost' IDENTIFIED BY '';
CREATE USER IF NOT EXISTS 'football'@'127.0.0.1' IDENTIFIED BY '';
GRANT ALL PRIVILEGES ON nykreditfoosballunity.* TO 'football'@'%';
GRANT ALL PRIVILEGES ON nykreditfoosballunity.* TO 'football'@'localhost';
GRANT ALL PRIVILEGES ON nykreditfoosballunity.* TO 'football'@'127.0.0.1';
FLUSH PRIVILEGES;
