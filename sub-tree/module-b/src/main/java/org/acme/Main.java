package org.acme;

public class Main {

    public static void main(String[] args) {
        System.out.println("oss project: " + new LibA().getHello(args.length == 1 ? args[0] : "World"));
    }
}