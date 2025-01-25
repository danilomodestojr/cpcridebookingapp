<?php
session_start();
require_once 'config.php';

if(!isset($_SESSION['admin_logged_in'])) {
    header("Location: index.php");
    exit();
}

// Pagination
$limit = 20;
$page = isset($_GET['page']) ? (int)$_GET['page'] : 1;
$offset = ($page - 1) * $limit;

// Sorting
$sort_column = isset($_GET['sort']) ? mysqli_real_escape_string($db, $_GET['sort']) : 'created_at';
$sort_order = isset($_GET['order']) ? mysqli_real_escape_string($db, $_GET['order']) : 'DESC';
$allowed_columns = ['id', 'first_name', 'role', 'contact_number', 'created_at'];

if (!in_array($sort_column, $allowed_columns)) {
    $sort_column = 'created_at';
}

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
    <style>
        .sortable { cursor: pointer; }
    </style>
</head>
<body>
    <?php include 'navbar.php'; ?>
    <div class="container mt-4">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h2>Users</h2>
        </div>
        <div class="table-responsive">
            <table class="table table-striped">
                <thead>
                    <tr>
                        <th class="sortable"><a href="<?php echo getSortUrl('id'); ?>" class="text-dark text-decoration-none">ID <?php echo getSortIcon('id'); ?></a></th>
                        <th class="sortable"><a href="<?php echo getSortUrl('first_name'); ?>" class="text-dark text-decoration-none">Name <?php echo getSortIcon('first_name'); ?></a></th>
                        <th class="sortable"><a href="<?php echo getSortUrl('role'); ?>" class="text-dark text-decoration-none">Role <?php echo getSortIcon('role'); ?></a></th>
                        <th class="sortable"><a href="<?php echo getSortUrl('contact_number'); ?>" class="text-dark text-decoration-none">Contact <?php echo getSortIcon('contact_number'); ?></a></th>
                        <th>License/Vehicle</th>
                        <th class="sortable"><a href="<?php echo getSortUrl('created_at'); ?>" class="text-dark text-decoration-none">Joined <?php echo getSortIcon('created_at'); ?></a></th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
    <?php if(mysqli_num_rows($result) > 0): ?>
        <?php while($row = mysqli_fetch_assoc($result)): ?>
        <tr>
            <td><?php echo $row['id']; ?></td>
            <td><?php echo $row['first_name'] . ' ' . $row['last_name']; ?></td>
            <td><?php echo ucfirst($row['role']); ?></td>
            <td><?php echo $row['contact_number']; ?></td>
            <td>
                <?php if($row['role'] == 'driver'): ?>
                    License: <?php echo $row['driver_license']; ?><br>
                    Vehicle: <?php echo $row['tricycle_number']; ?>
                <?php else: ?>
                    N/A
                <?php endif; ?>
            </td>
            <td><?php echo date('M d, Y', strtotime($row['created_at'])); ?></td>
            <td>
                <button class="btn btn-sm btn-warning">Edit</button>
                <button class="btn btn-sm btn-danger">Delete</button>
            </td>
        </tr>
        <?php endwhile; ?>
    <?php else: ?>
        <tr><td colspan="7" class="text-center">No users found</td></tr>
    <?php endif; ?>
</tbody>
            </table>
        </div>
        
        <!-- Pagination -->
        <nav>
            <ul class="pagination">
                <?php for($i = 1; $i <= $total_pages; $i++): ?>
                    <li class="page-item <?php echo $i == $page ? 'active' : ''; ?>">
                        <a class="page-link" href="?page=<?php echo $i; ?>&sort=<?php echo $sort_column; ?>&order=<?php echo $sort_order; ?>"><?php echo $i; ?></a>
                    </li>
                <?php endfor; ?>
            </ul>
        </nav>
    </div>
</body>
</html>