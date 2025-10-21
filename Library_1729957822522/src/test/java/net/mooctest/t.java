package net.mooctest;

import org.junit.Test;
import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 全量业务测试：覆盖 Library_1729957822522/src/main/java/net/mooctest 下的所有核心类
 * 目标：
 * 1) 分支覆盖率尽可能满覆盖
 * 2) 变异杀死率尽可能满覆盖
 * 3) 代码可读、可维护，测试高效
 */
public class t {

    private Date daysFromNow(int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }

    private Date dateAt(int year, int month0Based, int day) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month0Based, day, 0, 0, 0);
        return cal.getTime();
    }

    private Date addDays(Date base, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(base);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }

    private int diffDays(Date later, Date earlier) {
        long ms = later.getTime() - earlier.getTime();
        return (int) (ms / (1000L * 60 * 60 * 24));
    }

    private void setPrivate(Object obj, String field, Object value) {
        try {
            Field f = obj.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object getPrivate(Object obj, String field) {
        try {
            Field f = obj.getClass().getDeclaredField(field);
            f.setAccessible(true);
            return f.get(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 自定义测试用书籍类：用于触发 RegularUser.borrowBook 中“库存不足则入预约队列”的分支
    // 通过重写 isAvailable 让其在库存为 0 时仍返回 true，从而命中该分支
    static class TestBook extends Book {
        public TestBook(String title, String author, String isbn, BookType bookType, int totalCopies) {
            super(title, author, isbn, bookType, totalCopies);
        }
        @Override
        public boolean isAvailable() {
            return true; // 强制可借，以进入 RegularUser 的库存判断分支
        }
    }

    // 可检测 sendNotification 内部对 void 方法调用是否发生的可测替身
    static class TestNotificationService extends NotificationService {
        boolean emailCalled;
        boolean smsCalled;
        boolean appCalled;
        @Override
        public void sendEmail(String email, String message) throws EmailException {
            emailCalled = true;
            if (email == null || email.isEmpty()) throw new EmailException("The user does not have an email address.");
        }
        @Override
        public void sendSMS(String phoneNumber, String message) throws SMSException {
            smsCalled = true;
            if (phoneNumber == null || phoneNumber.isEmpty()) throw new SMSException("The user does not have a phone number.");
        }
        @Override
        public void sendAppNotification(User user, String message) {
            appCalled = true;
        }
    }

    @Test
    public void testBookAvailabilityAndBorrowAndReturn() throws Exception {
        // 中文注释：验证 Book 的可借判断、借书与还书的关键分支
        Book b = new Book("Title", "Author", "ISBN", BookType.GENERAL, 2);

        // 可借
        assertTrue(b.isAvailable());

        // 借出一本，availableCopies 递减，且 borrow() 返回 false（保持对返回值的断言以提高变异杀死率）
        boolean borrowRet = b.borrow();
        assertFalse(borrowRet);
        assertEquals(1, b.getAvailableCopies());

        // 还书成功（当前可借数量小于总量）
        b.returnBook();
        assertEquals(2, b.getAvailableCopies());

        // 当可借数量已满时再次还书，抛出异常
        try {
            b.returnBook();
            fail("应当抛出 InvalidOperationException");
        } catch (InvalidOperationException expected) {
            // pass
        }

        // 维修中 -> 不可借
        b.setInRepair(true);
        assertFalse(b.isAvailable());
        try {
            b.borrow();
            fail("维修中借书应抛出异常");
        } catch (BookNotAvailableException expected) {
            // pass
        }
        b.setInRepair(false);

        // 损坏 -> 不可借
        b.setDamaged(true);
        assertFalse(b.isAvailable());
        b.setDamaged(false);

        // 库存为 0 -> 不可借
        b.setAvailableCopies(0);
        assertFalse(b.isAvailable());

        // 再次设置为可借
        b.setAvailableCopies(1);
        assertTrue(b.isAvailable());

        // 报告损坏/维修分支（重复调用以覆盖已损坏/已在维修的分支）
        b.reportDamage();
        b.reportDamage();
        b.reportRepair();
        b.reportRepair();
    }

    @Test
    public void testReservationAddRemove() {
        // 中文注释：验证预约添加与取消队列的分支
        RegularUser user = new RegularUser("U", "1");
        Book book = new Book("T", "A", "I", BookType.GENERAL, 1);

        Reservation r = new Reservation(book, user);
        // 未添加直接移除 -> 走“不在队列”的分支
        book.removeReservation(r);

        // 添加再移除 -> 覆盖成功路径
        book.addReservation(r);
        assertFalse(book.getReservationQueue().isEmpty());
        book.removeReservation(r);
        assertTrue(book.getReservationQueue().isEmpty());
    }

    @Test
    public void testUserPayFineBranches() {
        // 中文注释：验证 User.payFine 的三条关键分支
        RegularUser user = new RegularUser("U", "1");

        // 黑名单用户无法缴费 -> IllegalStateException
        user.setAccountStatus(AccountStatus.BLACKLISTED);
        try {
            user.payFine(10);
            fail("黑名单应禁止缴费");
        } catch (IllegalStateException expected) {
            // pass
        }

        // 非黑名单，金额大于罚金 -> IllegalArgumentException
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.fines = 20.0; // 受保护字段，同包可直接设置
        try {
            user.payFine(30.0);
            fail("金额大于现有罚金应报错");
        } catch (IllegalArgumentException expected) {
            // pass
        }

        // 冻结用户罚金清零后应恢复 ACTIVE
        user.setAccountStatus(AccountStatus.FROZEN);
        user.fines = 30.0;
        user.payFine(30.0);
        assertEquals(0.0, user.getFines(), 0.0001);
        assertEquals(AccountStatus.ACTIVE, user.getAccountStatus());
    }

    @Test
    public void testUserReserveCancelReceiveScoreDeduct() throws Exception {
        // 中文注释：验证预约/取消预约、通知接收、加分/扣分等分支
        RegularUser user = new RegularUser("U", "1");
        Book book = new Book("T", "A", "I", BookType.GENERAL, 1);

        // 黑名单无法预约
        user.setAccountStatus(AccountStatus.BLACKLISTED);
        try {
            user.reserveBook(book);
            fail("黑名单应禁止预约");
        } catch (IllegalStateException expected) {
            // pass
        }

        // 冻结无法预约
        user.setAccountStatus(AccountStatus.FROZEN);
        try {
            user.reserveBook(book);
            fail("冻结应禁止预约");
        } catch (AccountFrozenException expected) {
            // pass
        }

        // 信用分不足无法预约（<50）
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.creditScore = 40;
        try {
            user.reserveBook(book);
            fail("信用分不足应禁止预约");
        } catch (InsufficientCreditException expected) {
            // pass
        }

        // 预约重复校验（利用泛型擦除，向 reservations 放入 Book 实例以命中 contains(book) 分支）
        user.creditScore = 100;
        ((List) user.reservations).add(book);
        try {
            user.reserveBook(book);
            fail("重复预约应抛出 ReservationNotAllowedException");
        } catch (ReservationNotAllowedException expected) {
            // pass
        }
        user.reservations.clear();

        // 图书不可借时也允许预约，进入预约队列
        book.setAvailableCopies(0);
        user.reserveBook(book);
        assertEquals(1, book.getReservationQueue().size());
        assertEquals(1, user.reservations.size());

        // 取消预约：存在预约
        user.cancelReservation(book);
        assertEquals(0, book.getReservationQueue().size());
        assertEquals(0, user.reservations.size());

        // 取消预约：不存在预约 -> 抛出异常
        try {
            user.cancelReservation(book);
            fail("无预约应抛出异常");
        } catch (InvalidOperationException expected) {
            // pass
        }

        // 接收通知：黑名单与正常用户
        user.setAccountStatus(AccountStatus.BLACKLISTED);
        user.receiveNotification("msg");
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.receiveNotification("msg2");

        // 加分：黑名单抛出+正常加分
        user.setAccountStatus(AccountStatus.BLACKLISTED);
        try {
            user.addScore(5);
            fail("黑名单不允许加分");
        } catch (IllegalStateException expected) {
            // pass
        }
        user.setAccountStatus(AccountStatus.ACTIVE);
        int old = user.getCreditScore();
        user.addScore(5);
        assertEquals(old + 5, user.getCreditScore());

        // 扣分致小于0、并触发冻结
        user.deductScore(1000);
        assertEquals(0, user.getCreditScore());
        assertEquals(AccountStatus.FROZEN, user.getAccountStatus());
    }

    @Test
    public void testRegularUserBorrowAndReturn() throws Exception {
        // 中文注释：验证普通用户借还书的所有关键分支
        RegularUser user = new RegularUser("R", "2");
        Book general = new Book("G", "A", "I1", BookType.GENERAL, 2);
        Book rare = new Book("R", "B", "I2", BookType.RARE, 1);

        // 黑名单借书 -> 抛异常
        user.setAccountStatus(AccountStatus.BLACKLISTED);
        try {
            user.borrowBook(general);
            fail("黑名单不允许借书");
        } catch (IllegalStateException expected) {
            // pass
        }

        // 冻结借书 -> 抛异常
        user.setAccountStatus(AccountStatus.FROZEN);
        try {
            user.borrowBook(general);
            fail("冻结不允许借书");
        } catch (AccountFrozenException expected) {
            // pass
        }

        // 达到借阅上限 -> 抛异常
        user.setAccountStatus(AccountStatus.ACTIVE);
        for (int i = 0; i < 5; i++) {
            BorrowRecord rec = new BorrowRecord(general, user, new Date(), daysFromNow(14));
            user.borrowedBooks.add(rec);
        }
        try {
            user.borrowBook(general);
            fail("超过借阅上限应抛异常");
        } catch (InvalidOperationException expected) {
            // pass
        }
        user.borrowedBooks.clear();

        // 罚金过高 -> 冻结并抛异常
        user.fines = 60.0;
        try {
            user.borrowBook(general);
            fail("罚金过高应抛异常");
        } catch (OverdueFineException expected) {
            assertEquals(AccountStatus.FROZEN, user.getAccountStatus());
        }
        user.fines = 0.0;
        user.setAccountStatus(AccountStatus.ACTIVE);

        // 图书不可借 -> 抛异常
        general.setDamaged(true);
        try {
            user.borrowBook(general);
            fail("不可借图书应抛异常");
        } catch (BookNotAvailableException expected) {
            // pass
        }
        general.setDamaged(false);

        // 信用分不足 -> 抛异常（<60）
        user.creditScore = 59;
        try {
            user.borrowBook(general);
            fail("信用分不足应抛异常");
        } catch (InsufficientCreditException expected) {
            // pass
        }
        user.creditScore = 100;

        // 普通用户借稀有书籍 -> 抛异常
        try {
            user.borrowBook(rare);
            fail("普通用户不能借稀有书籍");
        } catch (InvalidOperationException expected) {
            // pass
        }

        // 命中“库存不足入预约队列”的分支（通过 TestBook 重写 isAvailable）
        TestBook tb = new TestBook("TG", "TA", "TI", BookType.GENERAL, 1);
        tb.setAvailableCopies(0); // 库存为 0，但 isAvailable 返回 true
        user.borrowBook(tb); // 将进入 reserveBook 并返回
        assertEquals(1, tb.getReservationQueue().size());
        assertEquals(1, user.reservations.size());

        // 正常借书成功
        Book ok = new Book("OK", "OA", "OI", BookType.GENERAL, 2);
        int before = user.getCreditScore();
        user.borrowBook(ok);
        assertEquals(before + 1, user.getCreditScore());
        assertEquals(1, user.getBorrowedBooks().size());

        // 还书：无借阅记录 -> 抛异常
        Book notBorrowed = new Book("NB", "N", "N", BookType.GENERAL, 1);
        try {
            user.returnBook(notBorrowed);
            fail("未借阅书籍还书应报错");
        } catch (InvalidOperationException expected) {
            // pass
        }

        // 还书：正常（不逾期 -> 加 2 分）
        // 先保证库存状态可还
        ok.setAvailableCopies(1);
        ok.setTotalCopies(2);
        int beforeReturnScore = user.getCreditScore();
        user.returnBook(ok);
        assertEquals(beforeReturnScore + 2, user.getCreditScore());

        // 还书：逾期 -> 扣 5 分，且若分数<50 将冻结
        // 构造一条逾期记录
        Book late = new Book("L", "LA", "LI", BookType.GENERAL, 2);
        late.setAvailableCopies(1);
        BorrowRecord lateRec = new BorrowRecord(late, user, daysFromNow(-30), daysFromNow(-15));
        user.borrowedBooks.add(lateRec);
        // returnBook 内部会设置 returnDate 并计算罚金，这里先保证不触发 >100 的罚金冻结
        user.creditScore = 52;
        user.returnBook(late);
        // 因为逾期，扣 5 分 -> 将触发冻结（<50）
        assertEquals(AccountStatus.FROZEN, user.getAccountStatus());
    }

    @Test
    public void testVipUserBorrowReturnAndExtend() throws Exception {
        // 中文注释：验证 VIP 用户借还书及续借相关分支
        VIPUser vip = new VIPUser("V", "3");
        Book rare = new Book("R", "A", "I", BookType.RARE, 1);
        Book general = new Book("G", "B", "J", BookType.GENERAL, 2);

        // 黑名单借书 -> 异常
        vip.setAccountStatus(AccountStatus.BLACKLISTED);
        try {
            vip.borrowBook(general);
            fail("黑名单不允许借书");
        } catch (IllegalStateException expected) {
            // pass
        }

        // 冻结借书 -> 异常
        vip.setAccountStatus(AccountStatus.FROZEN);
        try {
            vip.borrowBook(general);
            fail("冻结不允许借书");
        } catch (AccountFrozenException expected) {
            // pass
        }

        // 达到借阅上限 10 -> 异常
        vip.setAccountStatus(AccountStatus.ACTIVE);
        for (int i = 0; i < 10; i++) {
            BorrowRecord rec = new BorrowRecord(general, vip, new Date(), daysFromNow(30));
            vip.borrowedBooks.add(rec);
        }
        try {
            vip.borrowBook(general);
            fail("超过借阅上限应异常");
        } catch (InvalidOperationException expected) {
            // pass
        }
        vip.borrowedBooks.clear();

        // 罚金过高 -> 冻结并异常
        vip.fines = 80.0;
        try {
            vip.borrowBook(general);
            fail("罚金过高应异常");
        } catch (OverdueFineException expected) {
            assertEquals(AccountStatus.FROZEN, vip.getAccountStatus());
        }
        vip.fines = 0.0;
        vip.setAccountStatus(AccountStatus.ACTIVE);

        // 不可借 -> 异常
        general.setDamaged(true);
        try {
            vip.borrowBook(general);
            fail("不可借应异常");
        } catch (BookNotAvailableException expected) {
            // pass
        }
        general.setDamaged(false);

        // 信用分不足（<50）-> 异常
        vip.creditScore = 49;
        try {
            vip.borrowBook(general);
            fail("信用分不足应异常");
        } catch (InsufficientCreditException expected) {
            // pass
        }
        vip.creditScore = 100;

        // VIP 可借稀有书籍 -> 成功
        vip.borrowBook(rare);
        assertEquals(1, vip.getBorrowedBooks().size());

        // 续借：需已有借阅记录
        Date beforeDue = vip.getBorrowedBooks().get(0).getDueDate();
        vip.extendBorrowPeriod(rare);
        Date afterDue = vip.getBorrowedBooks().get(0).getDueDate();
        assertEquals(7, diffDays(afterDue, beforeDue));
        // 再次续借 -> 异常（已续借过）
        try {
            vip.extendBorrowPeriod(rare);
            fail("重复续借应异常");
        } catch (InvalidOperationException expected) {
            // pass
        }

        // 还书：无记录 -> 异常
        Book nb = new Book("NB", "N", "N", BookType.GENERAL, 1);
        try {
            vip.returnBook(nb);
            fail("未借阅书籍还书应异常");
        } catch (InvalidOperationException expected) {
            // pass
        }

        // 还书：罚金>100 -> 冻结并异常
        rare.setAvailableCopies(0); // 借出状态
        rare.setTotalCopies(1);
        vip.fines = 101.0; // 触发 >100 分支
        try {
            vip.returnBook(rare);
            fail("罚金过高应异常");
        } catch (OverdueFineException expected) {
            assertEquals(AccountStatus.FROZEN, vip.getAccountStatus());
        }
    }

    @Test
    public void testBorrowRecordFineAndExtend() {
        // 中文注释：验证 BorrowRecord 的罚金计算与到期延长（近似断言）
        RegularUser user = new RegularUser("U", "1");
        Book g = new Book("G", "A", "I", BookType.GENERAL, 1);
        Book r = new Book("R", "B", "J", BookType.RARE, 1);
        Book j = new Book("J", "C", "K", BookType.JOURNAL, 1);

        Date borrow = new Date();
        Date due = daysFromNow(5);

        BorrowRecord rec = new BorrowRecord(g, user, borrow, due);
        // 未归还或提前归还 -> 罚金为 0
        assertEquals(0.0, rec.calculateFine(), 0.0001);
        rec.setReturnDate(daysFromNow(2)); // 提前归还
        assertEquals(0.0, rec.calculateFine(), 0.0001);

        // 逾期一般书籍：每天 1 元
        rec = new BorrowRecord(g, user, borrow, daysFromNow(-2));
        rec.setReturnDate(new Date());
        assertTrue(rec.calculateFine() >= 2.0);

        // 逾期稀有书籍：每天 5 元
        BorrowRecord rr = new BorrowRecord(r, user, borrow, daysFromNow(-2));
        rr.setReturnDate(new Date());
        assertTrue(rr.calculateFine() >= 10.0);

        // 逾期期刊：每天 2 元
        BorrowRecord rj = new BorrowRecord(j, user, borrow, daysFromNow(-3));
        rj.setReturnDate(new Date());
        assertTrue(rj.calculateFine() >= 6.0);

        // 黑名单用户罚金翻倍 + 图书损坏额外 +50
        user.setAccountStatus(AccountStatus.BLACKLISTED);
        g.setDamaged(true);
        BorrowRecord bad = new BorrowRecord(g, user, borrow, daysFromNow(-1));
        bad.setReturnDate(new Date());
        assertTrue(bad.calculateFine() >= 52.0);

        // 延长到期时间精确校验
        Date oldDue = rec.getDueDate();
        rec.extendDueDate(7);
        assertEquals(7, diffDays(rec.getDueDate(), oldDue));
    }

    @Test
    public void testBorrowRecordExactFine() {
        // 中文注释：使用固定日期，精确校验不同书籍类型的罚金计算以杀死 MATH/NEGATE 变异
        RegularUser user = new RegularUser("U", "1");
        Date base = dateAt(2020, 0, 1);
        // 一般书，逾期 3 天
        Book g = new Book("G", "A", "I", BookType.GENERAL, 1);
        BorrowRecord rg = new BorrowRecord(g, user, base, addDays(base, 0));
        rg.setReturnDate(addDays(base, 3));
        assertEquals(3.0, rg.calculateFine(), 0.0001);
        // 稀有书，逾期 3 天 -> 每天 5 元
        Book r = new Book("R", "B", "J", BookType.RARE, 1);
        BorrowRecord rr = new BorrowRecord(r, user, base, addDays(base, 0));
        rr.setReturnDate(addDays(base, 3));
        assertEquals(15.0, rr.calculateFine(), 0.0001);
        // 期刊，逾期 3 天 -> 每天 2 元
        Book j = new Book("J", "C", "K", BookType.JOURNAL, 1);
        BorrowRecord rj = new BorrowRecord(j, user, base, addDays(base, 0));
        rj.setReturnDate(addDays(base, 3));
        assertEquals(6.0, rj.calculateFine(), 0.0001);
        // 黑名单 + 损坏，逾期 1 天 -> (1*1)*2 + 50 = 52
        user.setAccountStatus(AccountStatus.BLACKLISTED);
        g.setDamaged(true);
        BorrowRecord bad = new BorrowRecord(g, user, base, addDays(base, 0));
        bad.setReturnDate(addDays(base, 1));
        assertEquals(52.0, bad.calculateFine(), 0.0001);
    }

    @Test
    public void testReservationPriority() {
        // 中文注释：验证预约优先级计算（VIP 加成、逾期扣分、黑名单特殊处理）
        RegularUser reg = new RegularUser("R", "1");
        VIPUser vip = new VIPUser("V", "2");
        Book book = new Book("B", "A", "I", BookType.GENERAL, 1);

        // 正常用户：无逾期记录
        reg.creditScore = 70;
        Reservation rr = new Reservation(book, reg);
        assertEquals(70, rr.getPriority());

        // VIP 用户：有 +10 加成
        vip.creditScore = 80;
        Reservation vr = new Reservation(book, vip);
        assertEquals(90, vr.getPriority());

        // 添加一条逾期借阅记录 -> 优先级 -5
        BorrowRecord late = new BorrowRecord(book, reg, daysFromNow(-10), daysFromNow(-5));
        late.setReturnDate(new Date());
        reg.borrowedBooks.add(late);
        Reservation rr2 = new Reservation(book, reg);
        assertEquals(65, rr2.getPriority());

        // 黑名单用户 -> 返回 -1
        reg.setAccountStatus(AccountStatus.BLACKLISTED);
        Reservation rr3 = new Reservation(book, reg);
        assertEquals(-1, rr3.getPriority());
    }

    @Test
    public void testAutoRenewalService() throws Exception {
        // 中文注释：验证自动续借服务的所有分支
        AutoRenewalService svc = new AutoRenewalService();
        RegularUser user = new RegularUser("U", "1");
        Book book = new Book("B", "A", "I", BookType.GENERAL, 1);

        // 账户非 ACTIVE -> 抛冻结异常
        user.setAccountStatus(AccountStatus.FROZEN);
        try {
            svc.autoRenew(user, book);
            fail("冻结账户不可自动续借");
        } catch (AccountFrozenException expected) {
            // pass
        }

        // 有预约队列 -> 抛不允许续借
        user.setAccountStatus(AccountStatus.ACTIVE);
        book.addReservation(new Reservation(book, user));
        try {
            svc.autoRenew(user, book);
            fail("被预约不可续借");
        } catch (InvalidOperationException expected) {
            // pass
        }
        // 清空预约队列
        book.getReservationQueue().clear();

        // 信用分不足 -> 抛异常
        user.creditScore = 59;
        try {
            svc.autoRenew(user, book);
            fail("信用分不足不可续借");
        } catch (InsufficientCreditException expected) {
            // pass
        }

        // 无借阅记录 -> 抛异常
        user.creditScore = 100;
        try {
            svc.autoRenew(user, book);
            fail("无借阅记录不可续借");
        } catch (InvalidOperationException expected) {
            // pass
        }

        // 有借阅记录 -> 正常续借 14 天（精确校验差值）
        Date borrowDate = dateAt(2020, 0, 1);
        Date dueDate = addDays(borrowDate, 7);
        BorrowRecord rec = new BorrowRecord(book, user, borrowDate, dueDate);
        user.borrowedBooks.add(rec);
        svc.autoRenew(user, book);
        assertEquals(14, diffDays(rec.getDueDate(), dueDate));
    }

    @Test
    public void testCreditRepairService() throws Exception {
        // 中文注释：验证信用修复服务
        CreditRepairService svc = new CreditRepairService();
        RegularUser user = new RegularUser("U", "1");

        // 支付金额过小 -> 异常
        try {
            svc.repairCredit(user, 5.0);
            fail("最小支付应为 10 元");
        } catch (InvalidOperationException expected) {
            // pass
        }

        // 正常修复 -> 提升信用分，若 >=60 则恢复 ACTIVE
        user.setAccountStatus(AccountStatus.FROZEN);
        user.creditScore = 55;
        svc.repairCredit(user, 50.0); // 增加 5 分
        assertEquals(60, user.getCreditScore());
        assertEquals(AccountStatus.ACTIVE, user.getAccountStatus());
    }

    @Test
    public void testInventoryService() throws Exception {
        // 中文注释：验证库存服务报告丢失/损坏，并精确校验罚金扣减
        InventoryService svc = new InventoryService();
        RegularUser user = new RegularUser("U", "1");
        Book book = new Book("B", "A", "I", BookType.GENERAL, 2);

        // 未借书报告丢失/损坏 -> 异常
        try {
            svc.reportLost(book, user);
            fail("未借书不能上报丢失");
        } catch (InvalidOperationException expected) {
            // pass
        }
        try {
            svc.reportDamaged(book, user);
            fail("未借书不能上报损坏");
        } catch (InvalidOperationException expected) {
            // pass
        }

        // 通过原始类型插入 Book 到 borrowedBooks，使 contains(book) 命中（由于泛型擦除）
        ((List) user.getBorrowedBooks()).add(book);
        // 确保可以支付相应费用：设置罚金余额
        user.fines = 200.0;
        int oldTotal = book.getTotalCopies();
        int oldAvail = book.getAvailableCopies();
        double beforeFine = user.getFines();
        double comp = book.getTotalCopies() * 50.0;
        svc.reportLost(book, user);
        assertEquals(oldTotal - 1, book.getTotalCopies());
        assertEquals(oldAvail - 1, book.getAvailableCopies());
        assertEquals(beforeFine - comp, user.getFines(), 0.0001);

        // 损坏上报：需可支付修理费 30
        user.fines = 30.0;
        double beforeFine2 = user.getFines();
        svc.reportDamaged(book, user);
        assertEquals(beforeFine2 - 30.0, user.getFines(), 0.0001);
        // 进入维修状态 -> 不可借
        assertFalse(book.isAvailable());
    }

    @Test
    public void testNotificationService() throws Exception {
        // 中文注释：验证通知服务的三条发送路径（不做断言仅确保不抛异常）
        NotificationService svc = new NotificationService();
        RegularUser user = new RegularUser("U", "1");

        // 黑名单直接返回
        user.setAccountStatus(AccountStatus.BLACKLISTED);
        svc.sendNotification(user, "msg");

        // 邮件成功发送
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setEmail("u@test.com");
        svc.sendNotification(user, "email ok");

        // 邮件失败 -> 短信成功
        user.setEmail("");
        user.setPhoneNumber("123456");
        svc.sendNotification(user, "sms ok");

        // 邮件失败 -> 短信失败 -> 走 APP 通知
        user.setEmail("");
        user.setPhoneNumber("");
        svc.sendNotification(user, "app ok");
    }

    @Test
    public void testNotificationDirectMethodsThrows() {
        // 中文注释：验证直接调用邮件/短信发送的异常分支
        NotificationService svc = new NotificationService();
        try {
            svc.sendEmail(null, "m");
            fail("空邮箱应抛出 EmailException");
        } catch (EmailException expected) {
            // pass
        }
        try {
            svc.sendEmail("", "m");
            fail("空邮箱应抛出 EmailException");
        } catch (EmailException expected) {
            // pass
        }
        try {
            svc.sendSMS(null, "m");
            fail("空手机号应抛出 SMSException");
        } catch (SMSException expected) {
            // pass
        }
        try {
            svc.sendSMS("", "m");
            fail("空手机号应抛出 SMSException");
        } catch (SMSException expected) {
            // pass
        }
    }

    @Test
    public void testNotificationServiceCallPathsWithSubclass() {
        // 中文注释：通过可测替身验证 sendNotification 内部是否真正调用了各通道，杀死 VOID_METHOD_CALLS 变异
        TestNotificationService svc = new TestNotificationService();
        RegularUser user = new RegularUser("U", "1");
        user.setAccountStatus(AccountStatus.ACTIVE);

        user.setEmail("e@test.com");
        user.setPhoneNumber("123");
        svc.sendNotification(user, "m");
        assertTrue(svc.emailCalled);
        assertFalse(svc.smsCalled);
        assertFalse(svc.appCalled);

        svc.emailCalled = svc.smsCalled = svc.appCalled = false;
        user.setEmail("");
        user.setPhoneNumber("123");
        svc.sendNotification(user, "m");
        assertFalse(svc.emailCalled);
        assertTrue(svc.smsCalled);
        assertFalse(svc.appCalled);

        svc.emailCalled = svc.smsCalled = svc.appCalled = false;
        user.setEmail("");
        user.setPhoneNumber("");
        svc.sendNotification(user, "m");
        assertFalse(svc.emailCalled);
        assertFalse(svc.smsCalled);
        assertTrue(svc.appCalled);
    }

    @Test
    public void testLibrary() throws Exception {
        // 中文注释：验证图书馆聚合类的核心流程
        Library lib = new Library();
        RegularUser u1 = new RegularUser("U1", "001");
        RegularUser u2 = new RegularUser("U2", "002");
        Book b1 = new Book("B1", "A1", "I1", BookType.GENERAL, 1);

        // 注册用户：信用分过低 -> 不注册（通过反射校验内部 users 列表未变化）
        u1.creditScore = 49;
        lib.registerUser(u1);
        List users0 = (List) getPrivate(lib, "users");
        assertEquals(0, users0.size());
        // 注册成功
        u1.creditScore = 60;
        lib.registerUser(u1);
        List users1 = (List) getPrivate(lib, "users");
        assertEquals(1, users1.size());
        // 重复注册 -> 不应增加
        lib.registerUser(u1);
        List users2 = (List) getPrivate(lib, "users");
        assertEquals(1, users2.size());

        // 添加图书：成功与重复（通过反射校验 books 列表）
        lib.addBook(b1);
        List books1 = (List) getPrivate(lib, "books");
        assertEquals(1, books1.size());
        lib.addBook(b1);
        List books2 = (List) getPrivate(lib, "books");
        assertEquals(1, books2.size());

        // 处理预约：图书不可借 -> 直接返回（不出队）
        Reservation tmp = new Reservation(b1, u2);
        b1.addReservation(tmp);
        b1.setDamaged(true);
        lib.processReservations(b1);
        assertEquals(1, b1.getReservationQueue().size());
        b1.setDamaged(false);
        b1.getReservationQueue().clear();

        // 处理预约：有预约且可借 -> 借出并发送通知
        u2.creditScore = 100;
        u2.setEmail("u2@test.com");
        Reservation r = new Reservation(b1, u2);
        b1.addReservation(r);
        // 注入可测通知服务，验证确实调用了通知
        TestNotificationService tns = new TestNotificationService();
        setPrivate(lib, "notificationService", tns);
        // 处理
        lib.processReservations(b1);
        // 借出成功：用户借阅记录 +1，图书库存 -1
        assertEquals(1, u2.getBorrowedBooks().size());
        assertEquals(0, b1.getAvailableCopies());
        // 确实调用过通知
        assertTrue(tns.emailCalled || tns.smsCalled || tns.appCalled);

        // 自动续借：无借阅记录 -> 触发异常路径
        RegularUser u3 = new RegularUser("U3", "003");
        lib.autoRenewBook(u3, b1);
        // 构造借阅记录后再次续借（校验精确 14 天）
        BorrowRecord rec = new BorrowRecord(b1, u2, dateAt(2020, 0, 1), addDays(dateAt(2020, 0, 1), 3));
        u2.borrowedBooks.add(rec);
        lib.autoRenewBook(u2, b1);
        assertEquals(14, diffDays(rec.getDueDate(), addDays(dateAt(2020, 0, 1), 3)));

        // 信用修复：异常与成功
        try {
            lib.repairUserCredit(u2, 5.0);
        } catch (Exception ignore) {
        }
        lib.repairUserCredit(u2, 50.0);

        // 丢失/损坏上报（会由库内部捕获异常），这里仅调用以覆盖
        lib.reportLostBook(u2, b1);
        lib.reportDamagedBook(u2, b1);
    }

    @Test
    public void testVipUserOnTimeReturnIncreasesCredit() throws Exception {
        // 中文注释：验证 VIP 用户按时归还后信用分 +3 的分支
        VIPUser vip = new VIPUser("VX", "100");
        Book book = new Book("B", "A", "I", BookType.GENERAL, 1);
        int before = vip.getCreditScore();
        vip.borrowBook(book);
        // 立即归还，不会逾期，应加 3 分
        vip.returnBook(book);
        assertEquals(before + 3, vip.getCreditScore());
    }

    @Test
    public void testCalculateDueDateHelpers() {
        // 中文注释：精确验证两类用户的到期日计算函数
        RegularUser ru = new RegularUser("R", "1");
        VIPUser vu = new VIPUser("V", "2");
        Date base = dateAt(2021, 5, 1);
        assertEquals(10, diffDays(ru.calculateDueDate(base, 10), base));
        assertEquals(30, diffDays(vu.calculateDueDate(base, 30), base));
    }

    @Test
    public void testExternalLibraryAPI() {
        // 中文注释：验证外部库 API 的稳定性（概率性断言，确保变异为常量返回时能被检测）
        boolean seenTrue = false;
        boolean seenFalse = false;
        for (int i = 0; i < 200; i++) { // 次数适中，保证效率同时足够杀死常量返回的变异
            boolean v = ExternalLibraryAPI.checkAvailability("X");
            seenTrue |= v;
            seenFalse |= !v;
            if (seenTrue && seenFalse) break;
        }
        assertTrue("应该既出现 true 也出现 false", seenTrue && seenFalse);

        // requestBook 仅验证可调用（无异常）
        ExternalLibraryAPI.requestBook("uid", "title");
    }
}

/*
评估报告：
1) 分支覆盖率：预计≈100%（除去源代码中逻辑上不可达的极个别分支，如 RegularUser.borrowBook 中先判 isAvailable 后再判库存 <1 的分支，已通过 TestBook 技巧覆盖该路径）。
2) 变异杀死率：目标≥90%。
   - 对关键返回值（如 Book.borrow 返回 false）进行了断言，避免 RETURN_VALS 类变异存活；
   - 对条件、边界、异常分支均设有对应断言，覆盖 NEGATE_CONDITIONALS/CONDITIONALS_BOUNDARY/MATH 等变异；
   - 对日期加减（extendDueDate/autoRenew/calculateDueDate）进行“精确天数差”断言，杀死 INCREMENTS/MATH 变异；
   - 对随机返回（ExternalLibraryAPI.checkAvailability）采用多次采样并断言“真/假均出现”，有效杀死“恒真/恒假”变异；
   - 通过可测替身 TestNotificationService 断言 sendNotification 内部确实调用了 email/sms/app 路径，
     有效杀死 VOID_METHOD_CALLS 变异；
   - 对 InventoryService 罚金扣减与库存变更做精确断言，杀死 MATH/VOID_METHOD_CALLS 变异。
3) 可读性与可维护性：
   - 每个测试方法均以中文注释说明用例目的与预期；
   - 使用辅助方法 daysFromNow/dateAt/addDays/diffDays 与可测替身类简化构造，提高复用性与可维护性；
   - 同包访问与适度的原始类型转换仅用于命中特定分支，并有明确注释说明原因。
4) 运行效率：
   - 外部库随机性测试循环 200 次，能有效杀死变异且执行时间可控；
   - 其余用例均为常量时间构造，整体执行迅速。
改进建议：
- 建议业务代码中修复若干潜在问题（如 List<BorrowRecord> 上 contains(Book) 的类型不一致、字符串拼接错误等），以提升代码健壮性并减少测试的“规避式写法”。
*/
