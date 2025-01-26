<?php
session_start();
require_once 'config.php';

// Web login (admin):
if ($_SERVER['REQUEST_METHOD'] == 'POST' && isset($_POST['username'])) {
    $username = $_POST['username'];
    $password = $_POST['password'];
    
    // Simple check for admin
    if ($username === "admin" && $password === "admin123") {
        $_SESSION['admin_logged_in'] = true;
        die(header("Location: dashboard.php"));
    } else {
        die(header("Location: index.php?error=1"));
    }
    exit();
}

// Mobile app login:
$input = file_get_contents('php://input');
$data = json_decode($input, true);

if ($data) {
    $username = mysqli_real_escape_string($db, $data['username']);
    $password = $data['password']; // from JSON request

    $sql = "SELECT * FROM users WHERE username = '$username' LIMIT 1";
    $result = mysqli_query($db, $sql);

    if ($result && mysqli_num_rows($result) > 0) {
        $user = mysqli_fetch_assoc($result);

        // Compare the provided password with the one in database
        // If using password_hash():
        // if (password_verify($password, $user['password'])) {
        //     ... success ...
        // } else {
        //     ... fail ...
        // }

        // Use password_verify for bcrypt-hashed passwords
if (password_verify($password, $user['password'])) {
    // Return userId, role, etc.
    echo json_encode([
        'success' => true,
        'message' => 'Login successful',
        'role'    => $user['role'],
        'userId'  => $user['id']
    ]);
} else {
    // Wrong password
    echo json_encode([
        'success' => false,
        'message' => 'Incorrect password'
    ]);
}
    } else {
        // No user found with that username
        echo json_encode([
            'success' => false,
            'message' => 'User not found'
        ]);
    }
}
