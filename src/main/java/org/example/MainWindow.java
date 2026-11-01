package org.example;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Modern retro styled GUI with Live Sorting and Sort old files tabs, activity table/log, Start/Stop toggle, Undo, and Toast notifications. */
public class MainWindow extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(MainWindow.class.getName());
    private final Config config;
    private final MoveHistory moveHistory;
    private final Extractor extractor;
    private final Orchestrator orchestrator;
    private final FileMover fileMover;
    private final BackfillSorter backfillSorter;
    private final ReviewQueueStore reviewQueueStore;

    private JTextArea logArea;
    private DefaultTableModel activityTableModel;
    private DefaultTableModel previewTableModel;
    private DefaultTableModel reviewTableModel;
    private JTable reviewTable;
    private RetroButton startStopButton;
    private RetroButton movePreviewButton;
    private JLabel statusLabel;
    private JPanel statusChip;
    private volatile boolean isRunning;
    private Thread watcherThread;
    private java.nio.file.WatchService watchService;
    private Timer ollamaPollTimer;
    private JWindow toastWindow;
    private final List<BackfillSorter.Preview> backfillPreview = new java.util.ArrayList<>();

    public MainWindow(Config config, MoveHistory moveHistory, Extractor extractor, Orchestrator orchestrator, FileMover fileMover) {
        this.config = config;
        this.moveHistory = moveHistory;
        this.extractor = extractor;
        this.orchestrator = orchestrator;
        this.fileMover = fileMover;
        this.backfillSorter = new BackfillSorter(config, extractor, orchestrator, fileMover);
        this.reviewQueueStore = new ReviewQueueStore();

        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ignored) {}

        if (config.hasWatchedFolderOverride()) {
            setTitle("TEST MODE: " + config.getWatchedFolderPath() + " - Universal File Sorter");
        } else {
            setTitle("Universal File Sorter");
        }
        setSize(980, 720);
        setMinimumSize(new Dimension(800, 560));
        setIconImages(AppTheme.createPumpkinImages());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopSorter();
                if (ollamaPollTimer != null) {
                    ollamaPollTimer.stop();
                }
            }
        });

        initComponents();
        loadHistoryTable();
        refreshReviewTable();
        startOllamaPollTimer();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // Header Banner
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(AppTheme.PANEL);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, AppTheme.INK),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePanel.setOpaque(false);

        JPanel pumpkinIconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                BufferedImage img = AppTheme.createPumpkinImage(36);
                g2.drawImage(img, 0, 0, null);
                g2.dispose();
            }
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(36, 36);
            }
        };
        pumpkinIconPanel.setOpaque(false);
        titlePanel.add(pumpkinIconPanel);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("File Sorter");
        titleLabel.setFont(Fonts.title());
        titleLabel.setForeground(AppTheme.CREAM);

        JLabel subtitleLabel = new JLabel("OFFLINE / PRIVATE / LOCAL AI");
        subtitleLabel.setFont(Fonts.label());
        subtitleLabel.setForeground(AppTheme.GHOST_BORDER);

        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(subtitleLabel);
        titlePanel.add(textPanel);

        JPanel headerRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        headerRightPanel.setOpaque(false);

        statusChip = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.WINDOW_BG);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
                g2.setColor(AppTheme.INK);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight()));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        statusChip.setOpaque(false);
        statusChip.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        statusLabel = new JLabel("Status: Idle");
        statusLabel.setFont(Fonts.ui().deriveFont(Font.BOLD, 12f));
        statusLabel.setForeground(AppTheme.CREAM);
        statusChip.add(statusLabel);

        startStopButton = new RetroButton("Start Sorter", RetroButton.Variant.PRIMARY);
        startStopButton.addActionListener(e -> toggleSorter());

        headerRightPanel.add(statusChip);
        headerRightPanel.add(startStopButton);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(headerRightPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Center Tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Tab 1: Live sorting
        JPanel liveTabPanel = new JPanel(new BorderLayout(8, 8));
        liveTabPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        liveTabPanel.setBackground(AppTheme.WINDOW_BG);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(Fonts.mono());
        logArea.setBackground(AppTheme.INK);
        logArea.setForeground(AppTheme.CREAM);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        logScroll.getViewport().setBackground(AppTheme.INK);

        RetroCard logCard = new RetroCard();
        logCard.setLayout(new BorderLayout(0, 8));
        logCard.add(new SectionLabel("Activity Log"), BorderLayout.NORTH);
        logCard.add(logScroll, BorderLayout.CENTER);

        String[] columns = {"Time", "File Name", "Category", "Source / Destination"};
        activityTableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable activityTable = new JTable(activityTableModel);
        activityTable.setAutoCreateRowSorter(true);
        activityTable.setRowHeight(32);
        activityTable.setShowGrid(false);
        activityTable.setIntercellSpacing(new Dimension(0, 0));
        activityTable.setBackground(AppTheme.PANEL);
        activityTable.setForeground(AppTheme.CREAM);
        activityTable.setSelectionBackground(AppTheme.AMBER);
        activityTable.setSelectionForeground(AppTheme.INK);
        activityTable.getTableHeader().setBackground(AppTheme.INK);
        activityTable.getTableHeader().setForeground(AppTheme.CREAM);
        activityTable.getTableHeader().setFont(Fonts.label());
        activityTable.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, AppTheme.INK));

        // Custom Category Chip Renderer
        DefaultTableCellRenderer categoryRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                String cat = value != null ? value.toString() : "Misc";
                JLabel label = new JLabel(cat, JLabel.CENTER) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(AppTheme.CREAM);
                        g2.fill(new RoundRectangle2D.Double(2, 4, getWidth() - 4, getHeight() - 8, 8, 8));
                        g2.setColor(AppTheme.INK);
                        g2.setStroke(new BasicStroke(2f));
                        g2.draw(new RoundRectangle2D.Double(2, 4, getWidth() - 5, getHeight() - 9, 8, 8));
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                label.setFont(Fonts.ui().deriveFont(Font.BOLD, 11f));
                label.setForeground(AppTheme.INK);
                label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
                JPanel wrapper = new JPanel(new GridBagLayout());
                wrapper.setOpaque(false);
                if (isSelected) {
                    wrapper.setBackground(table.getSelectionBackground());
                    wrapper.setOpaque(true);
                }
                wrapper.add(label);
                return wrapper;
            }
        };
        activityTable.getColumnModel().getColumn(2).setCellRenderer(categoryRenderer);

        // Cell renderers: Mono for file name and path, Sans for time
        DefaultTableCellRenderer monoCellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setFont(Fonts.mono());
                if (!isSelected) {
                    c.setForeground(AppTheme.CREAM);
                    c.setBackground(row % 2 == 0 ? AppTheme.PANEL : Color.decode("#35534D"));
                }
                return c;
            }
        };

        DefaultTableCellRenderer sansCellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setFont(Fonts.ui());
                if (!isSelected) {
                    c.setForeground(AppTheme.CREAM);
                    c.setBackground(row % 2 == 0 ? AppTheme.PANEL : Color.decode("#35534D"));
                }
                return c;
            }
        };

        activityTable.getColumnModel().getColumn(0).setCellRenderer(sansCellRenderer);
        activityTable.getColumnModel().getColumn(1).setCellRenderer(monoCellRenderer);
        activityTable.getColumnModel().getColumn(3).setCellRenderer(monoCellRenderer);

        JScrollPane tableScroll = new JScrollPane(activityTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(AppTheme.PANEL);

        RetroCard tableCard = new RetroCard();
        tableCard.setLayout(new BorderLayout(0, 8));
        tableCard.add(new SectionLabel("Sorted Files Activity"), BorderLayout.NORTH);
        tableCard.add(tableScroll, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableCard, logCard);
        splitPane.setResizeWeight(0.65);
        splitPane.setOpaque(false);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        liveTabPanel.add(splitPane, BorderLayout.CENTER);

        // Tab 2: Sort old files (Backfill & Preview)
        JPanel oldFilesPanel = new JPanel(new BorderLayout(8, 8));
        oldFilesPanel.setBackground(AppTheme.WINDOW_BG);
        oldFilesPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        RetroCard oldFilesTopCard = new RetroCard();
        oldFilesTopCard.setLayout(new FlowLayout(FlowLayout.LEFT, 16, 12));

        JLabel oldFilesInfo = new JLabel("Watched Folder: " + config.getWatchedFolderPath());
        oldFilesInfo.setFont(Fonts.ui().deriveFont(Font.BOLD, 13f));
        oldFilesInfo.setForeground(AppTheme.CREAM);

        JCheckBox allTimeCheckbox = new JCheckBox("All Time (No Date Filter)", true);
        allTimeCheckbox.setFont(Fonts.ui());
        allTimeCheckbox.setForeground(AppTheme.CREAM);
        allTimeCheckbox.setOpaque(false);

        RetroButton scanOldButton = new RetroButton("Scan & Preview", RetroButton.Variant.PRIMARY);
        movePreviewButton = new RetroButton("Move Previewed Files", RetroButton.Variant.PRIMARY);
        movePreviewButton.setEnabled(false);

        scanOldButton.addActionListener(e -> runBackfillPreview(scanOldButton, allTimeCheckbox.isSelected()));
        movePreviewButton.addActionListener(e -> executeBackfillMove(movePreviewButton));

        oldFilesTopCard.add(oldFilesInfo);
        oldFilesTopCard.add(allTimeCheckbox);
        oldFilesTopCard.add(scanOldButton);
        oldFilesTopCard.add(movePreviewButton);

        String[] previewColumns = {"File Name", "Proposed Category", "Absolute Path"};
        previewTableModel = new DefaultTableModel(previewColumns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable previewTable = new JTable(previewTableModel);
        previewTable.setAutoCreateRowSorter(true);
        previewTable.setRowHeight(30);
        previewTable.setShowGrid(false);
        previewTable.setIntercellSpacing(new Dimension(0, 0));
        previewTable.setBackground(AppTheme.PANEL);
        previewTable.setForeground(AppTheme.CREAM);
        previewTable.setSelectionBackground(AppTheme.AMBER);
        previewTable.setSelectionForeground(AppTheme.INK);
        previewTable.getTableHeader().setBackground(AppTheme.INK);
        previewTable.getTableHeader().setForeground(AppTheme.CREAM);
        previewTable.getTableHeader().setFont(Fonts.label());
        previewTable.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, AppTheme.INK));
        previewTable.getColumnModel().getColumn(1).setCellRenderer(categoryRenderer);
        previewTable.getColumnModel().getColumn(0).setCellRenderer(monoCellRenderer);
        previewTable.getColumnModel().getColumn(2).setCellRenderer(monoCellRenderer);

        JScrollPane previewScroll = new JScrollPane(previewTable);
        previewScroll.setBorder(BorderFactory.createEmptyBorder());
        previewScroll.getViewport().setBackground(AppTheme.PANEL);

        RetroCard previewCard = new RetroCard();
        previewCard.setLayout(new BorderLayout(0, 8));
        previewCard.add(new SectionLabel("Backfill Preview Files"), BorderLayout.NORTH);
        previewCard.add(previewScroll, BorderLayout.CENTER);

        oldFilesPanel.add(oldFilesTopCard, BorderLayout.NORTH);
        oldFilesPanel.add(previewCard, BorderLayout.CENTER);

        // Tab 3: Review Queue
        JPanel reviewTabPanel = new JPanel(new BorderLayout(8, 8));
        reviewTabPanel.setBackground(AppTheme.WINDOW_BG);
        reviewTabPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        RetroCard reviewTopCard = new RetroCard();
        reviewTopCard.setLayout(new BorderLayout(10, 8));

        JLabel reviewInfo = new JLabel("Files requiring manual review due to low confidence:");
        reviewInfo.setFont(Fonts.ui().deriveFont(Font.BOLD, 12f));
        reviewInfo.setForeground(AppTheme.CREAM);

        RetroButton approveButton = new RetroButton("Approve Selected", RetroButton.Variant.PRIMARY);
        RetroButton changeCategoryButton = new RetroButton("Change Category...", RetroButton.Variant.PRIMARY);
        RetroButton skipButton = new RetroButton("Skip / Remove", RetroButton.Variant.GHOST);
        RetroButton approveAllButton = new RetroButton("Approve All", RetroButton.Variant.PRIMARY);
        reviewInfo.setToolTipText("Items below the confidence threshold are held here for confirmation.");
        approveButton.setToolTipText("Move the selected file into its proposed category (Enter).");
        changeCategoryButton.setToolTipText("Choose a different category (double-click).");
        skipButton.setToolTipText("Remove the selected item without moving it (Delete).");
        approveAllButton.setToolTipText("Approve every item currently in the queue.");

        approveButton.addActionListener(e -> approveSelectedReviewItem());
        changeCategoryButton.addActionListener(e -> changeCategoryReviewItem());
        skipButton.addActionListener(e -> skipSelectedReviewItem());
        approveAllButton.addActionListener(e -> approveAllReviewItems());

        JPanel reviewToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        reviewToolbar.setOpaque(false);
        reviewToolbar.add(approveButton);
        reviewToolbar.add(changeCategoryButton);
        reviewToolbar.add(skipButton);
        reviewToolbar.add(approveAllButton);
        reviewTopCard.add(reviewInfo, BorderLayout.NORTH);
        reviewTopCard.add(reviewToolbar, BorderLayout.CENTER);

        String[] reviewColumns = {"File Name", "Proposed Category", "Confidence", "Summary"};
        reviewTableModel = new DefaultTableModel(reviewColumns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        reviewTable = new JTable(reviewTableModel);
        reviewTable.setAutoCreateRowSorter(true);
        reviewTable.setRowHeight(30);
        reviewTable.setShowGrid(false);
        reviewTable.setIntercellSpacing(new Dimension(0, 0));
        reviewTable.setBackground(AppTheme.PANEL);
        reviewTable.setForeground(AppTheme.CREAM);
        reviewTable.setSelectionBackground(AppTheme.AMBER);
        reviewTable.setSelectionForeground(AppTheme.INK);
        reviewTable.getTableHeader().setBackground(AppTheme.INK);
        reviewTable.getTableHeader().setForeground(AppTheme.CREAM);
        reviewTable.getTableHeader().setFont(Fonts.label());
        reviewTable.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, AppTheme.INK));
        reviewTable.getColumnModel().getColumn(1).setCellRenderer(categoryRenderer);
        reviewTable.getColumnModel().getColumn(0).setCellRenderer(monoCellRenderer);
        reviewTable.getColumnModel().getColumn(3).setCellRenderer(monoCellRenderer);

        JScrollPane reviewScroll = new JScrollPane(reviewTable);
        reviewScroll.setBorder(BorderFactory.createEmptyBorder());
        reviewScroll.getViewport().setBackground(AppTheme.PANEL);

        RetroCard reviewTableCard = new RetroCard();
        reviewTableCard.setLayout(new BorderLayout(0, 8));
        reviewTableCard.add(new SectionLabel("Review Queue"), BorderLayout.NORTH);
        reviewTableCard.add(reviewScroll, BorderLayout.CENTER);

        reviewTabPanel.add(reviewTopCard, BorderLayout.NORTH);
        reviewTabPanel.add(reviewTableCard, BorderLayout.CENTER);

        tabbedPane.addTab("Live sorting", liveTabPanel);
        tabbedPane.addTab("Sort old files", oldFilesPanel);
        tabbedPane.addTab("Review Queue", reviewTabPanel);

        // Controls Bottom Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(AppTheme.PANEL);
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0, AppTheme.INK),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonsPanel.setOpaque(false);

        RetroButton undoButton = new RetroButton("Undo Last Move", RetroButton.Variant.GHOST);
        undoButton.addActionListener(e -> undoMove());

        RetroButton historyButton = new RetroButton("History & Undo", RetroButton.Variant.GHOST);
        historyButton.addActionListener(e -> showHistoryDialog());

        RetroButton settingsButton = new RetroButton("Settings", RetroButton.Variant.GHOST);
        settingsButton.addActionListener(e -> showSettings());

        buttonsPanel.add(undoButton);
        buttonsPanel.add(historyButton);
        buttonsPanel.add(settingsButton);

        JLabel footerNote = new JLabel("Universal File Sorter v1.0");
        footerNote.setFont(Fonts.ui().deriveFont(Font.ITALIC, 11f));
        footerNote.setForeground(AppTheme.GHOST_BORDER);

        bottomBar.add(buttonsPanel, BorderLayout.WEST);
        bottomBar.add(footerNote, BorderLayout.EAST);

        add(tabbedPane, BorderLayout.CENTER);
        add(bottomBar, BorderLayout.SOUTH);
    }

    public void setStatus(String text) {
        Runnable update = () -> {
            statusLabel.setText(text);
            if (text.contains("Connected") || text.contains("Running")) {
                statusChip.setForeground(AppTheme.SUCCESS);
            } else if (text.contains("Offline") || text.contains("Error")) {
                statusChip.setForeground(AppTheme.CORAL);
            } else {
                statusChip.setForeground(AppTheme.AMBER);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            update.run();
        } else {
            SwingUtilities.invokeLater(update);
        }
    }

    private void startOllamaPollTimer() {
        ollamaPollTimer = new Timer(10000, e -> {
            CompletableFuture.supplyAsync(orchestrator::isAvailable)
                    .thenAccept(available -> {
                        String currentStatus = statusLabel.getText();
                        boolean isOffline = currentStatus.contains("Offline") || currentStatus.contains("Error");
                        if (available && isOffline) {
                            setStatus("Loading model...");
                            CompletableFuture.runAsync(() -> {
                                try {
                                    orchestrator.warmUp();
                                    setStatus("Status: Ollama Connected");
                                } catch (Exception ex) {
                                    LOGGER.log(java.util.logging.Level.SEVERE, "Ollama recovery warm-up failed", ex);
                                    setStatus("Ollama Error: " + ex.getMessage());
                                }
                            });
                        } else if (!available && !isOffline && !currentStatus.contains("Loading")) {
                            setStatus("Status: Offline (Rule-based)");
                        }
                    });
        });
        ollamaPollTimer.setInitialDelay(10000);
        ollamaPollTimer.start();
    }

    private void toggleSorter() {
        if (!isRunning) {
            startSorter();
        } else {
            stopSorter();
        }
    }

    private void startSorter() {
        isRunning = true;
        startStopButton.setText("Stop Sorter");
        statusLabel.setText("Status: Running");
        log("[INFO] Universal Sorter started. Watching: " + config.getWatchedFolderPath());

        watcherThread = new Thread(() -> {
            try {
                Path downloadsFolder = Path.of(config.getWatchedFolderPath()).toAbsolutePath().normalize();
                Files.createDirectories(downloadsFolder);
                watchService = downloadsFolder.getFileSystem().newWatchService();
                try (var service = watchService) {
                    downloadsFolder.register(service, java.nio.file.StandardWatchEventKinds.ENTRY_CREATE);
                    while (isRunning) {
                        var key = service.take();
                        for (var event : key.pollEvents()) {
                            if (event.kind() != java.nio.file.StandardWatchEventKinds.ENTRY_CREATE) continue;
                            Path path = downloadsFolder.resolve((Path) event.context());
                            if (FileStability.isIgnored(path)) continue;
                            if (!FileStability.waitUntilStable(path, config.getStabilityIntervalMs(), 300)) continue;
                            if (Files.isRegularFile(path)) {
                                processFile(path.toFile(), downloadsFolder);
                            }
                        }
                        if (!key.reset()) break;
                    }
                }
            } catch (InterruptedException ignored) {
            } catch (Exception e) {
                LOGGER.log(java.util.logging.Level.SEVERE, "Watcher error", e);
                log("[ERROR] Watcher error: " + e.getMessage());
            } finally {
                stopSorter();
            }
        }, "watcher-thread");
        watcherThread.start();
    }

    private void stopSorter() {
        isRunning = false;
        if (watcherThread != null) {
            watcherThread.interrupt();
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (Exception ignored) {
            }
        }
        SwingUtilities.invokeLater(() -> {
            startStopButton.setText("Start Sorter");
            statusLabel.setText("Status: Stopped");
            log("[STOP] Sorter stopped.");
        });
    }

    private void processFile(File file, Path downloadsFolder) {
        log("[FILE] New file detected: " + file.getName());
        try {
            String ruleCat = RuleEngine.evaluate(file);
            Classification classification;
            if (ruleCat != null) {
                classification = new Classification(ruleCat, 1.0, true);
            } else {
                String content = extractor.extractContent(file);
                try {
                    classification = orchestrator.classify(content, file.getName());
                } catch (Exception e) {
                    LOGGER.log(java.util.logging.Level.SEVERE, "Classification failed for file: " + file.getName(), e);
                    log("[WARN] Classification failed for " + file.getName() + ", falling back to Misc: " + e.getMessage());
                    classification = new Classification("Misc", 0.0, false);
                }
            }

            String category = CategorySanitizer.sanitize(classification.category(), config.getCategories());
            boolean needsReview = classification.confidence() < config.getConfidenceThreshold() && !classification.ruleBased();
            if (needsReview) {
                category = "Needs_Review";
                reviewQueueStore.add(new ReviewItem(file, category, classification.confidence(), classification.ruleBased(),
                        extractor.extractContent(file)));
                addLive(file, category);
                log("[REVIEW] " + file.getName() + " queued for review.");
                return;
            }

            Path targetDir = downloadsFolder.resolve(category).normalize();
            fileMover.move(file.toPath(), targetDir, category);
            addLive(file, category);
            log("[SORTED] " + file.getName() + " -> " + category);
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Error processing file: " + file.getName(), e);
            log("[ERROR] Error processing " + file.getName() + ": " + e.getMessage());
        }
    }

    public void addLive(File file, String category) {
        SwingUtilities.invokeLater(() -> {
            String time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .format(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
            activityTableModel.insertRow(0, new Object[]{time, file.getName(), category, file.getAbsolutePath()});
        });
    }

    private void undoMove() {
        try {
            MoveRecord record = moveHistory.undoLastMove();
            if (record == null) {
                log("[INFO] Nothing to undo.");
            } else {
                log("[UNDO] Restored: " + record.getFileName());
                loadHistoryTable();
            }
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Undo failed", e);
            log("[ERROR] Undo failed: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Undo failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runBackfillPreview(JButton scanButton, boolean allTime) {
        scanButton.setEnabled(false);
        log("[BACKFILL] Starting backfill scan & preview...");
        SwingWorker<List<BackfillSorter.Preview>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<BackfillSorter.Preview> doInBackground() throws Exception {
                Path folder = Path.of(config.getWatchedFolderPath());
                return backfillSorter.preview(folder, null, null, false);
            }

            @Override
            protected void done() {
                scanButton.setEnabled(true);
                try {
                    List<BackfillSorter.Preview> previews = get();
                    backfillPreview.clear();
                    backfillPreview.addAll(previews);
                    previewTableModel.setRowCount(0);
                    for (BackfillSorter.Preview p : previews) {
                        previewTableModel.addRow(new Object[]{p.file().getName(), p.category(), p.file().getAbsolutePath()});
                    }
                    movePreviewButton.setEnabled(!previews.isEmpty());
                    log("[BACKFILL] Scan complete. Preview rows: " + previews.size());
                    showToast("Scan complete: " + previews.size() + " files found.");
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Backfill scan error", e);
                    log("[ERROR] Backfill scan error: " + e.getMessage());
                    JOptionPane.showMessageDialog(MainWindow.this, "Backfill scan error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void executeBackfillMove(JButton moveButton) {
        moveButton.setEnabled(false);
        log("[BACKFILL] Moving " + backfillPreview.size() + " previewed files...");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                backfillSorter.movePreview(backfillPreview, (file, cat) -> addLive(file, cat));
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    int count = backfillPreview.size();
                    backfillPreview.clear();
                    previewTableModel.setRowCount(0);
                    loadHistoryTable();
                    log("[BACKFILL] Successfully moved " + count + " files.");
                    showToast(count + " files sorted successfully!");
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Backfill move error", e);
                    log("[ERROR] Backfill move error: " + e.getMessage());
                    JOptionPane.showMessageDialog(MainWindow.this, "Backfill move error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    moveButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void showToast(String message) {
        if (toastWindow != null && toastWindow.isVisible()) {
            toastWindow.dispose();
        }
        toastWindow = new JWindow(this);
        toastWindow.setAlwaysOnTop(true);

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PANEL);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(AppTheme.INK);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);

        JLabel iconLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.drawImage(AppTheme.createPumpkinImage(24), 0, 0, null);
                g2.dispose();
            }
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(24, 24);
            }
        };

        JLabel msgLabel = new JLabel(message);
        msgLabel.setFont(Fonts.ui().deriveFont(Font.BOLD, 12f));
        msgLabel.setForeground(AppTheme.CREAM);

        panel.add(iconLabel);
        panel.add(msgLabel);
        toastWindow.add(panel);
        toastWindow.pack();

        Point loc = getLocationOnScreen();
        int x = loc.x + getWidth() - toastWindow.getWidth() - 24;
        int y = loc.y + getHeight() - toastWindow.getHeight() - 24;
        toastWindow.setLocation(x, y);
        toastWindow.setVisible(true);

        Timer timer = new Timer(3000, e -> {
            if (toastWindow != null) {
                toastWindow.dispose();
                toastWindow = null;
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void loadHistoryTable() {
        activityTableModel.setRowCount(0);
        List<MoveRecord> records = moveHistory.getRecords();
        for (int i = records.size() - 1; i >= 0; i--) {
            MoveRecord r = records.get(i);
            String time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .format(LocalDateTime.ofInstant(Instant.ofEpochMilli(r.getTimestamp()), ZoneId.systemDefault()));
            activityTableModel.addRow(new Object[]{time, r.getFileName(), r.getCategory(), r.getTargetPath()});
        }
    }

    private void refreshReviewTable() {
        if (reviewTableModel == null) return;
        reviewTableModel.setRowCount(0);
        for (ReviewItem item : reviewQueueStore.getItems()) {
            reviewTableModel.addRow(new Object[]{item.getFileName(), item.getProposedCategory(),
                    String.format("%.0f%%", item.getConfidence() * 100), item.getExtractedSummary()});
        }
    }

    private ReviewItem selectedReviewItem() {
        if (reviewTable == null || reviewTable.getSelectedRow() < 0) return null;
        int viewRow = reviewTable.getSelectedRow();
        int modelRow = reviewTable.convertRowIndexToModel(viewRow);
        List<ReviewItem> items = reviewQueueStore.getItems();
        return modelRow >= 0 && modelRow < items.size() ? items.get(modelRow) : null;
    }

    private void approveSelectedReviewItem() {
        ReviewItem item = selectedReviewItem();
        if (item == null) {
            showToast("Select a review item first.");
            return;
        }
        approveReviewItem(item, item.getProposedCategory());
    }

    private void changeCategoryReviewItem() {
        ReviewItem item = selectedReviewItem();
        if (item == null) {
            showToast("Select a review item first.");
            return;
        }
        JComboBox<String> categories = new JComboBox<>(config.getCategories().toArray(new String[0]));
        categories.setSelectedItem(item.getProposedCategory());
        int result = JOptionPane.showConfirmDialog(this, categories, "Choose Category", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION && categories.getSelectedItem() != null) {
            approveReviewItem(item, categories.getSelectedItem().toString());
        }
    }

    private void approveReviewItem(ReviewItem item, String category) {
        try {
            File file = item.getFile();
            if (!file.isFile()) throw new java.io.IOException("File no longer exists: " + item.getFilePath());
            Path folder = Path.of(config.getWatchedFolderPath()).toAbsolutePath().normalize();
            fileMover.move(file.toPath(), folder.resolve(CategorySanitizer.sanitize(category, config.getCategories())), category);
            reviewQueueStore.remove(item);
            refreshReviewTable();
            addLive(file, category);
            log("[REVIEW] Approved " + item.getFileName() + " -> " + category);
        } catch (Exception e) {
            log("[ERROR] Review approval failed: " + e.getMessage());
            showToast("Approval failed: " + e.getMessage());
        }
    }

    private void skipSelectedReviewItem() {
        ReviewItem item = selectedReviewItem();
        if (item == null) {
            showToast("Select a review item first.");
            return;
        }
        reviewQueueStore.remove(item);
        refreshReviewTable();
        log("[REVIEW] Skipped " + item.getFileName());
    }

    private void approveAllReviewItems() {
        List<ReviewItem> items = reviewQueueStore.getItems();
        if (items.isEmpty()) {
            showToast("Review queue is empty.");
            return;
        }
        for (ReviewItem item : items) approveReviewItem(item, item.getProposedCategory());
        refreshReviewTable();
    }

    private void showHistoryDialog() {
        JDialog dialog = new JDialog(this, "History & Undo", true);
        dialog.setSize(820, 460);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.getContentPane().setBackground(AppTheme.WINDOW_BG);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"File", "Category", "Destination", "Time"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        List<MoveRecord> records = moveHistory.getRecords();
        for (int i = records.size() - 1; i >= 0; i--) {
            MoveRecord r = records.get(i);
            model.addRow(new Object[]{r.getFileName(), r.getCategory(), r.getTargetPath(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(r.getTimestamp()), ZoneId.systemDefault()))});
        }
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        RetroButton undo = new RetroButton("Undo Selected", RetroButton.Variant.PRIMARY);
        RetroButton close = new RetroButton("Close", RetroButton.Variant.GHOST);
        undo.addActionListener(e -> {
            int selected = table.getSelectedRow();
            if (selected < 0) return;
            int modelRow = table.convertRowIndexToModel(selected);
            int historyIndex = records.size() - 1 - modelRow;
            try {
                MoveRecord record = moveHistory.undoRecord(historyIndex);
                if (record != null) {
                    log("[UNDO] Restored: " + record.getFileName());
                    loadHistoryTable();
                    dialog.dispose();
                }
            } catch (Exception ex) {
                log("[ERROR] Undo failed: " + ex.getMessage());
                showToast("Undo failed: " + ex.getMessage());
            }
        });
        close.addActionListener(e -> dialog.dispose());
        actions.add(undo);
        actions.add(close);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showSettings() {
        JTextField folderField = new JTextField(config.getWatchedFolderPath(), 25);
        JComboBox<String> backendField = new JComboBox<>(new String[]{"ollama", "openai-compatible"});
        backendField.setSelectedItem(config.getAiBackend());
        JTextField urlField = new JTextField(config.getAiBaseUrl(), 25);
        JTextField modelField = new JTextField(config.getOllamaModel(), 15);
        JTextField apiKeyField = new JTextField(config.getAiApiKey(), 15);
        JCheckBox localNetworkField = new JCheckBox("Allow local network", config.isAllowLocalNetwork());
        JCheckBox disableAiField = new JCheckBox("Disable AI (rules-only)", config.isDisableAi());
        JTextField thresholdField = new JTextField(String.valueOf(config.getConfidenceThreshold()), 5);

        folderField.setFont(Fonts.ui());
        urlField.setFont(Fonts.ui());
        modelField.setFont(Fonts.ui());
        thresholdField.setFont(Fonts.ui());

        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.setBackground(AppTheme.PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel l1 = new JLabel("Watched Folder:"); l1.setForeground(AppTheme.CREAM); l1.setFont(Fonts.ui());
        JLabel l2 = new JLabel("Ollama URL:"); l2.setForeground(AppTheme.CREAM); l2.setFont(Fonts.ui());
        JLabel l3 = new JLabel("Ollama Model:"); l3.setForeground(AppTheme.CREAM); l3.setFont(Fonts.ui());
        JLabel l4 = new JLabel("Confidence Threshold:"); l4.setForeground(AppTheme.CREAM); l4.setFont(Fonts.ui());

        panel.add(l1); panel.add(folderField);
        panel.add(l2); panel.add(urlField);
        panel.add(l3); panel.add(modelField);
        panel.add(l4); panel.add(thresholdField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Sorter Settings", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                config.setWatchedFolderPath(folderField.getText().trim());
                config.setOllamaUrl(urlField.getText().trim());
                config.setOllamaModel(modelField.getText().trim());
                config.setConfidenceThreshold(Double.parseDouble(thresholdField.getText().trim()));
                config.save();
                log("[CONFIG] Settings updated successfully.");
            } catch (Exception e) {
                LOGGER.log(java.util.logging.Level.SEVERE, "Settings update failed", e);
                JOptionPane.showMessageDialog(this, "Invalid settings format: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void log(String msg) {
        if (SwingUtilities.isEventDispatchThread()) {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        } else {
            SwingUtilities.invokeLater(() -> {
                logArea.append(msg + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }
    }
}
