--CREATE LANGUAGE plpgsql;

DROP TRIGGER updatePublishedAttr on vpro4_mediafragments;

CREATE OR REPLACE FUNCTION updatePublishedAttr () RETURNS TRIGGER AS $PROC$

  DECLARE
    owner_id BIGINT;
  BEGIN
    IF (TG_OP = 'INSERT') THEN
      IF (NEW.create_date = NULL) THEN
        SELECT INTO owner_id p.number FROM vpro4_people p WHERE p.account = NEW.owner ORDER BY p.number LIMIT 1;
        UPDATE vpro4_mediafragments SET create_date=now(), modified_date=now(), create_by=owner_id, modified_by=owner_id, workflow='PUBLISHED' WHERE number = NEW.number;
      END IF;
      END IF;
      IF (TG_OP = 'UPDATE') THEN
        IF (NEW.modified_date = OLD.modified_date) THEN
          UPDATE vpro4_mediafragments SET modified_date=now() WHERE number = NEW.number;
        END IF;
      END IF;
    RETURN NEW;
  END;
$PROC$ LANGUAGE 'plpgsql';

CREATE TRIGGER updatePublishedAttr AFTER INSERT OR UPDATE ON vpro4_mediafragments FOR EACH ROW EXECUTE PROCEDURE updatePublishedAttr();
