UPDATE vpro4_mediafragments m
SET
  create_date =
    (SELECT
      (d2.daycount * interval '1 day' + date('1970-01-01 00:00:00'))
    FROM
      vpro4_daymarks d1,
      vpro4_daymarks d2
    WHERE
      d1.mark < m.number AND
      m.number < d2.mark AND
      d1.daycount = d2.daycount - 1
    ORDER BY d2.number
    LIMIT 1),
  create_by = (
    SELECT DISTINCT
      p.number
    FROM
      vpro4_people p
    WHERE
      p.account = m.owner
    ORDER BY p.number
    LIMIT 1),
  workflow = 'PUBLISHED';

UPDATE vpro4_mediafragments
SET
  modified_date = create_date,
  modified_by = create_by;
