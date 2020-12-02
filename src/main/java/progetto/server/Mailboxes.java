package progetto.server;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import progetto.common.Mail;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Mailboxes {
    private static Map<String, Mailbox> mailboxList;

    public Mailboxes() {
        mailboxList = new HashMap<>();
        newMailbox("first@gmail.com");
        newMailbox("second@gmail.com");
        newMailbox("third@gmail.com");
    }

    private Mailbox newMailbox(String address) {
        Mailbox m = new Mailbox(address);
        mailboxList.put(address, m);
        return m;
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private class Mailbox {
        private AtomicInteger SKIP_LINES = new AtomicInteger(0);

        private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        private Lock readLock = readWriteLock.readLock();
        private Lock writeLock = readWriteLock.writeLock();

        private String address;

        private Mailbox(String address) {
            this.address = address;
        }

        private List<Mail> getMailList(boolean mode) {
            List<Mail> mailList = new ArrayList<>();
            readLock.lock();
            try {
                Reader reader = Files.newBufferedReader(Paths.get("C:\\Users\\stefa\\Desktop\\" + address + ".csv"));
                CsvToBean<Mail> csvToBean;
                if(mode){
                    csvToBean = new CsvToBeanBuilder<Mail>(reader)
                            .withType(Mail.class)
                            .withIgnoreLeadingWhiteSpace(true)
                            .withSkipLines(SKIP_LINES.get())
                            .build();
                }else{
                    csvToBean = new CsvToBeanBuilder<Mail>(reader)
                            .withType(Mail.class)
                            .withIgnoreLeadingWhiteSpace(true)
                            .build();
                }

                mailList = csvToBean.parse();
                SKIP_LINES.getAndAdd(mailList.size());
                reader.close();

                // TODO: cancellazione in posizione < SKIP_LINES ==>    SKIP_LINES--;
                //       altrimenti                              ==>    nulla

            }catch (Exception e) {
                e.printStackTrace();
            } finally {
                readLock.unlock();
                return mailList;
            }
        }

        private void updateMailList(Mail m) {
            writeLock.lock();
            try {
                Writer writer = Files.newBufferedWriter(
                        Paths.get("C:\\Users\\stefa\\Desktop\\" + address + ".csv"),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );

                StatefulBeanToCsv<Mail> beanToCsv = new StatefulBeanToCsvBuilder<Mail>(writer)
                        //.withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                        .build();

                beanToCsv.write(m);
                writer.close();
            }catch (Exception e) {
                e.printStackTrace();
            } finally {
                writeLock.unlock();
            }
        }
    }
}
