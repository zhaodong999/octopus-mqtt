package org.octopus.server.db.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.octopus.server.db.pojo.UserEntity;

public interface UserMapper {

    @Insert("INSERT INTO user(name, device, password, createTime) values (#{name}, #{device}, #{password}, #{createTime})")
    int insert(UserEntity userEntity);

    @Select("SELECT * FROM user WHERE device = #{device}")
    UserEntity selectByDevice(String device);
}
