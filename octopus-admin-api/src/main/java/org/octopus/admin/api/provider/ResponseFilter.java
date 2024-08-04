package org.octopus.admin.api.provider;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@Provider
@Priority(1)
public class ResponseFilter implements WriterInterceptor {

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        final OutputStream originalOutputStream = context.getOutputStream();
        ByteArrayOutputStream modifiedOutputStream = new ByteArrayOutputStream();

        // 将输出流替换为我们的ByteArrayOutputStream
        context.setOutputStream(modifiedOutputStream);

        // 继续链路的执行
        context.proceed();

        // 在这里修改响应体
        String responseBody = modifiedOutputStream.toString();
        responseBody = responseBody.replace("oldString", "newString");

        // 将修改后的响应体写回到原始输出流中
        originalOutputStream.write(responseBody.getBytes());

        // 重设输出流
        context.setOutputStream(originalOutputStream);
    }
}
