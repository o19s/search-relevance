# SPDX-License-Identifier: Apache-2.0
#
# The OpenSearch Contributors require contributions made to
# this file be licensed under the Apache-2.0 license or a
# compatible open source license.

#!/bin/bash -e

QUERY_SET_ID="${1}"

curl -s "http://localhost:9200/srw_query_sets/_doc/${QUERY_SET_ID}" | jq
