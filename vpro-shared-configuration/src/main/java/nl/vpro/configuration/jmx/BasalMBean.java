package nl.vpro.configuration.jmx;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;

import org.springframework.jmx.export.annotation.*;

import nl.vpro.util.CommandExecutorImpl;

/**
 * @author Michiel Meeuwissen
 * @since 5.7
 */
@ManagedResource(
    description =
        "Exposes some basal operations, which would be otherwise cumbersome without file system access",
    objectName = "nl.vpro:name=basal"
)
@Slf4j
public class BasalMBean {

    @ManagedOperation(description = "Delete a file. E.g. a log-file (sometimes they linger for years!). Useful if you don't have shell access")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "file", description = "Path to file or directory to remove"),
        @ManagedOperationParameter(name = "confirm", description = "For deleting a directory, type here 'DIRECTORY'"),
    })
    public String delete(String file, String confirm) {
        File f = new File(file);
        if (f.isDirectory()) {
            if (confirm.equals("DIRECTORY")) {
                int count = deleteDirectory(f);
                return "Deleted " + count + " files";
            } else {
                return "Will not delete directory " + f + " because confirm is not 'DIRECTORY";
            }
        } else {
            if (f.delete()) {
                return "Deleted " + f;
            } else {
                return "Could not delete " + f;
            }
        }
    }


    @ManagedOperation(description = "Create a simple file. Using oc rsync is a bit cumbersome from scripts")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "file", description = "Path to file or directory to remove"),
        @ManagedOperationParameter(name = "contents", description = "Contents of the file"),
    })
    public String create(String fileName, String contents) throws IOException {
        File file = new File(fileName);
        if (file.exists()) {
            throw new IllegalArgumentException("Already exists " + file);
        }
        try(FileOutputStream fileOutputStream = new FileOutputStream(file)){
            fileOutputStream.write(contents.getBytes(StandardCharsets.UTF_8));
            return "Created new " + file + " " + file.length() + " bytes";
        }
    }



    @ManagedOperation(description = "Execute a shell command")
    public String shell(String arg) {
        CommandExecutorImpl executor = CommandExecutorImpl.builder()
            .executablesPaths("/bin/bash")
            .commonArg("-c")
            .build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int result = executor.execute(outputStream, outputStream, arg);
        return result + ":" + outputStream;

    }

    protected int deleteDirectory(File f) {
        int count = 0;
        for (File fileInDir : f.listFiles()) {
            if (fileInDir.isDirectory()) {
                count+= deleteDirectory(fileInDir);
            } else {
                if (fileInDir.delete()) {
                    log.info("Deleted {}", fileInDir);
                    count++;
                } else {
                    log.info("Could not delete {}", fileInDir);
                }
            }
        }
        if (f.delete()) {
            log.info("Deleted {}", f);
            count++;
        } else {
            log.info("Could not delete {}", f);
        }
        return count;
    }
}
