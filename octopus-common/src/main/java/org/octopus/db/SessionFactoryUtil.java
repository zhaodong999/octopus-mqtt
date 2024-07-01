package org.octopus.db;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;

public class SessionFactoryUtil {

    private SqlSessionFactory sqlSessionFactory;

    private SessionFactoryUtil() {
    }

    private static class SessionFactoryHolder {
        private static final SessionFactoryUtil INSTANCE = new SessionFactoryUtil();
    }

    public static SessionFactoryUtil getInstance() {
        return SessionFactoryHolder.INSTANCE;
    }

    public void init() throws IOException {
        Reader reader = Resources.getResourceAsReader("mybatis-config.xml");
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
    }

    public SqlSessionFactory getSessionFactory() {
        return sqlSessionFactory;
    }
}
