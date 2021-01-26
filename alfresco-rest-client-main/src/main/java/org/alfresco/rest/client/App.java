package org.alfresco.rest.client;

import org.alfresco.rest.client.cmis.CmisClient;
import org.alfresco.rest.client.rest.RestClient;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
public class App implements CommandLineRunner {

    static final Logger LOG = LoggerFactory.getLogger(App.class);

    static final String JsonPath = "/Users/aborroy/Desktop/git/sizing-guide-data-generator/generated/json/docx20_pptx20_pdf20_jpg20_txt20_metadataId1/0";

    @Autowired
    Environment env;

    @Autowired
    RestClient restClient;

    @Autowired
    CmisClient cmisClient;


    public void run(String... args) throws Exception {

        Instant start = Instant.now();

        LOG.info("Processing {} JSON files...", Files.list(Paths.get(JsonPath)).count());

        AtomicInteger counter = new AtomicInteger(1);
        Files.list(Paths.get(JsonPath)).forEach(json -> {
            Folder folder = cmisClient.createFolder(cmisClient.getRootFolder(), json.getFileName().toString().replace(".", "-"));
            String response = restClient.createDocuments(folder.getId(), json.toFile());
            LOG.info("Processed {} files with response {}", counter.getAndIncrement(), response);
        });

        LOG.info("... all JSON files processed!");

        Instant finish = Instant.now();

        LOG.info("The process took {} minutes", Duration.between(start, finish).toMinutes());

    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

}
