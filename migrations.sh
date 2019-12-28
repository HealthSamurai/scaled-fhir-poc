#!/bin/bash
docker exec -i citus_master psql -U postgres < ./resources/migrations/initial.sql