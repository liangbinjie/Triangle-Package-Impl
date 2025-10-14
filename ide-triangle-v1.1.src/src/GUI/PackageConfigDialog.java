package GUI;

import Triangle.PackageManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;

public class PackageConfigDialog extends JDialog {
    
    private DefaultListModel<String> pathListModel;
    private JList<String> pathList;
    private JButton btnAdd;
    private JButton btnRemove;
    private JButton btnClose;
    private JButton btnRescan;
    private JButton btnCreateDefault;
    
    private PackageManager packageManager;
    
    public PackageConfigDialog(Frame parent, boolean modal) {
        super(parent, "Package Configuration", modal);
        packageManager = PackageManager.getInstance();
        initComponents();
        loadPackagePaths();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(600, 450);
        setLocationRelativeTo(getParent());
        
        // Panel superior con título e info
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        JLabel titleLabel = new JLabel("Package Search Paths:");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        topPanel.add(titleLabel, BorderLayout.NORTH);
        
        JLabel infoLabel = new JLabel("<html><i>Add directories where compiled packages (.tam files) are stored</i></html>");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        topPanel.add(infoLabel, BorderLayout.SOUTH);
        
        add(topPanel, BorderLayout.NORTH);
        
        // Panel central con lista
        pathListModel = new DefaultListModel<>();
        pathList = new JList<>(pathListModel);
        pathList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pathList.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JScrollPane scrollPane = new JScrollPane(pathList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);
        
        // Panel derecho con botones
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));
        
        btnAdd = new JButton("Add Path...");
        btnAdd.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnAdd.setMaximumSize(new Dimension(150, 30));
        btnAdd.addActionListener(e -> addPath());
        
        btnRemove = new JButton("Remove");
        btnRemove.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRemove.setMaximumSize(new Dimension(150, 30));
        btnRemove.addActionListener(e -> removePath());
        
        btnCreateDefault = new JButton("Create Default");
        btnCreateDefault.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnCreateDefault.setMaximumSize(new Dimension(150, 30));
        btnCreateDefault.addActionListener(e -> createDefaultDirectory());
        btnCreateDefault.setToolTipText("Create default 'packages' directory");
        
        btnRescan = new JButton("Rescan");
        btnRescan.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRescan.setMaximumSize(new Dimension(150, 30));
        btnRescan.addActionListener(e -> rescanPackages());
        
        rightPanel.add(btnAdd);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        rightPanel.add(btnRemove);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        rightPanel.add(btnCreateDefault);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        rightPanel.add(btnRescan);
        rightPanel.add(Box.createVerticalGlue());
        
        add(rightPanel, BorderLayout.EAST);
        
        // Panel inferior con botón cerrar
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        btnClose = new JButton("Close");
        btnClose.addActionListener(e -> dispose());
        bottomPanel.add(btnClose);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void loadPackagePaths() {
        pathListModel.clear();
        List<String> paths = packageManager.getPackagePaths();
        for (String path : paths) {
            pathListModel.addElement(path);
        }
    }
    
    private void addPath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Package Directory");
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            packageManager.addPackagePath(path);
            loadPackagePaths();
            
            JOptionPane.showMessageDialog(this,
                "Path added successfully:\n" + path,
                "Path Added",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void removePath() {
        String selectedPath = pathList.getSelectedValue();
        if (selectedPath != null) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Remove path:\n" + selectedPath + "?",
                "Confirm Remove",
                JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                packageManager.removePackagePath(selectedPath);
                loadPackagePaths();
            }
        } else {
            JOptionPane.showMessageDialog(this,
                "Please select a path to remove",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private void createDefaultDirectory() {
        String defaultPath = packageManager.getDefaultPackageDirectory();
        File defaultDir = new File(defaultPath);
        
        if (defaultDir.exists()) {
            JOptionPane.showMessageDialog(this,
                "Default directory already exists:\n" + defaultPath,
                "Already Exists",
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            if (defaultDir.mkdirs()) {
                packageManager.addPackagePath(defaultPath);
                loadPackagePaths();
                JOptionPane.showMessageDialog(this,
                    "Default directory created successfully:\n" + defaultPath,
                    "Directory Created",
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Failed to create default directory:\n" + defaultPath,
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void rescanPackages() {
        packageManager.rescanAllPackages();
        List<String> availablePackages = packageManager.getAvailablePackages();
        
        StringBuilder message = new StringBuilder();
        message.append("Packages rescanned successfully.\n\n");
        message.append("Found ").append(availablePackages.size()).append(" package(s):\n");
        
        if (availablePackages.isEmpty()) {
            message.append("  (none)");
        } else {
            for (String pkg : availablePackages) {
                message.append("  - ").append(pkg).append("\n");
            }
        }
        
        JOptionPane.showMessageDialog(this,
            message.toString(),
            "Rescan Complete",
            JOptionPane.INFORMATION_MESSAGE);
    }
}