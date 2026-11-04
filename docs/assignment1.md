# Assignment 1 — Black Box Testing

CS 4472B Software Specification, Testing, and Quality Assurance.

This document is the deliverable for Assignment 1. It explains how the test cases were derived (Section 1), lists them
(Section 2), reports the results (Section 3) and localizes the faults that the failing tests expose (Section 4).
The JUnit 5 code is in `src/test/java`; every test-case ID below (e.g. `WD-017`) appears as the first CSV column of
the corresponding parameterized test, so a row here maps one-to-one to a test invocation in the CI report.

| Target | Test class | Technique |
|---|---|---|
| `atm.session.Session` (PIN format, withdrawal amount) | `atm.session.SessionTest` | Robust Worst Case BVA, single fault assumption |
| `FeesCalculator` — withdrawal | `bank.utils.FeesCalculatorWithdrawalTest` | Robust Worst Case BVA |
| `FeesCalculator` — deposit | `bank.utils.FeesCalculatorDepositTest` | Weak Robust Equivalence Class Analysis |
| `FeesCalculator` — transfer | `bank.utils.FeesCalculatorTransferTest` | Decision Table Analysis |

---

## 1. Process and assumptions

### 1.1 General assumptions

* **Expected results come only from the specification.** The tests are black box: inputs and expected outputs were
  derived from the business rules in the assignment, not from the code. The code was read only afterwards, for fault
  localization (Section 4).
* **Fees are compared with a tolerance of 1e-6**, since they are `double` products of an amount and a rate.
* **The deposit fee is computed by `FeesCalculator.calculateDepositInterest`.** It is the only deposit method on the
  class (the bank's receipt also labels it "Interest"), so it is tested against the deposit *fee* rules in the spec.
* **Day of week** is the `java.util.Calendar` constant passed to `calculateWithdrawalFee` (`SUNDAY = 1` …
  `SATURDAY = 7`). "Weekday" means Monday–Friday (2–6).
* **Out-of-domain inputs to `FeesCalculator`.** The spec defines no error behaviour for the fee calculator (it has no
  exception), so for robust cases we assume:
  * a positive amount outside the ATM's range, or a negative balance, is still charged by the literal rule (the rules
    are plain inequalities);
  * a day value outside 1–7 is *not a weekday*, so a student is charged 0.1%;
  * a non-positive deposit amount is not a real deposit and must not produce a fee: expected fee **0** (in particular,
    a fee must never be negative).

### 1.2 `atm.session.Session` — Robust Worst Case BVA (PIN format and withdrawal amount)

**Set-up.** The assignment asks for an account with a **daily limit of $1,000** and an **available balance of
$5,000**. The bank is replaced by a Mockito stub of `MessageDispatcher` that accepts the credentials and returns a
successful withdrawal with the new balance ($5,000 − amount), so only the session's own validation is under test.
**The code has no daily-limit setting:** no class in `src/main` stores or checks a daily limit, and the session never
asks the bank for one, so it could not be configured. The $1,000 limit is therefore applied from the specification
as the expected upper boundary of the amount; the missing check is itself a fault (SF4, Section 4). The session is
driven through the console `ATM` facade, which owns the `Session`: it validates the PIN (`ATM.checkPin`, which must
throw `InvalidPinFormatException`) before handing it to `Session.addPin`, and passes the amount to `Session.setAmount`
(which must throw `InvalidAmountException`). Each case selects transaction choice 1 (Withdrawal) from Chequing.

**Input variables and their robust boundary values (7 each).**

| Variable | Valid domain | min− | min | min+ | nom | max− | max | max+ |
|---|---|---|---|---|---|---|---|---|
| PIN | exactly 5 decimal digits, i.e. `00000`–`99999` | `0000` | `00000` | `00001` | `55555` | `99998` | `99999` | `100000` |
| Amount | dispensable in $20/$50 bills, $20 – $1,000 | $0 | $20 | $40 | $500 | $990 | $1,000 | $1,010 |

* **PIN.** A 5-digit PIN is treated as a number in 00000–99999 written with exactly five digits. The values just
  outside the domain are the nearest strings that break the format: `0000` (one digit short) and `100000` (one
  digit too many). The valid boundary values also cover the digit-character boundaries `0` and `9`.
* **Amount.** The smallest dispensable amount is $20 and the daily limit ($1,000) is the upper boundary, as the
  assignment states. With $20 and $50 bills, $20 and every multiple of $10 from $40 upwards can be dispensed
  ($30 cannot), so the dispensable values closest to each boundary are: min+ = $40, max− = $990
  (19 × $50 + 2 × $20) and max+ = $1,010 (19 × $50 + 3 × $20). min− = $0 is the value below the smallest
  amount. Both min− and max+ are themselves dispensable, so by the specification they test *only* the range
  boundaries and not the bill rule.

**Worst case.** Robust worst-case BVA takes the Cartesian product of the two 7-value sets: 7 × 7 = **49 cases**
(`SE-01`–`SE-49`).

**Worst case versus the single fault assumption.** The assignment asks for robust *worst case* BVA *under the single
fault assumption*, but these two pull in different directions: worst-case BVA is by definition the
*multiple*-fault variant (every combination of boundary values), while the single fault assumption on its own gives
robust BVA with one variable off nominal at a time (6n + 1 = 13 cases for n = 2). We followed the explicit request
for worst case and generated all 49 cases; they include the 13 single-fault cases (every case where the PIN or the
amount is at its nominal value). We applied the single fault assumption to how each case is *checked*, so that each
check sees one fault at a time:

* The ATM asks for the PIN before the amount, so a case with an invalid PIN expects `InvalidPinFormatException`, and
  the session never reaches the amount.
* For a valid-PIN case, the PIN step and the amount step are checked separately (`assertAll`) in two sessions. The
  PIN step uses the real `FormatChecker`. The amount step uses a session whose PIN check is stubbed to accept, so the
  session really is at the amount step (Withdrawal from Chequing) when the amount is entered, whatever the PIN code
  does. A fault in PIN handling therefore cannot hide, or change the state of, the amount check.
* A case with a valid PIN and an invalid amount expects `InvalidAmountException`, and a case with both valid is
  accepted.

Whether the course intends this reading or plain single-fault robust BVA is worth confirming with the TA; the 13
single-fault cases are a subset of the 49, so the results for either reading can be read off Section 3.1.

**Supplementary cases.** BVA only exercises the range boundaries; it says nothing about the second part of the
amount rule ("can be dispensed in $20 and $50 bills"). Six extra cases (`SE-X1`–`SE-X6`) use a nominal valid PIN and
amounts that need different bill combinations. A seventh, `SE-X7` ($1,020), tests the daily limit on its own: the
BVA max+ value $1,010 is wrongly rejected by the code's bill check (SF2), which would hide a missing limit check,
while $1,020 passes that bill check. The supplementary cases are reported separately and are not part of the BVA
count.

### 1.3 `FeesCalculator.calculateWithdrawalFee` — Robust Worst Case BVA

The rule has two numeric variables that matter for each kind of client:

* **Non-student:** amount and account **balance** (sub-domain boundaries at $1,000 and $5,000).
* **Student:** amount and **day of week** (weekday vs. weekend); the balance is irrelevant and is held at a nominal
  $2,500.

| Variable | Values used (min−, min, min+, nom, max−, max, max+) |
|---|---|
| Amount | 19, 20, 21, 500, 999, 1000, 1001 (ATM withdrawal range $20–$1,000) |
| Day of week | 0, 1 (Sun), 2 (Mon), 4 (Wed), 6 (Fri), 7 (Sat), 8 (valid range 1–7) |
| Balance | −0.01, 0, 0.01 (domain minimum) · 999.99, 1000, 1000.01 (boundary $1,000) · 2500 (nominal) · 4999.99, 5000, 5000.01 (boundary $5,000) |

The balance has two internal boundaries where the fee rate changes, so the min/max idea is applied to each: values
are taken just below, at and just above $1,000 and $5,000, as well as around the domain minimum of $0, with $2,500
as the nominal. That gives 10 balance values.

* Non-student worst case: 7 amounts × 10 balances = **70 cases** (`WD-001`–`WD-070`), day fixed at Wednesday (it does
  not affect non-students).
* Student worst case: 7 amounts × 7 days = **49 cases** (`WD-071`–`WD-119`).

Total: **119 cases**. Expected fee = amount × rate from the rule (Section 1.1 covers the out-of-domain values).

### 1.4 `FeesCalculator` deposit fee — Weak Robust Equivalence Class Analysis

Equivalence classes come from the conditions in the deposit rule, separately for students and non-students. Invalid
classes are a non-positive amount and a negative balance.

| Client | Amount classes | Balance classes |
|---|---|---|
| Student | SA0: a ≤ 0 (invalid) · SA1: 0 < a ≤ 50 · SA2: a > 50 | SB0: b < 0 (invalid) · SB1: 0 ≤ b ≤ 500 · SB2: 500 < b ≤ 5,000 · SB3: b > 5,000 |
| Non-student | NA0: a ≤ 0 (invalid) · NA1: 0 < a ≤ 250 · NA2: a > 250 | NB0: b < 0 (invalid) · NB1: 0 ≤ b ≤ 2,500 · NB2: 2,500 < b ≤ 10,000 · NB3: b > 10,000 |

The balance boundaries 500/5,000 (student) and 2,500/10,000 (non-student) are both used by the rule, so together
they split the balance domain into three valid classes.

**Weak robust:**

* **Valid cases:** each valid class appears in at least one case. The number of cases equals the largest number of
  valid classes for one variable (3 balance classes), giving 3 cases per client type.
* **Invalid cases:** one invalid class at a time, with every other variable at a valid value. That gives 2 cases per
  client type.

This gives **10 cases** (`DE-01`–`DE-10`). Representatives sit in the middle of their classes (e.g. 25 for 0 < a ≤ 50).
The weak method does not cover every *combination* of classes, so some rule outcomes are not exercised: a student
paying 0.25% (a > 50, b ≤ 500) and a non-student paying 0.4% (a > 250, b ≤ 2,500). This is a known limitation of the
weak variant.

### 1.5 `FeesCalculator.calculateTransferFee` — Decision Table Analysis

The rule has four binary conditions, so the full decision table has 2⁴ = **16 rules**. The thresholds for C2–C4
depend on the client type and on C2, exactly as the specification states:

* C1: client is a student.
* C2: amount is "small": student < $200; non-student < $100.
* C3: the source balance is "low": student < $2,000; non-student small amount < $4,000; non-student large amount
  < $2,000.
* C4: the destination balance is "low": student < $1,000; non-student small amount < $2,000; non-student large
  amount < $1,000.

Every rule has its own action (fee rate), so no rules could be merged and there is no "don't care" entry. Each
rule gets one test case (`TR-01`–`TR-16`), using representative values that sit clearly on one side of each
threshold:

* Student amounts: $150 and $500. Source balances $1,500 / $3,000; destination balances $500 / $1,500.
* Non-student amounts: $50 (with sources $3,000 / $5,000 and destinations $1,500 / $2,500) and $500 (with sources
  $1,500 / $3,000 and destinations $500 / $1,500).

Read literally, the spec's "Otherwise" in the first and third sub-rules of each group means "the first condition
holds, the second does not". This is the only reading that keeps the four sub-rules disjoint and complete. For
example, student, amount < $200, source < $2,000, destination ≥ $1,000 → 0.05%.

---

## 2. Test cases

### 2.1 `SessionTest` — PIN format and withdrawal amount (Robust Worst Case BVA, 49 cases)

| ID | PIN | PIN value class | Amount | Amount value class | Expected outcome |
|---|---|---|---|---|---|
| SE-01 | `0000` | min- (4 digits) | $0 | min- | InvalidPinFormatException |
| SE-02 | `0000` | min- (4 digits) | $20 | min | InvalidPinFormatException |
| SE-03 | `0000` | min- (4 digits) | $40 | min+ | InvalidPinFormatException |
| SE-04 | `0000` | min- (4 digits) | $500 | nom | InvalidPinFormatException |
| SE-05 | `0000` | min- (4 digits) | $990 | max- | InvalidPinFormatException |
| SE-06 | `0000` | min- (4 digits) | $1000 | max | InvalidPinFormatException |
| SE-07 | `0000` | min- (4 digits) | $1010 | max+ | InvalidPinFormatException |
| SE-08 | `00000` | min | $0 | min- | InvalidAmountException |
| SE-09 | `00000` | min | $20 | min | accepted |
| SE-10 | `00000` | min | $40 | min+ | accepted |
| SE-11 | `00000` | min | $500 | nom | accepted |
| SE-12 | `00000` | min | $990 | max- | accepted |
| SE-13 | `00000` | min | $1000 | max | accepted |
| SE-14 | `00000` | min | $1010 | max+ | InvalidAmountException |
| SE-15 | `00001` | min+ | $0 | min- | InvalidAmountException |
| SE-16 | `00001` | min+ | $20 | min | accepted |
| SE-17 | `00001` | min+ | $40 | min+ | accepted |
| SE-18 | `00001` | min+ | $500 | nom | accepted |
| SE-19 | `00001` | min+ | $990 | max- | accepted |
| SE-20 | `00001` | min+ | $1000 | max | accepted |
| SE-21 | `00001` | min+ | $1010 | max+ | InvalidAmountException |
| SE-22 | `55555` | nom | $0 | min- | InvalidAmountException |
| SE-23 | `55555` | nom | $20 | min | accepted |
| SE-24 | `55555` | nom | $40 | min+ | accepted |
| SE-25 | `55555` | nom | $500 | nom | accepted |
| SE-26 | `55555` | nom | $990 | max- | accepted |
| SE-27 | `55555` | nom | $1000 | max | accepted |
| SE-28 | `55555` | nom | $1010 | max+ | InvalidAmountException |
| SE-29 | `99998` | max- | $0 | min- | InvalidAmountException |
| SE-30 | `99998` | max- | $20 | min | accepted |
| SE-31 | `99998` | max- | $40 | min+ | accepted |
| SE-32 | `99998` | max- | $500 | nom | accepted |
| SE-33 | `99998` | max- | $990 | max- | accepted |
| SE-34 | `99998` | max- | $1000 | max | accepted |
| SE-35 | `99998` | max- | $1010 | max+ | InvalidAmountException |
| SE-36 | `99999` | max | $0 | min- | InvalidAmountException |
| SE-37 | `99999` | max | $20 | min | accepted |
| SE-38 | `99999` | max | $40 | min+ | accepted |
| SE-39 | `99999` | max | $500 | nom | accepted |
| SE-40 | `99999` | max | $990 | max- | accepted |
| SE-41 | `99999` | max | $1000 | max | accepted |
| SE-42 | `99999` | max | $1010 | max+ | InvalidAmountException |
| SE-43 | `100000` | max+ (6 digits) | $0 | min- | InvalidPinFormatException |
| SE-44 | `100000` | max+ (6 digits) | $20 | min | InvalidPinFormatException |
| SE-45 | `100000` | max+ (6 digits) | $40 | min+ | InvalidPinFormatException |
| SE-46 | `100000` | max+ (6 digits) | $500 | nom | InvalidPinFormatException |
| SE-47 | `100000` | max+ (6 digits) | $990 | max- | InvalidPinFormatException |
| SE-48 | `100000` | max+ (6 digits) | $1000 | max | InvalidPinFormatException |
| SE-49 | `100000` | max+ (6 digits) | $1010 | max+ | InvalidPinFormatException |

**Supplementary cases** (nominal valid PIN; not part of the BVA count):

| ID | PIN | Amount | Dispensable as | Expected outcome |
|---|---|---|---|---|
| SE-X1 | `55555` | $30 | not dispensable | InvalidAmountException |
| SE-X2 | `55555` | $60 | 3 × $20 | accepted |
| SE-X3 | `55555` | $70 | $50 + $20 | accepted |
| SE-X4 | `55555` | $80 | 4 × $20 | accepted |
| SE-X5 | `55555` | $110 | $50 + 3 × $20 | accepted |
| SE-X6 | `55555` | $130 | $50 + 4 × $20 | accepted |
| SE-X7 | `55555` | $1,020 | 20 × $50 + $20, but over the $1,000 daily limit | InvalidAmountException |

### 2.2 `FeesCalculatorWithdrawalTest` (Robust Worst Case BVA, 119 cases)

**Non-student: amount × balance (70 cases)**

| ID | Amount (class) | Balance (class) | Student | Day | Expected rate | Expected fee |
|---|---|---|---|---|---|---|
| WD-001 | 19 (min-) | -0.01 (min-) | No | 4 (Wed) | 0.3% | 0.057 |
| WD-002 | 19 (min-) | 0 (min) | No | 4 (Wed) | 0.3% | 0.057 |
| WD-003 | 19 (min-) | 0.01 (min+) | No | 4 (Wed) | 0.3% | 0.057 |
| WD-004 | 19 (min-) | 999.99 (b1-) | No | 4 (Wed) | 0.3% | 0.057 |
| WD-005 | 19 (min-) | 1000 (b1) | No | 4 (Wed) | 0.1% | 0.019 |
| WD-006 | 19 (min-) | 1000.01 (b1+) | No | 4 (Wed) | 0.1% | 0.019 |
| WD-007 | 19 (min-) | 2500 (nom) | No | 4 (Wed) | 0.1% | 0.019 |
| WD-008 | 19 (min-) | 4999.99 (b2-) | No | 4 (Wed) | 0.1% | 0.019 |
| WD-009 | 19 (min-) | 5000 (b2) | No | 4 (Wed) | 0% | 0 |
| WD-010 | 19 (min-) | 5000.01 (b2+) | No | 4 (Wed) | 0% | 0 |
| WD-011 | 20 (min) | -0.01 (min-) | No | 4 (Wed) | 0.3% | 0.06 |
| WD-012 | 20 (min) | 0 (min) | No | 4 (Wed) | 0.3% | 0.06 |
| WD-013 | 20 (min) | 0.01 (min+) | No | 4 (Wed) | 0.3% | 0.06 |
| WD-014 | 20 (min) | 999.99 (b1-) | No | 4 (Wed) | 0.3% | 0.06 |
| WD-015 | 20 (min) | 1000 (b1) | No | 4 (Wed) | 0.1% | 0.02 |
| WD-016 | 20 (min) | 1000.01 (b1+) | No | 4 (Wed) | 0.1% | 0.02 |
| WD-017 | 20 (min) | 2500 (nom) | No | 4 (Wed) | 0.1% | 0.02 |
| WD-018 | 20 (min) | 4999.99 (b2-) | No | 4 (Wed) | 0.1% | 0.02 |
| WD-019 | 20 (min) | 5000 (b2) | No | 4 (Wed) | 0% | 0 |
| WD-020 | 20 (min) | 5000.01 (b2+) | No | 4 (Wed) | 0% | 0 |
| WD-021 | 21 (min+) | -0.01 (min-) | No | 4 (Wed) | 0.3% | 0.063 |
| WD-022 | 21 (min+) | 0 (min) | No | 4 (Wed) | 0.3% | 0.063 |
| WD-023 | 21 (min+) | 0.01 (min+) | No | 4 (Wed) | 0.3% | 0.063 |
| WD-024 | 21 (min+) | 999.99 (b1-) | No | 4 (Wed) | 0.3% | 0.063 |
| WD-025 | 21 (min+) | 1000 (b1) | No | 4 (Wed) | 0.1% | 0.021 |
| WD-026 | 21 (min+) | 1000.01 (b1+) | No | 4 (Wed) | 0.1% | 0.021 |
| WD-027 | 21 (min+) | 2500 (nom) | No | 4 (Wed) | 0.1% | 0.021 |
| WD-028 | 21 (min+) | 4999.99 (b2-) | No | 4 (Wed) | 0.1% | 0.021 |
| WD-029 | 21 (min+) | 5000 (b2) | No | 4 (Wed) | 0% | 0 |
| WD-030 | 21 (min+) | 5000.01 (b2+) | No | 4 (Wed) | 0% | 0 |
| WD-031 | 500 (nom) | -0.01 (min-) | No | 4 (Wed) | 0.3% | 1.5 |
| WD-032 | 500 (nom) | 0 (min) | No | 4 (Wed) | 0.3% | 1.5 |
| WD-033 | 500 (nom) | 0.01 (min+) | No | 4 (Wed) | 0.3% | 1.5 |
| WD-034 | 500 (nom) | 999.99 (b1-) | No | 4 (Wed) | 0.3% | 1.5 |
| WD-035 | 500 (nom) | 1000 (b1) | No | 4 (Wed) | 0.1% | 0.5 |
| WD-036 | 500 (nom) | 1000.01 (b1+) | No | 4 (Wed) | 0.1% | 0.5 |
| WD-037 | 500 (nom) | 2500 (nom) | No | 4 (Wed) | 0.1% | 0.5 |
| WD-038 | 500 (nom) | 4999.99 (b2-) | No | 4 (Wed) | 0.1% | 0.5 |
| WD-039 | 500 (nom) | 5000 (b2) | No | 4 (Wed) | 0% | 0 |
| WD-040 | 500 (nom) | 5000.01 (b2+) | No | 4 (Wed) | 0% | 0 |
| WD-041 | 999 (max-) | -0.01 (min-) | No | 4 (Wed) | 0.3% | 2.997 |
| WD-042 | 999 (max-) | 0 (min) | No | 4 (Wed) | 0.3% | 2.997 |
| WD-043 | 999 (max-) | 0.01 (min+) | No | 4 (Wed) | 0.3% | 2.997 |
| WD-044 | 999 (max-) | 999.99 (b1-) | No | 4 (Wed) | 0.3% | 2.997 |
| WD-045 | 999 (max-) | 1000 (b1) | No | 4 (Wed) | 0.1% | 0.999 |
| WD-046 | 999 (max-) | 1000.01 (b1+) | No | 4 (Wed) | 0.1% | 0.999 |
| WD-047 | 999 (max-) | 2500 (nom) | No | 4 (Wed) | 0.1% | 0.999 |
| WD-048 | 999 (max-) | 4999.99 (b2-) | No | 4 (Wed) | 0.1% | 0.999 |
| WD-049 | 999 (max-) | 5000 (b2) | No | 4 (Wed) | 0% | 0 |
| WD-050 | 999 (max-) | 5000.01 (b2+) | No | 4 (Wed) | 0% | 0 |
| WD-051 | 1000 (max) | -0.01 (min-) | No | 4 (Wed) | 0.3% | 3 |
| WD-052 | 1000 (max) | 0 (min) | No | 4 (Wed) | 0.3% | 3 |
| WD-053 | 1000 (max) | 0.01 (min+) | No | 4 (Wed) | 0.3% | 3 |
| WD-054 | 1000 (max) | 999.99 (b1-) | No | 4 (Wed) | 0.3% | 3 |
| WD-055 | 1000 (max) | 1000 (b1) | No | 4 (Wed) | 0.1% | 1 |
| WD-056 | 1000 (max) | 1000.01 (b1+) | No | 4 (Wed) | 0.1% | 1 |
| WD-057 | 1000 (max) | 2500 (nom) | No | 4 (Wed) | 0.1% | 1 |
| WD-058 | 1000 (max) | 4999.99 (b2-) | No | 4 (Wed) | 0.1% | 1 |
| WD-059 | 1000 (max) | 5000 (b2) | No | 4 (Wed) | 0% | 0 |
| WD-060 | 1000 (max) | 5000.01 (b2+) | No | 4 (Wed) | 0% | 0 |
| WD-061 | 1001 (max+) | -0.01 (min-) | No | 4 (Wed) | 0.3% | 3.003 |
| WD-062 | 1001 (max+) | 0 (min) | No | 4 (Wed) | 0.3% | 3.003 |
| WD-063 | 1001 (max+) | 0.01 (min+) | No | 4 (Wed) | 0.3% | 3.003 |
| WD-064 | 1001 (max+) | 999.99 (b1-) | No | 4 (Wed) | 0.3% | 3.003 |
| WD-065 | 1001 (max+) | 1000 (b1) | No | 4 (Wed) | 0.1% | 1.001 |
| WD-066 | 1001 (max+) | 1000.01 (b1+) | No | 4 (Wed) | 0.1% | 1.001 |
| WD-067 | 1001 (max+) | 2500 (nom) | No | 4 (Wed) | 0.1% | 1.001 |
| WD-068 | 1001 (max+) | 4999.99 (b2-) | No | 4 (Wed) | 0.1% | 1.001 |
| WD-069 | 1001 (max+) | 5000 (b2) | No | 4 (Wed) | 0% | 0 |
| WD-070 | 1001 (max+) | 5000.01 (b2+) | No | 4 (Wed) | 0% | 0 |

**Student: amount × day of week (49 cases)**

| ID | Amount (class) | Day of week (class) | Student | Balance | Expected rate | Expected fee |
|---|---|---|---|---|---|---|
| WD-071 | 19 (min-) | 0 (invalid) (min-) | Yes | 2500 | 0.1% | 0.019 |
| WD-072 | 19 (min-) | 1 (Sun) (min) | Yes | 2500 | 0.1% | 0.019 |
| WD-073 | 19 (min-) | 2 (Mon) (min+) | Yes | 2500 | 0% | 0 |
| WD-074 | 19 (min-) | 4 (Wed) (nom) | Yes | 2500 | 0% | 0 |
| WD-075 | 19 (min-) | 6 (Fri) (max-) | Yes | 2500 | 0% | 0 |
| WD-076 | 19 (min-) | 7 (Sat) (max) | Yes | 2500 | 0.1% | 0.019 |
| WD-077 | 19 (min-) | 8 (invalid) (max+) | Yes | 2500 | 0.1% | 0.019 |
| WD-078 | 20 (min) | 0 (invalid) (min-) | Yes | 2500 | 0.1% | 0.02 |
| WD-079 | 20 (min) | 1 (Sun) (min) | Yes | 2500 | 0.1% | 0.02 |
| WD-080 | 20 (min) | 2 (Mon) (min+) | Yes | 2500 | 0% | 0 |
| WD-081 | 20 (min) | 4 (Wed) (nom) | Yes | 2500 | 0% | 0 |
| WD-082 | 20 (min) | 6 (Fri) (max-) | Yes | 2500 | 0% | 0 |
| WD-083 | 20 (min) | 7 (Sat) (max) | Yes | 2500 | 0.1% | 0.02 |
| WD-084 | 20 (min) | 8 (invalid) (max+) | Yes | 2500 | 0.1% | 0.02 |
| WD-085 | 21 (min+) | 0 (invalid) (min-) | Yes | 2500 | 0.1% | 0.021 |
| WD-086 | 21 (min+) | 1 (Sun) (min) | Yes | 2500 | 0.1% | 0.021 |
| WD-087 | 21 (min+) | 2 (Mon) (min+) | Yes | 2500 | 0% | 0 |
| WD-088 | 21 (min+) | 4 (Wed) (nom) | Yes | 2500 | 0% | 0 |
| WD-089 | 21 (min+) | 6 (Fri) (max-) | Yes | 2500 | 0% | 0 |
| WD-090 | 21 (min+) | 7 (Sat) (max) | Yes | 2500 | 0.1% | 0.021 |
| WD-091 | 21 (min+) | 8 (invalid) (max+) | Yes | 2500 | 0.1% | 0.021 |
| WD-092 | 500 (nom) | 0 (invalid) (min-) | Yes | 2500 | 0.1% | 0.5 |
| WD-093 | 500 (nom) | 1 (Sun) (min) | Yes | 2500 | 0.1% | 0.5 |
| WD-094 | 500 (nom) | 2 (Mon) (min+) | Yes | 2500 | 0% | 0 |
| WD-095 | 500 (nom) | 4 (Wed) (nom) | Yes | 2500 | 0% | 0 |
| WD-096 | 500 (nom) | 6 (Fri) (max-) | Yes | 2500 | 0% | 0 |
| WD-097 | 500 (nom) | 7 (Sat) (max) | Yes | 2500 | 0.1% | 0.5 |
| WD-098 | 500 (nom) | 8 (invalid) (max+) | Yes | 2500 | 0.1% | 0.5 |
| WD-099 | 999 (max-) | 0 (invalid) (min-) | Yes | 2500 | 0.1% | 0.999 |
| WD-100 | 999 (max-) | 1 (Sun) (min) | Yes | 2500 | 0.1% | 0.999 |
| WD-101 | 999 (max-) | 2 (Mon) (min+) | Yes | 2500 | 0% | 0 |
| WD-102 | 999 (max-) | 4 (Wed) (nom) | Yes | 2500 | 0% | 0 |
| WD-103 | 999 (max-) | 6 (Fri) (max-) | Yes | 2500 | 0% | 0 |
| WD-104 | 999 (max-) | 7 (Sat) (max) | Yes | 2500 | 0.1% | 0.999 |
| WD-105 | 999 (max-) | 8 (invalid) (max+) | Yes | 2500 | 0.1% | 0.999 |
| WD-106 | 1000 (max) | 0 (invalid) (min-) | Yes | 2500 | 0.1% | 1 |
| WD-107 | 1000 (max) | 1 (Sun) (min) | Yes | 2500 | 0.1% | 1 |
| WD-108 | 1000 (max) | 2 (Mon) (min+) | Yes | 2500 | 0% | 0 |
| WD-109 | 1000 (max) | 4 (Wed) (nom) | Yes | 2500 | 0% | 0 |
| WD-110 | 1000 (max) | 6 (Fri) (max-) | Yes | 2500 | 0% | 0 |
| WD-111 | 1000 (max) | 7 (Sat) (max) | Yes | 2500 | 0.1% | 1 |
| WD-112 | 1000 (max) | 8 (invalid) (max+) | Yes | 2500 | 0.1% | 1 |
| WD-113 | 1001 (max+) | 0 (invalid) (min-) | Yes | 2500 | 0.1% | 1.001 |
| WD-114 | 1001 (max+) | 1 (Sun) (min) | Yes | 2500 | 0.1% | 1.001 |
| WD-115 | 1001 (max+) | 2 (Mon) (min+) | Yes | 2500 | 0% | 0 |
| WD-116 | 1001 (max+) | 4 (Wed) (nom) | Yes | 2500 | 0% | 0 |
| WD-117 | 1001 (max+) | 6 (Fri) (max-) | Yes | 2500 | 0% | 0 |
| WD-118 | 1001 (max+) | 7 (Sat) (max) | Yes | 2500 | 0.1% | 1.001 |
| WD-119 | 1001 (max+) | 8 (invalid) (max+) | Yes | 2500 | 0.1% | 1.001 |

### 2.3 `FeesCalculatorDepositTest` (Weak Robust ECA, 10 cases)

| ID | Student | Amount | Amount class | Balance | Balance class | Type | Expected rate | Expected fee |
|---|---|---|---|---|---|---|---|---|
| DE-01 | Yes | 25 | SA1: 0 < a <= 50 | 250 | SB1: 0 <= b <= 500 | valid | 0% | 0 |
| DE-02 | Yes | 100 | SA2: a > 50 | 1000 | SB2: 500 < b <= 5000 | valid | 0.5% | 0.5 |
| DE-03 | Yes | 25 | SA1: 0 < a <= 50 | 7500 | SB3: b > 5000 | valid | 0.5% | 0.125 |
| DE-04 | Yes | -100 | SA0: a <= 0 (invalid) | 250 | SB1: 0 <= b <= 500 | invalid | 0% | 0 |
| DE-05 | Yes | 25 | SA1: 0 < a <= 50 | -100 | SB0: b < 0 (invalid) | invalid | 0% | 0 |
| DE-06 | No | 100 | NA1: 0 < a <= 250 | 1000 | NB1: 0 <= b <= 2500 | valid | 0.1% | 0.1 |
| DE-07 | No | 500 | NA2: a > 250 | 5000 | NB2: 2500 < b <= 10000 | valid | 0.8% | 4 |
| DE-08 | No | 100 | NA1: 0 < a <= 250 | 15000 | NB3: b > 10000 | valid | 0% | 0 |
| DE-09 | No | -100 | NA0: a <= 0 (invalid) | 1000 | NB1: 0 <= b <= 2500 | invalid | 0% | 0 |
| DE-10 | No | 100 | NA1: 0 < a <= 250 | -100 | NB0: b < 0 (invalid) | invalid | 0.1% | 0.1 |

### 2.4 `FeesCalculatorTransferTest` (Decision Table, 16 cases)

**Decision table** (S = student, N = non-student; T = condition true, F = false):

| | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 | 15 | 16 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| C1: student | T | T | T | T | T | T | T | T | F | F | F | F | F | F | F | F |
| C2: amount small (S: < 200, N: < 100) | T | T | T | T | F | F | F | F | T | T | T | T | F | F | F | F |
| C3: from low (S: < 2000; N small: < 4000; N large: < 2000) | T | T | F | F | T | T | F | F | T | T | F | F | T | T | F | F |
| C4: to low (S: < 1000; N small: < 2000; N large: < 1000) | T | F | T | F | T | F | T | F | T | F | T | F | T | F | T | F |
| **Action: fee rate** | 0.1% | 0.05% | 0.05% | 0.025% | 0.05% | 0.025% | 0.25% | 0.125% | 0.2% | 0.1% | 1% | 0.5% | 0.2% | 0.1% | 0.5% | 0.25% |

**Test cases (one per rule):**

| ID | Rule | Student | Amount | From balance | To balance | Conditions | Expected rate | Expected fee |
|---|---|---|---|---|---|---|---|---|
| TR-01 | 1 | Yes | 150 | 1500 | 500 | a < 200, from < 2000, to < 1000 | 0.1% | 0.15 |
| TR-02 | 2 | Yes | 150 | 1500 | 1500 | a < 200, from < 2000, to >= 1000 | 0.05% | 0.075 |
| TR-03 | 3 | Yes | 150 | 3000 | 500 | a < 200, from >= 2000, to < 1000 | 0.05% | 0.075 |
| TR-04 | 4 | Yes | 150 | 3000 | 1500 | a < 200, from >= 2000, to >= 1000 | 0.025% | 0.0375 |
| TR-05 | 5 | Yes | 500 | 1500 | 500 | a >= 200, from < 2000, to < 1000 | 0.05% | 0.25 |
| TR-06 | 6 | Yes | 500 | 1500 | 1500 | a >= 200, from < 2000, to >= 1000 | 0.025% | 0.125 |
| TR-07 | 7 | Yes | 500 | 3000 | 500 | a >= 200, from >= 2000, to < 1000 | 0.25% | 1.25 |
| TR-08 | 8 | Yes | 500 | 3000 | 1500 | a >= 200, from >= 2000, to >= 1000 | 0.125% | 0.625 |
| TR-09 | 9 | No | 50 | 3000 | 1500 | a < 100, from < 4000, to < 2000 | 0.2% | 0.1 |
| TR-10 | 10 | No | 50 | 3000 | 2500 | a < 100, from < 4000, to >= 2000 | 0.1% | 0.05 |
| TR-11 | 11 | No | 50 | 5000 | 1500 | a < 100, from >= 4000, to < 2000 | 1% | 0.5 |
| TR-12 | 12 | No | 50 | 5000 | 2500 | a < 100, from >= 4000, to >= 2000 | 0.5% | 0.25 |
| TR-13 | 13 | No | 500 | 1500 | 500 | a >= 100, from < 2000, to < 1000 | 0.2% | 1 |
| TR-14 | 14 | No | 500 | 1500 | 1500 | a >= 100, from < 2000, to >= 1000 | 0.1% | 0.5 |
| TR-15 | 15 | No | 500 | 3000 | 500 | a >= 100, from >= 2000, to < 1000 | 0.5% | 2.5 |
| TR-16 | 16 | No | 500 | 3000 | 1500 | a >= 100, from >= 2000, to >= 1000 | 0.25% | 1.25 |

---

## 3. Test results

The results below are from running the suite with Java 11 (`mvn -B test`) and match the JUnit report that CI posts on
the pull request. A test **fails** when the code's output differs from the specification. As the assignment says,
failing tests are the point: the code was not changed.

| Test class | Technique | Cases | Passed | Failed |
|---|---|---|---|---|
| `atm.session.SessionTest` | Robust Worst Case BVA (+7 supplementary) | 56 | 7 | 49 |
| `bank.utils.FeesCalculatorWithdrawalTest` | Robust Worst Case BVA | 119 | 42 | 77 |
| `bank.utils.FeesCalculatorDepositTest` | Weak Robust ECA | 10 | 2 | 8 |
| `bank.utils.FeesCalculatorTransferTest` | Decision Table | 16 | 0 | 16 |
| **Total** | | **201** | **51** | **150** |

The three starter tests that came with the repository (`ATMTest`, `FeesCalculatorTest`) are unchanged and not
counted above.

### 3.1 `SessionTest`

"PIN step" is what `ATM.checkPin` did with the real `FormatChecker`. "Amount step" is what `Session.setAmount` did for
a withdrawal, in a session that had already passed the PIN step and selected Withdrawal from Chequing (Section 1.2).
A valid-PIN case passes only if both steps match the expected outcome.

| ID | Expected outcome | PIN step (actual) | Amount step (actual) | Result |
|---|---|---|---|---|
| SE-01 | InvalidPinFormatException | accepted | — (not reached) | **FAIL** |
| SE-02 | InvalidPinFormatException | accepted | — (not reached) | **FAIL** |
| SE-03 | InvalidPinFormatException | accepted | — (not reached) | **FAIL** |
| SE-04 | InvalidPinFormatException | accepted | — (not reached) | **FAIL** |
| SE-05 | InvalidPinFormatException | accepted | — (not reached) | **FAIL** |
| SE-06 | InvalidPinFormatException | accepted | — (not reached) | **FAIL** |
| SE-07 | InvalidPinFormatException | accepted | — (not reached) | **FAIL** |
| SE-08 | InvalidAmountException | InvalidPinFormatException | accepted | **FAIL** |
| SE-09 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-10 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-11 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-12 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-13 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-14 | InvalidAmountException | InvalidPinFormatException | InvalidAmountException | **FAIL** |
| SE-15 | InvalidAmountException | InvalidPinFormatException | accepted | **FAIL** |
| SE-16 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-17 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-18 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-19 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-20 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-21 | InvalidAmountException | InvalidPinFormatException | InvalidAmountException | **FAIL** |
| SE-22 | InvalidAmountException | InvalidPinFormatException | accepted | **FAIL** |
| SE-23 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-24 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-25 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-26 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-27 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-28 | InvalidAmountException | InvalidPinFormatException | InvalidAmountException | **FAIL** |
| SE-29 | InvalidAmountException | InvalidPinFormatException | accepted | **FAIL** |
| SE-30 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-31 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-32 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-33 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-34 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-35 | InvalidAmountException | InvalidPinFormatException | InvalidAmountException | **FAIL** |
| SE-36 | InvalidAmountException | InvalidPinFormatException | accepted | **FAIL** |
| SE-37 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-38 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-39 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-40 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-41 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-42 | InvalidAmountException | InvalidPinFormatException | InvalidAmountException | **FAIL** |
| SE-43 | InvalidPinFormatException | InvalidPinFormatException | — (not reached) | PASS |
| SE-44 | InvalidPinFormatException | InvalidPinFormatException | — (not reached) | PASS |
| SE-45 | InvalidPinFormatException | InvalidPinFormatException | — (not reached) | PASS |
| SE-46 | InvalidPinFormatException | InvalidPinFormatException | — (not reached) | PASS |
| SE-47 | InvalidPinFormatException | InvalidPinFormatException | — (not reached) | PASS |
| SE-48 | InvalidPinFormatException | InvalidPinFormatException | — (not reached) | PASS |
| SE-49 | InvalidPinFormatException | InvalidPinFormatException | — (not reached) | PASS |
| SE-X1 | InvalidAmountException | InvalidPinFormatException | InvalidAmountException | **FAIL** |
| SE-X2 | accepted | InvalidPinFormatException | InvalidAmountException | **FAIL** |
| SE-X3 | accepted | InvalidPinFormatException | accepted | **FAIL** |
| SE-X4 | accepted | InvalidPinFormatException | InvalidAmountException | **FAIL** |
| SE-X5 | accepted | InvalidPinFormatException | InvalidAmountException | **FAIL** |
| SE-X6 | accepted | InvalidPinFormatException | InvalidAmountException | **FAIL** |
| SE-X7 | InvalidAmountException | InvalidPinFormatException | accepted | **FAIL** |

### 3.2 `FeesCalculatorWithdrawalTest`

| ID | Expected fee | Actual fee | Result |
|---|---|---|---|
| WD-001 | 0.057 | 0.038 | **FAIL** |
| WD-002 | 0.057 | 0.038 | **FAIL** |
| WD-003 | 0.057 | 0.038 | **FAIL** |
| WD-004 | 0.057 | 0.038 | **FAIL** |
| WD-005 | 0.019 | 0.019 | PASS |
| WD-006 | 0.019 | 0.019 | PASS |
| WD-007 | 0.019 | 0.019 | PASS |
| WD-008 | 0.019 | 0.019 | PASS |
| WD-009 | 0 | 0.019 | **FAIL** |
| WD-010 | 0 | 0.019 | **FAIL** |
| WD-011 | 0.06 | 0.04 | **FAIL** |
| WD-012 | 0.06 | 0.04 | **FAIL** |
| WD-013 | 0.06 | 0.04 | **FAIL** |
| WD-014 | 0.06 | 0.04 | **FAIL** |
| WD-015 | 0.02 | 0.02 | PASS |
| WD-016 | 0.02 | 0.02 | PASS |
| WD-017 | 0.02 | 0.02 | PASS |
| WD-018 | 0.02 | 0.02 | PASS |
| WD-019 | 0 | 0.02 | **FAIL** |
| WD-020 | 0 | 0.02 | **FAIL** |
| WD-021 | 0.063 | 0.042 | **FAIL** |
| WD-022 | 0.063 | 0.042 | **FAIL** |
| WD-023 | 0.063 | 0.042 | **FAIL** |
| WD-024 | 0.063 | 0.042 | **FAIL** |
| WD-025 | 0.021 | 0.021 | PASS |
| WD-026 | 0.021 | 0.021 | PASS |
| WD-027 | 0.021 | 0.021 | PASS |
| WD-028 | 0.021 | 0.021 | PASS |
| WD-029 | 0 | 0.021 | **FAIL** |
| WD-030 | 0 | 0.021 | **FAIL** |
| WD-031 | 1.5 | 1 | **FAIL** |
| WD-032 | 1.5 | 1 | **FAIL** |
| WD-033 | 1.5 | 1 | **FAIL** |
| WD-034 | 1.5 | 1 | **FAIL** |
| WD-035 | 0.5 | 0.5 | PASS |
| WD-036 | 0.5 | 0.5 | PASS |
| WD-037 | 0.5 | 0.5 | PASS |
| WD-038 | 0.5 | 0.5 | PASS |
| WD-039 | 0 | 0.5 | **FAIL** |
| WD-040 | 0 | 0.5 | **FAIL** |
| WD-041 | 2.997 | 1.998 | **FAIL** |
| WD-042 | 2.997 | 1.998 | **FAIL** |
| WD-043 | 2.997 | 1.998 | **FAIL** |
| WD-044 | 2.997 | 1.998 | **FAIL** |
| WD-045 | 0.999 | 0.999 | PASS |
| WD-046 | 0.999 | 0.999 | PASS |
| WD-047 | 0.999 | 0.999 | PASS |
| WD-048 | 0.999 | 0.999 | PASS |
| WD-049 | 0 | 0.999 | **FAIL** |
| WD-050 | 0 | 0.999 | **FAIL** |
| WD-051 | 3 | 2 | **FAIL** |
| WD-052 | 3 | 2 | **FAIL** |
| WD-053 | 3 | 2 | **FAIL** |
| WD-054 | 3 | 2 | **FAIL** |
| WD-055 | 1 | 1 | PASS |
| WD-056 | 1 | 1 | PASS |
| WD-057 | 1 | 1 | PASS |
| WD-058 | 1 | 1 | PASS |
| WD-059 | 0 | 1 | **FAIL** |
| WD-060 | 0 | 1 | **FAIL** |
| WD-061 | 3.003 | 2.002 | **FAIL** |
| WD-062 | 3.003 | 2.002 | **FAIL** |
| WD-063 | 3.003 | 2.002 | **FAIL** |
| WD-064 | 3.003 | 2.002 | **FAIL** |
| WD-065 | 1.001 | 1.001 | PASS |
| WD-066 | 1.001 | 1.001 | PASS |
| WD-067 | 1.001 | 1.001 | PASS |
| WD-068 | 1.001 | 1.001 | PASS |
| WD-069 | 0 | 1.001 | **FAIL** |
| WD-070 | 0 | 1.001 | **FAIL** |
| WD-071 | 0.019 | 0.019 | PASS |
| WD-072 | 0.019 | 0 | **FAIL** |
| WD-073 | 0 | 0.019 | **FAIL** |
| WD-074 | 0 | 0.019 | **FAIL** |
| WD-075 | 0 | 0.019 | **FAIL** |
| WD-076 | 0.019 | 0 | **FAIL** |
| WD-077 | 0.019 | 0.019 | PASS |
| WD-078 | 0.02 | 0.02 | PASS |
| WD-079 | 0.02 | 0 | **FAIL** |
| WD-080 | 0 | 0.02 | **FAIL** |
| WD-081 | 0 | 0.02 | **FAIL** |
| WD-082 | 0 | 0.02 | **FAIL** |
| WD-083 | 0.02 | 0 | **FAIL** |
| WD-084 | 0.02 | 0.02 | PASS |
| WD-085 | 0.021 | 0.021 | PASS |
| WD-086 | 0.021 | 0 | **FAIL** |
| WD-087 | 0 | 0.021 | **FAIL** |
| WD-088 | 0 | 0.021 | **FAIL** |
| WD-089 | 0 | 0.021 | **FAIL** |
| WD-090 | 0.021 | 0 | **FAIL** |
| WD-091 | 0.021 | 0.021 | PASS |
| WD-092 | 0.5 | 0.5 | PASS |
| WD-093 | 0.5 | 0 | **FAIL** |
| WD-094 | 0 | 0.5 | **FAIL** |
| WD-095 | 0 | 0.5 | **FAIL** |
| WD-096 | 0 | 0.5 | **FAIL** |
| WD-097 | 0.5 | 0 | **FAIL** |
| WD-098 | 0.5 | 0.5 | PASS |
| WD-099 | 0.999 | 0.999 | PASS |
| WD-100 | 0.999 | 0 | **FAIL** |
| WD-101 | 0 | 0.999 | **FAIL** |
| WD-102 | 0 | 0.999 | **FAIL** |
| WD-103 | 0 | 0.999 | **FAIL** |
| WD-104 | 0.999 | 0 | **FAIL** |
| WD-105 | 0.999 | 0.999 | PASS |
| WD-106 | 1 | 1 | PASS |
| WD-107 | 1 | 0 | **FAIL** |
| WD-108 | 0 | 1 | **FAIL** |
| WD-109 | 0 | 1 | **FAIL** |
| WD-110 | 0 | 1 | **FAIL** |
| WD-111 | 1 | 0 | **FAIL** |
| WD-112 | 1 | 1 | PASS |
| WD-113 | 1.001 | 1.001 | PASS |
| WD-114 | 1.001 | 0 | **FAIL** |
| WD-115 | 0 | 1.001 | **FAIL** |
| WD-116 | 0 | 1.001 | **FAIL** |
| WD-117 | 0 | 1.001 | **FAIL** |
| WD-118 | 1.001 | 0 | **FAIL** |
| WD-119 | 1.001 | 1.001 | PASS |

### 3.3 `FeesCalculatorDepositTest`

| ID | Expected fee | Actual fee | Result |
|---|---|---|---|
| DE-01 | 0 | 0.05 | **FAIL** |
| DE-02 | 0.5 | 0.2 | **FAIL** |
| DE-03 | 0.125 | 0.125 | PASS |
| DE-04 | 0 | -0.2 | **FAIL** |
| DE-05 | 0 | 0.05 | **FAIL** |
| DE-06 | 0.1 | 0 | **FAIL** |
| DE-07 | 4 | 0 | **FAIL** |
| DE-08 | 0 | 0.5 | **FAIL** |
| DE-09 | 0 | 0 | PASS |
| DE-10 | 0.1 | 0 | **FAIL** |

### 3.4 `FeesCalculatorTransferTest`

| ID | Expected fee | Actual fee | Result |
|---|---|---|---|
| TR-01 | 0.15 | 3.75 | **FAIL** |
| TR-02 | 0.075 | 1.875 | **FAIL** |
| TR-03 | 0.075 | 3.75 | **FAIL** |
| TR-04 | 0.0375 | 1.875 | **FAIL** |
| TR-05 | 0.25 | 12.5 | **FAIL** |
| TR-06 | 0.125 | 6.25 | **FAIL** |
| TR-07 | 1.25 | 12.5 | **FAIL** |
| TR-08 | 0.625 | 6.25 | **FAIL** |
| TR-09 | 0.1 | 2.5 | **FAIL** |
| TR-10 | 0.05 | 2.5 | **FAIL** |
| TR-11 | 0.5 | 2.5 | **FAIL** |
| TR-12 | 0.25 | 2.5 | **FAIL** |
| TR-13 | 1 | 25 | **FAIL** |
| TR-14 | 0.5 | 27.5 | **FAIL** |
| TR-15 | 2.5 | 25 | **FAIL** |
| TR-16 | 1.25 | 27.5 | **FAIL** |

---

## 4. Fault localization

Each entry is one fault, with every failing test case that exposes it. The failing cases were traced into the code
by following the path their inputs take and finding the first statement where the code departs from the spec. Some
`SessionTest` cases appear under two faults: in a valid-PIN case both the PIN step and the amount step can fail
independently (Section 1.2). Every fault is marked **Confirmed** (the failure follows directly from the
specification's own wording) or **Assumption-based** (the failure depends on an assumption we stated in Section 1,
where the specification is silent).

| ID | Failing test cases | Suspected fault (file, method, line) | Justification | Basis |
|---|---|---|---|---|
| SF1 | SE-01–42, SE-X1–X7 (49) | `src/main/java/atm/utils/FormatChecker.java` — `checkPinFormat`, line 33: `pin.length == 4` | Every 5-digit PIN is rejected with InvalidPinFormatException and the 4-digit PIN `0000` is accepted, because the length check is 4 instead of 5. (Line 35, `pin[i] >= '9'`, would also reject digit 9 — e.g. 99999 — but this is masked until the length is fixed.) | Confirmed |
| SF2 | SE-X2, SE-X4, SE-X5, SE-X6 (4) | `src/main/java/atm/utils/CashValidator.java` — `validateWithdrawal`, lines 23–27 (`% 50` then `% 20`) | Amounts that need more than one $20 after the $50s ($60, $80, $110, $130) are rejected with InvalidAmountException even though they can be dispensed in $20 and $50 bills. The same check also rejects the BVA max+ value $1,010, so `SE-14`, `SE-21`, `SE-28`, `SE-35` and `SE-42` get the expected InvalidAmountException only by coincidence (see SF4). | Confirmed |
| SF3 | SE-08, SE-15, SE-22, SE-29, SE-36 (5) | `src/main/java/atm/session/Session.java` — `setAmount`, lines 87–91 (withdrawal branch) | An amount of $0 is accepted because the only check is dispensability (0 % 50 == 0); there is no lower-bound check, so InvalidAmountException is never thrown. | Assumption-based: the specification never states that $0 is invalid; we assumed the smallest valid withdrawal is $20 (Section 1.2). |
| SF4 | SE-X7 (1) | `src/main/java/atm/session/Session.java` — `setAmount`, lines 87–91 (withdrawal branch) | $1,020 passes the code's bill check but exceeds the $1,000 daily limit; setAmount never compares the amount with a daily limit (the code has none), so it is accepted instead of throwing InvalidAmountException. The BVA max+ cases ($1,010) do not show this fault because SF2 rejects $1,010 first. | Confirmed |
| WF1 | WD-072–076, WD-079–083, WD-086–090, WD-093–097, WD-100–104, WD-107–111, WD-114–118 (35) | `src/main/java/bank/utils/FeesCalculator.java` — `calculateWithdrawalFee`, line 12: student branch tests `dayOfWeek == SATURDAY \|\| dayOfWeek == SUNDAY` for the free case | The weekday/weekend condition is inverted: students are charged 0.1% on weekdays (spec: free) and nothing on Saturday/Sunday (spec: 0.1%); days 0 and 8 pass only because neither branch treats them as weekend. | Confirmed |
| WF2 | WD-001–004, WD-011–014, WD-021–024, WD-031–034, WD-041–044, WD-051–054, WD-061–064 (28) | `src/main/java/bank/utils/FeesCalculator.java` — `calculateWithdrawalFee`, line 19: `feePercentage = 0.002` | Every non-student case with balance below $1,000 returns 0.2% of the amount instead of the specified 0.3%. | Confirmed |
| WF3 | WD-009–010, WD-019–020, WD-029–030, WD-039–040, WD-049–050, WD-059–060, WD-069–070 (14) | `src/main/java/bank/utils/FeesCalculator.java` — `calculateWithdrawalFee`, line 20: `accountBalance < 10000.00` | Balances of $5,000 and $5,000.01 are still charged 0.1%, so the no-fee boundary is at $10,000 instead of the specified $5,000. | Confirmed |
| DF1 | DE-01, DE-04–05 (3) | `src/main/java/bank/utils/FeesCalculator.java` — `calculateDepositInterest`, line 50: `interestPercentage = 0.002` | A student depositing ≤ $50 into an account with balance ≤ $5,000 should pay nothing, but is charged 0.2% (and with no guard on the amount, a −$100 deposit yields a negative fee of −0.2). | Confirmed |
| DF2 | DE-02 (1) | `src/main/java/bank/utils/FeesCalculator.java` — `calculateDepositInterest`, line 39: `amount > 100.00` | A $100 student deposit is "more than $50" per the spec but takes the small-deposit branch because the threshold is $100; the large-deposit rates on lines 42/44 (1%/0.5%) are also double the spec's 0.5%/0.25%. | Confirmed |
| DF3 | DE-06, DE-08, DE-10 (3) | `src/main/java/bank/utils/FeesCalculator.java` — `calculateDepositInterest`, lines 61–64: `> 10000.00 → 0.005`, else `0.0` | For a non-student deposit ≤ $250 the two rates are swapped/wrong: balance > $10,000 should be free (code charges 0.5%) and otherwise 0.1% (code charges nothing). | Confirmed |
| DF4 | DE-07 (1) | `src/main/java/bank/utils/FeesCalculator.java` — `calculateDepositInterest`, line 54: `amount > 500.00` | A $500 non-student deposit is "more than $250" but the code's threshold is $500, so it falls into the small-deposit branch and returns 0 instead of 0.8%. | Confirmed |
| TF1 | TR-01–04 (4) | `src/main/java/bank/utils/FeesCalculator.java` — `calculateTransferFee`, line 80: `amount < 100.00` (student branch) | A $150 student transfer is below the spec's $200 threshold but is routed to the ≥ $100 branch, which then applies a rate that is also not the spec's. | Confirmed |
| TF2 | TR-05–06, TR-13–14 (4) | `src/main/java/bank/utils/FeesCalculator.java` — `calculateTransferFee`, lines 81, 95 and 125: `fromAccountBalance < 1000.00` | A source balance of $1,500 is below the spec's $2,000 threshold but the code compares against $1,000, selecting the high-balance rate; the selected rates are also not the spec's. | Confirmed |
| TF3 | TR-09–12 (4) | `src/main/java/bank/utils/FeesCalculator.java` — `calculateTransferFee`, lines 111, 112 and 118: thresholds `1000.00` in the non-student `amount < 100` branch | For non-student transfers under $100 the spec uses from < $4,000 and to < $2,000, but the code uses $1,000 for both, so all four rules collapse onto the wrong rates (e.g. $3,000 / $1,500 should be 0.2%). | Confirmed |
| TF4 | TR-07–08, TR-15–16 (4) | `src/main/java/bank/utils/FeesCalculator.java` — `calculateTransferFee`, rate constants on lines 103, 105, 133 and 135 | These cases take the correct branch but still fail, so the constants themselves are wrong: lines 103, 105 and 133 are 10× the spec (e.g. 0.025 for 0.25%), and line 135 is 0.055 where the spec gives 0.25% (0.0025). | Confirmed |

**Faults that the techniques did not reach.** Rule outcomes that the weak ECA does not combine (Section 1.4) were not
tested. In `checkPinFormat`, the digit-9 rejection on line 35 cannot be seen yet because the length check on line 33
rejects every 5-digit PIN first. It should show up as new failures for `SE-29`–`SE-42` once the length fault is fixed.
Likewise, the missing daily-limit check (SF4) is hidden in the BVA max+ cases by the bill-check fault (SF2) and only
shows in `SE-X7`; once SF2 is fixed, `SE-14`, `SE-21`, `SE-28`, `SE-35` and `SE-42` should fail on the amount step too.
For the fee calculator, every fault above is also exposed by at least one in-domain case, so none of the fee faults
depends on the out-of-domain assumptions in Section 1.1.

---

## 5. How to reproduce

```bash
mvn -B test                       # whole suite (fails the build because tests fail)
mvn -B -Dmaven.test.failure.ignore=true test   # same, as CI runs it in phase:tests
mvn -B -Dtest=SessionTest test    # one class
mvn -B checkstyle:check           # lint
```
