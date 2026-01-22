#!/bin/bash

# Test script for Cart Features JSON responses
# Usage: ./test_cart_json.sh

BASE_URL="http://localhost:8080/MT_movie_project_war/api"
TIMEOUT=3

echo "=========================================="
echo "Testing Cart Features JSON Responses"
echo "=========================================="
echo ""

# Test 1: GET /api/shopping_cart (empty cart)
echo "=== Test 1: GET /api/shopping_cart (Empty Cart) ==="
curl -s --max-time $TIMEOUT -X GET \
  -H "Content-Type: application/json" \
  "$BASE_URL/shopping_cart" | python3 -m json.tool 2>/dev/null || echo "❌ Failed or invalid JSON"
echo ""
echo ""

# Test 2: POST /api/shopping_cart (add item)
echo "=== Test 2: POST /api/shopping_cart (Add Movie) ==="
curl -s --max-time $TIMEOUT -X POST \
  -H "Content-Type: application/json" \
  -d '{"id":"tt1234567","title":"Test Movie","quantity":1}' \
  "$BASE_URL/shopping_cart" | python3 -m json.tool 2>/dev/null || echo "❌ Failed or invalid JSON"
echo ""
echo ""

# Test 3: GET /api/shopping_cart (with items)
echo "=== Test 3: GET /api/shopping_cart (With Items) ==="
curl -s --max-time $TIMEOUT -X GET \
  -H "Content-Type: application/json" \
  "$BASE_URL/shopping_cart" | python3 -m json.tool 2>/dev/null || echo "❌ Failed or invalid JSON"
echo ""
echo ""

# Test 4: PUT /api/shopping_cart (increase quantity)
echo "=== Test 4: PUT /api/shopping_cart (Increase Quantity) ==="
curl -s --max-time $TIMEOUT -X PUT \
  -H "Content-Type: application/json" \
  -d '{"movieId":"tt1234567","action":"increase"}' \
  "$BASE_URL/shopping_cart" | python3 -m json.tool 2>/dev/null || echo "❌ Failed or invalid JSON"
echo ""
echo ""

# Test 5: DELETE /api/shopping_cart (remove item)
echo "=== Test 5: DELETE /api/shopping_cart (Remove Item) ==="
curl -s --max-time $TIMEOUT -X DELETE \
  -H "Content-Type: application/json" \
  "$BASE_URL/shopping_cart?movieId=tt1234567" | python3 -m json.tool 2>/dev/null || echo "❌ Failed or invalid JSON"
echo ""
echo ""

# Test 6: POST /api/payment (test payment endpoint)
echo "=== Test 6: POST /api/payment (Payment) ==="
curl -s --max-time $TIMEOUT -X POST \
  -H "Content-Type: application/json" \
  -d '{"customerId":"test123","movieIds":["tt1234567","tt7654321"],"saleDate":"2025-11-22"}' \
  "$BASE_URL/payment" | python3 -m json.tool 2>/dev/null || echo "❌ Failed or invalid JSON"
echo ""
echo ""

echo "=========================================="
echo "Testing Complete"
echo "=========================================="

