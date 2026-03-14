CREATE TABLE public.domain_queue (
id int8 GENERATED ALWAYS AS IDENTITY NOT NULL,
  	status int4 NOT NULL,
  	updated_at timestamptz NOT NULL,
  	payload text NOT NULL,
  	CONSTRAINT domain_queue_pk PRIMARY KEY (id)
  );


DO
$$
BEGIN
   FOR i IN 1..1000 LOOP
	IF i % 2 = 0 THEN
		INSERT INTO domain_queue(status, updated_at, payload) VALUES(1, NOW(), FORMAT('{"name":"koen","id":"%s","pet":"dog"}', i));
	ELSE
		INSERT INTO domain_queue(status, updated_at, payload) VALUES(1, NOW(), FORMAT('{"name":"koen","id":"%s","pet":"cat"}', i));
	END IF;
   END LOOP;
END;
$$;


-- POLL batch with size N from queue
WITH messages AS MATERIALIZED (
    SELECT * FROM domain_queue
    WHERE status = 1
    ORDER BY updated_at
    LIMIT 10
    FOR UPDATE SKIP LOCKED
)
UPDATE domain_queue
SET status = 2, updated_at = NOW()
WHERE id = ANY(SELECT id FROM messages)
returning *;

-- COMMIT batch with size N to queue
UPDATE domain_queue SET status = 3, updated_at = NOW() WHERE status = 2;

-- UPDATE queue for any record that has pet=dog
UPDATE domain_queue
SET status = 1
WHERE payload LIKE '%dog%'
AND status = 3;

-- UPDATE queue for messages within period
UPDATE domain_queue
SET status = 1
WHERE updated_at > NOW() - INTERVAL '3 minute';

-- CLEANUP queue
DELETE FROM domain_queue dq
WHERE dq.updated_at < NOW() - INTERVAL '1 minute'
AND status = 3;
