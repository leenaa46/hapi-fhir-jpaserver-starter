#!/bin/bash

echo "Cleaning up Docker resources..."

# Stop any running containers (optional, uncomment if needed)
# docker-compose down

# Remove unused containers, networks, images and volumes
echo "Pruning unused Docker resources..."
docker system prune -f

# Remove dangling images (images with <none> tag)
echo "Removing dangling images..."
docker image prune -f

# Remove unused volumes
echo "Pruning volumes..."
docker volume prune -f

# Show remaining disk space
echo "Current disk space:"
df -h

echo "Docker cleanup completed."
echo "Now you can rebuild with: docker-compose up -d --build"

# Make the script executable after saving with: chmod +x docker-cleanup.sh
