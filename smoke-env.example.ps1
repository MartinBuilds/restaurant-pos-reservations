# Copy once:  Copy-Item .\smoke-env.example.ps1 .\smoke-env.ps1
# Then edit smoke-env.ps1 and set DB_PASSWORD to your local MySQL password.
# smoke-env.ps1 is gitignored — never commit it.

$env:DB_URL = "jdbc:mysql://localhost:3306/restaurant_management"
$env:DB_USERNAME = "restaurant_app"
$env:DB_PASSWORD = "<your-mysql-password>"

$env:INITIAL_ADMIN_EMAIL = "maria.adminova@example.com"
$env:INITIAL_ADMIN_PASSWORD = "SecurePassword123!"
$env:INITIAL_ADMIN_FULL_NAME = "Мария Админова"

# Shared password for seeded waiter / cook / demo client (with demo profile)
$env:DEMO_USER_PASSWORD = "SecurePassword123!"
