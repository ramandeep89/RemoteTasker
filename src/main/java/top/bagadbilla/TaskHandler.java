package top.bagadbilla;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TaskHandler {
    private final File directory;
    private final List<String> command;
    private volatile StringBuilder output = new StringBuilder();
    private volatile Process process;

    private ExecutorService executor;
    private Future<?> future;

    public TaskHandler(String directory, String command) throws FileNotFoundException {
        this.directory = new File(directory);
        this.command = Arrays.asList(command.split(" "));
        if (!this.directory.exists()) {
            throw new FileNotFoundException(directory);
        }
    }

    public void resetProcess() throws InterruptedException {
        this.shutdown();
        executor = Executors.newSingleThreadExecutor();
        future = executor.submit(() -> {
            try {
                process = new ProcessBuilder(command).directory(directory).redirectErrorStream(true).start();
                output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line);
                        output.append(System.lineSeparator());
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void shutdown() throws InterruptedException {
        if(process != null && process.isAlive()) {
            process.destroyForcibly();
        }
        if(future != null && !future.isDone()) {
            future.cancel(true);
        }
        if (executor != null && !executor.isTerminated()) {
            executor.shutdownNow();
            executor.awaitTermination(5000, TimeUnit.MILLISECONDS);
        }
    }

    public String getOutput() {
        return output.toString();
    }

}
