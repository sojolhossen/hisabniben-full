package com.sajoldev.hisabniben.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class UnitConverterHelper {
    public static final double DEFAULT_BAG_WEIGHT = 50.0;

    /**
     * Converts Bag Quantity to Total Kilograms
     */
    public static double bagsToKg(double bags, double bagWeightKg) {
        if (bagWeightKg <= 0) bagWeightKg = DEFAULT_BAG_WEIGHT;
        return bags * bagWeightKg;
    }

    /**
     * Converts Kilograms to Bag Quantity
     */
    public static double kgToBags(double totalKg, double bagWeightKg) {
        if (bagWeightKg <= 0) bagWeightKg = DEFAULT_BAG_WEIGHT;
        return totalKg / bagWeightKg;
    }

    /**
     * Calculates Weighted Average Cost (WAC) per KG after a new purchase
     */
    public static double calculateWeightedAverageCost(double currentStockKg, double currentWacPerKg, double newPurchaseKg, double newPurchaseRatePerKg) {
        if (currentStockKg <= 0) {
            return newPurchaseRatePerKg;
        }
        double totalStock = currentStockKg + newPurchaseKg;
        if (totalStock <= 0) return newPurchaseRatePerKg;
        
        double totalValue = (currentStockKg * currentWacPerKg) + (newPurchaseKg * newPurchaseRatePerKg);
        return totalValue / totalStock;
    }

    /**
     * Formats currency using Taka symbol with Western digits e.g. ৳1,099
     */
    public static String formatCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(2);
        return "৳" + formatter.format(amount);
    }

    /**
     * Formats stock display e.g. "200 Bags (10,000 KG)"
     */
    public static String formatStock(double totalKg, double bagWeightKg) {
        double bags = kgToBags(totalKg, bagWeightKg);
        DecimalFormat df = new DecimalFormat("#,##0.#");
        return df.format(bags) + " Bags (" + df.format(totalKg) + " KG)";
    }

    public static String formatStockBagsAndKg(double totalKg, double bagWeightKg) {
        return formatStock(totalKg, bagWeightKg);
    }

    /**
     * Formats weight display e.g. "500 KG"
     */
    public static String formatKg(double totalKg) {
        DecimalFormat df = new DecimalFormat("#,##0.#");
        return df.format(totalKg) + " KG";
    }
}
