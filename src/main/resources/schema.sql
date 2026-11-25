drop table if exists notifications;
drop table if exists notification_blacklist;

create table notification_blacklist(
    target_dest varchar(255) primary key,
    blocked_at timestamp not null,
    blocked_until timestamp,
    blocking_reason varchar(255) not null
);

create table notifications (
    id bigint generated always as identity primary key,
    notification_type varchar(32) not null,
    target_dest varchar(255) not null,
    payload_content clob,
    current_state varchar(1) not null,
    failure_count integer not null,
    dispatch_count integer not null,
    is_blocked boolean not null,
    last_msg_or_err varchar(255),
    created_at timestamp not null,
    updated_at timestamp not null
);

create index ix_ntf_tx_tgt on notifications (target_dest);
create index ix_ntf_tx_st on notifications (current_state);