create table if not exists users(
     id bigint not null auto_increment,
     name varchar(100) not null,
     email varchar(100) not null,
     password varchar(100)  not null,
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

);