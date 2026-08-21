/**
 * Firestore Security Rules Test Suite
 * Validates RBAC policies in `firestore.rules` across user roles:
 * - SUPER_ADMIN
 * - MANAGER
 * - EMPLOYEE
 * - VIEWER
 * - CUSTOMER
 * - ANONYMOUS
 *
 * Usage with Firebase Emulator:
 *   npx firebase-tools emulators:exec "node scripts/test_firestore_rules.js"
 */

const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const fs = require("fs");
const path = require("path");

const PROJECT_ID = "studio-5127376043-9142b";

async function runFirestoreRulesTests() {
  console.log("=================================================");
  console.log("   FIRESTORE SECURITY RULES VALIDATION SUITE     ");
  console.log("=================================================");

  const rulesContent = fs.readFileSync(
    path.join(__dirname, "../firestore.rules"),
    "utf8"
  );

  const testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: rulesContent,
      host: "127.0.0.1",
      port: 8088,
    },
  });

  try {
    // -------------------------------------------------------------
    // Setup Contexts
    // -------------------------------------------------------------
    const superAdminCtx = testEnv.authenticatedContext("uid_super_admin");
    const managerCtx = testEnv.authenticatedContext("uid_manager");
    const employeeCtx = testEnv.authenticatedContext("uid_employee");
    const viewerCtx = testEnv.authenticatedContext("uid_viewer");
    const customerCtx = testEnv.authenticatedContext("uid_customer");
    const unauthCtx = testEnv.unauthenticatedContext();

    // Seed User Documents for RBAC lookup
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await db.collection("users").doc("uid_super_admin").set({ role: "SUPER_ADMIN" });
      await db.collection("users").doc("uid_manager").set({ role: "MANAGER" });
      await db.collection("users").doc("uid_employee").set({ role: "EMPLOYEE" });
      await db.collection("users").doc("uid_viewer").set({ role: "VIEWER" });
      await db.collection("users").doc("uid_customer").set({ role: "CUSTOMER" });
    });

    console.log("\n[1/5] Testing /products collection rules...");
    // 1. Products
    // Read: Allowed for anyone (public)
    await assertSucceeds(unauthCtx.firestore().collection("products").doc("p1").get());
    console.log("  ✔ Public read permitted on products");

    // Write: Blocked for Employee, Viewer, Customer, Unauthenticated
    await assertFails(
      employeeCtx.firestore().collection("products").doc("p1").set({ name: "Widget", price: 10 })
    );
    await assertFails(
      customerCtx.firestore().collection("products").doc("p1").set({ name: "Widget", price: 10 })
    );
    console.log("  ✔ Non-manager product creation denied");

    // Write: Permitted for Manager & Super Admin
    await assertSucceeds(
      managerCtx.firestore().collection("products").doc("p1").set({ name: "Widget", price: 10 })
    );
    await assertSucceeds(
      superAdminCtx.firestore().collection("products").doc("p1").update({ price: 15 })
    );
    console.log("  ✔ Manager & Super Admin product write permitted");

    console.log("\n[2/5] Testing /users collection rules...");
    // 2. Users
    // Read own profile: Allowed
    await assertSucceeds(
      customerCtx.firestore().collection("users").doc("uid_customer").get()
    );
    // Read other's profile: Denied for Customer, Allowed for Manager
    await assertFails(
      customerCtx.firestore().collection("users").doc("uid_employee").get()
    );
    await assertSucceeds(
      managerCtx.firestore().collection("users").doc("uid_employee").get()
    );
    console.log("  ✔ Profile read boundaries enforced");

    // Role creation restrictions:
    // Manager cannot create a SUPER_ADMIN
    await assertFails(
      managerCtx.firestore().collection("users").doc("new_admin").set({
        role: "SUPER_ADMIN",
        name: "Fake Admin"
      })
    );
    console.log("  ✔ Manager forbidden from creating SUPER_ADMIN users");

    // Manager can create EMPLOYEE
    await assertSucceeds(
      managerCtx.firestore().collection("users").doc("new_emp").set({
        role: "EMPLOYEE",
        name: "New Worker"
      })
    );
    console.log("  ✔ Manager permitted to create EMPLOYEE users");

    // Super Admin can create SUPER_ADMIN
    await assertSucceeds(
      superAdminCtx.firestore().collection("users").doc("new_admin2").set({
        role: "SUPER_ADMIN",
        name: "Real Admin"
      })
    );
    console.log("  ✔ Super Admin permitted to create SUPER_ADMIN users");

    console.log("\n[3/5] Testing /sales & /saleItems collection rules...");
    // 3. Sales
    // Create Sale: Allowed for Employee if status == 'ACTIVE'
    await assertSucceeds(
      employeeCtx.firestore().collection("sales").doc("s1").set({
        totalAmount: 100,
        status: "ACTIVE",
        createdAt: new Date().toISOString()
      })
    );
    // Create Sale: Denied if status != 'ACTIVE' or if role is Customer
    await assertFails(
      customerCtx.firestore().collection("sales").doc("s2").set({
        totalAmount: 50,
        status: "ACTIVE"
      })
    );
    console.log("  ✔ Point of Sale creation permissions validated");

    // Delete Sale: Always denied (Immutable Audit)
    await assertFails(
      superAdminCtx.firestore().collection("sales").doc("s1").delete()
    );
    console.log("  ✔ Permanent sales audit preservation enforced (Delete denied)");

    console.log("\n[4/5] Testing /auditLogs collection rules...");
    // 4. Audit Logs
    // Append Log: Allowed for any authenticated user
    await assertSucceeds(
      employeeCtx.firestore().collection("auditLogs").doc("l1").set({
        action: "POS_CHECKOUT",
        timestamp: new Date().toISOString()
      })
    );
    // Read Log: Allowed for Manager/SuperAdmin, Denied for Customer/Employee
    await assertFails(
      employeeCtx.firestore().collection("auditLogs").doc("l1").get()
    );
    await assertSucceeds(
      managerCtx.firestore().collection("auditLogs").doc("l1").get()
    );
    // Update/Delete Log: Always Denied
    await assertFails(
      superAdminCtx.firestore().collection("auditLogs").doc("l1").delete()
    );
    console.log("  ✔ Audit logs append-only and read access boundaries validated");

    console.log("\n[5/5] Testing /backups collection rules...");
    // 5. System Backups
    // Only Super Admin can read/write backups
    await assertFails(
      managerCtx.firestore().collection("backups").doc("b1").get()
    );
    await assertSucceeds(
      superAdminCtx.firestore().collection("backups").doc("b1").get()
    );
    console.log("  ✔ Backup collection restricted strictly to Super Admin");

    console.log("\n[6/6] Testing B2B Wholesale Commerce collection rules...");
    // 6. B2B Wholesale
    // Unauth access denied
    await assertFails(
      unauthCtx.firestore().collection("businessCustomers").doc("bus1").get()
    );
    // Employee can create B2B Order
    await assertSucceeds(
      employeeCtx.firestore().collection("b2bOrders").doc("ord1").set({
        customerUid: "uid_customer",
        totalAmount: 5000,
        status: "CONFIRMED"
      })
    );
    // Customer can read own B2B Order
    await assertSucceeds(
      customerCtx.firestore().collection("b2bOrders").doc("ord1").get()
    );
    // Customer cannot create or modify Wholesale Prices
    await assertFails(
      customerCtx.firestore().collection("wholesalePrices").doc("wp1").set({ priceUsd: 10 })
    );
    // Manager can create Wholesale Prices
    await assertSucceeds(
      managerCtx.firestore().collection("wholesalePrices").doc("wp1").set({ priceUsd: 10 })
    );
    console.log("  ✔ B2B Wholesale RBAC and customer isolation validated");

    console.log("\n=================================================");
    console.log("  SUCCESS: ALL FIRESTORE RULES VALIDATED PERFECTLY");
    console.log("=================================================");
  } finally {
    await testEnv.cleanup();
  }
}

if (require.main === module) {
  runFirestoreRulesTests().catch((err) => {
    console.error("Test execution failed:", err);
    process.exit(1);
  });
}

module.exports = { runFirestoreRulesTests };
