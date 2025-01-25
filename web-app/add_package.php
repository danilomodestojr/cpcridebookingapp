<?php
session_start();
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $name = mysqli_real_escape_string($db, $_POST['name']);
    $description = mysqli_real_escape_string($db, $_POST['description']);
    $route_points = mysqli_real_escape_string($db, $_POST['route_points']);
    $duration = (int)$_POST['duration'];
    $price = (float)$_POST['price'];

    $sql = "INSERT INTO tour_packages (name, description, route_points, duration_minutes, price) 
            VALUES ('$name', '$description', '$route_points', $duration, $price)";

    if (mysqli_query($db, $sql)) {
        header("Location: tour_packages.php?success=1");
    } else {
        header("Location: tour_packages.php?error=1");
    }
}
?>