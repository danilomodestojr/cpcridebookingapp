<?php
session_start();
require_once 'config.php';

// Web login
if ($_SERVER['REQUEST_METHOD'] == 'POST' && isset($_POST['username'])) {
    $username = $_POST['username'];
    $password = $_POST['password'];
    
    if ($username === "admin" && $password === "admin123") {
        $_SESSION['admin_logged_in'] = true;
        die(header("Location: dashboard.php"));
    } else {
        die(header("Location: index.php?error=1"));
    }
    exit();
}

// Mobile app login
$input = file_get_contents('php://input');
$data = json_decode($input, true);

if ($data) {
    $username = mysqli_real_escape_string($db, $data['username']);
    $sql = "SELECT * FROM users WHERE username = '$username'";
    $result = mysqli_query($db, $sql);

    if ($result && mysqli_num_rows($result) > 0) {
        $user = mysqli_fetch_assoc($result);
        echo json_encode([
            'success' => true,
            'message' => 'Login successful',
            'role' => $user['role']
        ]);
    } else {
        echo json_encode([
            'success' => false,
            'message' => 'User not found'
        ]);
    }
}
?>