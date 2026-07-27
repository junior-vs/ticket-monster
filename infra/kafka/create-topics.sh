#!/usr/bin/env bash
set -euo pipefail

BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVERS:-kafka:9092}"

create_topic() {
  local topic="$1"
  local partitions="$2"
  kafka-topics.sh \
    --bootstrap-server "${BOOTSTRAP_SERVER}" \
    --create \
    --if-not-exists \
    --topic "${topic}" \
    --partitions "${partitions}" \
    --replication-factor 1
}

create_topic "catalog-events" 3
create_topic "booking-events" 6
create_topic "seat-reservation-events" 6
create_topic "booking-events-dlq" 3
create_topic "seat-reservation-events-dlq" 3

kafka-topics.sh --bootstrap-server "${BOOTSTRAP_SERVER}" --list
