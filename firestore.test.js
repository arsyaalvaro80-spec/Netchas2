const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_owner_123";
const BOB_UID = "bob_other_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function nowPast(offsetMs = 2000) {
  return new Date(Date.now() - offsetMs);
}

function validSettingsPayload(uid, ts = nowPast()) {
  return {
    userId: uid,
    businessName: "NetKas Wi-Fi Nusantara",
    businessPhone: "081234567890",
    businessAddress: "Jl. Merdeka No. 12, Bandung",
    bankAccountInfo: "BCA 1234567890 a.n. NetKas",
    monthlyExpenseBudget: 2500000,
    reminderDaysBefore: 3,
    remindersEnabled: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validCustomerPayload(uid, ts = nowPast()) {
  return {
    userId: uid,
    name: "Budi Santoso",
    phone: "081298765432",
    email: "budi@example.com",
    address: "Blok A No. 5 RT 02",
    packageName: "20 Mbps Home",
    monthlyFee: 150000,
    dueDay: 10,
    active: true,
    notes: "Router TP-Link lantai 1",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validPaymentPayload(uid, customerId, yearMonth = "2026-11", ts = nowPast()) {
  return {
    userId: uid,
    customerId,
    customerName: "Budi Santoso",
    packageName: "20 Mbps Home",
    yearMonth,
    amount: 150000,
    paymentMethod: "BANK_TRANSFER",
    bankReference: "BCA #TRX9921",
    receiptNumber: "NK-202611-001",
    paidDateMillis: Date.now() - 5000,
    notes: "Lunas tepat waktu",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validExpensePayload(uid, yearMonth = "2026-11", ts = nowPast()) {
  return {
    userId: uid,
    yearMonth,
    title: "Langganan Bandwidth ISP Induk",
    category: "BANDWIDTH",
    amount: 1200000,
    walletType: "BANK_TRANSFER",
    expenseDateMillis: Date.now() - 5000,
    notes: "Tagihan bulanan fiber 200Mbps",
    createdAt: ts,
    updatedAt: ts,
  };
}

test("Unauthenticated user: denied all reads and writes across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/customers`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/payments`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/expenses`).get());
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/settings/profile`).get());
  await assertFails(
    unauthDb
      .doc(`users/${ALICE_UID}/customers/cust_1`)
      .set(validCustomerPayload(ALICE_UID))
  );
});

test("BusinessSettings: owner create, partial update, shadow field & cross-user denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = nowPast(10000);

  await assertSucceeds(
    aliceDb
      .doc(`users/${ALICE_UID}/settings/profile`)
      .set(validSettingsPayload(ALICE_UID, created))
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/settings/profile`).update({
      monthlyExpenseBudget: 3200000,
      remindersEnabled: false,
      updatedAt: nowPast(1000),
    })
  );

  // Reject ghost field (Shadow Update)
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/settings/profile`).update({
      isAdmin: true,
      updatedAt: nowPast(500),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/settings/profile`).update({
      createdAt: nowPast(500),
      updatedAt: nowPast(500),
    })
  );

  // Reject cross-user read & write
  await assertFails(bobDb.doc(`users/${ALICE_UID}/settings/profile`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/settings/profile`).update({
      businessName: "Hacked ISP",
      updatedAt: nowPast(500),
    })
  );
});

test("Customer: owner CRUD, list query, future timestamp & extra @DocumentId field rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = nowPast(10000);

  await assertSucceeds(
    aliceDb
      .doc(`users/${ALICE_UID}/customers/cust_1`)
      .set(validCustomerPayload(ALICE_UID, created))
  );

  // List query matching Kotlin repository
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/customers`).get());

  // Partial update
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/customers/cust_1`).update({
      packageName: "50 Mbps Pro",
      monthlyFee: 250000,
      updatedAt: nowPast(1000),
    })
  );

  // Reject storing @DocumentId `id` field
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/customers/cust_2`).set({
      ...validCustomerPayload(ALICE_UID, created),
      id: "cust_2",
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb
      .doc(`users/${ALICE_UID}/customers/cust_3`)
      .set(validCustomerPayload(ALICE_UID, new Date(Date.now() + 60000)))
  );

  // Reject invalid dueDay > 28
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/customers/cust_4`).set({
      ...validCustomerPayload(ALICE_UID, created),
      dueDay: 31,
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection(`users/${ALICE_UID}/customers`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/customers/cust_1`).delete());

  // Owner delete
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/customers/cust_1`).delete());
});

test("Payment: relational customer check, edit previous month, wallet validation, cross-user denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = nowPast(10000);

  // Orphaned payment (customer does not exist) must fail
  await assertFails(
    aliceDb
      .doc(`users/${ALICE_UID}/payments/cust_missing_2026-11`)
      .set(validPaymentPayload(ALICE_UID, "cust_missing", "2026-11", created))
  );

  // Create customer first
  await assertSucceeds(
    aliceDb
      .doc(`users/${ALICE_UID}/customers/cust_1`)
      .set(validCustomerPayload(ALICE_UID, created))
  );

  // Create payment for previous month (2026-10)
  await assertSucceeds(
    aliceDb
      .doc(`users/${ALICE_UID}/payments/cust_1_2026-10`)
      .set(validPaymentPayload(ALICE_UID, "cust_1", "2026-10", created))
  );

  // Edit previous month payment: switch BANK_TRANSFER -> CASH and adjust amount
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/payments/cust_1_2026-10`).update({
      amount: 175000,
      paymentMethod: "CASH",
      bankReference: "",
      notes: "Koreksi input bulan lalu",
      updatedAt: nowPast(1000),
    })
  );

  // Reject invalid paymentMethod
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/payments/cust_1_2026-10`).update({
      paymentMethod: "CRYPTO",
      updatedAt: nowPast(500),
    })
  );

  // Reject mutating immutable customerId
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/payments/cust_1_2026-10`).update({
      customerId: "cust_other",
      updatedAt: nowPast(500),
    })
  );

  // Reject invalid yearMonth format
  await assertFails(
    aliceDb
      .doc(`users/${ALICE_UID}/payments/cust_1_2026-13`)
      .set(validPaymentPayload(ALICE_UID, "cust_1", "2026-13", created))
  );

  // List query matching Kotlin repository
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/payments`).get());

  // Cross-user isolation
  await assertFails(bobDb.collection(`users/${ALICE_UID}/payments`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/payments/cust_1_2026-10`).get()
  );
});

test("Expense: owner create, previous-month edit, category & wallet guards, cross-user denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = nowPast(10000);

  await assertSucceeds(
    aliceDb
      .doc(`users/${ALICE_UID}/expenses/exp_1`)
      .set(validExpensePayload(ALICE_UID, "2026-10", created))
  );

  // Edit previous month expense
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/expenses/exp_1`).update({
      amount: 1350000,
      walletType: "CASH",
      category: "MAINTENANCE",
      updatedAt: nowPast(1000),
    })
  );

  // Reject invalid category
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/expenses/exp_1`).update({
      category: "LUXURY",
      updatedAt: nowPast(500),
    })
  );

  // Reject non-positive amount
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/expenses/exp_2`).set({
      ...validExpensePayload(ALICE_UID, "2026-11", created),
      amount: 0,
    })
  );

  // List query matching Kotlin repository
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/expenses`).get());

  // Cross-user isolation
  await assertFails(bobDb.collection(`users/${ALICE_UID}/expenses`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/expenses/exp_1`).delete());

  // Owner delete
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/expenses/exp_1`).delete());
});

test("WalletTransfer: owner create/edit/delete, same-wallet rejection, cross-user denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = nowPast(10000);

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/wallet_transfers/trf_1`).set({
      userId: ALICE_UID,
      yearMonth: "2026-11",
      fromWallet: "CASH",
      toWallet: "BANK_TRANSFER",
      amount: 500000,
      transferDateMillis: Date.now() - 3000,
      notes: "Setor tunai ke rekening BCA",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject identical source and destination wallet
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/wallet_transfers/trf_bad`).set({
      userId: ALICE_UID,
      yearMonth: "2026-11",
      fromWallet: "CASH",
      toWallet: "CASH",
      amount: 100000,
      transferDateMillis: Date.now() - 3000,
      notes: "Invalid same wallet",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update succeeds
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/wallet_transfers/trf_1`).update({
      amount: 600000,
      notes: "Koreksi nominal setor",
      updatedAt: nowPast(1000),
    })
  );

  // List query succeeds for owner, fails for Bob
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/wallet_transfers`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/wallet_transfers`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/wallet_transfers/trf_1`).delete());

  // Owner delete
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/wallet_transfers/trf_1`).delete());
});

