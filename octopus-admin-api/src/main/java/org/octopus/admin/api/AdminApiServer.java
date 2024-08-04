package org.octopus.admin.api;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.octopus.admin.api.filter.CORSFilter;
import org.octopus.admin.api.provider.GsonMessageBodyHandler;
import org.octopus.admin.api.provider.ResponseFilter;
import org.octopus.admin.api.rest.MsgRest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public class AdminApiServer {


    private static final Logger LOGGER = LoggerFactory.getLogger(AdminApiServer.class);

    public static void main(String[] args) {
        try {
            //2.初始化http server
            String port = "8000";
            if (args.length != 0) {
                port = args[0];
            }

            Set<Class<?>> resources = new HashSet<>();
            resources.add(GsonMessageBodyHandler.class);
            resources.add(ResponseFilter.class);
            resources.add(CORSFilter.class);
            resources.add(MsgRest.class);

            ResourceConfig resourceConfig = new ResourceConfig(resources);

            URI baseUri = UriBuilder.fromUri("http://0.0.0.0/").port(Integer.parseInt(port)).build();
            HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig);

            Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));
            LOGGER.info("server start listen");
            Thread.currentThread().join();
        } catch (InterruptedException ex) {
            LOGGER.error("start web Server rest api error", ex);
            Thread.currentThread().interrupt();
        }

    }
}
