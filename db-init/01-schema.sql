-- phpMyAdmin SQL Dump
-- version 4.5.4.1deb2ubuntu2
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: May 05, 2017 at 12:08 PM
-- Server version: 5.7.13-0ubuntu0.16.04.2
-- PHP Version: 7.0.8-0ubuntu0.16.04.3

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `NykreditFoosballUnity`
--

-- --------------------------------------------------------

--
-- Table structure for table `tbl_configuration`
--

CREATE TABLE `tbl_configuration` (
  `name` varchar(255) NOT NULL,
  `value` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Dumping data for table `tbl_configuration`
--

INSERT INTO `tbl_configuration` (`name`, `value`) VALUES
('nameTable1', 'Fort Nordjylland, Nikolaj Arena'),
('nameTable2', 'John og Nikolaj Stadion'),
('nameTable3', 'Henrik Park'),
('numberOfTables', '3');

-- --------------------------------------------------------

--
-- Table structure for table `tbl_fights`
--

CREATE TABLE `tbl_fights` (
  `id` int(11) NOT NULL,
  `player_red_1` varchar(200) NOT NULL,
  `player_red_2` varchar(200) NOT NULL,
  `player_blue_1` varchar(200) NOT NULL,
  `player_blue_2` varchar(200) NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `match_winner` varchar(20) NOT NULL,
  `points_at_steake` int(11) NOT NULL,
  `winning_table` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `tbl_latest_rfid_registration`
--

CREATE TABLE `tbl_latest_rfid_registration` (
  `id` int(11) NOT NULL,
  `lastRFIDRegisteredTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `registeredRFIDTag` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `tbl_players`
--

CREATE TABLE `tbl_players` (
  `id` int(11) NOT NULL,
  `name` varchar(20) NOT NULL,
  `playerReady` tinyint(1) NOT NULL DEFAULT '0',
  `oprettet` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `registeredRFIDTag` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Dumping data for table `tbl_players`
--

INSERT INTO `tbl_players` (`id`, `name`, `playerReady`, `oprettet`, `registeredRFIDTag`) VALUES
(1, 'Lars', 1, '2016-10-10 19:46:36', '9E5D046000'),
(2, 'Joan', 1, '2016-10-10 19:46:36', '7E8F0C6000'),
(3, 'Michael', 1, '2016-10-10 19:46:36', ''),
(4, 'John', 1, '2016-10-10 19:46:36', ''),
(5, 'Nikolaj', 1, '2016-10-10 19:46:36', 'EE39046000'),
(6, 'Jens', 1, '2016-10-10 19:46:36', ''),
(7, 'Daniel', 1, '2016-10-10 19:46:36', ''),
(9, 'Frank', 1, '2016-10-10 19:46:36', '0EEE4BCD00'),
(10, 'Kristine', 1, '2016-10-10 19:46:36', '0020B35A00'),
(11, 'Casper', 1, '2016-10-10 19:46:36', 'FEF7096000'),
(12, 'Carsten', 1, '2016-10-10 19:46:36', ''),
(13, 'Morten', 1, '2016-10-10 19:46:36', ''),
(14, 'Henrik', 1, '2016-10-10 19:46:36', 'BEFB096000'),
(15, 'Thomas', 1, '2016-10-10 19:46:36', ''),
(17, 'Rasmus', 1, '2016-10-10 19:46:36', ''),
(19, 'Peter', 1, '2016-10-10 19:46:36', '2EDB3ECD00'),
(20, 'Rune', 1, '2016-10-10 19:46:36', '6272B35A00'),
(21, 'Christian', 1, '2016-10-10 19:46:36', ''),
(22, 'Per', 1, '2016-10-10 19:46:36', ''),
(24, 'Frederik', 1, '2016-10-10 19:46:36', ''),
(25, 'Allan', 1, '2016-10-10 19:46:36', ''),
(34, 'ThomasBo', 1, '2016-10-10 19:46:36', '9EFA096000'),
(37, 'Tina', 1, '2016-10-10 19:46:36', ''),
(38, 'Kenneth', 1, '2016-10-10 19:46:36', ''),
(39, 'Rene', 1, '2016-10-10 19:46:36', ''),
(46, 'Kasper', 1, '2016-10-10 19:46:36', ''),
(47, 'MortenMOHI', 1, '2016-10-10 19:46:36', ''),
(48, 'LarsLAJE', 1, '2016-10-10 19:46:36', ''),
(49, 'Erik', 1, '2016-10-10 19:46:36', ''),
(54, 'Ole', 1, '2016-10-10 19:46:36', ''),
(55, 'LarsLAKR', 1, '2016-10-10 19:46:36', '9E37046000'),
(58, 'MortenMONC', 1, '2016-10-10 19:46:36', ''),
(75, 'MortenMONN', 1, '2016-10-10 19:46:36', ''),
(76, 'METP', 1, '2016-10-10 19:46:36', ''),
(77, 'Laila', 1, '2016-10-10 19:46:36', ''),
(78, 'ThomasQ1M7', 1, '2016-10-10 19:46:36', ''),
(79, 'Elin', 1, '2016-10-10 19:46:36', ''),
(81, 'KasperM', 1, '2016-10-10 19:46:36', ''),
(83, 'DOKI', 1, '2016-10-10 19:46:36', ''),
(84, 'Mikkel', 1, '2016-10-10 19:46:36', ''),
(85, 'MHWH', 1, '2016-10-10 19:46:36', '');

-- --------------------------------------------------------

--
-- Table structure for table `tbl_timer`
--

CREATE TABLE `tbl_timer` (
  `id` int(11) NOT NULL,
  `lastRequestedTimerStart` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Dumping data for table `tbl_timer`
--

INSERT INTO `tbl_timer` (`id`, `lastRequestedTimerStart`) VALUES
(1, '2017-04-24 12:37:51');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `tbl_configuration`
--
ALTER TABLE `tbl_configuration`
  ADD UNIQUE KEY `name` (`name`);

--
-- Indexes for table `tbl_fights`
--
ALTER TABLE `tbl_fights`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `tbl_latest_rfid_registration`
--
ALTER TABLE `tbl_latest_rfid_registration`
  ADD UNIQUE KEY `id` (`id`);

--
-- Indexes for table `tbl_players`
--
ALTER TABLE `tbl_players`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `name` (`name`);

--
-- Indexes for table `tbl_timer`
--
ALTER TABLE `tbl_timer`
  ADD UNIQUE KEY `id` (`id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `tbl_fights`
--
ALTER TABLE `tbl_fights`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `tbl_players`
--
ALTER TABLE `tbl_players`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=87;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
