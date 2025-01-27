<?php
session_start();
require_once 'config.php';

// API endpoint for mobile app
if (!isset($_SESSION['admin_logged_in'])) {
    header('Access-Control-Allow-Origin: *');
    header('Content-Type: application/json');

    // Handle GET request for pending bookings
    if ($_SERVER['REQUEST_METHOD'] === 'GET' && isset($_GET['status'])) {
        $sql = "SELECT b.*, 
                p.first_name as passenger_name
                FROM bookings b 
                LEFT JOIN users p ON b.passenger_id = p.id 
                WHERE b.status = ?";
        
        $stmt = mysqli_prepare($db, $sql);
        mysqli_stmt_bind_param($stmt, "s", $_GET['status']);
        mysqli_stmt_execute($stmt);
        
        $result = mysqli_stmt_get_result($stmt);
        $bookings = [];
        
        while ($row = mysqli_fetch_assoc($result)) {
            $bookings[] = $row;
        }
        
        echo json_encode($bookings);
        exit();
    }

    // Handle POST request for new bookings
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        error_log("Received booking request: " . print_r($_POST, true));
		error_log("Received coordinates:");
		error_log("Pickup: " . $_POST['pickup_latitude'] . ", " . $_POST['pickup_longitude']);
		error_log("Dropoff: " . $_POST['dropoff_latitude'] . ", " . $_POST['dropoff_longitude']);
        
        $sql = "INSERT INTO bookings (
            passenger_id, 
            booking_type,
            pickup_location,
            dropoff_location,
            pickup_latitude,
            pickup_longitude,
            dropoff_latitude,
            dropoff_longitude,
            distance_km,
            total_fare,
            status
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending')";

        $stmt = mysqli_prepare($db, $sql);
		error_log("Binding values:");
		error_log("pickup_longitude: " . $_POST['pickup_longitude']);
		error_log("dropoff_longitude: " . $_POST['dropoff_longitude']);
        mysqli_stmt_bind_param($stmt, 
            "isssdddddd",
            $_POST['passenger_id'],
            $_POST['booking_type'],
            $_POST['pickup_location'],
            $_POST['dropoff_location'],
            $_POST['pickup_latitude'],
            $_POST['pickup_longitude'],
            $_POST['dropoff_latitude'],
            $_POST['dropoff_longitude'],
            $_POST['distance_km'],
            $_POST['total_fare']
        );

        if (!mysqli_stmt_execute($stmt)) {
            error_log("SQL Error: " . mysqli_stmt_error($stmt));
            $response = ['success' => false, 'error' => mysqli_stmt_error($stmt)];
        } else {
            $response = ['success' => true];
        }
        
        echo json_encode($response);
        exit();
    }
}

// Web interface
if(!isset($_SESSION['admin_logged_in'])) {
    header("Location: index.php");
    exit();
}

$sql = "SELECT b.*, 
        p.first_name as passenger_name, 
        d.first_name as driver_name 
        FROM bookings b 
        LEFT JOIN users p ON b.passenger_id = p.id 
        LEFT JOIN users d ON b.driver_id = d.id 
        ORDER BY b.created_at DESC";
$result = mysqli_query($db, $sql);
?>
<!DOCTYPE html>
<html>
<head>
    <title>TrikeSafe Admin - Bookings</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <?php include 'navbar.php'; ?>
    <div class="container mt-4">
        <h2>Bookings</h2>
        <div class="table-responsive mt-3">
            <table class="table table-striped">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Type</th>
                        <th>Passenger</th>
                        <th>Driver</th>
                        <th>Pickup</th>
                        <th>Dropoff</th>
                        <th>Status</th>
                        <th>Fare</th>
                        <th>Created</th>
                    </tr>
                </thead>
                <tbody>
                    <?php while($row = mysqli_fetch_assoc($result)): ?>
                    <tr>
                        <td><?php echo $row['id']; ?></td>
                        <td><?php echo ucfirst($row['booking_type']); ?></td>
                        <td><?php echo $row['passenger_name']; ?></td>
                        <td><?php echo $row['driver_name'] ?? 'Unassigned'; ?></td>
                        <td><?php echo $row['pickup_location']; ?></td>
                        <td><?php echo $row['dropoff_location']; ?></td>
                        <td>
                            <span class="badge bg-<?php 
                                echo match($row['status']) {
                                    'pending' => 'warning',
                                    'accepted' => 'primary',
                                    'completed' => 'success',
                                    'cancelled' => 'danger'
                                };
                            ?>">
                                <?php echo ucfirst($row['status']); ?>
                            </span>
                        </td>
                        <td>₱<?php echo number_format($row['total_fare'], 2); ?></td>
                        <td><?php echo date('M d, Y H:i', strtotime($row['created_at'])); ?></td>
                    </tr>
                    <?php endwhile; ?>
                </tbody>
            </table>
        </div>
    </div>
</body>
</html>