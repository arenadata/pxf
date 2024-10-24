ARG GPDB_IMAGE=hub.adsw.io/library/gpdb6_regress:adb-6.x-dev
FROM $GPDB_IMAGE as base

# install go, ginkgo and keep env variables which may be used as a part of base image
RUN set -eux; \
    ARCH="$(uname -m)"; \
    case "${ARCH}" in \
       aarch64|armhf|armv7l|amd64|x86_64) \
         wget https://go.dev/dl/go1.21.3.linux-amd64.tar.gz \
         && rm -rf /usr/local/go && tar -C /usr/local -xzf go1.21.3.linux-amd64.tar.gz && rm go1.21.3.linux-amd64.tar.gz \
         ;; \
       arm64) \
         wget https://go.dev/dl/go1.21.3.linux-arm64.tar.gz \
         && rm -rf /usr/local/go && tar -C /usr/local -xzf go1.21.3.linux-arm64.tar.gz && rm go1.21.3.linux-arm64.tar.gz \
         ;; \
       ppc64el|ppc64le) \
         wget https://go.dev/dl/go1.21.3.linux-ppc64le.tar.gz \
         && rm -rf /usr/local/go && tar -C /usr/local -xzf go1.21.3.linux-ppc64le.tar.gz && rm go1.21.3.linux-ppc64le.tar.gz \
         ;; \
       *) \
         echo "Unsupported arch: ${ARCH}"; \
         exit 1; \
         ;; \
    esac; \
    . /etc/os-release; \
    case "$ID" in \
       centos*) \
         yum-config-manager --disable epel && yum-config-manager --add-repo 'http://archives.fedoraproject.org/pub/archive/epel/7/$basearch'; \
         curl -fSL https://download.oracle.com/java/17/archive/jdk-17.0.12_linux-x64_bin.rpm -o /tmp/jdk-17.0.12_linux-x64_bin.rpm \
             && yum -y install /tmp/jdk-17.0.12_linux-x64_bin.rpm \
             && rm -rf /tmp/jdk-17.0.12_linux-x64_bin.rpm; \
         sed -i "s/JAVA_HOME=.*/JAVA_HOME=\$(readlink -f \/usr\/bin\/java | sed 's:bin\/java::')/g" /etc/profile.d/jdk_home.sh; \
         ;; \
       ubuntu*) \
         apt-get install -y openjdk-17-jdk; \
         ;; \
        esac;
ENV GOPATH=$HOME/go
ENV PATH=$PATH:/usr/local/go/bin:$GOPATH/bin
RUN go install github.com/onsi/ginkgo/ginkgo@latest \
    && go install github.com/onsi/ginkgo/v2/ginkgo@latest

# leave pxf artifacts dir env also
ENV OUTPUT_ARTIFACT_DIR="pxf_tarball"

# remove unnecessary artifacts and create symlinks
# concource scripts expects gpdb and pxf placed in the same folder
RUN rm /home/gpadmin/bin_gpdb/server-*.tar.gz && \
    mkdir /tmp/build && \
    ln -s /home/gpadmin/gpdb_src /tmp/build/gpdb_src && \
    ln -s /home/gpadmin/bin_gpdb /tmp/build/bin_gpdb
# default working dir - the place where all sources and artifacts are placed
WORKDIR /tmp/build

# create separate image with files we don't want to keep in base image
FROM base as build
COPY . /tmp/build/pxf_src
ENV JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8
SHELL ["bash", "-c"]
RUN source gpdb_src/concourse/scripts/common.bash && \
    install_gpdb && \
    source '/usr/local/greenplum-db-devel/greenplum_path.sh' && \
    mkdir ${OUTPUT_ARTIFACT_DIR} && \
    export SKIP_FDW_BUILD_REASON=0 && \
    pxf_src/concourse/scripts/compile_pxf.bash

# create test image which prepares base image and keeps only pxf artifacts from build image
FROM base as test
COPY --from=build /tmp/build/${OUTPUT_ARTIFACT_DIR}/pxf.tar.gz /tmp/build/${OUTPUT_ARTIFACT_DIR}/
COPY --from=build /tmp/build/pxf_src /tmp/build/pxf_src
