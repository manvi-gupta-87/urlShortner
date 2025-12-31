#!/bin/bash
#
# BUILD ALL MICROSERVICES
# =======================
# This script builds all microservices in the correct dependency order.
#
# WHY THIS SCRIPT EXISTS:
# - Pure microservices architecture - no parent POM aggregator
# - Each service is independent and can be built separately
# - This script provides convenience for building all at once
#
# BUILD ORDER (dependency graph):
# 1. shared-library       - No dependencies, used by all services
# 2. auth-service         - Depends on shared-library
# 3. analytics-service    - Depends on shared-library
# 4. url-service          - Depends on shared-library, auth-service-lib, analytics-service-lib
# 5. api-gateway          - Depends on shared-library
# 6. service-discovery    - No dependencies (pure infrastructure)
#
# USAGE:
#   ./build-all.sh           # Build all services
#   ./build-all.sh --quick   # Skip tests for faster build
#

set -e  # Exit immediately if a command exits with a non-zero status

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check for --quick flag
SKIP_TESTS=""
if [ "$1" == "--quick" ]; then
    SKIP_TESTS="-DskipTests"
    echo -e "${YELLOW}Quick mode: Skipping tests${NC}"
fi

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "=========================================="
echo "Building URL Shortener Microservices"
echo "=========================================="
echo ""

# Function to build a service
build_service() {
    local service_name=$1
    local service_path=$2

    echo -e "${YELLOW}Building: $service_name${NC}"
    cd "$SCRIPT_DIR/$service_path"

    if mvn clean install $SKIP_TESTS; then
        echo -e "${GREEN}✓ $service_name built successfully${NC}"
        echo ""
    else
        echo -e "${RED}✗ $service_name build FAILED${NC}"
        exit 1
    fi
}

# Step 1: Build shared-library (no dependencies)
build_service "shared-library" "shared-library"

# Step 2: Build auth-service (depends on shared-library)
build_service "auth-service" "auth-service"

# Step 3: Build analytics-service (depends on shared-library)
build_service "analytics-service" "analytics-service"

# Step 4: Build url-service (depends on shared-library, auth-service-lib, analytics-service-lib)
build_service "url-service" "url-service"

# Step 5: Build api-gateway (depends on shared-library)
build_service "api-gateway" "api-gateway"

# Step 6: Build service-discovery (no dependencies)
build_service "service-discovery" "service-discovery"

echo "=========================================="
echo -e "${GREEN}All services built successfully!${NC}"
echo "=========================================="
echo ""
echo "Next steps:"
echo "  1. Start Docker: docker-compose up --build"
echo "  2. Or run individual services with: mvn spring-boot:run"
echo ""
