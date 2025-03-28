# SPDX-License-Identifier: Apache-2.0
#
# The OpenSearch Contributors require contributions made to
# this file be licensed under the Apache-2.0 license or a
# compatible open source license.

#!/bin/bash -e

QUERY_SET_ID="705cdaf2-2ac1-4e37-913b-7755b04c028e"
JUDGMENTS_ID="6f888334-2bdb-4738-a464-d7d813c6a698"
INDEX="ecommerce"
ID_FIELD="asin"
K="50"
THRESHOLD="1.0" # Default value

curl -s -X DELETE "http://localhost:9200/srw_metrics"

# Keyword search
curl -s -X POST "http://localhost:9200/_plugins/search_relevance/experiments?id=${QUERY_SET_ID}&judgments_id=${JUDGMENTS_ID}&index=${INDEX}&id_field=${ID_FIELD}&k=${K}" \
   -H "Content-Type: application/json" \
    --data-binary '{
                      "multi_match": {
                        "query": "#$query##",
                        "fields": ["id", "title", "category", "bullets", "description", "attrs.Brand", "attrs.Color"]
                      }
                  }'

## Neural search
#curl -s -X POST "http://localhost:9200/_plugins/search_relevance/experiments?id=${QUERY_SET_ID}&judgments_id=${JUDGMENTS_ID}&index=${INDEX}&id_field=${ID_FIELD}&k=${K}&search_pipeline=neural-search-pipeline" \
#   -H "Content-Type: application/json" \
#    --data-binary '{
#                      "neural": {
#                        "title_embedding": {
#                          "query_text": ""#$query##",
#                          "k": "50"
#                        }
#                      }
#                  }'

# Hybrid search
#curl -s -X POST "http://localhost:9200/_plugins/search_relevance/experiments?id=${QUERY_SET_ID}&judgments_id=${JUDGMENTS_ID}&index=${INDEX}&id_field=${ID_FIELD}&k=${K}&search_pipeline=hybrid-search-pipeline" \
#   -H "Content-Type: application/json" \
#    --data-binary '{
#                      "hybrid": {
#                        "queries": [
#                          {
#                            "match": {
#                              "title": {
#                                "query": "#$query##"
#                              }
#                            }
#                          },
#                          {
#                            "neural": {
#                              "title_embedding": {
#                                "query_text": "#$query##",
#                                "k": "50"
#                              }
#                            }
#                          }
#                        ]
#                      }
#                  }'
