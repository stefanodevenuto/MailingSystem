package progetto.server;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import progetto.common.Mail;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public List<Mail> getMailboxMailist(String address) {
        Mailbox m = mailboxList.get(address);
        if(m == null){
            //TODO: gestire la non esistenza di un utente
            System.out.println("UTENTE NON ESISTE");
            return null;
        }
        return m.getMailList();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private class Mailbox {
        private AtomicInteger SKIP_LINES = new AtomicInteger(0);

        private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        private Lock readLock = readWriteLock.readLock();
        private Lock writeLock = readWriteLock.writeLock();

        private String address;
        private int tokenID;

        private Mailbox(String address) {
            this.address = address;
        }

        private List<Mail> getMailList() {
            List<Mail> mailList = new ArrayList<>();
            readLock.lock();
            try {
                Reader reader = Files.newBufferedReader(Paths.get("C:\\Users\\stefa\\Desktop\\" + address + ".csv"));

                CsvToBean<Mail> csvToBean = new CsvToBeanBuilder<Mail>(reader)
                        .withType(Mail.class)
                        .withIgnoreLeadingWhiteSpace(true)
                        .withSkipLines(SKIP_LINES.get())
                        .build();

                mailList = csvToBean.parse();
                SKIP_LINES.getAndAdd(mailList.size());
                reader.close();

            }catch (Exception e) {
                e.printStackTrace();
            } finally {
                readLock.unlock();
                return mailList;
            }
        }

        private void updateMailList() {

        }
    }
}
