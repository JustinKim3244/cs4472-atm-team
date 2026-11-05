package bank.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Black-box tests for
 * {@link FeesCalculator#calculateTransferFee(double, double, double, boolean)}.
 *
 * <p>Specification: Assignment 1, "Transfer" fee rules.
 * Technique: Decision Table Analysis (one test per rule, 16 rules).
 * Test-case IDs (TR-xx) match the decision table in docs/assignment1.md.
 */
class FeesCalculatorTransferTest {
	private static final double DELTA = 1e-6;

	private final FeesCalculator calculator = new FeesCalculator();

	@ParameterizedTest(name = "{0}: amount={1}, from={2}, to={3}, student={4} -> fee {5}")
	@CsvSource({
		"TR-01, 150, 1500, 500, true, 0.15",
		"TR-02, 150, 1500, 1500, true, 0.075",
		"TR-03, 150, 3000, 500, true, 0.075",
		"TR-04, 150, 3000, 1500, true, 0.0375",
		"TR-05, 500, 1500, 500, true, 0.25",
		"TR-06, 500, 1500, 1500, true, 0.125",
		"TR-07, 500, 3000, 500, true, 1.25",
		"TR-08, 500, 3000, 1500, true, 0.625",
		"TR-09, 50, 3000, 1500, false, 0.1",
		"TR-10, 50, 3000, 2500, false, 0.05",
		"TR-11, 50, 5000, 1500, false, 0.5",
		"TR-12, 50, 5000, 2500, false, 0.25",
		"TR-13, 500, 1500, 500, false, 1",
		"TR-14, 500, 1500, 1500, false, 0.5",
		"TR-15, 500, 3000, 500, false, 2.5",
		"TR-16, 500, 3000, 1500, false, 1.25"
	})
	void transferFee(String id, double amount, double fromBalance, double toBalance, boolean student,
			double expectedFee) {
		double actualFee = calculator.calculateTransferFee(amount, fromBalance, toBalance, student);
		assertEquals(expectedFee, actualFee, DELTA, id + " (spec: Transfer)");
	}
}
