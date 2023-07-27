/*
 * Copyright 2018 Emmanouil Gkatziouras
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gkatzioura.maven.cloud.oss;

import org.apache.commons.io.IOUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.gkatzioura.maven.cloud.resolver.KeyResolver;
import com.gkatzioura.maven.cloud.transfer.TransferProgress;
import com.gkatzioura.maven.cloud.transfer.TransferProgressFileInputStream;
import com.gkatzioura.maven.cloud.transfer.TransferProgressFileOutputStream;
import com.gkatzioura.maven.cloud.wagon.PublicReadProperty;
import com.qlangtech.tis.AliyunOSSCfg;
import com.qlangtech.tis.OssConfig;

public class OSSStorageRepository {

    public static final String OSS_CONFIG_NAME = "mvn-config";

    //private final String bucket;
    private final String baseDirectory;

    private final KeyResolver keyResolver = new KeyResolver();
    private OssConfig ossCfg;
    private OSS aliyunOSS;
    private PublicReadProperty publicReadProperty;

    private static final Logger LOGGER = Logger.getLogger(OSSStorageRepository.class.getName());

    public OSSStorageRepository() {
        // this.bucket = bucket;
        this.baseDirectory = "";
        this.publicReadProperty = new PublicReadProperty(false);
    }

    public OSSStorageRepository(PublicReadProperty publicReadProperty) {

        this.baseDirectory = "";
        this.publicReadProperty = publicReadProperty;
    }

    public OSSStorageRepository(String baseDirectory) {

        this.baseDirectory = baseDirectory;
        this.publicReadProperty = new PublicReadProperty(false);
    }

    public OSSStorageRepository(String baseDirectory, PublicReadProperty publicReadProperty) {

        this.baseDirectory = baseDirectory;
        this.publicReadProperty = publicReadProperty;
    }


    public void connect(AuthenticationInfo authenticationInfo, String region) throws AuthenticationException {
        this.ossCfg = AliyunOSSCfg.getInstance().getOss(OSS_CONFIG_NAME); // TaskConfig.getInstance().getOss();
        Objects.requireNonNull(ossCfg, "ossCfg can not be null");
        this.aliyunOSS = this.ossCfg.getOSS();// TaskConfig.getInstance().getOSSClient(); //S3Connect.connect(authenticationInfo, region, endpoint, pathStyle);
    }

    public void copy(String resourceName, File destination, TransferProgress transferProgress) throws TransferFailedException, ResourceDoesNotExistException {

        final String key = resolveKey(resourceName);

        try {

            final OSSObject ossObj;
            try {
                ossObj = aliyunOSS.getObject(this.ossCfg.getBucket(), key);
            } catch (OSSException e) {
                throw new ResourceDoesNotExistException("Resource does not exist");
            }
            destination.getParentFile().mkdirs();//make sure the folder exists or the outputStream will fail.
            try (OutputStream outputStream = new TransferProgressFileOutputStream(destination, transferProgress);
                 InputStream inputStream = ossObj.getObjectContent()) {
                IOUtils.copy(inputStream, outputStream);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not transfer file", e);
            throw new TransferFailedException("Could not download resource " + key);
        }
    }

    public void put(File file, String destination, TransferProgress transferProgress) throws TransferFailedException {

        final String key = resolveKey(destination);

        try {
            try (InputStream inputStream = new TransferProgressFileInputStream(file, transferProgress)) {
                PutObjectRequest putObjectRequest = new PutObjectRequest(this.ossCfg.getBucket(), key, inputStream, createContentLengthMetadata(file));
                applyPublicRead(putObjectRequest);
                aliyunOSS.putObject(putObjectRequest);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not transfer file ", e);
            throw new TransferFailedException("Could not transfer file " + file.getName());
        }
    }

    private ObjectMetadata createContentLengthMetadata(File file) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.length());
        return metadata;
    }

    public boolean newResourceAvailable(String resourceName, long timeStamp) throws ResourceDoesNotExistException {

        final String key = resolveKey(resourceName);

        LOGGER.log(Level.FINER, String.format("Checking if new key %s exists", key));

        // try {
        ObjectMetadata objectMetadata = aliyunOSS.getObjectMetadata(this.ossCfg.getBucket(), key);

        long updated = objectMetadata.getLastModified().getTime();
        return updated > timeStamp;
//        } catch (AmazonS3Exception e) {
//            LOGGER.log(Level.SEVERE, String.format("Could not retrieve %s", key), e);
//            throw new ResourceDoesNotExistException("Could not retrieve key " + key);
//        }
    }


    public List<String> list(String path) {

        String key = resolveKey(path);

        ListObjectsRequest listRequest = (ListObjectsRequest) new ListObjectsRequest()
                .withBucketName(this.ossCfg.getBucket()).withKey(key);

        ObjectListing objectListing = aliyunOSS.listObjects(listRequest);
        // .withPrefix(key));
        List<String> objects = new ArrayList<>();
        retrieveAllObjects(objectListing, objects);
        return objects;
    }

    private void applyPublicRead(PutObjectRequest putObjectRequest) {
        if (publicReadProperty.get()) {
           // LOGGER.info("Public read was set to true");
            //putObjectRequest.withCannedAcl(CannedAccessControlList.PublicRead);
        }
    }

    private void retrieveAllObjects(ObjectListing objectListing, List<String> objects) {

        objectListing.getObjectSummaries().forEach(os -> objects.add(os.getKey()));

        if (objectListing.isTruncated()) {
//            ObjectListing nextObjectListing = amazonS3.listNextBatchOfObjects(objectListing);
//            retrieveAllObjects(nextObjectListing, objects);
        }
    }

    public boolean exists(String resourceName) {

        final String key = resolveKey(resourceName);

        try {
            aliyunOSS.getObjectMetadata(this.ossCfg.getBucket(), key);
            return true;
        } catch (OSSException e) {
            return false;
        }
    }

    public void disconnect() {
        this.aliyunOSS = null;
        this.ossCfg = null;
    }

    private String resolveKey(String path) {
        return keyResolver.resolve(baseDirectory, path);
    }


    public String getBaseDirectory() {
        return baseDirectory;
    }


}
