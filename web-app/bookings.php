<?php
session_start();
require_once 'config.php';

// API endpoint for mobile app
if (!isset($_SESSION['admin_logged_in'])) {
    header('Access-Control-Allow-Origin: *');
    header('Content-Type: application/json');

    // Handle GET request for pending bookings
    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        // Check for active booking (driver)
        if (isset($_GET['action']) && $_GET['action'] === 'check_active' && isset($_GET['driver_id'])) {
            $driver_id = $_GET['driver_id'];
            
            $sql = "SELECT b.*, 
        p.first_name AS passenger_name,
        p.contact_number AS passenger_contact
        FROM bookings b 
        LEFT JOIN users p ON b.passenger_id = p.id 
        WHERE b.driver_id = ? 
        AND b.status = 'accepted'";
            
            $stmt = mysqli_prepare($db, $sql);
            mysqli_stmt_bind_param($stmt, "i", $driver_id);
            mysqli_stmt_execute($stmt);
            
            $result = mysqli_stmt_get_result($stmt);
            $booking = mysqli_fetch_assoc($result);
            
            echo json_encode($booking);
            exit();
        }

        // Check for active booking (passenger)
        if (isset($_GET['action']) && $_GET['action'] === 'check_passenger_active' && isset($_GET['passenger_id'])) {
            $passenger_id = $_GET['passenger_id'];
            
            $sql = "SELECT b.*, 
                    p.first_name as passenger_name,
                    d.first_name as driver_name
                    FROM bookings b 
                    LEFT JOIN users p ON b.passenger_id = p.id 
                    LEFT JOIN users d ON b.driver_id = d.id 
                    WHERE b.passenger_id = ? 
                    AND b.status IN ('accepted', 'pending')";
            
            $stmt = mysqli_prepare($db, $sql);
            mysqli_stmt_bind_param($stmt, "i", $passenger_id);
            mysqli_stmt_execute($stmt);
            
            $result = mysqli_stmt_get_result($stmt);
            $booking = mysqli_fetch_assoc($result);
            
            echo json_encode($booking);
            exit();
        }
        
        // Get bookings by status
        if (isset($_GET['status'])) {
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
    }

    // Handle POST requests
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        error_log("Raw POST data: " . file_get_contents('php://input'));
        error_log("POST array: " . print_r($_POST, true));

        // Handle booking acceptance
        if (isset($_POST['action']) && $_POST['action'] === 'accept') {
            error_log("Processing booking acceptance request");
            
            if (!isset($_POST['booking_id']) || !isset($_POST['driver_id'])) {
                echo json_encode([
                    'success' => false,
                    'message' => 'Missing required fields'
                ]);
                exit();
            }
            
            $booking_id = $_POST['booking_id'];
            $driver_id = $_POST['driver_id'];
            
            // First check if booking is still available
            $check_sql = "SELECT status FROM bookings WHERE id = ?";
            $check_stmt = mysqli_prepare($db, $check_sql);
            mysqli_stmt_bind_param($check_stmt, "i", $booking_id);
            mysqli_stmt_execute($check_stmt);
            $result = mysqli_stmt_get_result($check_stmt);
            $booking = mysqli_fetch_assoc($result);
            
            if ($booking && $booking['status'] === 'pending') {
                // Update booking with driver and change status
                $update_sql = "UPDATE bookings SET driver_id = ?, status = 'accepted' WHERE id = ? AND status = 'pending'";
                $update_stmt = mysqli_prepare($db, $update_sql);
                mysqli_stmt_bind_param($update_stmt, "ii", $driver_id, $booking_id);
                
                if (mysqli_stmt_execute($update_stmt)) {
                    error_log("Successfully accepted booking $booking_id by driver $driver_id");
                    echo json_encode([
                        'success' => true,
                        'message' => 'Booking accepted successfully'
                    ]);
                } else {
                    error_log("SQL Error during accept: " . mysqli_stmt_error($update_stmt));
                    echo json_encode([
                        'success' => false,
                        'message' => 'Failed to update booking'
                    ]);
                }
            } else {
                echo json_encode([
                    'success' => false,
                    'message' => 'Booking no longer available'
                ]);
            }
            exit();
        }

        // Handle driver marking booking as complete
        if (isset($_POST['action']) && $_POST['action'] === 'driver_complete') {
            error_log("Processing driver complete request");
            
            if (!isset($_POST['booking_id']) || !isset($_POST['driver_id'])) {
                echo json_encode([
                    'success' => false,
                    'message' => 'Missing required fields'
                ]);
                exit();
            }
            
            $booking_id = $_POST['booking_id'];
            $driver_id = $_POST['driver_id'];
            
            // Check if this is their booking and it's in accepted status
            $check_sql = "SELECT status FROM bookings WHERE id = ? AND driver_id = ? AND status = 'accepted'";
            $check_stmt = mysqli_prepare($db, $check_sql);
            mysqli_stmt_bind_param($check_stmt, "ii", $booking_id, $driver_id);
            mysqli_stmt_execute($check_stmt);
            $result = mysqli_stmt_get_result($check_stmt);
            $booking = mysqli_fetch_assoc($result);
            
            if ($booking) {
                echo json_encode([
                    'success' => true,
                    'message' => 'Please wait for passenger confirmation'
                ]);
            } else {
                echo json_encode([
                    'success' => false,
                    'message' => 'Invalid booking or not authorized'
                ]);
            }
            exit();
        }

        // Handle passenger confirming completion
        if (isset($_POST['action']) && $_POST['action'] === 'passenger_confirm') {
            error_log("Processing passenger confirmation request");
            
            if (!isset($_POST['booking_id']) || !isset($_POST['passenger_id'])) {
                echo json_encode([
                    'success' => false,
                    'message' => 'Missing required fields'
                ]);
                exit();
            }
            
            $booking_id = $_POST['booking_id'];
            $passenger_id = $_POST['passenger_id'];
            
            // Check if this is their booking and it's in accepted status
            $check_sql = "SELECT status FROM bookings WHERE id = ? AND passenger_id = ? AND status = 'accepted'";
            $check_stmt = mysqli_prepare($db, $check_sql);
            mysqli_stmt_bind_param($check_stmt, "ii", $booking_id, $passenger_id);
            mysqli_stmt_execute($check_stmt);
            $result = mysqli_stmt_get_result($check_stmt);
            $booking = mysqli_fetch_assoc($result);
            
            if ($booking) {
                // Update booking status to completed
                $update_sql = "UPDATE bookings SET status = 'completed' WHERE id = ?";
                $update_stmt = mysqli_prepare($db, $update_sql);
                mysqli_stmt_bind_param($update_stmt, "i", $booking_id);
                
                if (mysqli_stmt_execute($update_stmt)) {
                    echo json_encode([
                        'success' => true,
                        'message' => 'Booking completed successfully'
                    ]);
                } else {
                    echo json_encode([
                        'success' => false,
                        'message' => 'Failed to complete booking'
                    ]);
                }
            } else {
                echo json_encode([
                    'success' => false,
                    'message' => 'Invalid booking or not authorized'
                ]);
            }
            exit();
        }
        
        // Handle new booking creation
        error_log("Processing new booking request");
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
				echo json_encode([
					'success' => false,
					'message' => mysqli_stmt_error($stmt),
					'booking' => null
				]);
			} else {
				// Get the created booking
				$booking_id = mysqli_insert_id($db);
				$sql = "SELECT b.*, 
						p.first_name as passenger_name
						FROM bookings b 
						LEFT JOIN users p ON b.passenger_id = p.id 
						WHERE b.id = ?";
				
				$stmt = mysqli_prepare($db, $sql);
				mysqli_stmt_bind_param($stmt, "i", $booking_id);
				mysqli_stmt_execute($stmt);
				
				$result = mysqli_stmt_get_result($stmt);
				$booking = mysqli_fetch_assoc($result);
				
				echo json_encode([
					'success' => true,
					'message' => 'Booking created successfully',
					'booking' => $booking
				]);
			}
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