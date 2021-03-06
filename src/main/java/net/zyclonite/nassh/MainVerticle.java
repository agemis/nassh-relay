/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 *
 * Website: https://github.com/zyclonite/nassh-relay
 *
 * Copyright 2014-2018   zyclonite    networx
 *                       http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CorsHandler;
import net.zyclonite.nassh.handler.*;

@SuppressWarnings("unused")
public class MainVerticle extends AbstractVerticle {
    private static Logger logger = LoggerFactory.getLogger(MainVerticle.class);
    private HttpServer server;

    @Override
    public void start(final Future<Void> startFuture) {
        final JsonObject config = config().getJsonObject("webservice");
        server = vertx.createHttpServer();
        final Router router = Router.router(vertx);
        router.route().handler(CorsHandler
            .create(".*")
            .allowCredentials(true)
        );
        router.route().handler(io.vertx.ext.web.handler.CookieHandler.create());
        router.get("/cookie").handler(new CookieHandler(config().getJsonObject("application").copy().put("auth", config().getJsonObject("google-sso"))));
        router.post("/cookie").handler(new CookiePostHandler(vertx, new JsonObject().put("auth", config().getJsonObject("google-sso"))));
        router.get("/proxy").handler(new ProxyHandler(vertx, config()));
        router.get("/write").handler(new WriteHandler(vertx));
        router.get("/read").handler(new ReadHandler(vertx));
        server.requestHandler(router);
        server.websocketHandler(new ConnectHandler(vertx));
        server.listen(config.getInteger("port", 8022), config.getString("hostname", "localhost"),
            result -> {
                if (result.succeeded()) {
                    logger.info("nassh-relay listening on port " + result.result().actualPort());
                    startFuture.complete();
                } else {
                    startFuture.fail(result.cause());
                }
            }
        );
    }

    @Override
    public void stop(final Future<Void> stopFuture) {
        logger.debug("stopped");
        if (server != null) {
            server.close(complete -> stopFuture.complete());
        } else {
            stopFuture.complete();
        }
    }
}
