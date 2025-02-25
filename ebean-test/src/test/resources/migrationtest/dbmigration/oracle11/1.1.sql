-- Migrationscripts for ebean unittest
-- drop dependencies
alter table migtest_fk_cascade drop constraint fk_migtest_fk_cascade_one_id;
alter table migtest_fk_set_null drop constraint fk_migtest_fk_set_null_one_id;
delimiter $$
declare
  expected_error exception;
  pragma exception_init(expected_error, -2443);
begin
  execute immediate 'alter table migtest_e_basic drop constraint ck_migtest_e_basic_status';
exception
  when expected_error then null;
end;
$$;
delimiter $$
declare
  expected_error exception;
  pragma exception_init(expected_error, -2443);
begin
  execute immediate 'alter table migtest_e_basic drop constraint ck_migtest_e_basic_status2';
exception
  when expected_error then null;
end;
$$;
delimiter $$
declare
  expected_error exception;
  pragma exception_init(expected_error, -2443);
begin
  execute immediate 'alter table migtest_e_basic drop constraint uq_migtest_e_basic_indextest2';
exception
  when expected_error then null;
end;
$$;
delimiter $$
declare
  expected_error exception;
  pragma exception_init(expected_error, -2443);
begin
  execute immediate 'alter table migtest_e_basic drop constraint uq_migtest_e_basic_indextest6';
exception
  when expected_error then null;
end;
$$;
delimiter $$
declare
  expected_error exception;
  pragma exception_init(expected_error, -2443);
begin
  execute immediate 'alter table migtest_e_enum drop constraint ck_migtest_e_enum_test_status';
exception
  when expected_error then null;
end;
$$;
drop index ix_migtest_e_basic_indextest1;
drop index ix_migtest_e_basic_indextest5;
drop index ix_migtest_quoted_status1;
-- apply changes
create table migtest_e_user (
  id                            number(10) not null,
  constraint pk_migtest_e_user primary key (id)
);
create sequence migtest_e_user_seq;

create table migtest_mtm_c_migtest_mtm_m (
  migtest_mtm_c_id              number(10) not null,
  migtest_mtm_m_id              number(19) not null,
  constraint pk_migtest_mtm_c_migtest_mtm_m primary key (migtest_mtm_c_id,migtest_mtm_m_id)
);

create table migtest_mtm_m_migtest_mtm_c (
  migtest_mtm_m_id              number(19) not null,
  migtest_mtm_c_id              number(10) not null,
  constraint pk_migtest_mtm_m_migtest_mtm_c primary key (migtest_mtm_m_id,migtest_mtm_c_id)
);

create table migtest_mtm_m_phone_numbers (
  migtest_mtm_m_id              number(19) not null,
  value                         varchar2(255) not null
);


update migtest_e_basic set status = 'A' where status is null;

-- rename all collisions;

insert into migtest_e_user (id) select distinct user_id from migtest_e_basic;

-- NOTE: table has @History - special migration may be necessary
update migtest_e_history2 set test_string = 'unknown' where test_string is null;

-- NOTE: table has @History - special migration may be necessary
update migtest_e_history6 set test_number1 = 42 where test_number1 is null;
-- apply alter tables
alter table "table" modify textfield null;
alter table "table" add "select" varchar2(255);
alter table "table" add textfield2 varchar2(255);
alter table migtest_ckey_detail add one_key number(10);
alter table migtest_ckey_detail add two_key varchar2(127);
alter table migtest_ckey_parent add assoc_id number(10);
alter table migtest_e_basic modify status default 'A';
alter table migtest_e_basic modify status not null;
alter table migtest_e_basic modify status2 varchar2(127);
alter table migtest_e_basic modify status2 default null;
alter table migtest_e_basic modify status2 null;
alter table migtest_e_basic modify a_lob default null;
alter table migtest_e_basic modify a_lob null;
alter table migtest_e_basic modify user_id null;
alter table migtest_e_basic add new_string_field varchar2(255) default 'foo''bar' not null;
alter table migtest_e_basic add new_boolean_field number(1) default 1 not null;
alter table migtest_e_basic add new_boolean_field2 number(1) default 1 not null;
alter table migtest_e_basic add progress number(10) default 0 not null;
alter table migtest_e_basic add new_integer number(10) default 42 not null;
alter table migtest_e_history modify test_string number(19);
alter table migtest_e_history2 modify test_string default 'unknown';
alter table migtest_e_history2 modify test_string not null;
alter table migtest_e_history2 add test_string2 varchar2(255);
alter table migtest_e_history2 add test_string3 varchar2(255) default 'unknown' not null;
alter table migtest_e_history2 add new_column varchar2(20);
alter table migtest_e_history4 modify test_number number(19);
alter table migtest_e_history5 add test_boolean number(1) default 0 not null;
alter table migtest_e_history6 modify test_number1 default 42;
alter table migtest_e_history6 modify test_number1 not null;
alter table migtest_e_history6 modify test_number2 null;
alter table migtest_e_softdelete add deleted number(1) default 0 not null;
alter table migtest_oto_child add master_id number(19);
-- apply post alter
alter table migtest_e_basic add constraint ck_migtest_e_basic_status check ( status in ('N','A','I','?'));
-- NOT YET IMPLEMENTED: alter table migtest_e_basic add constraint uq_migtest_e_basic_description unique  (description);
update migtest_e_basic set new_boolean_field = old_boolean;

alter table migtest_e_basic add constraint ck_migtest_e_basic_progress check ( progress in (0,1,2));
-- NOT YET IMPLEMENTED: alter table migtest_e_basic add constraint uq_mgtst__bsc_stts_ndxtst1 unique  (status,indextest1);
-- NOT YET IMPLEMENTED: alter table migtest_e_basic add constraint uq_migtest_e_basic_name unique  (name);
-- NOT YET IMPLEMENTED: alter table migtest_e_basic add constraint uq_migtest_e_basic_indextest4 unique  (indextest4);
-- NOT YET IMPLEMENTED: alter table migtest_e_basic add constraint uq_migtest_e_basic_indextest5 unique  (indextest5);
comment on column migtest_e_history.test_string is 'Column altered to long now';
comment on table migtest_e_history is 'We have history now';
comment on column "table"."index" is 'this is an other comment';
-- NOT YET IMPLEMENTED: alter table "table" add constraint uq_table_select unique  ("select");
-- foreign keys and indices
create index ix_mgtst_mtm_c_mgtst_mt_3ug4ok on migtest_mtm_c_migtest_mtm_m (migtest_mtm_c_id);
alter table migtest_mtm_c_migtest_mtm_m add constraint fk_mgtst_mtm_c_mgtst_mt_93awga foreign key (migtest_mtm_c_id) references migtest_mtm_c (id);

create index ix_mgtst_mtm_c_mgtst_mt_3ug4ou on migtest_mtm_c_migtest_mtm_m (migtest_mtm_m_id);
alter table migtest_mtm_c_migtest_mtm_m add constraint fk_mgtst_mtm_c_mgtst_mt_93awgk foreign key (migtest_mtm_m_id) references migtest_mtm_m (id);

create index ix_mgtst_mtm_m_mgtst_mt_b7nbcu on migtest_mtm_m_migtest_mtm_c (migtest_mtm_m_id);
alter table migtest_mtm_m_migtest_mtm_c add constraint fk_mgtst_mtm_m_mgtst_mt_ggi34k foreign key (migtest_mtm_m_id) references migtest_mtm_m (id);

create index ix_mgtst_mtm_m_mgtst_mt_b7nbck on migtest_mtm_m_migtest_mtm_c (migtest_mtm_c_id);
alter table migtest_mtm_m_migtest_mtm_c add constraint fk_mgtst_mtm_m_mgtst_mt_ggi34a foreign key (migtest_mtm_c_id) references migtest_mtm_c (id);

create index ix_mgtst_mtm_m_phn_nmbr_do9ma3 on migtest_mtm_m_phone_numbers (migtest_mtm_m_id);
alter table migtest_mtm_m_phone_numbers add constraint fk_mgtst_mtm_m_phn_nmbr_s8neid foreign key (migtest_mtm_m_id) references migtest_mtm_m (id);

alter table migtest_ckey_detail add constraint fk_migtest_ckey_detail_parent foreign key (one_key,two_key) references migtest_ckey_parent (one_key,two_key);
create index ix_mgtst_cky_prnt_ssc_d on migtest_ckey_parent (assoc_id);
alter table migtest_ckey_parent add constraint fk_mgtst_cky_prnt_ssc_d foreign key (assoc_id) references migtest_ckey_assoc (id);

alter table migtest_fk_cascade add constraint fk_migtest_fk_cascade_one_id foreign key (one_id) references migtest_fk_cascade_one (id);
alter table migtest_fk_none add constraint fk_migtest_fk_none_one_id foreign key (one_id) references migtest_fk_one (id);
alter table migtest_fk_none_via_join add constraint fk_mgtst_fk_nn_v_jn_n_d foreign key (one_id) references migtest_fk_one (id);
alter table migtest_fk_set_null add constraint fk_migtest_fk_set_null_one_id foreign key (one_id) references migtest_fk_one (id);
alter table migtest_e_basic add constraint fk_migtest_e_basic_user_id foreign key (user_id) references migtest_e_user (id);
alter table migtest_oto_child add constraint fk_migtest_oto_child_master_id foreign key (master_id) references migtest_oto_master (id);

create index ix_migtest_e_basic_indextest3 on migtest_e_basic (indextest3);
create index ix_migtest_e_basic_indextest6 on migtest_e_basic (indextest6);
create index ix_table_textfield2 on "table" (textfield2);
