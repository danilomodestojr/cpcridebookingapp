<?php
session_start();
require_once 'config.php';

if(!isset($_SESSION['admin_logged_in'])) {
    header("Location: index.php");
    exit();
}

// Handle form submission
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $base_fare = $_POST['base_fare'];
    $additional_per_km = $_POST['additional_per_km'];
    $minimum_distance = $_POST['minimum_distance'];
    
    $sql = "UPDATE fare_settings SET base_fare = ?, additional_per_km = ?, minimum_distance = ?";
    $stmt = mysqli_prepare($db, $sql);
    mysqli_stmt_bind_param($stmt, "ddd", $base_fare, $additional_per_km, $minimum_distance);
    
    if (mysqli_stmt_execute($stmt)) {
        $success_message = "Fare settings updated successfully!";
    } else {
        $error_message = "Failed to update fare settings.";
    }
}

// Get current settings
$sql = "SELECT * FROM fare_settings LIMIT 1";
$result = mysqli_query($db, $sql);
$settings = mysqli_fetch_assoc($result);
?>

<!DOCTYPE html>
<html>
<head>
    <title>TrikeSafe Admin - Fare Settings</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <?php include 'navbar.php'; ?>
    <div class="container mt-4">
        <h2>Fare Settings</h2>
        
        <?php if (isset($success_message)): ?>
            <div class="alert alert-success"><?php echo $success_message; ?></div>
        <?php endif; ?>
        
        <?php if (isset($error_message)): ?>
            <div class="alert alert-danger"><?php echo $error_message; ?></div>
        <?php endif; ?>
        
        <form method="post" class="mt-4">
            <div class="mb-3">
                <label class="form-label">Base Fare (₱) - First few km</label>
                <input type="number" step="0.01" class="form-control" name="base_fare" 
                       value="<?php echo $settings['base_fare']; ?>" required>
                <small class="text-muted">This is the fixed fare for the first 4 kilometers</small>
            </div>
            
            <div class="mb-3">
                <label class="form-label">Additional Rate per KM (₱)</label>
                <input type="number" step="0.01" class="form-control" name="additional_per_km" 
                       value="<?php echo $settings['additional_per_km']; ?>" required>
                <small class="text-muted">Rate charged for each kilometer beyond base km</small>
            </div>
            
            <div class="mb-3">
                <label class="form-label">Minimum Distance (KM)</label>
                <input type="number" step="0.01" class="form-control" name="minimum_distance" 
                       value="<?php echo $settings['minimum_distance']; ?>" required>
                <small class="text-muted">Distance covered by base fare (typically 4km)</small>
            </div>
            
            <button type="submit" class="btn btn-primary">Update Settings</button>
        </form>
    </div>
</body>
</html>