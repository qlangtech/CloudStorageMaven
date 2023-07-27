package com.gkatzioura.maven.cloud.oss;

import org.codehaus.plexus.util.StringUtils;

public class OssConfig {

    private String endpoint;
    private String accessId;
    private String accessSecret;
    private String bucket;

    public String getAccessSecret() {
        return accessSecret;
    }

    public void setAccessSecret(String accessSecret) {
        this.accessSecret = accessSecret;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessId() {
        return accessId;
    }

    public void setAccessId(String accessId) {
        this.accessId = accessId;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public void validate() {
        if (StringUtils.isEmpty(this.accessId) || StringUtils.isEmpty(this.endpoint) || StringUtils.isEmpty(this.bucket)
                || StringUtils.isEmpty(this.accessSecret)) {
            throw new IllegalStateException("illegal " + this.toString());
        }
    }

    @Override
    public String toString() {
        return "OssConfig{" +
                "endpoint='" + endpoint + '\'' +
                ", accessId='" + accessId + '\'' +
                ", accessSecret='" + accessSecret + '\'' +
                ", bucket='" + bucket + '\'' +
                '}';
    }
}
