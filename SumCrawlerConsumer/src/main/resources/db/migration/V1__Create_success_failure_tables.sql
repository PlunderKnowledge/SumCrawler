create table signature_types(signature_type varchar(30) primary key);
insert into signature_types values
  ('md5'),
  ('gpg');

create table signature(
  id serial primary key,
  file_url text not null,
  signature text not null,
  signature_type varchar(30) not null references signature_types(signature_type),
  checked_date timestamp not null,
  success bool not null
);

create table gpg_keyring(
  signature_id int primary key references signature(id),
  keyring text not null default ''
)