<nav class="navbar navbar-expand-lg navbar-dark bg-primary">
    <div class="container-fluid">
        <a class="navbar-brand" href="#">TrikeSafe Admin</a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarNav">
            <ul class="navbar-nav me-auto">
                <li class="nav-item">
                    <a class="nav-link" href="dashboard.php">Dashboard</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="users.php">Users</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="bookings.php">Bookings</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="tour_packages.php">Tour Packages</a>
                </li>
				
            </ul>
			<ul class="navbar-nav me-right">
			<li class="nav-item">
    <a class="nav-link" href="fare_settings.php">Fare Settings</a>
</li>
</ul>
            <form class="d-flex" action="logout.php" method="POST">
                <button class="btn btn-light" type="submit">Logout</button>
            </form>
        </div>
    </div>
</nav>