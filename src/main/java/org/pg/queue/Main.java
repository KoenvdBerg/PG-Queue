package org.pg.queue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Starting postgres docker
 * sudo docker run --name queue -e POSTGRES_PASSWORD=docker -p 5432:5432 -d postgres
 * <p>
 * GRAALVM DOC: <a href="https://www.graalvm.org/latest/reference-manual/native-image/">graal VM docs</a>
 * <p>
 * to create native:
 * ~$ sdk use java 24.0.2-graalce
 * ~$ mvn -Pnative package
 */
public class Main {

    public static void main(String[] args) throws SQLException, InterruptedException {
        int batchSize = 1000;
        String workerName = "tester";
        long pollInterval = 5000L;
        if (args.length > 0) {
            try {
                workerName = args[0];
                batchSize = Integer.parseInt(args[1]);
                pollInterval = Long.parseLong(args[2]);
            } catch (Exception ignored) {
                System.out.println("Usage: ./pg_queue <workername> <batchsize> <pollinterval_ms>");
                System.exit(1);
            }
        }

        System.out.println("Starting worker with name=" + workerName + ", batch size=" + batchSize);

        Connection conn = PG.connect();
        PGQueue pgQueue = new PGQueue(conn, batchSize, "domain_queue", workerName);

        // POLLING THE QUEUE
        while (true) {
            List<DomainQueue> messages = pgQueue.pollQueue(PGQueue.Status.PENDING);
            if (messages.isEmpty()) {
                System.out.println("No messages left to process, now waiting " + pollInterval + " ms intervals");
                Thread.sleep(pollInterval);
            } else {
                long min = Collections.min(messages.stream().map(DomainQueue::id).toList());
                long max = Collections.max(messages.stream().map(DomainQueue::id).toList());
                System.out.println("Now processing " + messages.size() + " messages with ID ranging from [" + min + ", " + max + "]");
                Thread.sleep(100); // this is here as demonstration of "latency" of the work
                pgQueue.commit(messages.stream().map(DomainQueue::id).toList());
            }
        }
    }
}