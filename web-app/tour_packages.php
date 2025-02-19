<?php
session_start();
require_once 'config.php';
if(!isset($_SESSION['admin_logged_in'])) {
    header("Location: index.php");
    exit();
}

$sql = "SELECT * FROM tour_packages ORDER BY created_at DESC";
$result = mysqli_query($db, $sql);
?>

<!DOCTYPE html>
<html>
<head>
    <title>TrikeSafe Admin - Tour Packages</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <?php include 'navbar.php'; ?>

    <div class="container mt-4">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h2>Tour Packages</h2>
            <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#addPackageModal">
                Add New Package
            </button>
        </div>

        <div class="table-responsive">
            <table class="table table-striped">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Name</th>
                        <th>Description</th>
                        <th>Duration</th>
                        <th>Price</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <?php while($row = mysqli_fetch_assoc($result)): ?>
                    <tr>
                        <td><?php echo $row['id']; ?></td>
                        <td><?php echo $row['name']; ?></td>
                        <td><?php echo $row['description']; ?></td>
                        <td><?php echo $row['duration_minutes']; ?> mins</td>
                        <td>₱<?php echo number_format($row['price'], 2); ?></td>
                        <td>
							<button class="btn btn-sm btn-warning edit-btn" 
									data-id="<?php echo $row['id']; ?>" 
									data-name="<?php echo htmlentities($row['name']); ?>" 
									data-description="<?php echo htmlentities($row['description']); ?>" 
									data-duration="<?php echo $row['duration_minutes']; ?>"
									data-price="<?php echo $row['price']; ?>"
									data-bs-toggle="modal" data-bs-target="#editPackageModal">
								Edit
							</button>

							<button class="btn btn-sm btn-danger delete-btn" 
									data-id="<?php echo $row['id']; ?>">
								Delete
							</button>
						</td>
                    </tr>
                    <?php endwhile; ?>
                </tbody>
            </table>
        </div>
    </div>

    <!-- Add Package Modal -->
    <div class="modal fade" id="addPackageModal">
        <div class="modal-dialog">
            <div class="modal-content">
                <form action="add_package.php" method="POST">
                    <div class="modal-header">
                        <h5 class="modal-title">Add New Tour Package</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <div class="mb-3">
                            <label>Package Name</label>
                            <input type="text" name="name" class="form-control" required>
                        </div>
                        <div class="mb-3">
                            <label>Description</label>
                            <textarea name="description" class="form-control" rows="3" required></textarea>
                        </div>
                        <div class="mb-3">
                            <label>Route Points</label>
                            <textarea name="route_points" class="form-control" rows="3" required></textarea>
                        </div>
						<div class="mb-3">
    <label>Dropoff Latitude</label>
    <input type="text" name="dropoff_latitude" class="form-control" placeholder="Enter latitude" required>
</div>
<div class="mb-3">
    <label>Dropoff Longitude</label>
    <input type="text" name="dropoff_longitude" class="form-control" placeholder="Enter longitude" required>
</div>
                        <div class="mb-3">
                            <label>Duration (minutes)</label>
                            <input type="number" name="duration" class="form-control" required>
                        </div>
                        <div class="mb-3">
                            <label>Price</label>
                            <input type="number" step="0.01" name="price" class="form-control" required>
                        </div>
						
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                        <button type="submit" class="btn btn-primary">Save Package</button>
                    </div>
                </form>
            </div>
        </div>
    </div>


	<!-- Edit Package Modal -->
<div class="modal fade" id="editPackageModal">
    <div class="modal-dialog">
        <div class="modal-content">
            <form action="edit_package.php" method="POST">
                <div class="modal-header">
                    <h5 class="modal-title">Edit Tour Package</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <input type="hidden" name="id" id="edit-id">
                    <div class="mb-3">
                        <label>Package Name</label>
                        <input type="text" name="name" id="edit-name" class="form-control" required>
                    </div>
                    <div class="mb-3">
                        <label>Description</label>
                        <textarea name="description" id="edit-description" class="form-control" rows="3" required></textarea>
                    </div>
                    <div class="mb-3">
                        <label>Duration (minutes)</label>
                        <input type="number" name="duration" id="edit-duration" class="form-control" required>
                    </div>
                    <div class="mb-3">
                        <label>Price</label>
                        <input type="number" step="0.01" name="price" id="edit-price" class="form-control" required>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                    <button type="submit" class="btn btn-primary">Update Package</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
    document.addEventListener("DOMContentLoaded", function () {
        // Handle Edit Button Click
        document.querySelectorAll(".edit-btn").forEach(button => {
            button.addEventListener("click", function () {
                document.getElementById("edit-id").value = this.getAttribute("data-id");
                document.getElementById("edit-name").value = this.getAttribute("data-name");
                document.getElementById("edit-description").value = this.getAttribute("data-description");
                document.getElementById("edit-duration").value = this.getAttribute("data-duration");
                document.getElementById("edit-price").value = this.getAttribute("data-price");
            });
        });

        // Handle Delete Button Click
        document.querySelectorAll(".delete-btn").forEach(button => {
            button.addEventListener("click", function () {
                let packageId = this.getAttribute("data-id");
                if (confirm("Are you sure you want to delete this package?")) {
                    window.location.href = "delete_package.php?id=" + packageId;
                }
            });
        });
    });
</script>


    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>