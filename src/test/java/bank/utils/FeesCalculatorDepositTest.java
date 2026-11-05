package bank.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Black-box tests for the deposit fee, computed by
 * {@link FeesCalculator#calculateDepositInterest(double, double, boolean)}.
 *
 * <p>Specification: Assignment 1, "Deposit" fee rules.
 * Technique: Weak Robust Equivalence Class Analysis.
 * Test-case IDs (DE-xx) match the tables in docs/assignment1.md.
 */
class FeesCalculatorDepositTest {
	private static final double DELTA = 1e-6;

	private final FeesCalculator calculator = new FeesCalculator();

	@ParameterizedTest(name = "{0}: amount={1}, balance={2}, student={3} -> fee {4}")
	@CsvSource({
		"DE-01, 25, 250, true, 0",
		"DE-02, 100, 1000, true, 0.5",
		"DE-03, 25, 7500, true, 0.125",
		"DE-04, -100, 250, true, 0",
		"DE-05, 25, -100, true, 0",
		"DE-06, 100, 1000, false, 0.1",
		"DE-07, 500, 5000, false, 4",
		"DE-08, 100, 15000, false, 0",
		"DE-09, -100, 1000, false, 0",
		"DE-10, 100, -100, false, 0.1"
	})
	void depositFee(String id, double amount, double balance, boolean student, double expectedFee) {
		double actualFee = calculator.calculateDepositInterest(amount, balance, student);
		assertEquals(expectedFee, actualFee, DELTA, id + " (spec: Deposit)");
	}
}
