--CREATE TYPE workflow AS ENUM ('DRAFT', 'FOR_APPROVAL' , 'PUBLISHED', 'REFUSED', 'DELETED', ', MERGED');

ALTER TABLE vpro4_mediafragments
    ADD create_date timestamp,
    ADD create_by bigint REFERENCES vpro4_people (number),
    ADD modified_date timestamp,
    ADD modified_by bigint REFERENCES vpro4_people (number),
    ADD workflow workflow;

ALTER TABLE vpro4_audiofragments
    ADD create_date timestamp,
    ADD create_by bigint REFERENCES vpro4_people (number),
    ADD modified_date timestamp,
    ADD modified_by bigint REFERENCES vpro4_people (number),
    ADD workflow workflow;

ALTER TABLE vpro4_videofragments
    ADD create_date timestamp,
    ADD create_by bigint REFERENCES vpro4_people (number),
    ADD modified_date timestamp,
    ADD modified_by bigint REFERENCES vpro4_people (number),
    ADD workflow workflow;

ALTER TABLE vpro4_mediasources
    ADD create_date timestamp,
    ADD create_by bigint REFERENCES vpro4_people (number),
    ADD modified_date timestamp,
    ADD modified_by bigint REFERENCES vpro4_people (number),
    ADD workflow workflow;

ALTER TABLE vpro4_audiosources
    ADD create_date timestamp,
    ADD create_by bigint REFERENCES vpro4_people (number),
    ADD modified_date timestamp,
    ADD modified_by bigint REFERENCES vpro4_people (number),
    ADD workflow workflow;

ALTER TABLE vpro4_videosources
    ADD create_date timestamp,
    ADD create_by bigint REFERENCES vpro4_people (number),
    ADD modified_date timestamp,
    ADD modified_by bigint REFERENCES vpro4_people (number),
    ADD workflow workflow;