package org.alfresco.rest.client;

import org.alfresco.rest.client.cmis.CmisClient;
import org.alfresco.rest.client.rest.ElasticsearchClient;
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

    @Autowired
    ElasticsearchClient elasticsearchClient;

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
        var wrapper = new Object(){ Folder currentFolder; };
        jsonFiles.stream().forEach(json -> {
            // Group folders in 1,000 subfolder trees, to improve Repository performance
            if (counterFolders.get() % 1000 == 1) {
                wrapper.currentFolder = cmisClient.createFolder(cmisClient.getRootFolder(), "folder-" + counterFolders.get());
            }
            Folder folder = cmisClient.createFolder(wrapper.currentFolder, json.getFileName().toString().replace(".", "-"));
            folders.put(json, folder.getId());
            LOG.info("Created {} folders", counterFolders.getAndIncrement());
        });

        // Create files and metadata requests
        final AtomicInteger counterFiles = new AtomicInteger(1);
        jsonFiles.stream().forEach(json -> {
            String response = restClient.createDocuments(folders.get(json), json.toFile());
            LOG.info("Processed {} files with response {} [{}]", counterFiles.getAndIncrement(), response, json.getFileName());
        });

        LOG.info("... all JSON files processed!");

        Instant finish = Instant.now();

        LOG.info("The Bulk Ingestion process took {} minutes", Duration.between(start, finish).toMinutes());

        Integer documentCount = counterFolders.get() * 100;
        // We need to remove 75 documents, as the first invocation to Bulk Object Mapper
        // is only creating 25 documents successfully (due to a transaction bug when using 4 threads)
        documentCount = documentCount - 25;
        LOG.info("Elasticsearch indexed {} of {} documents...", elasticsearchClient.getDocumentCount(), documentCount);

        // Stop polling when 99% has been reached, so some indexing errors may happen
        Integer documentCount99 = (documentCount * 99) / 100;
        start = Instant.now();
        while (elasticsearchClient.getDocumentCount() < documentCount99) {
            LOG.info("{} of {} ", elasticsearchClient.getDocumentCount(), documentCount);
            Thread.sleep(10000);
        }
        finish = Instant.now();
        LOG.info("Elasticsearch took {} seconds to catch up with Repo ({} documents indexed)",
                Duration.between(start, finish).toSeconds(), elasticsearchClient.getDocumentCount());

    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

}
