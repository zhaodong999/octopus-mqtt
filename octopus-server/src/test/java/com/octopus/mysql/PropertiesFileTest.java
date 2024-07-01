package com.octopus.mysql;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

class PropertiesFileTest {

    @Test
    void testFile() throws IOException {
        Properties properties = new Properties();
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("hikaricp.properties");
        properties.load(resourceAsStream);
        Assertions.assertEquals("com.mysql.cj.jdbc.Driver", properties.getProperty("dataSourceClassName"));
    }
}
