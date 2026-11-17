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
const CHARLIE_UID = "charlie_789";

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

test("Unauthenticated user cannot read or write workspaces or invites", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("workspaces").get());
  await assertFails(unauthDb.collection("workspaceInvites").doc("BPS2026").get());
  await assertFails(
    unauthDb.collection("workspaces").doc("ws_1").collection("messages").get()
  );
});

test("Workspace creation, invite lookup, joining by invite code, and non-member isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  const batch = aliceDb.batch();
  const wsRef = aliceDb.collection("workspaces").doc("ws_bps");
  const inviteRef = aliceDb.collection("workspaceInvites").doc("BPS2026");

  batch.set(wsRef, {
    name: "Tim PJU BPS",
    ownerId: ALICE_UID,
    inviteCode: "BPS2026",
    memberUids: [ALICE_UID],
    memberNames: { [ALICE_UID]: "Alice" },
    createdAt: now,
    updatedAt: now,
  });
  batch.set(inviteRef, {
    workspaceId: "ws_bps",
    workspaceName: "Tim PJU BPS",
    ownerId: ALICE_UID,
    createdAt: now,
  });
  await assertSucceeds(batch.commit());

  // Listing invites must always fail
  await assertFails(bobDb.collection("workspaceInvites").get());

  // Bob looks up exact invite code and joins
  await assertSucceeds(bobDb.collection("workspaceInvites").doc("BPS2026").get());
  await assertSucceeds(
    bobDb.collection("workspaces").doc("ws_bps").update({
      memberUids: [ALICE_UID, BOB_UID],
      memberNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Filtered workspace list query works for Bob, fails for non-member Charlie
  await assertSucceeds(
    bobDb.collection("workspaces").where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertFails(
    charlieDb.collection("workspaces").where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertFails(charlieDb.collection("workspaces").doc("ws_bps").get());
});

test("Shared checklist period, issue creation, and live chat messages work for members only", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("workspaces").doc("ws_bps").set({
      name: "Tim PJU BPS",
      ownerId: ALICE_UID,
      inviteCode: "BPS2026",
      memberUids: [ALICE_UID, BOB_UID],
      memberNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      createdAt: now,
      updatedAt: now,
    });
  });

  // Alice creates shared period + issue atomically
  const batch = aliceDb.batch();
  const periodRef = aliceDb.collection("workspaces").doc("ws_bps").collection("periods").doc("2026_NOVEMBER");
  const issueRef = periodRef.collection("issues").doc("iss_1");
  batch.set(periodRef, {
    workspaceId: "ws_bps",
    memberUids: [ALICE_UID, BOB_UID],
    updatedByUid: ALICE_UID,
    updatedByName: "Alice",
    month: "NOVEMBER",
    year: 2026,
    checks: { "1_0_17": "error" },
    slotLabels: {},
    generalNotes: "",
    picName: "R. MOH. GANDI / NERI WELAS ANTORO",
    createdAt: now,
    updatedAt: now,
  });
  batch.set(issueRef, {
    workspaceId: "ws_bps",
    periodId: "2026_NOVEMBER",
    memberUids: [ALICE_UID, BOB_UID],
    createdByUid: ALICE_UID,
    createdByName: "Alice",
    date: 17,
    clusterId: 1,
    clusterName: "PJU KRISAN",
    unit: "Tiang 1",
    desc: "Mati total",
    status: "Belum Diperbaiki",
    createdAt: now,
    updatedAt: now,
  });
  await assertSucceeds(batch.commit());

  // Bob (teammate) can read period, query issues, update check, and send live chat message
  await assertSucceeds(
    bobDb.collection("workspaces").doc("ws_bps").collection("periods").doc("2026_NOVEMBER").get()
  );
  await assertSucceeds(
    bobDb.collection("workspaces").doc("ws_bps").collection("periods").doc("2026_NOVEMBER")
      .collection("issues").where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("workspaces").doc("ws_bps").collection("messages").doc("msg_1").set({
      workspaceId: "ws_bps",
      memberUids: [ALICE_UID, BOB_UID],
      senderId: BOB_UID,
      senderName: "Bob",
      senderEmail: "bob@example.com",
      text: "Sudah saya cek tiang 1 Krisan",
      tagCluster: "PJU KRISAN",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Bob can query messages scoped to memberUids; Charlie cannot
  await assertSucceeds(
    bobDb.collection("workspaces").doc("ws_bps").collection("messages")
      .where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertFails(
    charlieDb.collection("workspaces").doc("ws_bps").collection("messages")
      .where("memberUids", "array-contains", BOB_UID).get()
  );
  // Bob cannot spoof Alice's senderId
  await assertFails(
    bobDb.collection("workspaces").doc("ws_bps").collection("messages").doc("msg_spoof").set({
      workspaceId: "ws_bps",
      memberUids: [ALICE_UID, BOB_UID],
      senderId: ALICE_UID,
      senderName: "Alice",
      senderEmail: "alice@example.com",
      text: "Palsu",
      tagCluster: "",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Bob sends a voice note message
  await assertSucceeds(
    bobDb.collection("workspaces").doc("ws_bps").collection("messages").doc("msg_vn_1").set({
      workspaceId: "ws_bps",
      memberUids: [ALICE_UID, BOB_UID],
      senderId: BOB_UID,
      senderName: "Bob",
      senderEmail: "bob@example.com",
      text: "Pesan suara (0:08)",
      tagCluster: "PJU KRISAN",
      messageType: "voice",
      audioBase64: "AAAAIGZ0eXBpc29tAAACAGlzb21pc28y",
      audioDurationSec: 8,
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Alice updates her workspace profile photo; Bob can read it but cannot overwrite Alice's profile
  await assertSucceeds(
    aliceDb.collection("workspaces").doc("ws_bps").collection("profiles").doc(ALICE_UID).set({
      workspaceId: "ws_bps",
      userId: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      displayName: "Alice Petugas",
      photoBase64: "/9j/4AAQSkZJRgABAQAAAQABAAD/",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("workspaces").doc("ws_bps").collection("profiles")
      .where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertFails(
    bobDb.collection("workspaces").doc("ws_bps").collection("profiles").doc(ALICE_UID).update({
      displayName: "Dibajak",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  // Bob sends a photo report message
  await assertSucceeds(
    bobDb.collection("workspaces").doc("ws_bps").collection("messages").doc("msg_img_1").set({
      workspaceId: "ws_bps",
      memberUids: [ALICE_UID, BOB_UID],
      senderId: BOB_UID,
      senderName: "Bob",
      senderEmail: "bob@example.com",
      text: "Lampu tiang Krisan 2 mati",
      tagCluster: "PJU KRISAN",
      messageType: "image",
      audioBase64: "",
      audioDurationSec: 0,
      imageBase64: "/9j/4AAQSkZJRgABAQAAAQABAAD/",
      createdAt: new Date(Date.now() - 1000),
    })
  );
});
