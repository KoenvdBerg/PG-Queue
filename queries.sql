CREATE TABLE public.domain_queue (
id int8 GENERATED ALWAYS AS IDENTITY NOT NULL,
  	status int4 NOT NULL,
  	updated_at timestamptz NOT NULL,
    updated_by text NULL,
  	payload text NOT NULL,
  	CONSTRAINT domain_queue_pk PRIMARY KEY (id)
  );


DO
$$
BEGIN
   FOR i IN 1..100 LOOP
	IF i % 2 = 0 THEN
		INSERT INTO domain_queue(status, updated_at, updated_by, payload) VALUES(1, NOW(), NULL, FORMAT('{"name":"koen","id":"%s","pet":"dog"}', i));
	ELSE
		INSERT INTO domain_queue(status, updated_at, updated_by, payload) VALUES(1, NOW(), NULL, FORMAT('{"name":"koen","id":"%s","pet":"cat"}', i));
	END IF;
   END LOOP;
END;
$$;


-- POLL batch with size N from queue
-- status: 1 = pending, 2 = in progress, 3 = done
WITH messages AS MATERIALIZED (
    SELECT * FROM domain_queue
    WHERE status = 1
    ORDER BY updated_at
    LIMIT 10
    FOR UPDATE SKIP LOCKED
)
UPDATE domain_queue
SET status = 2, updated_at = NOW(), updated_by = 'koen'
WHERE id = ANY(SELECT id FROM messages)
returning *;

-- COMMIT batch with size N to queue
UPDATE domain_queue SET status = 3, updated_at = NOW(), updated_by = 'koen' WHERE id in (1,2,3,4,5,6,7,8,9,10)

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

-- MULTIPLE WORKERS
SELECT updated_by, count(*) FROM domain_queue
GROUP BY updated_by;


-- FURTHER ideas
CREATE TABLE public.domain_queue_retryable (
     id int8 GENERATED ALWAYS AS IDENTITY NOT NULL,
     status int4 NOT NULL,
     updated_at timestamptz NOT NULL,
     n_retries int4 NOT NULL,
     payload text NOT NULL,
     CONSTRAINT domain_queue_pk PRIMARY KEY (id)
);

-- POLL batch with size N from queue
-- status: 1 = pending, 2 = in progress, 3 = done
WITH messages AS MATERIALIZED (
SELECT * FROM domain_queue
WHERE status = 1
ORDER BY updated_at
LIMIT 10
FOR UPDATE SKIP LOCKED
)
UPDATE domain_queue
SET status = 2, updated_at = NOW(), n_retries = n_retries - 1
WHERE id = ANY(SELECT id FROM messages)
returning *;

-- FURTHER ideas
CREATE TABLE public.domain_queue_priority (
   id int8 GENERATED ALWAYS AS IDENTITY NOT NULL,
   status int4 NOT NULL,
   updated_at timestamptz NOT NULL,
   priority int4 NOT NULL,
   payload text NOT NULL,
   CONSTRAINT domain_queue_pk PRIMARY KEY (id)
);

-- POLL batch with size N from queue
-- status: 1 = pending, 2 = in progress, 3 = done
WITH messages AS MATERIALIZED (
SELECT * FROM domain_queue
WHERE status = 1
ORDER BY priority ASC
LIMIT 10
FOR UPDATE SKIP LOCKED
)
UPDATE domain_queue
SET status = 2, updated_at = NOW(), n_retries = n_retries - 1
WHERE id = ANY(SELECT id FROM messages)
    returning *;
