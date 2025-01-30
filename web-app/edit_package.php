<?php
session_start();
require_once 'config.php';

if (!isset($_SESSION['admin_logged_in'])) {
    header("Location: index.php");
    exit();
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $id = $_POST['id'];
    $name = mysqli_real_escape_string($db, $_POST['name']);
    $description = mysqli_real_escape_string($db, $_POST['description']);
    $duration = intval($_POST['duration']);
    $price = floatval($_POST['price']);

    $sql = "UPDATE tour_packages SET 
            name = '$name', 
            description = '$description', 
            duration_minutes = $duration, 
            price = $price 
            WHERE id = $id";

    if (mysqli_query($db, $sql)) {
        $_SESSION['success_message'] = "Tour package updated successfully!";
    } else {
        $_SESSION['error_message'] = "Failed to update tour package.";
    }
    
    header("Location: tour_packages.php");
    exit();
}
?>
