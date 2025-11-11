/*
 * IDE-Triangle v1.1
 * Main.java
 * Extended with LLVM support
 */

package GUI;
import Core.Console.InputRedirector;
import Core.Console.OutputRedirector;
import Core.IDE.IDEDisassembler;
import Core.IDE.IDEInterpreter;
import Core.Visitors.TableVisitor;
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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import Triangle.IDECompiler;
import Core.ExampleFileFilter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import Core.Visitors.TreeVisitor;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * The Main class. Contains the main form.
 *
 * @author Luis Leopoldo Pérez <luiperpe@ns.isi.ulatina.ac.cr>
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
            compileLLVMMenuItem.setEnabled(false);
        } else {
            checkSaveChanges();
            compileLLVMMenuItem.setEnabled(true);
        }
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
        compileLLVMMenuItem.setEnabled(true);
       
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
     * Create LLVM display frame
     */
    private void createLLVMFrame() {
        llvmFrame = new javax.swing.JInternalFrame("LLVM Code", true, true, true, true);
        llvmCodeArea = new javax.swing.JTextArea();
        llvmCodeArea.setEditable(false);
        llvmCodeArea.setFont(new java.awt.Font("Consolas", 0, 12));
        llvmCodeArea.setTabSize(2);
        
        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(llvmCodeArea);
        llvmFrame.getContentPane().add(scrollPane);
        llvmFrame.setSize(700, 500);
        llvmFrame.setLocation(50, 50);
        llvmFrame.setVisible(false);
        
        desktopPane.add(llvmFrame);
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

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("IDE-Triangle 1.1 (LLVM)");
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
        newMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_DOWN_MASK));
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

        openMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        openMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/Icons/iconFileOpen.gif")));
        openMenuItem.setMnemonic('O');
        openMenuItem.setText("Open");
        openMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openMenuItem);

        saveMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK));
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
        cutMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_DOWN_MASK));
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

        copyMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_DOWN_MASK));
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

        pasteMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_DOWN_MASK));
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
        
        triangleMenu.addSeparator();
        
        // LLVM menu items
        compileLLVMMenuItem = new javax.swing.JMenuItem();
        compileLLVMMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
            java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        compileLLVMMenuItem.setText("Compile to LLVM");
        compileLLVMMenuItem.setEnabled(false);
        compileLLVMMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                compileLLVMMenuItemActionPerformed(evt);
            }
        });
        triangleMenu.add(compileLLVMMenuItem);
        
        saveLLVMMenuItem = new javax.swing.JMenuItem();
        saveLLVMMenuItem.setText("Save LLVM Code");
        saveLLVMMenuItem.setEnabled(false);
        saveLLVMMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveLLVMMenuItemActionPerformed(evt);
            }
        });
        triangleMenu.add(saveLLVMMenuItem);
        
        compileLLVMToNativeMenuItem = new javax.swing.JMenuItem();
        compileLLVMToNativeMenuItem.setText("Compile LLVM to Native");
        compileLLVMToNativeMenuItem.setEnabled(false);
        compileLLVMToNativeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                compileLLVMToNativeActionPerformed(evt);
            }
        });
        triangleMenu.add(compileLLVMToNativeMenuItem);
        
        runLLVMMenuItem = new javax.swing.JMenuItem();
        runLLVMMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
            java.awt.event.KeyEvent.VK_R, 
            java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        runLLVMMenuItem.setText("Run LLVM Program");
        runLLVMMenuItem.setEnabled(false);
        runLLVMMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runLLVMMenuItemActionPerformed(evt);
            }
        });
        triangleMenu.add(runLLVMMenuItem);
        
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

        setJMenuBar(menuBar);
        createLLVMFrame();
        pack();
    }// </editor-fold>//GEN-END:initComponents

    // <editor-fold defaultstate="collapsed" desc=" Event Handlers Implementation ">
    
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

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        while (desktopPane.getComponentCount() > 0) {
            try { 
                desktopPane.getSelectedFrame().setClosed(true);
            } catch (Exception e) { }
        }
    }//GEN-LAST:event_formWindowClosing

    private void pasteMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pasteMenuItemActionPerformed
        ((FileFrame)desktopPane.getSelectedFrame()).pasteText(Clip.getClipboardContents());
    }//GEN-LAST:event_pasteMenuItemActionPerformed

    private void cutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cutMenuItemActionPerformed
        Clip.setClipboardContents(((FileFrame)desktopPane.getSelectedFrame()).getSelectedText());
        ((FileFrame)desktopPane.getSelectedFrame()).cutText();
    }//GEN-LAST:event_cutMenuItemActionPerformed

    private void copyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyMenuItemActionPerformed
        Clip.setClipboardContents(((FileFrame)desktopPane.getSelectedFrame()).getSelectedText());
    }//GEN-LAST:event_copyMenuItemActionPerformed

    private void saveAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsMenuItemActionPerformed
        boolean _previouslySaved = ((FileFrame)desktopPane.getSelectedFrame()).getPreviouslySaved();        
        ((FileFrame)desktopPane.getSelectedFrame()).setPreviouslySaved(false);
        saveMenuItemActionPerformed(evt);
        ((FileFrame)desktopPane.getSelectedFrame()).setPreviouslySaved(_previouslySaved);        
    }//GEN-LAST:event_saveAsMenuItemActionPerformed

    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        new AboutDialog(this, true).setVisible(true);
    }//GEN-LAST:event_aboutMenuItemActionPerformed

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
            new File(desktopPane.getSelectedFrame().getTitle().replace(".tri", ".tam")).delete();
            
            output.setDelegate(delegateConsole);
            String sourceName = desktopPane.getSelectedFrame().getTitle();
            String objectName = sourceName.replace(".tri", ".tam");
            
            if (compiler.compileProgram(sourceName, objectName, false)) {           
                output.setDelegate(delegateTAMCode);
                disassembler.Disassemble(objectName);
                ((FileFrame)desktopPane.getSelectedFrame()).setTree((DefaultMutableTreeNode)treeVisitor.visitProgram(compiler.rootAST, null));
                ((FileFrame)desktopPane.getSelectedFrame()).setTable(tableVisitor.getTable(compiler.rootAST));
                
                runMenuItem.setEnabled(true);
                buttonRun.setEnabled(true);
            } else {
                runMenuItem.setEnabled(false);
                buttonRun.setEnabled(false);
            }
        }
    }//GEN-LAST:event_compileMenuItemActionPerformed

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

    private void newMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newMenuItemActionPerformed
        addInternalFrame("Untitled-" + String.valueOf(untitledCount), "");      
        untitledCount++;
    }//GEN-LAST:event_newMenuItemActionPerformed

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        formWindowClosing(null);
        System.exit(0);
    }//GEN-LAST:event_exitMenuItemActionPerformed

    // LLVM Event Handlers
    private void compileLLVMMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        if (desktopPane.getSelectedFrame() == null) {
            JOptionPane.showMessageDialog(this, 
                "Please select a source file first.", 
                "No File Selected", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String sourceName = desktopPane.getSelectedFrame().getTitle();
        ((FileFrame)desktopPane.getSelectedFrame()).selectConsole();
        ((FileFrame)desktopPane.getSelectedFrame()).clearConsole();
        
        output.setDelegate(delegateConsole);
        
        if (compiler.compileProgramToLLVM(sourceName)) {
            String llvmCode = compiler.getLLVMCode(sourceName);
            llvmCodeArea.setText(llvmCode);
            llvmFrame.setVisible(true);
            
            try {
                llvmFrame.setSelected(true);
            } catch (java.beans.PropertyVetoException e) { }
            
            currentLLVMFile = compiler.getLLVMFilePath(sourceName);
            
            saveLLVMMenuItem.setEnabled(true);
            compileLLVMToNativeMenuItem.setEnabled(true);
            runLLVMMenuItem.setEnabled(false);
        } else {
            JOptionPane.showMessageDialog(this, 
                "Compilation failed. Check console for errors.", 
                "Compilation Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void saveLLVMMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save LLVM Code");
        fileChooser.setFileFilter(new FileNameExtensionFilter("LLVM IR Files (*.ll)", "ll"));
        
        if (currentLLVMFile != null) {
            fileChooser.setSelectedFile(new File(currentLLVMFile));
        }
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String filePath = file.getAbsolutePath();
            if (!filePath.endsWith(".ll")) {
                filePath += ".ll";
            }
            
            try (PrintWriter writer = new PrintWriter(filePath)) {
                writer.print(llvmCodeArea.getText());
                JOptionPane.showMessageDialog(this, 
                    "LLVM code saved successfully!", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                    "Error saving file: " + e.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void compileLLVMToNativeActionPerformed(java.awt.event.ActionEvent evt) {
        if (currentLLVMFile == null) {
            JOptionPane.showMessageDialog(this, 
                "No LLVM file to compile.", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String execFile = currentLLVMFile.replace(".ll", "");
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            execFile += ".exe";
        }
        
        output.setDelegate(delegateConsole);
        
        try {
            ProcessBuilder pb = new ProcessBuilder("clang", currentLLVMFile, "-o", execFile);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            String line;
            while ((line = reader.readLine()) != null) {
                ((FileFrame)desktopPane.getSelectedFrame()).writeToConsole(line + "\n");
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                JOptionPane.showMessageDialog(this, 
                    "Compilation to native code successful!\nExecutable: " + execFile, 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
                runLLVMMenuItem.setEnabled(true);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Compilation failed. Check console for errors.", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, 
                "Clang compiler not found.\nPlease install LLVM/Clang and add it to your PATH.", 
                "Compiler Not Found", 
                JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error during compilation: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void runLLVMMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        if (currentLLVMFile == null) {
            JOptionPane.showMessageDialog(this, 
                "No program to run.", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String execFile = currentLLVMFile.replace(".ll", "");
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            execFile += ".exe";
        }
        
        File executable = new File(execFile);
        if (!executable.exists()) {
            JOptionPane.showMessageDialog(this, 
                "Executable not found. Please compile to native first.", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            
            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", "start", "cmd", "/k", execFile);
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", "-a", "Terminal", execFile);
            } else {
                pb = new ProcessBuilder("x-terminal-emulator", "-e", execFile);
            }
            
            pb.start();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error running program: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // </editor-fold>    
           
    // <editor-fold defaultstate="collapsed" desc=" Delegates and Listeners ">    
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
        public void mouseExited(java.awt.event.MouseEvent evt) { }
        public void mouseEntered(java.awt.event.MouseEvent evt) { }        
        public void mouseReleased(java.awt.event.MouseEvent evt) { }       
        public void mousePressed(java.awt.event.MouseEvent evt) { }        
    };
    
    InternalFrameListener delegateInternalFrame = new InternalFrameListener() {        
        public void internalFrameActivated(InternalFrameEvent evt) { 
            checkPaneChanges();
            ((FileFrame)desktopPane.getSelectedFrame()).UpdateRowColNumbers();
        }
        
        public void internalFrameClosed(InternalFrameEvent evt) {
            checkPaneChanges();
        }
            
        public void internalFrameClosing(InternalFrameEvent evt) {
            if (((FileFrame)desktopPane.getSelectedFrame()).hasChanged()) { 
                if (JOptionPane.showConfirmDialog(null, "Do you want to save the changes to " + desktopPane.getSelectedFrame().getTitle() + "?", "Save", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)    
                    saveMenuItemActionPerformed(null);    
            }
        }
        
        public void internalFrameDeactivated(InternalFrameEvent evt) { }
        public void internalFrameDeiconified(InternalFrameEvent evt) { }
        public void internalFrameIconified(InternalFrameEvent evt) { }
        public void internalFrameOpened(InternalFrameEvent evt) { }
    };
    
    ActionListener delegateConsole = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            while (output.peekQueue())
                ((FileFrame)desktopPane.getSelectedFrame()).writeToConsole(output.readQueue());
        }
    };
    
    ActionListener delegateTAMCode = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            while (output.peekQueue())
                ((FileFrame)desktopPane.getSelectedFrame()).writeToTAMCode(output.readQueue());
        }        
    };
    
    ActionListener delegateInput = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            ((FileFrame)desktopPane.getSelectedFrame()).setInputEnabled(true);
        }
    };
    
    ActionListener delegateEnter = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            ((FileFrame)desktopPane.getSelectedFrame()).setInputEnabled(false);
            input.addInput(((FileFrame)desktopPane.getSelectedFrame()).getInputFieldText() + "\n");
            ((FileFrame)desktopPane.getSelectedFrame()).clearInputField();
        }
    };
    
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
    javax.swing.JMenuItem aboutMenuItem;
    javax.swing.JButton buttonCompile;
    javax.swing.JButton buttonCopy;
    javax.swing.JButton buttonCut;
    javax.swing.JButton buttonNew;
    javax.swing.JButton buttonOpen;
    javax.swing.JButton buttonPaste;
    javax.swing.JButton buttonRun;
    javax.swing.JButton buttonSave;
    javax.swing.JMenuItem compileMenuItem;
    javax.swing.JMenuItem copyMenuItem;
    javax.swing.JMenuItem cutMenuItem;
    javax.swing.JDesktopPane desktopPane;
    javax.swing.JMenu editMenu;
    javax.swing.JToolBar editToolBar;
    javax.swing.JMenuItem exitMenuItem;
    javax.swing.JMenu fileMenu;
    javax.swing.JToolBar fileToolBar;
    javax.swing.JMenu helpMenu;
    javax.swing.JMenuBar menuBar;
    javax.swing.JMenuItem newMenuItem;
    javax.swing.JMenuItem openMenuItem;
    javax.swing.JMenuItem pasteMenuItem;
    javax.swing.JMenuItem runMenuItem;
    javax.swing.JMenuItem saveAsMenuItem;
    javax.swing.JMenuItem saveMenuItem;
    javax.swing.JSeparator separatorExit;
    javax.swing.JPanel toolBarsPanel;
    javax.swing.JMenu triangleMenu;
    javax.swing.JToolBar triangleToolBar;
    
    // LLVM Components
    private javax.swing.JMenuItem compileLLVMMenuItem;
    private javax.swing.JMenuItem saveLLVMMenuItem;
    private javax.swing.JMenuItem compileLLVMToNativeMenuItem;
    private javax.swing.JMenuItem runLLVMMenuItem;
    private javax.swing.JInternalFrame llvmFrame;
    private javax.swing.JTextArea llvmCodeArea;
    private String currentLLVMFile = null;
    // End of variables declaration//GEN-END:variables
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc=" Non-GUI Variables ">
    int untitledCount = 1;
    clipBoard Clip = new clipBoard();
    IDECompiler compiler = new IDECompiler();
    IDEDisassembler disassembler = new IDEDisassembler();
    IDEInterpreter interpreter = new IDEInterpreter(delegateRun);
    OutputRedirector output = new OutputRedirector();
    InputRedirector input = new InputRedirector(delegateInput);
    TreeVisitor treeVisitor = new TreeVisitor();
    TableVisitor tableVisitor = new TableVisitor();
    File directory;
    // </editor-fold>    
    
    // <editor-fold defaultstate="collapsed" desc=" Internal Class - ClipboardOwner ">
    private class clipBoard implements ClipboardOwner {
        public void lostOwnership(Clipboard aClipboard, Transferable aContents) { }
        
        public void setClipboardContents(String _contents) {
            StringSelection stringSelection = new StringSelection(_contents);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, this);
        }
        
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
}
