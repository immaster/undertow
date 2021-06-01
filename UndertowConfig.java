package com.zzunz.hst;

import com.hst.ces.base.constant.CommonConst;
import com.hst.ces.base.util.PathUtil;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

/**
 * @author SunYang
 * @version 1.0
 * @description: web会控容器配置
 * @date 2021/05/28 17:25
 */
@Configuration
public class UndertowConfig {


    @Value("${server.ssl.enabled}")
    private boolean enableHttps;

    @Value("${server.webControl.port}")
    private Integer webControlPort;

    @Value("${server.ssl.key-store-password:''}")
    private String keyStorePassword;


    private static String origin = "origin";
    private static String referer = "referer";


    /**
     * web会控服务模式配置
     */
    @PostConstruct
    public void undertowFactory() {




        Undertow.Builder builder = Undertow.builder();
        String path = PathUtil.getRunDirectory() + "/webControl";
		//资源Handler
        ResourceHandler resourceHandler = new ResourceHandler(
                new PathResourceManager(Paths.get(path))).setDirectoryListingEnabled(false);
		//cors Handler
        HttpHandler httpHandler = exchange -> {
            String refer = CommonConst.START;
            if (exchange.getRequestHeaders() != null) {
                if (exchange.getRequestHeaders().get(origin) != null) {
                    refer = exchange.getRequestHeaders().get(origin).getFirst();
                } else if (exchange.getRequestHeaders().get(referer) != null) {
                    refer = exchange.getRequestHeaders().get(referer).getFirst();
                }
                if (refer.endsWith(CommonConst.SLASH)) {
                    refer.substring(0, refer.length() - 1);
                }
            }
            exchange.getResponseHeaders()
                    .put(HttpString.tryFromString("Access-Control-Allow-Origin"), refer)
                    .put(HttpString.tryFromString("Access-Control-Allow-Methods"),
                            "POST, GET, PUT, OPTIONS, DELETE")
                    .put(HttpString.tryFromString("Access-Control-Allow-Credentials"), "true");

			//do 资源Handler
            resourceHandler.handleRequest(exchange);

        };

        try {
            if (enableHttps) {
				//HTTPS
                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(getKeyManagers(), null, null);
                builder.addHttpsListener(webControlPort, "0.0.0.0", sslContext)
                        .setHandler(httpHandler).build().start();
            } else {
                builder.addHttpListener(webControlPort, "0.0.0.0").setHandler(httpHandler).build().start();
            }
        } catch (NoSuchAlgorithmException | KeyManagementException ex) {
            ex.printStackTrace();
        }
    }

    private KeyManager[] getKeyManagers() {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(this.getClass().getResourceAsStream("/keystore.p12"),
                    keyStorePassword.toCharArray());

            KeyManagerFactory keyManagerFactory = KeyManagerFactory
                    .getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());
            return keyManagerFactory.getKeyManagers();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
