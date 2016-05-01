using sumcrawler;

create table success(id serial primary key, file_url TEXT NOT NULL, sum_url TEXT NOT NULL, checked_date timestamp);