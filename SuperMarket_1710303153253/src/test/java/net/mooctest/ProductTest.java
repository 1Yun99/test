package net.mooctest;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ProductTest {

    private static final double DELTA = 1e-6;

    @Before
    public void setUp() throws Exception {
        // 中文说明：在每个用例前重置共享状态，避免静态集合导致的相互影响
        Order.orders.clear();
        Method init = Shop.class.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(null);
    }

    private Product createBaseProduct() {
        // 中文说明：构造一个基础商品对象，方便复用
        return new Product(1, "PID001", "基础商品", 10.0, 10, ProductEnum.DRINK, 1.0);
    }

    private String captureOutput(Runnable runnable) {
        // 中文说明：捕获控制台输出，便于校验打印内容
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        try {
            runnable.run();
        } finally {
            System.setOut(originalOut);
        }
        return outputStream.toString();
    }

    // 目的：验证商品名称超过上限时触发异常，预期抛出IllegalArgumentException
    @Test(expected = IllegalArgumentException.class)
    public void testProductNameTooLong() {
        new Product(1, "P001", "超过二十个字符的非法商品名称示例文本", 100.0, 10, ProductEnum.BOOK, 1.0);
    }

    // 目的：验证商品名称恰好二十个字符时允许设置，预期名称被成功保存
    @Test
    public void testProductNameBoundaryAllowed() {
        Product product = createBaseProduct();
        String validName = "ABCDEFGHIJKLMNOPQRST";
        product.setName(validName);
        assertEquals(validName, product.getName());
    }

    // 目的：验证商品价格为负数时触发异常，预期抛出IllegalArgumentException
    @Test(expected = IllegalArgumentException.class)
    public void testNegativePrice() {
        new Product(2, "P002", "价格非法商品", -10.0, 5, ProductEnum.DRINK, 1.0);
    }

    // 目的：验证商品价格为零时触发异常，预期抛出IllegalArgumentException
    @Test(expected = IllegalArgumentException.class)
    public void testZeroPrice() {
        new Product(3, "P003", "零价格商品", 0.0, 5, ProductEnum.ELECTRONICS, 1.0);
    }

    // 目的：验证价格精度超过两位小数时触发异常，预期抛出IllegalArgumentException
    @Test(expected = IllegalArgumentException.class)
    public void testPriceWithMoreThanTwoDecimalPlaces() {
        new Product(4, "P004", "价格小数过长商品", 10.123, 5, ProductEnum.ELECTRONICS, 1.0);
    }

    // 目的：验证价格为两位小数时允许设置，预期商品正常创建
    @Test
    public void testPriceWithTwoDecimalPlacesAllowed() {
        Product product = new Product(5, "P005", "两位小数商品", 10.99, 2, ProductEnum.DRINK, 1.0);
        assertEquals(10.99, product.getPrice(), DELTA);
    }

    // 目的：验证简化构造方法赋值正确，预期默认折扣为1
    @Test
    public void testSimpleConstructorDefaults() {
        Product product = new Product("简易商品", 5.0, 3);
        assertEquals("简易商品", product.getName());
        assertEquals(5.0, product.getPrice(), DELTA);
        assertEquals(3, product.getCount());
        assertEquals(1.0, product.getDiscount(), DELTA);
        assertNull(product.getProductEnum());
    }

    // 目的：验证合法商品属性能够正确设置与读取，预期所有getter返回正确数值
    @Test
    public void testValidProductSettersAndGetters() {
        Product product = new Product(6, "P006", "正常商品", 50.0, 20, ProductEnum.BOOK, 0.8);
        product.setId(7);
        product.setPid("P007");
        product.setProductEnum(ProductEnum.DRINK);
        assertEquals("正常商品", product.getName());
        assertEquals(50.0, product.getPrice(), DELTA);
        assertEquals(20, product.getCount());
        assertEquals(ProductEnum.DRINK, product.getProductEnum());
        assertEquals(7, product.getId());
        assertEquals("P007", product.getPid());
    }

    // 目的：验证库存小于等于零时触发异常，预期抛出IllegalArgumentException
    @Test(expected = IllegalArgumentException.class)
    public void testProductCountLessThanOrEqualZero() {
        new Product(8, "P008", "库存非法商品", 50.0, 0, ProductEnum.BOOK, 0.9);
    }

    // 目的：验证折扣小于等于零时触发异常，预期抛出IllegalArgumentException
    @Test(expected = IllegalArgumentException.class)
    public void testDiscountLessThanOrEqualZero() {
        Product product = createBaseProduct();
        product.setDiscount(-0.01);
    }

    // 目的：验证折扣大于一时触发异常，预期抛出IllegalArgumentException
    @Test(expected = IllegalArgumentException.class)
    public void testDiscountGreaterThanOne() {
        Product product = createBaseProduct();
        product.setDiscount(1.01);
    }

    // 目的：验证折扣精度超过两位小数时触发异常，预期抛出IllegalArgumentException
    @Test(expected = IllegalArgumentException.class)
    public void testDiscountWithTooManyDecimalPlaces() {
        Product product = createBaseProduct();
        product.setDiscount(0.123);
    }

    // 目的：验证折扣为两位小数时允许设置，预期折扣与售价正确
    @Test
    public void testDiscountWithTwoDecimalPlacesAllowed() {
        Product product = createBaseProduct();
        product.setDiscount(0.55);
        assertEquals(0.55, product.getDiscount(), DELTA);
        assertEquals(5.5, product.getPaymentPrice(), DELTA);
    }

    // 目的：验证折扣正常设置后计算售价正确，预期售卖价格为原价乘折扣
    @Test
    public void testValidDiscountAffectPaymentPrice() {
        Product product = new Product(9, "P009", "折扣商品", 100.0, 5, ProductEnum.ELECTRONICS, 0.75);
        assertEquals(75.0, product.getPaymentPrice(), DELTA);
    }

    // 目的：验证商品信息在无折扣时显示“无折扣”，预期折扣字段为"No discount"
    @Test
    public void testProductInfoWithoutDiscount() {
        Product product = new Product(10, "P010", "无折扣商品", 80.0, 8, ProductEnum.DRINK, 1.0);
        String expected = "Product{id=10, pid='P010', name='无折扣商品', price=80.0, count=8, productEnum=DRINK, discount=No discount}";
        assertEquals(expected, product.getInfo());
    }

    // 目的：验证商品信息在有折扣时显示折扣百分比，预期折扣字段为具体百分比
    @Test
    public void testProductInfoWithDiscount() {
        Product product = new Product(11, "P011", "打折商品", 200.0, 3, ProductEnum.ELECTRONICS, 0.8);
        String expected = "Product{id=11, pid='P011', name='打折商品', price=200.0, count=3, productEnum=ELECTRONICS, discount=80%}";
        assertEquals(expected, product.getInfo());
    }

    // 目的：验证订单项金额计算与格式化输出，预期金额为单价乘数量且格式化为两位小数
    @Test
    public void testOrderItemPrintFormat() {
        OrderItem item = new OrderItem("测试商品", 12.345, 3);
        String expected = "测试商品\t12.34\t3\t37.03";
        assertEquals(expected, item.PrintOrderItem());
    }

    // 目的：验证订单项的setter能够生效，预期所有属性更新后正确返回
    @Test
    public void testOrderItemSetters() {
        OrderItem item = new OrderItem("原始商品", 5.0, 2);
        item.setProductName("更新商品");
        item.setPaymentPrice(6.5);
        item.setCount(4);
        assertEquals("更新商品", item.getProductName());
        assertEquals(6.5, item.getPaymentPrice(), DELTA);
        assertEquals(4, item.getCount());
        assertEquals(26.0, item.getAmount(), DELTA);
    }

    // 目的：验证订单总金额计算逻辑，预期返回各订单项金额之和
    @Test
    public void testOrderTotalAmount() {
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem("商品A", 20.0, 2));
        items.add(new OrderItem("商品B", 30.0, 1));
        Order order = new Order(items);
        assertEquals(70.0, order.totalAmount(), DELTA);
    }

    // 目的：验证订单项为空时总金额为零，预期返回0
    @Test
    public void testOrderTotalAmountEmpty() {
        Order order = new Order(new ArrayList<OrderItem>());
        assertEquals(0.0, order.totalAmount(), DELTA);
    }

    // 目的：验证订单金额不足500时不打折，预期订单金额保持原值
    @Test
    public void testCreateOrderWithoutDiscount() {
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem("商品A", 100.0, 2));
        Order.createOrder(items);
        assertEquals(1, Order.orders.size());
        assertEquals(200.0, Order.orders.get(0).totalAmount(), DELTA);
        assertEquals(100.0, Order.orders.get(0).getItems().get(0).getPaymentPrice(), DELTA);
    }

    // 目的：验证订单金额恰好500时执行九折，预期单价降为原价的90%
    @Test
    public void testCreateOrderBoundaryFiveHundred() {
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem("商品C", 100.0, 5));
        Order.createOrder(items);
        Order savedOrder = Order.orders.get(0);
        assertEquals(450.0, savedOrder.totalAmount(), DELTA);
        assertEquals(90.0, savedOrder.getItems().get(0).getPaymentPrice(), DELTA);
    }

    // 目的：验证订单金额位于500到1000之间时给予九折优惠，预期总额打九折
    @Test
    public void testCreateOrderWithNinetyPercentDiscount() {
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem("商品A", 200.0, 3));
        Order.createOrder(items);
        Order savedOrder = Order.orders.get(0);
        assertEquals(540.0, savedOrder.totalAmount(), DELTA);
        assertEquals(180.0, savedOrder.getItems().get(0).getPaymentPrice(), DELTA);
    }

    // 目的：验证订单金额恰好1000时执行八折，预期单价降为原价的80%
    @Test
    public void testCreateOrderWithEightyPercentDiscount() {
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem("商品B", 250.0, 4));
        Order.createOrder(items);
        Order savedOrder = Order.orders.get(0);
        assertEquals(800.0, savedOrder.totalAmount(), DELTA);
        assertEquals(200.0, savedOrder.getItems().get(0).getPaymentPrice(), DELTA);
    }

    // 目的：验证订单金额超过1000时仍执行八折，预期单价降为原价的80%
    @Test
    public void testCreateOrderGreaterThanOneThousand() {
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem("商品D", 400.0, 3));
        Order.createOrder(items);
        Order savedOrder = Order.orders.get(0);
        assertEquals(960.0, savedOrder.totalAmount(), DELTA);
        assertEquals(320.0, savedOrder.getItems().get(0).getPaymentPrice(), DELTA);
    }

    // 目的：验证订单打印信息包含每张订单与总金额，预期输出含有关键字段
    @Test
    public void testPrintOrdersOutput() {
        List<OrderItem> orderItems1 = new ArrayList<>();
        orderItems1.add(new OrderItem("商品A", 100.0, 2));
        Order.createOrder(orderItems1);
        List<OrderItem> orderItems2 = new ArrayList<>();
        orderItems2.add(new OrderItem("商品B", 500.0, 2));
        Order.createOrder(orderItems2);
        String output = captureOutput(Order::printOrders);
        assertTrue(output.contains("Order No.1"));
        assertTrue(output.contains("Order No.2"));
        assertTrue(output.contains("商品A\t100.00\t2\t200.00"));
        assertTrue(output.contains("商品B\t400.00\t2\t800.00"));
        assertTrue(output.contains("Order Total:"));
        assertTrue(output.contains("AllAmount: 1000.00"));
    }

    // 目的：验证搜索最大金额订单逻辑，预期返回总价最高的订单
    @Test
    public void testSearchMaxOrder() {
        List<OrderItem> orderItems1 = new ArrayList<>();
        orderItems1.add(new OrderItem("商品A", 100.0, 2));
        Order.createOrder(orderItems1);
        List<OrderItem> orderItems2 = new ArrayList<>();
        orderItems2.add(new OrderItem("商品B", 500.0, 2));
        Order.createOrder(orderItems2);
        Order maxOrder = Order.searchMaxOrder();
        assertEquals(800.0, maxOrder.totalAmount(), DELTA);
        assertSame(maxOrder, Order.orders.get(0));
    }

    // 目的：验证最大订单在金额相等时依旧能返回有效结果，预期返回值为等额订单之一
    @Test
    public void testSearchMaxOrderWithEqualTotals() {
        List<OrderItem> orderItems1 = new ArrayList<>();
        orderItems1.add(new OrderItem("商品C", 100.0, 5));
        Order.createOrder(orderItems1);
        List<OrderItem> orderItems2 = new ArrayList<>();
        orderItems2.add(new OrderItem("商品D", 50.0, 10));
        Order.createOrder(orderItems2);
        Order first = Order.orders.get(0);
        Order second = Order.orders.get(1);
        Order maxOrder = Order.searchMaxOrder();
        assertEquals(450.0, maxOrder.totalAmount(), DELTA);
        assertTrue(maxOrder == first || maxOrder == second);
        assertEquals(maxOrder, Order.orders.get(0));
        assertEquals(Order.orders.get(0).totalAmount(), Order.orders.get(1).totalAmount(), DELTA);
    }

    // 目的：验证金额格式化采用截断模式，预期小数第三位被直接舍去
    @Test
    public void testOrderFormatDoubleRoundingDown() {
        assertEquals("12.34", Order.formatDouble(12.349));
        assertEquals("12.35", Order.formatDouble(12.359));
    }

    // 目的：验证商品信息按照售价排序且相同价格按名称排序，预期顺序符合排序规则
    @Test
    public void testGetAllProductsInfoSortedWithTie() {
        Shop shop = new Shop();
        shop.addProduct(new Product(12, "P012", "AA饮料", 2.0, 10, ProductEnum.DRINK, 1.0));
        String info = shop.getAllProductsInfo();
        assertTrue(info.indexOf("AA饮料") < info.indexOf("矿泉水"));
    }

    // 目的：验证追加已有商品时数量累加，预期返回位置索引且库存增加
    @Test
    public void testAddProductExisting() {
        Shop shop = new Shop();
        int previousIndex = shop.searchProduct("冰红茶");
        Product extra = new Product(13, "P013", "冰红茶", 3.0, 5, ProductEnum.DRINK, 1.0);
        int index = shop.addProduct(extra);
        assertEquals(previousIndex, index);
        assertEquals(105, shop.getProductByName("冰红茶").getCount());
    }

    // 目的：验证追加新商品时返回总数量，预期能够在列表中找到该商品
    @Test
    public void testAddProductNew() {
        Shop shop = new Shop();
        Product extra = new Product(14, "P014", "全新商品", 15.0, 7, ProductEnum.ELECTRONICS, 1.0);
        int result = shop.addProduct(extra);
        assertEquals(result - 1, shop.searchProduct("全新商品"));
        assertNotNull(shop.getProductByName("全新商品"));
    }

    // 目的：验证删除存在商品时返回正确索引并从列表中移除，预期删除后无法再找到
    @Test
    public void testDeleteProductSuccess() {
        Shop shop = new Shop();
        int expectedIndex = shop.searchProduct("健力宝");
        int index = shop.deletProduct("健力宝");
        assertEquals(expectedIndex, index);
        assertNull(shop.getProductByName("健力宝"));
    }

    // 目的：验证删除不存在商品时抛出空指针异常，预期捕获NullPointerException
    @Test(expected = NullPointerException.class)
    public void testDeleteProductNotFound() {
        Shop shop = new Shop();
        shop.deletProduct("不存在商品");
    }

    // 目的：验证售卖商品库存足够时成功扣减，预期返回剩余库存
    @Test
    public void testSellProductSuccess() {
        Shop shop = new Shop();
        int remaining = shop.sellProduct("矿泉水", 10);
        assertEquals(90, remaining);
        assertEquals(90, shop.getProductByName("矿泉水").getCount());
    }

    // 目的：验证售卖不存在的商品时抛出异常，预期捕获NullPointerException
    @Test(expected = NullPointerException.class)
    public void testSellProductNotFound() {
        Shop shop = new Shop();
        shop.sellProduct("不存在商品", 1);
    }

    // 目的：验证库存不足时售卖失败，预期抛出IllegalArgumentException
    @Test(expected = IllegalArgumentException.class)
    public void testSellProductInsufficient() {
        Shop shop = new Shop();
        shop.sellProduct("矿泉水", 101);
    }

    // 目的：验证售卖数量小于等于零时失败，预期抛出IllegalArgumentException
    @Test(expected = IllegalArgumentException.class)
    public void testSellProductInvalidCount() {
        Shop shop = new Shop();
        shop.sellProduct("矿泉水", 0);
    }

    // 目的：验证商品更新价格与折扣成功，预期字段更新并返回商品实例
    @Test
    public void testUpdateProductSuccess() {
        Shop shop = new Shop();
        Product updated = shop.updateProduct("矿泉水", 5.5, 0.9);
        assertEquals(5.5, updated.getPrice(), DELTA);
        assertEquals(0.9, updated.getDiscount(), DELTA);
        assertEquals(4.95, updated.getPaymentPrice(), DELTA);
    }

    // 目的：验证带类型的商品更新成功，预期返回的新类型被保存
    @Test
    public void testUpdateProductWithEnumSuccess() {
        Shop shop = new Shop();
        Product updated = shop.updateProduct("矿泉水", 4.0, 0.5, ProductEnum.BOOK);
        assertEquals(ProductEnum.BOOK, updated.getProductEnum());
        assertEquals(4.0, updated.getPrice(), DELTA);
        assertEquals(0.5, updated.getDiscount(), DELTA);
    }

    // 目的：验证更新不存在商品时抛出空指针异常，预期捕获NullPointerException
    @Test(expected = NullPointerException.class)
    public void testUpdateProductNotFound() {
        Shop shop = new Shop();
        shop.updateProduct("不存在商品", 5.0, 0.9);
    }

    // 目的：验证带类型的更新在商品不存在时同样抛异常，预期捕获NullPointerException
    @Test(expected = NullPointerException.class)
    public void testUpdateProductWithEnumNotFound() {
        Shop shop = new Shop();
        shop.updateProduct("不存在商品", 5.0, 0.9, ProductEnum.DRINK);
    }

    // 目的：验证搜索商品接口，预期存在返回非负索引，不存在返回-1
    @Test
    public void testSearchProductFoundAndNotFound() {
        Shop shop = new Shop();
        assertTrue(shop.searchProduct("矿泉水") >= 0);
        assertEquals(-1, shop.searchProduct("找不到的商品"));
    }

    // 目的：验证根据名称获取商品在不存在时返回null，预期空值
    @Test
    public void testGetProductByNameReturnNull() {
        Shop shop = new Shop();
        assertNull(shop.getProductByName("未上架商品"));
    }

    // 目的：验证店员展示全部商品信息，预期输出与商店查询结果一致
    @Test
    public void testShowAllProducts() {
        Shop shop = new Shop();
        ShopKeeper keeper = new ShopKeeper();
        keeper.setShop(shop);
        String output = captureOutput(keeper::showAllProducts);
        assertEquals(shop.getAllProductsInfo(), output);
    }

    // 目的：验证店员售卖时部分商品失败仍可生成订单，预期输出包含成功与失败提示
    @Test
    public void testSellProductsPartialFailure() {
        Shop shop = new Shop();
        ShopKeeper keeper = new ShopKeeper();
        keeper.setShop(shop);
        Map<String, Integer> request = new LinkedHashMap<>();
        request.put("电吹风", 1);
        request.put("不存在商品", 1);
        String output = captureOutput(() -> keeper.sellProducts(request));
        assertEquals(1, Order.orders.size());
        assertTrue(output.contains("Selld Successfully:电吹风*1"));
        assertTrue(output.contains("Selld Failed:Product is not exists."));
    }

    // 目的：验证店员售卖全部失败时不生成订单，预期订单列表保持为空
    @Test
    public void testSellProductsAllFail() {
        Shop shop = new Shop();
        ShopKeeper keeper = new ShopKeeper();
        keeper.setShop(shop);
        Map<String, Integer> request = new LinkedHashMap<>();
        request.put("电吹风", 0);
        String output = captureOutput(() -> keeper.sellProducts(request));
        assertEquals(0, Order.orders.size());
        assertTrue(output.contains("Selld Failed:Count cannot less than 0."));
    }

    // 目的：验证店员售卖成功时减少库存并创建订单，预期库存降低且输出成功提示
    @Test
    public void testSellProductsSuccess() {
        Shop shop = new Shop();
        ShopKeeper keeper = new ShopKeeper();
        keeper.setShop(shop);
        Map<String, Integer> request = new LinkedHashMap<>();
        request.put("矿泉水", 2);
        request.put("冰红茶", 3);
        String output = captureOutput(() -> keeper.sellProducts(request));
        assertEquals(1, Order.orders.size());
        assertEquals(98, shop.getProductByName("矿泉水").getCount());
        assertEquals(97, shop.getProductByName("冰红茶").getCount());
        assertTrue(output.contains("Selld Successfully:矿泉水*2"));
        assertTrue(output.contains("Selld Successfully:冰红茶*3"));
    }
}
