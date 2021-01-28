package org.alfresco.rest.client;

import org.alfresco.rest.client.cmis.CmisClient;
import org.alfresco.rest.client.rest.RestClient;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SpringBootApplication
public class App implements CommandLineRunner {

    static final Logger LOG = LoggerFactory.getLogger(App.class);

    @Autowired
    Environment env;

    @Autowired
    RestClient restClient;

    @Autowired
    CmisClient cmisClient;

    @Value("${json.path}")
    String jsonPath;

    public void run(String... args) throws Exception {

        Instant start = Instant.now();

        LOG.info("Processing {} JSON files...", Files.list(Paths.get(jsonPath)).count());

        // It's required to get a Path List in order to run parallel threads,
        // as "Files.list" doesn't support the feature by itself
        List<Path> jsonFiles = Files.list(Paths.get(jsonPath)).collect(Collectors.toList());

        // Create folders
        Map<Path, String> folders = new HashMap<>();
        final AtomicInteger counterFolders = new AtomicInteger(1);
        jsonFiles.stream().parallel().forEach(json -> {
            Folder folder = cmisClient.createFolder(cmisClient.getRootFolder(), json.getFileName().toString().replace(".", "-"));
            folders.put(json, folder.getId());
            LOG.info("Created {} folders", counterFolders.getAndIncrement());
        });

        // Create files and metadata requests
        final AtomicInteger counterFiles = new AtomicInteger(1);
        jsonFiles.stream().forEach(json -> {
            String response = restClient.createDocuments(folders.get(json), json.toFile());
            LOG.info("Processed {} files with response {}", counterFiles.getAndIncrement(), response);
        });

        LOG.info("... all JSON files processed!");

        Instant finish = Instant.now();

        LOG.info("The process took {} minutes", Duration.between(start, finish).toMinutes());

    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

}
