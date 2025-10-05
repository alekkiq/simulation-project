DROP DATABASE IF EXISTS simulation_db;
CREATE DATABASE simulation_db;
USE simulation_db;

CREATE USER IF NOT EXISTS 'appuser'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON simulation_db.* TO 'appuser'@'localhost';
FLUSH PRIVILEGES;

CREATE TABLE IF NOT EXISTS simulation_data (
    id INT AUTO_INCREMENT PRIMARY KEY,
    receptionServed INT NOT NULL,
    receptionServers INT NOT NULL,
    receptionAvgWait DOUBLE NOT NULL,
    receptionAvgService DOUBLE NOT NULL,
    receptionAvgTotal DOUBLE NOT NULL,
    receptionUtil DOUBLE NOT NULL,
    mechanicServed INT NOT NULL,
    mechanicServers INT NOT NULL,
    mechanicAvgWait DOUBLE NOT NULL,
    mechanicAvgService DOUBLE NOT NULL,
    mechanicAvgTotal DOUBLE NOT NULL,
    mechanicUtil DOUBLE NOT NULL,
    washServed INT NOT NULL,
    washServers INT NOT NULL,
    washAvgWait DOUBLE NOT NULL,
    washAvgService DOUBLE NOT NULL,
    washAvgTotal DOUBLE NOT NULL,
    washUtil DOUBLE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);