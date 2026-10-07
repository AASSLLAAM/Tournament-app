const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ADMIN_UID = "admin_user_999";
const ADMIN_EMAIL = "aslamhussain096@gmail.com";
const PLAYER_UID = "player_user_111";
const PLAYER_EMAIL = "player@test.com";

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: "127.0.0.1",
      port: 8085,
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

test("Admin user can create new tournament", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL }).firestore();
  await assertSucceeds(
    adminDb.collection("tournaments").doc("admin_tour_01").set({
      title: "Admin Custom Cup",
      entryFee: 100,
      prizePool: 5000,
      totalSlots: 100,
      filledSlots: 0,
      statusText: "Open"
    })
  );
});

test("Admin can declare tournament winner", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tournaments").doc("tour_match_1").set({
      title: "Championship Finals",
      entryFee: 100,
      prizePool: 5000,
      totalSlots: 50,
      filledSlots: 50,
      statusText: "Ongoing"
    });
  });

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL }).firestore();
  await assertSucceeds(
    adminDb.collection("tournaments").doc("tour_match_1").update({
      winnerName: "Team Soul (Shadow99)",
      statusText: "Completed"
    })
  );
});

test("Regular player cannot declare tournament winner", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tournaments").doc("tour_match_2").set({
      title: "Championship Finals",
      entryFee: 100,
      prizePool: 5000,
      totalSlots: 50,
      filledSlots: 50,
      statusText: "Ongoing"
    });
  });

  const playerDb = testEnv.authenticatedContext(PLAYER_UID, { email: PLAYER_EMAIL }).firestore();
  await assertFails(
    playerDb.collection("tournaments").doc("tour_match_2").update({
      winnerName: "SelfDeclaredWinner",
      statusText: "Completed"
    })
  );
});

test("User can deposit money and create transaction for Razorpay payment", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(PLAYER_UID).set({
      userId: PLAYER_UID,
      email: PLAYER_EMAIL,
      walletBalance: 500,
      matchesPlayed: 0
    });
  });

  const playerDb = testEnv.authenticatedContext(PLAYER_UID, { email: PLAYER_EMAIL }).firestore();
  
  // Update wallet balance
  await assertSucceeds(
    playerDb.collection("users").doc(PLAYER_UID).update({
      walletBalance: 1000
    })
  );

  // Record deposit transaction
  await assertSucceeds(
    playerDb.collection("users").doc(PLAYER_UID).collection("transactions").doc("tx_rzp_pay_test123").set({
      userId: PLAYER_UID,
      title: "Razorpay Deposit (Test Mode)",
      amount: 500,
      isCredit: true,
      paymentId: "pay_test123",
      gateway: "Razorpay Standard"
    })
  );
});

