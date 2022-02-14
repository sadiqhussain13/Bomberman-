package gameobjects;

import util.GameObjectCollection;
import util.ResourceCollection;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Bomb objects that are created by bombers.
 */
public class Bomb extends TileObject {

    // Original bomber that placed this bomb
    private Bomber bomber;

    // Animation
    private BufferedImage[][] sprites;
    private int spriteIndex;
    private int spriteTimer;

    // Stats
    private int firepower;
    private boolean pierce;
    private int timeToDetonate;
    private int timeElapsed;

    // Kicking bomb
    private boolean kicked;
    private KickDirection kickDirection;

    /**
     * Constructs a bomb object with values passed in by a bomber object.
     * @param position Coordinates of this object in the game world
     * @param firepower Strength of the bomb explosionContact
     * @param pierce Whether or not the explosions will pierce soft walls
     * @param timer How long before the bomb detonates
     * @param bomber Original bomber that placed this bomb
     */
    public Bomb(Point2D.Float position, int firepower, boolean pierce, int timer, Bomber bomber) {
        super(position, pierce ? ResourceCollection.SpriteMaps.BOMB_PIERCE.getSprites()[0][0] : ResourceCollection.SpriteMaps.BOMB.getSprites()[0][0]);
        this.collider.setRect(this.position.x, this.position.y, this.width, this.height);

        // Animation
        this.sprites = pierce ? ResourceCollection.SpriteMaps.BOMB_PIERCE.getSprites() : ResourceCollection.SpriteMaps.BOMB.getSprites();
        this.spriteIndex = 0;
        this.spriteTimer = 0;

        // Stats
        this.firepower = firepower;
        this.pierce = pierce;
        this.timeToDetonate = timer;
        this.bomber = bomber;
        this.timeElapsed = 0;
        this.breakable = true;

        // Kicking bomb
        this.kicked = false;
        this.kickDirection = KickDirection.Nothing;
    }

    /**
     * Bomb detonates upon destroy and creates explosions. Also replenishes ammo for original bomber.
     */
    private void explode() {
        // Snap bombs to the grid on the map before exploding
        this.snapToGrid();
        GameObjectCollection.spawn(new Explosion.Horizontal(this.position, this.firepower, this.pierce));
        GameObjectCollection.spawn(new Explosion.Vertical(this.position, this.firepower, this.pierce));
        this.bomber.restoreAmmo();
    }

    public void setKicked(boolean kicked, KickDirection kickDirection) {
        this.kicked = kicked;
        this.kickDirection = kickDirection;
    }

    public boolean isKicked() {
        return this.kicked;
    }

    public void stopKick() {
        this.kicked = false;
        this.kickDirection = KickDirection.Nothing;
        this.snapToGrid();
    }

    /**
     * Controls animation and detonation timer.
     */
    @Override
    public void update() {
        this.collider.setRect(this.position.x, this.position.y, this.width, this.height);

        // Animate sprite
        if (this.spriteTimer++ >= 4) {
            this.spriteIndex++;
            this.spriteTimer = 0;
        }
        if (this.spriteIndex >= this.sprites[0].length) {
            this.spriteIndex = 0;
        }
        this.sprite = this.sprites[0][this.spriteIndex];

        // Detonate after timeToDetonate
        if (this.timeElapsed++ >= this.timeToDetonate) {
            this.destroy();
        }

        // Continue traveling when kicked
        if (this.kicked) {
            this.position.setLocation(this.position.x + this.kickDirection.getVelocity().x,
                    this.position.y + this.kickDirection.getVelocity().y);
        }
    }

    @Override
    public void onDestroy() {
        this.explode();
    }

    @Override
    public void onCollisionEnter(GameObject collidingObj) {
        collidingObj.handleCollision(this);
    }

    /**
     * Stops the bomb from moving when it encounters a bomber. Very ugly calculation to get this working so touching
     * this code is very dangerous and can introduce bugs to the kicking logic.
     * @param collidingObj Bomber object in the way
     */
    @Override
    public void handleCollision(Bomber collidingObj) {
        Point2D.Float temp = new Point2D.Float((float) this.collider.getCenterX() + this.kickDirection.getVelocity().x, (float) this.collider.getCenterY() + this.kickDirection.getVelocity().y);
        Rectangle2D intersection = this.collider.createIntersection(collidingObj.collider);
        if (this.kicked && intersection.contains(temp)) {
            System.out.println("Stop kick called");
            this.stopKick();
            this.solidCollision(collidingObj);
            this.snapToGrid();
        }
    }

    @Override
    public void handleCollision(Wall collidingObj) {
        this.solidCollision(collidingObj);
        this.stopKick();
    }

    @Override
    public void handleCollision(Bomb collidingObj) {
        this.solidCollision(collidingObj);
        this.stopKick();
    }

    /**
     * Bombs are immediately destroyed when colliding with explosionContact.
     * This is a different behavior than powerups and walls since they are not destroyed until the explosionContact animation finishes.
     * @param collidingObj Explosion that will detonate this bomb
     */
    @Override
    public void handleCollision(Explosion collidingObj) {
        this.destroy();
    }

    @Override
    public boolean isBreakable() {
        return this.breakable;
    }

}

/**
 * Provides the speed for bomb moving from kick. Speed should be 6 to ensure the kicking logic is as smooth
 * as possible. Changing the value is dangerous and can introduce bugs to the kicking logic.
 */
enum KickDirection {

    FromTop(new Point2D.Float(0, 6)),
    FromBottom(new Point2D.Float(0, -6)),
    FromLeft(new Point2D.Float(6, 0)),
    FromRight(new Point2D.Float(-6, 0)),
    Nothing(new Point2D.Float(0, 0));

    private Point2D.Float velocity;

    KickDirection(Point2D.Float velocity) {
        this.velocity = velocity;
    }

    public Point2D.Float getVelocity() {
        return this.velocity;
    }

}
==================================================================================================
package gameobjects;

import util.GameObjectCollection;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Bomberman player object to be controlled by a user.
 */
public class Bomber extends Player {

    private Bomb bomb;
    private boolean dead;

    // Animation
    private BufferedImage[][] sprites;
    private int direction;  // 0: up, 1: down, 2: left, 3: right
    private int spriteIndex;
    private int spriteTimer;

    // Stats
    private float moveSpeed;
    private int firepower;
    private int maxBombs;
    private int bombAmmo;
    private int bombTimer;
    private boolean pierce;
    private boolean kick;

    /**
     * Constructs a bomber at position with a two-dimensional array of sprites.
     * @param position Coordinates of this object in the game world
     * @param spriteMap 2D array of sprites used for animation
     */
    public Bomber(Point2D.Float position, BufferedImage[][] spriteMap) {
        super(position, spriteMap[1][0]);
        this.collider.setRect(this.position.x + 3, this.position.y + 16 + 3, this.width - 6, this.height - 16 - 6);

        // Animation
        this.sprites = spriteMap;
        this.direction = 1;     // Facing down
        this.spriteIndex = 0;
        this.spriteTimer = 0;

        // Default stats
        this.moveSpeed = 1;
        this.firepower = 1;
        this.maxBombs = 1;
        this.bombAmmo = this.maxBombs;
        this.bombTimer = 250;
        this.pierce = false;
        this.kick = false;
    }

    // --- MOVEMENT ---
    private void moveUp() {
        this.direction = 0;     // Using sprites that face up
        this.position.setLocation(this.position.x, this.position.y - this.moveSpeed);
    }
    private void moveDown() {
        this.direction = 1;     // Using sprites that face down
        this.position.setLocation(this.position.x, this.position.y + this.moveSpeed);
    }
    private void moveLeft() {
        this.direction = 2;     // Using sprites that face left
        this.position.setLocation(this.position.x - this.moveSpeed, this.position.y);
    }
    private void moveRight() {
        this.direction = 3;     // Using sprites that face right
        this.position.setLocation(this.position.x + this.moveSpeed, this.position.y);
    }

    // --- ACTION ---
    private void plantBomb() {
        // Snap bombs to the grid on the map
        float x = Math.round(this.position.getX() / 32) * 32;
        float y = Math.round((this.position.getY() + 16) / 32) * 32;
        Point2D.Float spawnLocation = new Point2D.Float(x, y);

        // Only one tile object allowed per tile; Cannot place a bomb on another object
        for (int i = 0; i < GameObjectCollection.tileObjects.size(); i++) {
            GameObject obj = GameObjectCollection.tileObjects.get(i);
            if (obj.collider.contains(spawnLocation)) {
                return;
            }
        }

        // Spawn the bomb
        this.bomb = new Bomb(spawnLocation, this.firepower, this.pierce, this.bombTimer, this);
        GameObjectCollection.spawn(bomb);
        this.bombAmmo--;
    }

    public void restoreAmmo() {
        this.bombAmmo = Math.min(this.maxBombs, this.bombAmmo + 1);
    }

    // --- POWERUPS ---
    public void addAmmo(int value) {
        System.out.print("Bombs set from " + this.maxBombs);
        this.maxBombs = Math.min(6, this.maxBombs + value);
        this.restoreAmmo();
        System.out.println(" to " + this.maxBombs);
    }
    public void addFirepower(int value) {
        System.out.print("Firepower set from " + this.firepower);
        this.firepower = Math.min(6, this.firepower + value);
        System.out.println(" to " + this.firepower);
    }
    public void addSpeed(float value) {
        System.out.print("Move Speed set from " + this.moveSpeed);
        this.moveSpeed = Math.min(4, this.moveSpeed + value);
        System.out.println(" to " + this.moveSpeed);
    }
    public void setPierce(boolean value) {
        System.out.print("Pierce set from " + this.pierce);
        this.pierce = value;
        System.out.println(" to " + this.pierce);
    }
    public void setKick(boolean value) {
        System.out.print("Kick set from " + this.kick);
        this.kick = value;
        System.out.println(" to " + this.kick);
    }
    public void reduceTimer(int value) {
        System.out.print("Bomb Timer set from " + this.bombTimer);
        this.bombTimer = Math.max(160, this.bombTimer - value);
        System.out.println(" to " + this.bombTimer);
    }

    /**
     * Used in game HUD to draw the base sprite to the info box.
     * @return The sprite of the bomber facing down
     */
    public BufferedImage getBaseSprite() {
        return this.sprites[1][0];
    }

    /**
     * Checks if this bomber is dead.
     * @return true = dead, false = not dead
     */
    public boolean isDead() {
        return this.dead;
    }

    /**
     * Controls movement, action, and animation.
     */
    @Override
    public void update() {
        this.collider.setRect(this.position.x + 3, this.position.y + 16 + 3, this.width - 6, this.height - 16 - 6);

        if (!this.dead) {
            // Animate sprite
            if ((this.spriteTimer += this.moveSpeed) >= 12) {
                this.spriteIndex++;
                this.spriteTimer = 0;
            }
            if ((!this.UpPressed && !this.DownPressed && !this.LeftPressed && !this.RightPressed) || (this.spriteIndex >= this.sprites[0].length)) {
                this.spriteIndex = 0;
            }
            this.sprite = this.sprites[this.direction][this.spriteIndex];

            // Movement
            if (this.UpPressed) {
                this.moveUp();
            }
            if (this.DownPressed) {
                this.moveDown();
            }
            if (this.LeftPressed) {
                this.moveLeft();
            }
            if (this.RightPressed) {
                this.moveRight();
            }

            // Action
            if (this.ActionPressed && this.bombAmmo > 0) {
                this.plantBomb();
            }
        } else {
            // Animate dying animation
            if (this.spriteTimer++ >= 30) {
                this.spriteIndex++;
                if (this.spriteIndex < this.sprites[4].length) {
                    this.sprite = this.sprites[4][this.spriteIndex];
                    this.spriteTimer = 0;
                } else if (this.spriteTimer >= 250) {
                    this.destroy();
                }
            }
        }
    }

    @Override
    public void onCollisionEnter(GameObject collidingObj) {
        collidingObj.handleCollision(this);
    }

    @Override
    public void handleCollision(Wall collidingObj) {
        this.solidCollision(collidingObj);
    }

    /**
     * Die immediately if not dead. This bomber is also killed.
     * @param collidingObj The explosion that kills this bomber
     */
    @Override
    public void handleCollision(Explosion collidingObj) {
        if (!this.dead) {
            this.dead = true;
            this.spriteIndex = 0;
        }
    }

    /**
     * Bombs act as walls if the bomber is not already within the a certain distance as the bomb.
     * This is also the big and ugly kicking logic. Touching this code is very dangerous and can introduce
     * bugs to the kicking logic including stopping the bomb from moving.
     * (ie. if the bomber is not standing on the bomb)
     * @param collidingObj Solid bomb
     */
    @Override
    public void handleCollision(Bomb collidingObj) {
        Rectangle2D intersection = this.collider.createIntersection(collidingObj.collider);
        // Vertical collision
        if (intersection.getWidth() >= intersection.getHeight() && intersection.getHeight() <= 6 && Math.abs(this.collider.getCenterX() - collidingObj.collider.getCenterX()) <= 8) {
            if (this.kick && !collidingObj.isKicked()) {
                // From the top
                if (intersection.getMaxY() >= this.collider.getMaxY() && this.DownPressed) {
                    collidingObj.setKicked(true, KickDirection.FromTop);
                }
                // From the bottom
                if (intersection.getMaxY() >= collidingObj.collider.getMaxY() && this.UpPressed) {
                    collidingObj.setKicked(true, KickDirection.FromBottom);
                }
            }
            this.solidCollision(collidingObj);
        }
        // Horizontal collision
        if (intersection.getHeight() >= intersection.getWidth() && intersection.getWidth() <= 6 && Math.abs(this.collider.getCenterY() - collidingObj.collider.getCenterY()) <= 8) {
            if (this.kick && !collidingObj.isKicked()) {
                // From the left
                if (intersection.getMaxX() >= this.collider.getMaxX() && this.RightPressed) {
                    collidingObj.setKicked(true, KickDirection.FromLeft);
                }
                // From the right
                if (intersection.getMaxX() >= collidingObj.collider.getMaxX() && this.LeftPressed) {
                    collidingObj.setKicked(true, KickDirection.FromRight);
                }
            }
            this.solidCollision(collidingObj);
        }
    }

    /**
     * Get powerup bonus depending on the type.
     * @param collidingObj Powerup that provides the bonus
     */
    @Override
    public void handleCollision(Powerup collidingObj) {
        collidingObj.grantBonus(this);
        collidingObj.destroy();
    }

}
======================================================================================================================
package gameobjects;

import util.GameObjectCollection;
import util.ResourceCollection;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Base class for two types of explosions: horizontal and vertical.
 */
public abstract class Explosion extends GameObject {

    /**
     * Horizontal explosionContact class.
     */
    public static class Horizontal extends Explosion {

        /**
         * Constructs a horizontal explosionContact that varies in length depending on firepower and pierce.
         * @param position Coordinates of this object in the game world
         * @param firepower Strength of this explosionContact
         * @param pierce Whether or not this explosionContact will pierce soft walls
         */
        Horizontal(Point2D.Float position, int firepower, boolean pierce) {
            super(position);

            float leftX = this.checkHorizontal(this.position, firepower, pierce, -32);
            float rightX = this.checkHorizontal(this.position, firepower, pierce, 32);
            this.centerOffset = position.x - leftX; // The offset is used to draw the center explosionContact sprite

            Rectangle2D.Float recH = new Rectangle2D.Float(leftX, this.position.y, rightX - leftX + 32, 32);
            this.init(recH);

            this.animation = this.drawSprite((int) this.width, (int) this.height);

            this.sprite = this.animation[0];
        }

        /**
         * Check for walls to determine explosionContact range. Used for left and right.
         * @param position Original position of bomb prior to explosionContact
         * @param firepower Maximum range of explosionContact
         * @param blockWidth Size of each game object tile, negative for left, positive for right
         * @return Position of the explosionContact's maximum range in horizontal direction
         */
        private float checkHorizontal(Point2D.Float position, int firepower, boolean pierce, int blockWidth) {
            float value = position.x;   // Start at the origin tile

            outer: for (int i = 1; i <= firepower; i++) {
                // Expand one tile at a time
                value += blockWidth;

                // Check this tile for wall collision
                for (int index = 0; index < GameObjectCollection.tileObjects.size(); index++) {
                    TileObject obj = GameObjectCollection.tileObjects.get(index);
                    if (obj.collider.contains(value, position.y)) {
                        if (!obj.isBreakable()) {
                            // Hard wall found, move value back to the tile before
                            value -= blockWidth;
                        }

                        // Stop checking for tile objects after the first breakable is found
                        if (!pierce) {
                            break outer;
                        }
                    }
                }
            }

            return value;
        }

        /**
         * Draws the explosionContact sprite after determining its length and center.
         * @param width Explosion width
         * @param height Explosion height
         * @return Array of sprites for animation
         */
        private BufferedImage[] drawSprite(int width, int height) {
            // Initialize each image in the array to be drawn to
            BufferedImage[] spriteAnimation = new BufferedImage[ResourceCollection.SpriteMaps.EXPLOSION_SPRITEMAP.getImage().getWidth() / 32];
            for (int i = 0; i < spriteAnimation.length; i++) {
                spriteAnimation[i] = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            }

            // Draw to each image in the array
            for (int i = 0; i < spriteAnimation.length; i++) {
                Graphics2D g2 = spriteAnimation[i].createGraphics();
                g2.setColor(new Color(0, 0, 0, 0));
                g2.fillRect(0, 0, spriteAnimation[i].getWidth(), spriteAnimation[i].getHeight());

                for (int j = 0; j < spriteAnimation[i].getWidth() / 32; j++) {
                    if (spriteAnimation[i].getWidth() / 32 == 1 || this.centerOffset == j * 32) {
                        // Center sprite
                        g2.drawImage(this.sprites[0][i], j * 32, 0, null);
                    } else if (j == 0) {
                        // Leftmost sprite
                        g2.drawImage(this.sprites[3][i], j * 32, 0, null);
                    } else if (j == (spriteAnimation[i].getWidth() / 32) - 1) {
                        // Rightmost sprite
                        g2.drawImage(this.sprites[4][i], j * 32, 0, null);
                    } else {
                        // Horizontal between sprite
                        g2.drawImage(this.sprites[1][i], j * 32, 0, null);
                    }
                }

                g2.dispose();
            }

            return spriteAnimation;
        }

    }

    /**
     * Vertical explosionContact class.
     */
    public static class Vertical extends Explosion {

        /**
         * Constructs a horizontal explosionContact that varies in length depending on firepower and pierce.
         * @param position Coordinates of this object in the game world
         * @param firepower Strength of this explosionContact
         * @param pierce Whether or not this explosionContact will pierce soft walls
         */
        Vertical(Point2D.Float position, int firepower, boolean pierce) {
            super(position);

            float topY = this.checkVertical(this.position, firepower, pierce, -32);
            float bottomY = this.checkVertical(this.position, firepower, pierce, 32);
            this.centerOffset = position.y - topY;  // The offset is used to draw the center explosionContact sprite

            Rectangle2D.Float recV = new Rectangle2D.Float(this.position.x, topY, 32, bottomY - topY + 32);
            this.init(recV);

            this.animation = this.drawSprite((int) this.width, (int) this.height);

            this.sprite = this.animation[0];
        }

        /**
         * Check for walls to determine explosionContact range. Used for top and bottom.
         * @param position Original position of bomb prior to explosionContact
         * @param firepower Maximum range of explosionContact
         * @param blockHeight Size of each game object tile, negative for top, positive for bottom
         * @return Position of the explosionContact's maximum range in vertical direction
         */
        private float checkVertical(Point2D.Float position, int firepower, boolean pierce, int blockHeight) {
            float value = position.y;   // Start at the origin tile

            outer: for (int i = 1; i <= firepower; i++) {
                // Expand one tile at a time
                value += blockHeight;

                // Check this tile for wall collision
                for (int index = 0; index < GameObjectCollection.tileObjects.size(); index++) {
                    TileObject obj = GameObjectCollection.tileObjects.get(index);
                    if (obj.collider.contains(position.x, value)) {
                        if (!obj.isBreakable()) {
                            // Hard wall found, move value back to the tile before
                            value -= blockHeight;
                        }

                        // Stop checking for tile objects after the first breakable is found
                        if (!pierce) {
                            break outer;
                        }
                    }
                }
            }

            return value;
        }

        /**
         * Draws the explosionContact sprite after determining its length and center.
         * @param width Explosion width
         * @param height Explosion height
         * @return Array of sprites for animation
         */
        private BufferedImage[] drawSprite(int width, int height) {
            // Initialize each image in the array to be drawn to
            BufferedImage[] spriteAnimation = new BufferedImage[ResourceCollection.SpriteMaps.EXPLOSION_SPRITEMAP.getImage().getWidth() / 32];
            for (int i = 0; i < spriteAnimation.length; i++) {
                spriteAnimation[i] = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            }

            // Draw to each image in the array
            for (int i = 0; i < spriteAnimation.length; i++) {
                Graphics2D g2 = spriteAnimation[i].createGraphics();
                g2.setColor(new Color(0, 0, 0, 0));
                g2.fillRect(0, 0, spriteAnimation[i].getWidth(), spriteAnimation[i].getHeight());

                for (int j = 0; j < spriteAnimation[i].getHeight() / 32; j++) {
                    if (spriteAnimation[i].getHeight() / 32 == 1 || this.centerOffset == j * 32) {
                        // Center sprite
                        g2.drawImage(this.sprites[0][i], 0, j * 32, null);
                    } else if (j == 0) {
                        // Topmost sprite
                        g2.drawImage(this.sprites[5][i], 0, j * 32, null);
                    } else if (j == (spriteAnimation[i].getHeight() / 32) - 1) {
                        // Bottommost sprite
                        g2.drawImage(this.sprites[6][i], 0, j * 32, null);
                    } else {
                        // Vertical between sprite
                        g2.drawImage(this.sprites[2][i], 0, j * 32, null);
                    }
                }

                g2.dispose();
            }

            return spriteAnimation;
        }

    }


    // --- BASE CLASS ---

    protected BufferedImage[][] sprites;
    protected BufferedImage[] animation;
    protected float centerOffset;
    private int spriteIndex;
    private int spriteTimer;

    /**
     * Constructor called in horizontal and vertical constructors.
     * @param position Coordinates of this object in the game world
     */
    Explosion(Point2D.Float position) {
        super(position);
        this.sprites = ResourceCollection.SpriteMaps.EXPLOSION_SPRITEMAP.getSprites();

        this.centerOffset = 0;
        this.spriteIndex = 0;
        this.spriteTimer = 0;
    }

    /**
     * Called later in the constructor to set collider.
     * @param collider Collider for this to be set to
     */
    protected void init(Rectangle2D.Float collider) {
        this.collider = collider;
        this.width = this.collider.width;
        this.height = this.collider.height;
        this.sprite = new BufferedImage((int) this.width, (int) this.height, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Controls animation and destroy when it finishes
     */
    @Override
    public void update() {
        // Animate sprite
        if (this.spriteTimer++ >= 4) {
            this.spriteIndex++;
            this.spriteTimer = 0;
        }
        if (this.spriteIndex >= this.animation.length) {
            this.destroy();
        } else {
            this.sprite = this.animation[this.spriteIndex];
        }
    }

    @Override
    public void onCollisionEnter(GameObject collidingObj) {
        collidingObj.handleCollision(this);
    }

    /**
     * Draw based on the collider's position instead of this object's own position.
     * @param g Graphics object that is passed in for the game object to draw to
     */
    @Override
    public void drawImage(Graphics g) {
        AffineTransform rotation = AffineTransform.getTranslateInstance(this.collider.x, this.collider.y);
        rotation.rotate(Math.toRadians(this.rotation), this.collider.width / 2.0, this.collider.height / 2.0);
        Graphics2D g2d = (Graphics2D) g;
        g2d.drawImage(this.sprite, rotation, null);
    }

}
======================================================================================================================
package gameobjects;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Abstract game object class. All game objects extend this class.
 * The base class for all game objects with properties that allow it to exist in the game world.
 */
public abstract class GameObject implements Observable, Collidable, Comparable<GameObject> {

    // Common data fields for game objects
    BufferedImage sprite;
    Point2D.Float position;
    Rectangle2D.Float collider;
    float rotation;
    float width;
    float height;

    // Marked for deletion
    private boolean destroyed;

    /**
     * Creates a new position for this game object at position. Used for objects with no predefined sprite such as explosionContact.
     * @param position Position of this game object
     */
    GameObject(Point2D.Float position) {
        this.position = new Point2D.Float(position.x, position.y);
        this.rotation = 0;
    }

    /**
     * Creates a new position for this game object at position. Sets the sprite of this game object using constructor.
     * @param position Position of this game object
     * @param sprite Sprite of this game object
     */
    GameObject(Point2D.Float position, BufferedImage sprite) {
        this(sprite);
        this.position = new Point2D.Float(position.x, position.y);
        this.rotation = 0;
        this.collider = new Rectangle2D.Float(position.x, position.y, this.width, this.height);
    }

    /**
     * To be called by other constructors. Set the sprite of the game object and its width and height depending on the sprite.
     * @param sprite
     */
    private GameObject(BufferedImage sprite) {
        this.sprite = sprite;
        this.width = this.sprite.getWidth();
        this.height = this.sprite.getHeight();
    }

    /**
     * Mark this game object for deletion.
     */
    void destroy() {
        this.destroyed = true;
    }

    /**
     * Check if this game object is destroyed.
     * @return If this game object is destroyed or not
     */
    public boolean isDestroyed() {
        return destroyed;
    }

    /**
     * Handle collision with solid objects such as walls.
     * @param obj A solid object such as a wall
     */
    void solidCollision(GameObject obj) {
        Rectangle2D intersection = this.collider.createIntersection(obj.collider);
        // Vertical collision
        if (intersection.getWidth() >= intersection.getHeight()) {
            // From the top
            if (intersection.getMaxY() >= this.collider.getMaxY()) {
                this.position.setLocation(this.position.x, this.position.y - intersection.getHeight());
            }
            // From the bottom
            if (intersection.getMaxY() >= obj.collider.getMaxY()) {
                this.position.setLocation(this.position.x, this.position.y + intersection.getHeight());
            }

            // Smoothing around corners
            if (intersection.getWidth() < 16) {
                if (intersection.getMaxX() >= this.collider.getMaxX()) {
                    this.position.setLocation(this.position.x - 0.5, this.position.y);
                }
                if (intersection.getMaxX() >= obj.collider.getMaxX()) {
                    this.position.setLocation(this.position.x + 0.5, this.position.y);
                }
            }
        }

        // Horizontal collision
        if (intersection.getHeight() >= intersection.getWidth()) {
            // From the left
            if (intersection.getMaxX() >= this.collider.getMaxX()) {
                this.position.setLocation(this.position.x - intersection.getWidth(), this.position.y);
            }
            // From the right
            if (intersection.getMaxX() >= obj.collider.getMaxX()) {
                this.position.setLocation(this.position.x + intersection.getWidth(), this.position.y);
            }

            // Smoothing around corners
            if (intersection.getHeight() < 16) {
                if (intersection.getMaxY() >= this.collider.getMaxY()) {
                    this.position.setLocation(this.position.x, this.position.y - 0.5);
                }
                if (intersection.getMaxY() >= obj.collider.getMaxY()) {
                    this.position.setLocation(this.position.x, this.position.y + 0.5);
                }
            }
        }
    }

    /**
     * Get the rectangle collider of this game object.
     * @return A Rectangle2D collider
     */
    public Rectangle2D.Float getCollider() {
        return this.collider;
    }

    /**
     * Get the center of the collider of this game object.
     * @return A Point2D at the center of the collider
     */
    public Point2D.Float getColliderCenter() {
        return new Point2D.Float((float) this.collider.getCenterX(), (float) this.collider.getCenterY());
    }

    /**
     * Get the maximum y position of this game object.
     * @return y position + height
     */
    public float getPositionY() {
        return this.position.y + this.height;
    }

    /**
     * Draws the game object in the game world to g.
     * (ie. the buffer which will be drawn to the screen)
     * @param g Graphics object that is passed in for the game object to draw to
     */
    public void drawImage(Graphics g) {
        AffineTransform rotation = AffineTransform.getTranslateInstance(this.position.getX(), this.position.getY());
        rotation.rotate(Math.toRadians(this.rotation), this.sprite.getWidth() / 2.0, this.sprite.getHeight() / 2.0);
        Graphics2D g2d = (Graphics2D) g;
        g2d.drawImage(this.sprite, rotation, null);
    }

    /**
     * Draw the game object's collider to the game world for debugging.
     * @param g Graphics object that is passed in for the game object to draw to
     */
    public void drawCollider(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.draw(this.collider);
    }

    /**
     * Compares the y position of two game objects.
     * Used to sort game object collection so that drawing game objects will draw in the order of y positions.
     * This adds a kind of depth to the game world.
     * @param o Game object to be compared to
     * @return -1 = less than, 1 = greater than, 0 = equal
     */
    @Override
    public int compareTo(GameObject o) {
        return Float.compare(this.position.y, o.position.y);
    }

}

/**
 * Observer pattern game state updating. Game objects perform certain actions based on the state of the game.
 */
interface Observable {

    /**
     * Repeatedly called during the game loop.
     */
    default void update() {

    }

    /**
     * Called when the game object gets destroyed.
     */
    default void onDestroy() {

    }

}

/**
 * Visitor pattern collision handling. Blank default methods so that subclasses only need to
 * override the ones they need to avoid overriding them in every subclass only to leave them empty.
 * Not all game objects interact with every other game object.
 */
interface Collidable {

    /**
     * Called when two objects collide. Override this in GameObject subclasses.
     * Usage: collidingObj.handleCollision(this);   // Put this inside the method body
     * @param collidingObj The object that this is colliding with
     */
    void onCollisionEnter(GameObject collidingObj);

    default void handleCollision(Bomber collidingObj) {

    }

    default void handleCollision(Wall collidingObj) {

    }

    default void handleCollision(Explosion collidingObj) {

    }

    default void handleCollision(Bomb collidingObj) {

    }

    default void handleCollision(Powerup collidingObj) {

    }
}
===============================================================================================
package gameobjects;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * Player class for game objects that will be controlled by the user.
 */
public abstract class Player extends GameObject {

    protected boolean UpPressed = false;
    protected boolean DownPressed = false;
    protected boolean LeftPressed = false;
    protected boolean RightPressed = false;
    protected boolean ActionPressed = false;

    /**
     * Passing parameters to GameObject constructor.
     * @param position
     * @param sprite
     */
    Player(Point2D.Float position, BufferedImage sprite) {
        super(position, sprite);
    }

    public void toggleUpPressed() {
        this.UpPressed = true;
    }
    public void toggleDownPressed() {
        this.DownPressed = true;
    }
    public void toggleLeftPressed() {
        this.LeftPressed = true;
    }
    public void toggleRightPressed() {
        this.RightPressed = true;
    }
    public void toggleActionPressed() {
        this.ActionPressed = true;
    }

    public void unToggleUpPressed() {
        this.UpPressed = false;
    }
    public void unToggleDownPressed() {
        this.DownPressed = false;
    }
    public void unToggleLeftPressed() {
        this.LeftPressed = false;
    }
    public void unToggleRightPressed() {
        this.RightPressed = false;
    }
    public void unToggleActionPressed() {
        this.ActionPressed = false;
    }

}
=============================================================================================================
package gameobjects;

import util.ResourceCollection;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Powerups with predefined types that spawn from breakable walls at random.
 * These powerups grant bombers various bonuses when collided with.
 */
public class Powerup extends TileObject {

    public enum Type {
        // Additional bomb ammo
        Bomb(ResourceCollection.Images.POWER_BOMB.getImage()) {
            @Override
            protected void grantBonus(Bomber bomber) {
                bomber.addAmmo(1);
            }
        },

        // Increases firepower
        Fireup(ResourceCollection.Images.POWER_FIREUP.getImage()) {
            @Override
            protected void grantBonus(Bomber bomber) {
                bomber.addFirepower(1);
            }
        },

        // Increases firepower to max
        Firemax(ResourceCollection.Images.POWER_FIREMAX.getImage()) {
            @Override
            protected void grantBonus(Bomber bomber) {
                bomber.addFirepower(6);
            }
        },

        // Increases speed
        Speed(ResourceCollection.Images.POWER_SPEED.getImage()) {
            @Override
            protected void grantBonus(Bomber bomber) {
                bomber.addSpeed(0.5f);
            }
        },

        // Adds ability for explosions to pierce soft walls
        Pierce(ResourceCollection.Images.POWER_PIERCE.getImage()) {
            @Override
            protected void grantBonus(Bomber bomber) {
                bomber.setPierce(true);
            }
        },

        // Adds ability to kick bombs
        Kick(ResourceCollection.Images.POWER_KICK.getImage()) {
            @Override
            protected void grantBonus(Bomber bomber) {
                bomber.setKick(true);
            }
        },

        // Reduces time for bomb to detonate
        Timer(ResourceCollection.Images.POWER_TIMER.getImage()) {
            @Override
            protected void grantBonus(Bomber bomber) {
                bomber.reduceTimer(15);
            }
        };

        private BufferedImage sprite;

        /**
         * Sets the sprite of the powerup type.
         * @param sprite Powerup sprite
         */
        Type(BufferedImage sprite) {
            this.sprite = sprite;
        }

        /**
         * To be overridden by powerup types. Grants bonuses to bomber.
         * @param bomber Bomber object to be granted bonus
         */
        protected abstract void grantBonus(Bomber bomber);

    }

    private Type type;

    /**
     * Construct a powerup of type. Type can be random.
     * @param position Coordinates of this object in the game world
     * @param type Type of powerup
     */
    public Powerup(Point2D.Float position, Type type) {
        super(position, type.sprite);
        this.collider = new Rectangle2D.Float(position.x + 8, position.y + 8, this.width - 16, this.height - 16);
        this.type = type;
        this.breakable = true;
    }

    // Random powerups
    private static Powerup.Type[] powerups = Powerup.Type.values();
    private static Random random = new Random();
    static final Powerup.Type randomPower() {
        return powerups[random.nextInt(powerups.length)];
    }

    /**
     * Grants bonuses to bomber.
     * @param bomber Bomber object to be granted bonus
     */
    void grantBonus(Bomber bomber) {
        this.type.grantBonus(bomber);
    }

    /**
     * Destroy powerup when explosion animation finishes.
     */
    @Override
    public void update() {
        if (this.checkExplosion()) {
            this.destroy();
        }
    }

    @Override
    public void onCollisionEnter(GameObject collidingObj) {
        collidingObj.handleCollision(this);
    }

    @Override
    public void handleCollision(Bomb collidingObj) {
        this.destroy();
    }

    @Override
    public boolean isBreakable() {
        return this.breakable;
    }

}
========================================================================================================
package gameobjects;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * Bombs, walls, powerups; stationary objects that take up a tile on the map and may be destructible by explosions.
 */
public abstract class TileObject extends GameObject {

    // The explosionContact object that will destroy this object after the explosionContact animation finishes
    // and if this object is breakable
    protected Explosion explosionContact;
    protected boolean breakable;

    /**
     * Construct a tile-based object that is aligned with the grid-based map.
     * @param position Coordinates of this object in the game world
     * @param sprite Sprite of this object
     */
    TileObject(Point2D.Float position, BufferedImage sprite) {
        super(position, sprite);
        this.snapToGrid();
    }

    /**
     * Check if this object is allowed to be destroyed by other objects.
     * @return true = breakable, false = unbreakable
     */
    public abstract boolean isBreakable();

    /**
     * Checks if the explosion that is in contact with this object has been destroyed.
     * Used to destroy this object the moment the explosion animation finishes.
     * @return true = explosion animation finished, false = explosion is still animating
     */
    protected boolean checkExplosion() {
        return this.isBreakable() && this.explosionContact != null && this.explosionContact.isDestroyed();
    }

    /**
     * Snaps this object to be aligned to the grid with unit size 32x32.
     */
    protected void snapToGrid() {
        // Snap bombs to the grid on the map
        float x = Math.round(this.position.getX() / 32) * 32;
        float y = Math.round(this.position.getY() / 32) * 32;
        this.position.setLocation(x, y);
    }

    /**
     * First explosionContact to collide this wall will destroy this object once its animation finishes
     * @param collidingObj First explosionContact to collide this wall
     */
    @Override
    public void handleCollision(Explosion collidingObj) {
        if (this.isBreakable()) {
            if (this.explosionContact == null) {
                this.explosionContact = collidingObj;
            }
        }
    }

}
===============================================================================================
package gameobjects;

import util.GameObjectCollection;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * The base class for various types of walls. Namely hard wall and soft wall.
 */
public class Wall extends TileObject {

    /**
     * Constructs a wall object that is either breakable or not.
     * @param position Coordinates of this object in the game world
     * @param sprite Sprite of this object
     * @param isBreakable true = soft wall, false = hard wall
     */
    public Wall(Point2D.Float position, BufferedImage sprite, boolean isBreakable) {
        super(position, sprite);
        this.breakable = isBreakable;
    }

    /**
     * Destroy wall when explosionContact animation finishes.
     */
    @Override
    public void update() {
        if (this.checkExplosion()) {
            this.destroy();
        }
    }

    /**
     * Chance for a random powerup to spawn upon destroy.
     */
    @Override
    public void onDestroy() {
        double random = Math.random();
        if (random < 0.5) {
            Powerup powerup = new Powerup(this.position, Powerup.randomPower());
            GameObjectCollection.spawn(powerup);
        }
    }

    @Override
    public void onCollisionEnter(GameObject collidingObj) {
        collidingObj.handleCollision(this);
    }

    /**
     * Checks if this is a hard wall or soft wall
     * @return true = soft wall, false = hard wall
     */
    @Override
    public boolean isBreakable() {
        return this.breakable;
    }

}
=======================================================================================================