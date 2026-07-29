#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
HOTEL_ID="00000000-0000-0000-0000-000000000001"
CHECK_IN="${CHECK_IN:-2030-08-01}"
CHECK_OUT="${CHECK_OUT:-2030-08-03}"

curl_retry=(
  --fail
  --silent
  --show-error
  --retry 18
  --retry-delay 5
  --retry-all-errors
  --max-time 120
)

health_response="$(curl "${curl_retry[@]}" "${BASE_URL}/actuator/health")"
[[ "$health_response" == *'"status":"UP"'* ]]

openapi_response="$(curl "${curl_retry[@]}" "${BASE_URL}/v3/api-docs")"
[[ "$openapi_response" == *'"Hotel Booking API"'* ]]

swagger_response="$(curl "${curl_retry[@]}" "${BASE_URL}/swagger-ui/index.html")"
[[ "$swagger_response" == *'Swagger UI'* ]]

curl "${curl_retry[@]}" \
  --request POST \
  "${BASE_URL}/api/admin/reset" \
  >/dev/null
seed_response="$(curl "${curl_retry[@]}" \
  --request POST \
  "${BASE_URL}/api/admin/seed")"
[[ "$seed_response" == *'"roomsCreated":6'* ]]

hotel_response="$(curl "${curl_retry[@]}" "${BASE_URL}/api/hotels?name=Grand")"
[[ "$hotel_response" == *'"Grand Plaza Hotel"'* ]]

availability_response="$(curl "${curl_retry[@]}" \
  "${BASE_URL}/api/hotels/${HOTEL_ID}/rooms/available?checkIn=${CHECK_IN}&checkOut=${CHECK_OUT}&guests=2&roomType=DOUBLE")"
[[ "$availability_response" == *'"roomNumber":"201"'* ]]

booking_response="$(curl "${curl_retry[@]}" \
  --request POST \
  --header 'Content-Type: application/json' \
  --data "{
    \"hotelId\": \"${HOTEL_ID}\",
    \"guestName\": \"Ada Lovelace\",
    \"guestCount\": 2,
    \"checkInDate\": \"${CHECK_IN}\",
    \"checkOutDate\": \"${CHECK_OUT}\",
    \"roomType\": \"DOUBLE\"
  }" \
  "${BASE_URL}/api/bookings")"

booking_reference="$(printf '%s' "$booking_response" \
  | sed -n 's/.*"bookingReference":"\([^"]*\)".*/\1/p')"
[[ "$booking_reference" == HB-* ]]

booking_lookup_response="$(curl "${curl_retry[@]}" \
  "${BASE_URL}/api/bookings/${booking_reference}")"
[[ "$booking_lookup_response" == *"\"bookingReference\":\"${booking_reference}\""* ]]

printf 'API smoke test passed: %s (%s)\n' "$BASE_URL" "$booking_reference"
