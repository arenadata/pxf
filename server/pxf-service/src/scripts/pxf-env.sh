#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

PARENT_SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"

# Default PXF_HOME
export PXF_HOME=${PXF_HOME:=${PARENT_SCRIPT_DIR}}

# Path to HDFS native libraries
export LD_LIBRARY_PATH=/usr/lib/hadoop/lib/native:${LD_LIBRARY_PATH}

# Path to JAVA
export JAVA_HOME=${JAVA_HOME:=/usr/java/default}

# Path to Log directory
export PXF_LOGDIR=${PXF_HOME}/logs

# Path to Run directory
export PXF_RUNDIR=${PXF_HOME}/run

# Port
export PXF_PORT=${PXF_PORT:=5888}

# Memory
export PXF_JVM_OPTS=${PXF_JVM_OPTS:="-Xmx2g -Xms1g"}

# Kerberos path to keytab file owned by pxf service with permissions 0400
export PXF_KEYTAB=${PXF_KEYTAB:="${PXF_HOME}/conf/pxf.service.keytab"}

# Kerberos principal pxf service should use. _HOST is replaced automatically with hostnames FQDN
export PXF_PRINCIPAL=${PXF_PRINCIPAL:="gpadmin/_HOST@EXAMPLE.COM"}

# End-user identity impersonation, set to true to enable
export PXF_USER_IMPERSONATION=${PXF_USER_IMPERSONATION:=true}

# Set to true to enable Remote debug via port 8000
export PXF_DEBUG=${PXF_DEBUG:=false}
