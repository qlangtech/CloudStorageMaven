package com.gkatzioura.maven.cloud.oss;

import java.util.regex.Matcher;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-06-24 11:54
 **/
public class TestOSSStorageWagon {

    @Test
    public void testFilterTpiPattern() {
        Matcher matcher = OSSStorageWagon.TIS_PKG_TPI_EXTENSION.matcher("test/test/plugin.tpi");
        Assert.assertTrue(matcher.matches());

        matcher = OSSStorageWagon.TIS_PKG_TPI_EXTENSION.matcher("test/test/plugin.jar");
        Assert.assertFalse(matcher.matches());

        matcher = OSSStorageWagon.TIS_PKG_TPI_EXTENSION.matcher("test/test/plugin.tpi.md5");
        Assert.assertTrue(matcher.matches());

        matcher = OSSStorageWagon.TIS_PKG_TPI_EXTENSION.matcher("test/test/plugin.jar.md5");
        Assert.assertFalse(matcher.matches());

        matcher = OSSStorageWagon.TIS_PKG_TPI_EXTENSION.matcher("test/test/plugin.tar.md5");
        Assert.assertTrue(matcher.matches());

        matcher = OSSStorageWagon.TIS_PKG_TPI_EXTENSION.matcher("test/test/plugin.tar");
        Assert.assertTrue(matcher.matches());

        matcher = OSSStorageWagon.TIS_PKG_TPI_EXTENSION.matcher("test/test/plugin.tar.gz");
        Assert.assertTrue(matcher.matches());
    }
}
