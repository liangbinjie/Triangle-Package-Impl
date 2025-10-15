/*
 * IDE-Triangle v1.0
 * Main.java
 */

package GUI;
import Core.Console.InputRedirector;
import Core.Console.OutputRedirector;
import Core.IDE.IDEDisassembler;
import Core.IDE.IDEInterpreter;
import Core.Visitors.TableVisitor;
// import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import javax.swing.ImageIcon;
import java.awt.Image;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import Triangle.IDECompiler;
import Core.ExampleFileFilter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import Core.Visitors.TreeVisitor;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * The Main class. Contains the main form.
 *
 * @author Luis Leopoldo P�rez <luiperpe@ns.isi.ulatina.ac.cr>
 */
public class Main extends javax.swing.JFrame {

    // <editor-fold defaultstate="collapsed" desc=" Methods ">
    
    /**
     * Creates new form Main.
     */
    public Main() {        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());            
        } catch (Exception e) { }
        
        initComponents();
        setSize(640, 480);
        setVisible(true);
        directory = new File(".");
    }
    
    /**
     * Checks if the file has been changed - used to enable/disable 
     * the "Save" button.
     */
    private void checkSaveChanges() {
        if (((FileFrame)desktopPane.getSelectedFrame()).hasChanged()) {
            buttonSave.setEnabled(true);
            saveMenuItem.setEnabled(true);
        }
        else {
            buttonSave.setEnabled(false);
            saveMenuItem.setEnabled(false);
        }
    }
    
    /**
     * Checks if there are any open documents. Disables some buttons and
     * menu options if no document is open. 
     */
    private void checkPaneChanges() {
        if (desktopPane.getComponentCount() == 0) {
            saveAsMenuItem.setEnabled(false);
            saveMenuItem.setEnabled(false);
            buttonSave.setEnabled(false);            
            buttonCut.setEnabled(false);
            buttonCopy.setEnabled(false);
            buttonPaste.setEnabled(false);            
            buttonCompile.setEnabled(false);
            buttonRun.setEnabled(false);
            cutMenuItem.setEnabled(false);
            copyMenuItem.setEnabled(false);
            pasteMenuItem.setEnabled(false);            
            compileMenuItem.setEnabled(false);
            runMenuItem.setEnabled(false);           
        } else
            checkSaveChanges();
    }
    
    /**
     * Creates a new document frame and inserts it into the desktop pane,
     * enabling its buttons and menu options.
     *
     * @param title Title of the frame.
     * @param contents File contents.
     * @return The new file frame.
     */
    private FileFrame addInternalFrame(String title, String contents) {
        FileFrame x = new FileFrame(delegateSaveButton, delegateMouse, delegateInternalFrame, delegateEnter);
        
        x.setTitle(title);
        x.setSize(540, 250);
        x.setSourcePaneText(contents);
        x.setPreviousSize(contents.length());
        x.setPreviousText(contents);
        desktopPane.add(x);        
        x.setVisible(true);
        
        saveAsMenuItem.setEnabled(true);        
        buttonCut.setEnabled(true);
        buttonCopy.setEnabled(true);
        buttonPaste.setEnabled(true);            
        cutMenuItem.setEnabled(true);
        copyMenuItem.setEnabled(true);
        pasteMenuItem.setEnabled(true);
        compileMenuItem.setEnabled(true);
        buttonCompile.setEnabled(true);
       
        checkSaveChanges();        
        return(x);
    }
    
    /**
     * Opens a file chooser dialog, used in the "Open" and "Save" options.
     * @return The JFileChooser dialog object.
     */
    private JFileChooser drawFileChooser() {
        JFileChooser chooser = new JFileChooser();
        ExampleFileFilter filter = new ExampleFileFilter();        
        filter.setDescription("Triangle files");
        filter.addExtension("tri");
        chooser.setFileFilter(filter);
        chooser.setCurrentDirectory(directory);
        
        return(chooser);
    }

    /**
     * Main method, instantiates the Main class.
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Main();
            }
        });
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        toolBarsPanel = new javax.swing.JPanel();
        fileToolBar = new javax.swing.JToolBar();
        buttonNew = new javax.swing.JButton();
        buttonOpen = new javax.swing.JButton();
        buttonSave = new javax.swing.JButton();
        editToolBar = new javax.swing.JToolBar();
        buttonCut = new javax.swing.JButton();
        buttonCopy = new javax.swing.JButton();
        buttonPaste = new javax.swing.JButton();
        triangleToolBar = new javax.swing.JToolBar();
        buttonCompile = new javax.swing.JButton();
        buttonRun = new javax.swing.JButton();
        desktopPane = new javax.swing.JDesktopPane();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        newMenuItem = new javax.swing.JMenuItem();
        openMenuItem = new javax.swing.JMenuItem();
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        separatorExit = new javax.swing.JSeparator();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        cutMenuItem = new javax.swing.JMenuItem();
        copyMenuItem = new javax.swing.JMenuItem();
        pasteMenuItem = new javax.swing.JMenuItem();
        triangleMenu = new javax.swing.JMenu();
        compileMenuItem = new javax.swing.JMenuItem();
        runMenuItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        aboutMenuItem = new javax.swing.JMenuItem();
        menuPackages = new javax.swing.JMenu();
        menuItemCompileAsPackage = new javax.swing.JMenuItem();
        menuItemLinkProgram = new javax.swing.JMenuItem();
        menuItemRunLinked = new javax.swing.JMenuItem();
        menuItemConfigurePackages = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("IDE-Triangle 1.1");
        setFont(new java.awt.Font("Tahoma", 0, 11));
        setIconImage(new ImageIcon(this.getClass().getResource("Icons/iconMain.gif")).getImage());
        setLocationByPlatform(true);
        setName("mainFrame");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        toolBarsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        toolBarsPanel.setFocusable(false);
        fileToolBar.setFocusable(false);
        fileToolBar.setName("File");
        fileToolBar.setRequestFocusEnabled(false);
        buttonNew.setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/Icons/iconFileNew.gif")));
        buttonNew.setToolTipText("New...");
        buttonNew.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 3, 3, 3));
        buttonNew.setBorderPainted(false);
        buttonNew.setFocusPainted(false);
        buttonNew.setFocusable(false);
        buttonNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newMenuItemActionPerformed(evt);
            }
        });

        fileToolBar.add(buttonNew);

        buttonOpen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/Icons/iconFileOpen.gif")));
        buttonOpen.setToolTipText("Open...");
        buttonOpen.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 3, 3, 3));
        buttonOpen.setBorderPainted(false);
        buttonOpen.setFocusPainted(false);
        buttonOpen.setFocusable(false);
        buttonOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMenuItemActionPerformed(evt);
            }
        });

        fileToolBar.add(buttonOpen);

        buttonSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/Icons/iconFileSave.gif")));
        buttonSave.setToolTipText("Save...");
        buttonSave.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 3, 3, 3));
        buttonSave.setBorderPainted(false);
        buttonSave.setEnabled(false);
        buttonSave.setFocusPainted(false);
        buttonSave.setFocusable(false);
        buttonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });

        fileToolBar.add(buttonSave);

        toolBarsPanel.add(fileToolBar);

        editToolBar.setFocusable(false);
        editToolBar.setName("Edit");
        editToolBar.setRequestFocusEnabled(false);
        buttonCut.setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/Icons/iconEditCut.gif")));
        buttonCut.setToolTipText("Cut...");
        buttonCut.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 3, 3, 3));
        buttonCut.setBorderPainted(false);
        buttonCut.setEnabled(false);
        buttonCut.setFocusPainted(false);
        buttonCut.setFocusable(false);
        buttonCut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cutMenuItemActionPerformed(evt);
            }
        });

        editToolBar.add(buttonCut);

        buttonCopy.setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/Icons/iconEditCopy.gif")));
        buttonCopy.setToolTipText("Copy...");
        buttonCopy.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 3, 3, 3));
        buttonCopy.setBorderPainted(false);
        buttonCopy.setEnabled(false);
        buttonCopy.setFocusPainted(false);
        buttonCopy.setFocusable(false);
        buttonCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyMenuItemActionPerformed(evt);
            }
        });

        editToolBar.add(buttonCopy);

        buttonPaste.setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/Icons/iconEditPaste.gif")));
        buttonPaste.setToolTipText("Paste...");
        buttonPaste.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 3, 3, 3));
        buttonPaste.setBorderPainted(false);
        buttonPaste.setEnabled(false);
        buttonPaste.setFocusPainted(false);
        buttonPaste.setFocusable(false);
        buttonPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pasteMenuItemActionPerformed(evt);
            }
        });

        editToolBar.add(buttonPaste);

        toolBarsPanel.add(editToolBar);

        triangleToolBar.setFocusable(false);
        triangleToolBar.setName("Triangle");
        triangleToolBar.setRequestFocusEnabled(false);
        buttonCompile.setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/Icons/iconTriangleCompile.gif")));
        buttonCompile.setToolTipText("Compile...");
        buttonCompile.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 3, 3, 3));
        buttonCompile.setBorderPainted(false);
        buttonCompile.setEnabled(false);
        buttonCompile.setFocusPainted(false);
        buttonCompile.setFocusable(false);
        buttonCompile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                compileMenuItemActionPerformed(evt);
            }
        });

        triangleToolBar.add(buttonCompile);

        buttonRun.setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/Icons/iconTriangleRun.gif")));
        buttonRun.setToolTipText("Run...");
        buttonRun.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 3, 3, 3));
        buttonRun.setBorderPainted(false);
        buttonRun.setEnabled(false);
        buttonRun.setFocusPainted(false);
        buttonRun.setFocusable(false);
        buttonRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runMenuItemActionPerformed(evt);
            }
        });

        triangleToolBar.add(buttonRun);

        toolBarsPanel.add(triangleToolBar);

        getContentPane().add(toolBarsPanel, java.awt.BorderLayout.NORTH);

        desktopPane.setBackground(new java.awt.Color(0, 103, 201));
        desktopPane.setAutoscrolls(true);
        getContentPane().add(desktopPane, java.awt.BorderLayout.CENTER);

        menuBar.setFont(new java.awt.Font("Verdana", 0, 11));
        fileMenu.setMnemonic('F');
        fileMenu.setText("File");
        fileMenu.setBorderPainted(true);
        newMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        newMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/Icons/iconFileNew.gif")));
        newMenuItem.setMnemonic('N');
        newMenuItem.setText("New");
        newMenuItem.setRequestFocusEnabled(false);
        newMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newMenuItemActionPerformed(evt);
            }
        });

        fileMenu.add(newMenuItem);

        openMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/Icons/iconFileOpen.gif")));
        openMenuItem.setMnemonic('O');
        openMenuItem.setText("Open");
        openMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMenuItemActionPerformed(evt);
            }
        });

        fileMenu.add(openMenuItem);

        saveMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        saveMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/Icons/iconFileSave.gif")));
        saveMenuItem.setMnemonic('S');
        saveMenuItem.setText("Save");
        saveMenuItem.setEnabled(false);
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });

        fileMenu.add(saveMenuItem);

        saveAsMenuItem.setMnemonic('A');
        saveAsMenuItem.setText("Save As...");
        saveAsMenuItem.setEnabled(false);
        saveAsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsMenuItemActionPerformed(evt);
            }
        });

        fileMenu.add(saveAsMenuItem);

        fileMenu.add(separatorExit);

        exitMenuItem.setMnemonic('x');
        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });

        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setMnemonic('E');
        editMenu.setText("Edit");
        editMenu.setBorderPainted(true);
        cutMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        cutMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/Icons/iconEditCut.gif")));
        cutMenuItem.setMnemonic('t');
        cutMenuItem.setText("Cut");
        cutMenuItem.setEnabled(false);
        cutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cutMenuItemActionPerformed(evt);
            }
        });

        editMenu.add(cutMenuItem);

        copyMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
        copyMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/Icons/iconEditCopy.gif")));
        copyMenuItem.setMnemonic('C');
        copyMenuItem.setText("Copy");
        copyMenuItem.setEnabled(false);
        copyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyMenuItemActionPerformed(evt);
            }
        });

        editMenu.add(copyMenuItem);

        pasteMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK));
        pasteMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/Icons/iconEditPaste.gif")));
        pasteMenuItem.setMnemonic('P');
        pasteMenuItem.setText("Paste");
        pasteMenuItem.setToolTipText("");
        pasteMenuItem.setEnabled(false);
        pasteMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pasteMenuItemActionPerformed(evt);
            }
        });

        editMenu.add(pasteMenuItem);

        menuBar.add(editMenu);

        triangleMenu.setMnemonic('T');
        triangleMenu.setText("Triangle");
        triangleMenu.setBorderPainted(true);
        compileMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0));
        compileMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/Icons/iconTriangleCompile.gif")));
        compileMenuItem.setMnemonic('C');
        compileMenuItem.setText("Compile");
        compileMenuItem.setEnabled(false);
        compileMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                compileMenuItemActionPerformed(evt);
            }
        });

        triangleMenu.add(compileMenuItem);

        runMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F6, 0));
        runMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/Icons/iconTriangleRun.gif")));
        runMenuItem.setMnemonic('R');
        runMenuItem.setText("Run");
        runMenuItem.setEnabled(false);
        runMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runMenuItemActionPerformed(evt);
            }
        });

        triangleMenu.add(runMenuItem);

        menuBar.add(triangleMenu);

        helpMenu.setMnemonic('H');
        helpMenu.setText("Help");
        helpMenu.setBorderPainted(true);
        aboutMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/Icons/iconHelpAbout.gif")));
        aboutMenuItem.setMnemonic('A');
        aboutMenuItem.setText("About");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });

        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        // AGREGAR DIRECTAMENTE AL menuBar EXISTENTE:
        menuPackages.setText("Packages");
        menuPackages.setMnemonic('P');

        menuItemCompileAsPackage.setText("Compile as Package");
        menuItemCompileAsPackage.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuItemCompileAsPackage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                compileAsPackage();
            }
        });
        menuPackages.add(menuItemCompileAsPackage);

        menuItemLinkProgram.setText("Link Program");
        menuItemLinkProgram.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuItemLinkProgram.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                linkProgram();
            }
        });
        menuPackages.add(menuItemLinkProgram);

        menuItemRunLinked.setText("Run Linked Program");
        menuItemRunLinked.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        menuItemRunLinked.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runLinkedProgram();
            }
        });
        menuPackages.add(menuItemRunLinked);

        menuPackages.addSeparator();

        menuItemConfigurePackages.setText("Configure Package Paths...");
        menuItemConfigurePackages.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                configurePackages();
            }
        });
        menuPackages.add(menuItemConfigurePackages);

        menuBar.add(menuPackages);  // Agregar a menuBar

        setJMenuBar(menuBar);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // <editor-fold defaultstate="collapsed" desc=" Event Handlers Implementation ">
    
    /**
     * Handles the "Run TAM Program" button and menu option.
     */
    private void runMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runMenuItemActionPerformed
        ((FileFrame)desktopPane.getSelectedFrame()).clearConsole();
        ((FileFrame)desktopPane.getSelectedFrame()).selectConsole();
        output.setDelegate(delegateConsole);
        runMenuItem.setEnabled(false);
        buttonRun.setEnabled(false);
        compileMenuItem.setEnabled(false);
        buttonCompile.setEnabled(false);
        interpreter.Run(desktopPane.getSelectedFrame().getTitle().replace(".tri", ".tam"));
    }//GEN-LAST:event_runMenuItemActionPerformed

    /** 
     * Handles the "Close" program option
     */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        while (desktopPane.getComponentCount() > 0) {
            try { 
                desktopPane.getSelectedFrame().setClosed(true);
            } catch (Exception e) { }
        }
    }//GEN-LAST:event_formWindowClosing

    /** 
     * Handles the "Paste Text" button and menu option.
     */
    private void pasteMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pasteMenuItemActionPerformed
        ((FileFrame)desktopPane.getSelectedFrame()).pasteText(Clip.getClipboardContents());
    }//GEN-LAST:event_pasteMenuItemActionPerformed

    /**
     * Handles the "Cut Text" button and menu option.
     */
    private void cutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cutMenuItemActionPerformed
        Clip.setClipboardContents(((FileFrame)desktopPane.getSelectedFrame()).getSelectedText());
        ((FileFrame)desktopPane.getSelectedFrame()).cutText();
    }//GEN-LAST:event_cutMenuItemActionPerformed

    /**
     * Handles the "Copy Text" button and menu option.
     */
    private void copyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyMenuItemActionPerformed
        Clip.setClipboardContents(((FileFrame)desktopPane.getSelectedFrame()).getSelectedText());
    }//GEN-LAST:event_copyMenuItemActionPerformed

    /** 
     * Handles the "Save As" button and menu option.
     */
    private void saveAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsMenuItemActionPerformed
        boolean _previouslySaved = ((FileFrame)desktopPane.getSelectedFrame()).getPreviouslySaved();        
        ((FileFrame)desktopPane.getSelectedFrame()).setPreviouslySaved(false);
        saveMenuItemActionPerformed(evt);
        ((FileFrame)desktopPane.getSelectedFrame()).setPreviouslySaved(_previouslySaved);        
    }//GEN-LAST:event_saveAsMenuItemActionPerformed

    /**
     * Handles the "About" menu option.
     */
    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        new AboutDialog(this, true).setVisible(true);
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    /**
     * Handles the "Open File" button and menu option.
     */
    private void openMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openMenuItemActionPerformed
        JFileChooser chooser = drawFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                directory = chooser.getCurrentDirectory();
                BufferedReader br = new BufferedReader(new FileReader(chooser.getSelectedFile()));
                String sr = "";
                while (br.ready())
                    sr += (char)br.read();
                br.close();
                addInternalFrame(chooser.getSelectedFile().getPath(), sr.replace("\r\n", "\n")).setPreviouslySaved(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "An error occurred while trying to open the specified file", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_openMenuItemActionPerformed

    /**
     * Handles the "Compile" button and menu option.
     */
    private void compileMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_compileMenuItemActionPerformed
        if ((!((FileFrame)desktopPane.getSelectedFrame()).getPreviouslySaved()) || ((FileFrame)desktopPane.getSelectedFrame()).hasChanged()) {
            saveMenuItemActionPerformed(null);
        }
        
        if (((FileFrame)desktopPane.getSelectedFrame()).getPreviouslySaved()) {
            ((FileFrame)desktopPane.getSelectedFrame()).selectConsole();
            ((FileFrame)desktopPane.getSelectedFrame()).clearConsole();
            ((FileFrame)desktopPane.getSelectedFrame()).clearTAMCode();
            ((FileFrame)desktopPane.getSelectedFrame()).clearTree();
            ((FileFrame)desktopPane.getSelectedFrame()).clearTable();
            
            String sourceFile = desktopPane.getSelectedFrame().getTitle();
            String objectFile = sourceFile.replace(".tri", ".tam");
            new File(objectFile).delete();
            
            output.setDelegate(delegateConsole);            
            if (compiler.compileProgram(sourceFile, objectFile, false, false)) {           
                output.setDelegate(delegateTAMCode);
                disassembler.Disassemble(objectFile);
                
                // Comentar estas líneas si getAST() no existe aún
                // ((FileFrame)desktopPane.getSelectedFrame()).setTree((DefaultMutableTreeNode)treeVisitor.visitProgram(compiler.getAST(), null));
                // ((FileFrame)desktopPane.getSelectedFrame()).setTable(tableVisitor.getTable(compiler.getAST()));
                
                runMenuItem.setEnabled(true);
                buttonRun.setEnabled(true);
            } else {
                // Comentar esta línea si getErrorPosition() no existe aún
                // ((FileFrame)desktopPane.getSelectedFrame()).highlightError(compiler.getErrorPosition());
                runMenuItem.setEnabled(false);
                buttonRun.setEnabled(false);
            }
        }
    }//GEN-LAST:event_compileMenuItemActionPerformed

    /**
     * Handles the "Save" button and menu option.
     */
    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed
        String fileName = ((FileFrame)desktopPane.getSelectedFrame()).getTitle();
        boolean overwrite = true;
        
        if (!(((FileFrame)desktopPane.getSelectedFrame()).getPreviouslySaved())) {
            JFileChooser chooser = drawFileChooser();
            
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                if (chooser.getSelectedFile().exists()) {
                    overwrite = (JOptionPane.showConfirmDialog(this, chooser.getSelectedFile().getName() + " already exists.\nWould you like to replace it?", "Overwrite?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION);
                }
                
                directory = chooser.getCurrentDirectory();
                
                fileName = chooser.getSelectedFile().getPath();
                if (!(fileName.contains(".tri")))
                    fileName += ".tri";
            } else
                overwrite = false;
        }
 
        if (overwrite) {
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
                bw.write(((FileFrame)desktopPane.getSelectedFrame()).getSourcePaneText().replace("\n", "\r\n"));
                bw.close();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "An error occurred while trying to save the specified file", "Error", JOptionPane.ERROR_MESSAGE);
            }

            ((FileFrame)desktopPane.getSelectedFrame()).setPreviouslySaved(true);
            ((FileFrame)desktopPane.getSelectedFrame()).setTitle(fileName);            
            ((FileFrame)desktopPane.getSelectedFrame()).setPreviouslyModified(false);
            ((FileFrame)desktopPane.getSelectedFrame()).setPreviousSize(((FileFrame)desktopPane.getSelectedFrame()).getSourcePaneText().length());
            ((FileFrame)desktopPane.getSelectedFrame()).setPreviousText(((FileFrame)desktopPane.getSelectedFrame()).getSourcePaneText());
                
            saveMenuItem.setEnabled(false);
            buttonSave.setEnabled(false);
        }
    }//GEN-LAST:event_saveMenuItemActionPerformed

    /**
     * Handles the "New File" button and menu option.
     */
    private void newMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newMenuItemActionPerformed
        addInternalFrame("Untitled-" + String.valueOf(untitledCount), "");      
        untitledCount++;
    }//GEN-LAST:event_newMenuItemActionPerformed

    /**
     * Handles the "Exit" menu option.
     */
    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        formWindowClosing(null);
        System.exit(0);
    }//GEN-LAST:event_exitMenuItemActionPerformed

    // </editor-fold>    
           
    // <editor-fold defaultstate="collapsed" desc=" Delegates and Listeners ">    
    /**
     * Runs every time a key is pressed in the text editor frame, thus
     * determining if the file contents have changed, activating the
     * "Save" button and menu option.
     */   
    KeyAdapter delegateSaveButton = new KeyAdapter() {
        public void keyReleased(java.awt.event.KeyEvent evt) {
             checkSaveChanges();
             ((FileFrame)desktopPane.getSelectedFrame()).UpdateRowColNumbers();
        }
    };
    
    MouseListener delegateMouse = new MouseListener() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            ((FileFrame)desktopPane.getSelectedFrame()).UpdateRowColNumbers();
        }
        
        public void mouseExited(java.awt.event.MouseEvent evt) {
        }

        public void mouseEntered(java.awt.event.MouseEvent evt) {
        }        
        
        public void mouseReleased(java.awt.event.MouseEvent evt) {
        }       

        public void mousePressed(java.awt.event.MouseEvent evt) {
        }        
    };
    
    /**
     * Several events for the MDI text editor frames. 
     */
    InternalFrameListener delegateInternalFrame = new InternalFrameListener() {        
        
        /**
         * Every time a frame is focused/activated, some buttons can be
         * enabled (e.g. "Save", "Cut", "Copy", "Paste").
         */
        public void internalFrameActivated(InternalFrameEvent evt) { 
            checkPaneChanges();
            ((FileFrame)desktopPane.getSelectedFrame()).UpdateRowColNumbers();
        }
        
        /**
         * Every time a frame is closed, some buttons can be
         * disabled (e.g. "Save", "Cut", "Copy", "Paste").
         */        
        public void internalFrameClosed(InternalFrameEvent evt) {
            checkPaneChanges();
        }
            
        /**
         * Before closing a frame, checks if the file has not been
         * saved yet.
         */
        public void internalFrameClosing(InternalFrameEvent evt) {
            if (((FileFrame)desktopPane.getSelectedFrame()).hasChanged()) { 
                if (JOptionPane.showConfirmDialog(null, "Do you want to save the changes to " + desktopPane.getSelectedFrame().getTitle() + "?", "Save", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)    
                    saveMenuItemActionPerformed(null);    
            }
        }
        
        // Required by interface - not implemented
        public void internalFrameDeactivated(InternalFrameEvent evt) { }
        public void internalFrameDeiconified(InternalFrameEvent evt) { }
        public void internalFrameIconified(InternalFrameEvent evt) { }
        public void internalFrameOpened(InternalFrameEvent evt) { }
    };
    
    
    /**
     * Used to redirect the console output - writes in the "Console" panel.     
     */
    ActionListener delegateConsole = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            while (output.peekQueue())
                ((FileFrame)desktopPane.getSelectedFrame()).writeToConsole(output.readQueue());
        }
    };
    
    /**
     * Used to redirect the console output - writes in the "TAM Code" pane.
     */
    ActionListener delegateTAMCode = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            while (output.peekQueue())
                ((FileFrame)desktopPane.getSelectedFrame()).writeToTAMCode(output.readQueue());
        }        
    };
    
    /**
     * Used to redirect the console input - enables the "Console Input" text box.
     */
    ActionListener delegateInput = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            ((FileFrame)desktopPane.getSelectedFrame()).setInputEnabled(true);
        }
    };
    
    /**
     * Used to redirect the console input - writes the user input in the console.
     */
    ActionListener delegateEnter = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            ((FileFrame)desktopPane.getSelectedFrame()).setInputEnabled(false);
            input.addInput(((FileFrame)desktopPane.getSelectedFrame()).getInputFieldText() + "\n");
            ((FileFrame)desktopPane.getSelectedFrame()).clearInputField();
        }
    };
    
    /**
     * Used to control running programs - only one TAM program can be run at once
     */
    ActionListener delegateRun = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            runMenuItem.setEnabled(true);
            buttonRun.setEnabled(true);
            compileMenuItem.setEnabled(true);
            buttonCompile.setEnabled(true);
        }
    };
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc=" GUI Variables ">
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JButton buttonCompile;
    private javax.swing.JButton buttonCopy;
    private javax.swing.JButton buttonCut;
    private javax.swing.JButton buttonNew;
    private javax.swing.JButton buttonOpen;
    private javax.swing.JButton buttonPaste;
    private javax.swing.JButton buttonRun;
    private javax.swing.JButton buttonSave;
    private javax.swing.JMenuItem compileMenuItem;
    private javax.swing.JMenuItem copyMenuItem;
    private javax.swing.JMenuItem cutMenuItem;
    private javax.swing.JDesktopPane desktopPane;
    private javax.swing.JMenu editMenu;
    private javax.swing.JToolBar editToolBar;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JToolBar fileToolBar;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem newMenuItem;
    private javax.swing.JMenuItem openMenuItem;
    private javax.swing.JMenuItem pasteMenuItem;
    private javax.swing.JMenuItem runMenuItem;
    private javax.swing.JMenuItem saveAsMenuItem;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JSeparator separatorExit;
    private javax.swing.JPanel toolBarsPanel;
    private javax.swing.JMenu triangleMenu;
    private javax.swing.JToolBar triangleToolBar;
    
    // Agregar estas líneas ANTES de // End of variables declaration
    private javax.swing.JMenu menuPackages;
    private javax.swing.JMenuItem menuItemCompileAsPackage;
    private javax.swing.JMenuItem menuItemLinkProgram;
    private javax.swing.JMenuItem menuItemRunLinked;
    private javax.swing.JMenuItem menuItemConfigurePackages;
    // End of variables declaration//GEN-END:variables
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc=" Non-GUI Variables ">
    // [ Non-GUI variables declaration ]
    int untitledCount = 1;                                                  // Counts "Untitled" document names (e.g. "Untitled-1")
    clipBoard Clip = new clipBoard();                                       // Clipboard Management
    IDECompiler compiler = new IDECompiler();                               // Compiler - Analyzes/generates TAM programs
    IDEDisassembler disassembler = new IDEDisassembler();                   // Disassembler - Generates TAM Code
    IDEInterpreter interpreter = new IDEInterpreter(delegateRun);           // Interpreter - Runs TAM programs
    OutputRedirector output = new OutputRedirector();                       // Redirects the console output
    InputRedirector input = new InputRedirector(delegateInput);             // Redirects console input
    TreeVisitor treeVisitor = new TreeVisitor();                            // Draws the Abstract Syntax Trees
    TableVisitor tableVisitor = new TableVisitor();                         // Draws the Identifier Table
    File directory;                                                         // The current directory.
    // [ End of Non-GUI variables declaration ]
    // </editor-fold>    
    
    // <editor-fold defaultstate="collapsed" desc=" Internal Class - ClipboardOwner ">
    /**
     * ClipboardOwner Class
     * Internal clipboard management class, uses the default system clipboard.
     */
    private class clipBoard implements ClipboardOwner {
        
        /**
         * Required by interface - not implemented
         */
        public void lostOwnership(Clipboard aClipboard, Transferable aContents) { }
        
        /**
         * Sets the clipboard contents
         */
        public void setClipboardContents(String _contents) {
            StringSelection stringSelection = new StringSelection(_contents);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, this);
        }
        
        /**
         * Returns the clipboard contents
         */
        public String getClipboardContents() {
            String ret = "";
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable contents = clipboard.getContents(null);
            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    DataFlavor flavorSet[] = contents.getTransferDataFlavors();
                    boolean canString = false;
                    for (int i=0;i<flavorSet.length;i++)
                        if (flavorSet[i] == DataFlavor.stringFlavor)
                            canString = true;
                    
                    ret = (String)contents.getTransferData(DataFlavor.stringFlavor);
                } catch (Exception e) { }
            }
            return(ret);
        }        
    }
    
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc=" Package Methods ">

/**
 * Compila el archivo actual como paquete
 */
private void compileAsPackage() {
    FileFrame currentFrame = (FileFrame)desktopPane.getSelectedFrame();
    
    if (currentFrame == null) {
        javax.swing.JOptionPane.showMessageDialog(this,
            "No file is currently open",
            "Compile Error",
            javax.swing.JOptionPane.ERROR_MESSAGE);
        return;
    }
    
    // Usar getTitle() en lugar de getFilePath()
    String sourcePath = currentFrame.getTitle();
    
    // Verificar si el archivo ha sido guardado
    if (!currentFrame.getPreviouslySaved()) {
        javax.swing.JOptionPane.showMessageDialog(this,
            "Please save the file first",
            "Compile Error",
            javax.swing.JOptionPane.ERROR_MESSAGE);
        return;
    }
    
    // Si el archivo tiene cambios, guardarlo primero
    if (currentFrame.hasChanged()) {
        saveMenuItemActionPerformed(null);
    }
    
    // Seleccionar directorio de salida
    javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
    chooser.setDialogTitle("Select Package Output Directory");
    chooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
    
    // Obtener directorio de paquetes por defecto y asegurarse de que existe
    Triangle.PackageManager pm = Triangle.PackageManager.getInstance();
    String packagesDir = pm.getDefaultPackageDirectory();
    java.io.File packagesDirFile = new java.io.File(packagesDir);
    
    // Crear el directorio si no existe
    if (!packagesDirFile.exists()) {
        packagesDirFile.mkdirs();
        System.out.println("Created default packages directory: " + packagesDir);
    }
    
    chooser.setCurrentDirectory(packagesDirFile);
    
    int result = chooser.showSaveDialog(this);
    if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
        String outputDir = chooser.getSelectedFile().getAbsolutePath();
        
        System.out.println("=== Compiling as Package ===");
        System.out.println("Source: " + sourcePath);
        System.out.println("Output Dir: " + outputDir);
        
        // Limpiar consola y mostrarla
        currentFrame.clearConsole();
        currentFrame.selectConsole();
        output.setDelegate(delegateConsole);
        
        Triangle.IDECompiler packageCompiler = new Triangle.IDECompiler();
        boolean success = packageCompiler.compilePackage(sourcePath, outputDir);
        
        if (success) {
            // Verificar que los archivos se crearon
            java.io.File outDirFile = new java.io.File(outputDir);
            String packageName = new java.io.File(sourcePath).getName().replaceAll("\\.tri$", "");
            
            java.io.File tamFile = new java.io.File(outDirFile, packageName + ".tam");
            java.io.File mapFile = new java.io.File(outDirFile, packageName + ".map");
            
            StringBuilder message = new StringBuilder();
            message.append("Package compiled successfully!\n\n");
            message.append("Output directory: ").append(outputDir).append("\n\n");
            message.append("Files generated:\n");
            
            if (tamFile.exists()) {
                message.append("✓ ").append(packageName).append(".tam\n");
            } else {
                message.append("✗ ").append(packageName).append(".tam (MISSING!)\n");
            }
            
            if (mapFile.exists()) {
                message.append("✓ ").append(packageName).append(".map\n");
            } else {
                message.append("✗ ").append(packageName).append(".map (MISSING!)\n");
            }
            
            javax.swing.JOptionPane.showMessageDialog(this,
                message.toString(),
                "Compilation Successful",
                javax.swing.JOptionPane.INFORMATION_MESSAGE);
            
            // Rescan packages
            pm.rescanAllPackages();
            
            System.out.println("=== Compilation Complete ===");
        } else {
            javax.swing.JOptionPane.showMessageDialog(this,
                "Package compilation failed.\nCheck console for errors.",
                "Compilation Failed",
                javax.swing.JOptionPane.ERROR_MESSAGE);
                
            System.err.println("=== Compilation Failed ===");
        }
    }
}

/**
 * Linkea el programa actual con sus dependencias
 */
private void linkProgram() {
    FileFrame currentFrame = (FileFrame)desktopPane.getSelectedFrame();
    
    if (currentFrame == null) {
        javax.swing.JOptionPane.showMessageDialog(this,
            "No file is currently open",
            "Link Error",
            javax.swing.JOptionPane.ERROR_MESSAGE);
        return;
    }
    
    // Usar getTitle() en lugar de getFilePath()
    String sourcePath = currentFrame.getTitle();
    
    // Verificar si el archivo ha sido guardado
    if (!currentFrame.getPreviouslySaved()) {
        javax.swing.JOptionPane.showMessageDialog(this,
            "Please save the file first",
            "Link Error",
            javax.swing.JOptionPane.ERROR_MESSAGE);
        return;
    }
    
    // Si el archivo tiene cambios, guardarlo primero
    if (currentFrame.hasChanged()) {
        saveMenuItemActionPerformed(null);
    }
    
    // Determinar directorio base
    java.io.File sourceFile = new java.io.File(sourcePath);
    String baseDir = sourceFile.getParent();
    if (baseDir == null) baseDir = ".";
    
    String objPath = baseDir + java.io.File.separator + "obj.tam";
    String importPath = baseDir + java.io.File.separator + "program.imp";
    String relocPath = baseDir + java.io.File.separator + "program.reloc";
    String linkedPath = baseDir + java.io.File.separator + "linked.tam";
    
    System.out.println("=== Linking Program ===");
    System.out.println("Source: " + sourcePath);
    System.out.println("Base Dir: " + baseDir);
    
    // Limpiar consola y mostrarla
    currentFrame.clearConsole();
    currentFrame.selectConsole();
    output.setDelegate(delegateConsole);
    
    // Primero compilar el programa
    Triangle.IDECompiler linkCompiler = new Triangle.IDECompiler();
    boolean compiled = linkCompiler.compileProgram(sourcePath, objPath, false, false);
    
    if (!compiled) {
        javax.swing.JOptionPane.showMessageDialog(this,
            "Compilation failed. Cannot link.\nCheck console for errors.",
            "Link Error",
            javax.swing.JOptionPane.ERROR_MESSAGE);
        System.err.println("=== Compilation Failed ===");
        return;
    }
    
    // Verificar que obj.tam se creó
    java.io.File objFile = new java.io.File(objPath);
    if (!objFile.exists()) {
        javax.swing.JOptionPane.showMessageDialog(this,
            "obj.tam was not created.\nCompilation may have failed.",
            "Link Error",
            javax.swing.JOptionPane.ERROR_MESSAGE);
        System.err.println("obj.tam not found at: " + objPath);
        return;
    }
    
    System.out.println("obj.tam created successfully: " + objPath);
    
    // Linkear - CORRECCIÓN: Pasar los 4 parámetros requeridos
    Triangle.IDELinker linker = new Triangle.IDELinker();
    
    // Agregar ruta de paquetes al linker
    Triangle.PackageManager pm = Triangle.PackageManager.getInstance();
    for (String path : pm.getPackagePaths()) {
        linker.addPackagePath(path);
    }
    
    // CORRECCIÓN: Llamar con los 4 parámetros: objectFile, importFile, relocFile, outputFile
    boolean linked = linker.link(objPath, importPath, relocPath, linkedPath);
    
    if (linked) {
        javax.swing.JOptionPane.showMessageDialog(this,
            "Program linked successfully!\n\n" +
            "Output: " + linkedPath,
            "Linking Successful",
            javax.swing.JOptionPane.INFORMATION_MESSAGE);
        System.out.println("=== Linking Complete ===");
    } else {
        javax.swing.JOptionPane.showMessageDialog(this,
            "Linking failed.\nCheck console for errors.",
            "Linking Failed",
            javax.swing.JOptionPane.ERROR_MESSAGE);
        System.err.println("=== Linking Failed ===");
    }
}

/**
 * Ejecuta el programa linkeado
 */
private void runLinkedProgram() {
    FileFrame currentFrame = (FileFrame)desktopPane.getSelectedFrame();
    
    if (currentFrame == null) {
        javax.swing.JOptionPane.showMessageDialog(this,
            "No file is currently open",
            "Run Error",
            javax.swing.JOptionPane.ERROR_MESSAGE);
        return;
    }
    
    // Verificar si el archivo ha sido guardado
    if (!currentFrame.getPreviouslySaved()) {
        javax.swing.JOptionPane.showMessageDialog(this,
            "Please save the file first",
            "Run Error",
            javax.swing.JOptionPane.ERROR_MESSAGE);
        return;
    }
    
    // Usar getTitle() en lugar de getFilePath()
    String sourcePath = currentFrame.getTitle();
    java.io.File sourceFile = new java.io.File(sourcePath);
    String baseDir = sourceFile.getParent();
    if (baseDir == null) baseDir = ".";
    
    String linkedPath = baseDir + java.io.File.separator + "linked.tam";
    
    java.io.File linkedFile = new java.io.File(linkedPath);
    if (!linkedFile.exists()) {
        javax.swing.JOptionPane.showMessageDialog(this,
            "linked.tam not found. Please link the program first.",
            "Run Error",
            javax.swing.JOptionPane.ERROR_MESSAGE);
        return;
    }
    
    // Limpiar y preparar consola
    currentFrame.clearConsole();
    currentFrame.selectConsole();
    output.setDelegate(delegateConsole);
    
    // Deshabilitar botones mientras se ejecuta
    runMenuItem.setEnabled(false);
    buttonRun.setEnabled(false);
    compileMenuItem.setEnabled(false);
    buttonCompile.setEnabled(false);
    
    System.out.println("=== Running Linked Program ===");
    System.out.println("Executing: " + linkedPath);
    
    // Ejecutar usando el intérprete existente
    interpreter.Run(linkedPath);
}

/**
 * Muestra el diálogo de configuración de paquetes
 */
private void configurePackages() {
    GUI.PackageConfigDialog dialog = new GUI.PackageConfigDialog(this, true);
    dialog.setVisible(true);
}

// </editor-fold>
    
} // Fin de la clase Main
