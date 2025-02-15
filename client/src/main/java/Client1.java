import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class Client1 {
    private static final Object lock = new Object();
    private static final BlockingQueue<String[]> metrics = new LinkedBlockingQueue<>();
    private static final String CSV = "metrics.csv";

    static Request POISON_PILL;
    static AtomicLong successCounter = new AtomicLong();
    static AtomicLong failedCounter = new AtomicLong();
    static AtomicLong responseTimeSum = new AtomicLong();
    static Random random = new Random(System.nanoTime());

    static final int maxRequests = 200000;
    static final int threads1 = 32;
    static final int threads2 = 200;
    static final int perRequests = 1000;
    private static boolean flag = false;

    static class Request {
        LiftRide liftRide;
        int skierID;
        String seasonID;
        String dayID;
        int resortID;

        Request() {
            liftRide = new LiftRide();
            liftRide.setLiftID(uniform(1, 41));
            liftRide.setTime(uniform(1, 361));
            resortID = uniform(1, 11);
            seasonID = "2025";
            dayID = "1";
            skierID = uniform(1, 100001);
        }
    }

    static class ProducerThread implements Runnable {
        private final BlockingQueue<Request> buffer;

        public ProducerThread(BlockingQueue<Request> data) {
            this.buffer = data;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < maxRequests; i++)
                    this.buffer.put(new Request());

                for (int i = 0; i < threads1 + threads2; i++)
                    this.buffer.put(POISON_PILL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    static class Consumer implements Runnable {
        private final CountDownLatch latch;
        private final BlockingQueue<Request> buffer;
        private final SkiersApi api;

        public Consumer(BlockingQueue<Request> buffer, CountDownLatch latch, String serverUrl) {
            this.buffer = buffer;
            this.latch = latch;
            this.api = new SkiersApi();
            api.getApiClient().setBasePath(serverUrl);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Request request = buffer.take();
                    if (request == POISON_PILL) break;
                    sendRequest(request, api);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            latch.countDown();
        }
    }

    static class ConsumerInitial implements Runnable {
        private final CountDownLatch latch;
        private final BlockingQueue<Request> buffer;
        private final SkiersApi api;

        public ConsumerInitial(BlockingQueue<Request> buffer, CountDownLatch latch, String serverUrl) {
            this.buffer = buffer;
            this.latch = latch;
            this.api = new SkiersApi();
            api.getApiClient().setBasePath(serverUrl);
        }

        @Override
        public void run() {
            for (int i = 0; i < perRequests; i++) {
                try {
                    Request request = buffer.take();
                    if (request == POISON_PILL) break;
                    sendRequest(request, api);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            synchronized (lock) {
                if (!flag) {
                    flag = true;
                    lock.notifyAll();
                }
            }
            latch.countDown();
        }
    }

    static class Writer implements Runnable {
        private final BlockingQueue<String[]> queue;
        private final String filePath;

        public Writer(BlockingQueue<String[]> queue, String filePath) {
            this.queue = queue;
            this.filePath = filePath;
        }

        @Override
        public void run() {
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath));
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
                while (true) {
                    String[] record = queue.take();
                    if (record.length == 1 && "END".equals(record[0])) {
                        break;
                    }
                    csvPrinter.printRecord((Object[]) record);
                    csvPrinter.flush();
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void runPhase1(BlockingQueue<Request> buffer, CountDownLatch latch, String serverUrl)
            throws InterruptedException {
        successCounter = new AtomicLong(0);
        failedCounter = new AtomicLong(0);
        responseTimeSum = new AtomicLong(0);

        for (int i = 0; i < threads1; i++) {
            Thread thread = new Thread(new ConsumerInitial(buffer, latch, serverUrl));
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.start();
        }

        synchronized (lock) {
            while (!flag) {
                lock.wait();
            }
        }
    }

    private static void runPhase2(BlockingQueue<Request> buffer, CountDownLatch latch, String serverUrl)
            throws InterruptedException {
        for (int i = 0; i < threads2; i++) {
            new Thread(new Consumer(buffer, latch, serverUrl)).start();
        }
        latch.await();
    }

    private static void sendRequest(Request request, SkiersApi api) {
        int attempts = 0;
        long st = System.currentTimeMillis();

        while (attempts < 5) {
            try {
                ApiResponse<Void> response = api.writeNewLiftRideWithHttpInfo(
                        request.liftRide, request.resortID,
                        request.seasonID, request.dayID, request.skierID
                );
                long et = System.currentTimeMillis();
                responseTimeSum.getAndAdd(et - st);

                String[] record = {
                        String.valueOf(st), "POST",
                        String.valueOf(et - st),
                        String.valueOf(response.getStatusCode())
                };
                metrics.put(record);

                if (response.getStatusCode() == 200) {
                    successCounter.incrementAndGet();
                    return;
                }
            } catch (ApiException | InterruptedException e) {
                long et = System.currentTimeMillis();
                responseTimeSum.getAndAdd(et - st);
                String[] record = {
                        String.valueOf(st), "POST",
                        String.valueOf(et - st), "500"
                };
                try {
                    metrics.put(record);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }

            attempts++;
        }

        failedCounter.incrementAndGet();
    }

    public static int uniform(int a, int b) {
        return a + random.nextInt(b - a);
    }

    private static void metrics(String filePath) throws IOException {
        List<Long> latencies = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT)) {
            for (CSVRecord record : csvParser) {
                latencies.add(Long.parseLong(record.get(2)));
            }
        }

        Collections.sort(latencies);
        double total = 0;
        for (long latency : latencies) total += latency;
        System.out.println("Mean: " + total / latencies.size() + " ms");
        System.out.println("Median: " + latencies.get(latencies.size() / 2) + " ms");
        System.out.println("Min: " + latencies.get(0) + " ms");
        System.out.println("Max: " + latencies.get(latencies.size() - 1) + " ms");
        System.out.println("P99: " + latencies.get((int) (latencies.size() * 0.99)) + " ms");
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        POISON_PILL = new Client1.Request();
        String serverUrl = "http://54.188.78.26:8080/assignment1-1.0";
        long startTime = System.currentTimeMillis();

        CountDownLatch latch = new CountDownLatch(threads1 + threads2);
        BlockingQueue<Client1.Request> buffer = new LinkedBlockingQueue<>(5000);

        Thread producerThread = new Thread(new Client1.ProducerThread(buffer));
        producerThread.start();

        runPhase1(buffer, latch, serverUrl);
        System.out.println("First phase request sent: " + successCounter.get());
        System.out.println("First phase time: " + (System.currentTimeMillis() - startTime) + " ms");

        runPhase2(buffer, latch, serverUrl);

        long endTime = System.currentTimeMillis();
        System.out.println("Successful requests: " + successCounter.get());
        System.out.println("Unsuccessful requests: " + failedCounter.get());
        System.out.println("End-to-end time: " + (endTime - startTime) + " ms");
        System.out.println("Response time: " + (double) responseTimeSum.get() / maxRequests + " ms");
        System.out.println("Throughput: " + maxRequests / ((double) (endTime - startTime) / 1000));
    }
}
