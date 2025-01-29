<?php
session_start();
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $name = mysqli_real_escape_string($db, $_POST['name']);
    $description = mysqli_real_escape_string($db, $_POST['description']);
    $route_points = mysqli_real_escape_string($db, $_POST['route_points']);
    $duration = (int)$_POST['duration'];
    $price = (float)$_POST['price'];
    $dropoff_latitude = (float)$_POST['dropoff_latitude']; // New
    $dropoff_longitude = (float)$_POST['dropoff_longitude']; // New

    $sql = "INSERT INTO tour_packages (name, description, route_points, duration_minutes, price, dropoff_latitude, dropoff_longitude) 
            VALUES ('$name', '$description', '$route_points', $duration, $price, $dropoff_latitude, $dropoff_longitude)";

    if (mysqli_query($db, $sql)) {
        header("Location: tour_packages.php?success=1");
    } else {
        header("Location: tour_packages.php?error=1");
    }
}
?>
