#!/bin/bash

NAGIOS_OK=0
NAGIOS_WARNING=1
NAGIOS_CRITICAL=2
NAGIOS_UNKNOWN=3

USER=$1
PASS=$2
PORT=$3
WARN=$4
CRIT=$5

if [[ -z "$USER" || -z "$PASS" || -z "$PORT" || -z "$WARN" || -z "$CRIT" ]]
then
   echo -e "Syntax error\nUsage: $0 <jolokia_user> <jolokia_password> <jolokia_port> <warning_threshold> <critical_threshold>"
   exit $NAGIOS_UNKNOWN
fi

STATUS_CODE=$NAGIOS_OK
CURL_OUTPUT=$(mktemp)
URL="http://localhost:${PORT}/jolokia/read/com.atlassian.jira:type=jira.license"
HTTP_CODE=$(curl -su "${USER}:${PASS}" -w "%{http_code}" -o $CURL_OUTPUT $URL)

if [[ $? -ne 0 ]]
then
  echo "Unexpected code returned by curl: $?"
  exit $NAGIOS_UNKNOWN
fi
if [[ $HTTP_CODE != "200" ]]
then
  echo "Unexpected HTTP code: $HTTP_CODE"
  exit $NAGIOS_UNKOWN
fi

response=$(curl -X GET -u "${USER}:${PASS}" $URL)

service_desk_used=$(echo $response | jq -r '.value' | jq '.["jira-servicedesk.current.user.count"]')
service_desk_max=$(echo $response |  jq -r '.value' | jq '.["jira-core.max.user.count"]')
service_desk_free=$(($service_desk_max-$service_desk_used))

software_used=$(echo $response |  jq -r '.value' | jq '.["jira-software.current.user.count"]')
software_max=$(echo $response |  jq -r '.value' | jq '.["jira-software.max.user.count"]')
software_free=$(($software_max-$software_used))

core_used=$(echo $response |  jq -r '.value' | jq '.["jira-core.current.user.count"]')
core_max=$(echo $response |  jq -r '.value' | jq '.["jira-core.max.user.count"]')
core_free=$(($core_max-$core_used))

# Jira core licenses

if [[ $CRIT -ge $core_free ]]
then
   msg="CRITICAL - ${core_free} core licenses left\n"
   STATUS_CODE=$NAGIOS_CRITICAL
elif [[ $WARN -ge $core_free ]]
then
   msg="WARNING - ${core_free} core licenses left\n"
   STATUS_CODE=$NAGIOS_WARNING
else
   msg="OK - ${core_free} core licenses left\n"
fi

# Jira Service desk licenses

if [[ $CRIT -ge $service_desk_free ]]
then
   msg="${msg}CRITICAL - ${service_desk_free} service desk licenses left\n"
   STATUS_CODE=$NAGIOS_CRITICAL
elif [[ $WARN -ge $service_desk_free ]]
then
   msg="${msg}WARNING - ${service_desk_free} service desk licenses left\n"
   STATUS_CODE=$NAGIOS_WARNING
   if [[ $STATUS_CODE -lt $NAGIOS_WARNING ]]  # do not change status code is current status code is higher
   then
     STATUS_CODE=$NAGIOS_WARNING
  fi
else
   msg="${msg}OK - ${service_desk_free} service desk licenses left\n"
fi

# Jira Software licenses

if [[ $CRIT -ge $software_free ]]
then
   msg="${msg}CRITICAL - ${software_free} software licenses left\n"
   STATUS_CODE=$NAGIOS_CRITICAL
elif [[ $WARN -ge $software_free ]]
then
   msg="${msg}WARNING - ${software_free} software licenses left\n"
   if [[ $STATUS_CODE -lt $NAGIOS_WARNING ]]  # do not change status code is current status code is higher
   then
     STATUS_CODE=$NAGIOS_WARNING
  fi
else
   msg="${msg}OK - ${software_free} software licenses left\n"
fi

echo -e $msg
exit $STATUS_CODE
