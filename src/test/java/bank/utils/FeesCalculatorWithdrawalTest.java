package bank.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Black-box tests for {@link FeesCalculator#calculateWithdrawalFee(double, double, boolean, int)}.
 *
 * <p>Specification: Assignment 1, "Withdrawal" fee rules.
 * Technique: Robust Worst Case Boundary Value Analysis.
 * Test-case IDs (WD-xxx) match the tables in docs/assignment1.md.
 *
 * <p>Day of week uses java.util.Calendar constants (SUNDAY = 1 ... SATURDAY = 7);
 * a weekday is MONDAY (2) to FRIDAY (6).
 */
class FeesCalculatorWithdrawalTest {
	private static final double DELTA = 1e-6;

	private final FeesCalculator calculator = new FeesCalculator();

	@ParameterizedTest(name = "{0}: amount={1}, balance={2}, student={3}, day={4} -> fee {5}")
	@CsvSource({
		"WD-001, 19, -0.01, false, 4, 0.057",
		"WD-002, 19, 0, false, 4, 0.057",
		"WD-003, 19, 0.01, false, 4, 0.057",
		"WD-004, 19, 999.99, false, 4, 0.057",
		"WD-005, 19, 1000, false, 4, 0.019",
		"WD-006, 19, 1000.01, false, 4, 0.019",
		"WD-007, 19, 2500, false, 4, 0.019",
		"WD-008, 19, 4999.99, false, 4, 0.019",
		"WD-009, 19, 5000, false, 4, 0",
		"WD-010, 19, 5000.01, false, 4, 0",
		"WD-011, 20, -0.01, false, 4, 0.06",
		"WD-012, 20, 0, false, 4, 0.06",
		"WD-013, 20, 0.01, false, 4, 0.06",
		"WD-014, 20, 999.99, false, 4, 0.06",
		"WD-015, 20, 1000, false, 4, 0.02",
		"WD-016, 20, 1000.01, false, 4, 0.02",
		"WD-017, 20, 2500, false, 4, 0.02",
		"WD-018, 20, 4999.99, false, 4, 0.02",
		"WD-019, 20, 5000, false, 4, 0",
		"WD-020, 20, 5000.01, false, 4, 0",
		"WD-021, 21, -0.01, false, 4, 0.063",
		"WD-022, 21, 0, false, 4, 0.063",
		"WD-023, 21, 0.01, false, 4, 0.063",
		"WD-024, 21, 999.99, false, 4, 0.063",
		"WD-025, 21, 1000, false, 4, 0.021",
		"WD-026, 21, 1000.01, false, 4, 0.021",
		"WD-027, 21, 2500, false, 4, 0.021",
		"WD-028, 21, 4999.99, false, 4, 0.021",
		"WD-029, 21, 5000, false, 4, 0",
		"WD-030, 21, 5000.01, false, 4, 0",
		"WD-031, 500, -0.01, false, 4, 1.5",
		"WD-032, 500, 0, false, 4, 1.5",
		"WD-033, 500, 0.01, false, 4, 1.5",
		"WD-034, 500, 999.99, false, 4, 1.5",
		"WD-035, 500, 1000, false, 4, 0.5",
		"WD-036, 500, 1000.01, false, 4, 0.5",
		"WD-037, 500, 2500, false, 4, 0.5",
		"WD-038, 500, 4999.99, false, 4, 0.5",
		"WD-039, 500, 5000, false, 4, 0",
		"WD-040, 500, 5000.01, false, 4, 0",
		"WD-041, 999, -0.01, false, 4, 2.997",
		"WD-042, 999, 0, false, 4, 2.997",
		"WD-043, 999, 0.01, false, 4, 2.997",
		"WD-044, 999, 999.99, false, 4, 2.997",
		"WD-045, 999, 1000, false, 4, 0.999",
		"WD-046, 999, 1000.01, false, 4, 0.999",
		"WD-047, 999, 2500, false, 4, 0.999",
		"WD-048, 999, 4999.99, false, 4, 0.999",
		"WD-049, 999, 5000, false, 4, 0",
		"WD-050, 999, 5000.01, false, 4, 0",
		"WD-051, 1000, -0.01, false, 4, 3",
		"WD-052, 1000, 0, false, 4, 3",
		"WD-053, 1000, 0.01, false, 4, 3",
		"WD-054, 1000, 999.99, false, 4, 3",
		"WD-055, 1000, 1000, false, 4, 1",
		"WD-056, 1000, 1000.01, false, 4, 1",
		"WD-057, 1000, 2500, false, 4, 1",
		"WD-058, 1000, 4999.99, false, 4, 1",
		"WD-059, 1000, 5000, false, 4, 0",
		"WD-060, 1000, 5000.01, false, 4, 0",
		"WD-061, 1001, -0.01, false, 4, 3.003",
		"WD-062, 1001, 0, false, 4, 3.003",
		"WD-063, 1001, 0.01, false, 4, 3.003",
		"WD-064, 1001, 999.99, false, 4, 3.003",
		"WD-065, 1001, 1000, false, 4, 1.001",
		"WD-066, 1001, 1000.01, false, 4, 1.001",
		"WD-067, 1001, 2500, false, 4, 1.001",
		"WD-068, 1001, 4999.99, false, 4, 1.001",
		"WD-069, 1001, 5000, false, 4, 0",
		"WD-070, 1001, 5000.01, false, 4, 0",
		"WD-071, 19, 2500, true, 0, 0.019",
		"WD-072, 19, 2500, true, 1, 0.019",
		"WD-073, 19, 2500, true, 2, 0",
		"WD-074, 19, 2500, true, 4, 0",
		"WD-075, 19, 2500, true, 6, 0",
		"WD-076, 19, 2500, true, 7, 0.019",
		"WD-077, 19, 2500, true, 8, 0.019",
		"WD-078, 20, 2500, true, 0, 0.02",
		"WD-079, 20, 2500, true, 1, 0.02",
		"WD-080, 20, 2500, true, 2, 0",
		"WD-081, 20, 2500, true, 4, 0",
		"WD-082, 20, 2500, true, 6, 0",
		"WD-083, 20, 2500, true, 7, 0.02",
		"WD-084, 20, 2500, true, 8, 0.02",
		"WD-085, 21, 2500, true, 0, 0.021",
		"WD-086, 21, 2500, true, 1, 0.021",
		"WD-087, 21, 2500, true, 2, 0",
		"WD-088, 21, 2500, true, 4, 0",
		"WD-089, 21, 2500, true, 6, 0",
		"WD-090, 21, 2500, true, 7, 0.021",
		"WD-091, 21, 2500, true, 8, 0.021",
		"WD-092, 500, 2500, true, 0, 0.5",
		"WD-093, 500, 2500, true, 1, 0.5",
		"WD-094, 500, 2500, true, 2, 0",
		"WD-095, 500, 2500, true, 4, 0",
		"WD-096, 500, 2500, true, 6, 0",
		"WD-097, 500, 2500, true, 7, 0.5",
		"WD-098, 500, 2500, true, 8, 0.5",
		"WD-099, 999, 2500, true, 0, 0.999",
		"WD-100, 999, 2500, true, 1, 0.999",
		"WD-101, 999, 2500, true, 2, 0",
		"WD-102, 999, 2500, true, 4, 0",
		"WD-103, 999, 2500, true, 6, 0",
		"WD-104, 999, 2500, true, 7, 0.999",
		"WD-105, 999, 2500, true, 8, 0.999",
		"WD-106, 1000, 2500, true, 0, 1",
		"WD-107, 1000, 2500, true, 1, 1",
		"WD-108, 1000, 2500, true, 2, 0",
		"WD-109, 1000, 2500, true, 4, 0",
		"WD-110, 1000, 2500, true, 6, 0",
		"WD-111, 1000, 2500, true, 7, 1",
		"WD-112, 1000, 2500, true, 8, 1",
		"WD-113, 1001, 2500, true, 0, 1.001",
		"WD-114, 1001, 2500, true, 1, 1.001",
		"WD-115, 1001, 2500, true, 2, 0",
		"WD-116, 1001, 2500, true, 4, 0",
		"WD-117, 1001, 2500, true, 6, 0",
		"WD-118, 1001, 2500, true, 7, 1.001",
		"WD-119, 1001, 2500, true, 8, 1.001"
	})
	void withdrawalFee(String id, double amount, double balance, boolean student, int dayOfWeek,
			double expectedFee) {
		double actualFee = calculator.calculateWithdrawalFee(amount, balance, student, dayOfWeek);
		assertEquals(expectedFee, actualFee, DELTA, id + " (spec: Withdrawal)");
	}
}
