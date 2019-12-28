.EXPORT_ALL_VARIABLES:
.PHONY: test deploy

SHELL = bash

VERSION = $(shell cat VERSION)
DATE = $(shell date)

include .env

repl:
	clj -A:test:nrepl -R:test:nrepl -e "(-main)" -r

up:
	docker-compose up --scale worker=5

stop:
	docker-compose stop

down:
	docker-compose down

jar:
	rm -rf target && clj -A:build

docker:
	docker build -t scaled-fhir .

pub:
	docker push ${IMG}

deploy:
	cd deploy && cat srv.tpl.yaml | ./envtpl.mac | kubectl apply -f -

all: jar docker up
	echo "Done"

test:
	clj -A:test:runner


# connect to fhir DBs

master:
	source .env && psql -h localhost -U postgres -p ${PGPORT_MASTER}

alfa:
	source .env && psql -h localhost -U postgres -p ${PGPORT_ALFA}

beta:
	source .env && psql -h localhost -U postgres -p ${PGPORT_BETA}

clear:
	rm -rf pgdata*
