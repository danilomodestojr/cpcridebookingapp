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
        p.contact_number AS passenger_contact,
        tp.name as tour_name,
        tp.description as tour_description,
        tp.route_points as tour_points,
        COALESCE(tp.route_points, b.dropoff_location) as route
        FROM bookings b 
        LEFT JOIN users p ON b.passenger_id = p.id 
        LEFT JOIN tour_packages tp ON b.tour_package_id = tp.id
        WHERE b.driver_id = ? 
        AND b.status = 'accepted'";
            
            $stmt = mysqli_prepare($db, $sql);
            mysqli_stmt_bind_param($stmt, "i", $driver_id);
            mysqli_stmt_execute($stmt);
            
            $result = mysqli_stmt_get_result($stmt);
            $booking = mysqli_fetch_assoc($result);
            
			// ✅ Debugging: Log API response to XAMPP logs
    error_log("Booking Details API Response: " . json_encode($booking));
			
            echo json_encode($booking);
            exit();
        }

        // Check for active booking (passenger)
if (isset($_GET['action']) && $_GET['action'] === 'check_passenger_active' && isset($_GET['passenger_id'])) {
    $passenger_id = $_GET['passenger_id'];
    
    $sql = "SELECT 
                b.id,
                b.passenger_id,
                b.driver_id,
                b.booking_type,
                b.pickup_location,
                COALESCE(b.dropoff_location, tp.route_points) AS dropoff_location,  -- ✅ Ensures route_points is used if dropoff_location is NULL
                b.pickup_latitude,
                b.pickup_longitude,
                b.dropoff_latitude,
                b.dropoff_longitude,
                b.distance_km,
                b.base_fare,
                b.total_fare,
                b.status,
                b.created_at,
                b.tour_package_id,
                p.first_name AS passenger_name,
                p.contact_number AS passenger_contact,
                COALESCE(d.first_name, 'Not Assigned') AS driver_name, 
                COALESCE(d.contact_number, 'Not Available') AS driver_contact,
                tp.name AS tour_name,
                tp.description AS tour_description,
                tp.route_points AS route,  
				tp.route_points AS tour_points
            FROM bookings b
            LEFT JOIN users p ON b.passenger_id = p.id 
            LEFT JOIN users d ON b.driver_id = d.id 
            LEFT JOIN tour_packages tp ON b.tour_package_id = tp.id
            WHERE b.passenger_id = ? 
            AND b.status IN ('accepted', 'pending')";
    
    $stmt = mysqli_prepare($db, $sql);
    mysqli_stmt_bind_param($stmt, "i", $passenger_id);
    mysqli_stmt_execute($stmt);
    
    $result = mysqli_stmt_get_result($stmt);
    $booking = mysqli_fetch_assoc($result);
    
    error_log("Passenger Booking Details API Response: " . json_encode($booking)); // ✅ Debug Log

    echo json_encode($booking);
    exit();
}

		
		
		 // Add the new fare settings endpoint here, before the "Get bookings by status" section
    if (isset($_GET['action']) && $_GET['action'] === 'get_fare_settings') {
        $sql = "SELECT base_fare, additional_per_km, minimum_distance FROM fare_settings LIMIT 1";
        $result = mysqli_query($db, $sql);
        
        if ($settings = mysqli_fetch_assoc($result)) {
            echo json_encode([
                'success' => true,
                'settings' => $settings
            ]);
        } else {
            echo json_encode([
                'success' => false,
                'message' => 'Failed to get fare settings'
            ]);
        }
        exit();
    }
        
		// Tour packages endpoint
// Tour packages endpoint
if (isset($_GET['action']) && $_GET['action'] === 'get_tour_packages') {
    header('Content-Type: application/json');  // Ensure correct response format
    error_log("Fetching tour packages");

    // ✅ Updated SQL query to include dropoff_latitude and dropoff_longitude
    $sql = "SELECT id, name, description, route_points, duration_minutes, price, dropoff_latitude, dropoff_longitude FROM tour_packages";
    $result = mysqli_query($db, $sql);

    if ($result) {
        $packages = [];
        while ($row = mysqli_fetch_assoc($result)) {
            $packages[] = [
                'id' => intval($row['id']),
                'name' => $row['name'],
                'description' => $row['description'],
                'route_points' => $row['route_points'],
                'duration_minutes' => intval($row['duration_minutes']),
                'price' => floatval($row['price']),
                'dropoff_latitude' => floatval($row['dropoff_latitude']), // ✅ Added
                'dropoff_longitude' => floatval($row['dropoff_longitude']) // ✅ Added
            ];
        }

        echo json_encode([
            'success' => true,
            'packages' => $packages
        ]);
    } else {
        echo json_encode([
            'success' => false,
            'message' => 'Failed to fetch tour packages: ' . mysqli_error($db)
        ]);
    }
    exit();
}

		// Add the get_pending section here
if (isset($_GET['action']) && $_GET['action'] === 'get_pending') {
    $sql = "SELECT b.*, 
        tp.name as tour_name,
        tp.route_points as tour_points,
        tp.description as tour_description
        FROM bookings b 
        LEFT JOIN tour_packages tp ON b.tour_package_id = tp.id
        WHERE b.status = 'pending'
        ORDER BY b.created_at DESC";
    
    $result = mysqli_query($db, $sql);
    $bookings = array();
    
    while ($row = mysqli_fetch_assoc($result)) {
        $bookings[] = $row;
    }
    
    echo json_encode($bookings);
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
	
	 // Handle feedback submission
    // Handle feedback submission
if (isset($_POST['action']) && $_POST['action'] === 'feedback') {
    if (!isset($_POST['booking_id']) || !isset($_POST['passenger_id']) || 
        !isset($_POST['driver_id']) || !isset($_POST['rating'])) {
        echo json_encode([
            'success' => false,
            'message' => 'Missing required fields'
        ]);
        exit();
    }

    $booking_id = $_POST['booking_id'];
    $passenger_id = $_POST['passenger_id'];
    $driver_id = $_POST['driver_id'];
    $rating = $_POST['rating'];
    $comment = $_POST['comment'] ?? '';

    $sql = "INSERT INTO feedback (booking_id, passenger_id, driver_id, rating, comment) 
            VALUES (?, ?, ?, ?, ?)";
    
    $stmt = mysqli_prepare($db, $sql);
    mysqli_stmt_bind_param($stmt, "iiiis", $booking_id, $passenger_id, $driver_id, $rating, $comment);

    if (mysqli_stmt_execute($stmt)) {
        echo json_encode([
            'success' => true,
            'message' => 'Feedback submitted successfully'
        ]);
    } else {
        echo json_encode([
            'success' => false,
            'message' => 'Failed to submit feedback'
        ]);
    }
    exit();
}
	// End Handle feedback submission
	
	// Add the cancellation code here
	
	// Handle booking cancellation
if (isset($_POST['action']) && $_POST['action'] === 'cancel') {
    error_log("Processing booking cancellation request");

    if (!isset($_POST['booking_id']) || !isset($_POST['passenger_id'])) {
        echo json_encode([
            'success' => false,
            'message' => 'Missing required fields'
        ]);
        exit();
    }

    $booking_id = $_POST['booking_id'];
    $passenger_id = $_POST['passenger_id'];

    // Start transaction
    mysqli_begin_transaction($db);

    try {
        // Check if booking is still pending
        $check_sql = "SELECT status FROM bookings WHERE id = ? AND passenger_id = ? FOR UPDATE";
        $check_stmt = mysqli_prepare($db, $check_sql);
        mysqli_stmt_bind_param($check_stmt, "ii", $booking_id, $passenger_id);
        mysqli_stmt_execute($check_stmt);
        $result = mysqli_stmt_get_result($check_stmt);
        $booking = mysqli_fetch_assoc($result);

        if ($booking && $booking['status'] === 'pending') {
            // Update booking status to cancelled
            $update_sql = "UPDATE bookings SET status = 'cancelled' WHERE id = ?";
            $update_stmt = mysqli_prepare($db, $update_sql);
            mysqli_stmt_bind_param($update_stmt, "i", $booking_id);

            if (mysqli_stmt_execute($update_stmt)) {
                mysqli_commit($db);
                echo json_encode([
                    'success' => true,
                    'message' => 'Booking cancelled successfully'
                ]);
            } else {
                mysqli_rollback($db);
                echo json_encode([
                    'success' => false,
                    'message' => 'Failed to cancel booking'
                ]);
            }
        } else {
            mysqli_rollback($db);
            echo json_encode([
                'success' => false,
                'message' => 'Booking has already been accepted by a driver'
            ]);
        }
    } catch (Exception $e) {
        mysqli_rollback($db);
        echo json_encode([
            'success' => false,
            'message' => 'An error occurred while processing your request'
        ]);
    }
    exit();
}
	
	
	// End the cancellation code here

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

        // Start transaction
        mysqli_begin_transaction($db);

        try {
            // First check if booking is still available using FOR UPDATE to lock the row
            $check_sql = "SELECT status FROM bookings WHERE id = ? FOR UPDATE";
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
                    // Commit the transaction
                    mysqli_commit($db);
                    error_log("Successfully accepted booking $booking_id by driver $driver_id");
                    echo json_encode([
                        'success' => true,
                        'message' => 'Booking accepted successfully'
                    ]);
                } else {
                    // Rollback on update failure
                    mysqli_rollback($db);
                    error_log("SQL Error during accept: " . mysqli_stmt_error($update_stmt));
                    echo json_encode([
                        'success' => false,
                        'message' => 'Failed to update booking'
                    ]);
                }
            } else {
                // Rollback if booking not available
                mysqli_rollback($db);
                echo json_encode([
                    'success' => false,
                    'message' => 'Booking no longer available'
                ]);
            }
        } catch (Exception $e) {
            // Rollback on any error
            mysqli_rollback($db);
            error_log("Error in booking acceptance: " . $e->getMessage());
            echo json_encode([
                'success' => false,
                'message' => 'An error occurred while processing your request'
            ]);
        }
        exit();
    } // ✅ **This missing closing `}` was added**

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

        // Check if this is their booking and check both accepted and completed status
        $check_sql = "SELECT status FROM bookings WHERE id = ? AND driver_id = ? AND status IN ('accepted', 'completed')";
        $check_stmt = mysqli_prepare($db, $check_sql);
        mysqli_stmt_bind_param($check_stmt, "ii", $booking_id, $driver_id);
        mysqli_stmt_execute($check_stmt);
        $result = mysqli_stmt_get_result($check_stmt);
        $booking = mysqli_fetch_assoc($result);

        if ($booking) {
            if ($booking['status'] === 'completed') {
                // Booking is already completed, notify driver
                echo json_encode([
                    'success' => true,
                    'message' => 'Booking completed',
                    'status' => 'completed'
                ]);
            } else {
                // Still waiting for passenger confirmation
                echo json_encode([
                    'success' => true,
                    'message' => 'Please wait for passenger confirmation',
                    'status' => 'accepted'
                ]);
            }
        } else {
            echo json_encode([
                'success' => false,
                'message' => 'Invalid booking or not authorized'
            ]);
        }
        exit();
    } // ✅ **This missing closing `}` was added**

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
    } // ✅ **This missing closing `}` was added**
} // ✅ **Final closing `}` for `if ($_SERVER['REQUEST_METHOD'] === 'POST')`**

        
        // Handle new booking creation
        error_log("Processing new booking request");
        error_log("Received coordinates:");
        error_log("Pickup: " . $_POST['pickup_latitude'] . ", " . $_POST['pickup_longitude']);
        error_log("Dropoff: " . $_POST['dropoff_latitude'] . ", " . $_POST['dropoff_longitude']);
        
        $dropoff_location = !empty($_POST['dropoff_location']) ? $_POST['dropoff_location'] : "Unknown"; // ✅ Ensure non-null dropoff location
// ✅ Ensure dropoff_latitude & dropoff_longitude are never NULL
$dropoff_latitude = !empty($_POST['dropoff_latitude']) ? $_POST['dropoff_latitude'] : 0.0;
$dropoff_longitude = !empty($_POST['dropoff_longitude']) ? $_POST['dropoff_longitude'] : 0.0;

// ✅ If booking is a tour, override dropoff coordinates and route_points
if ($_POST['booking_type'] === 'tour' && !empty($_POST['tour_package_id'])) {
    $package_id = $_POST['tour_package_id'];
    $package_sql = "SELECT dropoff_latitude, dropoff_longitude, route_points FROM tour_packages WHERE id = ?";
    $package_stmt = mysqli_prepare($db, $package_sql);
    mysqli_stmt_bind_param($package_stmt, "i", $package_id);
    mysqli_stmt_execute($package_stmt);
    $package_result = mysqli_stmt_get_result($package_stmt);
    
    if ($package_row = mysqli_fetch_assoc($package_result)) {
        $dropoff_latitude = $package_row['dropoff_latitude'];  // ✅ Ensures correct coordinates
        $dropoff_longitude = $package_row['dropoff_longitude'];
        $dropoff_location = $package_row['route_points'];  // ✅ Ensures route_points are displayed as location
    }
}

// ✅ Debugging Log to Ensure Correct Dropoff Data
error_log("Final Dropoff Coordinates: lat=$dropoff_latitude, lon=$dropoff_longitude");

// Now, proceed with the `INSERT INTO bookings`
$dropoff_location = !empty($_POST['dropoff_location']) ? $_POST['dropoff_location'] : "Unknown"; // ✅ Ensure non-null dropoff location

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
    status,
    tour_package_id  
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending', ?)";

$stmt = mysqli_prepare($db, $sql);
mysqli_stmt_bind_param($stmt, 
    "isssddddddi",
    $_POST['passenger_id'],
    $_POST['booking_type'],
    $_POST['pickup_location'],
    $dropoff_location,  // ✅ Ensure it is never NULL
    $_POST['pickup_latitude'],
    $_POST['pickup_longitude'],
    $dropoff_latitude,  // ✅ Overridden with `tour_packages` data if applicable
    $dropoff_longitude,  // ✅ Overridden with `tour_packages` data if applicable
    $_POST['distance_km'],
    $_POST['total_fare'],
    $_POST['tour_package_id']
);


					if (!mysqli_stmt_execute($stmt)) {
    error_log("SQL Error: " . mysqli_stmt_error($stmt));
    echo json_encode([
        'success' => false,
        'message' => mysqli_stmt_error($stmt)
    ]);
    exit();  // Ensure script stops execution
} else {
    $booking_id = mysqli_insert_id($db);
    $sql = "SELECT * FROM bookings WHERE id = ?";
    
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
    exit();  // Ensure script stops execution
}
}

// Web interface
if(!isset($_SESSION['admin_logged_in'])) {
    header("Location: index.php");
    exit();
}

// Pagination Setup
$records_per_page = 15;
$page = isset($_GET['page']) && is_numeric($_GET['page']) ? (int) $_GET['page'] : 1;
$offset = ($page - 1) * $records_per_page;

// Sorting Logic
$columns = ['id', 'booking_type', 'tour_package_name', 'passenger_name', 'driver_name', 'rating', 'comment', 'status', 'total_fare', 'created_at'];
$sort_column = isset($_GET['sort']) && in_array($_GET['sort'], $columns) ? $_GET['sort'] : 'created_at';
$sort_order = isset($_GET['order']) && $_GET['order'] === 'asc' ? 'ASC' : 'DESC';

// Query with Sorting & Pagination
$sql = "SELECT b.*, 
        p.first_name AS passenger_name, 
        d.first_name AS driver_name,
        f.rating, 
        f.comment,
        COALESCE(tp.name, 'Regular') AS tour_package_name -- ✅ Ensure correct package name
        FROM bookings b 
        LEFT JOIN users p ON b.passenger_id = p.id 
        LEFT JOIN users d ON b.driver_id = d.id 
        LEFT JOIN feedback f ON b.id = f.booking_id
        LEFT JOIN tour_packages tp ON b.tour_package_id = tp.id 
        ORDER BY $sort_column $sort_order
        LIMIT $records_per_page OFFSET $offset";


$result = mysqli_query($db, $sql);

if (!$result) {
    die("Database query failed: " . mysqli_error($db)); // Debugging query failure
}

// Get Total Number of Records for Pagination
$total_sql = "SELECT COUNT(*) AS total FROM bookings";
$total_result = mysqli_query($db, $total_sql);
$total_row = mysqli_fetch_assoc($total_result);
$total_records = $total_row['total'];
$total_pages = ceil($total_records / $records_per_page);

?>
<!DOCTYPE html>
<html>
<head>
<style>
.pagination {
    display: flex;
    justify-content: center;
    margin-top: 20px;
}

.pagination .page-link {
    color: #007bff;
}

.pagination .active .page-link {
    background-color: #007bff;
    color: #fff;
    border-color: #007bff;
}
</style>
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
                        <?php 
						// Toggle sorting order
						$order = isset($_GET['order']) && $_GET['order'] === 'asc' ? 'desc' : 'asc'; 
						?>

						<th><a href="?sort=id&order=<?php echo $order; ?>">ID</a></th>
						<th><a href="?sort=booking_type&order=<?php echo $order; ?>">Type</a></th>
						<th>
    <a href="?page=1&sort=tour_package_name&order=<?php echo ($sort_column === 'tour_package_name' && $sort_order === 'ASC') ? 'desc' : 'asc'; ?>">
        Tour Package
    </a>
</th>
						<th><a href="?sort=passenger_name&order=<?php echo $order; ?>">Passenger</a></th>
						<th><a href="?sort=driver_name&order=<?php echo $order; ?>">Driver</a></th>
						<th><a href="?sort=rating&order=<?php echo $order; ?>">Rating</a></th>
						<th><a href="?sort=comment&order=<?php echo $order; ?>">Comment</a></th>
						<th><a href="?sort=status&order=<?php echo $order; ?>">Status</a></th>
						<th><a href="?sort=total_fare&order=<?php echo $order; ?>">Fare</a></th>
						<th><a href="?sort=created_at&order=<?php echo $order; ?>">Created</a></th>
                    </tr>
                </thead>
                <tbody>
                    <?php while($row = mysqli_fetch_assoc($result)): ?>
                    <tr>
                        <td><?php echo $row['id']; ?></td>
                        <td><?php echo ucfirst($row['booking_type']); ?></td>
						<td><?php echo htmlentities($row['tour_package_name']); ?></td>
                        <td><?php echo $row['passenger_name']; ?></td>
                        <td><?php echo $row['driver_name'] ?? 'Unassigned'; ?></td>
                        <td><?php echo isset($row['rating']) ? $row['rating'] : 'N/A'; ?></td>
                        <td><?php echo !empty($row['comment']) ? htmlentities($row['comment']) : 'No comment'; ?></td>
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
	<div class="pagination">
    <nav>
        <ul class="pagination">
            <?php if ($page > 1): ?>
                <li class="page-item">
                    <a class="page-link" href="?page=1&sort=<?php echo $sort_column; ?>&order=<?php echo $sort_order; ?>">First</a>
                </li>
                <li class="page-item">
                    <a class="page-link" href="?page=<?php echo $page - 1; ?>&sort=<?php echo $sort_column; ?>&order=<?php echo $sort_order; ?>">Previous</a>
                </li>
            <?php endif; ?>

            <?php for ($i = 1; $i <= $total_pages; $i++): ?>
                <li class="page-item <?php echo ($i === $page) ? 'active' : ''; ?>">
                    <a class="page-link" href="?page=<?php echo $i; ?>&sort=<?php echo $sort_column; ?>&order=<?php echo $sort_order; ?>"><?php echo $i; ?></a>
                </li>
            <?php endfor; ?>

            <?php if ($page < $total_pages): ?>
                <li class="page-item">
                    <a class="page-link" href="?page=<?php echo $page + 1; ?>&sort=<?php echo $sort_column; ?>&order=<?php echo $sort_order; ?>">Next</a>
                </li>
                <li class="page-item">
                    <a class="page-link" href="?page=<?php echo $total_pages; ?>&sort=<?php echo $sort_column; ?>&order=<?php echo $sort_order; ?>">Last</a>
                </li>
            <?php endif; ?>
        </ul>
    </nav>
</div>

</body>
</html>