# SPDX-License-Identifier: Apache-2.0
#
# The OpenSearch Contributors require contributions made to
# this file be licensed under the Apache-2.0 license or a
# compatible open source license.

#!/bin/bash -e

curl -s -X DELETE "http://localhost:9200/srw_judgments" | jq
curl -s -X DELETE "http://localhost:9200/srw_query_results" | jq
curl -s -X DELETE "http://localhost:9200/ubi_queries,ubi_events" | jq
