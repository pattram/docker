#!/bin/bash

TEST_REALM=GamsTest
TEST_CLIENT=CiriloTest

echo "Select how to run Gams:"
echo "->[1] Run GAMS in configured realm (default mode)"
echo "  [2]  Run in test realm (development and testing only)"
read -p "Enter 1 (or just hit enter) or 2: "
if [[ "${REPLY}" == "2" ]]; then
	KEYCLOAK_REALM=${TEST_REALM}  KEYCLOAK_CLIENT=${TEST_CLIENT} docker-compose up -d
else
	docker-compose up -d

fi


