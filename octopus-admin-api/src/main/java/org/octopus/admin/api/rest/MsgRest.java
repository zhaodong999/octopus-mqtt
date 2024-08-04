package org.octopus.admin.api.rest;

import org.octopus.admin.api.model.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("msg")
public class MsgRest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MsgRest.class);

    @Path("get")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Activity getMsg() {
        LOGGER.info("invoke");
        return new Activity("1", "hello", "hello", 1, 1.0);
    }

    @Path("test")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getTest() {
        return "hello";
    }
}
