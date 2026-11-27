package com.example

import com.example.model.ConnectionType
import com.example.model.SolarCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolarCalculatorTest {
    @Test
    fun recommendPanelCount_calculatesExpectedCountForGoias() {
        val count = SolarCalculator.recommendPanelCount(
            consumptionValue = 1349.0,
            isCurrencyReais = false,
            kwhTariff = 0.95,
            connectionType = ConnectionType.BIFASICO,
            stateUf = "GO",
            panelPowerW = 605
        )
        assertTrue(count in 15..22)
    }

    @Test
    fun calculateMetrics_computesPowerAndPaybackAccurately() {
        val metrics = SolarCalculator.calculateMetrics(
            panelCount = 20,
            panelPowerW = 605,
            stateUf = "GO",
            kwhTariff = 0.95,
            connectionType = ConnectionType.BIFASICO,
            totalPrice = 33899.0,
            cardInstallments = 12,
            cardFeePercent = 15.29
        )
        assertEquals(12.1, metrics.powerKwp, 0.01)
        assertTrue(metrics.estimatedMonthlyKwh > 1400.0)
        assertTrue(metrics.estimatedMonthlySavings > 1200.0)
        assertTrue(metrics.paybackMonths in 18..30)
        assertEquals(12, metrics.monthlyBreakdownKwh.size)
    }
}
