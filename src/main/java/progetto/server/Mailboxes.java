package progetto.server;

import com.opencsv.bean.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.image.Image;
import progetto.common.Mail;
import progetto.common.Request;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Mailboxes {
    private final Map<String, Mailbox> mailboxList;                    // Mailboxes of all users indexed by mail address
    private ObservableList<Log> logs =
            FXCollections.observableArrayList();                       // List of logs
    private final String path;

    public static Image LOAD;                                          // Load image for image requests
    public static Image TICK;                                          // Load image for image requests
    public static Image CROSS;                                         // Load image for image requests

    /**
     * Create a new set of mailboxes for all the users
     * @param path path to users files
     * @throws IOException users file not found, requests' images not found
     */
    public Mailboxes(String path) throws IOException {
        mailboxList = new HashMap<>();
        this.path = path;
        String filePath = "users.txt";
        try(Scanner users = new Scanner(new File(path + filePath))) {
            while (users.hasNextLine()){
                String user = users.nextLine();
                System.out.println(user);
                newMailbox(user);
            }
        }

        LOAD = new Image(getClass().getResource("/progetto.server/load.jpg").toExternalForm());
        CROSS = new Image(getClass().getResource("/progetto.server/cross.jpg").toExternalForm());
        TICK = new Image(getClass().getResource("/progetto.server/tick.jpg").toExternalForm());
    }

    // Logs property usual methods
    public ObservableList<Log> logsProperty() {
        return logs;
    }
    public void setLogs(ObservableList<Log> current) {
        logs = current;
    }

    // Create a new mailbox for an individual user
    private void newMailbox(String address) {
        Mailbox m = new Mailbox(path + address);
        mailboxList.put(address, m);
    }

    /**
     * Recover the mail list (or a portion) of a given user
     * @param address mail address used to recover the proper mailbox
     * @param skipLines number of mails to skip, used to avoid the recover of the entire mail list every time
     * @return the mail list of the given address
     * @throws NoSuchElementException when mail address is not associated with a saved user
     * @throws InternalError in case something gone wrong
     */
    public List<Mail> getMailboxMailList(String address, int skipLines) throws NoSuchElementException, InternalError{
        Mailbox m = mailboxList.get(address);
        if(m == null){
            System.out.println("GetMailList: UTENTE NON ESISTE");
            throw new NoSuchElementException();
        }
        return m.getMailList(skipLines);
    }

    /**
     * Update (new mail) a user mailbox
     * @param address mail address used to recover the proper mailbox
     * @param mail the new mail to be written
     * @throws NoSuchElementException when mail address is not associated with a saved user
     * @throws InternalError in case something gone wrong
     */
    public void updateMailboxMailList(String address, Mail mail) throws NoSuchElementException, InternalError{
        Mailbox m = mailboxList.get(address);
        if(m == null){
            System.out.println("Update: UTENTE NON ESISTE");
            throw new NoSuchElementException();
        }
        m.updateMailList(mail);
    }

    /**
     * Delete a mail from a mailbox
     * @param address mail address used to recover the proper mailbox
     * @param mailID the ID of the mail to be deleted
     * @throws NoSuchElementException when mail address is not associated with a saved user
     * @throws InternalError in case something gone wrong
     */
    public void deleteMailboxMail(String address, int mailID) throws NoSuchElementException, InternalError{
        Mailbox m = mailboxList.get(address);
        if(m == null){
            System.out.println("Delete: UTENTE NON ESISTE");
            throw new NoSuchElementException();
        }
        m.deleteMail(mailID);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static class Mailbox {
        private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();   // In order to handle the user file
        private final Lock readLock = readWriteLock.readLock();                     // To regulate read accesses
        private final Lock writeLock = readWriteLock.writeLock();                   // To regulate write accesses

        private final String path;                                                  // User file path
        private final AtomicInteger emailCounter = new AtomicInteger();             // ID giver for the new mails

        private Mailbox(String path) {
            this.path = path + ".csv";

            // Recover the last set MailID if the mailbox is not empty
            try(BufferedReader br = new BufferedReader(new FileReader(this.path))) {

                String lastLine = "";
                String currentLine = "";

                // Recover the last line
                while ((currentLine = br.readLine()) != null) {
                    lastLine = currentLine;
                }

                emailCounter.set(getIDFromLine(lastLine) + 1);
            } catch (IOException e){
                // Set counter to 0 otherwise (first mail)
                System.out.println("IOException");
                emailCounter.set(0);
            }
        }

        // Recover the mail list of the user from his file
        private List<Mail> getMailList(int skipLines) {
            List<Mail> mailList = new ArrayList<>();
            readLock.lock(); // Acquires the READ lock
            try(Reader reader = new FileReader(path);
                BufferedReader bufferedReader = new ReplaceNewLineReader(reader)) {

                // Ignores empty lines from the input
                CsvToBeanFilter ignoreEmptyLines = strings -> {
                    for (String one : strings) {
                        if (one != null && one.length() > 0) {
                            return true;
                        }
                    }
                    return false;
                };

                // Create a new builder that maps a CSV file to a list of Mails
                CsvToBean<Mail> csvToBean = new CsvToBeanBuilder<Mail>(bufferedReader)
                        .withType(Mail.class)
                        .withIgnoreLeadingWhiteSpace(true)
                        .withFilter(ignoreEmptyLines)
                        .withSkipLines(skipLines)
                        .build();

                // Parse it
                mailList = csvToBean.parse();

                System.out.println("Skippo: " + skipLines + " Letto: " + mailList.size());

                reader.close();

            }catch (IOException | IllegalStateException e) {
                return mailList; // In case the file doesn't exist (mail list empty)
            } catch (Exception e){
                throw new InternalError();  // Something gone wrong
            } finally{
                readLock.unlock();  // In any case, unlock
            }

            return mailList;
        }

        // Write a new mail in the user file
        private void updateMailList(Mail m) {

            // Retrieve and set a new MailID
            m.setID(emailCounter.getAndIncrement());

            // Replace every '\n' character to the two literal \n characters (avoid CSV parsing problems)
            String text = m.getText();
            if(text != null){
                m.setText(text.replace("\n", "\\n"));
            }

            writeLock.lock();   // Acquires the WRITE lock
            try(Writer writer = Files.newBufferedWriter(Paths.get(path),
                    StandardOpenOption.CREATE,  // If the file doesn't exists, create it (first mail)
                    StandardOpenOption.APPEND)) // If it already exists, add the new mail
            {

                // Create a new builder that maps a Mail to a CSV line
                StatefulBeanToCsv<Mail> beanToCsv = new StatefulBeanToCsvBuilder<Mail>(writer)
                        .withEscapechar('\\')
                        .build();

                // Write the new Mail
                beanToCsv.write(m);

            }catch (Exception e) {
                throw new InternalError(e.getMessage());    // Something gone wrong
            } finally {
                writeLock.unlock(); // In any case, unlock
            }
        }

        // Delete a mail from the user file
        private void deleteMail(int mailID){
            writeLock.lock();   // Acquires the READ lock

            List<String> tempMailList = new ArrayList<>();  // Temporary "file"
            BufferedReader br = null;
            BufferedWriter bw = null;
            try {
                br = new BufferedReader(new FileReader(path));
                String currentLine = "";

                // Copy all the mails to the "temporary file", except the one to be deleted
                while ((currentLine = br.readLine()) != null) {
                    if(getIDFromLine(currentLine) != mailID){
                        tempMailList.add(currentLine);
                    }

                }
                br.close();

                bw = new BufferedWriter(new FileWriter(path));

                // Copy all the remaining mails to the file
                for (String mail : tempMailList){
                    bw.write(mail + System.lineSeparator());
                }

                bw.close();

            }catch (Exception e) {
                throw new InternalError();
            } finally {
                writeLock.unlock();
                try {
                    if(br != null)
                        br.close();
                    if(bw != null)
                        bw.close();
                } catch (Exception ignored) {
                    // TODO: capire come gestirla qua
                }
            }

        }
    }

    // Custom BufferedReader implemented to restore the '\n' character
    private static class ReplaceNewLineReader extends BufferedReader {

        private ReplaceNewLineReader(Reader r) {
            super(r);
        }

        @Override
        public String readLine() throws IOException {
            String line = super.readLine();
            if(line != null)
                return line.replace("\\n", "\n");
            return null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Get the ID by matching the first value inside double quotes
    private static int getIDFromLine(String line){
        Matcher m = Pattern.compile("(?<=\")([^\"]+)(?=\")").matcher(line);
        if(m.find())
            return Integer.parseInt(m.group(1));
        return 0;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
