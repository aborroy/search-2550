package org.alfresco.rest.client.cmis;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

@Service
public class CmisClient {

    @Value("${alfresco.repository.url}")
    String alfrescoUrl;
    @Value("${alfresco.repository.user}")
    String alfrescoUser;
    @Value("${alfresco.repository.pass}")
    String alfrescoPass;
    @Value("${cmis.timeout}")
    String timeOut;

    Session session;

    @PostConstruct
    public void init() {
        String alfrescoBrowserUrl = alfrescoUrl + "/api/-default-/public/cmis/versions/1.1/browser";

        Map<String, String> parameter = new HashMap<>();

        parameter.put(SessionParameter.USER, alfrescoUser);
        parameter.put(SessionParameter.PASSWORD, alfrescoPass);

        parameter.put(SessionParameter.BROWSER_URL, alfrescoBrowserUrl);
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.BROWSER.value());

        parameter.put(SessionParameter.CONNECT_TIMEOUT, timeOut);
        parameter.put(SessionParameter.READ_TIMEOUT, timeOut);

        SessionFactory factory = SessionFactoryImpl.newInstance();
        session = factory.getRepositories(parameter).get(0).createSession();
    }

    public Folder getRootFolder() {
        return session.getRootFolder();
    }

    public Folder getFolderByPath(String path) {
        return (Folder) session.getObjectByPath(path);
    }

    public Folder createFolder(Folder parentFolder, String name) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_FOLDER.value());
        properties.put(PropertyIds.NAME, name);
        return parentFolder.createFolder(properties);
    }

    public Document createSimpleDocument(Folder folder, String name) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_DOCUMENT.value());
        properties.put(PropertyIds.NAME, name);

        byte[] content = "Hello World!".getBytes();
        InputStream stream = new ByteArrayInputStream(content);
        ContentStream contentStream = new ContentStreamImpl(name, BigInteger.valueOf(content.length),
                MimeTypeUtils.TEXT_PLAIN_VALUE, stream);

        return folder.createDocument(properties, contentStream, VersioningState.MAJOR);
    }
}
