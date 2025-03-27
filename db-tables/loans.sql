-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Mar 27, 2025 at 11:03 AM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.0.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `dacsproject`
--

-- --------------------------------------------------------

--
-- Table structure for table `loans`
--

CREATE TABLE `loans` (
  `loan_id` int(11) NOT NULL,
  `isbn` varchar(32) NOT NULL,
  `member_id` int(11) NOT NULL,
  `loan_date` date NOT NULL,
  `due_date` date NOT NULL,
  `fine_per_day` decimal(10,2) NOT NULL DEFAULT 5.00,
  `returned` tinyint(1) DEFAULT 0,
  `return_date` date DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `loans`
--

INSERT INTO `loans` (`loan_id`, `isbn`, `member_id`, `loan_date`, `due_date`, `fine_per_day`, `returned`, `return_date`) VALUES
(11, '9780596009205', 8, '2025-03-26', '2025-03-28', 5.00, 1, '2025-03-26'),
(12, '9780131103627', 8, '2025-03-26', '2025-04-05', 5.00, 0, NULL),
(13, '9781491950357', 9, '2025-03-26', '2025-03-24', 5.00, 1, '2025-03-26'),
(14, '9781492078005', 10, '2025-03-26', '2025-04-09', 5.00, 1, '2025-03-26'),
(15, '9780596009205', 8, '2025-03-26', '2025-03-26', 5.00, 1, '2025-03-26'),
(16, '9781617295522', 11, '2025-03-27', '2025-03-27', 5.00, 1, '2025-03-27'),
(18, '9780132350884', 11, '2025-03-05', '2025-03-10', 5.00, 1, '2025-03-27'),
(19, '9781617295522', 12, '2025-03-18', '2025-03-25', 5.00, 1, '2025-03-27'),
(20, '9780132350884', 12, '2025-03-20', '2025-03-25', 5.00, 0, NULL),
(21, '9781491950357', 10, '2025-03-27', '2025-04-20', 5.00, 1, '2025-03-27'),
(22, '9780596009205', 16, '2025-03-05', '2025-03-20', 5.00, 0, NULL),
(23, '9781492078005', 11, '2025-03-27', '2025-03-29', 5.00, 0, NULL);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `loans`
--
ALTER TABLE `loans`
  ADD PRIMARY KEY (`loan_id`),
  ADD KEY `member_id` (`member_id`),
  ADD KEY `isbn` (`isbn`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `loans`
--
ALTER TABLE `loans`
  MODIFY `loan_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=24;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `loans`
--
ALTER TABLE `loans`
  ADD CONSTRAINT `loans_ibfk_1` FOREIGN KEY (`member_id`) REFERENCES `members` (`member_id`),
  ADD CONSTRAINT `loans_ibfk_2` FOREIGN KEY (`isbn`) REFERENCES `books` (`isbn`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
