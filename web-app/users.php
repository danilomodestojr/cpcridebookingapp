<?php
session_start();
require_once 'config.php';

if (!isset($_SESSION['admin_logged_in'])) {
    header("Location: index.php");
    exit();
}

// Pagination
$limit = 12;
$page = isset($_GET['page']) ? (int)$_GET['page'] : 1;
$offset = ($page - 1) * $limit;

// Sorting with SQL Injection Protection
$allowed_columns = ['id', 'first_name', 'role', 'contact_number', 'created_at'];
$sort_column = isset($_GET['sort']) && in_array($_GET['sort'], $allowed_columns) ? $_GET['sort'] : 'created_at';
$sort_order = isset($_GET['order']) && in_array($_GET['order'], ['ASC', 'DESC']) ? $_GET['order'] : 'DESC';

// Get total records for pagination
$total_records = mysqli_fetch_assoc(mysqli_query($db, "SELECT COUNT(*) as count FROM users"))['count'];
$total_pages = ceil($total_records / $limit);

$sql = "SELECT * FROM users ORDER BY $sort_column $sort_order LIMIT $offset, $limit";
$result = mysqli_query($db, $sql);

function getSortIcon($column) {
    global $sort_column, $sort_order;
    if ($sort_column == $column) {
        return $sort_order == 'ASC' ? '↑' : '↓';
    }
    return '';
}

function getSortUrl($column) {
    global $sort_column, $sort_order, $page;
    $new_order = ($sort_column == $column && $sort_order == 'ASC') ? 'DESC' : 'ASC';
    return "?sort=$column&order=$new_order&page=$page";
}
?>
<!DOCTYPE html>
<html>
<head>
    <title>TrikeSafe Admin - Users</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <?php include 'navbar.php'; ?>
    <div class="container mt-4">
        <h2>Users</h2>
        <div class="table-responsive">
            <table class="table table-striped">
                <thead>
                    <tr>
                        <th><a href="<?php echo getSortUrl('id'); ?>">ID <?php echo getSortIcon('id'); ?></a></th>
                        <th><a href="<?php echo getSortUrl('first_name'); ?>">Name <?php echo getSortIcon('first_name'); ?></a></th>
                        <th><a href="<?php echo getSortUrl('role'); ?>">Role <?php echo getSortIcon('role'); ?></a></th>
                        <th><a href="<?php echo getSortUrl('contact_number'); ?>">Contact <?php echo getSortIcon('contact_number'); ?></a></th>
                        <th>License/Vehicle</th>
                        <th><a href="<?php echo getSortUrl('created_at'); ?>">Joined <?php echo getSortIcon('created_at'); ?></a></th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <?php while ($row = mysqli_fetch_assoc($result)): ?>
                        <tr>
                            <td><?php echo $row['id']; ?></td>
                            <td><?php echo $row['first_name'] . ' ' . $row['last_name']; ?></td>
                            <td><?php echo ucfirst($row['role']); ?></td>
                            <td><?php echo $row['contact_number']; ?></td>
                            <td>
                                <?php if ($row['role'] == 'driver'): ?>
                                    License: <?php echo $row['driver_license']; ?><br>
                                    Vehicle: <?php echo $row['tricycle_number']; ?>
                                <?php else: ?>
                                    N/A
                                <?php endif; ?>
                            </td>
                            <td><?php echo date('M d, Y', strtotime($row['created_at'])); ?></td>
                            <td>
                                <button class="btn btn-sm btn-warning edit-btn" 
									data-id="<?php echo $row['id']; ?>" 
									data-firstname="<?php echo $row['first_name']; ?>" 
									data-lastname="<?php echo $row['last_name']; ?>"
									data-role="<?php echo $row['role']; ?>" 
									data-contact="<?php echo $row['contact_number']; ?>"
									data-license="<?php echo $row['driver_license']; ?>" 
									data-vehicle="<?php echo $row['tricycle_number']; ?>">
									Edit
								</button>
                                <button class="btn btn-sm btn-danger delete-btn" data-id="<?php echo $row['id']; ?>">Delete</button>
                            </td>
                        </tr>
                    <?php endwhile; ?>
                </tbody>
            </table>
        </div>
        <nav>
            <ul class="pagination">
                <?php for ($i = 1; $i <= $total_pages; $i++): ?>
                    <li class="page-item <?php echo $i == $page ? 'active' : ''; ?>">
                        <a class="page-link" href="?page=<?php echo $i; ?>&sort=<?php echo $sort_column; ?>&order=<?php echo $sort_order; ?>"><?php echo $i; ?></a>
                    </li>
                <?php endfor; ?>
            </ul>
        </nav>
    </div>
	
<!-- Edit User Modal -->
<div class="modal fade" id="editUserModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <form id="editUserForm">
                <div class="modal-header">
                    <h5 class="modal-title">Edit User</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <input type="hidden" id="editUserId" name="id">
                    <div class="mb-3">
                        <label>First Name</label>
                        <input type="text" id="editUserFirstName" name="first_name" class="form-control" required>
                    </div>
                    <div class="mb-3">
                        <label>Last Name</label>
                        <input type="text" id="editUserLastName" name="last_name" class="form-control" required>
                    </div>
                    <div class="mb-3">
                        <label>Role</label>
                        <select id="editUserRole" name="role" class="form-control">
                            <option value="passenger">Passenger</option>
                            <option value="driver">Driver</option>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label>Contact</label>
                        <input type="text" id="editUserContact" name="contact" class="form-control">
                    </div>
                    <div id="editDriverFields">
                        <div class="mb-3">
                            <label>License</label>
                            <input type="text" id="editUserLicense" name="license" class="form-control">
                        </div>
                        <div class="mb-3">
                            <label>Vehicle</label>
                            <input type="text" id="editUserVehicle" name="vehicle" class="form-control">
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                    <button type="submit" class="btn btn-primary">Save Changes</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll(".delete-btn").forEach(button => {
        button.addEventListener("click", function () {
            const userId = this.dataset.id;
            if (confirm("Are you sure you want to delete this user?")) {
                fetch("delete_user.php", {
                    method: "POST",
                    headers: { "Content-Type": "application/x-www-form-urlencoded" },
                    body: "id=" + userId
                })
                .then(response => response.json())
                .then(data => {
                    alert(data.message);
                    if (data.success) location.reload();
                })
                .catch(error => console.error("Error:", error));
            }
        });
    });
});

document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll(".edit-btn").forEach(button => {
        button.addEventListener("click", function () {
            document.getElementById("editUserId").value = this.dataset.id;
            document.getElementById("editUserFirstName").value = this.dataset.firstname;
            document.getElementById("editUserLastName").value = this.dataset.lastname;
            document.getElementById("editUserRole").value = this.dataset.role;
            document.getElementById("editUserContact").value = this.dataset.contact;
            document.getElementById("editUserLicense").value = this.dataset.license || "";
            document.getElementById("editUserVehicle").value = this.dataset.vehicle || "";

            var myModal = new bootstrap.Modal(document.getElementById("editUserModal"));
            myModal.show();
        });
    });

    document.getElementById("editUserForm").addEventListener("submit", function (e) {
        e.preventDefault();
        fetch("edit_user.php", {
            method: "POST",
            body: new FormData(this)
        })
        .then(response => response.json())
        .then(data => {
            alert(data.message);
            if (data.success) location.reload();
        })
        .catch(error => console.error("Error:", error));
    });
});


</script>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
