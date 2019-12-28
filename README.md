# Scaled FHIR Server POC

## Start

```sh
make up
make repl
# connect from ide
```

## To build project, make docker image and start 5-node citius cluster

```sh
make all
```

## Summary

- user's not able to define ID for sharded resources (except Patient). All sharded resources must consist shard id (or patient id).
- reference to patient must be defined when sharded resource is created.
- reference between sharded resource and patient is immutable.
- Group resource may be shared. If Encounter refers to Group, then it could have specific patient_id like 'group_id'.

