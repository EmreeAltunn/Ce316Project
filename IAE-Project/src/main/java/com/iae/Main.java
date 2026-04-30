package com.iae;

import com.iae.database.DatabaseManager;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, IAE Project!");
        System.out.println("Project setup is working perfectly.");
        DatabaseManager.initializeDatabase();
    }
}
