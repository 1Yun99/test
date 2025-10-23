package net.mooctest;

import org.junit.Test;
import static org.junit.Assert.*;

public class ProductTest {

	// 测试商品名称超过20个字符时抛出异常
	@Test(expected = IllegalArgumentException.class)
	public void testProductNameTooLong() {
		Product product = new Product(1, "P001", "ThisProductNameIsWayTooLong", 100.0, 10, ProductEnum.BOOK, 1.0);
	}

	// 测试商品价格为负数时抛出异常
	@Test(expected = IllegalArgumentException.class)
	public void testNegativePrice() {
		Product product = new Product(2, "P002", "Test Product", -10.0, 5, ProductEnum.DRINK, 1.0);
	}

	// 测试商品价格有超过两位小数时抛出异常
	@Test(expected = IllegalArgumentException.class)
	public void testPriceWithMoreThanTwoDecimalPlaces() {
		Product product = new Product(3, "P003", "Test Product", 10.123, 5, ProductEnum.ELECTRONICS, 1.0);
	}

	// 测试商品折扣设置正常
	@Test
	public void testValidDiscount() {
		Product product = new Product(6, "P004", "Discounted Product", 100.0, 5, ProductEnum.ELECTRONICS, 0.9);
		assertEquals(90.0, product.getPaymentPrice(), 0.01);
	}

	// 测试商品折扣是1时，价格不变
	@Test
	public void testNoDiscount() {
		Product product = new Product(7, "P005", "Full Price Product", 100.0, 5, ProductEnum.BOOK, 1.0);
		assertEquals(100.0, product.getPaymentPrice(), 0.01);
	}

	// 测试商品的价格正常设置和获取
	@Test
	public void testPriceSettingAndGetting() {
		Product product = new Product(8, "P006", "Regular Product", 20.0, 30, ProductEnum.DRINK, 1.0);
		assertEquals(20.0, product.getPrice(), 0.01);
	}

	// 测试商品库存设置正常
	@Test
	public void testProductCount() {
		Product product = new Product(9, "P007", "Test Product", 15.0, 100, ProductEnum.ELECTRONICS, 0.8);
		assertEquals(100, product.getCount());
	}

	// 测试商品库存小于等于零时抛出异常
	@Test(expected = IllegalArgumentException.class)
	public void testProductCountLessThanZero() {
		Product product = new Product(10, "P010", "Invalid Product", 50.0, 0, ProductEnum.BOOK, 0.9);
	}

	// 测试商品信息的输出
	@Test
	public void testProductInfo() {
		Product product = new Product(11, "P011", "Test Product", 100.0, 10, ProductEnum.ELECTRONICS, 0.8);
		String expectedInfo = "Product{id=11, pid='P011', name='Test Product', price=100.0, count=10, productEnum=ELECTRONICS, discount=80%}";
		assertEquals(expectedInfo, product.getInfo());
	}

}
