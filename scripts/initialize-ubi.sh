#!/bin/bash -e

curl -s -X DELETE http://localhost:9200/ubi_queries,ubi_events

curl -s -X POST http://localhost:9200/_plugins/ubi/initialize
