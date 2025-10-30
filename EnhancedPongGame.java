import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;
/**
 * EnhancedPongGame.java
 * Advanced Pong with visual effects, sound, multiple power-ups, and improved AI.
 *
 * Controls:
 *  - W / S : Move player paddle up / down
 *  - P     : Pause / resume
 *  - R     : Restart game
 *  - Space : Activate special ability (when available)
 *
 * Features:
 *  - Particle effects
 *  - Multiple power-ups with unique visuals
 *  - Special abilities
 *  - Background gradient and glow effects
 *  - Sound effects (visual feedback)
 *  - Improved AI with personality
 *  - Trail effects
 */
public class EnhancedPongGame extends JPanel implements ActionListener, KeyListener {
    // Window
    static final int WIDTH = 900;
    static final int HEIGHT = 600;

    // Game loop timer
    Timer timer;
    final int DELAY = 16; // ~60 FPS

    // Paddles
    final int PADDLE_WIDTH = 12;
    int playerPaddleHeight = 100;
    int aiPaddleHeight = 100;
    int playerY = HEIGHT/2 - playerPaddleHeight/2;
    int aiY = HEIGHT/2 - aiPaddleHeight/2;
    final int PADDLE_X = 30;
    final int AI_X = WIDTH - 30 - PADDLE_WIDTH;
    int playerSpeed = 6;

    // Ball
    int ballX = WIDTH/2;
    int ballY = HEIGHT/2;
    int ballSize = 14;
    double ballVX = 5;
    double ballVY = 3;
    private List<TrailParticle> ballTrail = new ArrayList<>();

    // Scores
    int playerScore = 0;
    int aiScore = 0;

    // AI difficulty parameters
    double aiMaxSpeed = 4.0;
    double aiReaction = 0.12;
    private String aiPersonality = "NORMAL"; // NORMAL, AGGRESSIVE, DEFENSIVE

    // Input
    boolean upPressed = false;
    boolean downPressed = false;

    // Game state
    boolean paused = false;
    boolean running = true;
    private boolean gameStarted = false;

    // Enhanced Power-ups
    enum PowerType { PADDLE_BIG, BALL_SLOW, MULTI_BALL, SPEED_BOOST, MAGNET, CONFUSE_AI }
    class PowerUp {
        int x, y, size = 20;
        PowerType type;
        boolean active = true;
        float pulse = 0f;
        boolean growing = true;
        
        PowerUp(int x, int y, PowerType t) { 
            this.x = x; 
            this.y = y; 
            type = t; 
        }
        
        void update() {
            if (growing) {
                pulse += 0.05f;
                if (pulse >= 1f) growing = false;
            } else {
                pulse -= 0.05f;
                if (pulse <= 0f) growing = true;
            }
        }
    }
    PowerUp currentPower = null;
    final int POWER_SPAWN_INTERVAL = 10 * 1000; // ms
    long lastPowerSpawnTime = System.currentTimeMillis();

    // Timers for effects
    long paddleBoostEndTime = 0;
    long ballSlowEndTime = 0;
    long speedBoostEndTime = 0;
    long magnetEndTime = 0;
    long confuseAIEndTime = 0;

    // Multi-ball system
    class ExtraBall {
        int x, y;
        double vx, vy;
        boolean active = true;
        Color color;
        
        ExtraBall(int x, int y, double vx, double vy, Color color) {
            this.x = x; this.y = y;
            this.vx = vx; this.vy = vy;
            this.color = color;
        }
    }
    private List<ExtraBall> extraBalls = new ArrayList<>();
    
    // Particle effects
    class Particle {
        float x, y;
        float vx, vy;
        float life;
        Color color;
        float size;
        
        Particle(float x, float y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.vx = (float) (Math.random() - 0.5) * 8;
            this.vy = (float) (Math.random() - 0.5) * 8;
            this.life = 1.0f;
            this.size = (float) (Math.random() * 4 + 2);
        }
        
        boolean update() {
            x += vx;
            y += vy;
            life -= 0.02f;
            return life > 0;
        }
    }
    
    class TrailParticle {
        float x, y;
        float life;
        Color color;
        
        TrailParticle(float x, float y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.life = 1.0f;
        }
        
        boolean update() {
            life -= 0.05f;
            return life > 0;
        }
    }
    
    private List<Particle> particles = new ArrayList<>();
    
    // Special abilities
    private int playerAbilityCharge = 0;
    private final int MAX_ABILITY_CHARGE = 100;
    private boolean abilityActive = false;
    private long abilityEndTime = 0;

    // Visual effects
    private float screenShake = 0f;
    private Color backgroundColor1 = new Color(10, 10, 40);
    private Color backgroundColor2 = new Color(5, 5, 20);
    
    // Sound visualization
    private List<Float> soundBars = new ArrayList<>();
    private long lastSoundTime = 0;

    Random rnd = new Random();

    public EnhancedPongGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        timer = new Timer(DELAY, this);
        timer.start();
        resetBall(true);
        
        // Initialize sound bars
        for (int i = 0; i < 20; i++) {
            soundBars.add(0f);
        }
    }

    void resetBall(boolean toPlayerServe) {
        ballX = WIDTH/2 - ballSize/2;
        ballY = HEIGHT/2 - ballSize/2;
        double speed = 5;
        double angle = (rnd.nextDouble() * Math.PI/3) - Math.PI/6;
        ballVX = speed * (toPlayerServe ? -1 : 1) * Math.cos(angle);
        ballVY = speed * Math.sin(angle);
        
        // Clear trail
        ballTrail.clear();
    }

    void restartGame() {
        playerScore = 0;
        aiScore = 0;
        playerPaddleHeight = 100;
        aiPaddleHeight = 100;
        playerY = HEIGHT/2 - playerPaddleHeight/2;
        aiY = HEIGHT/2 - aiPaddleHeight/2;
        resetBall(rnd.nextBoolean());
        running = true;
        paused = false;
        gameStarted = false;
        playerAbilityCharge = 0;
        extraBalls.clear();
        particles.clear();
        setAIPersonality("NORMAL");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!running || paused) { 
            repaint(); 
            return; 
        }

        if (!gameStarted) {
            repaint();
            return;
        }

        // Update screen shake
        if (screenShake > 0) {
            screenShake *= 0.9f;
            if (screenShake < 0.1f) screenShake = 0;
        }

        // Update power-up animation
        if (currentPower != null) {
            currentPower.update();
        }

        // Player movement
        if (upPressed) playerY -= playerSpeed;
        if (downPressed) playerY += playerSpeed;
        playerY = Math.max(0, Math.min(HEIGHT - playerPaddleHeight, playerY));

        long now = System.currentTimeMillis();

        // Spawn power-up occasionally
        if (currentPower == null && now - lastPowerSpawnTime > POWER_SPAWN_INTERVAL) {
            int px = rnd.nextInt(WIDTH/2) + WIDTH/4;
            int py = rnd.nextInt(HEIGHT - 60) + 30;
            PowerType[] types = PowerType.values();
            PowerType type = types[rnd.nextInt(types.length)];
            currentPower = new PowerUp(px, py, type);
            lastPowerSpawnTime = now;
        }

        // Move ball with trail effect
        ballTrail.add(0, new TrailParticle(ballX + ballSize/2, ballY + ballSize/2, Color.WHITE));
        if (ballTrail.size() > 10) {
            ballTrail.remove(ballTrail.size() - 1);
        }
        
        // Update trail particles
        for (int i = 0; i < ballTrail.size(); i++) {
            if (!ballTrail.get(i).update()) {
                ballTrail.remove(i);
                i--;
            }
        }

        ballX += (int)Math.round(ballVX);
        ballY += (int)Math.round(ballVY);

        // Handle extra balls
        for (int i = 0; i < extraBalls.size(); i++) {
            ExtraBall eb = extraBalls.get(i);
            if (!eb.active) {
                extraBalls.remove(i);
                i--;
                continue;
            }
            
            eb.x += (int)Math.round(eb.vx);
            eb.y += (int)Math.round(eb.vy);
            
            // Collision with walls
            if (eb.y <= 0) { eb.y = 0; eb.vy = -eb.vy; createParticles(eb.x, 0, eb.color); }
            if (eb.y + ballSize >= HEIGHT) { eb.y = HEIGHT - ballSize; eb.vy = -eb.vy; createParticles(eb.x, HEIGHT - ballSize, eb.color); }
            
            // Collision with paddles
            if (eb.x <= PADDLE_X + PADDLE_WIDTH && eb.x + ballSize >= PADDLE_X) {
                if (eb.y + ballSize >= playerY && eb.y <= playerY + playerPaddleHeight) {
                    eb.x = PADDLE_X + PADDLE_WIDTH;
                    reflectExtraBallFromPaddle(eb, playerY, playerPaddleHeight, true);
                    createParticles(eb.x, eb.y, eb.color);
                }
            }
            if (eb.x + ballSize >= AI_X && eb.x <= AI_X + PADDLE_WIDTH) {
                if (eb.y + ballSize >= aiY && eb.y <= aiY + aiPaddleHeight) {
                    eb.x = AI_X - ballSize;
                    reflectExtraBallFromPaddle(eb, aiY, aiPaddleHeight, false);
                    createParticles(eb.x, eb.y, eb.color);
                }
            }
            
            // Score check
            if (eb.x + ballSize < 0 || eb.x > WIDTH) {
                eb.active = false;
                createParticles(eb.x, eb.y, eb.color);
            }
        }

        // Collide with top/bottom
        if (ballY <= 0) { 
            ballY = 0; 
            ballVY = -ballVY; 
            createParticles(ballX, 0, Color.CYAN);
            visualizeSound();
        }
        if (ballY + ballSize >= HEIGHT) { 
            ballY = HEIGHT - ballSize; 
            ballVY = -ballVY; 
            createParticles(ballX, HEIGHT - ballSize, Color.CYAN);
            visualizeSound();
        }

        // Collide with player paddle
        if (ballX <= PADDLE_X + PADDLE_WIDTH && ballX + ballSize >= PADDLE_X) {
            if (ballY + ballSize >= playerY && ballY <= playerY + playerPaddleHeight) {
                // hit
                ballX = PADDLE_X + PADDLE_WIDTH;
                reflectFromPaddle(playerY, playerPaddleHeight, true);
                createParticles(ballX, ballY, Color.GREEN);
                visualizeSound();
                
                // Charge ability on successful hit
                playerAbilityCharge = Math.min(MAX_ABILITY_CHARGE, playerAbilityCharge + 10);
            }
        }
        // Collide with AI paddle
        if (ballX + ballSize >= AI_X && ballX <= AI_X + PADDLE_WIDTH) {
            if (ballY + ballSize >= aiY && ballY <= aiY + aiPaddleHeight) {
                ballX = AI_X - ballSize;
                reflectFromPaddle(aiY, aiPaddleHeight, false);
                createParticles(ballX, ballY, Color.RED);
                visualizeSound();
            }
        }

        // Score check
        if (ballX + ballSize < 0) {
            aiScore++;
            createParticles(ballX, ballY, Color.RED);
            screenShake = 5f;
            resetBall(false);
            setAIPersonalityBasedOnScore();
        } else if (ballX > WIDTH) {
            playerScore++;
            createParticles(ballX, ballY, Color.GREEN);
            screenShake = 5f;
            resetBall(true);
            setAIPersonalityBasedOnScore();
        }

        // Adaptive AI difficulty
        adaptAIDifficulty();

        // Move AI paddle (with confusion effect)
        if (now < confuseAIEndTime) {
            // AI moves randomly when confused
            aiY += (rnd.nextInt(7) - 3);
        } else {
            double targetY = ballY + ballSize/2 - aiPaddleHeight/2;
            
            // Adjust target based on AI personality
            if ("AGGRESSIVE".equals(aiPersonality)) {
                targetY += (ballVY > 0 ? 20 : -20);
            } else if ("DEFENSIVE".equals(aiPersonality)) {
                targetY += (ballVY > 0 ? -10 : 10);
            }
            
            double dy = targetY - aiY;
            aiY += (int)Math.round(dy * aiReaction);
            
            // Clamp ai movement speed
            if (aiY - (int)Math.round(targetY) > aiMaxSpeed) aiY -= aiMaxSpeed;
            if ((int)Math.round(targetY) - aiY > aiMaxSpeed) aiY += aiMaxSpeed;
        }
        aiY = Math.max(0, Math.min(HEIGHT - aiPaddleHeight, aiY));

        // Magnet effect for player
        if (now < magnetEndTime) {
            double centerY = playerY + playerPaddleHeight/2;
            double distY = centerY - (ballY + ballSize/2);
            if (Math.abs(distY) < 100) {
                ballVY -= distY * 0.03;
            }
        }

        // Power-up collection
        if (currentPower != null && currentPower.active) {
            Rectangle powerRect = new Rectangle(currentPower.x, currentPower.y, currentPower.size, currentPower.size);
            Rectangle playerPaddleRect = new Rectangle(PADDLE_X, playerY, PADDLE_WIDTH, playerPaddleHeight);
            Rectangle aiPaddleRect = new Rectangle(AI_X, aiY, PADDLE_WIDTH, aiPaddleHeight);
            Rectangle ballRect = new Rectangle(ballX, ballY, ballSize, ballSize);

            if (powerRect.intersects(playerPaddleRect) || powerRect.intersects(ballRect)) {
                applyPowerToPlayer(currentPower.type);
                currentPower.active = false;
                currentPower = null;
                createParticles(powerRect.x, powerRect.y, Color.YELLOW);
            } else if (powerRect.intersects(aiPaddleRect)) {
                applyPowerToAI(currentPower.type);
                currentPower.active = false;
                currentPower = null;
                createParticles(powerRect.x, powerRect.y, Color.ORANGE);
            }
        }

        // Update particles
        for (int i = 0; i < particles.size(); i++) {
            if (!particles.get(i).update()) {
                particles.remove(i);
                i--;
            }
        }

        // Update sound visualization
        updateSoundBars();

        // Effects expiry
        if (now > paddleBoostEndTime) {
            playerPaddleHeight = 100;
            aiPaddleHeight = 100;
        }
        if (now > ballSlowEndTime) {
            normalizeBallSpeed();
        }
        if (now > speedBoostEndTime && now > ballSlowEndTime) {
            normalizeBallSpeed();
        }
        if (now > abilityEndTime) {
            abilityActive = false;
        }

        repaint();
    }

    void setAIPersonalityBasedOnScore() {
        int diff = playerScore - aiScore;
        if (diff >= 3) {
            setAIPersonality("AGGRESSIVE");
        } else if (diff <= -2) {
            setAIPersonality("DEFENSIVE");
        } else {
            setAIPersonality("NORMAL");
        }
    }

    void setAIPersonality(String personality) {
        this.aiPersonality = personality;
    }

    void adaptAIDifficulty() {
        int diff = aiScore - playerScore;
        if (playerScore > aiScore) {
            aiMaxSpeed = 4.5 + Math.min(3.0, (playerScore - aiScore) * 0.6);
            aiReaction = 0.12 + Math.min(0.25, (playerScore - aiScore) * 0.03);
        } else {
            aiMaxSpeed = Math.max(3.0, 4.5 - Math.min(2.0, (aiScore - playerScore) * 0.3));
            aiReaction = Math.max(0.08, 0.12 - Math.min(0.04, (aiScore - playerScore) * 0.01));
        }
    }

    void reflectFromPaddle(int paddleY, int paddleH, boolean wasPlayer) {
        double relativeIntersectY = (ballY + ballSize/2.0) - (paddleY + paddleH/2.0);
        double normalized = relativeIntersectY / (paddleH/2.0);
        double bounceAngle = normalized * Math.toRadians(60);
        double speed = Math.hypot(ballVX, ballVY);
        
        // Speed boost effect
        if (System.currentTimeMillis() < speedBoostEndTime) {
            speed = Math.min(15, speed + 1.0);
        } else {
            speed = Math.min(12, speed + 0.2);
        }

        double dir = wasPlayer ? 1 : -1;
        ballVX = dir * speed * Math.cos(bounceAngle);
        ballVY = speed * Math.sin(bounceAngle);
    }

    void reflectExtraBallFromPaddle(ExtraBall ball, int paddleY, int paddleH, boolean wasPlayer) {
        double relativeIntersectY = (ball.y + ballSize/2.0) - (paddleY + paddleH/2.0);
        double normalized = relativeIntersectY / (paddleH/2.0);
        double bounceAngle = normalized * Math.toRadians(60);
        double speed = Math.hypot(ball.vx, ball.vy);
        speed = Math.min(12, speed + 0.2);

        double dir = wasPlayer ? 1 : -1;
        ball.vx = dir * speed * Math.cos(bounceAngle);
        ball.vy = speed * Math.sin(bounceAngle);
    }

    void applyPowerToPlayer(PowerType t) {
        long now = System.currentTimeMillis();
        switch (t) {
            case PADDLE_BIG:
                playerPaddleHeight = 160;
                paddleBoostEndTime = now + 7_000;
                break;
            case BALL_SLOW:
                slowBall();
                ballSlowEndTime = now + 6_000;
                break;
            case MULTI_BALL:
                spawnExtraBalls(2);
                break;
            case SPEED_BOOST:
                speedBoostEndTime = now + 5_000;
                break;
            case MAGNET:
                magnetEndTime = now + 8_000;
                break;
            case CONFUSE_AI:
                confuseAIEndTime = now + 5_000;
                break;
        }
    }

    void applyPowerToAI(PowerType t) {
        long now = System.currentTimeMillis();
        switch (t) {
            case PADDLE_BIG:
                aiPaddleHeight = 160;
                paddleBoostEndTime = now + 7_000;
                break;
            case BALL_SLOW:
                slowBall();
                ballSlowEndTime = now + 6_000;
                break;
            case MULTI_BALL:
                spawnExtraBalls(2);
                break;
            case SPEED_BOOST:
                speedBoostEndTime = now + 5_000;
                break;
            case CONFUSE_AI:
                // AI using confuse doesn't make sense, so give it paddle boost instead
                aiPaddleHeight = 160;
                paddleBoostEndTime = now + 7_000;
                break;
        }
    }

    void spawnExtraBalls(int count) {
        for (int i = 0; i < count; i++) {
            double angle = (rnd.nextDouble() * Math.PI/2) + Math.PI/4;
            double speed = 6;
            Color color = new Color(
                rnd.nextInt(200) + 55,
                rnd.nextInt(200) + 55,
                rnd.nextInt(200) + 55
            );
            
            ExtraBall eb = new ExtraBall(
                ballX, ballY,
                speed * Math.cos(angle),
                speed * Math.sin(angle),
                color
            );
            extraBalls.add(eb);
        }
    }

    void activatePlayerAbility() {
        if (playerAbilityCharge >= MAX_ABILITY_CHARGE && !abilityActive) {
            abilityActive = true;
            playerAbilityCharge = 0;
            abilityEndTime = System.currentTimeMillis() + 3000;
            
            // Time slow ability
            ballSlowEndTime = System.currentTimeMillis() + 3000;
            slowBall();
            
            // Visual effect
            for (int i = 0; i < 50; i++) {
                particles.add(new Particle(
                    PADDLE_X + PADDLE_WIDTH/2, 
                    playerY + playerPaddleHeight/2,
                    Color.CYAN
                ));
            }
        }
    }

    void slowBall() {
        ballVX *= 0.55;
        ballVY *= 0.55;
        for (ExtraBall eb : extraBalls) {
            eb.vx *= 0.55;
            eb.vy *= 0.55;
        }
    }

    void normalizeBallSpeed() {
        double speed = Math.hypot(ballVX, ballVY);
        double target = System.currentTimeMillis() < speedBoostEndTime ? 8.0 : 5.5;
        target = Math.max(4.5, Math.min(12.0, target));
        if (speed == 0) speed = 1;
        double scale = target / speed;
        ballVX *= scale;
        ballVY *= scale;
        
        for (ExtraBall eb : extraBalls) {
            speed = Math.hypot(eb.vx, eb.vy);
            if (speed == 0) speed = 1;
            scale = target / speed;
            eb.vx *= scale;
            eb.vy *= scale;
        }
    }

    void createParticles(float x, float y, Color color) {
        for (int i = 0; i < 8; i++) {
            particles.add(new Particle(x, y, color));
        }
    }

    void visualizeSound() {
        lastSoundTime = System.currentTimeMillis();
        for (int i = 0; i < soundBars.size(); i++) {
            soundBars.set(i, 10f + (float) Math.random() * 5f);
        }
    }

    void updateSoundBars() {
        long now = System.currentTimeMillis();
        float decay = (now - lastSoundTime > 100) ? 0.9f : 1.0f;
        
        for (int i = 0; i < soundBars.size(); i++) {
            float value = soundBars.get(i) * decay;
            if (value < 1f) value = 0f;
            soundBars.set(i, value);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        
        // Apply screen shake
        if (screenShake > 0) {
            g2.translate(
                (Math.random() - 0.5) * screenShake,
                (Math.random() - 0.5) * screenShake
            );
        }

        // Enable anti-aliasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw gradient background
        GradientPaint gradient = new GradientPaint(0, 0, backgroundColor1, 0, HEIGHT, backgroundColor2);
        g2.setPaint(gradient);
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw stars in background
        g2.setColor(Color.WHITE);
        for (int i = 0; i < 50; i++) {
            int x = (int) (Math.random() * WIDTH);
            int y = (int) (Math.random() * HEIGHT);
            int size = (int) (Math.random() * 2) + 1;
            g2.fillOval(x, y, size, size);
        }

        // Draw middle line with glow
        g2.setColor(new Color(255, 255, 255, 100));
        for (int y = 0; y < HEIGHT; y += 30) {
            g2.fillRect(WIDTH/2 - 1, y, 2, 15);
        }

        // Draw particles
        for (Particle p : particles) {
            g2.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), (int)(p.life * 255)));
            g2.fillOval((int)p.x, (int)p.y, (int)p.size, (int)p.size);
        }

        // Draw ball trail
        for (int i = 0; i < ballTrail.size(); i++) {
            TrailParticle tp = ballTrail.get(i);
            int alpha = (int)(tp.life * 255 * (1 - i/(float)ballTrail.size()));
            int size = ballSize - i;
            if (size < 2) size = 2;
            g2.setColor(new Color(255, 255, 255, alpha));
            g2.fillOval((int)tp.x - size/2, (int)tp.y - size/2, size, size);
        }

        // Draw extra balls
        for (ExtraBall eb : extraBalls) {
            g2.setColor(eb.color);
            g2.fillOval(eb.x, eb.y, ballSize, ballSize);
            
            // Glow effect
            g2.setColor(new Color(eb.color.getRed(), eb.color.getGreen(), eb.color.getBlue(), 100));
            for (int i = 1; i <= 2; i++) {
                g2.fillOval(eb.x - i, eb.y - i, ballSize + i*2, ballSize + i*2);
            }
        }

        // Draw paddles with glow effects
        // Player paddle
        GradientPaint playerGradient = new GradientPaint(
            PADDLE_X, playerY, new Color(100, 255, 100),
            PADDLE_X, playerY + playerPaddleHeight, new Color(0, 200, 0)
        );
        g2.setPaint(playerGradient);
        g2.fillRoundRect(PADDLE_X, playerY, PADDLE_WIDTH, playerPaddleHeight, 10, 10);
        
        // AI paddle
        GradientPaint aiGradient = new GradientPaint(
            AI_X, aiY, new Color(255, 100, 100),
            AI_X, aiY + aiPaddleHeight, new Color(200, 0, 0)
        );
        g2.setPaint(aiGradient);
        g2.fillRoundRect(AI_X, aiY, PADDLE_WIDTH, aiPaddleHeight, 10, 10);

        // Draw ball with glow
        g2.setColor(Color.WHITE);
        g2.fillOval(ballX, ballY, ballSize, ballSize);
        g2.setColor(new Color(255, 255, 255, 100));
        g2.fillOval(ballX - 2, ballY - 2, ballSize + 4, ballSize + 4);

        // Draw power-up with pulsing effect
        if (currentPower != null && currentPower.active) {
            int pulseSize = (int)(currentPower.size * (1 + currentPower.pulse * 0.3));
            int offset = (pulseSize - currentPower.size) / 2;
            
            switch (currentPower.type) {
                case PADDLE_BIG:
                    g2.setColor(new Color(0, 255, 255, 200));
                    break;
                case BALL_SLOW:
                    g2.setColor(new Color(255, 200, 0, 200));
                    break;
                case MULTI_BALL:
                    g2.setColor(new Color(255, 0, 255, 200));
                    break;
                case SPEED_BOOST:
                    g2.setColor(new Color(255, 100, 100, 200));
                    break;
                case MAGNET:
                    g2.setColor(new Color(100, 255, 100, 200));
                    break;
                case CONFUSE_AI:
                    g2.setColor(new Color(255, 100, 255, 200));
                    break;
            }
            
            g2.fillRect(currentPower.x - offset, currentPower.y - offset, pulseSize, pulseSize);
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            String label = getPowerUpSymbol(currentPower.type);
            g2.drawString(label, currentPower.x + 6 - offset, currentPower.y + 14 - offset);
        }

        // Draw sound visualization
        g2.setColor(new Color(255, 255, 255, 100));
        int barWidth = 4;
        for (int i = 0; i < soundBars.size(); i++) {
            float height = soundBars.get(i);
            g2.fillRect(10 + i * (barWidth + 2), HEIGHT - 20 - (int)height, barWidth, (int)height);
        }

        // Draw scores with glow
        g2.setFont(new Font("Consolas", Font.BOLD, 48));
        
        // Player score
        g2.setColor(new Color(100, 255, 100, 150));
        g2.drawString(String.valueOf(playerScore), WIDTH/4 - 50, 70);
        g2.setColor(Color.GREEN);
        g2.drawString(String.valueOf(playerScore), WIDTH/4 - 50, 65);
        
        // AI score
        g2.setColor(new Color(255, 100, 100, 150));
        g2.drawString(String.valueOf(aiScore), WIDTH*3/4 - 20, 70);
        g2.setColor(Color.RED);
        g2.drawString(String.valueOf(aiScore), WIDTH*3/4 - 20, 65);

        // Draw ability charge bar
        if (playerAbilityCharge > 0) {
            int barWidthFull = 100;
            int chargedWidth = (int)(barWidthFull * (playerAbilityCharge / (float)MAX_ABILITY_CHARGE));
            
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(PADDLE_X, playerY - 15, barWidthFull, 8);
            g2.setColor(Color.CYAN);
            g2.fillRect(PADDLE_X, playerY - 15, chargedWidth, 8);
            
            if (playerAbilityCharge >= MAX_ABILITY_CHARGE) {
                g2.setColor(Color.YELLOW);
                g2.drawString("READY!", PADDLE_X, playerY - 20);
            }
        }

        // Draw AI personality indicator
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.setColor(Color.WHITE);
        g2.drawString("AI: " + aiPersonality, AI_X - 50, aiY - 10);

        // Draw effect timers
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.setColor(Color.WHITE);
        long now = System.currentTimeMillis();
        int yOffset = HEIGHT - 12;
        
        if (now < paddleBoostEndTime) {
            int secs = (int)((paddleBoostEndTime - now)/1000);
            g2.drawString("Paddle Boost: " + secs + "s", WIDTH - 170, yOffset);
            yOffset -= 15;
        }
        if (now < ballSlowEndTime) {
            int secs = (int)((ballSlowEndTime - now)/1000);
            g2.drawString("Ball Slow: " + secs + "s", WIDTH - 170, yOffset);
            yOffset -= 15;
        }
        if (now < speedBoostEndTime) {
            int secs = (int)((speedBoostEndTime - now)/1000);
            g2.drawString("Speed Boost: " + secs + "s", WIDTH - 170, yOffset);
            yOffset -= 15;
        }
        if (now < magnetEndTime) {
            int secs = (int)((magnetEndTime - now)/1000);
            g2.drawString("Magnet: " + secs + "s", WIDTH - 170, yOffset);
            yOffset -= 15;
        }
        if (now < confuseAIEndTime) {
            int secs = (int)((confuseAIEndTime - now)/1000);
            g2.drawString("AI Confused: " + secs + "s", WIDTH - 170, yOffset);
        }

        // Draw controls
        g2.drawString("W/S: Move  |  P: Pause  |  R: Restart  |  SPACE: Ability", 10, HEIGHT - 12);

        // Game state messages
        if (!gameStarted) {
            g2.setFont(new Font("Arial", Font.BOLD, 36));
            g2.setColor(Color.YELLOW);
            g2.drawString("Press ANY KEY to Start", WIDTH/2 - 180, HEIGHT/2 - 10);
        }
        
        if (!running) {
            g2.setFont(new Font("Arial", Font.BOLD, 48));
            g2.setColor(Color.YELLOW);
            g2.drawString("Game Over", WIDTH/2 - 140, HEIGHT/2 - 10);
        }
        if (paused) {
            g2.setFont(new Font("Arial", Font.BOLD, 48));
            g2.setColor(Color.YELLOW);
            g2.drawString("Paused", WIDTH/2 - 80, HEIGHT/2 - 10);
        }
    }

    private String getPowerUpSymbol(PowerType type) {
        switch (type) {
            case PADDLE_BIG: return "P";
            case BALL_SLOW: return "S";
            case MULTI_BALL: return "M";
            case SPEED_BOOST: return "B";
            case MAGNET: return "G";
            case CONFUSE_AI: return "C";
            default: return "?";
        }
    }

    // ------------ Input handling ------------
    @Override
    public void keyPressed(KeyEvent e) {
        int kc = e.getKeyCode();
        if (!gameStarted) {
            gameStarted = true;
            return;
        }
        
        if (kc == KeyEvent.VK_W) upPressed = true;
        if (kc == KeyEvent.VK_S) downPressed = true;
        if (kc == KeyEvent.VK_P) paused = !paused;
        if (kc == KeyEvent.VK_R) restartGame();
        if (kc == KeyEvent.VK_SPACE) activatePlayerAbility();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int kc = e.getKeyCode();
        if (kc == KeyEvent.VK_W) upPressed = false;
        if (kc == KeyEvent.VK_S) downPressed = false;
    }

    @Override public void keyTyped(KeyEvent e) {}

    // ------------ Main ------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Enhanced Pong - AI + Power-ups + Visual Effects");
            EnhancedPongGame game = new EnhancedPongGame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

}
