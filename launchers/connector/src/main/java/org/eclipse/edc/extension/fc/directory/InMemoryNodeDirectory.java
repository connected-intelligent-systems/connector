/*
 *  Copyright (c) 2024 Materna Information & Communications SE
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Materna Information & Communications SE - Initial Implementation
 *
 */

package org.eclipse.edc.extension.fc.directory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;


public class InMemoryNodeDirectory implements TargetNodeDirectory {
    
    private final Monitor monitor;

    private String nodesFilePath;
    private String nodesUrl;

    public InMemoryNodeDirectory(String nodesFilePath, String nodesUrl, Monitor monitor) {

        // monitor.info(format("nodesFilePath: %s.", nodesFilePath));
        // monitor.info(format("nodesUrl: %s.", nodesUrl));

        this.nodesFilePath = nodesFilePath;
        this.nodesUrl = nodesUrl;
        this.monitor = monitor;
    }

    @Override
    public List<TargetNode> getAll() {
        List<TargetNode> nodes = new ArrayList<>();

        // Jackson ObjectMapper for JSON parsing
        ObjectMapper objectMapper = new ObjectMapper(); 

        Map<String, String> parsedProperties = null;

        if (this.nodesUrl != null) {
            try {
                // Create HttpClient instance
                HttpClient client = HttpClient.newHttpClient();
                monitor.info(format("Try to get nodes from endpoint: %s.", this.nodesUrl));
                // Create a GET HttpRequest
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(this.nodesUrl))
                        .GET()
                        .build();
                // Send the request and get the response (body as String)
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                // Check if the request was successful (HTTP 200 OK)
                if (response.statusCode() == 200) {
                    monitor.info(format("Received response from endpoint: %s.", response.body()));
                } else {
                    monitor.info(format("Failed to load properties from the URL. Response Code: %s.", response.statusCode()));
                }
                // Parse the response body as a Map using Jackson
                parsedProperties = objectMapper.readValue(response.body(), Map.class);
            } catch (Exception e) {
                monitor.info("An unexpected error occurred: " + e.getMessage(), e);
            }

        } else if (this.nodesFilePath != null) {
            try {
                monitor.info(format("Try to get nodes from file: %s.", this.nodesFilePath));
                InputStream inputStream = Files.newInputStream(Paths.get(this.nodesFilePath));
                parsedProperties = objectMapper.readValue(inputStream, Map.class);
            } catch (Exception e) {
                monitor.info("An unexpected error occurred: " + e.getMessage(), e);
            }
        } else {
            // Do nothing
        }

        if (parsedProperties != null) {
            try {
                // Iterate over all key-value pairs
                for (Map.Entry<String, String> entry : parsedProperties.entrySet()) {
                    String key = (String) entry.getKey();
                    String value = (String) entry.getValue();
                    String nodeName = key + "-node";
                    // Print the key, nodeName, and value
                    monitor.info(format("Key: %s.", key));
                    monitor.info(format("Value: %s.", value));
                    monitor.info(format("Node Name: %s.", nodeName));
                    nodes.add(new TargetNode(nodeName, key, value, List.of("dataspace-protocol-http")));
                }
            } catch (Exception e) {
                monitor.info("An unexpected error occurred: " + e.getMessage(), e);
            }
        }

        return nodes;
    }

    @Override
    public void insert(TargetNode node) {

    }

    @Override
    public TargetNode remove(String id) {
        return null;
    }
}