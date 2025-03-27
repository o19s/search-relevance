# SPDX-License-Identifier: Apache-2.0
#
# The OpenSearch Contributors require contributions made to
# this file be licensed under the Apache-2.0 license or a
# compatible open source license.

curl -s "http://localhost:9200/ecommerce/_search?search_pipeline=hybrid-search-pipeline" -H "Content-Type: application/json" -d'
{
  "query": {
    "hybrid": {
      "queries": [
        {
          "match": {
            "title": {
              "query": "shoes"
            }
          }
        },
        {
          "neural": {
            "title_embedding": {
              "query_text": "shoes",
              "k": "50"
            }
          }
        }
      ]
    }
  }
}' | jq