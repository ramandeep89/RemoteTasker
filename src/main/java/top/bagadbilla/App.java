package top.bagadbilla;

import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

@CommandLine.Command(name = "remote-tasker", version = "remote-tasker 1.0", mixinStandardHelpOptions = true)
public class App implements Runnable {
    @CommandLine.Option(names = {"-d", "--dir"}, description = "Directory to run the task in")
    String directory = "";
    @CommandLine.Option(names = {"-c", "--cmd"}, description = "Command to run the task")
    String command = "";
    @CommandLine.Option(names = {"-0", "--port"}, description = "Command to run the task")
    int port = 9090;

    public static void main(String[] args) {
        new CommandLine(new App()).execute(args);
    }

    @Override
    public void run() {
        if (command.isEmpty()) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            Objects.requireNonNull(
                                    App.class.getResourceAsStream("/run.sh"))))) {
                command = reader.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            TaskHandler handler = new TaskHandler(directory, command);

            Javalin app = Javalin.create()
                    .put("/", ctx -> {
                        handler.resetProcess();
                        ctx.status(HttpStatus.CREATED);
                    })
                    .get("/", ctx -> ctx.result(handler.getOutput()))
                    .start(port);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    handler.shutdown();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                app.stop();
            }));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
