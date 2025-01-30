<?php
session_start();
require_once 'config.php';

if (!isset($_SESSION['admin_logged_in'])) {
    header("Location: index.php");
    exit();
}

if (isset($_GET['id'])) {
    $id = intval($_GET['id']);
    $sql = "DELETE FROM tour_packages WHERE id = $id";

    if (mysqli_query($db, $sql)) {
        $_SESSION['success_message'] = "Tour package deleted successfully!";
    } else {
        $_SESSION['error_message'] = "Failed to delete tour package.";
    }

    header("Location: tour_packages.php");
    exit();
}
?>
