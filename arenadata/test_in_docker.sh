#!/usr/bin/env bash
# This script depends on hub.adsw.io/library/gpdb6_pxf_regress

# test_pxf.bash use relative paths for unpacking gpdb commands and sourcing of env vars
cd /tmp/build
# unpack gpdb and pxf; run gpdb cluster and pxf server
pxf_src/concourse/scripts/test_pxf.bash
# tweak necessary folders to run regression tests later
chown gpadmin:gpadmin -R /tmp/build/pxf_src

# test fdw and external-table
su - gpadmin -c "
    source '/usr/local/greenplum-db-devel/greenplum_path.sh';
    source /tmp/build/gpdb_src/gpAux/gpdemo/gpdemo-env.sh';
    cd /tmp/build/pxf_src/fdw &&
    make install &&
    make installcheck &&
    cd ../external-table/ &&
    make install &&
    make installcheck;
"
