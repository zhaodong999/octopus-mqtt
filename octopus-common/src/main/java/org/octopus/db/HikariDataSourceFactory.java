package org.octopus.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.datasource.DataSourceFactory;

import javax.sql.DataSource;
import java.util.Properties;

public class HikariDataSourceFactory implements DataSourceFactory {

    private DataSource dataSource;
    private Properties properties;

    @Override
    public void setProperties(Properties props) {
        this.properties = props;
    }

    @Override
    public DataSource getDataSource() {
        if (dataSource != null) {
            return dataSource;
        }
        HikariConfig config = new HikariConfig(properties);
        dataSource = new HikariDataSource(config);
        return dataSource;
    }
}