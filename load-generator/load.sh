#!/bin/sh

set -eu

BASE_URL="${BASE_URL:-http://telemetry-demo-service:8080}"

PROFILE="${PROFILE:-normal}"

PROFILE_ROTATION_ENABLED="${PROFILE_ROTATION_ENABLED:-true}"
PROFILE_ROTATE_SECONDS="${PROFILE_ROTATE_SECONDS:-300}"
PROFILE_SEQUENCE="${PROFILE_SEQUENCE:-normal mixed degraded}"

SLEEP_SECONDS="${SLEEP_SECONDS:-1}"
BATCH_SIZE="${BATCH_SIZE:-3}"
REQUEST_TIMEOUT_SECONDS="${REQUEST_TIMEOUT_SECONDS:-10}"

NORMAL_BATCH_SIZE="${NORMAL_BATCH_SIZE:-$BATCH_SIZE}"
MIXED_BATCH_SIZE="${MIXED_BATCH_SIZE:-40}"
DEGRADED_BATCH_SIZE="${DEGRADED_BATCH_SIZE:-120}"

NORMAL_SLEEP_SECONDS="${NORMAL_SLEEP_SECONDS:-1}"
MIXED_SLEEP_SECONDS="${MIXED_SLEEP_SECONDS:-0.2}"
DEGRADED_SLEEP_SECONDS="${DEGRADED_SLEEP_SECONDS:-0.05}"

BURST_ENABLED="${BURST_ENABLED:-false}"
BURST_EVERY_ITERATIONS="${BURST_EVERY_ITERATIONS:-10}"
BURST_REQUESTS="${BURST_REQUESTS:-80}"
BURST_PROFILES="${BURST_PROFILES:-mixed degraded}"

SERVICE_NAME="${SERVICE_NAME:-telemetry-load-generator}"

iteration=0
total_requests=0
profile_started_at="$(date +%s)"

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"
}

random_0_99() {
  if command -v od >/dev/null 2>&1; then
    n="$(od -An -N2 -tu2 /dev/urandom | tr -d ' ')"
    echo $((n % 100))
  else
    awk 'BEGIN{srand(); print int(rand() * 100)}'
  fi
}

is_supported_profile() {
  candidate="$1"

  for profile in $PROFILE_SEQUENCE; do
    if [ "$profile" = "$candidate" ]; then
      return 0
    fi
  done

  return 1
}

next_profile() {
  current="$PROFILE"
  previous=""
  first=""
  found_current="false"

  for profile in $PROFILE_SEQUENCE; do
    if [ -z "$first" ]; then
      first="$profile"
    fi

    if [ "$found_current" = "true" ]; then
      PROFILE="$profile"
      profile_started_at="$(date +%s)"
      log "PROFILE switched from $current to $PROFILE"
      return
    fi

    if [ "$profile" = "$current" ]; then
      found_current="true"
    fi

    previous="$profile"
  done

  PROFILE="$first"
  profile_started_at="$(date +%s)"
  log "PROFILE switched from $current to $PROFILE"
}

profile_batch_size() {
  case "$PROFILE" in
    normal)
      echo "$NORMAL_BATCH_SIZE"
      ;;
    mixed)
      echo "$MIXED_BATCH_SIZE"
      ;;
    degraded)
      echo "$DEGRADED_BATCH_SIZE"
      ;;
    *)
      echo "$BATCH_SIZE"
      ;;
  esac
}

profile_sleep_seconds() {
  case "$PROFILE" in
    normal)
      echo "$NORMAL_SLEEP_SECONDS"
      ;;
    mixed)
      echo "$MIXED_SLEEP_SECONDS"
      ;;
    degraded)
      echo "$DEGRADED_SLEEP_SECONDS"
      ;;
    *)
      echo "$SLEEP_SECONDS"
      ;;
  esac
}

is_burst_profile() {
  for profile in $BURST_PROFILES; do
    if [ "$profile" = "$PROFILE" ]; then
      return 0
    fi
  done

  return 1
}

rotate_profile_if_needed() {
  if [ "$PROFILE_ROTATION_ENABLED" != "true" ]; then
    return
  fi

  if [ "$PROFILE_ROTATE_SECONDS" -le 0 ]; then
    return
  fi

  now="$(date +%s)"
  elapsed=$((now - profile_started_at))

  if [ "$elapsed" -ge "$PROFILE_ROTATE_SECONDS" ]; then
    next_profile
  fi
}

hit() {
  path="$1"
  scenario="$2"

  correlation_id="$SERVICE_NAME-$(date +%s)-$iteration-$(random_0_99)"
  trace_id="$(date +%s)$(random_0_99)$(random_0_99)"

  code="$(
    curl \
      -s \
      -o /dev/null \
      -w "%{http_code}" \
      --max-time "$REQUEST_TIMEOUT_SECONDS" \
      -H "X-Correlation-Id: $correlation_id" \
      -H "X-B3-TraceId: $trace_id" \
      -H "X-Demo-Scenario: $scenario" \
      "$BASE_URL$path" || true
  )"

  case "$code" in
    2*|3*)
      log "OK       code=$code path=$path scenario=$scenario profile=$PROFILE"
      ;;
    4*|5*)
      log "ERROR    code=$code path=$path scenario=$scenario profile=$PROFILE"
      ;;
    000|000*)
      log "TIMEOUT  code=$code path=$path scenario=$scenario profile=$PROFILE"
      ;;
    *)
      log "UNKNOWN  code=$code path=$path scenario=$scenario profile=$PROFILE"
      ;;
  esac
}

hit_background() {
  path="$1"
  scenario="$2"

  total_requests=$((total_requests + 1))

  (
    hit "$path" "$scenario"
  ) &
}

run_normal_request() {
  n="$(random_0_99)"

  if [ "$n" -lt 35 ]; then
    hit_background "/test" "normal-http"
  elif [ "$n" -lt 65 ]; then
    hit_background "/payment" "normal-payment"
  elif [ "$n" -lt 85 ]; then
    hit_background "/orders/1" "normal-order-by-id"
  elif [ "$n" -lt 95 ]; then
    hit_background "/business" "normal-business"
  else
    hit_background "/manual/track-void" "normal-manual-track-void"
  fi
}

run_mixed_request() {
  n="$(random_0_99)"

  if [ "$n" -lt 22 ]; then
    hit_background "/test" "mixed-stable"
  elif [ "$n" -lt 47 ]; then
    hit_background "/payment" "mixed-payment"
  elif [ "$n" -lt 70 ]; then
    hit_background "/business" "mixed-business"
  elif [ "$n" -lt 80 ]; then
    hit_background "/orders/1" "mixed-order-by-id"
  elif [ "$n" -lt 90 ]; then
    hit_background "/flaky" "mixed-flaky"
  elif [ "$n" -lt 97 ]; then
    hit_background "/slow" "mixed-slow"
  else
    hit_background "/manual/error" "mixed-manual-error"
  fi
}

run_degraded_request() {
  n="$(random_0_99)"

  if [ "$n" -lt 15 ]; then
    hit_background "/test" "degraded-baseline"
  elif [ "$n" -lt 35 ]; then
    hit_background "/payment" "degraded-payment"
  elif [ "$n" -lt 60 ]; then
    hit_background "/business" "degraded-business"
  elif [ "$n" -lt 68 ]; then
    hit_background "/orders/1" "degraded-order-by-id"
  elif [ "$n" -lt 82 ]; then
    hit_background "/flaky" "degraded-flaky"
  elif [ "$n" -lt 96 ]; then
    hit_background "/slow" "degraded-slow"
  else
    hit_background "/fail" "degraded-fail"
  fi
}

run_burst() {
  if [ "$BURST_ENABLED" != "true" ]; then
    return
  fi

  if ! is_burst_profile; then
    return
  fi

  if [ "$BURST_EVERY_ITERATIONS" -le 0 ]; then
    return
  fi

  if [ $((iteration % BURST_EVERY_ITERATIONS)) -ne 0 ]; then
    return
  fi

  log "BURST started requests=$BURST_REQUESTS profile=$PROFILE"

  i=0
  while [ "$i" -lt "$BURST_REQUESTS" ]; do
    case "$PROFILE" in
      mixed)
        hit_background "/slow" "burst-mixed-slow"
        hit_background "/flaky" "burst-mixed-flaky"
        ;;
      degraded)
        hit_background "/slow" "burst-degraded-slow"
        hit_background "/fail" "burst-degraded-fail"
        ;;
      *)
        hit_background "/test" "burst-stable"
        ;;
    esac
    i=$((i + 1))
  done

  wait

  log "BURST finished profile=$PROFILE"
}

run_iteration() {
  i=0
  current_batch_size="$(profile_batch_size)"

  while [ "$i" -lt "$current_batch_size" ]; do
    case "$PROFILE" in
      normal)
        run_normal_request
        ;;
      mixed)
        run_mixed_request
        ;;
      degraded)
        run_degraded_request
        ;;
      *)
        log "Unknown PROFILE=$PROFILE. Supported profiles are: $PROFILE_SEQUENCE"
        exit 1
        ;;
    esac

    i=$((i + 1))
  done

  wait
}

if ! is_supported_profile "$PROFILE"; then
  log "Initial PROFILE=$PROFILE is not in PROFILE_SEQUENCE=$PROFILE_SEQUENCE"
  log "Using first profile from PROFILE_SEQUENCE"
  PROFILE="$(echo "$PROFILE_SEQUENCE" | awk '{print $1}')"
fi

log "Starting telemetry load generator"
log "BASE_URL=$BASE_URL"
log "PROFILE=$PROFILE"
log "PROFILE_ROTATION_ENABLED=$PROFILE_ROTATION_ENABLED"
log "PROFILE_ROTATE_SECONDS=$PROFILE_ROTATE_SECONDS"
log "PROFILE_SEQUENCE=$PROFILE_SEQUENCE"
log "SLEEP_SECONDS=$SLEEP_SECONDS"
log "BATCH_SIZE=$BATCH_SIZE"
log "NORMAL_BATCH_SIZE=$NORMAL_BATCH_SIZE"
log "MIXED_BATCH_SIZE=$MIXED_BATCH_SIZE"
log "DEGRADED_BATCH_SIZE=$DEGRADED_BATCH_SIZE"
log "NORMAL_SLEEP_SECONDS=$NORMAL_SLEEP_SECONDS"
log "MIXED_SLEEP_SECONDS=$MIXED_SLEEP_SECONDS"
log "DEGRADED_SLEEP_SECONDS=$DEGRADED_SLEEP_SECONDS"
log "REQUEST_TIMEOUT_SECONDS=$REQUEST_TIMEOUT_SECONDS"
log "BURST_ENABLED=$BURST_ENABLED"
log "BURST_EVERY_ITERATIONS=$BURST_EVERY_ITERATIONS"
log "BURST_REQUESTS=$BURST_REQUESTS"
log "BURST_PROFILES=$BURST_PROFILES"

while true; do
  iteration=$((iteration + 1))

  rotate_profile_if_needed

  run_iteration
  run_burst

  if [ $((iteration % 10)) -eq 0 ]; then
    log "STATS iteration=$iteration totalRequestsApprox=$total_requests profile=$PROFILE"
  fi

  sleep "$(profile_sleep_seconds)"
done
