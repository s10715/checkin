package checkin.process;

import checkin.M;
import checkin.utils.Common;
import checkin.utils.EncryptUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class UIWindow {

    public void runPasswordFrame() {


        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            //e.printStackTrace();
        }

        //-----------------------------------------密码框部分-----------------------------------------

        JFrame pwdFrame = new JFrame();
        pwdFrame.setSize(400, 200);
        pwdFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        pwdFrame.setLocationRelativeTo(null);
        pwdFrame.setTitle("请输入使用密码");
        pwdFrame.setBackground(Color.GRAY);
        pwdFrame.setLayout(null);
        pwdFrame.setResizable(false);
        try {
            //直接编译运行要使用图标用这种方法，但是生成jar包后无法显示图标
            pwdFrame.setIconImage(new ImageIcon("icon.png").getImage());
        } catch (NullPointerException e) {
            //e.printStackTrace();
        }

        try {
            //要使生成的jar有图标，就使用该方法，并手动把图标文件复制到jar包中的checking目录下（和Main.class同级目录），但是直接编译运行使用这种方法会报错
            pwdFrame.setIconImage(new ImageIcon(M.class.getResource("icon.png")).getImage());
        } catch (NullPointerException e) {
            //e.printStackTrace();
        }
        JTextField pwdField = new JTextField();
        pwdField.setBounds(50, 40, 300, 40);
        pwdField.setBackground(Color.WHITE);
        pwdField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String pwd = pwdField.getText();
                    if (pwd != null && EncryptUtils.generatePassWord().equals(pwd.toLowerCase())) {
                        pwdFrame.setVisible(false);
                        runMainFrame();
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });
        pwdFrame.add(pwdField);

        JButton pwdButton = new JButton("确定");
        pwdButton.setBounds(75, 100, 100, 40);
        pwdButton.setFocusPainted(false);
        pwdButton.setBackground(Color.WHITE);
        pwdButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String pwd = pwdField.getText();
                if (pwd != null && EncryptUtils.generatePassWord().equals(pwd.toLowerCase())) {
                    pwdFrame.setVisible(false);
                    runMainFrame();
                }
            }
        });
        pwdFrame.add(pwdButton);


        JButton cancelButton = new JButton("退出");
        cancelButton.setBounds(225, 100, 100, 40);
        cancelButton.setFocusPainted(false);
        cancelButton.setBackground(Color.WHITE);
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pwdFrame.setVisible(false);
                System.exit(0);
            }
        });
        pwdFrame.add(cancelButton);

        pwdFrame.setVisible(true);


    }

    public void runMainFrame() {
        //-----------------------------------------主界面部分-----------------------------------------

        JFrame frame = new JFrame();
        frame.setSize(300, 350);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setTitle("考勤统计");
        frame.setBackground(Color.GRAY);
        frame.setLayout(null);
        frame.setResizable(false);


        try {
            //直接编译运行要使用图标用这种方法，但是生成jar包后无法显示图标
            frame.setIconImage(new ImageIcon("icon.png").getImage());
        } catch (NullPointerException e) {
            //e.printStackTrace();
        }

        try {
            //要使生成的jar有图标，就使用该方法，并手动把图标文件复制到jar包中的checking目录下（和Main.class同级目录），但是直接编译运行使用这种方法会报错
            frame.setIconImage(new ImageIcon(M.class.getResource("icon.png")).getImage());
        } catch (NullPointerException e) {
            //e.printStackTrace();
        }

        //加载用户配置的打卡时间
        File currentDir = new File(".");
        File classTimeFile = new File(currentDir.getAbsolutePath() + File.separator + "classTimes.cfg");
        if (classTimeFile.exists()) {
            String content = new String(Common.readFile(classTimeFile.getAbsolutePath()), StandardCharsets.UTF_8);//读取配置的打卡时间
            Executor.parseClassTimes(content);//应用配置的打卡时间
        }

        //-------------------------------菜单-------------------------------
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("   选项   ");
        JMenuItem classTimeItem = new JMenuItem("配置打卡时间");
        menu.add(classTimeItem);
        JMenuItem helpItem = new JMenuItem("使用说明");
        menu.add(helpItem);
        menuBar.add(menu);
        frame.setJMenuBar(menuBar);

        classTimeItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String content = "";

                //如果之前配置过打卡时间，则加载之前配置的打卡时间
                if (classTimeFile.exists()) {
                    content = new String(Common.readFile(classTimeFile.getAbsolutePath()), StandardCharsets.UTF_8);//读取配置的打卡时间
                    content = Common.cleanupString(content);
                }

                //弹出编辑框，让用户修改
                FileEditorDialog fileEditorDialog = new FileEditorDialog(content);
                content = fileEditorDialog.showForResult();

                //保存配置的打卡时间
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(classTimeFile), StandardCharsets.UTF_8))) {
                    writer.write(Common.cleanupString(content));
                    writer.flush();
                } catch (Exception ex) {
                    //ex.printStackTrace();
                }

                Executor.parseClassTimes(content);//应用配置
            }
        });

        helpItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                HelpDialog helpDialog = new HelpDialog();
                helpDialog.setVisible(true);
            }
        });


        //-------------------------------按钮-------------------------------
        JButton button1 = new JButton("班次匹配");
        button1.setBounds(50, 50, 200, 50);
        button1.setFocusPainted(false);
        button1.setBackground(Color.WHITE);
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setDialogTitle("选择要进行班次匹配的文件夹");

                frame.add(fileChooser);
                int returnVal = fileChooser.showOpenDialog(fileChooser);
                frame.remove(fileChooser);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    try {
                        String filePath = fileChooser.getSelectedFile().getAbsolutePath();//这个就是你选择的文件夹的路径
                        frame.setTitle("匹配中...");
                        frame.setEnabled(false);
                        Executor.pairClassInDir(filePath);
                        frame.setEnabled(true);
                        frame.setTitle("考勤统计");
                        JOptionPane.showMessageDialog(frame.getContentPane(), "完成！", "提示", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e1) {
                        JOptionPane.showMessageDialog(frame.getContentPane(), "匹配时出现了错误，请检查数据格式是否正确", "错误", JOptionPane.INFORMATION_MESSAGE);
                        frame.setTitle("考勤统计");
                        frame.setEnabled(true);
                    }
                }
            }
        });
        new DropTarget(button1, DnDConstants.ACTION_COPY_OR_MOVE, new DropTargetListener() {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {

            }

            @Override
            public void dragOver(DropTargetDragEvent dtde) {

            }

            @Override
            public void dropActionChanged(DropTargetDragEvent dtde) {

            }

            @Override
            public void dragExit(DropTargetEvent dte) {

            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                boolean isAccept = false;

                try {
                    if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {//拖进来的是个文件夹或文件
                        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                        isAccept = true;
                        java.util.List<File> files = (java.util.List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                        if (files.size() == 0) {
                            return;
                        }

                        //不允许文件和文件夹混在一起拖进来，不然无法知道根路径是哪个
                        boolean isAllDirectory = true;
                        boolean isAllFile = true;
                        for (File file : files) {
                            if (file.exists()) {
                                if (file.isDirectory()) {
                                    isAllFile = false;
                                } else if (file.isFile()) {
                                    isAllDirectory = false;
                                }
                            }
                        }

                        if (isAllDirectory) {//如果是一个或多个文件夹
                            frame.setTitle("匹配中...");
                            frame.setEnabled(false);
                            for (File file : files) {
                                Executor.pairClassInDir(file.getAbsolutePath());
                            }
                            frame.setEnabled(true);
                            frame.setTitle("考勤统计");
                            JOptionPane.showMessageDialog(frame.getContentPane(), "完成！", "提示", JOptionPane.INFORMATION_MESSAGE);
                        } else if (isAllFile) {//如果是一个或多个文件
                            frame.setTitle("匹配中...");
                            frame.setEnabled(false);
                            for (File file : files) {
                                Executor.pairClassOfFile(file.getAbsolutePath());
                            }
                            frame.setEnabled(true);
                            frame.setTitle("考勤统计");
                            JOptionPane.showMessageDialog(frame.getContentPane(), "完成！", "提示", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                    if (isAccept) {
                        dtde.dropComplete(true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(frame.getContentPane(), "匹配时出现了错误，请检查数据格式是否正确", "错误", JOptionPane.INFORMATION_MESSAGE);
                    frame.setTitle("考勤统计");
                    frame.setEnabled(true);
                }
            }
        }, true);


        JButton button2 = new JButton("生成考勤确认表");
        button2.setBounds(50, 125, 200, 50);
        button2.setFocusPainted(false);
        button2.setBackground(Color.WHITE);
        button2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();/* {
                    @Override
                    public Icon getIcon(File f) {
                        ImageIcon icon = null;
                        try {
                            icon = new ImageIcon("icon.png");
                        } catch (NullPointerException e) {
                            try {
                                icon = new ImageIcon(Main.class.getResource("icon.png"));
                            } catch (NullPointerException e1) {
                                //e.printStackTrace();
                            }
                        }
                        return icon;
                    }
                };*/
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setDialogTitle("选择要生成考勤确认表的文件夹");

                frame.add(fileChooser);
                int returnVal = fileChooser.showOpenDialog(fileChooser);
                frame.remove(fileChooser);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    try {
                        String filePath = fileChooser.getSelectedFile().getAbsolutePath();//这个就是你选择的文件夹的路径
                        frame.setTitle("生成中...");
                        frame.setEnabled(false);
                        Executor.generateConfirmFormInDir(filePath);
                        frame.setEnabled(true);
                        frame.setTitle("考勤统计");
                        JOptionPane.showMessageDialog(frame.getContentPane(), "完成！", "提示", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e1) {
                        JOptionPane.showMessageDialog(frame.getContentPane(), "生成时出现了错误，请检查数据格式是否正确", "错误", JOptionPane.INFORMATION_MESSAGE);
                        frame.setTitle("考勤统计");
                        frame.setEnabled(true);
                    }
                }
            }
        });
        new DropTarget(button2, DnDConstants.ACTION_COPY_OR_MOVE, new DropTargetListener() {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {

            }

            @Override
            public void dragOver(DropTargetDragEvent dtde) {

            }

            @Override
            public void dropActionChanged(DropTargetDragEvent dtde) {

            }

            @Override
            public void dragExit(DropTargetEvent dte) {

            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                boolean isAccept = false;

                try {
                    if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                        isAccept = true;
                        java.util.List<File> files = (java.util.List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                        if (files.size() == 0) {
                            return;
                        }

                        //不允许文件和文件夹混在一起拖进来，不然无法知道根路径是哪个
                        boolean isAllDirectory = true;
                        boolean isAllFile = true;
                        for (File file : files) {
                            if (file.exists()) {
                                if (file.isDirectory()) {
                                    isAllFile = false;
                                } else if (file.isFile()) {
                                    isAllDirectory = false;
                                }
                            }
                        }

                        if (isAllDirectory) {//如果是一个或多个文件夹，则每个文件夹下都生成考勤确认表
                            frame.setTitle("生成中...");
                            frame.setEnabled(false);
                            for (File file : files) {
                                Executor.generateConfirmFormInDir(file.getAbsolutePath());
                            }
                            frame.setEnabled(true);
                            frame.setTitle("考勤统计");
                            JOptionPane.showMessageDialog(frame.getContentPane(), "完成！", "提示", JOptionPane.INFORMATION_MESSAGE);
                        } else if (isAllFile) {//如果是一个或多个文件，则在文件的目录下生成一个考勤确认表
                            frame.setTitle("生成中...");
                            frame.setEnabled(false);
                            Executor.generateConfirmFormOfFileList(files.get(0).getParent(), files);
                            frame.setEnabled(true);
                            frame.setTitle("考勤统计");
                            JOptionPane.showMessageDialog(frame.getContentPane(), "完成！", "提示", JOptionPane.INFORMATION_MESSAGE);
                        }

                    }
                    if (isAccept) {
                        dtde.dropComplete(true);
                    }
                } catch (Exception e) {
                    //e.printStackTrace();
                    JOptionPane.showMessageDialog(frame.getContentPane(), "生成时出现了错误，请检查数据格式是否正确", "错误", JOptionPane.INFORMATION_MESSAGE);
                    frame.setTitle("考勤统计");
                    frame.setEnabled(true);
                }
            }
        }, true);


        JButton button3 = new JButton("退出");
        button3.setBounds(50, 225, 200, 50);
        button3.setFocusPainted(false);
        button3.setBackground(Color.WHITE);
        button3.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        frame.getContentPane().add(button1);
        frame.getContentPane().add(button2);
        frame.getContentPane().add(button3);


        frame.setVisible(true);

    }

    private class FileEditorDialog extends JDialog {
        private String newContent;

        FileEditorDialog(String content) {
            newContent = content;

            setSize(700, 700);
            setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            setLocationRelativeTo(null);
            setTitle("编辑");
            setBackground(Color.GRAY);

            try {
                //直接编译运行要使用图标用这种方法，但是生成jar包后无法显示图标
                setIconImage(new ImageIcon("icon.png").getImage());
            } catch (NullPointerException e) {
                //e.printStackTrace();
            }

            try {
                //要使生成的jar有图标，就使用该方法，并手动把图标文件复制到jar包中的checking目录下（和Main.class同级目录），但是直接编译运行使用这种方法会报错
                setIconImage(new ImageIcon(M.class.getResource("icon.png")).getImage());
            } catch (NullPointerException e) {
                //e.printStackTrace();
            }

            JScrollPane scrollPane = new JScrollPane();
            JTextArea textArea = new JTextArea();
            textArea.setText(content);
            scrollPane.setViewportView(textArea);
            add(scrollPane);

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    super.windowClosing(e);

                    String text = textArea.getText();
                    if (!text.equals(content)) {//有改动才询问是否保存，否则直接退出
                        int option = JOptionPane.showConfirmDialog(FileEditorDialog.this, "是否保存?");
                        switch (option) {
                            case JOptionPane.CANCEL_OPTION://取消
                                break;
                            case JOptionPane.YES_OPTION://是
                                newContent = text;
                            case JOptionPane.NO_OPTION://否
                            case JOptionPane.CLOSED_OPTION://直接关闭，没有选择
                            default:
                                setVisible(false);
                                dispose();
                                break;
                        }
                    } else {
                        setVisible(false);
                        dispose();
                    }
                }
            });

        }

        String showForResult() {
            setModal(true);
            setVisible(true);
            return newContent;
        }

    }

    private class HelpDialog extends JDialog {

        HelpDialog() {
            setSize(700, 700);
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            setLocationRelativeTo(null);
            setTitle("使用说明");
            setResizable(false);
            setBackground(Color.GRAY);

            try {
                //直接编译运行要使用图标用这种方法，但是生成jar包后无法显示图标
                setIconImage(new ImageIcon("icon.png").getImage());
            } catch (NullPointerException e) {
                //e.printStackTrace();
            }

            try {
                //要使生成的jar有图标，就使用该方法，并手动把图标文件复制到jar包中的checking目录下（和Main.class同级目录），但是直接编译运行使用这种方法会报错
                setIconImage(new ImageIcon(M.class.getResource("icon.png")).getImage());
            } catch (NullPointerException e) {
                //e.printStackTrace();
            }

            JScrollPane scrollPane = new JScrollPane();
            JLabel label = new JLabel();
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setVerticalAlignment(SwingConstants.CENTER);

            label.setText("<html><body>使用说明:<br/>" +
                    "<br/>" +
                    "Excel文档数据格式：<br/>" +
                    "第一行为空或者表头（第一行不会被读取）<br/>" +
                    "后面每行的格式为：" +
                    "<table border='1'><tr><td>考勤号码</td><td>姓名</td><td>日期</td><td>时间1</td><td>时间2</td><td>时间3</td><td>...</td></tr></table><br/>" +
                    "<br/>" +
                    "打卡时间配置样例（文件夹名1->文件夹名2->文件名称（即部门名称）=班次名称>时间1,时间2,...）：<br/>" +
                    "<span style=\"color: rgb(255,0,0)\">技术部=A班&gt;8:00,12:00,13:00,18:00</span><br/>" +
                    "<span style=\"color: rgb(255,0,0)\">技术部=B班&gt;9:00,13:00,14:00,19:00</span><br/>" +
                    "<span style=\"color: rgb(255,0,0)\">技术部=C班&gt;9:00,17:00</span><br/>" +
                    "<span style=\"color: rgb(255,0,0)\">项目经理=A班&gt;9:00,18:00</span><br/>" +
                    "<span style=\"color: rgb(255,0,0)\">美工=张三A班&gt;8:00,12:00,13:00,18:00</span><br/>" +
                    "<span style=\"color: rgb(255,0,0)\">美工=张三B班&gt;9:00,17:00</span><br/>" +
                    "<span style=\"color: rgb(255,0,0)\">分公司1->美工=李四A班&gt;9:00,17:00</span><br/>" +
                    "<span style=\"color: rgb(255,0,0)\">分公司2->子公司1->美工=王五A班&gt;9:00,17:00</span><br/>" +
                    "1、每打完一个班次换一行，注意以上所有标点都是英文标点，写成中文标点会导致无法识别<br/>" +
                    "2、超过下班点半个小时才算晚走（早来同理），但迟到早退1分钟都算<br/>" +
                    "3、默认第一个时间为上班点，第二个为下班点，...，如果顺序错了，迟到早退和早来晚走的标记会相反<br/>" +
                    "4、要给每个人定制班次，班次名称包含这个人的全名即可<br/>" +
                    "5、Excel的文件名要和这里设置的部门名称一致且路径对应才会进行打卡记录分析<br/>" +
                    "6、文件夹名可有可无，也可以有一个或多个<br/>" +
                    "7、这里设置的文件夹名、文件名、班次名最后都会按照顿号进行拆分<br/>" +
                    "8、如果有通宵班，第一个时间点为下班点，那么这里随便填一个字符即可，比如<br/>" +
                    "<span style=\"color: rgb(255,0,0)\">分公司3->保安=A班&gt;-1,8:00,16:00</span><br/>" +
                    "9、以#开头的行不会被解析<br/>" +
                    "<br/>" +
                    "<br/>" +
                    "设置好打卡时间后，把整个考勤文件夹拖到相应按钮上，或者点击按钮选择考勤文件夹<br/>" +
                    "等待片刻即可<br/>" +
                    "如果没有反应，或者少了某个班次，检查是不是配置写错了<br/>" +
                    "<br/>" +
                    "<br/>" +
                    "想要用本软件生成考勤确认表，就要按生成的每个人的汇总条填写，随便改动可能导致无法识别<br/>" +
                    "<br/>" +
                    "</body></html>");
            scrollPane.setViewportView(label);
            add(scrollPane);
        }
    }
}
