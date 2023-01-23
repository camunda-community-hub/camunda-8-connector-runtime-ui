package org.camunda.runtime.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.camunda.runtime.exception.TechnicalException;
import org.camunda.runtime.jsonmodel.Connector;
import org.camunda.runtime.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ConnectorStorageService {

  public static final String CONNECTORS = "connectors";
  public static final String JARS = "jars";

  @Value("${workspace:workspace}")
  private String workspace;

  public Path resolve(String name) {
    return Path.of(workspace).resolve(CONNECTORS).resolve(name);
  }

  public List<Connector> all() throws TechnicalException {
    File[] connectorFiles = Path.of(workspace).resolve(CONNECTORS).toFile().listFiles();
    List<Connector> connectors = new ArrayList<>();
    for (File file : connectorFiles) {
      if (!file.getName().equals(JARS) && !file.getName().equals("secrets.json")) {
        connectors.add(findByName(file.getName()));
      }
    }
    return connectors;
  }

  public Connector findByName(String connectorName) throws TechnicalException {
    try {
      return JsonUtils.fromJsonFile(resolve(connectorName), Connector.class);
    } catch (IOException e) {
      throw new TechnicalException("Error reading the connector " + connectorName, e);
    }
  }

  public Connector save(Connector connector) throws TechnicalException {
    try {
      connector.setModified(new Date());
      JsonUtils.toJsonFile(resolve(connector.getName()), connector);
      return connector;
    } catch (IOException e) {
      throw new TechnicalException("Error saving the connector " + connector.getName(), e);
    }
  }

  public File storeJarFile(MultipartFile file) throws TechnicalException {
    try {
      File target =
          Path.of(workspace)
              .resolve(CONNECTORS)
              .resolve(JARS)
              .resolve(file.getOriginalFilename())
              .toFile();
      if (!target.exists()) {
        target.createNewFile();
      }
      try (FileOutputStream out = new FileOutputStream(target)) {
        IOUtils.copy(file.getInputStream(), out);
      }
      return target;
    } catch (IOException e) {
      throw new TechnicalException("Error storing the library", e);
    }
  }

  public Path getJarPath(Connector connector) {
    return Path.of(workspace).resolve(CONNECTORS).resolve(JARS).resolve(connector.getJarFile());
  }

  public void deleteByName(String name) throws TechnicalException {
    try {
      Files.delete(resolve(name));
    } catch (IOException e) {
      throw new TechnicalException("Error storing the library", e);
    }
  }
}
