begin
    dbms_aqadm.create_queue_table(queue_table => 'queue', queue_payload_type => 'sys.aq$_jms_message', sort_list => 'ENQ_TIME');
    dbms_aqadm.create_queue(queue_name => 'my_queue', queue_table => 'queue', max_retries => 999999999, retry_delay => 5);
    dbms_aqadm.start_queue(queue_name => 'my_queue');
end;
/

CREATE TABLE MONKEY (
    ID NUMBER(19, 0),
    MESSAGE VARCHAR2(255 CHAR),
    CONSTRAINT PK_MONKEY PRIMARY KEY (ID)
);

CREATE SEQUENCE SEQ_MONKEY;
