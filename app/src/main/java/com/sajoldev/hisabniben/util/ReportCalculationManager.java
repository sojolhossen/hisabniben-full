package com.sajoldev.hisabniben.util;

import com.sajoldev.hisabniben.model.Customer;
import com.sajoldev.hisabniben.model.Expense;
import com.sajoldev.hisabniben.model.Purchase;
import com.sajoldev.hisabniben.model.PurchaseItem;
import com.sajoldev.hisabniben.model.RiceProduct;
import com.sajoldev.hisabniben.model.Sale;
import com.sajoldev.hisabniben.model.SaleItem;
import com.sajoldev.hisabniben.model.Supplier;
import com.sajoldev.hisabniben.model.WalletTransaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportCalculationManager {

    // =========================================================================
    // DATA DTO CLASSES FOR REPORT CALCULATIONS
    // =========================================================================

    public static class OverallSummary {
        public double totalSales;
        public double totalPurchases;
        public double totalExpenses;
        public double totalCogs;
        public double grossProfit;
        public double netProfit;
        public double totalCustomerDue;
        public double totalSupplierPayable;
        public double totalStockValue;
        public double totalMoneyIn;
        public double totalMoneyOut;
        public double netCashFlow;
    }

    public static class SalesSummary {
        public double totalSales;
        public double totalPaid;
        public double totalDue;
        public int invoiceCount;
        public double averageSaleValue;
        public double totalKg;
        public double totalBags;
    }

    public static class PurchaseSummary {
        public double totalPurchase;
        public double totalPaid;
        public double totalPayable;
        public int purchaseCount;
        public double averagePurchaseValue;
        public double totalKg;
        public double totalBags;
    }

    public static class ProfitLossSummary {
        public double salesRevenue;
        public double cogs;
        public double grossProfit;
        public double expenses;
        public double netProfit;
        public double grossProfitMargin;
        public double netProfitMargin;
    }

    public static class StockSummary {
        public double totalBags;
        public double totalKg;
        public double totalValuation;
        public int productCount;
        public int lowStockCount;
    }

    public static class ProductPerformanceItem {
        public String productId;
        public String productName;
        public String brand;
        public double salesKg;
        public double salesBags;
        public double salesRevenue;
        public double cogs;
        public double grossProfit;
        public double profitMargin;
    }

    // =========================================================================
    // CALCULATION LOGIC METHODS
    // =========================================================================

    public static OverallSummary calculateOverallSummary(
            List<Sale> sales,
            List<Purchase> purchases,
            List<Expense> expenses,
            List<Customer> customers,
            List<Supplier> suppliers,
            List<RiceProduct> products,
            List<WalletTransaction> walletTransactions,
            long startDate,
            long endDate) {

        OverallSummary summary = new OverallSummary();

        // 1. Sales & COGS
        if (sales != null) {
            for (Sale sale : sales) {
                if (sale != null && !Sale.SALE_STATUS_CANCELLED.equals(sale.getSaleStatus())) {
                    long date = sale.getSaleDate() > 0 ? sale.getSaleDate() : sale.getCreatedAt();
                    if (isWithinRange(date, startDate, endDate)) {
                        summary.totalSales += sale.getGrandTotal();
                        if (sale.getItems() != null) {
                            for (SaleItem item : sale.getItems()) {
                                double buyingPrice = item.getCostPerKg();
                                if (buyingPrice <= 0 && item.getSaleRatePerKg() > 0) {
                                    buyingPrice = item.getSaleRatePerKg() * 0.88; // Estimated cost if not provided
                                }
                                summary.totalCogs += (item.getTotalKg() * buyingPrice);
                            }
                        }
                    }
                }
            }
        }

        // 2. Purchases
        if (purchases != null) {
            for (Purchase purchase : purchases) {
                if (purchase != null) {
                    long date = purchase.getPurchaseDate() > 0 ? purchase.getPurchaseDate() : purchase.getCreatedAt();
                    if (isWithinRange(date, startDate, endDate)) {
                        summary.totalPurchases += purchase.getGrandTotal();
                    }
                }
            }
        }

        // 3. Expenses
        if (expenses != null) {
            for (Expense exp : expenses) {
                if (exp != null) {
                    long date = exp.getDate() > 0 ? exp.getDate() : exp.getCreatedAt();
                    if (isWithinRange(date, startDate, endDate)) {
                        summary.totalExpenses += exp.getAmount();
                    }
                }
            }
        }

        // 4. Customer Due
        if (customers != null) {
            for (Customer c : customers) {
                if (c != null) summary.totalCustomerDue += c.getBaki();
            }
        }

        // 5. Supplier Payable
        if (suppliers != null) {
            for (Supplier s : suppliers) {
                if (s != null) summary.totalSupplierPayable += s.getCurrentPayable();
            }
        }

        // 6. Stock Valuation
        if (products != null) {
            for (RiceProduct p : products) {
                if (p != null) {
                    double wac = p.getPurchaseRatePerKg();
                    if (wac <= 0 && p.getSaleRatePerKg() > 0) {
                        wac = p.getSaleRatePerKg() * 0.88;
                    }
                    summary.totalStockValue += (p.getCurrentStockKg() * wac);
                }
            }
        }

        // 7. Wallet Cash Flow
        if (walletTransactions != null) {
            for (WalletTransaction wt : walletTransactions) {
                if (wt != null && !WalletTransaction.STATUS_REVERSED.equals(wt.getStatus())) {
                    long date = wt.getTransactionDate() > 0 ? wt.getTransactionDate() : wt.getCreatedAt();
                    if (isWithinRange(date, startDate, endDate)) {
                        if (WalletTransaction.DIRECTION_IN.equals(wt.getDirection())) {
                            summary.totalMoneyIn += wt.getAmount();
                        } else if (WalletTransaction.DIRECTION_OUT.equals(wt.getDirection())) {
                            summary.totalMoneyOut += wt.getAmount();
                        }
                    }
                }
            }
        }

        summary.grossProfit = summary.totalSales - summary.totalCogs;
        summary.netProfit = summary.grossProfit - summary.totalExpenses;
        summary.netCashFlow = summary.totalMoneyIn - summary.totalMoneyOut;

        return summary;
    }

    public static SalesSummary calculateSalesSummary(List<Sale> sales, long startDate, long endDate) {
        SalesSummary summary = new SalesSummary();
        if (sales == null) return summary;

        for (Sale sale : sales) {
            if (sale != null && !Sale.SALE_STATUS_CANCELLED.equals(sale.getSaleStatus())) {
                long date = sale.getSaleDate() > 0 ? sale.getSaleDate() : sale.getCreatedAt();
                if (isWithinRange(date, startDate, endDate)) {
                    summary.invoiceCount++;
                    summary.totalSales += sale.getGrandTotal();
                    summary.totalPaid += sale.getPaidAmount();
                    summary.totalDue += sale.getDueAmount();

                    if (sale.getItems() != null) {
                        for (SaleItem item : sale.getItems()) {
                            summary.totalKg += item.getTotalKg();
                            summary.totalBags += item.getBagQuantity();
                        }
                    }
                }
            }
        }

        if (summary.invoiceCount > 0) {
            summary.averageSaleValue = summary.totalSales / summary.invoiceCount;
        }

        return summary;
    }

    public static PurchaseSummary calculatePurchaseSummary(List<Purchase> purchases, long startDate, long endDate) {
        PurchaseSummary summary = new PurchaseSummary();
        if (purchases == null) return summary;

        for (Purchase purchase : purchases) {
            if (purchase != null) {
                long date = purchase.getPurchaseDate() > 0 ? purchase.getPurchaseDate() : purchase.getCreatedAt();
                if (isWithinRange(date, startDate, endDate)) {
                    summary.purchaseCount++;
                    summary.totalPurchase += purchase.getGrandTotal();
                    summary.totalPaid += purchase.getPaidAmount();
                    summary.totalPayable += purchase.getDueAmount();

                    if (purchase.getItems() != null) {
                        for (PurchaseItem item : purchase.getItems()) {
                            summary.totalKg += item.getTotalKg();
                            summary.totalBags += item.getBagQuantity();
                        }
                    }
                }
            }
        }

        if (summary.purchaseCount > 0) {
            summary.averagePurchaseValue = summary.totalPurchase / summary.purchaseCount;
        }

        return summary;
    }

    public static ProfitLossSummary calculateProfitLossSummary(List<Sale> sales, List<Expense> expenses, long startDate, long endDate) {
        ProfitLossSummary summary = new ProfitLossSummary();

        if (sales != null) {
            for (Sale sale : sales) {
                if (sale != null && !Sale.SALE_STATUS_CANCELLED.equals(sale.getSaleStatus())) {
                    long date = sale.getSaleDate() > 0 ? sale.getSaleDate() : sale.getCreatedAt();
                    if (isWithinRange(date, startDate, endDate)) {
                        summary.salesRevenue += sale.getGrandTotal();
                        if (sale.getItems() != null) {
                            for (SaleItem item : sale.getItems()) {
                                double buyingPrice = item.getCostPerKg();
                                if (buyingPrice <= 0 && item.getSaleRatePerKg() > 0) {
                                    buyingPrice = item.getSaleRatePerKg() * 0.88;
                                }
                                summary.cogs += (item.getTotalKg() * buyingPrice);
                            }
                        }
                    }
                }
            }
        }

        if (expenses != null) {
            for (Expense exp : expenses) {
                if (exp != null) {
                    long date = exp.getDate() > 0 ? exp.getDate() : exp.getCreatedAt();
                    if (isWithinRange(date, startDate, endDate)) {
                        summary.expenses += exp.getAmount();
                    }
                }
            }
        }

        summary.grossProfit = summary.salesRevenue - summary.cogs;
        summary.netProfit = summary.grossProfit - summary.expenses;

        if (summary.salesRevenue > 0) {
            summary.grossProfitMargin = (summary.grossProfit / summary.salesRevenue) * 100.0;
            summary.netProfitMargin = (summary.netProfit / summary.salesRevenue) * 100.0;
        }

        return summary;
    }

    public static StockSummary calculateStockSummary(List<RiceProduct> products) {
        StockSummary summary = new StockSummary();
        if (products == null) return summary;

        summary.productCount = products.size();
        for (RiceProduct p : products) {
            if (p != null) {
                summary.totalBags += p.getCurrentStockBags();
                summary.totalKg += p.getCurrentStockKg();

                double wac = p.getPurchaseRatePerKg();
                if (wac <= 0 && p.getSaleRatePerKg() > 0) {
                    wac = p.getSaleRatePerKg() * 0.88;
                }
                summary.totalValuation += (p.getCurrentStockKg() * wac);

                if (p.getMinStockAlertKg() > 0 && p.getCurrentStockKg() <= p.getMinStockAlertKg()) {
                    summary.lowStockCount++;
                }
            }
        }

        return summary;
    }

    public static List<ProductPerformanceItem> calculateProductPerformance(List<Sale> sales, long startDate, long endDate) {
        Map<String, ProductPerformanceItem> map = new HashMap<>();

        if (sales != null) {
            for (Sale sale : sales) {
                if (sale != null && !Sale.SALE_STATUS_CANCELLED.equals(sale.getSaleStatus())) {
                    long date = sale.getSaleDate() > 0 ? sale.getSaleDate() : sale.getCreatedAt();
                    if (isWithinRange(date, startDate, endDate) && sale.getItems() != null) {
                        for (SaleItem item : sale.getItems()) {
                            String name = item.getProductNameSnapshot() != null ? item.getProductNameSnapshot() : "Rice";
                            String key = item.getProductId() != null ? item.getProductId() : name;
                            ProductPerformanceItem pItem = map.get(key);
                            if (pItem == null) {
                                pItem = new ProductPerformanceItem();
                                pItem.productId = item.getProductId();
                                pItem.productName = name;
                                pItem.brand = item.getBrandSnapshot();
                                map.put(key, pItem);
                            }

                            double buyingPrice = item.getCostPerKg();
                            if (buyingPrice <= 0 && item.getSaleRatePerKg() > 0) {
                                buyingPrice = item.getSaleRatePerKg() * 0.88;
                            }

                            pItem.salesKg += item.getTotalKg();
                            pItem.salesBags += item.getBagQuantity();
                            pItem.salesRevenue += item.getItemTotal();
                            pItem.cogs += (item.getTotalKg() * buyingPrice);
                        }
                    }
                }
            }
        }

        List<ProductPerformanceItem> list = new ArrayList<>(map.values());
        for (ProductPerformanceItem item : list) {
            item.grossProfit = item.salesRevenue - item.cogs;
            if (item.salesRevenue > 0) {
                item.profitMargin = (item.grossProfit / item.salesRevenue) * 100.0;
            }
        }

        list.sort((a, b) -> Double.compare(b.grossProfit, a.grossProfit));
        return list;
    }

    private static boolean isWithinRange(long timestamp, long startDate, long endDate) {
        if (startDate <= 0 && endDate <= 0) return true;
        if (startDate > 0 && timestamp < startDate) return false;
        if (endDate > 0 && timestamp > endDate) return false;
        return true;
    }
}
