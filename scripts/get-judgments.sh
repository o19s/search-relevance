# SPDX-License-Identifier: Apache-2.0
#
# The OpenSearch Contributors require contributions made to
# this file be licensed under the Apache-2.0 license or a
# compatible open source license.

#!/bin/bash -e

#JUDGMENT_ID=$1
#curl -s "http://localhost:9200/srw_judgments/_doc/${JUDGMENT_ID}" | jq

curl -s "http://localhost:9200/srw_judgments/_search" | jq
