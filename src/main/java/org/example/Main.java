package org.example;

import HTTPhandler.BuyerHTTPHandler;
import HTTPhandler.RestaurantHttpHandler;
import HTTPhandler.UserHTTPHandler;
import HTTPhandler.ProfileHTTPHandler;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws Exception {


        HttpServer server = HttpServer.create(new InetSocketAddress(8080),0);
        server.createContext("/auth", new UserHTTPHandler());
        server.createContext("/auth/profile", new ProfileHTTPHandler());
        server.createContext("/restaurants", new RestaurantHttpHandler());
        server.createContext("/vendors", new BuyerHTTPHandler());
        server.start();
        System.out.println("Server started on http://localhost:8080");
    }
}
