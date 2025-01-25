<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');
header('Access-Control-Allow-Headers: Content-Type');

require_once 'config.php';

// Log the raw input data for debugging
error_log("Received data: " . file_get_contents('php://input'));

$data = json_decode(file_get_contents('php://input'), true);

// Validate required fields
if (!isset($data['username'], $data['name'], $data['last_name'], $data['phone'], $data['email'], $data['password'], $data['role'])) {
    echo json_encode(['success' => false, 'message' => 'Missing required fields']);
    exit;
}

// Escape and sanitize input
$username = mysqli_real_escape_string($db, $data['username']);
$first_name = mysqli_real_escape_string($db, $data['name']);
$last_name = mysqli_real_escape_string($db, $data['last_name']);
$contact_number = mysqli_real_escape_string($db, $data['phone']);
$email = mysqli_real_escape_string($db, $data['email']);
$password = password_hash($data['password'], PASSWORD_DEFAULT); // Hash the password
$role = mysqli_real_escape_string($db, $data['role']);

// Additional fields for Driver role
$driver_license = null;
$tricycle_number = null;

if ($role === "Driver") {
    if (!isset($data['driver_license'], $data['tricycle_number'])) {
        echo json_encode(['success' => false, 'message' => 'Driver license and tricycle number are required for Drivers']);
        exit;
    }
    $driver_license = mysqli_real_escape_string($db, $data['driver_license']);
    $tricycle_number = mysqli_real_escape_string($db, $data['tricycle_number']);
}

// Construct SQL query
$sql = "INSERT INTO users (username, first_name, last_name, contact_number, email, password, role, driver_license, tricycle_number, created_at) 
        VALUES ('$username', '$first_name', '$last_name', '$contact_number', '$email', '$password', '$role', 
                " . ($driver_license ? "'$driver_license'" : "NULL") . ", 
                " . ($tricycle_number ? "'$tricycle_number'" : "NULL") . ", NOW())";

// Execute query
if (mysqli_query($db, $sql)) {
    echo json_encode(['success' => true, 'message' => 'Registration successful']);
} else {
    echo json_encode(['success' => false, 'message' => 'Registration failed: ' . mysqli_error($db)]);
}
