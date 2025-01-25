<?php
$db = mysqli_connect("localhost", "root", "", "trikesafe_db");
if (!$db) {
    die("Connection failed: " . mysqli_connect_error());
}
?>