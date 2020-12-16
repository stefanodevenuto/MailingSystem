package progetto.server;

import com.opencsv.bean.*;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import progetto.common.Mail;
import progetto.common.Request;

import java.io.*;
import java.nio.charset.Charset;
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
    private Map<String, Mailbox> mailboxList;
    private ObservableList<Log> logs = FXCollections.observableArrayList();
    private String path;

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
    }

    public ObservableList<Log> logsProperty() {
        return logs;
    }
    public void setLogs(ObservableList<Log> current) {
        logs = current;
    }

    private void newMailbox(String address) {
        Mailbox m = new Mailbox(path + address);
        mailboxList.put(address, m);
    }

    public List<Mail> getMailboxMailList(String address, int skipLines) throws NoSuchElementException, InternalError{
        Mailbox m = mailboxList.get(address);
        if(m == null){
            System.out.println("GetMailList: UTENTE NON ESISTE");
            throw new NoSuchElementException();
        }
        return m.getMailList(skipLines);
    }

    public void updateMailboxMailList(String address, Mail mail) throws NoSuchElementException, InternalError{
        Mailbox m = mailboxList.get(address);
        if(m == null){
            System.out.println("Update: UTENTE NON ESISTE");
            throw new NoSuchElementException();
        }
        m.updateMailList(mail);
    }

    public void deleteMailboxMail(String address, int mailID) throws NoSuchElementException, InternalError{
        Mailbox m = mailboxList.get(address);
        if(m == null){
            System.out.println("Delete: UTENTE NON ESISTE");
            throw new NoSuchElementException();
        }
        m.deleteMail(mailID);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private class Mailbox {
        private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        private final Lock readLock = readWriteLock.readLock();
        private final Lock writeLock = readWriteLock.writeLock();

        private final String path;
        private final AtomicInteger emailCounter = new AtomicInteger();

        private Mailbox(String path) {
            this.path = path + ".csv";
            try(BufferedReader br = new BufferedReader(new FileReader(this.path))) {

                String lastLine = "";
                String currentLine = "";

                // Recover the last line
                while ((currentLine = br.readLine()) != null) {
                    lastLine = currentLine;
                }

                emailCounter.set(getIDFromLine(lastLine) + 1);
            } catch (IOException e){
                System.out.println("IOException");
                emailCounter.set(0);
            }
        }

        private List<Mail> getMailList(int skipLines) {
            List<Mail> mailList = new ArrayList<>();
            readLock.lock();
            try {
                Reader reader = new FileReader(path);
                BufferedReader bufferedReader = new ReplaceNewLineReader(reader);
                //Reader reader = Files.newBufferedReader(Paths.get("C:\\Users\\stefa\\Desktop\\" + address + ".csv"));
                CsvToBean<Mail> csvToBean;

                // Ignores empty lines from the input
                CsvToBeanFilter ignoreEmptyLines = strings -> {
                    for (String one : strings) {
                        if (one != null && one.length() > 0) {
                            return true;
                        }
                    }
                    return false;
                };

                csvToBean = new CsvToBeanBuilder<Mail>(bufferedReader)
                        .withType(Mail.class)
                        .withIgnoreLeadingWhiteSpace(true)
                        .withFilter(ignoreEmptyLines)
                        .withSkipLines(skipLines)
                        .build();

                mailList = csvToBean.parse();

                System.out.println("Skippo: " + skipLines + " Letto: " + mailList.size());

                reader.close();

            }catch (IOException | IllegalStateException e) {
                return mailList;
            } catch (Exception e){
                throw new InternalError();
            } finally{
                readLock.unlock();
            }

            return mailList;
        }

        private void updateMailList(Mail m) {
            m.setID(emailCounter.getAndIncrement());
            String text = m.getText();
            if(text != null){
                m.setText(text.replace("\n", "\\n"));
            }

            writeLock.lock();
            try(Writer writer = Files.newBufferedWriter(Paths.get(path),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND))
            {


                StatefulBeanToCsv<Mail> beanToCsv = new StatefulBeanToCsvBuilder<Mail>(writer)
                        .withEscapechar('\\')
                        .build();

                beanToCsv.write(m);

            }catch (Exception e) {
                throw new InternalError(e.getMessage());
            } finally {
                writeLock.unlock();
            }
        }

        private void deleteMail(int mailID){
            writeLock.lock();

            List<String> tempMailList = new ArrayList<>();
            try {
                BufferedReader br = new BufferedReader(new FileReader(path));
                String currentLine = "";

                // Remove the mail
                while ((currentLine = br.readLine()) != null) {
                    if(getIDFromLine(currentLine) != mailID){
                        tempMailList.add(currentLine);
                    }

                }
                br.close();

                BufferedWriter bw = new BufferedWriter(new FileWriter(path));

                for (String mail : tempMailList){
                    bw.write(mail + System.lineSeparator());
                }

                bw.close();

            }catch (Exception e) {
                throw new InternalError();
            } finally {
                writeLock.unlock();
            }

        }
    }

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

}
