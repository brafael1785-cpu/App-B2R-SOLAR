const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_b2r_123";
const BOB_UID = "bob_b2r_456";

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

function pastDate(msOffset = 5000) {
  return new Date(Date.now() - msOffset);
}

test("Unauthenticated user cannot read or write any B2R Solar collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("clients").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("proposals").get());
  await assertFails(
    unauth.collection("users").doc(ALICE_UID).collection("clients").doc("c1").set({
      userId: ALICE_UID,
      name: "Cliente Teste",
      phone: "62999990000",
      email: "",
      state: "GO",
      city: "Goiânia",
      connectionType: "BIFASICO",
      avgConsumptionKwh: 800,
      notes: "",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("CompanySettings: owner create, partial update, cross-user denial, and bounds validation", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const t = pastDate(10000);

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("settings").doc("company").set({
      userId: ALICE_UID,
      companyName: "B2R Solar",
      documentNumber: "12.345.678/0001-90",
      phone: "(62) 99999-0000",
      email: "contato@b2rsolar.com.br",
      address: "Av. Goiás, 1000 - Goiânia/GO",
      defaultState: "GO",
      defaultCity: "Goiânia",
      kwhTariff: 0.95,
      pricePerWp: 2.8,
      defaultValidityDays: 15,
      defaultTemplate: 1,
      logoConfigured: false,
      createdAt: t,
      updatedAt: t,
    })
  );

  // Partial update succeeds
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("settings").doc("company").update({
      logoConfigured: true,
      kwhTariff: 1.05,
      updatedAt: pastDate(1000),
    })
  );

  // Reject ghost field
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("settings").doc("company").update({
      isAdmin: true,
      updatedAt: pastDate(1000),
    })
  );

  // Reject cross-user read or write
  await assertFails(
    bob.collection("users").doc(ALICE_UID).collection("settings").doc("company").get()
  );
  await assertFails(
    bob.collection("users").doc(ALICE_UID).collection("settings").doc("company").update({
      companyName: "Hacked",
      updatedAt: pastDate(1000),
    })
  );
});

test("SolarClient: owner CRUD, list query, and validation checks", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const t = pastDate(10000);

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("clients").doc("cli_1").set({
      userId: ALICE_UID,
      name: "Felipe Rocha",
      phone: "(62) 98888-1122",
      email: "felipe@exemplo.com",
      state: "GO",
      city: "Senador Canedo",
      connectionType: "BIFASICO",
      avgConsumptionKwh: 1349,
      notes: "Telhado colonial",
      createdAt: t,
      updatedAt: t,
    })
  );

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("clients").get()
  );

  // Reject @DocumentId "id" key injection
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("clients").doc("cli_2").set({
      id: "cli_2",
      userId: ALICE_UID,
      name: "Outro",
      phone: "",
      email: "",
      state: "GO",
      city: "",
      connectionType: "MONOFASICO",
      avgConsumptionKwh: 300,
      notes: "",
      createdAt: t,
      updatedAt: t,
    })
  );

  // Reject invalid connectionType enum
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("clients").doc("cli_3").set({
      userId: ALICE_UID,
      name: "Invalido",
      phone: "",
      email: "",
      state: "GO",
      city: "",
      connectionType: "TETRAFASICO",
      avgConsumptionKwh: 300,
      notes: "",
      createdAt: t,
      updatedAt: t,
    })
  );

  // Bob cannot list Alice's clients
  await assertFails(
    bob.collection("users").doc(ALICE_UID).collection("clients").get()
  );
});

test("SolarProposal: owner create, status update, terminal FECHADA locking, and invalid bounds", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const t = pastDate(10000);

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("proposals").doc("prop_1").set({
      userId: ALICE_UID,
      proposalCode: "B2R-2026-001",
      clientId: "cli_1",
      clientName: "Galpão Comercial",
      clientPhone: "(62) 98888-1122",
      state: "GO",
      city: "Senador Canedo",
      systemType: "ON_GRID",
      connectionType: "BIFASICO",
      consumptionInput: 1349,
      panelCount: 20,
      panelModel: "MODULO BIFACIAL 132 CEL. N TYPE 605W",
      panelPowerW: 605,
      powerKwp: 12.1,
      inverters: ["1x Inversor String 10kW"],
      batterySummary: "",
      totalPrice: 33899.0,
      cashEnabled: true,
      cardEnabled: true,
      financingEnabled: true,
      boletoEnabled: false,
      cardInstallments: 12,
      cardFeePercent: 15.29,
      templateModel: 4,
      estimatedMonthlyKwh: 1512.5,
      estimatedMonthlySavings: 1565.0,
      paybackMonths: 22,
      status: "EM_ANALISE",
      createdAt: t,
      updatedAt: t,
    })
  );

  // Update status to FECHADA succeeds
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("proposals").doc("prop_1").update({
      status: "FECHADA",
      updatedAt: pastDate(2000),
    })
  );

  // Terminal state locking: once FECHADA, further updates are rejected
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("proposals").doc("prop_1").update({
      status: "EM_ANALISE",
      updatedAt: pastDate(1000),
    })
  );

  // Reject future timestamp
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("proposals").doc("prop_future").set({
      userId: ALICE_UID,
      proposalCode: "B2R-2026-002",
      clientId: "",
      clientName: "Cliente Futuro",
      clientPhone: "",
      state: "SP",
      city: "",
      systemType: "ON_GRID",
      connectionType: "MONOFASICO",
      consumptionInput: 500,
      panelCount: 8,
      panelModel: "MODULO 605W",
      panelPowerW: 605,
      powerKwp: 4.84,
      inverters: ["1x Inversor 5kW"],
      batterySummary: "",
      totalPrice: 15000,
      cashEnabled: true,
      cardEnabled: false,
      financingEnabled: false,
      boletoEnabled: false,
      cardInstallments: 0,
      cardFeePercent: 0,
      templateModel: 1,
      estimatedMonthlyKwh: 550,
      estimatedMonthlySavings: 520,
      paybackMonths: 29,
      status: "EM_ANALISE",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: pastDate(1000),
    })
  );
});

test("AgendaAppointment & SolarEquipment: owner CRUD and cross-user isolation", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const t = pastDate(10000);

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("appointments").doc("apt_1").set({
      userId: ALICE_UID,
      title: "Visita técnica telhado",
      clientName: "Felipe Rocha",
      dateLabel: "28/11/2026 14:00",
      visitType: "VISITA_TECNICA",
      notes: "Levar trena e drone",
      completed: false,
      createdAt: t,
      updatedAt: t,
    })
  );

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("appointments").doc("apt_1").update({
      completed: true,
      updatedAt: pastDate(1000),
    })
  );

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("equipment").doc("eq_1").set({
      userId: ALICE_UID,
      kind: "MODULO",
      name: "Canadian Solar 610W",
      powerValue: 610,
      createdAt: t,
      updatedAt: t,
    })
  );

  await assertFails(
    bob.collection("users").doc(ALICE_UID).collection("appointments").get()
  );
  await assertFails(
    bob.collection("users").doc(ALICE_UID).collection("equipment").get()
  );
});
