import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

// ----------------- DSA: Obstacle + Doubly Linked List -----------------
class Obstacle {
    int lane;
    int row;
    Obstacle(int lane, int row) {
        this.lane = lane;
        this.row = row;
    }
}

class DoublyLinkedList<T> {
    static class Node<E> {
        E data;
        Node<E> prev, next;
        Node(E d) { data = d; }
    }
    private Node<T> head, tail;

    public Node<T> getHead() { return head; }

    public void addLast(T data) {
        Node<T> n = new Node<>(data);
        if (head == null) head = tail = n;
        else {
            tail.next = n;
            n.prev = tail;
            tail = n;
        }
    }

    public void remove(Node<T> node) {
        if (node == null) return;
        if (node == head && node == tail) {
            head = tail = null;
        } else if (node == head) {
            head = head.next;
            if (head != null) head.prev = null;
        } else if (node == tail) {
            tail = tail.prev;
            if (tail != null) tail.next = null;
        } else {
            node.prev.next = node.next;
            node.next.prev = node.prev;
        }
    }
}

// -------------------------- Game Panel --------------------------
public class CarGameGUI extends JPanel implements KeyListener, ActionListener {

    private static final int LANES = 3;
    private static final int ROWS  = 18;

    private int playerLane = 1;
    private int playerRow  = ROWS - 2;

    private DoublyLinkedList<Obstacle> obstacles = new DoublyLinkedList<>();
    private Random random = new Random();
    private Timer timer;

    private int score = 0;
    private boolean gameOver = false;

    // images
    private Image playerCarImg;
    private Image enemyCarImg;

    public CarGameGUI() {
        setFocusable(true);
        addKeyListener(this);

        // load images (PNG files should be in the same folder)
        playerCarImg = loadImage("player_car.png");
        enemyCarImg  = loadImage("enemy_car.png");

        timer = new Timer(180, this); // game speed
        timer.start();
    }

    private Image loadImage(String path) {
        ImageIcon icon = new ImageIcon(path);
        if (icon.getIconWidth() <= 0) {
            System.out.println("Could not load image: " + path + " (fallback to rectangle)");
            return null;
        }
        return icon.getImage();
    }

    // ----------------------- Drawing helpers -----------------------

    private void drawRoad(Graphics2D g2, int w, int h) {
        int laneWidth = w / LANES;

        // grass
        g2.setColor(new Color(15, 120, 25));
        g2.fillRect(0, 0, w, h);

        // road
        int roadX = laneWidth / 4;
        int roadW = w - laneWidth / 2;
        g2.setColor(new Color(50, 50, 50));
        g2.fillRect(roadX, 0, roadW, h);

        // borders
        g2.setColor(Color.YELLOW);
        g2.setStroke(new BasicStroke(4));
        g2.drawLine(roadX, 0, roadX, h);
        g2.drawLine(roadX + roadW, 0, roadX + roadW, h);

        // dashed lane lines
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3));
        for (int i = 1; i < LANES; i++) {
            int x = roadX + i * (roadW / LANES);
            for (int y = 0; y < h; y += 40) {
                g2.drawLine(x, y, x, y + 20);
            }
        }
    }

    private void drawCar(Graphics2D g2, int lane, int row,
                         int w, int h, boolean isPlayer) {

        int laneWidth = w / LANES;
        int rowHeight = h / ROWS;

        int roadX = laneWidth / 4;
        int roadW = w - laneWidth / 2;

        int cellW = roadW / LANES;
        int cellH = rowHeight;

        int x = roadX + lane * cellW;
        int y = row * cellH;

        Image img = isPlayer ? playerCarImg : enemyCarImg;

        int carW = (int)(cellW * 0.75);
        int carH = (int)(cellH * 0.9);
        int drawX = x + (cellW - carW) / 2;
        int drawY = y + (cellH - carH) / 2;

        if (img != null) {
            g2.drawImage(img, drawX, drawY, carW, carH, null);
        } else {
            // fallback: simple colored rectangle
            g2.setColor(isPlayer ? Color.GREEN : Color.RED);
            g2.fillRoundRect(drawX, drawY, carW, carH, 15, 15);
        }
    }

    // --------------------------- Painting ---------------------------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        drawRoad(g2, w, h);

        // draw obstacles
        DoublyLinkedList.Node<Obstacle> cur = obstacles.getHead();
        while (cur != null) {
            Obstacle o = cur.data;
            drawCar(g2, o.lane, o.row, w, h, false);
            cur = cur.next;
        }

        // draw player
        drawCar(g2, playerLane, playerRow, w, h, true);

        // HUD
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.PLAIN, 22));
        g2.drawString("Score: " + score, 25, 35);
        g2.drawString("← → move   ENTER restart", 25, 60);

        if (gameOver) {
            g2.setColor(Color.YELLOW);
            g2.setFont(new Font("Arial", Font.BOLD, 52));
            String msg = "GAME OVER";
            int sw = g2.getFontMetrics().stringWidth(msg);
            g2.drawString(msg, (w - sw) / 2, h / 2);

            g2.setFont(new Font("Arial", Font.PLAIN, 26));
            String msg2 = "Press ENTER to play again";
            sw = g2.getFontMetrics().stringWidth(msg2);
            g2.drawString(msg2, (w - sw) / 2, h / 2 + 40);
        }
    }

    // --------------------------- Game loop ---------------------------
    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) return;

        // spawn obstacle
        if (random.nextBoolean()) {
            obstacles.addLast(new Obstacle(random.nextInt(LANES), 0));
        }

        // move obstacles
        DoublyLinkedList.Node<Obstacle> cur = obstacles.getHead();
        while (cur != null) {
            DoublyLinkedList.Node<Obstacle> next = cur.next;
            Obstacle o = cur.data;
            o.row++;
            if (o.row >= ROWS) {
                obstacles.remove(cur);
            }
            cur = next;
        }

        // collision check
        cur = obstacles.getHead();
        while (cur != null) {
            Obstacle o = cur.data;
            if (o.row == playerRow && o.lane == playerLane) {
                gameOver = true;
                timer.stop();
                break;
            }
            cur = cur.next;
        }

        score++;
        repaint();
    }

    // ----------------------------- Input -----------------------------
    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                // restart
                obstacles = new DoublyLinkedList<>();
                score = 0;
                gameOver = false;
                playerLane = 1;
                timer.start();
            }
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_LEFT && playerLane > 0) {
            playerLane--;
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT && playerLane < LANES - 1) {
            playerLane++;
        }
        repaint();
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}

    // ------------------------------ Main ------------------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Car Dodge Game - Real Cars");
            CarGameGUI game = new CarGameGUI();
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setExtendedState(JFrame.MAXIMIZED_BOTH); // fullscreen
            f.add(game);
            f.setVisible(true);
            game.requestFocusInWindow();
        });
    }
}
