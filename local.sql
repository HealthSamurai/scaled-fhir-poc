---- db: -h localhost -p 5437 -U postgres fhir


create table if not exists patient (
  id text primary key,
  resource text
);

create table if not exists practitioner (
  id text primary key,
  resource text
);

create table if not exists encounter (
  id text primary key,
  pat_id text,
  resource text
);




----


select *, 'wow' resour
from patient



----
