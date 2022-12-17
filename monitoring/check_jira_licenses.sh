#!/bin/bash

JOLOKIA_PORT=12334
BASE_URL="http://localhost:${JOLOKIA_PORT}/jolokia/read"

while getopts u:p:w:c flag
  do
    case "${flag}" in
      u) username=${OPTARG};;
      p) password=${OPTARG};;
      w) warning=${OPTARG};;
      c) critical=${OPTARG};;
      h) HELP;;
    esac
  done

response=$(curl -X GET -u" ${user}:${password}" "${BASE_URL}/com.atlassian.jira:type=jira.license")
service_desk_used=$(echo $respone | jq -r '.value' | jq '.["jira-servicedesk.current.user.count"]')
service_desk_max=$(echo $respone |  jq -r '.value' | jq '.["jira-core.max.user.count"]')
service_desk_free=$service_desk_max - $service_desk_used

software_used=$(echo $respone |  jq -r '.value' | jq '.["jira-software.current.user.count"]')
software_max$(echo $respone |  jq -r '.value' | jq '.["jira-software.max.user.count"]')
software_free=$software_max - $software_used

core_used=$(echo $respone |  jq -r '.value' | jq '.["jira-core.current.user.count"]')
core_max=$(echo $respone |  jq -r '.value' | jq '.["jira-core.max.user.count"]')
core_free=$core_max - $core_used

echo "service-desk free: ${service_desk_free}"
echo "software free: ${software_free}"
echo "core free: ${core_free}"
