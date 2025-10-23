package net.mooctest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Shop {

    private static List<Product> products;

    static {
        init();
    }

    public static void main (String args [] ){
        Shop s = new Shop();
        System.out.println(s.getAllProductsInfo());
    }

    private static void init (){
        products = new ArrayList<>();
        Product product1 = new Product(1, "123456789", "电吹风", 188, 3, ProductEnum.ELECTRONICS, 1);
        Product product2 = new Product(2, "23456789", "《计算机网络》", 18, 10, ProductEnum.BOOK, 0.8);
        Product product3 = new Product(3, "3456789", "矿泉水", 2, 100, ProductEnum.DRINK, 1);
        Product product4 = new Product(4, "456789", "冰红茶", 3, 100, ProductEnum.DRINK, 1);
        Product product5 = new Product(5, "56789", "健力宝", 4, 100, ProductEnum.DRINK, 1);
        products.add(product1);
        products.add(product2);
        products.add(product3);
        products.add(product4);
        products.add(product5);
    }

    /**
     * 对所有商品进行排序后输出信息
     */
    public String getAllProductsInfo(){
        String info = "";
        String sep = System.getProperty("line.separator");
        Collections.sort(products, (p1, p2) -> {
            if(p1.getPaymentPrice()>p2.getPaymentPrice())
                return 1;
            else if (p1.getPaymentPrice()<p2.getPaymentPrice())
                return -1;
            else{
                return p1.name.compareTo(p2.name);
            }
        });
        for (int i = 0; i<products.size(); i++){
            info += ("No."+(i+1)+sep+products.get(i).getInfo()+sep);
        }
        return info;
    }


    /**
     * 添加商品
     * @param product
     */
    public int addProduct(Product product){
        int productIndex = this.searchProduct(product.name);
        if (productIndex!=-1){
            products.get(productIndex).setCount(products.get(productIndex).count+product.count);
            return productIndex;
        }else{
            products.add(product);
            return products.size();
        }

    }

    /**
     * 下架商品,将商品从列表中删除
     * @param productName
     */
    public int deletProduct(String productName){
        int productIndex = this.searchProduct(productName);
        if (productIndex == -1)
            throw new NullPointerException("Product is not exists.");
        else
            products.remove(productIndex);
        return productIndex;
    }

    /**
     * 出售商品
     * @param productName
     * @param count
     */
    public int sellProduct(String productName, int count){
        int productIndex = this.searchProduct(productName);
        if (productIndex == -1)
            throw new NullPointerException("Product is not exists.");
        else if (products.get(productIndex).count<count)
            throw new IllegalArgumentException("Quantity of remaining products is not enough.");
        else if (count<=0)
            throw new IllegalArgumentException("Count cannot less than 0.");
        products.get(productIndex).setCount(products.get(productIndex).count-count);
        return products.get(productIndex).count;
    }

    /**
     * 更新商品信息
     * @param productName
     * @param price
     */
    public Product updateProduct(String productName, double price, double discount){
        int productIndex = this.searchProduct(productName);
        if (productIndex == -1)
            throw new NullPointerException("Product is not exists.");
        products.get(productIndex).setPrice(price);
        products.get(productIndex).setDiscount(discount);
        return products.get(productIndex);
    }

    public Product updateProduct(String productName, double price, double discount,ProductEnum productEnum){
        int productIndex = this.searchProduct(productName);
        if (productIndex == -1)
            throw new NullPointerException("Product is not exists.");
        products.get(productIndex).setPrice(price);
        products.get(productIndex).setDiscount(discount);
        products.get(productIndex).setProductEnum(productEnum);
        return products.get(productIndex);
    }


    /**
     * 根据商品名称查找商品
     * @param productName
     * @return
     */
    public int searchProduct(String productName){
        int index = -1;
        for(int i = 0; i<products.size(); i++){
            if (products.get(i).name.equals(productName)){
                index = i;
                break;
            }
        }
        return index;
    }

    /**
     * 根据商品名称查找商品
     * @param productName
     * @return
     */
    public Product getProductByName(String productName){
        int index = searchProduct(productName);
        if (index<0)
            return null;
        return products.get(index);
    }


}
