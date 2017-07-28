BEGIN

declare tempCode VARCHAR2 (4000);
rawTempVar RAW(32000); 

longRawTempVar long raw; 

begin

------------------BEGIN TABLE = T_TEST---------------------------

tempCode := 'aoeu';
MERGE INTO T_TEST sr USING dual ON (sr.c_code = tempCode) 
WHEN MATCHED THEN
 UPDATE SET C_CDT_TEST_YES_NO_MAYBE = null,C_NAME = 'aoeu',C_ORDER = null,ETK_END_DATE = null,ETK_START_DATE = null
WHEN NOT MATCHED THEN 
insert (C_CDT_TEST_YES_NO_MAYBE,C_CODE,C_NAME,C_ORDER,ETK_END_DATE,ETK_START_DATE,ID) values (null,'aoeu','aoeu',null,null,null,object_id.nextval);

delete from M_TEST_TRUNCATE where id_owner = (select id from T_TEST where c_code = tempCode);


insert into M_TEST_TRUNCATE(ID, ID_OWNER, LIST_ORDER, C_CAR_COLOR) values (object_id.nextval, (select id from T_TEST where c_code = tempCode), 1, (select (select ID from T_CAR_COLOR where C_CODE = 'Orange') from dual));
insert into M_TEST_TRUNCATE(ID, ID_OWNER, LIST_ORDER, C_CAR_COLOR) values (object_id.nextval, (select id from T_TEST where c_code = tempCode), 2, (select (select ID from T_CAR_COLOR where C_CODE = 'Purple') from dual));
insert into M_TEST_TRUNCATE(ID, ID_OWNER, LIST_ORDER, C_CAR_COLOR) values (object_id.nextval, (select id from T_TEST where c_code = tempCode), 3, (select (select ID from T_CAR_COLOR where C_CODE = 'Red') from dual));
insert into M_TEST_TRUNCATE(ID, ID_OWNER, LIST_ORDER, C_CAR_COLOR) values (object_id.nextval, (select id from T_TEST where c_code = tempCode), 4, (select (select ID from T_CAR_COLOR where C_CODE = 'White') from dual));
insert into M_TEST_TRUNCATE(ID, ID_OWNER, LIST_ORDER, C_CAR_COLOR) values (object_id.nextval, (select id from T_TEST where c_code = tempCode), 5, (select (select ID from T_CAR_COLOR where C_CODE = 'Yellow') from dual));


-----------------------------------------------

tempCode := 'Test';
MERGE INTO T_TEST sr USING dual ON (sr.c_code = tempCode) 
WHEN MATCHED THEN
 UPDATE SET C_CDT_TEST_YES_NO_MAYBE = null,C_NAME = 'Test',C_ORDER = null,ETK_END_DATE = null,ETK_START_DATE = null
WHEN NOT MATCHED THEN 
insert (C_CDT_TEST_YES_NO_MAYBE,C_CODE,C_NAME,C_ORDER,ETK_END_DATE,ETK_START_DATE,ID) values (null,'Test','Test',null,null,null,object_id.nextval);

delete from M_TEST_TRUNCATE where id_owner = (select id from T_TEST where c_code = tempCode);


insert into M_TEST_TRUNCATE(ID, ID_OWNER, LIST_ORDER, C_CAR_COLOR) values (object_id.nextval, (select id from T_TEST where c_code = tempCode), 1, (select (select ID from T_CAR_COLOR where C_CODE = 'Black') from dual));
insert into M_TEST_TRUNCATE(ID, ID_OWNER, LIST_ORDER, C_CAR_COLOR) values (object_id.nextval, (select id from T_TEST where c_code = tempCode), 2, (select (select ID from T_CAR_COLOR where C_CODE = 'Blue') from dual));
insert into M_TEST_TRUNCATE(ID, ID_OWNER, LIST_ORDER, C_CAR_COLOR) values (object_id.nextval, (select id from T_TEST where c_code = tempCode), 3, (select (select ID from T_CAR_COLOR where C_CODE = 'Green') from dual));
insert into M_TEST_TRUNCATE(ID, ID_OWNER, LIST_ORDER, C_CAR_COLOR) values (object_id.nextval, (select id from T_TEST where c_code = tempCode), 4, (select (select ID from T_CAR_COLOR where C_CODE = 'Grey') from dual));


-----------------------------------------------

tempCode := 'Adam 325 Test';
MERGE INTO T_TEST sr USING dual ON (sr.c_code = tempCode) 
WHEN MATCHED THEN
 UPDATE SET C_CDT_TEST_YES_NO_MAYBE = 2,C_NAME = 'Adam 325 Test',C_ORDER = 5666,ETK_END_DATE = to_date('2017-04-19 00:00:00','RRRR-MM-DD HH24:MI:SS'),ETK_START_DATE = to_date('2017-04-19 00:00:00','RRRR-MM-DD HH24:MI:SS')
WHEN NOT MATCHED THEN 
insert (C_CDT_TEST_YES_NO_MAYBE,C_CODE,C_NAME,C_ORDER,ETK_END_DATE,ETK_START_DATE,ID) values (2,'Adam 325 Test','Adam 325 Test',5666,to_date('2017-04-19 00:00:00','RRRR-MM-DD HH24:MI:SS'),to_date('2017-04-19 00:00:00','RRRR-MM-DD HH24:MI:SS'),object_id.nextval);

delete from M_TEST_TRUNCATE where id_owner = (select id from T_TEST where c_code = tempCode);


insert into M_TEST_TRUNCATE(ID, ID_OWNER, LIST_ORDER, C_CAR_COLOR) values (object_id.nextval, (select id from T_TEST where c_code = tempCode), 1, (select (select ID from T_CAR_COLOR where C_CODE = 'Grey') from dual));
insert into M_TEST_TRUNCATE(ID, ID_OWNER, LIST_ORDER, C_CAR_COLOR) values (object_id.nextval, (select id from T_TEST where c_code = tempCode), 2, (select (select ID from T_CAR_COLOR where C_CODE = 'Orange') from dual));
insert into M_TEST_TRUNCATE(ID, ID_OWNER, LIST_ORDER, C_CAR_COLOR) values (object_id.nextval, (select id from T_TEST where c_code = tempCode), 3, (select (select ID from T_CAR_COLOR where C_CODE = 'Purple') from dual));
insert into M_TEST_TRUNCATE(ID, ID_OWNER, LIST_ORDER, C_CAR_COLOR) values (object_id.nextval, (select id from T_TEST where c_code = tempCode), 4, (select (select ID from T_CAR_COLOR where C_CODE = 'Red') from dual));
insert into M_TEST_TRUNCATE(ID, ID_OWNER, LIST_ORDER, C_CAR_COLOR) values (object_id.nextval, (select id from T_TEST where c_code = tempCode), 5, (select (select ID from T_CAR_COLOR where C_CODE = 'White') from dual));
insert into M_TEST_TRUNCATE(ID, ID_OWNER, LIST_ORDER, C_CAR_COLOR) values (object_id.nextval, (select id from T_TEST where c_code = tempCode), 6, (select (select ID from T_CAR_COLOR where C_CODE = 'Yellow') from dual));


-----------------------------------------------

AEA_UPDATE_FILE_REFERENCE_ID;

END;
END;