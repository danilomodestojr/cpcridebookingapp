<?php
session_start();
require_once 'config.php';

if(!isset($_SESSION['admin_logged_in'])) {
    echo json_encode(["success" => false, "message" => "Unauthorized access"]);
    exit();
}

if ($_SERVER["REQUEST_METHOD"] == "POST" && isset($_POST['id'])) {
    $user_id = intval($_POST['id']);
    $first_name = mysqli_real_escape_string($db, $_POST['first_name']);
    $last_name = mysqli_real_escape_string($db, $_POST['last_name']);
    $role = mysqli_real_escape_string($db, $_POST['role']);
    $contact_number = mysqli_real_escape_string($db, $_POST['contact']);
    $driver_license = isset($_POST['license']) ? mysqli_real_escape_string($db, $_POST['license']) : null;
    $tricycle_number = isset($_POST['vehicle']) ? mysqli_real_escape_string($db, $_POST['vehicle']) : null;

    if ($role === "driver") {
        $sql = "UPDATE users SET first_name = ?, last_name = ?, role = ?, contact_number = ?, driver_license = ?, tricycle_number = ? WHERE id = ?";
        $stmt = mysqli_prepare($db, $sql);
        mysqli_stmt_bind_param($stmt, "ssssssi", $first_name, $last_name, $role, $contact_number, $driver_license, $tricycle_number, $user_id);
    } else {
        $sql = "UPDATE users SET first_name = ?, last_name = ?, role = ?, contact_number = ? WHERE id = ?";
        $stmt = mysqli_prepare($db, $sql);
        mysqli_stmt_bind_param($stmt, "ssssi", $first_name, $last_name, $role, $contact_number, $user_id);
    }

    if (mysqli_stmt_execute($stmt)) {
        echo json_encode(["success" => true, "message" => "User updated successfully"]);
    } else {
        echo json_encode(["success" => false, "message" => "Failed to update user"]);
    }

    mysqli_stmt_close($stmt);
    exit();
}
?>
