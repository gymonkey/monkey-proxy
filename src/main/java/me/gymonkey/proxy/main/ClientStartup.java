package me.gymonkey.proxy.main;

import me.gymonkey.proxy.client.ProxyClient;


public class ClientStartup {

    public static void main(String[] args) {
        ProxyClient client = new ProxyClient("127.0.0.1", 8087);
        client.bindUninterruptibly();
    }

}
