#!/bin/bash -e

curl -s -X DELETE http://localhost:9200/ubi_queries
curl -s -X DELETE http://localhost:9200/ubi_events

curl -s -X PUT "localhost:9200/ubi_queries" -H 'Content-Type: application/json' -d'
{
  "mappings": {
    "properties": {
      "timestamp": { "type": "date", "format": "strict_date_time" },
      "query_id": { "type": "keyword", "ignore_above": 100 },
      "query": { "type": "text" },
      "query_response_id": { "type": "keyword", "ignore_above": 100 },
      "query_response_hit_ids": { "type": "keyword" },
      "user_query": { "type": "keyword" },
      "query_attributes": { "type": "flat_object" },
      "client_id": { "type": "keyword", "ignore_above": 100 },
      "application": { "type": "keyword", "ignore_above": 100 }
    }
  }
}
' | jq

curl -s -X PUT "localhost:9200/ubi_events" -H 'Content-Type: application/json' -d'
{
  "mappings": {
    "properties": {
      "application": { "type": "keyword", "ignore_above": 256 },
      "action_name": { "type": "keyword", "ignore_above": 100 },
      "client_id": { "type": "keyword", "ignore_above": 100 },
      "query_id": { "type": "keyword", "ignore_above": 100 },
      "message": { "type": "keyword", "ignore_above": 1024 },
      "message_type": { "type": "keyword", "ignore_above": 100 },
      "user_query":  { "type": "keyword" },
      "timestamp": {
        "type": "date",
        "format":"strict_date_time",
        "ignore_malformed": true,
        "doc_values": true
      },
      "event_attributes": {
        "dynamic": true,
        "properties": {
          "position": {
            "properties": {
              "ordinal": { "type": "integer" },
              "x": { "type": "integer" },
              "y": { "type": "integer" },
              "page_depth": { "type": "integer" },
              "scroll_depth": { "type": "integer" },
              "trail": { "type": "text",
                "fields": { "keyword": { "type": "keyword", "ignore_above": 256 }
                }
              }
            }
          },
          "object": {
            "properties": {
              "internal_id": { "type": "keyword" },
              "object_id": { "type": "keyword", "ignore_above": 256 },
              "object_id_field": { "type": "keyword", "ignore_above": 100 },
              "name": { "type": "keyword", "ignore_above": 256 },
              "description": { "type": "text",
                "fields": { "keyword": { "type": "keyword", "ignore_above": 256 } }
              },
              "object_detail": { "type": "object" }
            }
          }
        }
      }
    }
  }
}
' | jq