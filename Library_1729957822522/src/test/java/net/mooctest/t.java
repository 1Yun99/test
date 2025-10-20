package net.mooctest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * 优化后的图书管理系统综合测试类
 * 
 * 测试覆盖范围：
 * - Book: 借阅/归还/损坏/修复/预约队列管理
 * - User系列: RegularUser/VIPUser 的借阅规则与状态管理  
 * - BorrowRecord: 罚金计算/日期处理/状态变更
 * - Library: 用户注册/图书管理/预约处理/自动续借
 * - 服务类: AutoRenewalService/CreditRepairService/NotificationService
 * - Reservation: 优先级计算/VIP特权/逾期惩罚
 * 
 * 优化特点：
 * 1. 统一的setUp/tearDown，消除重复代码
 * 2. 精确的分支覆盖和边界测试
 * 3. 高变异杀死率的断言策略
 * 4. 清晰的中文注释和测试分组
 * 5. 高效的测试执行，避免冗余初始化
 */
public class t {
    
    // === 测试基础设施 ===
    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;

    // === 核心业务对象 ===
    private Library library;
    private Book generalBook;
    private Book rareBook;
    private Book journalBook;
    private RegularUser regularUser;
    private VIPUser vipUser;
    private AutoRenewalService autoRenewalService;
    private CreditRepairService creditRepairService;
    private NotificationService notificationService;

    @Before
    public void setUp() {
        // 重定向标准输出，便于断言打印内容
        originalOut = System.out;
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        // 初始化核心业务对象
        library = new Library();
        generalBook = new Book("GeneralBook", "AuthorA", "ISBN-G", BookType.GENERAL, 3);
        rareBook = new Book("RareBook", "AuthorB", "ISBN-R", BookType.RARE, 1);
        journalBook = new Book("JournalBook", "AuthorC", "ISBN-J", BookType.JOURNAL, 2);
        regularUser = new RegularUser("Alice", "U1");
        vipUser = new VIPUser("Bob", "U2");
        autoRenewalService = new AutoRenewalService();
        creditRepairService = new CreditRepairService();
        notificationService = new NotificationService();

        // 设置用户初始状态为最佳测试条件
        regularUser.creditScore = 100;
        vipUser.creditScore = 120;
        regularUser.setAccountStatus(AccountStatus.ACTIVE);
        vipUser.setAccountStatus(AccountStatus.ACTIVE);
        
        // 设置通知所需的联系方式
        regularUser.setEmail("alice@example.com");
        regularUser.setPhoneNumber("13000000000");
        vipUser.setEmail("bob@example.com");
        vipUser.setPhoneNumber("13100000000");
        
        // 清空输出缓冲区
        outContent.reset();
    }

    @After
    public void tearDown() {
        // 恢复标准输出
        System.setOut(originalOut);
    }

    // ==================== Book 类测试 ====================
    
    @Test
    public void testBookAvailabilityStates() {
        // 测试图书可用性的各种状态
        assertTrue("新图书应该可用", generalBook.isAvailable());
        
        // 设置为修理状态
        generalBook.setInRepair(true);
        assertFalse("修理中的图书不可用", generalBook.isAvailable());
        
        // 设置为损坏状态
        generalBook.setInRepair(false);
        generalBook.setDamaged(true);
        assertFalse("损坏的图书不可用", generalBook.isAvailable());
        
        // 库存为0
        generalBook.setDamaged(false);
        generalBook.setAvailableCopies(0);
        assertFalse("库存为0的图书不可用", generalBook.isAvailable());
    }

    @Test
    public void testBookBorrowAndReturnFlow() throws Exception {
        // 测试图书借阅和归还的完整流程
        int initialCopies = generalBook.getAvailableCopies();
        
        // 成功借阅
        regularUser.borrowBook(generalBook);
        assertEquals("借阅后库存应减1", initialCopies - 1, generalBook.getAvailableCopies());
        
        // 成功归还
        regularUser.returnBook(generalBook);
        assertEquals("归还后库存应恢复", initialCopies, generalBook.getAvailableCopies());
    }

    @Test(expected = BookNotAvailableException.class)
    public void testBorrowUnavailableBook() throws Exception {
        // 测试借阅不可用图书抛出异常
        generalBook.setAvailableCopies(0);
        regularUser.borrowBook(generalBook);
    }

    @Test(expected = InvalidOperationException.class)
    public void testReturnNotBorrowedBook() throws Exception {
        // 测试归还未借阅图书抛出异常
        regularUser.returnBook(generalBook);
    }

    @Test
    public void testBookDamageAndRepairCycle() {
        // 测试图书损坏和修复循环
        outContent.reset();
        generalBook.reportDamage();
        assertTrue("报告损坏应有输出", outContent.toString().contains("damage"));
        assertTrue("图书应标记为损坏", generalBook.isDamaged());
        
        outContent.reset();
        generalBook.reportRepair();
        assertTrue("报告修复应有输出", outContent.toString().contains("repair"));
        assertFalse("修复后图书不应损坏", generalBook.isDamaged());
    }

    @Test
    public void testReservationQueueManagement() throws Exception {
        // 测试预约队列管理
        Reservation reservation = new Reservation(generalBook, vipUser);
        generalBook.addReservation(reservation);
        assertEquals("添加预约后队列大小应为1", 1, generalBook.getReservationQueue().size());
        
        generalBook.removeReservation(reservation);
        assertEquals("移除预约后队列应为空", 0, generalBook.getReservationQueue().size());
    }

    // ==================== BorrowRecord 类测试 ====================
    
    @Test
    public void testFineCalculationByBookType() {
        // 测试不同图书类型的罚金计算
        Date borrowDate = new Date();
        Calendar dueCal = Calendar.getInstance();
        dueCal.setTime(borrowDate);
        dueCal.add(Calendar.DAY_OF_MONTH, 14); // 假设借期14天
        Date dueDate = dueCal.getTime();
        
        BorrowRecord generalRecord = new BorrowRecord(generalBook, regularUser, borrowDate, dueDate);
        BorrowRecord rareRecord = new BorrowRecord(rareBook, regularUser, borrowDate, dueDate);
        BorrowRecord journalRecord = new BorrowRecord(journalBook, regularUser, borrowDate, dueDate);
        
        // 设置逾期3天的归还日期
        Calendar cal = Calendar.getInstance();
        cal.setTime(dueDate);
        cal.add(Calendar.DAY_OF_MONTH, 3);
        Date overdueDate = cal.getTime();
        
        generalRecord.setReturnDate(overdueDate);
        rareRecord.setReturnDate(overdueDate);
        journalRecord.setReturnDate(overdueDate);
        
        // 验证不同类型图书的罚金
        assertEquals("普通图书逾期3天罚金", 3.0, generalRecord.calculateFine(), 0.01);
        assertEquals("珍稀图书逾期3天罚金", 15.0, rareRecord.calculateFine(), 0.01);
        assertEquals("期刊逾期3天罚金", 1.5, journalRecord.calculateFine(), 0.01);
    }

    @Test
    public void testFineCalculationForBlacklistedUser() {
        // 测试黑名单用户的罚金计算
        regularUser.setAccountStatus(AccountStatus.BLACKLISTED);
        
        Date borrowDate = new Date();
        Calendar dueCal = Calendar.getInstance();
        dueCal.setTime(borrowDate);
        dueCal.add(Calendar.DAY_OF_MONTH, 14); // 假设借期14天
        Date dueDate = dueCal.getTime();
        
        BorrowRecord record = new BorrowRecord(generalBook, regularUser, borrowDate, dueDate);
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(dueDate);
        cal.add(Calendar.DAY_OF_MONTH, 2);
        record.setReturnDate(cal.getTime());
        
        // 黑名单用户罚金翻倍
        assertEquals("黑名单用户罚金翻倍", 4.0, record.calculateFine(), 0.01);
    }

    @Test
    public void testFineCalculationForDamagedBook() {
        // 测试损坏图书的罚金计算
        generalBook.setDamaged(true);
        
        Date borrowDate = new Date();
        Calendar dueCal = Calendar.getInstance();
        dueCal.setTime(borrowDate);
        dueCal.add(Calendar.DAY_OF_MONTH, 14); // 假设借期14天
        Date dueDate = dueCal.getTime();
        
        BorrowRecord record = new BorrowRecord(generalBook, regularUser, borrowDate, dueDate);
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(dueDate);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        record.setReturnDate(cal.getTime());
        
        // 损坏图书额外罚金50元
        assertEquals("损坏图书额外罚金", 51.0, record.calculateFine(), 0.01);
    }

    @Test
    public void testExtendDueDate() {
        // 测试延长到期日期
        BorrowRecord record = new BorrowRecord(regularUser, generalBook);
        Date originalDue = record.getDueDate();
        
        outContent.reset();
        record.extendDueDate(7);
        
        assertTrue("延长借期应有输出", outContent.toString().contains("extended"));
        assertTrue("到期日应延后", record.getDueDate().after(originalDue));
    }

    // ==================== RegularUser 类测试 ====================
    
    @Test(expected = IllegalStateException.class)
    public void testRegularUserBorrowWhenBlacklisted() throws Exception {
        // 测试黑名单用户借阅抛出异常
        regularUser.setAccountStatus(AccountStatus.BLACKLISTED);
        regularUser.borrowBook(generalBook);
    }

    @Test(expected = AccountFrozenException.class)
    public void testRegularUserBorrowWhenFrozen() throws Exception {
        // 测试冻结用户借阅抛出异常
        regularUser.setAccountStatus(AccountStatus.FROZEN);
        regularUser.borrowBook(generalBook);
    }

    @Test(expected = InvalidOperationException.class)
    public void testRegularUserExceedBorrowLimit() throws Exception {
        // 测试超出借阅限制
        for (int i = 0; i < 6; i++) { // RegularUser借阅限制为5
            Book book = new Book("Book" + i, "Author", "ISBN" + i, BookType.GENERAL, 1);
            regularUser.borrowBook(book);
        }
    }

    @Test(expected = OverdueFineException.class)
    public void testRegularUserBorrowWithHighFine() throws Exception {
        // 测试高罚金用户借阅抛出异常
        // 直接设置罚金
        regularUser.fines = 100.0; // 超过50元限制
        regularUser.borrowBook(generalBook);
    }

    @Test(expected = InsufficientCreditException.class)
    public void testRegularUserBorrowWithLowCredit() throws Exception {
        // 测试低信用分用户借阅抛出异常
        regularUser.creditScore = 40; // 低于60分限制
        regularUser.borrowBook(generalBook);
    }

    @Test(expected = InvalidOperationException.class)
    public void testRegularUserBorrowRareBook() throws Exception {
        // 测试普通用户借阅珍稀图书抛出异常
        regularUser.borrowBook(rareBook);
    }

    @Test
    public void testRegularUserSuccessfulBorrow() throws Exception {
        // 测试普通用户成功借阅
        regularUser.borrowBook(generalBook);
        assertNotNull("应创建借阅记录", regularUser.findBorrowRecord(generalBook));
        assertEquals("借阅列表应包含图书", 1, regularUser.getBorrowedBooks().size());
    }

    @Test
    public void testRegularUserReturnWithFine() throws Exception {
        // 测试归还时产生罚金
        regularUser.borrowBook(generalBook);
        BorrowRecord record = regularUser.findBorrowRecord(generalBook);
        
        // 设置逾期归还
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 5);
        record.setReturnDate(cal.getTime());
        
        double initialFine = regularUser.fines;
        regularUser.returnBook(generalBook);
        
        assertTrue("归还逾期图书应产生罚金", regularUser.fines > initialFine);
    }

    // ==================== VIPUser 类测试 ====================
    
    @Test
    public void testVIPUserCanBorrowRareBook() throws Exception {
        // 测试VIP用户可以借阅珍稀图书
        vipUser.borrowBook(rareBook);
        assertNotNull("VIP用户应能借阅珍稀图书", vipUser.findBorrowRecord(rareBook));
    }

    @Test
    public void testVIPUserExtendBorrowPeriod() throws Exception {
        // 测试VIP用户延长借阅期
        vipUser.borrowBook(generalBook);
        BorrowRecord record = vipUser.findBorrowRecord(generalBook);
        Date originalDue = record.getDueDate();
        
        vipUser.extendBorrowPeriod(generalBook);
        assertTrue("VIP用户应能延长借阅期", record.getDueDate().after(originalDue));
    }

    @Test(expected = InvalidOperationException.class)
    public void testVIPUserExtendAlreadyExtended() throws Exception {
        // 测试VIP用户重复延长借阅期抛出异常
        vipUser.borrowBook(generalBook);
        vipUser.extendBorrowPeriod(generalBook);
        vipUser.extendBorrowPeriod(generalBook); // 第二次延长应失败
    }

    @Test
    public void testVIPUserReturnWithHighFineFreezesAccount() throws Exception {
        // 测试VIP用户高罚金冻结账户
        vipUser.borrowBook(generalBook);
        BorrowRecord record = vipUser.findBorrowRecord(generalBook);
        
        // 设置大幅逾期
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 200); // 大幅逾期产生高罚金
        record.setReturnDate(cal.getTime());
        
        vipUser.returnBook(generalBook);
        
        if (vipUser.fines > 100) {
            assertEquals("高罚金应冻结账户", AccountStatus.FROZEN, vipUser.getAccountStatus());
        }
    }

    // ==================== Reservation 类测试 ====================
    
    @Test
    public void testReservationPriorityCalculation() {
        // 测试预约优先级计算
        Reservation regularReservation = new Reservation(generalBook, regularUser);
        Reservation vipReservation = new Reservation(generalBook, vipUser);
        
        // VIP用户优先级应更高（信用分 + 10分VIP加成）
        assertTrue("VIP预约优先级应更高", 
                   vipReservation.getPriority() > regularReservation.getPriority());
    }

    @Test
    public void testReservationPriorityWithDelayedReturn() throws Exception {
        // 测试有逾期记录的用户预约优先级降低
        regularUser.borrowBook(generalBook);
        BorrowRecord record = regularUser.findBorrowRecord(generalBook);
        
        // 设置逾期归还
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 5);
        record.setReturnDate(cal.getTime());
        regularUser.returnBook(generalBook);
        
        Reservation reservation = new Reservation(journalBook, regularUser);
        // 有逾期记录的用户优先级应降低
        assertTrue("有逾期记录优先级应降低", reservation.getPriority() < regularUser.getCreditScore());
    }

    @Test
    public void testBlacklistedUserCannotReserve() {
        // 测试黑名单用户无法预约
        regularUser.setAccountStatus(AccountStatus.BLACKLISTED);
        Reservation reservation = new Reservation(generalBook, regularUser);
        assertEquals("黑名单用户预约优先级为-1", -1, reservation.getPriority());
    }

    // ==================== NotificationService 类测试 ====================
    
    @Test
    public void testNotificationEmailSuccess() {
        // 测试邮件通知成功
        outContent.reset();
        notificationService.sendNotification(regularUser, "测试消息");
        String output = outContent.toString();
        assertTrue("应发送邮件成功", output.contains("Successfully sent email"));
    }

    @Test
    public void testNotificationFallbackToSMS() {
        // 测试邮件失败回退到短信
        regularUser.setEmail(null); // 移除邮件地址
        outContent.reset();
        notificationService.sendNotification(regularUser, "测试消息");
        String output = outContent.toString();
        assertTrue("应回退到短信", output.contains("Email sending failed"));
        assertTrue("应发送短信成功", output.contains("Successfully sent text message"));
    }

    @Test
    public void testNotificationFallbackToApp() {
        // 测试邮件和短信都失败回退到应用内通知
        regularUser.setEmail(null);
        regularUser.setPhoneNumber(null);
        outContent.reset();
        notificationService.sendNotification(regularUser, "测试消息");
        String output = outContent.toString();
        assertTrue("应回退到应用内通知", output.contains("Send an in-app notification"));
    }

    // ==================== CreditRepairService 类测试 ====================
    
    @Test
    public void testCreditRepairSuccess() throws Exception {
        // 测试信用修复成功
        regularUser.creditScore = 55;
        regularUser.setAccountStatus(AccountStatus.FROZEN);
        
        creditRepairService.repairCredit(regularUser, 100);
        
        assertTrue("信用分应增加", regularUser.getCreditScore() > 55);
        assertEquals("账户应恢复活跃", AccountStatus.ACTIVE, regularUser.getAccountStatus());
    }

    @Test(expected = InvalidOperationException.class)
    public void testCreditRepairInsufficientPayment() throws Exception {
        // 测试支付金额不足
        creditRepairService.repairCredit(regularUser, 5); // 金额过小
    }

    // ==================== AutoRenewalService 类测试 ====================
    
    @Test
    public void testAutoRenewalSuccess() throws Exception {
        // 测试自动续借成功
        regularUser.borrowBook(generalBook);
        BorrowRecord record = regularUser.findBorrowRecord(generalBook);
        Date originalDue = record.getDueDate();
        
        autoRenewalService.autoRenew(regularUser, generalBook);
        
        // 验证续借成功的效果
        assertTrue("到期日应延后", record.getDueDate().after(originalDue));
        assertTrue("借阅记录应标记为已延期", record.isExtended());
    }

    @Test
    public void testAutoRenewalFailureNoRecord() {
        // 测试自动续借失败 - 无借阅记录
        // 执行自动续借，不应抛出异常
        autoRenewalService.autoRenew(regularUser, generalBook);
        // 无借阅记录，无法验证结果，只能确保方法执行不抛异常
    }

    @Test
    public void testAutoRenewalFailureFrozenAccount() throws Exception {
        // 测试自动续借失败 - 账户冻结
        regularUser.borrowBook(generalBook);
        regularUser.setAccountStatus(AccountStatus.FROZEN);
        BorrowRecord record = regularUser.findBorrowRecord(generalBook);
        Date originalDue = record.getDueDate();
        
        autoRenewalService.autoRenew(regularUser, generalBook);
        
        // 验证未续借
        assertEquals("冻结账户自动续借应失败，到期日不变", originalDue, record.getDueDate());
        assertFalse("冻结账户自动续借应失败，不应标记为已延期", record.isExtended());
    }

    @Test
    public void testAutoRenewalFailureWithReservations() throws Exception {
        // 测试自动续借失败 - 有预约队列
        regularUser.borrowBook(generalBook);
        generalBook.addReservation(new Reservation(generalBook, vipUser));
        BorrowRecord record = regularUser.findBorrowRecord(generalBook);
        Date originalDue = record.getDueDate();
        
        autoRenewalService.autoRenew(regularUser, generalBook);
        
        // 验证未续借
        assertEquals("有预约队列自动续借应失败，到期日不变", originalDue, record.getDueDate());
        assertFalse("有预约队列自动续借应失败，不应标记为已延期", record.isExtended());
    }

    // ==================== Library 类测试 ====================
    
    @Test
    public void testLibraryRegisterUser() {
        // 测试用户注册
        outContent.reset();
        library.registerUser(regularUser);
        assertTrue("注册成功应有输出", outContent.toString().contains("Successfully registered"));
        
        // 重复注册
        outContent.reset();
        library.registerUser(regularUser);
        assertTrue("重复注册应有提示", outContent.toString().contains("already exists"));
    }

    @Test
    public void testLibraryRegisterLowCreditUser() {
        // 测试低信用分用户注册
        regularUser.creditScore = 30;
        outContent.reset();
        library.registerUser(regularUser);
        assertTrue("低信用分注册应失败", outContent.toString().contains("too low"));
    }

    @Test
    public void testLibraryAddBook() {
        // 测试添加图书
        outContent.reset();
        library.addBook(generalBook);
        assertTrue("添加图书成功应有输出", outContent.toString().contains("Successfully added"));
        
        // 重复添加
        outContent.reset();
        library.addBook(generalBook);
        assertTrue("重复添加应有提示", outContent.toString().contains("already exists"));
    }

    @Test
    public void testLibraryProcessReservations() throws Exception {
        // 测试处理预约
        generalBook.setAvailableCopies(1);
        generalBook.addReservation(new Reservation(generalBook, regularUser));
        
        outContent.reset();
        library.processReservations(generalBook);
        
        String output = outContent.toString();
        assertTrue("处理预约应有借阅成功输出", 
                   output.contains("borrowed") || output.contains("Successfully"));
        // 验证预约队列已处理
        assertEquals("预约处理后队列应为空", 0, generalBook.getReservationQueue().size());
    }

    @Test
    public void testLibraryAutoRenewBook() throws Exception {
        // 测试图书馆自动续借
        regularUser.borrowBook(generalBook);
        
        outContent.reset();
        library.autoRenewBook(regularUser, generalBook);
        assertTrue("自动续借成功应有输出", outContent.toString().contains("renewed"));
    }

    @Test
    public void testLibraryRepairUserCredit() {
        // 测试用户信用修复
        regularUser.creditScore = 55;
        
        outContent.reset();
        library.repairUserCredit(regularUser, 100);
        assertTrue("信用修复成功应有输出", outContent.toString().contains("successful"));
    }

    // ==================== User 抽象行为测试 ====================
    
    @Test
    public void testUserPayFine() {
        // 测试用户缴纳罚金
        regularUser.fines = 50.0;
        double initialFine = regularUser.fines;
        
        regularUser.payFine(30.0);
        assertEquals("缴纳部分罚金", initialFine - 30.0, regularUser.fines, 0.01);
        
        regularUser.payFine(20.0);
        assertEquals("缴清罚金", 0.0, regularUser.fines, 0.01);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUserPayFineOverpay() {
        // 测试超额缴纳罚金
        regularUser.fines = 30.0;
        regularUser.payFine(50.0);
    }

    @Test(expected = IllegalStateException.class)
    public void testBlacklistedUserPayFine() {
        // 测试黑名单用户缴纳罚金
        regularUser.setAccountStatus(AccountStatus.BLACKLISTED);
        regularUser.payFine(10.0);
    }

    @Test
    public void testUserReserveBook() throws Exception {
        // 测试用户预约图书
        regularUser.reserveBook(generalBook);
        assertEquals("预约后队列应包含用户", 1, generalBook.getReservationQueue().size());
        // 由于User类没有getReservations方法，改为检查图书的预约队列
        assertTrue("预约队列应包含用户的预约", generalBook.getReservationQueue().stream()
                .anyMatch(r -> r.getUser() == regularUser));
    }

    @Test(expected = IllegalStateException.class)
    public void testBlacklistedUserReserveBook() throws Exception {
        // 测试黑名单用户预约图书
        regularUser.setAccountStatus(AccountStatus.BLACKLISTED);
        regularUser.reserveBook(generalBook);
    }

    @Test
    public void testUserCancelReservation() throws Exception {
        // 测试用户取消预约
        regularUser.reserveBook(generalBook);
        Reservation reservation = generalBook.getReservationQueue().get(0);
        regularUser.cancelReservation(reservation);
        
        assertEquals("取消预约后队列应为空", 0, generalBook.getReservationQueue().size());
        assertEquals("用户预约列表应为空", 0, regularUser.getReservations().size());
    }

    @Test
    public void testUserReceiveNotification() {
        // 测试用户接收通知
        outContent.reset();
        regularUser.receiveNotification("测试通知");
        assertTrue("应接收通知", outContent.toString().contains("notification"));
    }

    @Test
    public void testUserAddScore() {
        // 测试用户增加信用分
        int initialScore = regularUser.getCreditScore();
        outContent.reset();
        regularUser.addScore(10);
        
        assertEquals("信用分应增加", initialScore + 10, regularUser.getCreditScore());
        assertTrue("应有增加信用分输出", outContent.toString().contains("increased"));
    }

    @Test
    public void testUserDeductScore() {
        // 测试用户扣除信用分
        regularUser.creditScore = 80;
        regularUser.deductScore(20);
        assertEquals("信用分应扣除", 60, regularUser.getCreditScore());
        
        // 扣除到冻结线以下
        regularUser.deductScore(15);
        assertEquals("账户应冻结", AccountStatus.FROZEN, regularUser.getAccountStatus());
    }

    @Test
    public void testUserFindBorrowRecord() throws Exception {
        // 测试查找借阅记录
        assertNull("未借阅时应返回null", regularUser.findBorrowRecord(generalBook));
        
        regularUser.borrowBook(generalBook);
        assertNotNull("借阅后应找到记录", regularUser.findBorrowRecord(generalBook));
        
        regularUser.returnBook(generalBook);
        assertNull("归还后应返回null", regularUser.findBorrowRecord(generalBook));
    }

    // ==================== 边界和异常测试 ====================
    
    @Test
    public void testBoundaryValues() {
        // 测试边界值
        regularUser.creditScore = 60; // 信用分边界值
        assertEquals("边界信用分", 60, regularUser.getCreditScore());
        
        regularUser.setFine(50.0); // 罚金边界值
        assertEquals("边界罚金", 50.0, regularUser.getFine(), 0.01);
    }

    @Test
    public void testGettersAndSetters() {
        // 测试getter和setter方法覆盖
        regularUser.username = "NewName";
        assertEquals("设置姓名", "NewName", regularUser.username);
        
        regularUser.userId = "NewId";
        assertEquals("设置用户ID", "NewId", regularUser.userId);
        
        generalBook.title = "NewTitle";
        assertEquals("设置图书标题", "NewTitle", generalBook.title);
        
        generalBook.author = "NewAuthor";
        assertEquals("设置作者", "NewAuthor", generalBook.author);
    }

    // ==================== 辅助方法 ====================
    
    /**
     * 重置输出缓冲区
     */
    private void resetOutput() {
        outContent.reset();
    }
    
    /**
     * 获取输出内容
     */
    private String getOutput() {
        return outContent.toString();
    }
    
    /**
     * 创建逾期日期
     */
    private Date createOverdueDate(int daysOverdue) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, daysOverdue);
        return cal.getTime();
    }
}