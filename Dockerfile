FROM opensearchproject/opensearch:3.0.0

ADD ./build/distributions/opensearch-search-relevance-3.0.0.0.zip  /tmp/opensearch-search-relevance-3.0.0.0.zip
RUN /usr/share/opensearch/bin/opensearch-plugin install --batch file:/tmp/opensearch-search-relevance-3.0.0.0.zip

ADD --chmod=444 https://o19s-public-datasets.s3.amazonaws.com/opensearch-ubi-3.0.0.0-SNAPSHOT.zip  /tmp/opensearch-ubi-3.0.0.0-SNAPSHOT.zip
RUN /usr/share/opensearch/bin/opensearch-plugin install --batch file:/tmp/opensearch-ubi-3.0.0.0-SNAPSHOT.zip
