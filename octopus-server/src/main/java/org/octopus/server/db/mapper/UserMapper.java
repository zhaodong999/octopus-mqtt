package org.octopus.server.db.mapper;

import org.apache.ibatis.annotations.Insert;
import org.octopus.server.db.pojo.UserEntity;

public interface UserMapper {

    @Insert("INSERT INTO user(name, age, email, createTime) values (#{name}, #{age}, #{email}, #{createTime})")
    int insert(UserEntity userEntity);

}
