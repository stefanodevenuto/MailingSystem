package progetto.server;

import com.opencsv.bean.*;
import com.opencsv.exceptions.CsvConstraintViolationException;
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

    public Mailboxes(String path) throws IOException {
        mailboxList = new HashMap<>();
        try(Scanner users = new Scanner(new File(path))) {
            while (users.hasNextLine()){
                String user = users.nextLine();
                System.out.println(user);
                newMailbox(user);
            }
        }
    }

    private void newMailbox(String address) {
        Mailbox m = new Mailbox(address);
        mailboxList.put(address, m);
    }

    public ObservableList<Log> logsProperty() {
        return logs;
    }
    public void setLogs(ObservableList<Log> current) {
        logs = current;
    }

    public List<Mail> getMailboxMailist(String address, boolean mode) throws NoSuchElementException{
        Mailbox m = mailboxList.get(address);
        if(m == null){
            System.out.println("GetMailList: UTENTE NON ESISTE");
            throw new NoSuchElementException();
        }
        return m.getMailList(mode);
    }

    public void updateMailboxMailist(String address, Mail mail) throws NoSuchElementException{
        Mailbox m = mailboxList.get(address);
        if(m == null){
            System.out.println("Update: UTENTE NON ESISTE");
            throw new NoSuchElementException();
        }
        m.updateMailList(mail);
    }

    public void deleteMailboxMail(String address, int mailID) throws NoSuchElementException{
        Mailbox m = mailboxList.get(address);
        if(m == null){
            System.out.println("Update: UTENTE NON ESISTE");
            throw new NoSuchElementException();
        }
        m.deleteMail(mailID);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private class Mailbox {
        private AtomicInteger SKIP_LINES = new AtomicInteger(0); // TODO: valutare se assegnare questo alla singola connessione
                                                                          // per evitare problemi sulla connessione dello stesso account contemporaneamente
        private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        private final Lock readLock = readWriteLock.readLock();
        private final Lock writeLock = readWriteLock.writeLock();

        private final String path;
        private final AtomicInteger emailCounter = new AtomicInteger();

        private Mailbox(String address) {
            this.path = "C:\\Users\\stefa\\Desktop\\" + address + ".csv";
            try(BufferedReader br = new BufferedReader(new FileReader(path))) {

                String lastLine = "";
                String currentLine = "";

                while ((currentLine = br.readLine()) != null) {
                    //System.out.println(currentLine);
                    lastLine = currentLine;
                }

                // Get the last ID by matching the first value inside double quotes
                Matcher m = Pattern.compile("(?<=\")([^\"]+)(?=\")").matcher(lastLine);
                m.find();

                emailCounter.set(Integer.parseInt(m.group(1)) + 1);
            } catch (IOException e){
                System.out.println("IOException");
                emailCounter.set(0);
            }

        }

        private List<Mail> getMailList(boolean mode) {
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

                if(mode){
                    csvToBean = new CsvToBeanBuilder<Mail>(bufferedReader)
                            .withType(Mail.class)
                            .withIgnoreLeadingWhiteSpace(true)
                            .withFilter(ignoreEmptyLines)
                            .withSkipLines(SKIP_LINES.get())
                            .build();
                }else{
                    SKIP_LINES.set(0); // To avoid the re-login update list view fail
                    csvToBean = new CsvToBeanBuilder<Mail>(bufferedReader)
                            .withType(Mail.class)
                            .withIgnoreLeadingWhiteSpace(true)
                            .withFilter(ignoreEmptyLines)
                            .build();
                }

                mailList = csvToBean.parse();

                SKIP_LINES.getAndAdd(mailList.size());
                reader.close();

            }catch (Exception e) {
                return mailList;
            } finally {
                readLock.unlock();
                return mailList;
            }
        }

        private void updateMailList(Mail m) {
            m.setID(emailCounter.getAndIncrement());
            m.setText(m.getText().replace("\n", "\\n"));
            writeLock.lock();
            try {
                Writer writer = Files.newBufferedWriter(
                        Paths.get(path),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );

                StatefulBeanToCsv<Mail> beanToCsv = new StatefulBeanToCsvBuilder<Mail>(writer)
                        //.withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                        .withEscapechar('\\')
                        .build();

                beanToCsv.write(m);
                writer.close();
            }catch (Exception e) {
                e.printStackTrace();
            } finally {
                writeLock.unlock();
            }
        }

        private void deleteMail(int mailID){
            writeLock.lock();
            List<Mail> tempMailList = new ArrayList<>();
            try {
                Reader reader = Files.newBufferedReader(Paths.get(path));

                // Ignores empty lines from the input
                CsvToBeanFilter ignoreEmptyLines = strings -> {
                    for (String one : strings) {
                        if (one != null && one.length() > 0) {
                            return true;
                        }
                    }
                    return false;
                };


                CsvToBean<Mail> csvToBean = new CsvToBeanBuilder<Mail>(reader)
                        .withType(Mail.class)
                        .withIgnoreLeadingWhiteSpace(true)
                        .withFilter(ignoreEmptyLines)
                        .withEscapeChar('\0')
                        .build();

                Iterator<Mail> csvMailIterator = csvToBean.iterator();

                int deletedIndex = -1;
                while(csvMailIterator.hasNext()){
                    deletedIndex++;
                    Mail m = csvMailIterator.next();
                    if(m.getID() != mailID){
                        tempMailList.add(m);
                    }
                }

                reader.close();

                System.out.println(tempMailList);

                Writer writer = Files.newBufferedWriter(Paths.get(path));

                StatefulBeanToCsv<Mail> beanToCsv = new StatefulBeanToCsvBuilder<Mail>(writer)
                        .withEscapechar('\0')
                        .build();

                beanToCsv.write(tempMailList);

                writer.close();
                if(deletedIndex < SKIP_LINES.intValue()){
                    SKIP_LINES.decrementAndGet();
                }


                //  cancellazione in posizione < SKIP_LINES ==>    SKIP_LINES--;
                //  altrimenti                              ==>    nulla

            }catch (Exception e) {
                e.printStackTrace();
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
            return line.replace("\\n", "\n");
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


}
