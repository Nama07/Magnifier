import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Hashtable;

/**
 * A lightweight magnifier tool with a floating zoom window
 * that follows the mouse cursor and a separate control panel
 * for enabling/disabling it and adjusting zoom level.
 */
public class Magnifier extends JFrame {

    // Dimensions of the magnifier window
    private static final int MAGNIFIER_WIDTH = 350;
    private static final int MAGNIFIER_HEIGHT = 200;

    // Controls zoom level and magnifier state
    private volatile double zoom = 1.7;
    private volatile boolean magnifierEnabled = true;

    private final Robot robot;                 // Robot to capture screen
    private final Timer timer;                 // Periodic screen capture
    private final JFrame magnifierFrame;       // The magnifier window
    private final JLabel imageLabel;           // Label to show the magnified image

    public Magnifier() throws AWTException {
        super("Magnifier Control");

        // === Magnifier Window Setup ===
        magnifierFrame = new JFrame();
        magnifierFrame.setUndecorated(true);                      // No window decorations
        magnifierFrame.setAlwaysOnTop(true);                      // Always on top
        magnifierFrame.setSize(MAGNIFIER_WIDTH, MAGNIFIER_HEIGHT);
        magnifierFrame.setBackground(new Color(0, 0, 0, 0));      // Transparent background

        imageLabel = new JLabel();
        magnifierFrame.add(imageLabel);

        robot = new Robot();

        // Timer to refresh the magnified image every 40ms
        timer = new Timer(40, e -> {
            if (magnifierEnabled) {
                updateMagnifier();
                if (!magnifierFrame.isVisible()) magnifierFrame.setVisible(true);
            } else {
                magnifierFrame.setVisible(false);
            }
        });
        timer.start();

        // === UI Control Window Setup ===
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(300, 180);
        setLocationRelativeTo(null);

        Font font = new Font("Segoe UI", Font.PLAIN, 13);

        // Main panel with dark background and padding
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setBackground(new Color(30, 30, 30));

        // Zoom label
        JLabel zoomLabel = new JLabel("Zoom Level:");
        zoomLabel.setFont(font);
        zoomLabel.setForeground(Color.LIGHT_GRAY);
        zoomLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Slider panel
        JPanel sliderPanel = new JPanel(new BorderLayout());
        sliderPanel.setBackground(new Color(30, 30, 30));
        sliderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Slider to control zoom
        JSlider zoomSlider = new JSlider(10, 50, (int)(zoom * 10));
        zoomSlider.setPaintTicks(true);
        zoomSlider.setMajorTickSpacing(10);
        zoomSlider.setPaintLabels(true);
        zoomSlider.setBackground(new Color(30, 30, 30));
        zoomSlider.setForeground(Color.LIGHT_GRAY);
        zoomSlider.setFont(font);

        // White custom tick labels
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        for (int i = 10; i <= 50; i += 10) {
            JLabel label = new JLabel(String.format("%.1f", i / 10.0));
            label.setForeground(Color.WHITE);
            label.setFont(font);
            labelTable.put(i, label);
        }
        zoomSlider.setLabelTable(labelTable);

        sliderPanel.add(zoomSlider, BorderLayout.CENTER);
        sliderPanel.setMaximumSize(new Dimension(280, 60)); // Wider slider space

        // Custom styled toggle button (enable/disable magnifier)
        JButton toggleButton = new JButton("Disable Magnifier") {
            @Override
            protected void paintComponent(Graphics g) {
                // Fill background depending on pressed state
                if (getModel().isPressed()) {
                    g.setColor(new Color(60, 60, 60));
                } else {
                    g.setColor(new Color(45, 45, 45));
                }
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g);
            }
        };
        toggleButton.setFont(font);
        toggleButton.setFocusPainted(false);
        toggleButton.setBorderPainted(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.setOpaque(false);
        toggleButton.setForeground(Color.WHITE);
        toggleButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        toggleButton.setPreferredSize(new Dimension(250, 30));

        // === Actions ===
        // Zoom slider changes the magnification
        zoomSlider.addChangeListener(e -> zoom = zoomSlider.getValue() / 10.0);

        // Toggle magnifier on/off
        toggleButton.addActionListener(e -> {
            magnifierEnabled = !magnifierEnabled;
            toggleButton.setText(magnifierEnabled ? "Disable Magnifier" : "Enable Magnifier");
            if (!magnifierEnabled) magnifierFrame.setVisible(false);
        });

        // === Final Assembly ===
        panel.add(zoomLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(sliderPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        panel.add(toggleButton);

        add(panel, BorderLayout.CENTER);
        setVisible(true);
    }

    /**
     * Captures the screen under the cursor, zooms it, and displays it in the floating magnifier window.
     */
    private void updateMagnifier() {
        Point mousePos = MouseInfo.getPointerInfo().getLocation();

        // Calculate size and position of the capture area
        int captureWidth = (int) (MAGNIFIER_WIDTH / zoom);
        int captureHeight = (int) (MAGNIFIER_HEIGHT / zoom);
        int x = mousePos.x - captureWidth / 2;
        int y = mousePos.y - captureHeight / 2;

        // Capture the screen region under the mouse
        BufferedImage screen = robot.createScreenCapture(new Rectangle(x, y, captureWidth, captureHeight));

        // Create a scaled image (zoomed)
        BufferedImage zoomedImage = new BufferedImage(MAGNIFIER_WIDTH, MAGNIFIER_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = zoomedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(screen, 0, 0, MAGNIFIER_WIDTH, MAGNIFIER_HEIGHT, null);

        //border around the magnifier window
        g2d.setColor(new Color(200, 200, 200, 150)); 
        g2d.setStroke(new BasicStroke(3f));
        g2d.drawRoundRect(1, 2, MAGNIFIER_WIDTH - 3, MAGNIFIER_HEIGHT - 4, 16, 16);

        g2d.dispose();

        // Show in the magnifier window
        imageLabel.setIcon(new ImageIcon(zoomedImage));
        magnifierFrame.setLocation(mousePos.x + 30, mousePos.y + 60); // Offset to avoid covering cursor
    }

    public static void main(String[] args) throws AWTException {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); // Native look
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            try {
                new Magnifier();
            } catch (AWTException ex) {
                ex.printStackTrace();
            }
        });
    }
}
