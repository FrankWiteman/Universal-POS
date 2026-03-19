#!/bin/bash
# ============================================================
#  UniversalPOS — JWT Token Helper (Mac / Linux)
#
#  Usage:
#    source scripts/login.sh                    # uses defaults
#    source scripts/login.sh myemail@x.com MyPass my-store
#
#  After running, $TOKEN is set in your shell session.
#  Use it like: curl -H "Authorization: Bearer $TOKEN" ...
#
#  Requires: curl, jq  (install jq: brew install jq / apt install jq)
# ============================================================

EMAIL="${1:-admin@universalpos.local}"
PASSWORD="${2:-ChangeMe123!}"
TENANT="${3:-demo-store}"
BASE_URL="${4:-http://localhost:8080/api}"

echo "Logging in as $EMAIL @ $TENANT..."

RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"tenantSlug\":\"$TENANT\"}")

# Check for success
SUCCESS=$(echo "$RESPONSE" | jq -r '.success' 2>/dev/null)

if [ "$SUCCESS" != "true" ]; then
  echo "Login failed:"
  echo "$RESPONSE" | jq . 2>/dev/null || echo "$RESPONSE"
  return 1
fi

# Extract token
export TOKEN=$(echo "$RESPONSE" | jq -r '.data.token')
export ROLE=$(echo "$RESPONSE" | jq -r '.data.role')
export EMPLOYEE=$(echo "$RESPONSE" | jq -r '.data.employeeName')

echo "✅ Logged in: $EMPLOYEE ($ROLE)"
echo "   Token saved to \$TOKEN"
echo ""
echo "Usage example:"
echo "   curl -H \"Authorization: Bearer \$TOKEN\" $BASE_URL/customers/search?q=Jane"
