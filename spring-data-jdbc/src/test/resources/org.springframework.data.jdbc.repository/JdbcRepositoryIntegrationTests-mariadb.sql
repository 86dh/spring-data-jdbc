CREATE TABLE dummy_entity
(
    id_Prop          BIGINT AUTO_INCREMENT PRIMARY KEY,
    NAME             VARCHAR(100),
    POINT_IN_TIME    TIMESTAMP(3),
    OFFSET_DATE_TIME TIMESTAMP(3),
    FLAG             BOOLEAN,
    REF              BIGINT,
    DIRECTION        VARCHAR(100),
    BYTES            BINARY(8)
);

CREATE TABLE ROOT
(
    ID   BIGINT AUTO_INCREMENT PRIMARY KEY,
    NAME VARCHAR(100)
);

CREATE TABLE INTERMEDIATE
(
    ID       BIGINT AUTO_INCREMENT PRIMARY KEY,
    NAME     VARCHAR(100),
    ROOT     BIGINT,
    ROOT_ID  BIGINT,
    ROOT_KEY INTEGER
);

CREATE TABLE LEAF
(
    ID               BIGINT AUTO_INCREMENT PRIMARY KEY,
    NAME             VARCHAR(100),
    INTERMEDIATE     BIGINT,
    INTERMEDIATE_ID  BIGINT,
    INTERMEDIATE_KEY INTEGER
);

CREATE TABLE WITH_DELIMITED_COLUMN
(
    ID                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    `ORG.XTUNIT.IDENTIFIER` VARCHAR(100),
    STYPE                   VARCHAR(100)
);

CREATE TABLE ENTITY_WITH_SEQUENCE
(
    ID BIGINT,
    NAME VARCHAR(100)
);

CREATE SEQUENCE `ENTITY_SEQUENCE` START WITH 1 INCREMENT BY 1 NO MAXVALUE;