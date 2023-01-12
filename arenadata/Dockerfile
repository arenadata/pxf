FROM hub.adsw.io/library/gpdb6_regress:latest as base

# install java, go, ginkgo and keep env variables which may be used as a part of base image
RUN yum install -y go
ENV GOPATH=$HOME/go
ENV PATH=$PATH:/usr/local/go/bin:$GOPATH/bin
RUN go install github.com/onsi/ginkgo/ginkgo@latest

# leave pxf artifacts dir env also
ENV OUTPUT_ARTIFACT_DIR="pxf_tarball"

COPY . /tmp/build/pxf_src

# create separate image with files we don't want to keep in base image
FROM base as build
WORKDIR /home/gpadmin
RUN source gpdb_src/concourse/scripts/common.bash && \
    install_gpdb

WORKDIR /tmp/build
RUN source '/usr/local/greenplum-db-devel/greenplum_path.sh' && \
    mkdir /tmp/build/${OUTPUT_ARTIFACT_DIR} && \
    pxf_src/concourse/scripts/compile_pxf.bash

# create test image which prepares base image and keeps only pxf artifacts from build image
FROM base as test
RUN rm /home/gpadmin/bin_gpdb/server-*.tar.gz
RUN rm -rf /tmp/build/pxf_src/.git/
COPY --from=build /tmp/build/${OUTPUT_ARTIFACT_DIR}/pxf.tar.gz /tmp/build/${OUTPUT_ARTIFACT_DIR}/
