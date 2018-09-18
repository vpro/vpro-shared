package nl.vpro.jmx;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.File;

import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;

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

    @ManagedOperation(description = "Delete a file. E.g. a log-file (sometime they linger for years!). Useful if you don't have shell access")
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



    @ManagedOperation(description = "Execute a shell command")
    public String shell(String arg) {
        CommandExecutorImpl executor = CommandExecutorImpl.builder()
            .executablesPaths("/bin/bash")
            .commonArg("-c")
            .build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int result = executor.execute(outputStream, outputStream, arg);
        return result + ":" + new String(outputStream.toByteArray());

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
            log.info("Deleted " + f);
            count++;
        } else {
            log.info("Could not delete " + f);
        }
        return count;
    }
}
