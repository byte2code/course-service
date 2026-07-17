#!/bin/bash
# Load testing script using Apache Bench (ab)
# Ensures that performance baselines are established for the Course Service.

HOST="http://localhost:8081"
ENDPOINT="/courses"
CONCURRENCY=50
REQUESTS=1000

echo "Starting load test on $HOST$ENDPOINT..."
echo "Concurrency: $CONCURRENCY"
echo "Total Requests: $REQUESTS"
echo "----------------------------------------"

ab -n $REQUESTS -c $CONCURRENCY $HOST$ENDPOINT

echo "----------------------------------------"
echo "Note: Document the P50, P95, and P99 latency along with TPS in the README.md"
