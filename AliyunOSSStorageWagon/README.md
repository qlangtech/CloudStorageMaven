# OSSStorageWagon

## Upload maven artifacts using aliyunOSS

The OSSStorageWagon project enables you to upload your artifacts to a aliyun cloud storage bucket. 

```xml
<project>
<build>
            <extensions>
                <extension>
                    <groupId>com.qlangtech.maven.cloud</groupId>
                    <artifactId>oss-storage-wagon</artifactId>
                    <version>2.3</version>
                </extension>
            </extensions>
</build>

<profiles>

   <profile>
           <id>oss-repo</id>
            <properties>
                <releasesRepository>oss://maven.qlangtech.com/release</releasesRepository>
                <snapshotRepository>oss://maven.qlangtech.com/snapshot</snapshotRepository>
            </properties>
   </profile>
</profiles>
</project>
```

And in local user_home `vi ~/aliyun-oss/mvn-config.properties` shall add aliyun bucket config.

```properties
endpoint=http://oss-cn-hangzhou.aliyuncs.com
accessKey=xxxxxxxxx
secretKey=xxxxxxxxx
bucketName=kkkk
```

Then you can deploy artifact by sh command `mvn deploy -Poss-repo`


