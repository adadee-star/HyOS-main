package com.adam;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HyOS extends Application {

    private Pane desktop;
    private StackPane root;
    private HBox dock;
    private VBox loginScreen;
    private Label systemClock;

    private boolean isRoot = false;
    private String currentUser = "admin";
    private String osName = "hyOS Apex v10.2";
    private List<String> virtualDisk = new ArrayList<>(
            List.of("kernel.sys", "config.cfg", "media_cache.tmp", "readme.txt"));

    // Stores actual text content for .txt files saved by the user
    private final java.util.Map<String, String> virtualDiskContents = new java.util.HashMap<>();

    // ── Theme state ─────────────────────────────────────────────────────────────
    public enum Theme {
        DARK("Dark", "#0f172a", "#1e293b", "#334155", "#38bdf8", "#94a3b8"),
        LIGHT("Light", "#e2e8f0", "#f8fafc", "#cbd5e1", "#0284c7", "#1e293b"),
        DRACULA("Dracula", "#282a36", "#44475a", "#6272a4", "#bd93f9", "#f8f8f2"),
        SOLARIZED("Solar", "#002b36", "#073642", "#586e75", "#2aa198", "#839496");

        public final String label, bg, surface, border, accent, text;

        Theme(String label, String bg, String surface, String border, String accent, String text) {
            this.label = label;
            this.bg = bg;
            this.surface = surface;
            this.border = border;
            this.accent = accent;
            this.text = text;
        }
    }

    private Theme currentTheme = Theme.DARK;

    // Apply the theme to the desktop + dock backgrounds
    private void applyTheme(Theme t) {
        currentTheme = t;
        desktop.setStyle("-fx-background-color: " + t.bg + ";");
        dock.setStyle("-fx-background-color: " + t.surface
                + "; -fx-background-radius: 20; -fx-border-color: " + t.border + ";");
        systemClock.setStyle("-fx-text-fill: " + t.accent
                + "; -fx-font-family: monospace; -fx-font-weight: bold;");
        root.setStyle("-fx-background-color: " + t.bg + ";");
    }

    // ── Startup ──────────────────────────────────────────────────────────────────
    @Override
    public void start(Stage primaryStage) {
        root = new StackPane();
        root.setStyle("-fx-background-color: #0f172a;");

        desktop = new Pane();
        desktop.setVisible(false);

        dock = new HBox(15);
        dock.setAlignment(Pos.CENTER);
        dock.setPadding(new Insets(10, 25, 10, 25));
        dock.setStyle(
                "-fx-background-color: rgba(15,23,42,0.9); -fx-background-radius: 20; -fx-border-color: #334155;");
        dock.setMaxHeight(65);
        dock.setVisible(false);

        systemClock = new Label();
        systemClock.setStyle("-fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-font-weight: bold;");
        startClockUpdate();

        refreshLauncher();
        createLoginScreen();

        VBox mainLayout = new VBox(desktop, dock);
        VBox.setVgrow(desktop, Priority.ALWAYS);
        StackPane.setMargin(dock, new Insets(0, 0, 15, 0));

        root.getChildren().addAll(mainLayout, loginScreen);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle(osName);
        primaryStage.setFullScreenExitHint("");
        primaryStage.setFullScreen(true);
        primaryStage.show();
    }

    private void refreshLauncher() {
        dock.getChildren().clear();
        dock.getChildren().addAll(
                createLauncher("🐚", "Terminal", () -> spawnWindow("Terminal", createTerminal())),
                createLauncher("🌐", "Browser",
                        () -> spawnWindow("Web Browser", createWebBrowser("https://www.google.com"))),
                createLauncher("📂", "hyDisk", () -> spawnWindow("Files", createFileManager())),
                createLauncher("🎬", "Media", () -> spawnWindow("hyMedia Player", createMediaPlayer())),
                createLauncher("🎨", "Paint", () -> spawnWindow("hyPaint", createPaintApp())),
                createLauncher("🛒", "Store", () -> spawnWindow("App Store", createAppStore())),
                new Separator(Orientation.VERTICAL),
                systemClock);
    }

    // ── Terminal ─────────────────────────────────────────────────────────────────
    private Node createTerminal() {
        TextArea ta = new TextArea(
                osName + " Shell\nType 'help' for available commands.\n\n" + currentUser + "@hyos:~$ ");
        ta.setStyle(
                "-fx-control-inner-background: black; -fx-text-fill: #10b981; -fx-font-family: 'Consolas', monospace;");
        ta.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String prompt = isRoot ? "root@hyos:# " : currentUser + "@hyos:~$ ";
                String[] lines = ta.getText().split("\n");
                String input = lines[lines.length - 1].replace(prompt, "").trim().toLowerCase();
                ta.appendText("\n");

                if (input.equals("help")) {
                    ta.appendText("Commands: help, sudo su\nLocked: ls, neofetch, clear, open [app], sudo rm -rf /");
                } else if (input.equals("sudo su")) {
                    isRoot = true;
                    ta.setStyle("-fx-control-inner-background: #1a0000; -fx-text-fill: #ef4444;");
                    ta.appendText("SUPERUSER PRIVILEGES ENABLED.");
                } else if (!isRoot) {
                    ta.appendText("Access Denied: You must be root. Type 'sudo su'.");
                } else {
                    if (input.equals("neofetch")) {
                        ta.appendText("      hyOS\n    _______     OS: " + osName
                                + "\n   /  ___  \\    Kernel: 10.2.0-hy-stable\n  |  /   \\  |   Shell: hyBash\n   \\_______/    Resolution: 1280x800\n");
                    } else if (input.equals("sudo rm -rf /")) {
                        virtualDisk.clear();
                        ta.setStyle("-fx-control-inner-background: #450a0a; -fx-text-fill: white;");
                        ta.appendText("ERASING ROOT... SYSTEM FAILURE IMMINENT.");
                        Platform.runLater(() -> spawnWindow("RECOVERY",
                                createWebBrowser("https://www.youtube.com/embed/dQw4w9WgXcQ?autoplay=1")));
                    } else if (input.startsWith("open ")) {
                        handleShellLaunch(input.substring(5));
                    } else if (input.equals("ls")) {
                        ta.appendText(virtualDisk.isEmpty() ? "fs error: empty" : String.join("  ", virtualDisk));
                    } else if (input.equals("clear")) {
                        ta.setText(isRoot ? "root@hyos:# " : currentUser + "@hyos:~$ ");
                        e.consume();
                        return;
                    } else {
                        ta.appendText("sh: unknown command: " + input);
                    }
                }
                ta.appendText("\n" + (isRoot ? "root@hyos:# " : currentUser + "@hyos:~$ "));
                ta.positionCaret(ta.getText().length());
                e.consume();
            }
        });
        return ta;
    }

    private void handleShellLaunch(String app) {
        if (app.contains("snake"))
            spawnSnake();
        else if (app.contains("media"))
            spawnWindow("hyMedia", createMediaPlayer());
        else if (app.contains("browser"))
            spawnWindow("Browser", createWebBrowser("https://www.google.com"));
        else if (app.contains("paint"))
            spawnWindow("hyPaint", createPaintApp());
        else if (app.contains("stat"))
            spawnWindow("hyStat", createStatsApp());
        else if (app.contains("theme"))
            spawnWindow("Theme Picker", createThemeApp());
    }

    // ── Window chrome ─────────────────────────────────────────────────────────
    class HyWindow extends VBox {
        private double x = 0, y = 0;
        private double oldX, oldY, oldW, oldH;
        private boolean isMaximized = false;

        public HyWindow(String title, Node content) {
            this.setPrefSize(800, 600);
            this.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-background-radius: 12;");
            this.setEffect(new DropShadow(20, Color.BLACK));

            HBox titleBar = new HBox(10);
            titleBar.setStyle(
                    "-fx-background-color: #0f172a; -fx-padding: 10; -fx-cursor: move; -fx-background-radius: 12 12 0 0;");
            Label l = new Label(title);
            l.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button maxBtn = new Button("▢");
            maxBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-font-size: 10;");
            maxBtn.setOnAction(e -> toggleMaximize());

            Button closeBtn = new Button("✕");
            closeBtn.setStyle(
                    "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 50; -fx-font-size: 10;");
            closeBtn.setOnAction(e -> desktop.getChildren().remove(this));

            titleBar.getChildren().addAll(l, spacer, maxBtn, closeBtn);
            titleBar.setOnMousePressed(e -> {
                x = e.getX();
                y = e.getY();
                this.toFront();
            });
            titleBar.setOnMouseDragged(e -> {
                if (!isMaximized) {
                    this.setLayoutX(e.getScreenX() - x - getScene().getWindow().getX());
                    this.setLayoutY(e.getScreenY() - y - getScene().getWindow().getY());
                }
            });

            VBox.setVgrow(content, Priority.ALWAYS);
            Rectangle resizeHandle = new Rectangle(15, 15, Color.web("#475569"));
            resizeHandle.setCursor(javafx.scene.Cursor.SE_RESIZE);
            resizeHandle.setOnMouseDragged(e -> {
                if (!isMaximized)
                    this.setPrefSize(e.getSceneX() - this.getLayoutX(), e.getSceneY() - this.getLayoutY());
            });
            HBox footer = new HBox(resizeHandle);
            footer.setAlignment(Pos.BOTTOM_RIGHT);

            this.getChildren().addAll(titleBar, content, footer);
            this.setLayoutX(100 + (new Random().nextInt(50)));
            this.setLayoutY(50 + (new Random().nextInt(50)));
            this.setOnMousePressed(e -> this.toFront());
        }

        private void toggleMaximize() {
            if (!isMaximized) {
                oldX = getLayoutX();
                oldY = getLayoutY();
                oldW = getWidth();
                oldH = getHeight();
                setLayoutX(0);
                setLayoutY(0);
                setPrefSize(desktop.getWidth(), desktop.getHeight());
                isMaximized = true;
            } else {
                setLayoutX(oldX);
                setLayoutY(oldY);
                setPrefSize(oldW, oldH);
                isMaximized = false;
            }
        }
    }

    // ── Snake ────────────────────────────────────────────────────────────────────
    // High-score list persisted for the session
    private final List<Integer> snakeHighScores = new ArrayList<>();

    private Node createSnakeGame() {
        final int CELL = 25;
        // We'll size the canvas dynamically once added to scene; use placeholders
        // that get replaced after layout. For now pick a safe default.
        final int COLS = 40, ROWS = 26; // cells across / down
        final int W = COLS * CELL, H = ROWS * CELL;
        final int TOTAL_CELLS = COLS * ROWS;

        Canvas canvas = new Canvas(W, H);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // ── Mutable game state (arrays so lambdas can close over them) ──
        final int[] speed = { 7 };
        final String[] dir = { "RIGHT" };
        final String[] nextDir = { "RIGHT" };
        final boolean[] dirChangedThisTick = { false };
        final List<int[]> snake = new ArrayList<>();
        final int[] food = { 10, 10 };
        final int[] score = { 0 };
        // 0 = playing, 1 = game over, 2 = win
        final int[] state = { 0 };
        final AnimationTimer[] timerRef = { null };

        // ── StackPane: canvas + overlay ──────────────────────────────────
        StackPane gamePane = new StackPane(canvas);
        gamePane.setStyle("-fx-background-color: black;");

        // Score label (top-left HUD)
        Label scoreLabel = new Label("Score: 0");
        scoreLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-family: monospace; "
                + "-fx-font-size: 16; -fx-font-weight: bold; -fx-padding: 8 14;");
        StackPane.setAlignment(scoreLabel, Pos.TOP_LEFT);
        gamePane.getChildren().add(scoreLabel);

        // Overlay (game-over / win screen)
        VBox overlay = new VBox(18);
        overlay.setAlignment(Pos.CENTER);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.78);");
        overlay.setVisible(false);
        gamePane.getChildren().add(overlay);

        // ── Helper: place food on empty cell ───────────────────────────
        Runnable placeFood = () -> {
            Random rng = new Random();
            int fx, fy;
            outer: do {
                fx = rng.nextInt(COLS);
                fy = rng.nextInt(ROWS);
                for (int[] p : snake)
                    if (p[0] == fx && p[1] == fy)
                        continue outer;
                break;
            } while (true);
            food[0] = fx;
            food[1] = fy;
        };

        // ── Helper: full reset ──────────────────────────────────────────
        Runnable[] resetRef = { null };
        resetRef[0] = () -> {
            snake.clear();
            snake.add(new int[] { COLS / 2, ROWS / 2 });
            score[0] = 0;
            speed[0] = 7;
            dir[0] = "RIGHT";
            nextDir[0] = "RIGHT";
            dirChangedThisTick[0] = false;
            state[0] = 0;
            placeFood.run();
            scoreLabel.setText("Score: 0");
            overlay.setVisible(false);
            canvas.requestFocus();
        };
        resetRef[0].run(); // initialise

        // ── Helper: show end-screen overlay ───────────────────────────
        Runnable showOverlay = () -> {
            // Record score
            snakeHighScores.add(score[0]);
            snakeHighScores.sort((a, b) -> b - a);

            overlay.getChildren().clear();

            boolean won = state[0] == 2;
            Label title = new Label(won ? "🎉 YOU WIN!" : "💀 GAME OVER");
            title.setStyle("-fx-text-fill: " + (won ? "#facc15" : "#ef4444")
                    + "; -fx-font-size: 36; -fx-font-weight: bold; -fx-font-family: monospace;");

            Label finalScore = new Label("Score: " + score[0]);
            finalScore.setStyle("-fx-text-fill: white; -fx-font-size: 20; -fx-font-family: monospace;");

            // High-score list (top 5)
            VBox hsBox = new VBox(4);
            hsBox.setAlignment(Pos.CENTER);
            Label hsTitle = new Label("── High Scores ──");
            hsTitle.setStyle("-fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-font-size: 14;");
            hsBox.getChildren().add(hsTitle);
            int limit = Math.min(5, snakeHighScores.size());
            for (int i = 0; i < limit; i++) {
                Label row = new Label((i + 1) + ".  " + snakeHighScores.get(i));
                row.setStyle("-fx-text-fill: #94a3b8; -fx-font-family: monospace; -fx-font-size: 13;");
                hsBox.getChildren().add(row);
            }

            Button restart = new Button("▶  Play Again");
            restart.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; "
                    + "-fx-font-size: 16; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 28;");
            restart.setOnAction(ev -> resetRef[0].run());

            overlay.getChildren().addAll(title, finalScore, hsBox, restart);
            overlay.setVisible(true);
        };

        // ── Key handler ─────────────────────────────────────────────────
        // Direction is applied immediately so there is zero perceptible latency.
        // dirChangedThisTick blocks a second turn within the same game tick so
        // the snake can never reverse into itself via rapid double-tap.
        canvas.setFocusTraversable(true);
        gamePane.setOnMouseClicked(e -> canvas.requestFocus());
        canvas.setOnKeyPressed(e -> {
            if (state[0] != 0)
                return;
            KeyCode k = e.getCode();
            String cur = dir[0];
            String want = null;
            if (k == KeyCode.W && !cur.equals("DOWN"))
                want = "UP";
            if (k == KeyCode.S && !cur.equals("UP"))
                want = "DOWN";
            if (k == KeyCode.A && !cur.equals("RIGHT"))
                want = "LEFT";
            if (k == KeyCode.D && !cur.equals("LEFT"))
                want = "RIGHT";
            if (want != null && !dirChangedThisTick[0]) {
                nextDir[0] = want;
                dirChangedThisTick[0] = true; // only one turn per tick
            }
        });

        // ── Animation loop ──────────────────────────────────────────────
        AnimationTimer timer = new AnimationTimer() {
            long last = 0;

            public void handle(long now) {
                if (state[0] != 0)
                    return;
                if (now - last < 1_000_000_000L / speed[0])
                    return;
                last = now;

                // Commit queued direction and reset per-tick turn guard
                dir[0] = nextDir[0];
                dirChangedThisTick[0] = false;

                int[] head = snake.get(0);
                int nX = head[0], nY = head[1];
                switch (dir[0]) {
                    case "UP":
                        nY--;
                        break;
                    case "DOWN":
                        nY++;
                        break;
                    case "LEFT":
                        nX--;
                        break;
                    case "RIGHT":
                        nX++;
                        break;
                }

                // Wrap around (toroidal map)
                nX = (nX + COLS) % COLS;
                nY = (nY + ROWS) % ROWS;

                // Self-collision
                for (int[] p : snake) {
                    if (p[0] == nX && p[1] == nY) {
                        state[0] = 1;
                        break;
                    }
                }

                if (state[0] == 0) {
                    snake.add(0, new int[] { nX, nY });
                    if (nX == food[0] && nY == food[1]) {
                        score[0]++;
                        scoreLabel.setText("Score: " + score[0]);
                        if (snake.size() == TOTAL_CELLS) {
                            state[0] = 2; // WIN
                        } else {
                            speed[0] = 7 + score[0] / 3;
                            placeFood.run();
                        }
                    } else {
                        snake.remove(snake.size() - 1);
                    }
                }

                // ── Draw ────────────────────────────────────────────────
                // Grid background
                gc.setFill(Color.web("#0a0a0a"));
                gc.fillRect(0, 0, W, H);
                gc.setStroke(Color.web("#1a1a1a"));
                gc.setLineWidth(0.5);
                for (int col = 0; col < COLS; col++)
                    gc.strokeLine(col * CELL, 0, col * CELL, H);
                for (int row = 0; row < ROWS; row++)
                    gc.strokeLine(0, row * CELL, W, row * CELL);

                // Food (pulsing red circle)
                gc.setFill(Color.web("#ef4444"));
                double pulse = 3 + 2 * Math.sin(now / 200_000_000.0);
                gc.fillOval(food[0] * CELL + pulse / 2, food[1] * CELL + pulse / 2,
                        CELL - pulse, CELL - pulse);

                // Snake body (gradient green → teal head)
                for (int i = snake.size() - 1; i >= 0; i--) {
                    int[] p = snake.get(i);
                    double t = 1.0 - (double) i / snake.size();
                    // interpolate lime → teal
                    Color c = Color.color(0, 0.55 + 0.45 * t, 0.3 + 0.5 * t);
                    gc.setFill(c);
                    int margin = (i == 0) ? 1 : 2;
                    gc.fillRoundRect(p[0] * CELL + margin, p[1] * CELL + margin,
                            CELL - margin * 2, CELL - margin * 2, 6, 6);
                }

                // Show overlay if game ended this tick
                if (state[0] != 0)
                    showOverlay.run();
            }
        };
        timerRef[0] = timer;
        timer.start();

        // Request focus once the scene is attached
        gamePane.sceneProperty().addListener((obs, o, n) -> {
            if (n != null)
                Platform.runLater(canvas::requestFocus);
        });

        return gamePane;
    }

    // ── App Store ────────────────────────────────────────────────────────────────
    private Node createAppStore() {
        VBox s = new VBox(15);
        s.setPadding(new Insets(20));
        s.setStyle("-fx-background-color: #0f172a;");
        Button b1 = new Button("Install 🕹️ Snake");
        b1.setOnAction(e -> {
            dock.getChildren().add(1, createLauncher("🕹️", "Snake", () -> spawnSnake()));
            b1.setText("✔ Installed");
            b1.setDisable(true);
        });
        Button b2 = new Button("Install 📝 Notepad");
        b2.setOnAction(e -> {
            dock.getChildren().add(1, createLauncher("📝", "Notepad", () -> spawnWindow("Notepad", createNotepad())));
            b2.setDisable(true);
        });
        Button b3 = new Button("Install 📊 hyStat");
        b3.setOnAction(e -> {
            dock.getChildren().add(1,
                    createLauncher("📊", "hyStat", () -> spawnWindow("System Monitor", createStatsApp())));
            b3.setText("✔ Installed");
            b3.setDisable(true);
        });
        Button b4 = new Button("Install 🎨 Themes");
        b4.setOnAction(e -> {
            dock.getChildren().add(1,
                    createLauncher("🎨", "Themes", () -> spawnWindow("Theme Picker", createThemeApp())));
            b4.setText("✔ Installed");
            b4.setDisable(true);
        });
        s.getChildren().addAll(new Label("Software Store"), b1, b2, b3, b4);
        return s;
    }

    // ── Stats
    // ─────────────────────────────────────────────────────────────────────
    private Node createStatsApp() {
        VBox stats = new VBox(10);
        stats.setPadding(new Insets(20));
        stats.getChildren().addAll(new Label("CPU Usage: 4%"), new Label("RAM: 850MB / 16GB"));
        return stats;
    }

    // ── Media
    // ─────────────────────────────────────────────────────────────────────
    private Node createMediaPlayer() {
        VBox p = new VBox(5);
        p.setStyle("-fx-background-color: #020617;");
        WebView w = new WebView();
        w.getEngine().load("https://www.youtube.com/embed/jfKfPfyJRdk");
        VBox.setVgrow(w, Priority.ALWAYS);
        p.getChildren().add(w);
        return p;
    }

    // ── Paint (upgraded)
    // ──────────────────────────────────────────────────────────
    private Node createPaintApp() {
        Canvas canvas = new Canvas(800, 550);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // White background
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // State
        final Color[] brushColor = { Color.BLACK };
        final double[] brushSize = { 5 };

        // ── Color palette ────────────────────────────────────────────────────
        String[] colorHex = {
                "#000000", "#ffffff", "#ef4444", "#f97316",
                "#eab308", "#22c55e", "#3b82f6", "#8b5cf6",
                "#ec4899", "#06b6d4", "#a16207", "#6b7280"
        };

        HBox palette = new HBox(6);
        palette.setPadding(new Insets(6, 10, 6, 10));
        palette.setAlignment(Pos.CENTER_LEFT);
        palette.setStyle("-fx-background-color: #e2e8f0;");

        // Selected color indicator
        Rectangle selected = new Rectangle(28, 28);
        selected.setFill(brushColor[0]);
        selected.setStroke(Color.web("#334155"));
        selected.setStrokeWidth(2);
        selected.setArcWidth(4);
        selected.setArcHeight(4);
        palette.getChildren().add(selected);

        Separator sep1 = new Separator(Orientation.VERTICAL);
        palette.getChildren().add(sep1);

        for (String hex : colorHex) {
            Circle dot = new Circle(12);
            dot.setFill(Color.web(hex));
            dot.setStroke(Color.web("#94a3b8"));
            dot.setStrokeWidth(1.5);
            dot.setCursor(javafx.scene.Cursor.HAND);
            dot.setOnMouseClicked(e -> {
                brushColor[0] = Color.web(hex);
                selected.setFill(brushColor[0]);
            });
            dot.hoverProperty().addListener((obs, oldV, newV) -> dot.setStrokeWidth(newV ? 3 : 1.5));
            palette.getChildren().add(dot);
        }

        Separator sep2 = new Separator(Orientation.VERTICAL);
        palette.getChildren().add(sep2);

        // Brush size slider
        Label sizeLabel = new Label("Size:");
        sizeLabel.setStyle("-fx-font-size: 11;");
        Slider sizeSlider = new Slider(1, 40, 5);
        sizeSlider.setPrefWidth(100);
        sizeSlider.valueProperty().addListener((obs, o, n) -> brushSize[0] = n.doubleValue());
        palette.getChildren().addAll(sizeLabel, sizeSlider);

        // Eraser button
        Button eraser = new Button("⬜ Eraser");
        eraser.setOnAction(e -> {
            brushColor[0] = Color.WHITE;
            selected.setFill(Color.WHITE);
        });
        palette.getChildren().add(eraser);

        // Clear button
        Button clear = new Button("🗑 Clear");
        clear.setOnAction(e -> {
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        });
        palette.getChildren().add(clear);

        // ── Drawing ──────────────────────────────────────────────────────────
        canvas.setOnMouseDragged(e -> {
            double r = brushSize[0] / 2.0;
            gc.setFill(brushColor[0]);
            gc.fillOval(e.getX() - r, e.getY() - r, brushSize[0], brushSize[0]);
        });
        canvas.setOnMousePressed(e -> {
            double r = brushSize[0] / 2.0;
            gc.setFill(brushColor[0]);
            gc.fillOval(e.getX() - r, e.getY() - r, brushSize[0], brushSize[0]);
        });

        ScrollPane scroll = new ScrollPane(canvas);
        scroll.setFitToWidth(false);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox root = new VBox(palette, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return root;
    }

    // ── Notepad with Save ────────────────────────────────────────────────────────
    private Node createNotepad() {
        return createNotepadFor(null, "");
    }

    private Node createNotepadFor(String existingFilename, String initialContent) {
        TextArea ta = new TextArea(initialContent);
        ta.setPromptText("Start typing...");
        ta.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 13;");

        // Track the current filename so re-saves update the same entry
        final String[] currentFilename = { existingFilename };

        Button saveBtn = new Button("💾 Save");
        Label status = new Label("");
        status.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 11;");

        saveBtn.setOnAction(e -> {
            if (currentFilename[0] == null) {
                // New file — generate a name
                currentFilename[0] = "note_" + System.currentTimeMillis() + ".txt";
                virtualDisk.add(currentFilename[0]);
            }
            // Persist the content
            virtualDiskContents.put(currentFilename[0], ta.getText());
            status.setText("Saved as " + currentFilename[0]);
            Thread t = new Thread(() -> {
                try {
                    Thread.sleep(3000);
                } catch (Exception ignored) {
                }
                Platform.runLater(() -> status.setText(""));
            });
            t.setDaemon(true);
            t.start();
        });

        HBox toolbar = new HBox(10, saveBtn, status);
        toolbar.setPadding(new Insets(6, 10, 6, 10));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #f1f5f9; -fx-border-color: #cbd5e1; -fx-border-width: 0 0 1 0;");

        VBox box = new VBox(toolbar, ta);
        VBox.setVgrow(ta, Priority.ALWAYS);
        return box;
    }

    // ── Theme Picker (NEW) ───────────────────────────────────────────────────────
    private Node createThemeApp() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(30));
        box.setAlignment(Pos.TOP_LEFT);
        box.setStyle("-fx-background-color: " + currentTheme.bg + ";");

        Label title = new Label("🎨  Choose a Theme");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: " + currentTheme.accent + ";");

        ToggleGroup group = new ToggleGroup();
        VBox buttons = new VBox(12);

        for (Theme t : Theme.values()) {
            RadioButton rb = new RadioButton(t.label);
            rb.setToggleGroup(group);
            rb.setSelected(t == currentTheme);
            rb.setUserData(t);

            // Mini preview swatch
            HBox swatch = new HBox(4);
            for (String col : new String[] { t.bg, t.surface, t.border, t.accent, t.text }) {
                Rectangle r = new Rectangle(18, 18);
                r.setFill(Color.web(col));
                r.setArcWidth(4);
                r.setArcHeight(4);
                swatch.getChildren().add(r);
            }

            HBox row = new HBox(15, rb, swatch);
            row.setAlignment(Pos.CENTER_LEFT);
            buttons.getChildren().add(row);
        }

        Button applyBtn = new Button("Apply Theme");
        applyBtn.setStyle("-fx-background-color: " + currentTheme.accent
                + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 20;");
        applyBtn.setOnAction(e -> {
            Toggle sel = group.getSelectedToggle();
            if (sel != null) {
                Theme chosen = (Theme) sel.getUserData();
                applyTheme(chosen);
                // Refresh the box style to match new theme
                box.setStyle("-fx-background-color: " + chosen.bg + ";");
                title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: " + chosen.accent + ";");
                applyBtn.setStyle("-fx-background-color: " + chosen.accent
                        + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 20;");
            }
        });

        box.getChildren().addAll(title, buttons, applyBtn);
        return box;
    }

    // ── File Manager ─────────────────────────────────────────────────────────────
    private Node createFileManager() {
        VBox container = new VBox();
        container.setStyle("-fx-background-color: #0f172a;");

        // Toolbar
        Label hint = new Label("Double-click a file to open it");
        hint.setStyle("-fx-text-fill: #475569; -fx-font-size: 11; -fx-padding: 6 10;");

        ListView<String> lv = new ListView<>();
        lv.getItems().addAll(virtualDisk);
        lv.setStyle("-fx-control-inner-background: #1e293b; -fx-text-fill: #38bdf8;");
        VBox.setVgrow(lv, Priority.ALWAYS);

        // Status bar at bottom
        Label status = new Label("");
        status.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11; -fx-padding: 4 10;");

        lv.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String file = lv.getSelectionModel().getSelectedItem();
                if (file == null)
                    return;
                openFile(file);
                status.setText("Opened: " + file);
            } else if (e.getClickCount() == 1) {
                String file = lv.getSelectionModel().getSelectedItem();
                if (file != null)
                    status.setText(file + "  —  " + getFileMeta(file));
            }
        });

        container.getChildren().addAll(hint, lv, status);
        return container;
    }

    /** Returns a fake metadata string for display in the status bar */
    private String getFileMeta(String filename) {
        if (filename.endsWith(".sys"))
            return "System file  •  Read-only";
        if (filename.endsWith(".cfg"))
            return "Configuration  •  Text";
        if (filename.endsWith(".tmp"))
            return "Temporary file  •  Cache";
        if (filename.endsWith(".txt"))
            return "Plain text  •  Editable";
        return "File";
    }

    /** Decides how to open a file based on its extension */
    private void openFile(String filename) {
        if (filename.endsWith(".txt")) {
            // Load saved content (empty string if never saved via notepad)
            String content = virtualDiskContents.getOrDefault(filename, "");
            spawnWindow("📝 " + filename, createNotepadFor(filename, content));

        } else if (filename.endsWith(".cfg")) {
            // Open as read-only terminal-style viewer
            TextArea ta = new TextArea("# " + filename
                    + "\n\n[system]\nversion=10.2\nuser=admin\ntheme=dark\n\n[network]\ndns=8.8.8.8\nproxy=none");
            ta.setEditable(false);
            ta.setStyle(
                    "-fx-control-inner-background: #0a0a0a; -fx-text-fill: #22c55e; -fx-font-family: 'Consolas', monospace;");
            spawnWindow("⚙️ " + filename, ta);

        } else if (filename.endsWith(".sys")) {
            // Kernel / system files — show a warning viewer
            TextArea ta = new TextArea("⚠️  SYSTEM FILE — DO NOT MODIFY\n\n" + filename
                    + "\nKernel module: hyOS Core 10.2\nStatus: LOADED\nChecksum: 0xABCDEF01");
            ta.setEditable(false);
            ta.setStyle(
                    "-fx-control-inner-background: #1a0000; -fx-text-fill: #fca5a5; -fx-font-family: 'Consolas', monospace;");
            spawnWindow("🔒 " + filename, ta);

        } else if (filename.endsWith(".tmp")) {
            // Cache files — hex-dump style
            TextArea ta = new TextArea(
                    "BINARY CACHE FILE\n\n00000000  4D 65 64 69 61 20 43 61  63 68 65 20 76 31 2E 30\n00000010  2E 30 00 00 00 00 00 00  00 00 00 00 00 00 00 00\n\n[EOF]");
            ta.setEditable(false);
            ta.setStyle(
                    "-fx-control-inner-background: #0a0f1a; -fx-text-fill: #38bdf8; -fx-font-family: 'Consolas', monospace;");
            spawnWindow("🗃 " + filename, ta);

        } else {
            // Generic: show an info dialog
            TextArea ta = new TextArea("Cannot preview this file type.\n\nFilename: " + filename);
            ta.setEditable(false);
            ta.setStyle(
                    "-fx-control-inner-background: #1e293b; -fx-text-fill: #94a3b8; -fx-font-family: 'Consolas', monospace;");
            spawnWindow("❓ " + filename, ta);
        }
    }

    // ── Login screen
    // ──────────────────────────────────────────────────────────────
    private void createLoginScreen() {
        loginScreen = new VBox(20);
        loginScreen.setAlignment(Pos.CENTER);
        loginScreen.setStyle("-fx-background-color: #020617;");
        PasswordField pf = new PasswordField();
        pf.setMaxWidth(200);
        pf.setPromptText("Password (admin)");
        Button b = new Button("LOG IN");
        b.setOnAction(e -> {
            if (pf.getText().equals("admin")) {
                loginScreen.setVisible(false);
                desktop.setVisible(true);
                dock.setVisible(true);
            }
        });
        loginScreen.getChildren().addAll(new Label("hyOS APEX"), pf, b);
    }

    // ── Clock
    // ─────────────────────────────────────────────────────────────────────
    private void startClockUpdate() {
        Thread t = new Thread(() -> {
            while (true) {
                String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                Platform.runLater(() -> systemClock.setText(time));
                try {
                    Thread.sleep(30000);
                } catch (Exception ignored) {
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Browser
    // ───────────────────────────────────────────────────────────────────
    private Node createWebBrowser(String url) {
        VBox v = new VBox();
        WebView w = new WebView();
        w.getEngine().load(url);
        VBox.setVgrow(w, Priority.ALWAYS);
        v.getChildren().add(w);
        return v;
    }

    // ── Helpers
    // ───────────────────────────────────────────────────────────────────
    private Button createLauncher(String icon, String name, Runnable action) {
        Button b = new Button(icon);
        b.setTooltip(new Tooltip(name));
        b.setStyle("-fx-font-size: 24; -fx-background-color: transparent; -fx-cursor: hand;");
        b.setOnAction(e -> action.run());
        return b;
    }

    private void spawnSnake() {
        HyWindow win = new HyWindow("Snake", createSnakeGame());
        desktop.getChildren().add(win);
        win.toFront();
        // Maximize after layout pass so desktop dimensions are known
        Platform.runLater(() -> {
            win.setLayoutX(0);
            win.setLayoutY(0);
            win.setPrefSize(desktop.getWidth(), desktop.getHeight());
        });
    }

    private void spawnWindow(String title, Node content) {
        HyWindow win = new HyWindow(title, content);
        desktop.getChildren().add(win);
        win.toFront();
    }

    public static void main(String[] args) {
        launch(args);
    }
}