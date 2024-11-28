package it.unibo.mvc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public final class DrawNumberApp implements DrawNumberViewObserver {
    
    private final DrawNumber model;
    private final List<DrawNumberView> views;

    
    /**
     * @param fileName
     * @param views
     * @throws FileNotFoundException
     */
    public DrawNumberApp(final String fileName, final DrawNumberView... views) throws FileNotFoundException {
        /*
         * Side-effect proof
         */
        this.views = Arrays.asList(Arrays.copyOf(views, views.length));
        for (final DrawNumberView view: views) {
            view.setObserver(this);
            view.start();
        }
        final Configuration c = readFile(fileName).build();
        if (c.isConsistent()) {
            this.model = new DrawNumberImpl(c);
        } else {
            displayError("Inconsistent configuration: " 
            + "min: " + c.getMin() + ", "
            + "max: " + c.getMax() + ", "
            + "attempts: " + c.getAttempts() + ". Using defaults instead.");
            this.model = new DrawNumberImpl(new Configuration.Builder().build());
        }
    }

    private Configuration.Builder readFile(final String fileName) {
        final Configuration.Builder config = new Configuration.Builder();
        try (var texts = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(fileName)))) {
            for (var line = texts.readLine(); line != null; line = texts.readLine()) {
                final String[] elem = line.split(":");
                if(elem.length == 2) {
                    final int value = Integer.parseInt(elem[1].trim());
                    final String key = elem[0].trim();
                    switch(key) {
                        case "maximum":
                            config.setMax(value);
                            break;
                        case "minimum": 
                            config.setMin(value);
                            break;
                        case "attempts":
                            config.setAttempts(value);
                            break;
                        default:
                            displayError("Out of range");
                            break;
                    }
                } else {
                    displayError("I cannot understand \"" + line + '"');
                }
            }
        } catch (IOException | NumberFormatException e) {
            displayError(e.getMessage());
        }
        return config;
    }
        
    private void displayError(final String err) {
        for (final DrawNumberView view : views) {
                view.displayError(err);
            }
    }
            
    @Override
    public void newAttempt(final int n) {
        try {
            final DrawResult result = model.attempt(n);
            for (final DrawNumberView view: views) {
                view.result(result);
            }
        } catch (IllegalArgumentException e) {
            for (final DrawNumberView view: views) {
                view.numberIncorrect();
            }
        }
    }
            
    @Override
    public void resetGame() {
        this.model.reset();
    }
            
    @Override
    public void quit() {
        /*
        * A bit harsh. A good application should configure the graphics to exit by
        * natural termination when closing is hit. To do things more cleanly, attention
        * should be paid to alive threads, as the application would continue to persist
        * until the last thread terminates.
        */
        System.exit(0);
    }
            
    public static void main(final String... args) throws FileNotFoundException {
        new DrawNumberApp("config.yml",
            new DrawNumberViewImpl(),
            new DrawNumberViewImpl(),
            new PrintStreamView(System.out),
            new PrintStreamView("output.log"));
    }
}
