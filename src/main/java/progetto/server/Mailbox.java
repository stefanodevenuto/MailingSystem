package progetto.server;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import progetto.common.Mail;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Mailbox {
    private static int SKIP_LINES = 0;
    private String address;
    //private List<Mail> mailList;

    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private Lock readLock = readWriteLock.readLock();
    private Lock writeLock = readWriteLock.writeLock();

    public Mailbox(String address) {
        this.address = address;
        //this.mailList = mailList;
    }

    public List<Mail> getMailList() {
        List<Mail> mailbox = new ArrayList<>();
        readLock.lock();
        try {
            Reader reader = Files.newBufferedReader(Paths.get("C:\\Users\\stefa\\Desktop\\" + address + ".csv"));

            CsvToBean<Mail> csvToBean = new CsvToBeanBuilder<Mail>(reader)
                    .withType(Mail.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            mailbox = csvToBean.parse();
            reader.close();

        }catch (Exception e) {
            e.printStackTrace();
        } finally {
            readLock.unlock();
            return mailbox;
        }
    }

    public void updateMailList() {

    }
}




