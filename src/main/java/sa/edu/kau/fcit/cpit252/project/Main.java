package sa.edu.kau.fcit.cpit252.project;

/**
 * A wrapper class to launch the JavaFX application.
 * This is a common workaround to fix the "JavaFX runtime components are missing" error
 * when running JavaFX applications directly from an IDE without configuring VM module arguments.
 */
public class Main {
    public static void main(String[] args) {
        App.main(args);
    }
}
