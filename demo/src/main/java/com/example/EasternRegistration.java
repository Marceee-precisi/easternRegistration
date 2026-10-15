package com.example;

/**
 * Starts the Eastern Registration console application.
 */
public final class EasternRegistration {
    private EasternRegistration() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Starts the main menu.
     *
     * @param args command-line arguments (not used)
     * @throws Exception if schedule data cannot be retrieved
     */
    public static void main(String[] args) throws Exception {
        MainMenu menu = new MainMenu();
        menu.open();
    }
}
