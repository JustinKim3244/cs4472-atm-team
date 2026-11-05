package atm.session;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import atm.ATM;
import atm.dispatcher.MessageDispatcher;
import atm.exceptions.InvalidAmountException;
import atm.exceptions.InvalidPinFormatException;
import atm.session.transactions.ATMTransaction;
import atm.ui.panels.MainPanel;
import atm.utils.CredentialsCheck;
import atm.utils.FormatChecker;
import bank.transactions.utils.AccountType;
import bank.transactions.utils.TransactionResult;
import bank.transactions.utils.TransactionType;

/**
 * Black-box tests for the ATM session (atm.session.Session): PIN format and
 * withdrawal amount for a withdrawal transaction (transaction choice 1).
 *
 * <p>Specification: Assignment 1, "ATM Session".
 * Technique: Robust Worst Case Boundary Value Analysis under the single fault
 * assumption (see docs/assignment1.md). Test-case IDs (SE-xx) match the tables there.
 *
 * <p>The session is driven through the console ATM facade, which owns the Session
 * and validates the PIN before handing it to Session#addPin; the amount goes to
 * Session#setAmount. The bank is stubbed as an account with a $5,000 available
 * balance. The code has no daily-limit setting, so the $1,000 daily limit from the
 * specification is applied as the expected upper boundary of the amount.
 *
 * <p>Two sessions are used per case so that each check sees a single fault:
 * {@code pinAtm} uses the real FormatChecker and checks only the PIN step;
 * {@code amountAtm} uses a stubbed FormatChecker that accepts the PIN, so its session
 * really reaches the amount step (Withdrawal from Chequing) before the amount is entered.
 */
class SessionTest {
	private static final double AVAILABLE_BALANCE = 5000.00;
	private static final String CARD = "4000000000000000";
	private static final String ACCEPTED = "accepted";
	private static final String PIN_EXCEPTION = "InvalidPinFormatException";

	private ATM pinAtm;
	private ATM amountAtm;

	@BeforeEach
	void setUp() throws Exception {
		pinAtm = newAtm(new FormatChecker());
		amountAtm = newAtm(Mockito.mock(FormatChecker.class));
	}

	private static ATM newAtm(FormatChecker formatChecker) throws Exception {
		MessageDispatcher dispatcher = Mockito.mock(MessageDispatcher.class);
		Mockito.when(dispatcher.checkCredentials(ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenReturn(true);
		Mockito.when(dispatcher.performTransaction(ArgumentMatchers.any(), ArgumentMatchers.any(),
				ArgumentMatchers.any())).thenAnswer(invocation -> {
					ATMTransaction transaction = invocation.getArgument(2);
					double[] balances = {AVAILABLE_BALANCE - transaction.getAmount()};
					return new TransactionResult(true, "", 0.0, balances);
				});

		ATM atm = new ATM(formatChecker, new CredentialsCheck(dispatcher), dispatcher);
		atm.setMainPanel(Mockito.mock(MainPanel.class));
		atm.createSession();
		atm.checkCardNumber(CARD);
		return atm;
	}

	@ParameterizedTest(name = "{0}: PIN={1}, amount={2} -> {3}")
	@CsvSource({
		"SE-01, '0000', 0, InvalidPinFormatException",
		"SE-02, '0000', 20, InvalidPinFormatException",
		"SE-03, '0000', 40, InvalidPinFormatException",
		"SE-04, '0000', 500, InvalidPinFormatException",
		"SE-05, '0000', 990, InvalidPinFormatException",
		"SE-06, '0000', 1000, InvalidPinFormatException",
		"SE-07, '0000', 1010, InvalidPinFormatException",
		"SE-08, '00000', 0, InvalidAmountException",
		"SE-09, '00000', 20, accepted",
		"SE-10, '00000', 40, accepted",
		"SE-11, '00000', 500, accepted",
		"SE-12, '00000', 990, accepted",
		"SE-13, '00000', 1000, accepted",
		"SE-14, '00000', 1010, InvalidAmountException",
		"SE-15, '00001', 0, InvalidAmountException",
		"SE-16, '00001', 20, accepted",
		"SE-17, '00001', 40, accepted",
		"SE-18, '00001', 500, accepted",
		"SE-19, '00001', 990, accepted",
		"SE-20, '00001', 1000, accepted",
		"SE-21, '00001', 1010, InvalidAmountException",
		"SE-22, '55555', 0, InvalidAmountException",
		"SE-23, '55555', 20, accepted",
		"SE-24, '55555', 40, accepted",
		"SE-25, '55555', 500, accepted",
		"SE-26, '55555', 990, accepted",
		"SE-27, '55555', 1000, accepted",
		"SE-28, '55555', 1010, InvalidAmountException",
		"SE-29, '99998', 0, InvalidAmountException",
		"SE-30, '99998', 20, accepted",
		"SE-31, '99998', 40, accepted",
		"SE-32, '99998', 500, accepted",
		"SE-33, '99998', 990, accepted",
		"SE-34, '99998', 1000, accepted",
		"SE-35, '99998', 1010, InvalidAmountException",
		"SE-36, '99999', 0, InvalidAmountException",
		"SE-37, '99999', 20, accepted",
		"SE-38, '99999', 40, accepted",
		"SE-39, '99999', 500, accepted",
		"SE-40, '99999', 990, accepted",
		"SE-41, '99999', 1000, accepted",
		"SE-42, '99999', 1010, InvalidAmountException",
		"SE-43, '100000', 0, InvalidPinFormatException",
		"SE-44, '100000', 20, InvalidPinFormatException",
		"SE-45, '100000', 40, InvalidPinFormatException",
		"SE-46, '100000', 500, InvalidPinFormatException",
		"SE-47, '100000', 990, InvalidPinFormatException",
		"SE-48, '100000', 1000, InvalidPinFormatException",
		"SE-49, '100000', 1010, InvalidPinFormatException"
	})
	void withdrawalPinAndAmountBoundaries(String id, String pin, int amount, String expected) {
		runWithdrawal(id, pin, amount, expected);
	}

	/**
	 * Supplementary cases. SE-X1 to SE-X6 test the $20/$50 dispensability rule inside the
	 * range. SE-X7 ($1,020) is over the daily limit and dispensable even by the code's own
	 * bill check, so it tests the $1,000 upper limit on its own (the BVA max+ value $1,010
	 * is also rejected by the faulty bill check, which would hide a missing limit check).
	 */
	@ParameterizedTest(name = "{0}: PIN={1}, amount={2} -> {3}")
	@CsvSource({
		"SE-X1, '55555', 30, InvalidAmountException",
		"SE-X2, '55555', 60, accepted",
		"SE-X3, '55555', 70, accepted",
		"SE-X4, '55555', 80, accepted",
		"SE-X5, '55555', 110, accepted",
		"SE-X6, '55555', 130, accepted",
		"SE-X7, '55555', 1020, InvalidAmountException"
	})
	void withdrawalAmountDispensability(String id, String pin, int amount, String expected) {
		runWithdrawal(id, pin, amount, expected);
	}

	/**
	 * Checks the PIN step and, if the PIN is valid per the spec, the amount step.
	 * An invalid PIN ends the session before an amount is asked for, so only the PIN
	 * outcome is checked. For a valid PIN, the PIN step runs on {@code pinAtm} and the
	 * amount step on {@code amountAtm}, whose session has passed the PIN and selected a
	 * withdrawal from Chequing; assertAll reports both, so a PIN fault cannot hide an
	 * amount fault and the amount step always starts from the correct session state.
	 */
	private void runWithdrawal(String id, String pin, int amount, String expected) {
		Executable enterPin = () -> pinAtm.checkPin(pin.toCharArray());
		if (PIN_EXCEPTION.equals(expected)) {
			assertThrows(InvalidPinFormatException.class, enterPin,
					id + ": PIN '" + pin + "' should be rejected (spec: ATM Session)");
			return;
		}

		Executable enterAmount = () -> {
			amountAtm.checkPin(pin.toCharArray());
			amountAtm.setTransaction(TransactionType.Withdrawal);
			amountAtm.setFromAccount(AccountType.Chequing);
			amountAtm.setAmount(amount);
		};
		assertAll(id,
			() -> assertDoesNotThrow(enterPin,
					id + ": PIN '" + pin + "' should be accepted (spec: ATM Session)"),
			() -> {
				if (ACCEPTED.equals(expected)) {
					assertDoesNotThrow(enterAmount,
							id + ": amount " + amount + " should be accepted (spec: ATM Session)");
				} else {
					assertThrows(InvalidAmountException.class, enterAmount,
							id + ": amount " + amount + " should be rejected (spec: ATM Session)");
				}
			});
	}
}
