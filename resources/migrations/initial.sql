create table patient (
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


SELECT create_distributed_table('patient', 'id');
SELECT create_distributed_table('encounter', 'patient_id', colocate_with => 'patient');

SELECT create_reference_table('practioner');
