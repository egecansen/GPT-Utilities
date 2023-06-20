package gpt.chat.ui.theme;

import api_assured.Caller;
import gpt.api.GPT;
import gpt.chat.ui.BufferAnimation;
import gpt.chat.ui.ChatGUI;
import gpt.chat.server.Server;
import gpt.models.Message;
import gpt.models.MessageModel;
import gpt.models.MessageResponse;
import lombok.Data;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class SupportGUILight implements ChatGUI {
    private JButton sendButton;
    private JFrame supportPanel;
    private JPanel loadingAnimation = new BufferAnimation.AnimationPanel();
    private JTextPane chatOverviewPanel = new JTextPane();
    private JTextArea messageInputPanel = new JTextArea();
    private String oldMsg;
    private Thread read;
    private String serverName;
    private int PORT;
    private String name;
    private BufferedReader input;
    private PrintWriter output;
    private Socket server;
    private List<Message> messages = new ArrayList<>();
    private String modelName;
    private Double temperature;
    private GPT gpt;
    private String responderName;
    private String userName;
    private String chatTitle;
    private String message;

    public void startServer(){
        Thread serverThread = new Thread(() -> {
            try {new Server(12345).run();}
            catch (IOException e) {throw new RuntimeException(e);}
        });
        serverThread.start();
    }

    public SupportGUILight setTemperature(double temperature){
        this.temperature = temperature;
        return this;
    }

    public SupportGUILight setModelName(String modelName){
        this.modelName = modelName;
        return this;
    }

    public SupportGUILight setResponderName(String responderName){
        this.responderName = responderName;
        return this;
    }

    public SupportGUILight setChatTitle(String chatTitle){
        this.chatTitle = chatTitle;
        return this;
    }

    public SupportGUILight setUserName(String userName){
        this.userName = userName;
        return this;
    }

    public SupportGUILight setMessages(List<Message> messages){
        this.messages = messages;
        return this;
    }

    @SuppressWarnings("unused")
    public SupportGUILight(GPT gpt) {
        this.modelName = "gpt-3.5-turbo";
        this.temperature = 0.7;
        this.gpt = gpt;
        this.userName = "User";
        this.responderName = "ChatGPT";
        this.chatTitle = "Chat";

        Caller.keepLogs(false);
    }

    @SuppressWarnings("unused")
    public SupportGUILight(
            List<String> prompts,
            String modelName,
            double temperature,
            GPT gpt,
            String userName,
            String responderName,
            String chatTitle
    ) {
        this.modelName = modelName;
        this.temperature = temperature;
        this.gpt = gpt;
        this.chatTitle = chatTitle;
        this.responderName = responderName;
        this.userName = userName;

        Caller.keepLogs(false);
        for (String prompt:prompts) messages.add(new Message("system", prompt));
    }

    public void startSupportGUI() {
        try {
            //Font
            String fontfamily = "OpenSans";
            Font font = new Font(fontfamily, Font.PLAIN, 15);
            supportPanel = new JFrame(chatTitle);
            supportPanel.getContentPane().setLayout(null);
            supportPanel.setSize(700, 500);
            supportPanel.setResizable(false);
            supportPanel.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Chat panel
            chatOverviewPanel.setBounds(25, 25, 650, 320);
            chatOverviewPanel.setFont(font);
            chatOverviewPanel.setMargin(new Insets(6, 6, 6, 6));
            chatOverviewPanel.setEditable(false);
            JScrollPane chatOverviewScrollPanel = new JScrollPane(chatOverviewPanel);
            chatOverviewScrollPanel.setBounds(25, 25, 650, 320);

            chatOverviewPanel.setContentType("text/html");
            chatOverviewPanel.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);

            // Field message user input
            messageInputPanel.setBounds(0, 350, 290, 50);
            messageInputPanel.setFont(font);
            messageInputPanel.setMargin(new Insets(6, 6, 6, 6));
            messageInputPanel.setLineWrap(true);
            final JScrollPane messageInputScrollPanel = new JScrollPane(messageInputPanel);
            messageInputScrollPanel.setBounds(25, 350, 540, 110);

            // Send button
            sendButton = new JButton("Send");
            sendButton.setFont(font);
            sendButton.setBounds(575, 350, 100, 105);

            messageInputPanel.addKeyListener(new KeyAdapter() {
                // Send message on Enter
                public void keyPressed(KeyEvent e) {

                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        sendMessage();
                    }

                    // Get last message typed
                    if (e.getKeyCode() == KeyEvent.VK_UP) {
                        String currentMessage = messageInputPanel.getText().trim();
                        messageInputPanel.setText(oldMsg);
                        oldMsg = currentMessage;
                    }

                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        String currentMessage = messageInputPanel.getText().trim();
                        messageInputPanel.setText(oldMsg);
                        oldMsg = currentMessage;
                    }

                }
            });

            // Send button click action
            sendButton.addActionListener(ae -> sendMessage());

            // Chat overview background color
            chatOverviewPanel.setBackground(Color.LIGHT_GRAY); //new Color(192, 192, 192);

            supportPanel.add(chatOverviewScrollPanel);
            supportPanel.setVisible(true);

            // Chat panel initial message
            appendToPane(chatOverviewPanel,
                    "<b>Welcome to " + chatTitle + ", please ask your questions!</b>", false
            );

            // Default server specifications
            name = "User";
            String port = "12345";
            serverName = "localhost";
            PORT = Integer.parseInt(port);

            server = new Socket(serverName, PORT);
            input = new BufferedReader(new InputStreamReader(server.getInputStream()));
            output = new PrintWriter(server.getOutputStream(), true);

            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                ex.printStackTrace();
            }

            // Create new read thread
            read = new Read();
            read.start();

            supportPanel.add(sendButton, JLayeredPane.DEFAULT_LAYER);
            supportPanel.add(messageInputScrollPanel, JLayeredPane.DEFAULT_LAYER);
            supportPanel.revalidate();
            supportPanel.repaint();
            chatOverviewPanel.setBackground(Color.WHITE); //new Color(192, 192, 192);

            loadingAnimation.setLocation(0,0);
            loadingAnimation.setBounds(supportPanel.getBounds());
            //loadingAnimation.setPreferredSize(supportPanel.getPreferredSize());
            loadingAnimation.setVisible(false);
            supportPanel.setGlassPane(loadingAnimation);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void sendMessage() {
        try {
            String message = messageInputPanel.getText().trim();
            if (message.equals("")) return;
            oldMsg = message;
            output.println("<b><span style='color:#c86730'>" + userName + ": </span></b>" + message);

            messages.add(new Message("user", message));
            messageInputPanel.requestFocus();
            messageInputPanel.setText(null);

            messageGPT();
        }
        catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
            System.exit(0);
        }
    }

    public void messageGPT() {
        sendButton.setEnabled(false);
        messageInputPanel.setEnabled(false);
        loadingAnimation.setVisible(true);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                gptResponse();
                return null;
            }
        };

        worker.addPropertyChangeListener(evt -> {
            if (SwingWorker.StateValue.DONE == evt.getNewValue()) {
                // you should also call get() on the worker allowing
                // you to capture and handle all exceptions it might throw
                messageInputPanel.setEnabled(true);
                sendButton.setEnabled(true);
                loadingAnimation.setVisible(false);
            }
        });

        worker.execute();  // run the worker
    }

    public void gptResponse() {
        try {
            MessageResponse messageResponse;
            if (messages.size()!=0)
                messageResponse = gpt.sendMessage(new MessageModel(modelName, messages, temperature));
            else
                messageResponse = gpt.sendMessage(
                        new MessageModel(
                                modelName,
                                List.of(new Message("user", "Hello!")),
                                temperature
                        )
                );
            messages.add(messageResponse.getChoices().get(0).getMessage());
            String message = messageResponse.getChoices().get(0).getMessage().getContent();
            output.println("<b><span style='color:#49984d'>" + responderName + ": </span></b>" + message); //HexCode
        }
        catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
            System.exit(0);
        }
    }

    public void appendToPane(JTextPane textPane, String message, Boolean isBlock){
        textPane = updatePane(textPane, message, isBlock);
        textPane.setVisible(true);
        HTMLDocument doc = (HTMLDocument) textPane.getDocument();
        HTMLEditorKit editorKit = (HTMLEditorKit) textPane.getEditorKit();
        try {
            editorKit.insertHTML(doc, doc.getLength(), message, 0, 0, null);
            textPane.setCaretPosition(doc.getLength());
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public JTextPane updatePane(JTextPane textArea, String message, Boolean isBlock){
        //System.out.println(message + " : " + isBlock);
        //TODO add frame for code blocks

        // Parse text
        if (!isBlock){
            // Regular text
            JTextPane textPane = new JTextPane();
            textPane.setText(message);
            textArea.add(textPane);
        }
        else {
            // Code block
            RSyntaxTextArea syntaxTextArea = new RSyntaxTextArea();
            syntaxTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
            syntaxTextArea.setCodeFoldingEnabled(false);
            syntaxTextArea.setText(message.replace("```", ""));
            textArea.add(new RTextScrollPane(syntaxTextArea));
        }
        return textArea;
    }

    class Read extends Thread {
        public void run() {
            String message;
            boolean isBLock = false;
            while(!Thread.currentThread().isInterrupted()){
                try {
                    message = input.readLine();
                    if(message != null){
                        if (message.contains("```")||message.contains("'''")) isBLock = !isBLock;
                        appendToPane(chatOverviewPanel, message, isBLock);
                    }
                }
                catch (IOException ex) {
                    System.err.println("Failed to parse incoming message");
                }
            }
        }
    }
}
