package com.gkatzioura.maven.cloud.oss;

import org.apache.commons.io.FileUtils;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.gkatzioura.maven.cloud.resolver.KeyResolver;
import com.gkatzioura.maven.cloud.transfer.TransferProgress;
import com.gkatzioura.maven.cloud.transfer.TransferProgressImpl;
import com.gkatzioura.maven.cloud.wagon.AbstractStorageWagon;
import com.gkatzioura.maven.cloud.wagon.PublicReadProperty;

/**
 * baisui(baisui@qlangtech.com)
 * 2023-06-23 09:21
 **/
public class OSSStorageWagon extends AbstractStorageWagon {
    private OSSStorageRepository ossRepository;
    private final KeyResolver keyResolver = new KeyResolver();
    private static final Logger LOGGER = Logger.getLogger(OSSStorageWagon.class.getName());
   // public static final Pattern TIS_PKG_TPI_EXTENSION = Pattern.compile(".+?\\.(tpi|tar)(\\.[^\\.]+)?$");

    public static final Pattern TIS_PKG_TPI_EXTENSION = Pattern.compile(".+?\\.(tar)(\\.[^\\.]+)?$");


    @Override
    public void get(String resourceName, File destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        Resource resource = new Resource(resourceName);
        transferListenerContainer.fireTransferInitiated(resource, TransferEvent.REQUEST_GET);
        transferListenerContainer.fireTransferStarted(resource, TransferEvent.REQUEST_GET, destination);
        resource.setContentLength(WagonConstants.UNKNOWN_LENGTH);
        final TransferProgress transferProgress = new TransferProgressImpl(resource, TransferEvent.REQUEST_GET, transferListenerContainer);

        try {
            ossRepository.copy(resourceName, destination, transferProgress);
            transferListenerContainer.fireTransferCompleted(resource, TransferEvent.REQUEST_GET);
        } catch (Exception e) {
            transferListenerContainer.fireTransferError(resource, TransferEvent.REQUEST_GET, e);
            throw e;
        }
    }

    @Override
    public boolean getIfNewer(String resourceName, File destination, long timeStamp) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        if (ossRepository.newResourceAvailable(resourceName, timeStamp)) {
            get(resourceName, destination);
            return true;
        }
        return false;
    }

    @Override
    public void put(File file, String resourceName) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        Resource resource = new Resource(resourceName);

        Matcher matcher = TIS_PKG_TPI_EXTENSION.matcher(resourceName);
        if (matcher.matches()) {
            LOGGER.log(Level.FINER, String.format("skip tip deploy: %s", resourceName));
            return;
        }

        LOGGER.log(Level.FINER, String.format("Uploading file %s to %s", file.getAbsolutePath(), resourceName));

        transferListenerContainer.fireTransferInitiated(resource, TransferEvent.REQUEST_PUT);
        transferListenerContainer.fireTransferStarted(resource, TransferEvent.REQUEST_PUT, file);
        final TransferProgress transferProgress = new TransferProgressImpl(resource, TransferEvent.REQUEST_PUT, transferListenerContainer);

        try {
            ossRepository.put(file, resourceName, transferProgress);
            transferListenerContainer.fireTransferCompleted(resource, TransferEvent.REQUEST_PUT);
        } catch (TransferFailedException e) {
            transferListenerContainer.fireTransferError(resource, TransferEvent.REQUEST_PUT, e);
            throw e;
        }
    }

    @Override
    public void putDirectory(File source, String destination)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        Collection<File> allFiles = FileUtils.listFiles(source, null, true);
        String relativeDestination = destination;
        //removes the initial .
        if (destination != null && destination.startsWith(".")) {
            relativeDestination = destination.length() == 1 ? "" : destination.substring(1);
        }
        for (File file : allFiles) {
            //compute relative path
            String relativePath = PathUtils.toRelative(source, file.getAbsolutePath());
            put(file, relativeDestination + "/" + relativePath);
        }
    }

    @Override
    public boolean resourceExists(String resourceName) throws TransferFailedException, AuthorizationException {
        return ossRepository.exists(resourceName);
    }

    @Override
    public List<String> getFileList(String destDir) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        // try {
        List<String> list = ossRepository.list(destDir);
        list = convertS3ListToMavenFileList(list, destDir);
        if (list.isEmpty()) {
            throw new ResourceDoesNotExistException(destDir);//expected by maven
        }
        return list;
//        } catch (AmazonS3Exception e) {
//            throw new TransferFailedException("Could not fetch objects for prefix "+s);
//        }
    }

    //removes the prefix path
    //adds folders files
    private List<String> convertS3ListToMavenFileList(List<String> list, String path) {
        String prefix = keyResolver.resolve(ossRepository.getBaseDirectory(), path);
        Set<String> folders = new HashSet<>();
        List<String> result = list.stream().map(key -> {
            String filePath = key;
            //removes the prefix from the object path
            if (prefix != null && prefix.length() > 0) {
                filePath = key.substring(prefix.length() + 1);
            }
            extractFolders(folders, filePath);
            return filePath;
        }).collect(Collectors.toList());
        result.addAll(folders);
        return result;
    }

    private void extractFolders(Set<String> folders, String filePath) {
        if (filePath.contains("/")) {
            String folder = filePath.substring(0, filePath.lastIndexOf('/'));
            folders.add(folder + '/');
            if (folder.contains("/")) {//recurse
                extractFolders(folders, folder);
            }//else we already stored it.
        } else {
            folders.add(filePath);
        }
    }


    @Override
    public void connect(Repository repository, AuthenticationInfo authenticationInfo, ProxyInfoProvider proxyInfoProvider) throws ConnectionException, AuthenticationException {

        this.repository = repository;
        this.sessionListenerContainer.fireSessionOpening();

        //final String bucket = accountResolver.resolve(repository);
        final String directory = containerResolver.resolve(repository);

        LOGGER.log(Level.FINER, String.format("Opening connection for  and directory %s", directory));
        ossRepository = new OSSStorageRepository(directory, new PublicReadProperty(true));
        ossRepository.connect(authenticationInfo, null);

        sessionListenerContainer.fireSessionLoggedIn();
        sessionListenerContainer.fireSessionOpened();
    }

    @Override
    public void disconnect() throws ConnectionException {
        sessionListenerContainer.fireSessionDisconnecting();
        ossRepository.disconnect();
        sessionListenerContainer.fireSessionLoggedOff();
        sessionListenerContainer.fireSessionDisconnected();
    }
}
