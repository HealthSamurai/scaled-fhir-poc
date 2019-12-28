(ns fhir.migrations)

(def init "create table patient (
    id text primary key,
    resource jsonb
);

create table encounter (
    id text primary key,
    patient_id text,
    resource jsonb
);

create table practitioner (
    id text primary key,
    resource jsonb
);

SELECT create_distributed_table('patient', 'id');
SELECT create_distributed_table('encounter', 'patient_id', colocate_with => 'patient');
SELECT create_reference_table('practioner');
")

(defn run-migration [db]
  (db.core/exec! db init))
