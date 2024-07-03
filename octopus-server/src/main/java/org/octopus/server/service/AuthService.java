package org.octopus.server.service;

import com.google.protobuf.Any;
import com.google.protobuf.StringValue;
import org.apache.ibatis.session.SqlSession;
import org.octopus.db.SessionFactoryUtil;
import org.octopus.proto.gateway.Server;
import org.octopus.proto.rpc.Rpc;
import org.octopus.proto.service.auth.Authservice;
import org.octopus.rpc.client.RpcClient;
import org.octopus.rpc.cluster.BalanceType;
import org.octopus.rpc.cluster.RpcClusterFactory;
import org.octopus.rpc.exception.RpcClientException;
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
            userEntity = mapper.selectByDevice(userMessage.getDevice());
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


    @RpcMethod(name = "test")
    public void test(String param) {
        RpcClient rpcClient = RpcClusterFactory.getRpcClient("gate", BalanceType.HASH, "userId");

        Server.ServerMessage.Builder builder = Server.ServerMessage.newBuilder();
        builder.setCmd(1);
        builder.setIdentity("userId_001");
        builder.setTopic("/sys/game");
        builder.setBody(Any.pack(StringValue.of("test" + param)));

        Rpc.RpcRequest.Builder requesBuilder = Rpc.RpcRequest.newBuilder();
        Rpc.RpcRequest rpcRequest = requesBuilder.setService("gate").setMethod("publish").addArgs(Any.pack(builder.build())).build();
        try {
            rpcClient.callOneway(rpcRequest);
        } catch (RpcClientException e) {
            LOGGER.info("test invoke rpc err", e);
        }
    }
}
