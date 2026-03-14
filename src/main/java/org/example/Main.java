package org.example;

import java.sql.Connection;
import java.util.List;

/**
 * Starting postgres docker
 * sudo docker run --name queue -e POSTGRES_PASSWORD=docker -p 5432:5432 -d postgres
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("STARTING");
        try {
            Connection conn = PG.connect();
            PGQueue pgQueue = new PGQueue(conn, 2000, "domain_queue");

            // POLLING THE QUEUE
            while (true) {
                List<DomainQueue> messages = pgQueue.pollQueue(PGQueue.Status.PENDING);
                if (messages.isEmpty()) {
                    System.out.println("No messages left to process, now waiting 5000ms intervals");
                    Thread.sleep(5000);
                } else {
                    for (DomainQueue message : messages) {
                        System.out.println("Now processing message with ID=" + message.id() + " and payload=" + message.payload());
                    }
                    Thread.sleep(50); // this is here as demonstration of "latency" of the work
                    pgQueue.commit(messages.stream().map(DomainQueue::id).toList());
                }
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}