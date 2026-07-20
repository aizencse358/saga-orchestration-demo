#!/bin/sh
set -eu

exec java -jar app.jar \
  "--tier=${LOADGEN_TIER:-custom}" \
  "--base-url=${LOADGEN_BASE_URL:-http://orchestrator-custom:8090}" \
  "--requests=${LOADGEN_REQUESTS:-100}" \
  "--concurrency=${LOADGEN_CONCURRENCY:-10}" \
  "--failure-rate=${LOADGEN_FAILURE_RATE:-0.0}" \
  "--output=results/${LOADGEN_TIER:-custom}-${LOADGEN_RUN_ID:-manual}.csv"
