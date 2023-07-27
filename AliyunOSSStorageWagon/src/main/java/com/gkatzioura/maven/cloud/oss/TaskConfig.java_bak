package com.gkatzioura.maven.cloud.oss;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.codehaus.plexus.util.StringUtils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;

public class TaskConfig {

    private static TaskConfig instance;


    private OssConfig oss;


    private TaskConfig() {

    }


    public OssConfig getOss() {
        return this.oss;
    }

    private OSS ossClient;

    public OSS getOSSClient() {

        if (ossClient == null) {
            ossClient = new OSSClientBuilder().build(oss.getEndpoint(), oss.getAccessId(), oss.getAccessSecret());
        }
        return ossClient;
    }


    public static TaskConfig getInstance() {
        if (instance == null) {
            synchronized (TaskConfig.class) {
                if (instance == null) {

                    instance = new TaskConfig();

                    // ResourceBundle bundle = ResourceBundle.getBundle("config/config");

                    // instance.tableFilter = new ArrayList<>();
                    String mvnOssConfigName = "aliyun-oss/mvn-config.properties";
                    File cfgFile = new File(System.getProperty("user.home"), mvnOssConfigName);
                    if (!cfgFile.exists()) {
                        throw new IllegalStateException("oss config file is not exist:" + cfgFile.getAbsoluteFile() + "\n config.properties template is ");
                    }
                    Properties props = new Properties();
                    try {
                        try (InputStream reader = FileUtils.openInputStream(cfgFile)) {
                            props.load(reader);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }

                    String endpoint = getProp(props, "endpoint");
                    String accessKeyId = getProp(props, "accessKey");
                    String secretKey = getProp(props, "secretKey");
                    String bucketName = getProp(props, "bucketName");


                    final OssConfig oss = new OssConfig();
                    oss.setAccessId(accessKeyId);
                    oss.setBucket(bucketName);
                    oss.setEndpoint(endpoint);
                    oss.setAccessSecret(secretKey);

                    oss.validate();
                    instance.oss = oss;
                }
            }
        }
        return instance;
    }

    private static String getProp(Properties props, String key) {
        String value = props.getProperty(key);
        if (StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException("key:" + key + " relevant value can not be null");
        }
        return value;
    }

}
