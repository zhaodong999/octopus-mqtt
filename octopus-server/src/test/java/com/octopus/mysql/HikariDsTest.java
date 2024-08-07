package com.octopus.mysql;


import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.octopus.db.SessionFactoryUtil;
import org.octopus.server.db.mapper.UserMapper;
import org.octopus.server.db.pojo.UserEntity;

import java.io.IOException;
import java.util.Date;

class HikariDsTest {

    @Test
    void testDatabase() throws IOException {
        SessionFactoryUtil sessionFactoryUtil = SessionFactoryUtil.getInstance();
        sessionFactoryUtil.init();
        SqlSessionFactory sessionFactory = sessionFactoryUtil.getSessionFactory();
        try (SqlSession sqlSession = sessionFactory.openSession()) {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            UserEntity userEntity = new UserEntity();
            userEntity.setName("zd");
            userEntity.setCreateTime(new Date());
            int value = userMapper.insert(userEntity);
            System.out.println(value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
