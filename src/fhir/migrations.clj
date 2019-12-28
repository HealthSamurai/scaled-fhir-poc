(ns fhir.migrations)

(def init "create table patient (
    id text,
    resource jsonb
);

create table encounter (
    id text,
    patient_id text,
    resource jsonb
);

create table practioner (
    id text,
    resource jsonb
);

ALTER TABLE patient ADD PRIMARY KEY (id);
ALTER TABLE practioner ADD PRIMARY KEY (id);

SELECT create_distributed_table('patient', 'id');
SELECT create_distributed_table('encounter', 'patient_id', colocate_with => 'patient');

SELECT create_reference_table('practioner');
")

(defn run-migration [db]
  (db.core/exec! db init))
