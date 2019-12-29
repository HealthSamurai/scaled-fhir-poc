(ns fhir.migrations)

(def init "

drop table if exists encounter;
drop table if exists practitioner;
drop table if exists patient;


create table patient (
    id text,
    resource jsonb,
    ts timestamp default now()
);

create table encounter (
    id text,
    patient_id text,
    resource jsonb,
    ts timestamp default now()
);

create table practitioner (
    id text,
    resource jsonb,
    ts timestamp default now()
);

create table group(
    id text,
    resource jsonb,
    ts timestamp default now()
);


ALTER TABLE patient ADD PRIMARY KEY (id);
alter table encounter add primary key (id, patient_id);
ALTER TABLE practitioner ADD PRIMARY KEY (id);
ALTER TABLE group ADD PRIMARY KEY (id);

SELECT create_distributed_table('patient', 'id');
SELECT create_distributed_table('encounter', 'patient_id', colocate_with => 'patient');
SELECT create_reference_table('practitioner');
SELECT create_reference_table('group');
")

(defn run-migration [db]
  (db.core/exec! db init))
