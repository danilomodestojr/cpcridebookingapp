<?php
session_start();
require_once 'config.php';

if(!isset($_SESSION['admin_logged_in'])) {
    echo json_encode(["success" => false, "message" => "Unauthorized access"]);
    exit();
}

if ($_SERVER["REQUEST_METHOD"] == "POST" && isset($_POST['id'])) {
    $user_id = intval($_POST['id']);

    $sql = "DELETE FROM users WHERE id = ?";
    $stmt = mysqli_prepare($db, $sql);
    mysqli_stmt_bind_param($stmt, "i", $user_id);

    if (mysqli_stmt_execute($stmt)) {
        echo json_encode(["success" => true, "message" => "User deleted successfully"]);
    } else {
        echo json_encode(["success" => false, "message" => "Failed to delete user"]);
    }

    mysqli_stmt_close($stmt);
    exit();
}
?>
