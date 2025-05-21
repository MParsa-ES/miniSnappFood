package org.example;

import HTTPhandler.UserHTTPHandler;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws Exception {


        HttpServer server = HttpServer.create(new InetSocketAddress(8080),0);
        server.createContext("/auth/register",new UserHTTPHandler());
        server.createContext("/auth/login",new UserHTTPHandler());
        server.start();
        System.out.println("Server started on http://localhost:8080");
    }
}
