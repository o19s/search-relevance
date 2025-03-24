#!/bin/bash -e

curl -s "http://localhost:9200/srw_metrics/_search" | jq
