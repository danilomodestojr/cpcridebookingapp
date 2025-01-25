<?php
session_start();
if (!isset($_SESSION['admin_logged_in'])) {
    header("Location: index.php");
    exit();
}

// Database connection
$host = 'localhost'; // Replace with your database host
$user = 'root'; // Replace with your database username
$password = ''; // Replace with your database password
$database = 'trikesafe_db'; // Replace with your database name

// Create the connection
$db = mysqli_connect($host, $user, $password, $database);

// Check the connection
if (!$db) {
    die("Connection failed: " . mysqli_connect_error());
}
?>


<?php
// Get counts for dashboard
$userCount = mysqli_fetch_assoc(mysqli_query($db, "SELECT COUNT(*) as count FROM users"))['count'];
$driverCount = mysqli_fetch_assoc(mysqli_query($db, "SELECT COUNT(*) as count FROM users WHERE role='driver'"))['count'];
$activeBookings = mysqli_fetch_assoc(mysqli_query($db, "SELECT COUNT(*) as count FROM bookings WHERE status IN ('pending','accepted')"))['count'];
$tourCount = mysqli_fetch_assoc(mysqli_query($db, "SELECT COUNT(*) as count FROM tour_packages"))['count'];
?>




<!DOCTYPE html>
<html>
<head>
    <title>TrikeSafe Admin - Dashboard</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary">
        <div class="container-fluid">
            <a class="navbar-brand" href="#">TrikeSafe Admin</a>
            <div class="collapse navbar-collapse">
                <ul class="navbar-nav me-auto">
                    <li class="nav-item">
                        <a class="nav-link active" href="dashboard.php">Dashboard</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="users.php">Users</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="bookings.php">Bookings</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="tour_packages.php">Tour Packages</a>
                    </li>
                </ul>
                <form class="d-flex" action="logout.php" method="POST">
                    <button class="btn btn-light" type="submit">Logout</button>
                </form>
            </div>
        </div>
    </nav>

    <div class="container mt-4">
        <h2>Dashboard</h2>
        <div class="row mt-4">
            <div class="col-md-3">
                <div class="card bg-primary text-white">
                    <div class="card-body">
                        <h5>Total Users</h5>
                        <h3><?php echo $userCount; ?></h3>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card bg-success text-white">
                    <div class="card-body">
                        <h5>Active Bookings</h5>
                        <h3><?php echo $activeBookings; ?></h3>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card bg-info text-white">
                    <div class="card-body">
                        <h5>Total Drivers</h5>
                        <h3><?php echo $driverCount; ?></h3>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card bg-warning text-white">
                    <div class="card-body">
                        <h5>Tour Packages</h5>
                        <h3><?php echo $tourCount; ?></h3>
                    </div>
                </div>
            </div>
        </div>
    </div>
</body>
</html>