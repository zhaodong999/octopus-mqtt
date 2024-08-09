package org.octopus.server.service;

import org.apache.ibatis.session.SqlSession;
import org.octopus.db.SessionFactoryUtil;
import org.octopus.proto.service.auth.Authservice;
import org.octopus.rpc.exception.RpcRuntimeException;
import org.octopus.rpc.server.anno.RpcMethod;
import org.octopus.rpc.server.anno.RpcService;
import org.octopus.server.db.mapper.UserMapper;
import org.octopus.server.db.pojo.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@RpcService(name = "authService")
public class AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

    @RpcMethod(name = "auth")
    public Authservice.AuthResult auth(Authservice.UserMessage userMessage) {
        UserEntity userEntity = null;
        try (SqlSession sqlSession = SessionFactoryUtil.getInstance().getSessionFactory().openSession()) {
            UserMapper mapper = sqlSession.getMapper(UserMapper.class);
            userEntity = mapper.selectByName(userMessage.getName());
        } catch (Exception e) {
            throw new RpcRuntimeException(e.getMessage());
        }

        if (userEntity == null) {
            return Authservice.AuthResult.newBuilder().setAuthType(Authservice.AuthType.NOT_ACCOUNT).build();
        }

        if (Arrays.equals(userEntity.getPassword(), userMessage.getPassword().toByteArray())) {
            return Authservice.AuthResult.newBuilder().setAuthType(Authservice.AuthType.LOGIN).build();
        }

        return Authservice.AuthResult.newBuilder().setAuthType(Authservice.AuthType.PASSWORD_ERROR).build();
    }



}
