#!/bin/bash

set -e

echo "Starting Docker cleanup process..."

# Stop running containers from the current directory
if [ -f docker-compose.yml ]; then
  echo "Stopping containers defined in docker-compose.yml..."
  docker-compose down --remove-orphans
else
  echo "No docker-compose.yml found in current directory, skipping container shutdown."
fi

# Free up disk space by removing unused Docker resources
echo "Pruning unused Docker resources..."
docker system prune -f

# Remove all dangling images (images with <none> tag)
echo "Removing dangling images..."
docker image prune -f

# Remove unused volumes
echo "Pruning volumes (except named volumes that are still in use)..."
docker volume prune -f

# Remove unused networks
echo "Pruning networks..."
docker network prune -f

# Display Docker resource usage after cleanup
echo "Current Docker disk usage after cleanup:"
docker system df

# Show remaining disk space
echo "Current system disk space:"
df -h | grep -E '(Filesystem|/dev/|tmpfs)'

echo "Docker cleanup completed successfully."
echo "You can now rebuild containers with: docker-compose up -d --build"

# Make the script executable after saving with: chmod +x docker-cleanup.sh
