package org.octopus.rpc.service;

import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;
import javassist.*;
import org.octopus.rpc.exception.GenerateClassException;
import org.octopus.rpc.exception.RpcServiceNotFound;
import org.octopus.rpc.server.anno.RpcMethod;
import org.octopus.rpc.server.anno.RpcService;
import org.octopus.rpc.util.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 创建代理类规则：
 * 1. 参数如果是pb类型的，命名为unpack_{index},参数是基础类型的，参数是基础类型的，使用param_{index}
 * 2. 返回值如果是pb类型的，命名为pack, 返回值是基础类型的，使用result
 */
public class RpcProxyManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcProxyManager.class);

    private static final String ROOT_PATH = "org.octopus.rpc.service";

    private static final String SUPER_CLASS_NAME = "ProxyWrapper";

    private static final String SUPER_CLASS_NAME_VOID = "ProxyOneWrapper";

    private static final String PROTO_BUF_ANY_NAME = "com.google.protobuf.Any";

    private final ConcurrentMap<ServiceId, ProxyWrapper> proxyMethods = new ConcurrentHashMap<>();

    private final ConcurrentMap<ServiceId, ProxyOneWrapper> proxyOneMethods = new ConcurrentHashMap<>();

    public CompletableFuture<Any> invoke(String serviceName, String method, Any... params) throws RpcServiceNotFound {
        ServiceId serviceId = new ServiceId(serviceName, method);
        if (proxyMethods.containsKey(serviceId)) {
            ProxyWrapper proxyWrapper = proxyMethods.get(serviceId);
            return CompletableFuture.supplyAsync(() -> {
                LOGGER.info("invoke proxy: {}\t{}\t", serviceId.serviceName, serviceId.methodName);
                return proxyWrapper.executor(params);
            });
        }

        // oneway
        if (!proxyOneMethods.containsKey(serviceId)) {
            LOGGER.warn("service not found: {}", serviceId);
            throw new RpcServiceNotFound(serviceId.toString());
        }

        CompletableFuture.runAsync(() -> {
            ProxyOneWrapper proxyOneWrapper = proxyOneMethods.get(serviceId);
            proxyOneWrapper.executor(params);
        });

        return null;

    }

    public Set<String> getServiceNames() {
        Set<String> hashSet = new HashSet<>(proxyMethods.keySet().stream().map(e -> e.serviceName).collect(Collectors.toSet()));
        hashSet.addAll(proxyOneMethods.keySet().stream().map(e -> e.serviceName).collect(Collectors.toSet()));
        return hashSet;
    }

    public void register(Object serviceInstance) {
        Class<?> serviceClass = serviceInstance.getClass();
        Method[] methods = serviceClass.getMethods();
        RpcService rpcServiceAnno = serviceClass.getAnnotation(RpcService.class);

        for (Method method : methods) {
            RpcMethod rpcMethodAnno = method.getAnnotation(RpcMethod.class);
            if (rpcMethodAnno == null) {
                continue;
            }

            ServiceId serviceId = new ServiceId(rpcServiceAnno.name(), rpcMethodAnno.name());

            if (proxyMethods.containsKey(serviceId)) {
                LOGGER.error("service already registered: {}", serviceId);
                continue;
            }

            generateProxy(method, serviceId, serviceClass, serviceInstance);
        }
    }

    private void generateProxy(Method method, ServiceId serviceId, Class<?> serviceClass, Object serviceInstance) {
        CtClass ctClass;
        try {
            ctClass = generateClass(serviceId, serviceClass, method);
        } catch (NotFoundException | CannotCompileException e) {
            LOGGER.error("generate service proxy class err", e);
            throw new GenerateClassException("generate service proxy class err");
        }

        try {
            ctClass.writeFile("/Users/zhaodong");
        } catch (CannotCompileException | IOException e) {
            throw new GenerateClassException("write service proxy class err");
        }

        try {
            Class<?> proxyClass = ctClass.toClass();
            Constructor<?> declaredConstructor = proxyClass.getDeclaredConstructor(serviceClass);
            if (void.class == method.getReturnType()) {
                ProxyOneWrapper proxyOneInstance = (ProxyOneWrapper) declaredConstructor.newInstance(serviceInstance);
                proxyOneMethods.put(serviceId, proxyOneInstance);
            } else {
                ProxyWrapper proxyInstance = (ProxyWrapper) declaredConstructor.newInstance(serviceInstance);
                proxyMethods.put(serviceId, proxyInstance);
            }

        } catch (CannotCompileException | InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException e) {
            throw new GenerateClassException("init service proxy instance err");
        }
    }

    private CtClass generateClass(ServiceId serviceId, Class<?> serviceClass, Method method) throws NotFoundException, CannotCompileException {
        ClassPool classPool = ClassPool.getDefault();

        CtClass rootClass = null;
        if (void.class == method.getReturnType()) {
            rootClass = classPool.get(ROOT_PATH + "." + SUPER_CLASS_NAME_VOID);
        } else {
            rootClass = classPool.get(ROOT_PATH + "." + SUPER_CLASS_NAME);
        }
        CtClass subClass = classPool.makeClass(serviceClass.getName() + ServiceUtil.firstUpperCase(serviceId.getMethodName()) + "Wrapper");
        subClass.setSuperclass(rootClass);

        CtField ctField = new CtField(classPool.get(serviceClass.getName()), "serviceInstance", subClass);
        ctField.setModifiers(Modifier.PRIVATE);
        subClass.addField(ctField);

        CtConstructor ctConstructor = new CtConstructor(new CtClass[]{classPool.get(serviceClass.getName())}, subClass);
        ctConstructor.setBody("{$0.serviceInstance = $1;}");
        subClass.addConstructor(ctConstructor);

        CtMethod ctMethod = null;
        if (void.class == method.getReturnType()) {
            ctMethod = new CtMethod(CtClass.voidType, "executor", new CtClass[]{classPool.get("com.google.protobuf.Any[]")}, subClass);
        } else {
            ctMethod = new CtMethod(classPool.get(PROTO_BUF_ANY_NAME), "executor", new CtClass[]{classPool.get("com.google.protobuf.Any[]")}, subClass);
        }
        ctMethod.setModifiers(Modifier.PUBLIC | Modifier.VARARGS);
        subClass.addMethod(ctMethod);
        ctMethod.setBody(generateMethodBody(method));
        return subClass;
    }

    private String generateMethodBody(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n{\n");

        //generate param
        generateMethodParam(method, sb);

        //invoke method
        generateMethodInvoke(method, sb);

        //generate end
        sb.append("}\n");

        LOGGER.debug("generate method: {} body: {}", method.getName(), sb);
        return sb.toString();
    }

    private void generateMethodParam(Method method, StringBuilder sb) {
        int index = 0;
        for (Class<?> parameterClass : method.getParameterTypes()) {
            if (parameterClass.isPrimitive() || parameterClass == String.class) {
                unpackPrimitive(sb, parameterClass, index);
            } else if (GeneratedMessageV3.class.isAssignableFrom(parameterClass)) {
                unpackPb(sb, index, parameterClass.getName());
            }
            ++index;
        }
    }

    private void generateMethodInvoke(Method method, StringBuilder sb) {
        if (void.class == method.getReturnType()) {
            sb.append("serviceInstance.");
        } else {
            sb.append(method.getReturnType().getName()).append(" result = serviceInstance.");
        }

        sb.append(method.getName()).append("(");
        for (int i = 0; i < method.getParameterTypes().length; i++) {
            if (method.getParameterTypes()[i].isPrimitive() || method.getParameterTypes()[i] == String.class) {
                sb.append("param");
            } else {
                sb.append("unpack");
            }
            sb.append("_").append(i);
        }
        sb.append(");\n");

        //处理有返回值的pack
        if (void.class != method.getReturnType()) {
            if (method.getReturnType().isPrimitive() || method.getReturnType() == String.class) {
                boxResult(sb, method.getReturnType());
                sb.append("return pack");
            }else if(GeneratedMessageV3.class.isAssignableFrom(method.getReturnType())){
                sb.append("return com.google.protobuf.Any.pack(result);\n");
            }
        }
    }

    private void boxResult(StringBuilder sb, Class<?> returnType) {
        if (returnType == int.class) {
            box(sb, "com.google.protobuf.Int32Value");
        } else if (returnType == String.class) {
            box(sb, "com.google.protobuf.StringValue");
        } else if (returnType == long.class) {
            box(sb, "com.google.protobuf.LongValue");
        } else if (returnType == double.class) {
            box(sb, "com.google.protobuf.DoubleValue");
        }
    }

    public void box(StringBuilder sb, String pbClassName) {
        sb.append(pbClassName).append(" pack = ");
        sb.append(pbClassName).append(".newBuilder().setValue(result).build();\n");
    }

    private void unpackPrimitive(StringBuilder sb, Type genericParameterType, int index) {
        if (genericParameterType == int.class) {
            unpackPrimitive(sb, index, "com.google.protobuf.Int32Value", "int");
        } else if (genericParameterType == String.class) {
            unpackPrimitive(sb, index, "com.google.protobuf.StringValue", "String");
        } else if (genericParameterType == long.class) {
            unpackPrimitive(sb, index, "com.google.protobuf.Int64Value", "long");
        } else if (genericParameterType == double.class) {
            unpackPrimitive(sb, index, "com.google.protobuf.DoubleValue", "double");
        }
    }

    private void unpackPb(StringBuilder sb, int index, String pbClassName) {
        String pbVar = "unpack_" + index;
        sb.append(pbClassName).append(" ").append(pbVar).append(" = ").append("(").append(pbClassName).append(")").append("$1[").append(index).append("].unpack(").append(pbClassName).append(".class);\n");
    }

    private void unpackPrimitive(StringBuilder sb, int index, String pbClassName, String primitiveName) {
        String pbVar = "unpack_" + index;
        String param = "param_" + index;
        sb.append(pbClassName).append(" ").append(pbVar).append(" = ").append("(").append(pbClassName).append(")").append("$1[").append(index).append("].unpack(").append(pbClassName).append(".class);\n")
                .append(primitiveName).append(" ").append(param).append(" = ").append(pbVar).append(".getValue();\n");
    }

    public static class ServiceId {
        private final String serviceName;
        private final String methodName;

        public ServiceId(String serviceName, String methodName) {
            this.serviceName = serviceName;
            this.methodName = methodName;
        }

        public static ServiceId of(String serviceName, String method) {
            return new ServiceId(serviceName, method);
        }

        public String getServiceName() {
            return serviceName;
        }

        public String getMethodName() {
            return methodName;
        }

        public String toString() {
            return serviceName + "." + methodName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ServiceId serviceId = (ServiceId) o;
            return Objects.equals(serviceName, serviceId.serviceName) && Objects.equals(methodName, serviceId.methodName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(serviceName, methodName);
        }


    }
}
