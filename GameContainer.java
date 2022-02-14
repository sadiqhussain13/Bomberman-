import gameobjects.Bomber;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Displays various game information on the screen such as each player's score.
 */
public class GameHUD {

    private Bomber[] players;
    private BufferedImage[] playerInfo;
    private int[] playerScore;
    boolean matchSet;

    GameHUD() {
        this.players = new Bomber[4];
        this.playerInfo = new BufferedImage[4];
        this.playerScore = new int[4];
        this.matchSet = false;
    }

    void init() {
        // Height of the HUD
        int height = GameWindow.HUD_HEIGHT;
        // Width of each player's information in the HUD, 4 players, 4 info boxes
        int infoWidth = GamePanel.panelWidth / 4;

        this.playerInfo[0] = new BufferedImage(infoWidth, height, BufferedImage.TYPE_INT_RGB);
        this.playerInfo[1] = new BufferedImage(infoWidth, height, BufferedImage.TYPE_INT_RGB);
        this.playerInfo[2] = new BufferedImage(infoWidth, height, BufferedImage.TYPE_INT_RGB);
        this.playerInfo[3] = new BufferedImage(infoWidth, height, BufferedImage.TYPE_INT_RGB);
    }

    /**
     * Used by game panel to draw player info to the screen
     * @return Player info box
     */
    BufferedImage getP1info() {
        return this.playerInfo[0];
    }
    BufferedImage getP2info() {
        return this.playerInfo[1];
    }
    BufferedImage getP3info() {
        return this.playerInfo[2];
    }
    BufferedImage getP4info() {
        return this.playerInfo[3];
    }

    /**
     * Assign an info box to a player that shows the information on this player.
     * @param player The player to be assigned
     * @param playerID Used as an index for the array
     */
    void assignPlayer(Bomber player, int playerID) {
        this.players[playerID] = player;
    }

    /**
     * Checks if there is only one player alive left and increases their score.
     * The match set boolean is used to check if a point is already added so that the winner can freely
     * move around for a while before resetting the map. This also allows the winner to kill themselves without
     * affecting their score since the score was already updated.
     */
    public void updateScore() {
        // Count dead players
        int deadPlayers = 0;
        for (int i = 0; i < this.players.length; i++) {
            if (this.players[i].isDead()) {
                deadPlayers++;
            }
        }

        // Check for the last player standing and conclude the match
        if (deadPlayers == this.players.length - 1) {
            for (int i = 0; i < this.players.length; i++) {
                if (!this.players[i].isDead()) {
                    this.playerScore[i]++;
                    this.matchSet = true;
                }
            }
        } else if (deadPlayers >= this.players.length) {
            // This should only be reached two or more of the last players die at the same time
            this.matchSet = true;
        }
    }

    /**
     * Continuously redraw player information such as score.
     */
    void drawHUD() {
        Graphics[] playerGraphics = {
                this.playerInfo[0].createGraphics(),
                this.playerInfo[1].createGraphics(),
                this.playerInfo[2].createGraphics(),
                this.playerInfo[3].createGraphics()};

        // Clean info boxes
        playerGraphics[0].clearRect(0, 0, playerInfo[0].getWidth(), playerInfo[0].getHeight());
        playerGraphics[1].clearRect(0, 0, playerInfo[1].getWidth(), playerInfo[1].getHeight());
        playerGraphics[2].clearRect(0, 0, playerInfo[1].getWidth(), playerInfo[1].getHeight());
        playerGraphics[3].clearRect(0, 0, playerInfo[1].getWidth(), playerInfo[1].getHeight());

        // Set border color per player
        playerGraphics[0].setColor(Color.WHITE);    // Player 1 info box border color
        playerGraphics[1].setColor(Color.GRAY);     // Player 2 info box border color
        playerGraphics[2].setColor(Color.RED);      // Player 3 info box border color
        playerGraphics[3].setColor(Color.BLUE);     // Player 4 info box border color

        // Iterate loop for each player
        for (int i = 0; i < playerGraphics.length; i++) {
            Font font = new Font("Courier New", Font.BOLD, 24);
            // Draw border and sprite
            playerGraphics[i].drawRect(1, 1, this.playerInfo[i].getWidth() - 2, this.playerInfo[i].getHeight() - 2);
            playerGraphics[i].drawImage(this.players[i].getBaseSprite(), 0, 0, null);

            // Draw score
            playerGraphics[i].setFont(font);
            playerGraphics[i].setColor(Color.WHITE);
            playerGraphics[i].drawString("" + this.playerScore[i], this.playerInfo[i].getWidth() / 2, 32);

            // Dispose
            playerGraphics[i].dispose();
        }
    }

}
=============================================================================================================
import util.ResourceCollection;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Contains the main method to launch the game.
 */
public class GameLauncher {

    // The one and only window for the game to run
    static GameWindow window;

    public static void main(String[] args) {
        ResourceCollection.readFiles();
        ResourceCollection.init();

        GamePanel game;
        try {
            game = new GamePanel(args[0]);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println(e + ": Program args not given");
            game = new GamePanel(null);
        }

        game.init();
        window = new GameWindow(game);

        System.gc();
    }

}

/**
 * Game window that contains the game panel seen by the user.
 */
class GameWindow extends JFrame {

    /**
     * Screen width and height is determined by the map size. Map size is set when loading the map in
     * the GamePanel class. For best results, do not use a map that is smaller than the default map
     * provided in resources.
     */

    static final int HUD_HEIGHT = 48;   // Size of the HUD. The HUD displays score.
    static final String TITLE = "Bomberman by Brian Lai";

    /**
     * Constructs a game window with the necessary configurations.
     * @param game Game panel that will be contained inside the game window
     */
    GameWindow(GamePanel game) {
        this.setTitle(TITLE);
        this.setIconImage(ResourceCollection.Images.ICON.getImage());
        this.setLayout(new BorderLayout());
        this.add(game, BorderLayout.CENTER);
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    /**
     * Called every second. Updates the FPS and Ticks counters and prints them to the console with the current time.
     * @param fps FPS counter
     * @param ticks Ticks counter
     */
    public void update(int fps, int ticks) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalDateTime time = LocalDateTime.now();
        System.out.println("[" + dtf.format(time) + "]" + " FPS: " + fps + ", Ticks: " + ticks);
        GameLauncher.window.setTitle(GameWindow.TITLE + " | " + "FPS: " + fps + ", Ticks: " + ticks);
    }

}
======================================================================================================
import gameobjects.*;
import util.GameObjectCollection;
import util.Key;
import util.ResourceCollection;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * JPanel that contains the entire game and game loop logic.
 */
public class GamePanel extends JPanel implements Runnable {

    // Screen size is determined by the map size
    static int panelWidth;
    static int panelHeight;

    private Thread thread;
    private boolean running;
    int resetDelay;

    private BufferedImage world;
    private Graphics2D buffer;
    private BufferedImage bg;
    private GameHUD gameHUD;

    private int mapWidth;
    private int mapHeight;
    private ArrayList<ArrayList<String>> mapLayout;
    private BufferedReader bufferedReader;

    private HashMap<Integer, Key> controls1;
    private HashMap<Integer, Key> controls2;
    private HashMap<Integer, Key> controls3;
    private HashMap<Integer, Key> controls4;

    private static final double SOFTWALL_RATE = 0.825;

    /**
     * Construct game panel and load in a map file.
     * @param filename Name of the map file
     */
    GamePanel(String filename) {
        this.setFocusable(true);
        this.requestFocus();
        this.setControls();
        this.bg = ResourceCollection.Images.BACKGROUND.getImage();
        this.loadMapFile(filename);
        this.addKeyListener(new GameController(this));
    }

    /**
     * Initialize the game panel with a HUD, window size, collection of game objects, and start the game loop.
     */
    void init() {
        this.resetDelay = 0;
        GameObjectCollection.init();
        this.gameHUD = new GameHUD();
        this.generateMap();
        this.gameHUD.init();
        this.setPreferredSize(new Dimension(this.mapWidth * 32, (this.mapHeight * 32) + GameWindow.HUD_HEIGHT));
        System.gc();
        this.running = true;
    }

    /**
     * Loads the map file into buffered reader or load default map when no file is given.
     * The file should be a file with strings separated by commas ",". Preferred .csv file.
     * @param mapFile Name of the map file
     */
    private void loadMapFile(String mapFile) {
        // Loading map file
        try {
            this.bufferedReader = new BufferedReader(new FileReader(mapFile));
        } catch (IOException | NullPointerException e) {
            // Load default map when map file could not be loaded
            System.err.println(e + ": Cannot load map file, loading default map");
            this.bufferedReader = new BufferedReader(ResourceCollection.Files.DEFAULT_MAP.getFile());
        }

        // Parsing map data from file
        this.mapLayout = new ArrayList<>();
        try {
            String currentLine;
            while ((currentLine = bufferedReader.readLine()) != null) {
                if (currentLine.isEmpty()) {
                    continue;
                }
                // Split row into array of strings and add to array list
                mapLayout.add(new ArrayList<>(Arrays.asList(currentLine.split(","))));
            }
        } catch (IOException | NullPointerException e) {
            System.out.println(e + ": Error parsing map data");
            e.printStackTrace();
        }
    }

    /**
     * Generate the map given the map file. The map is grid based and each tile is 32x32.
     * Create game objects depending on the string.
     */
    private void generateMap() {
        // Map dimensions
        this.mapWidth = mapLayout.get(0).size();
        this.mapHeight = mapLayout.size();
        panelWidth = this.mapWidth * 32;
        panelHeight = this.mapHeight * 32;

        this.world = new BufferedImage(this.mapWidth * 32, this.mapHeight * 32, BufferedImage.TYPE_INT_RGB);

        // Generate entire map
        for (int y = 0; y < this.mapHeight; y++) {
            for (int x = 0; x < this.mapWidth; x++) {
                switch (mapLayout.get(y).get(x)) {
                    case ("S"):     // Soft wall; breakable
                        if (Math.random() < SOFTWALL_RATE) {
                            BufferedImage sprSoftWall = ResourceCollection.Images.SOFT_WALL.getImage();
                            Wall softWall = new Wall(new Point2D.Float(x * 32, y * 32), sprSoftWall, true);
                            GameObjectCollection.spawn(softWall);
                        }
                        break;

                    case ("H"):     // Hard wall; unbreakable
                        // Code used to choose tile based on adjacent tiles
                        int code = 0;
                        if (y > 0 && mapLayout.get(y - 1).get(x).equals("H")) {
                            code += 1;  // North
                        }
                        if (y < this.mapHeight - 1 && mapLayout.get(y + 1).get(x).equals("H")) {
                            code += 4;  // South
                        }
                        if (x > 0 && mapLayout.get(y).get(x - 1).equals("H")) {
                            code += 8;  // West
                        }
                        if (x < this.mapWidth - 1 && mapLayout.get(y).get(x + 1).equals("H")) {
                            code += 2;  // East
                        }
                        BufferedImage sprHardWall = ResourceCollection.getHardWallTile(code);
                        Wall hardWall = new Wall(new Point2D.Float(x * 32, y * 32), sprHardWall, false);
                        GameObjectCollection.spawn(hardWall);
                        break;

                    case ("1"):     // Player 1; Bomber
                        BufferedImage[][] sprMapP1 = ResourceCollection.SpriteMaps.PLAYER_1.getSprites();
                        Bomber player1 = new Bomber(new Point2D.Float(x * 32, y * 32 - 16), sprMapP1);
                        PlayerController playerController1 = new PlayerController(player1, this.controls1);
                        this.addKeyListener(playerController1);
                        this.gameHUD.assignPlayer(player1, 0);
                        GameObjectCollection.spawn(player1);
                        break;

                    case ("2"):     // Player 2; Bomber
                        BufferedImage[][] sprMapP2 = ResourceCollection.SpriteMaps.PLAYER_2.getSprites();
                        Bomber player2 = new Bomber(new Point2D.Float(x * 32, y * 32 - 16), sprMapP2);
                        PlayerController playerController2 = new PlayerController(player2, this.controls2);
                        this.addKeyListener(playerController2);
                        this.gameHUD.assignPlayer(player2, 1);
                        GameObjectCollection.spawn(player2);
                        break;

                    case ("3"):     // Player 3; Bomber
                        BufferedImage[][] sprMapP3 = ResourceCollection.SpriteMaps.PLAYER_3.getSprites();
                        Bomber player3 = new Bomber(new Point2D.Float(x * 32, y * 32 - 16), sprMapP3);
                        PlayerController playerController3 = new PlayerController(player3, this.controls3);
                        this.addKeyListener(playerController3);
                        this.gameHUD.assignPlayer(player3, 2);
                        GameObjectCollection.spawn(player3);
                        break;

                    case ("4"):     // Player 4; Bomber
                        BufferedImage[][] sprMapP4 = ResourceCollection.SpriteMaps.PLAYER_4.getSprites();
                        Bomber player4 = new Bomber(new Point2D.Float(x * 32, y * 32 - 16), sprMapP4);
                        PlayerController playerController4 = new PlayerController(player4, this.controls4);
                        this.addKeyListener(playerController4);
                        this.gameHUD.assignPlayer(player4, 3);
                        GameObjectCollection.spawn(player4);
                        break;

                    case ("PB"):    // Powerup Bomb
                        Powerup powerBomb = new Powerup(new Point2D.Float(x * 32, y * 32), Powerup.Type.Bomb);
                        GameObjectCollection.spawn(powerBomb);
                        break;

                    case ("PU"):    // Powerup Fireup
                        Powerup powerFireup = new Powerup(new Point2D.Float(x * 32, y * 32), Powerup.Type.Fireup);
                        GameObjectCollection.spawn(powerFireup);
                        break;

                    case ("PM"):    // Powerup Firemax
                        Powerup powerFiremax = new Powerup(new Point2D.Float(x * 32, y * 32), Powerup.Type.Firemax);
                        GameObjectCollection.spawn(powerFiremax);
                        break;

                    case ("PS"):    // Powerup Speed
                        Powerup powerSpeed = new Powerup(new Point2D.Float(x * 32, y * 32), Powerup.Type.Speed);
                        GameObjectCollection.spawn(powerSpeed);
                        break;

                    case ("PP"):    // Powerup Pierce
                        Powerup powerPierce = new Powerup(new Point2D.Float(x * 32, y * 32), Powerup.Type.Pierce);
                        GameObjectCollection.spawn(powerPierce);
                        break;

                    case ("PK"):    // Powerup Kick
                        Powerup powerKick = new Powerup(new Point2D.Float(x * 32, y * 32), Powerup.Type.Kick);
                        GameObjectCollection.spawn(powerKick);
                        break;

                    case ("PT"):    // Powerup Timer
                        Powerup powerTimer = new Powerup(new Point2D.Float(x * 32, y * 32), Powerup.Type.Timer);
                        GameObjectCollection.spawn(powerTimer);
                        break;

                    default:
                        break;
                }
            }
        }
    }

    /**
     * Initialize default key bindings for all players.
     */
    private void setControls() {
        this.controls1 = new HashMap<>();
        this.controls2 = new HashMap<>();
        this.controls3 = new HashMap<>();
        this.controls4 = new HashMap<>();

        // Set Player 1 controls
        this.controls1.put(KeyEvent.VK_UP, Key.up);
        this.controls1.put(KeyEvent.VK_DOWN, Key.down);
        this.controls1.put(KeyEvent.VK_LEFT, Key.left);
        this.controls1.put(KeyEvent.VK_RIGHT, Key.right);
        this.controls1.put(KeyEvent.VK_SLASH, Key.action);

        // Set Player 2 controls
        this.controls2.put(KeyEvent.VK_W, Key.up);
        this.controls2.put(KeyEvent.VK_S, Key.down);
        this.controls2.put(KeyEvent.VK_A, Key.left);
        this.controls2.put(KeyEvent.VK_D, Key.right);
        this.controls2.put(KeyEvent.VK_E, Key.action);

        // Set Player 3 controls
        this.controls3.put(KeyEvent.VK_T, Key.up);
        this.controls3.put(KeyEvent.VK_G, Key.down);
        this.controls3.put(KeyEvent.VK_F, Key.left);
        this.controls3.put(KeyEvent.VK_H, Key.right);
        this.controls3.put(KeyEvent.VK_Y, Key.action);

        // Set Player 4 controls
        this.controls4.put(KeyEvent.VK_I, Key.up);
        this.controls4.put(KeyEvent.VK_K, Key.down);
        this.controls4.put(KeyEvent.VK_J, Key.left);
        this.controls4.put(KeyEvent.VK_L, Key.right);
        this.controls4.put(KeyEvent.VK_O, Key.action);
    }

    /**
     * When ESC is pressed, close the game
     */
    void exit() {
        this.running = false;
    }

    /**
     * When F5 is pressed, reset game object collection, collect garbage, reinitialize game panel, reload map
     */
    void resetGame() {
        this.init();
    }

    /**
     * Reset only the map, keeping the score
     */
    private void resetMap() {
        GameObjectCollection.init();
        this.generateMap();
        System.gc();
    }

    public void addNotify() {
        super.addNotify();

        if (this.thread == null) {
            this.thread = new Thread(this, "GameThread");
            this.thread.start();
        }
    }

    /**
     * The game loop.
     * The loop repeatedly calls update and repaints the panel.
     * Also reports the frames drawn per second and updates called per second (ticks).
     */
    @Override
    public void run() {
        long timer = System.currentTimeMillis();
        long lastTime = System.nanoTime();

        final double NS = 1000000000.0 / 60.0; // Locked ticks per second to 60
        double delta = 0;
        int fps = 0;    // Frames per second
        int ticks = 0;  // Ticks/Updates per second; should be 60 at all times

        // Count FPS, Ticks, and execute updates
        while (this.running) {
            long currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / NS;
            lastTime = currentTime;

            if (delta >= 1) {
                this.update();
                ticks++;
                delta--;
            }

            this.repaint();
            fps++;

            // Update FPS and Ticks counter every second
            if (System.currentTimeMillis() - timer > 1000) {
                timer = System.currentTimeMillis();
                GameLauncher.window.update(fps, ticks);
                fps = 0;
                ticks = 0;
            }
        }

        System.exit(0);
    }

    /**
     * The update method that loops through every game object and calls update.
     * Checks collisions between every two game objects.
     * Deletes game objects that are marked for deletion.
     * Checks if a player is a winner and updates score, then reset the map.
     */
    private void update() {
        GameObjectCollection.sortBomberObjects();
        // Loop through every game object arraylist
        for (int list = 0; list < GameObjectCollection.gameObjects.size(); list++) {
            for (int objIndex = 0; objIndex < GameObjectCollection.gameObjects.get(list).size(); ) {
                GameObject obj = GameObjectCollection.gameObjects.get(list).get(objIndex);
                obj.update();
                if (obj.isDestroyed()) {
                    // Destroy and remove game objects that were marked for deletion
                    obj.onDestroy();
                    GameObjectCollection.gameObjects.get(list).remove(obj);
                } else {
                    for (int list2 = 0; list2 < GameObjectCollection.gameObjects.size(); list2++) {
                        for (int objIndex2 = 0; objIndex2 < GameObjectCollection.gameObjects.get(list2).size(); objIndex2++) {
                            GameObject collidingObj = GameObjectCollection.gameObjects.get(list2).get(objIndex2);
                            // Skip detecting collision on the same object as itself
                            if (obj == collidingObj) {
                                continue;
                            }

                            // Visitor pattern collision handling
                            if (obj.getCollider().intersects(collidingObj.getCollider())) {
                                // Use one of these
                                collidingObj.onCollisionEnter(obj);
//                                obj.onCollisionEnter(collidingObj);
                            }
                        }
                    }
                    objIndex++;
                }
            }
        }

        // Check for the last bomber to survive longer than the others and increase score
        // Score is added immediately so there is no harm of dying when you are the last one
        // Reset map when there are 1 or less bombers left
        if (!this.gameHUD.matchSet) {
            this.gameHUD.updateScore();
        } else {
            // Checking size of array list because when a bomber dies, they do not immediately get deleted
            // This makes it so that the next round doesn't start until the winner is the only bomber object on the map
            if (GameObjectCollection.bomberObjects.size() <= 1) {
                this.resetMap();
                this.gameHUD.matchSet = false;
            }
        }

        // Used to prevent resetting the game really fast
        this.resetDelay++;

        try {
            Thread.sleep(1000 / 144);
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        this.buffer = this.world.createGraphics();
        this.buffer.clearRect(0, 0, this.world.getWidth(), this.world.getHeight());
        super.paintComponent(g2);

        this.gameHUD.drawHUD();

        // Draw background
        for (int i = 0; i < this.world.getWidth(); i += this.bg.getWidth()) {
            for (int j = 0; j < this.world.getHeight(); j += this.bg.getHeight()) {
                this.buffer.drawImage(this.bg, i, j, null);
            }
        }

        // Draw game objects
        for (int i = 0; i < GameObjectCollection.gameObjects.size(); i++) {
            for (int j = 0; j < GameObjectCollection.gameObjects.get(i).size(); j++) {
                GameObject obj = GameObjectCollection.gameObjects.get(i).get(j);
                obj.drawImage(this.buffer);
//                obj.drawCollider(this.buffer);
            }
        }

        // Draw HUD
        int infoBoxWidth = panelWidth / 4;
        g2.drawImage(this.gameHUD.getP1info(), infoBoxWidth * 0, 0, null);
        g2.drawImage(this.gameHUD.getP2info(), infoBoxWidth * 1, 0, null);
        g2.drawImage(this.gameHUD.getP3info(), infoBoxWidth * 2, 0, null);
        g2.drawImage(this.gameHUD.getP4info(), infoBoxWidth * 3, 0, null);

        // Draw game world offset by the HUD
        g2.drawImage(this.world, 0, GameWindow.HUD_HEIGHT, null);

        g2.dispose();
        this.buffer.dispose();
    }

}

/**
 * Used to control the game
 */
class GameController implements KeyListener {

    private GamePanel gamePanel;

    /**
     * Construct a universal game controller key listener for the game.
     * @param gamePanel Attach game controller to this game panel
     */
    GameController(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    /**
     * Key events for general game operations such as exit
     * @param e Keyboard key pressed
     */
    @Override
    public void keyPressed(KeyEvent e) {
        // Close game
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            System.out.println("Escape key pressed: Closing game");
            this.gamePanel.exit();
        }

        // Display controls
        if (e.getKeyCode() == KeyEvent.VK_F1) {
            System.out.println("F1 key pressed: Displaying help");

            String[] columnHeaders = { "", "White", "Black", "Red", "Blue" };
            Object[][] controls = {
                    {"Up", "Up", "W", "T", "I"},
                    {"Down", "Down", "S", "G", "K"},
                    {"Left", "Left", "A", "F", "J"},
                    {"Right", "Right", "D", "H", "L"},
                    {"Bomb", "/", "E", "Y", "O"},
                    {"", "", "", "", ""},
                    {"Help", "F1", "", "", ""},
                    {"Reset", "F5", "", "", ""},
                    {"Exit", "ESC", "", "", ""} };

            JTable controlsTable = new JTable(controls, columnHeaders);
            JTableHeader tableHeader = controlsTable.getTableHeader();

            // Wrap JTable inside JPanel to display
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            panel.add(tableHeader, BorderLayout.NORTH);
            panel.add(controlsTable, BorderLayout.CENTER);

            JOptionPane.showMessageDialog(this.gamePanel, panel, "Controls", JOptionPane.PLAIN_MESSAGE);
        }

        // Reset game
        // Delay prevents resetting too fast which causes the game to crash
        if (e.getKeyCode() == KeyEvent.VK_F5) {
            if (this.gamePanel.resetDelay >= 20) {
                System.out.println("F5 key pressed: Resetting game");
                this.gamePanel.resetGame();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

}
======================================================================================================================
import gameobjects.Player;
import util.Key;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;

/**
 * This class controls a player object through user input by listening for key events.
 */
public class PlayerController implements KeyListener {

    private Player player;
    private HashMap<Integer, Key> controls;

    /**
     * Assigns controls to a player game object.
     * @param obj The player game object to be controlled
     * @param controls The controls that will control the player game object
     */
    public PlayerController(Player obj, HashMap<Integer, Key> controls) {
        this.player = obj;
        this.controls = controls;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    /**
     * Reads the keys pressed and performs certain actions based on the key.
     * @param e The key pressed
     */
    @Override
    public void keyPressed(KeyEvent e) {
        if (this.controls.get(e.getKeyCode()) == Key.up) {
            this.player.toggleUpPressed();
        }
        if (this.controls.get(e.getKeyCode()) == Key.down) {
            this.player.toggleDownPressed();
        }
        if (this.controls.get(e.getKeyCode()) == Key.left) {
            this.player.toggleLeftPressed();
        }
        if (this.controls.get(e.getKeyCode()) == Key.right) {
            this.player.toggleRightPressed();
        }
        if (this.controls.get(e.getKeyCode()) == Key.action) {
            this.player.toggleActionPressed();
        }
    }

    /**
     * Reads the keys released and performs certain actions based on the key.
     * @param e The key released
     */
    @Override
    public void keyReleased(KeyEvent e) {
        if (this.controls.get(e.getKeyCode()) == Key.up) {
            this.player.unToggleUpPressed();
        }
        if (this.controls.get(e.getKeyCode()) == Key.down) {
            this.player.unToggleDownPressed();
        }
        if (this.controls.get(e.getKeyCode()) == Key.left) {
            this.player.unToggleLeftPressed();
        }
        if (this.controls.get(e.getKeyCode()) == Key.right) {
            this.player.unToggleRightPressed();
        }
        if (this.controls.get(e.getKeyCode()) == Key.action) {
            this.player.unToggleActionPressed();
        }
    }

}
=======================================================================================================================