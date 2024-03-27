#!/usr/bin/env bash

build_images=$1
run_test_service_name=mdw

# Set a variable to check the results of all tests at the end of the script
test_result_status=0

if [ "$build_images" == "true" ]; then
  echo "------------"
  echo "Build images"
  echo "------------"
  bash build-images.sh
fi

echo "----------------"
echo "Start containers"
echo "----------------"
docker-compose up -d

function check_docker_container_status() {
  local check_oracle_service_health=$1 # Whether the oracle service should be healthy immediately or not
  for i in {1..60}; do
    unhealthy_present="false"
    echo "-----------------------------------"
    echo "Check docker containers status: $i"
    echo "-----------------------------------"
    container_ids=$(docker-compose ps -q)
    for container_id in $container_ids
    do
      status=$(docker inspect $container_id --format "{{.State.Health.Status}}")
      if [ "$status" != "healthy" ]; then
        docker_name=$(docker container ls --all --no-trunc --filter "id=$container_id" --format "{{.Names}}")
        if [ "$docker_name" != "oracle" ]; then
          unhealthy_present="true"
          echo "Container '$docker_name' is not in a healthy status yet. Current status is '$status'."
        else
          if [ "$check_oracle_service_health" == "true" ]; then
            unhealthy_present="true"
            echo "Container '$docker_name' is not in a healthy status yet. Current status is '$status'."
          fi
        fi
      fi
    done
    if [ "$unhealthy_present" == "true" ]; then
      sleep 10
    else
      echo "---------------------------------------"
      echo "All containers are in the healthy state"
      echo "---------------------------------------"
      break;
    fi
  done

  if [ "$unhealthy_present" == "true" ]; then
      echo "--------------------------------------------"
      echo "Some containers are not in the healthy state"
      echo "--------------------------------------------"
      docker-compose ps
      exit 1
  fi
}

start_copy_artifacts() {
  local test=$1
  echo "-------------------------------------"
  echo "Start copy artifacts for $test"
  echo "-------------------------------------"
  test_dir=artifacts/$test
  mkdir -p $test_dir
  docker-compose cp $run_test_service_name:/home/gpadmin/workspace/pxf/automation/target/surefire-reports ./$test_dir
  docker-compose cp $run_test_service_name:/home/gpadmin/workspace/pxf/automation/sqlrepo ./$test_dir
  docker-compose cp $run_test_service_name:/home/gpadmin/workspace/pxf/automation/automation_logs ./$test_dir
}

check_test_result() {
  local exit_code=$1
  local test_group=$2
  if [ "$exit_code" -eq "0" ]; then
    echo "------------------------------------------------------"
    echo "Test for the group '$test_group' finished with SUCCESS"
    echo "------------------------------------------------------"
  else
    echo "----------------------------------------------------"
    echo "Test for the group $test_group finished with ERROR"
    echo "----------------------------------------------------"
    test_result_status=1
  fi
}

check_docker_container_status false # We don't need oracle service immediately

echo "-------------------------"
echo "Start running smoke tests"
echo "-------------------------"
docker-compose exec $run_test_service_name sudo -H -u gpadmin bash -l -c 'pushd $TEST_HOME && make GROUP=smoke'
check_test_result $? smoke
start_copy_artifacts smoke

echo "-----------------------------------------------"
echo "Start running integration tests in 'gpdb' group"
echo "-----------------------------------------------"
docker-compose exec $run_test_service_name sudo -H -u gpadmin bash -l -c 'pushd $TEST_HOME && make GROUP=gpdb'
check_test_result $? gpdb
start_copy_artifacts gpdb

echo "----------------------------------------------------"
echo "Start running integration tests in 'arenadata' group"
echo "----------------------------------------------------"
check_docker_container_status true # We need oracle service to be healthy for this group of tests
docker-compose exec $run_test_service_name sudo -H -u gpadmin bash -l -c 'pushd $TEST_HOME && make GROUP=arenadata'
check_test_result $? arenadata
start_copy_artifacts arenadata

echo "-------------------"
echo "Shutdown containers"
echo "-------------------"
docker-compose down

echo "-------------------------"
echo "Check tests result status"
echo "-------------------------"
if [ "$test_result_status" -eq "0" ]; then
  echo "----------------"
  echo "All tests passed"
  echo "----------------"
  exit 0
else
  echo "----------------------------------------------"
  echo "Some tests didn't pass. Check logs and reports"
  echo "----------------------------------------------"
  exit 1
fi