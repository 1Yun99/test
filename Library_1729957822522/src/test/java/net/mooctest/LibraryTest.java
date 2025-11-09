package net.mooctest;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class LibraryTest {

    private static final long ONE_DAY = 24L * 60L * 60L * 1000L;

    private Book createBook(BookType type, int copies) {
        return new Book("图书" + UUID.randomUUID(), "作者", UUID.randomUUID().toString(), type, copies);
    }

    private RegularUser createRegularUser(String name) {
        RegularUser user = new RegularUser(name, "REG-" + UUID.randomUUID());
        user.setEmail(name + "@example.com");
        user.setPhoneNumber("1380000" + (int) (Math.random() * 1000));
        return user;
    }

    private VIPUser createVipUser(String name) {
        VIPUser user = new VIPUser(name, "VIP-" + UUID.randomUUID());
        user.setEmail(name + "@vip.com");
        user.setPhoneNumber("1390000" + (int) (Math.random() * 1000));
        return user;
    }

    private Date day(int index) {
        return new Date(index * ONE_DAY);
    }

    private Date nowPlusDays(int offset) {
        return new Date(System.currentTimeMillis() + offset * ONE_DAY);
    }

    private String captureOutput(Runnable runnable) {
        PrintStream original = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        try {
            runnable.run();
        } finally {
            System.setOut(original);
        }
        return out.toString();
    }

    private BorrowRecord addBorrowRecord(User user, Book book, int borrowDay, int dueDay) {
        BorrowRecord record = new BorrowRecord(book, user, day(borrowDay), day(dueDay));
        user.borrowedBooks.add(record);
        return record;
    }

    private BorrowRecord addBorrowRecord(User user, Book book, Date borrowDate, Date dueDate) {
        BorrowRecord record = new BorrowRecord(book, user, borrowDate, dueDate);
        user.borrowedBooks.add(record);
        return record;
    }

    private static class AlwaysAvailableBook extends Book {
        public AlwaysAvailableBook(String title, String author, String isbn, BookType type, int totalCopies) {
            super(title, author, isbn, type, totalCopies);
        }

        @Override
        public boolean isAvailable() {
            return true;
        }
    }

    // ---------------------- Book 与 BorrowRecord 测试 ----------------------

    @Test
    public void testBookAvailabilityAndOperations() throws Exception {
        // 测试目的：全面验证图书可用性、借还流程以及损坏和预约处理。
        Book book = createBook(BookType.GENERAL, 2);

        // 正常情况下可借阅
        assertTrue(book.isAvailable());

        // 维修状态不可借阅
        book.setInRepair(true);
        assertFalse(book.isAvailable());
        book.setInRepair(false);

        // 损坏状态不可借阅
        book.setDamaged(true);
        assertFalse(book.isAvailable());
        book.setDamaged(false);

        // 库存为零时不可借阅
        book.setAvailableCopies(0);
        assertFalse(book.isAvailable());
        book.setAvailableCopies(2);

        // 成功借书后库存减少
        book.borrow();
        assertEquals(1, book.getAvailableCopies());

        // 归还后库存恢复，再次归还触发异常
        book.returnBook();
        assertEquals(2, book.getAvailableCopies());
        try {
            book.returnBook();
            fail("库存已满时应该抛出异常");
        } catch (InvalidOperationException expected) {
            // 捕获预期异常
        }

        // 库存不足时借阅抛出异常
        book.setAvailableCopies(0);
        try {
            book.borrow();
            fail("库存不足时应该抛出异常");
        } catch (BookNotAvailableException expected) {
            // 捕获预期异常
        }
        book.setAvailableCopies(1);

        // 报损与报修流程覆盖
        book.reportDamage();
        assertTrue(book.isDamaged());
        book.reportDamage();
        book.setDamaged(false);

        book.reportRepair();
        book.reportRepair();
        book.setInRepair(false);

        // 预约新增与移除逻辑
        RegularUser user = createRegularUser("图书预约测试用户");
        Reservation reservation = new Reservation(book, user);
        book.addReservation(reservation);
        assertEquals(1, book.getReservationQueue().size());
        book.removeReservation(reservation);
        assertTrue(book.getReservationQueue().isEmpty());
        book.removeReservation(reservation);
    }

    @Test
    public void testBorrowRecordFineScenarios() {
        // 测试目的：覆盖借阅记录不同类型图书的罚款计算路径。
        Book generalBook = createBook(BookType.GENERAL, 1);
        RegularUser user = createRegularUser("罚款测试用户");

        // 普通图书逾期两天
        BorrowRecord generalRecord = new BorrowRecord(generalBook, user, day(0), day(5));
        generalRecord.setReturnDate(day(7));
        assertEquals(2.0, generalRecord.calculateFine(), 0.0);
        assertEquals(2.0, generalRecord.getFineAmount(), 0.0);

        // 提前归还不罚款
        BorrowRecord earlyRecord = new BorrowRecord(createBook(BookType.GENERAL, 1), user, day(0), day(5));
        earlyRecord.setReturnDate(day(3));
        assertEquals(0.0, earlyRecord.calculateFine(), 0.0);

        // 未归还仍不罚款
        BorrowRecord notReturned = new BorrowRecord(createBook(BookType.GENERAL, 1), user, day(0), day(5));
        assertEquals(0.0, notReturned.calculateFine(), 0.0);

        // 珍本图书与期刊的不同罚款标准
        BorrowRecord rareRecord = new BorrowRecord(createBook(BookType.RARE, 1), user, day(0), day(5));
        rareRecord.setReturnDate(day(6));
        assertEquals(5.0, rareRecord.calculateFine(), 0.0);

        BorrowRecord journalRecord = new BorrowRecord(createBook(BookType.JOURNAL, 1), user, day(0), day(5));
        journalRecord.setReturnDate(day(6));
        assertEquals(2.0, journalRecord.calculateFine(), 0.0);
    }

    @Test
    public void testBorrowRecordExtendAndFineAmount() {
        // 测试目的：验证续期以及黑名单和损坏叠加罚款场景。
        Book damagedBook = createBook(BookType.GENERAL, 1);
        damagedBook.setDamaged(true);
        RegularUser blacklistedUser = createRegularUser("黑名单用户");
        blacklistedUser.setAccountStatus(AccountStatus.BLACKLISTED);
        BorrowRecord penaltyRecord = new BorrowRecord(damagedBook, blacklistedUser, day(0), day(5));
        penaltyRecord.setReturnDate(day(7));
        assertEquals(104.0, penaltyRecord.calculateFine(), 0.0);

        Book extendBook = createBook(BookType.EBOOK, 1);
        RegularUser extendUser = createRegularUser("续期用户");
        BorrowRecord extendRecord = new BorrowRecord(extendBook, extendUser, day(0), day(5));
        extendRecord.extendDueDate(4);
        assertEquals(day(9), extendRecord.getDueDate());
    }

    // ---------------------- User 行为测试 ----------------------

    @Test
    public void testUserPaymentAndScoreAdjustments() {
        // 测试目的：验证支付罚款、积分增减以及黑名单通知的综合逻辑。
        RegularUser user = createRegularUser("支付用户");

        // 支付超过欠款金额应抛出异常
        user.fines = 20;
        try {
            user.payFine(30);
            fail("支付金额超过欠款时应抛出异常");
        } catch (IllegalArgumentException expected) {
            // 预期异常
        }

        // 黑名单用户无法支付罚款
        user.setAccountStatus(AccountStatus.BLACKLISTED);
        try {
            user.payFine(10);
            fail("黑名单用户支付罚款应抛出异常");
        } catch (IllegalStateException expected) {
            // 预期异常
        }

        // 解冻用户并逐步清除罚款
        user.setAccountStatus(AccountStatus.FROZEN);
        user.fines = 20;
        user.payFine(10);
        assertEquals(10.0, user.getFines(), 0.0);
        user.payFine(10);
        assertEquals(0.0, user.getFines(), 0.0);
        assertEquals(AccountStatus.ACTIVE, user.getAccountStatus());

        // 积分增减逻辑
        int before = user.getCreditScore();
        user.addScore(5);
        assertEquals(before + 5, user.getCreditScore());

        user.creditScore = 3;
        user.deductScore(10);
        assertEquals(0, user.getCreditScore());
        assertEquals(AccountStatus.FROZEN, user.getAccountStatus());

        // 黑名单用户加分抛异常并无法收到通知
        user.setAccountStatus(AccountStatus.BLACKLISTED);
        try {
            user.addScore(1);
            fail("黑名单用户加分应抛出异常");
        } catch (IllegalStateException expected) {
            // 预期异常
        }
        String output = captureOutput(() -> user.receiveNotification("系统通知"));
        assertTrue(output.contains("Blacklisted users cannot receive notifications."));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testUserReservationFlows() throws Exception {
        // 测试目的：验证预约的多种异常场景与成功流程。
        RegularUser user = createRegularUser("预约用户");
        Book book = createBook(BookType.GENERAL, 1);

        user.setAccountStatus(AccountStatus.BLACKLISTED);
        try {
            user.reserveBook(book);
            fail("黑名单用户预约应抛出异常");
        } catch (IllegalStateException expected) {
            // 预期异常
        }

        user.setAccountStatus(AccountStatus.FROZEN);
        try {
            user.reserveBook(book);
            fail("冻结用户预约应抛出异常");
        } catch (AccountFrozenException expected) {
            // 预期异常
        }

        user.setAccountStatus(AccountStatus.ACTIVE);
        user.creditScore = 40;
        try {
            user.reserveBook(book);
            fail("信用不足应抛出异常");
        } catch (InsufficientCreditException expected) {
            // 预期异常
        }

        user.creditScore = 80;
        List rawReservations = (List) user.reservations;
        rawReservations.clear();
        rawReservations.add(book);
        try {
            user.reserveBook(book);
            fail("重复预约应抛出异常");
        } catch (ReservationNotAllowedException expected) {
            // 预期异常
        }
        rawReservations.clear();

        book.setInRepair(true);
        user.reserveBook(book);
        assertEquals(1, book.getReservationQueue().size());
        assertEquals(1, user.reservations.size());

        user.cancelReservation(book);
        assertTrue(book.getReservationQueue().isEmpty());
        assertTrue(user.reservations.isEmpty());

        try {
            user.cancelReservation(book);
            fail("未预约时取消应抛出异常");
        } catch (InvalidOperationException expected) {
            // 预期异常
        }
    }

    // ---------------------- RegularUser 行为测试 ----------------------

    @Test
    public void testRegularUserBorrowFailureBranches() throws Exception {
        // 测试目的：覆盖普通用户借书的各种失败场景。
        RegularUser blacklisted = createRegularUser("黑名单借阅用户");
        blacklisted.setAccountStatus(AccountStatus.BLACKLISTED);
        try {
            blacklisted.borrowBook(createBook(BookType.GENERAL, 1));
            fail("黑名单用户借书应抛出异常");
        } catch (IllegalStateException expected) {
            // 预期异常
        }

        RegularUser frozen = createRegularUser("冻结借阅用户");
        frozen.setAccountStatus(AccountStatus.FROZEN);
        try {
            frozen.borrowBook(createBook(BookType.GENERAL, 1));
            fail("冻结用户借书应抛出异常");
        } catch (AccountFrozenException expected) {
            // 预期异常
        }

        RegularUser limitUser = createRegularUser("超限用户");
        for (int i = 0; i < 5; i++) {
            BorrowRecord record = addBorrowRecord(limitUser, createBook(BookType.GENERAL, 1), day(i), day(i + 5));
            assertNotNull(limitUser.findBorrowRecord(record.getBook()));
        }
        try {
            limitUser.borrowBook(createBook(BookType.GENERAL, 1));
            fail("超出数量上限应抛出异常");
        } catch (InvalidOperationException expected) {
            // 预期异常
        }

        RegularUser fineUser = createRegularUser("罚款过高用户");
        fineUser.fines = 60;
        try {
            fineUser.borrowBook(createBook(BookType.GENERAL, 1));
            fail("罚款过高应抛出异常");
        } catch (OverdueFineException expected) {
            assertEquals(AccountStatus.FROZEN, fineUser.getAccountStatus());
            assertTrue(fineUser.getFines() > 50);
        }

        RegularUser unavailableUser = createRegularUser("库存不足用户");
        Book unavailableBook = createBook(BookType.GENERAL, 1);
        unavailableBook.setAvailableCopies(0);
        try {
            unavailableUser.borrowBook(unavailableBook);
            fail("库存不足应抛出异常");
        } catch (BookNotAvailableException expected) {
            // 预期异常
        }

        RegularUser lowCreditUser = createRegularUser("信用不足用户");
        lowCreditUser.creditScore = 50;
        try {
            lowCreditUser.borrowBook(createBook(BookType.GENERAL, 1));
            fail("信用不足借书应抛出异常");
        } catch (InsufficientCreditException expected) {
            assertEquals(50, lowCreditUser.getCreditScore());
        }

        RegularUser rareUser = createRegularUser("珍本限制用户");
        try {
            rareUser.borrowBook(createBook(BookType.RARE, 1));
            fail("普通用户借珍本应抛出异常");
        } catch (InvalidOperationException expected) {
            // 预期异常
        }

        RegularUser reserveUser = createRegularUser("预约触发用户");
        AlwaysAvailableBook special = new AlwaysAvailableBook("特例图书", "作者", "ISBN", BookType.GENERAL, 1);
        special.setAvailableCopies(0);
        reserveUser.borrowBook(special);
        assertEquals(1, special.getReservationQueue().size());
        assertTrue("触发预约时不应新增借阅记录", reserveUser.getBorrowedBooks().isEmpty());
    }

    @Test
    public void testRegularUserBorrowSuccessAndReturn() throws Exception {
        // 测试目的：验证普通用户正常借还流程与积分奖励。
        RegularUser user = createRegularUser("正常借阅用户");
        Book book = createBook(BookType.GENERAL, 2);

        user.borrowBook(book);
        assertEquals(1, user.getBorrowedBooks().size());
        assertEquals(101, user.getCreditScore());
        assertEquals(1, book.getAvailableCopies());

        user.returnBook(book);
        assertTrue(user.getBorrowedBooks().isEmpty());
        assertEquals(103, user.getCreditScore());
        assertEquals(2, book.getAvailableCopies());
    }

    @Test
    public void testRegularUserFindBorrowRecordUtility() {
        // 测试目的：验证普通用户查询借阅记录的辅助方法。
        RegularUser user = createRegularUser("记录查询用户");
        Book book = createBook(BookType.GENERAL, 1);
        assertNull(user.findBorrowRecord(book));
        BorrowRecord record = addBorrowRecord(user, book, day(1), day(5));
        assertEquals(record, user.findBorrowRecord(book));
        user.borrowedBooks.remove(record);
        assertNull(user.findBorrowRecord(book));
    }

    @Test
    public void testRegularUserCalculateDueDateHelper() {
        // 测试目的：验证普通用户的到期日计算逻辑。
        RegularUser user = createRegularUser("日期工具用户");
        Date borrowDate = day(2);
        Date dueDate = user.calculateDueDate(borrowDate, 14);
        assertEquals(day(16), dueDate);
        assertEquals(14, (dueDate.getTime() - borrowDate.getTime()) / ONE_DAY);
    }

    @Test
    public void testRegularUserReturnOverdueAndFineHandling() throws Exception {
        // 测试目的：验证逾期归还的扣分、冻结与高额罚款处理。
        RegularUser overdueUser = createRegularUser("逾期用户");
        overdueUser.creditScore = 53;
        overdueUser.setAccountStatus(AccountStatus.ACTIVE);
        Book overdueBook = createBook(BookType.GENERAL, 1);
        overdueBook.setTotalCopies(1);
        overdueBook.setAvailableCopies(0);
        assertNull(overdueUser.findBorrowRecord(overdueBook));
        BorrowRecord overdueRecord = addBorrowRecord(overdueUser, overdueBook, nowPlusDays(-10), nowPlusDays(-3));
        assertEquals(overdueRecord, overdueUser.findBorrowRecord(overdueBook));
        assertEquals(0, overdueRecord.getFineAmount(), 0.0);
        overdueUser.returnBook(overdueBook);
        assertTrue(overdueUser.getFines() > 0);
        assertEquals(48, overdueUser.getCreditScore());
        assertEquals(AccountStatus.FROZEN, overdueUser.getAccountStatus());

        RegularUser highFineUser = createRegularUser("高额罚款用户");
        highFineUser.fines = 95;
        highFineUser.setAccountStatus(AccountStatus.ACTIVE);
        Book highFineBook = createBook(BookType.GENERAL, 1);
        highFineBook.setTotalCopies(1);
        highFineBook.setAvailableCopies(0);
        BorrowRecord highFineRecord = addBorrowRecord(highFineUser, highFineBook, nowPlusDays(-10), nowPlusDays(-2));
        assertEquals(highFineRecord, highFineUser.findBorrowRecord(highFineBook));
        try {
            highFineUser.returnBook(highFineBook);
            fail("罚款过高应抛出异常");
        } catch (OverdueFineException expected) {
            assertEquals(AccountStatus.FROZEN, highFineUser.getAccountStatus());
        }
        assertTrue(highFineUser.getFines() > 100);
    }

    @Test
    public void testRegularUserReturnPrintsOverdueMessage() throws Exception {
        // 测试目的：验证归还超过借阅期时会输出提示信息。
        RegularUser user = createRegularUser("长借期用户");
        Book book = createBook(BookType.GENERAL, 1);
        book.setTotalCopies(1);
        book.setAvailableCopies(0);
        addBorrowRecord(user, book, nowPlusDays(-20), nowPlusDays(-5));
        String output = captureOutput(() -> {
            try {
                user.returnBook(book);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertTrue(output.contains("Return the book"));
    }

    // ---------------------- VIPUser 行为测试 ----------------------

    @Test
    public void testVipUserBorrowFailureBranches() throws Exception {
        // 测试目的：覆盖 VIP 用户借书的各种失败场景。
        VIPUser blacklisted = createVipUser("VIP黑名单用户");
        blacklisted.setAccountStatus(AccountStatus.BLACKLISTED);
        try {
            blacklisted.borrowBook(createBook(BookType.GENERAL, 1));
            fail("黑名单 VIP 借书应抛出异常");
        } catch (IllegalStateException expected) {
            // 预期异常
        }

        VIPUser frozen = createVipUser("VIP冻结用户");
        frozen.setAccountStatus(AccountStatus.FROZEN);
        try {
            frozen.borrowBook(createBook(BookType.GENERAL, 1));
            fail("冻结 VIP 借书应抛出异常");
        } catch (AccountFrozenException expected) {
            // 预期异常
        }

        VIPUser limitUser = createVipUser("VIP超限用户");
        for (int i = 0; i < 10; i++) {
            addBorrowRecord(limitUser, createBook(BookType.GENERAL, 1), 0, 5);
        }
        try {
            limitUser.borrowBook(createBook(BookType.GENERAL, 1));
            fail("超过上限应抛出异常");
        } catch (InvalidOperationException expected) {
            // 预期异常
        }

        VIPUser fineUser = createVipUser("VIP罚款用户");
        fineUser.fines = 60;
        try {
            fineUser.borrowBook(createBook(BookType.GENERAL, 1));
            fail("罚款过高应抛出异常");
        } catch (OverdueFineException expected) {
            assertEquals(AccountStatus.FROZEN, fineUser.getAccountStatus());
        }

        VIPUser unavailableUser = createVipUser("VIP库存不足");
        Book unavailableBook = createBook(BookType.GENERAL, 1);
        unavailableBook.setAvailableCopies(0);
        try {
            unavailableUser.borrowBook(unavailableBook);
            fail("库存不足应抛出异常");
        } catch (BookNotAvailableException expected) {
            // 预期异常
        }

        VIPUser lowCreditUser = createVipUser("VIP信用不足");
        lowCreditUser.creditScore = 40;
        try {
            lowCreditUser.borrowBook(createBook(BookType.GENERAL, 1));
            fail("信用不足借书应抛出异常");
        } catch (InsufficientCreditException expected) {
            // 预期异常
        }
    }

    @Test
    public void testVipUserBorrowSuccessAndReturnFlows() throws Exception {
        // 测试目的：验证 VIP 用户借阅珍本、按时归还与逾期归还逻辑。
        VIPUser vipUser = createVipUser("正常VIP");
        Book rareBook = createBook(BookType.RARE, 2);

        vipUser.borrowBook(rareBook);
        assertEquals(1, vipUser.getBorrowedBooks().size());
        assertEquals(102, vipUser.getCreditScore());
        assertEquals(1, rareBook.getAvailableCopies());

        vipUser.returnBook(rareBook);
        assertTrue(vipUser.getBorrowedBooks().isEmpty());
        assertEquals(105, vipUser.getCreditScore());
        assertEquals(2, rareBook.getAvailableCopies());

        // 逾期归还扣分
        Book overdueBook = createBook(BookType.GENERAL, 1);
        overdueBook.setTotalCopies(1);
        overdueBook.setAvailableCopies(0);
        BorrowRecord vipOverdueRecord = addBorrowRecord(vipUser, overdueBook, nowPlusDays(-10), nowPlusDays(-1));
        assertEquals(vipOverdueRecord, vipUser.findBorrowRecord(overdueBook));
        vipUser.setAccountStatus(AccountStatus.ACTIVE);
        vipUser.returnBook(overdueBook);
        assertTrue(vipUser.getFines() > 0);
        assertEquals(102, vipUser.getCreditScore());

        // 高额罚款抛异常
        VIPUser highFineVip = createVipUser("高额罚款VIP");
        highFineVip.fines = 99;
        highFineVip.setAccountStatus(AccountStatus.ACTIVE);
        Book highFineBook = createBook(BookType.GENERAL, 1);
        highFineBook.setTotalCopies(1);
        highFineBook.setAvailableCopies(0);
        addBorrowRecord(highFineVip, highFineBook, nowPlusDays(-10), nowPlusDays(-2));
        try {
            highFineVip.returnBook(highFineBook);
            fail("高额罚款应抛出异常");
        } catch (OverdueFineException expected) {
            assertEquals(AccountStatus.FROZEN, highFineVip.getAccountStatus());
        }
    }

    @Test(expected = InvalidOperationException.class)
    public void testVipUserReturnFailsWhenNotBorrowed() throws Exception {
        // 测试目的：验证未借阅的图书归还会抛出异常。
        VIPUser vipUser = createVipUser("未借阅VIP");
        Book book = createBook(BookType.GENERAL, 1);
        vipUser.returnBook(book);
    }

    @Test
    public void testVipUserFindBorrowRecordUtility() {
        // 测试目的：验证 VIP 用户的借阅记录查询逻辑。
        VIPUser vipUser = createVipUser("VIP记录用户");
        Book book = createBook(BookType.GENERAL, 1);
        assertNull(vipUser.findBorrowRecord(book));
        BorrowRecord record = addBorrowRecord(vipUser, book, day(0), day(6));
        assertEquals(record, vipUser.findBorrowRecord(book));
        vipUser.borrowedBooks.clear();
        assertNull(vipUser.findBorrowRecord(book));
    }

    @Test
    public void testVipUserCalculateDueDateHelper() {
        // 测试目的：验证 VIP 用户的到期日计算逻辑。
        VIPUser vipUser = createVipUser("VIP日期用户");
        Date borrowDate = day(3);
        Date dueDate = vipUser.calculateDueDate(borrowDate, 30);
        assertEquals(day(33), dueDate);
        assertEquals(30, (dueDate.getTime() - borrowDate.getTime()) / ONE_DAY);
    }

    @Test
    public void testVipUserExtendBorrowPeriodScenarios() throws Exception {
        // 测试目的：验证 VIP 用户续借的成功与异常分支。
        VIPUser vipUser = createVipUser("续借VIP");
        Book book = createBook(BookType.GENERAL, 1);
        BorrowRecord record = addBorrowRecord(vipUser, book, day(0), day(10));
        assertEquals(record, vipUser.findBorrowRecord(book));

        vipUser.extendBorrowPeriod(book);
        assertEquals(day(17), record.getDueDate());
        assertEquals(7, (record.getDueDate().getTime() - day(10).getTime()) / ONE_DAY);

        try {
            vipUser.extendBorrowPeriod(book);
            fail("重复续借应抛出异常");
        } catch (InvalidOperationException expected) {
            // 预期异常
        }

        try {
            vipUser.extendBorrowPeriod(createBook(BookType.GENERAL, 1));
            fail("未借阅图书续借应抛出异常");
        } catch (InvalidOperationException expected) {
            // 预期异常
        }
    }

    // ---------------------- Reservation 测试 ----------------------

    @Test
    public void testReservationPriorityCalculation() {
        // 测试目的：验证预约优先级的提升、降低与黑名单处理。
        RegularUser normalUser = createRegularUser("普通预约");
        normalUser.creditScore = 70;
        Book book = createBook(BookType.GENERAL, 1);
        BorrowRecord overdueRecord = new BorrowRecord(book, normalUser, day(0), day(5));
        overdueRecord.setReturnDate(day(7));
        normalUser.borrowedBooks.add(overdueRecord);
        Reservation normalReservation = new Reservation(book, normalUser);
        assertEquals(65, normalReservation.getPriority());

        VIPUser vipUser = createVipUser("VIP预约");
        vipUser.creditScore = 80;
        Reservation vipReservation = new Reservation(book, vipUser);
        assertEquals(90, vipReservation.getPriority());
        assertTrue(vipReservation.getUser() instanceof VIPUser);

        RegularUser blacklisted = createRegularUser("黑名单预约");
        blacklisted.setAccountStatus(AccountStatus.BLACKLISTED);
        Reservation blockedReservation = new Reservation(book, blacklisted);
        assertEquals(-1, blockedReservation.getPriority());
    }

    @Test
    public void testReservationPriorityCalculationMultipleOverdues() {
        // 测试目的：验证多次逾期会累积降低预约优先级。
        RegularUser user = createRegularUser("多次逾期用户");
        user.creditScore = 90;
        Book book = createBook(BookType.GENERAL, 1);
        BorrowRecord overdue1 = new BorrowRecord(book, user, day(0), day(5));
        overdue1.setReturnDate(day(7));
        BorrowRecord overdue2 = new BorrowRecord(book, user, day(10), day(15));
        overdue2.setReturnDate(day(20));
        user.borrowedBooks.add(overdue1);
        user.borrowedBooks.add(overdue2);
        Reservation reservation = new Reservation(book, user);
        assertEquals(80, reservation.getPriority());
    }

    // ---------------------- AutoRenewal 与 CreditRepair 测试 ----------------------

    @Test
    public void testAutoRenewalServiceFailureBranches() throws Exception {
        // 测试目的：验证自动续借的多种失败分支。
        AutoRenewalService service = new AutoRenewalService();
        RegularUser user = createRegularUser("续借失败用户");
        Book book = createBook(BookType.GENERAL, 1);

        user.setAccountStatus(AccountStatus.FROZEN);
        try {
            service.autoRenew(user, book);
            fail("冻结账号续借应抛出异常");
        } catch (AccountFrozenException expected) {
            // 预期异常
        }
        user.setAccountStatus(AccountStatus.ACTIVE);

        book.addReservation(new Reservation(book, createRegularUser("排队用户")));
        try {
            service.autoRenew(user, book);
            fail("有预约时续借应抛出异常");
        } catch (InvalidOperationException expected) {
            // 预期异常
        }
        book.getReservationQueue().clear();

        user.creditScore = 50;
        try {
            service.autoRenew(user, book);
            fail("信用不足续借应抛出异常");
        } catch (InsufficientCreditException expected) {
            // 预期异常
        }
        user.creditScore = 80;

        try {
            service.autoRenew(user, book);
            fail("无借阅记录续借应抛出异常");
        } catch (InvalidOperationException expected) {
            // 预期异常
        }
    }

    @Test
    public void testAutoRenewalServiceSuccess() throws Exception {
        // 测试目的：验证自动续借成功会延长到期时间。
        AutoRenewalService service = new AutoRenewalService();
        RegularUser user = createRegularUser("续借成功用户");
        user.creditScore = 80;
        user.setAccountStatus(AccountStatus.ACTIVE);
        Book book = createBook(BookType.GENERAL, 1);
        BorrowRecord record = addBorrowRecord(user, book, day(0), day(10));
        Date before = record.getDueDate();
        service.autoRenew(user, book);
        assertTrue(record.getDueDate().after(before));
        assertEquals(day(24), record.getDueDate());
        assertEquals(14, (record.getDueDate().getTime() - before.getTime()) / ONE_DAY);
    }

    @Test
    public void testCreditRepairServiceScenarios() {
        // 测试目的：验证信用修复的失败与成功逻辑。
        CreditRepairService service = new CreditRepairService();
        RegularUser user = createRegularUser("信用修复用户");
        user.creditScore = 55;
        user.setAccountStatus(AccountStatus.FROZEN);

        try {
            service.repairCredit(user, 5);
            fail("支付金额不足应抛出异常");
        } catch (InvalidOperationException expected) {
            // 预期异常
        }

        service.repairCredit(user, 50);
        assertEquals(60, user.getCreditScore());
        assertEquals(AccountStatus.ACTIVE, user.getAccountStatus());
    }

    @Test
    public void testCreditRepairDoesNotUnfreezeWhenScoreStillLow() throws Exception {
        // 测试目的：验证支付后信用仍不足时账号不会被激活。
        CreditRepairService service = new CreditRepairService();
        RegularUser user = createRegularUser("低分修复用户");
        user.creditScore = 30;
        user.setAccountStatus(AccountStatus.FROZEN);
        service.repairCredit(user, 20);
        assertEquals(32, user.getCreditScore());
        assertEquals(AccountStatus.FROZEN, user.getAccountStatus());
    }

    @Test(expected = IllegalStateException.class)
    public void testCreditRepairFailsForBlacklistedUser() throws Exception {
        // 测试目的：验证黑名单用户修复信用会失败。
        CreditRepairService service = new CreditRepairService();
        RegularUser user = createRegularUser("黑名单修复用户");
        user.setAccountStatus(AccountStatus.BLACKLISTED);
        service.repairCredit(user, 20);
    }

    // ---------------------- InventoryService 测试 ----------------------

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testInventoryServiceReportLostAndDamaged() throws Exception {
        // 测试目的：验证库存服务的报失与报损处理。
        InventoryService service = new InventoryService();
        RegularUser user = createRegularUser("库存用户");
        Book book = createBook(BookType.GENERAL, 2);

        try {
            service.reportLost(book, user);
            fail("未借阅报失应抛出异常");
        } catch (InvalidOperationException expected) {
            // 预期异常
        }

        user.fines = 500;
        List borrowedRaw = (List) user.borrowedBooks;
        borrowedRaw.add(book);
        book.setTotalCopies(2);
        book.setAvailableCopies(1);
        service.reportLost(book, user);
        assertEquals(1, book.getTotalCopies());
        assertEquals(0, book.getAvailableCopies());
        assertEquals(400.0, user.getFines(), 0.0);

        Book damagedBook = createBook(BookType.GENERAL, 1);
        try {
            service.reportDamaged(damagedBook, user);
            fail("未借阅报损应抛出异常");
        } catch (InvalidOperationException expected) {
            // 预期异常
        }

        borrowedRaw.clear();
        borrowedRaw.add(damagedBook);
        user.fines = 50;
        service.reportDamaged(damagedBook, user);
        assertEquals(20.0, user.getFines(), 0.0);
        assertFalse(damagedBook.isAvailable());
    }

    // ---------------------- NotificationService 测试 ----------------------

    @Test
    public void testNotificationServiceRouting() {
        // 测试目的：验证通知服务的多级降级策略。
        NotificationService service = new NotificationService();

        RegularUser blacklisted = createRegularUser("通知黑名单");
        blacklisted.setAccountStatus(AccountStatus.BLACKLISTED);
        String blacklistOutput = captureOutput(() -> service.sendNotification(blacklisted, "消息"));
        assertTrue(blacklistOutput.contains("Blacklisted users cannot receive notifications."));

        RegularUser emailUser = createRegularUser("通知邮件");
        String emailOutput = captureOutput(() -> service.sendNotification(emailUser, "邮件消息"));
        assertTrue(emailOutput.contains("Successfully sent email"));

        RegularUser smsUser = createRegularUser("通知短信");
        smsUser.setEmail("");
        smsUser.setPhoneNumber("13912345678");
        String smsOutput = captureOutput(() -> service.sendNotification(smsUser, "短信消息"));
        assertTrue(smsOutput.contains("Email sending failed"));
        assertTrue(smsOutput.contains("Successfully sent text message"));

        RegularUser appUser = createRegularUser("通知应用");
        appUser.setEmail(null);
        appUser.setPhoneNumber(null);
        String appOutput = captureOutput(() -> service.sendNotification(appUser, "应用消息"));
        assertTrue(appOutput.contains("Text message sending failed"));
        assertTrue(appOutput.contains("Send an in-app notification"));
    }

    @Test
    public void testNotificationServiceIndividualValidation() {
        // 测试目的：验证邮件与短信的参数校验。
        NotificationService service = new NotificationService();
        try {
            service.sendEmail("", "消息");
            fail("缺少邮箱应抛出异常");
        } catch (EmailException expected) {
            // 预期异常
        }
        try {
            service.sendSMS("", "消息");
            fail("缺少手机号应抛出异常");
        } catch (SMSException expected) {
            // 预期异常
        }
    }

    // ---------------------- Library 外观测试 ----------------------

    @Test
    public void testLibraryRegistrationAndBookAddition() {
        // 测试目的：验证图书馆注册与图书入库的多种情况。
        Library library = new Library();
        RegularUser lowCredit = createRegularUser("低信用用户");
        lowCredit.creditScore = 40;
        String rejectOutput = captureOutput(() -> library.registerUser(lowCredit));
        assertTrue(rejectOutput.contains("Credit score is too low"));

        RegularUser normal = createRegularUser("正常注册");
        String registerOutput = captureOutput(() -> library.registerUser(normal));
        assertTrue(registerOutput.contains("Successfully registered user"));
        String duplicateOutput = captureOutput(() -> library.registerUser(normal));
        assertTrue(duplicateOutput.contains("User already exists."));

        Book book = createBook(BookType.GENERAL, 1);
        String addOutput = captureOutput(() -> library.addBook(book));
        assertTrue(addOutput.contains("Successfully added book"));
        String duplicateBookOutput = captureOutput(() -> library.addBook(book));
        assertTrue(duplicateBookOutput.contains("This book already exists."));
    }

    @Test
    public void testLibraryProcessReservationsFlows() {
        // 测试目的：验证图书馆预约处理的成功与失败场景。
        Library library = new Library();
        Book book = createBook(BookType.GENERAL, 1);

        book.setInRepair(true);
        String unavailableOutput = captureOutput(() -> library.processReservations(book));
        assertTrue(unavailableOutput.contains("The book is unavailable"));
        book.setInRepair(false);

        String emptyOutput = captureOutput(() -> library.processReservations(book));
        assertTrue(emptyOutput.isEmpty());

        RegularUser user = createRegularUser("预约成功用户");
        Reservation reservation = new Reservation(book, user);
        book.addReservation(reservation);
        user.reservations.add(reservation);
        book.setAvailableCopies(1);
        String successOutput = captureOutput(() -> library.processReservations(book));
        assertTrue(successOutput.contains("The book ["));
        assertEquals(0, book.getReservationQueue().size());
        assertEquals(1, user.getBorrowedBooks().size());

        RegularUser failedUser = createRegularUser("预约失败用户");
        failedUser.creditScore = 40;
        Reservation failedReservation = new Reservation(book, failedUser);
        book.addReservation(failedReservation);
        failedUser.reservations.add(failedReservation);
        book.setAvailableCopies(1);
        String failedOutput = captureOutput(() -> library.processReservations(book));
        assertTrue(failedOutput.contains("An error occurred while processing the reservation"));
    }

    @Test
    public void testLibraryAutoRenewAndCreditRepairHandlers() {
        // 测试目的：验证图书馆对续借与信用修复的包装逻辑。
        Library library = new Library();
        RegularUser user = createRegularUser("封装续借用户");
        Book book = createBook(BookType.GENERAL, 1);
        addBorrowRecord(user, book, nowPlusDays(-5), nowPlusDays(0));
        user.creditScore = 80;
        user.setAccountStatus(AccountStatus.ACTIVE);

        String renewSuccess = captureOutput(() -> library.autoRenewBook(user, book));
        assertTrue(renewSuccess.contains("Successfully automatically renewed book"));

        user.setAccountStatus(AccountStatus.FROZEN);
        String renewFail = captureOutput(() -> library.autoRenewBook(user, book));
        assertTrue(renewFail.contains("Automatic renewal failed"));

        user.creditScore = 55;
        user.setAccountStatus(AccountStatus.FROZEN);
        String repairFail = captureOutput(() -> library.repairUserCredit(user, 5));
        assertTrue(repairFail.contains("Credit repair failed"));

        String repairSuccess = captureOutput(() -> library.repairUserCredit(user, 100));
        assertTrue(repairSuccess.contains("User credit repair is successful"));
        assertEquals(AccountStatus.ACTIVE, user.getAccountStatus());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testLibraryInventoryHandling() {
        // 测试目的：验证图书馆报失与报损包装逻辑。
        Library library = new Library();
        RegularUser user = createRegularUser("封装库存用户");
        Book book = createBook(BookType.GENERAL, 2);

        String lossFail = captureOutput(() -> library.reportLostBook(user, book));
        assertTrue(lossFail.contains("Reporting loss failed"));

        user.fines = 200;
        List raw = (List) user.borrowedBooks;
        raw.add(book);
        book.setTotalCopies(2);
        book.setAvailableCopies(1);
        String lossSuccess = captureOutput(() -> library.reportLostBook(user, book));
        assertTrue(lossSuccess.contains("Book loss report is successful"));

        raw.clear();
        Book damagedBook = createBook(BookType.GENERAL, 1);
        String damageFail = captureOutput(() -> library.reportDamagedBook(user, damagedBook));
        assertTrue(damageFail.contains("Reporting damage failed"));

        raw.add(damagedBook);
        user.fines = 50;
        String damageSuccess = captureOutput(() -> library.reportDamagedBook(user, damagedBook));
        assertTrue(damageSuccess.contains("Book damage report is successful"));
    }

    // ---------------------- ExternalLibraryAPI 测试 ----------------------

    @Test
    public void testExternalLibraryIntegration() {
        // 测试目的：验证外部图书馆接口的随机性与借书请求。
        boolean seenTrue = false;
        boolean seenFalse = false;
        for (int i = 0; i < 1000 && (!seenTrue || !seenFalse); i++) {
            boolean result = ExternalLibraryAPI.checkAvailability("外部测试图书");
            if (result) {
                seenTrue = true;
            } else {
                seenFalse = true;
            }
        }
        assertTrue(seenTrue);
        assertTrue(seenFalse);

        String requestOutput = captureOutput(() -> ExternalLibraryAPI.requestBook("USER-001", "外部借书"));
        assertTrue(requestOutput.contains("Request successful"));
    }

    // 所有核心模块测试均已覆盖主要场景。
}
