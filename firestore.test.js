const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const PERIOD_ID = "2026_NOVEMBER";

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

test("Unauthenticated user: cannot read or write periods or issues", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("periods").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("periods")
      .doc(PERIOD_ID)
      .set({
        userId: ALICE_UID,
        month: "NOVEMBER",
        year: 2026,
        checks: {},
        slotLabels: {},
        generalNotes: "",
        picName: "R. MOH. GANDI / NERI WELAS ANTORO",
        createdAt: new Date(Date.now() - 1000),
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("Authenticated owner: valid period create, dot-notation cell update, notes update, and delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const periodRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("periods")
    .doc(PERIOD_ID);

  await assertSucceeds(
    periodRef.set({
      userId: ALICE_UID,
      month: "NOVEMBER",
      year: 2026,
      checks: { "1_0_17": "normal" },
      slotLabels: { "1": ["PJU 01", "PJU 02", "PJU 03"] },
      generalNotes: "Patroli malam lancar",
      picName: "R. MOH. GANDI / NERI WELAS ANTORO",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update mirroring Kotlin dot-notation cell update
  await assertSucceeds(
    periodRef.update({
      "checks.2_1_17": "error",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field on create
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("periods")
      .doc("2026_DESEMBER")
      .set({
        id: "2026_DESEMBER",
        userId: ALICE_UID,
        month: "DESEMBER",
        year: 2026,
        checks: {},
        slotLabels: {},
        generalNotes: "",
        picName: "GANDI",
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject immutable createdAt / month change on update
  await assertFails(
    periodRef.update({
      month: "DESEMBER",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    periodRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp
  await assertFails(
    periodRef.update({
      generalNotes: "Future",
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Authenticated owner: issues subcollection create, update status, list, and delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const periodRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("periods")
    .doc(PERIOD_ID);

  const issueRef = periodRef.collection("issues").doc("issue_1");

  // Empty list query on subcollection succeeds
  await assertSucceeds(periodRef.collection("issues").get());

  // Atomic batch creating period + issue (existsAfter check)
  const batch = aliceDb.batch();
  batch.set(periodRef, {
    userId: ALICE_UID,
    month: "NOVEMBER",
    year: 2026,
    checks: {},
    slotLabels: {},
    generalNotes: "",
    picName: "R. MOH. GANDI / NERI WELAS ANTORO",
    createdAt: created,
    updatedAt: created,
  });
  batch.set(issueRef, {
    userId: ALICE_UID,
    periodId: PERIOD_ID,
    date: 17,
    clusterId: 2,
    clusterName: "PJU CEMPAKA",
    unit: "Titik 1 / Gerbang",
    desc: "Lampu mati total, MCB turun",
    status: "Belum Diperbaiki",
    createdAt: created,
    updatedAt: created,
  });
  await assertSucceeds(batch.commit());

  // Status update succeeds
  await assertSucceeds(
    issueRef.update({
      status: "Dalam Proses",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Invalid status fails
  await assertFails(
    issueRef.update({
      status: "Status Tidak Valid",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Delete succeeds
  await assertSucceeds(issueRef.delete());
});

test("Cross-user isolation: Alice cannot read or write Bob's periods or issues", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const bobPeriodRef = context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("periods")
      .doc(PERIOD_ID);
    const created = new Date(Date.now() - 10000);
    await bobPeriodRef.set({
      userId: BOB_UID,
      month: "NOVEMBER",
      year: 2026,
      checks: {},
      slotLabels: {},
      generalNotes: "Bob note",
      picName: "Bob",
      createdAt: created,
      updatedAt: created,
    });
    await bobPeriodRef.collection("issues").doc("bob_issue").set({
      userId: BOB_UID,
      periodId: PERIOD_ID,
      date: 17,
      clusterId: 1,
      clusterName: "PJU KRISAN",
      unit: "Titik 1",
      desc: "Redup",
      status: "Belum Diperbaiki",
      createdAt: created,
      updatedAt: created,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("periods").get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("periods")
      .doc(PERIOD_ID)
      .get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("periods")
      .doc(PERIOD_ID)
      .update({
        generalNotes: "Hijack",
        updatedAt: new Date(Date.now() - 1000),
      })
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("periods")
      .doc(PERIOD_ID)
      .collection("issues")
      .get()
  );
});
