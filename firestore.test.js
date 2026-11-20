const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const PARENT_UID = "parent_uid_123";
const CHILD_UID = "child_uid_456";
const STRANGER_UID = "stranger_uid_789";
const PIN_HASH = "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4"; // sha256("1234")

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

test("Unauthenticated users are denied everywhere", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("users").doc(PARENT_UID).get());
  await assertFails(db.collection("families").where("memberUids", "array-contains", PARENT_UID).get());
  await assertFails(db.collection("invite_codes").doc("EDU482").get());
});

test("Parent creates family, invite code, child profile, lesson, quiz and attempt", async () => {
  const parentDb = testEnv.authenticatedContext(PARENT_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // User account
  await assertSucceeds(
    parentDb.collection("users").doc(PARENT_UID).set({
      userId: PARENT_UID,
      displayName: "Maman",
      role: "PARENT",
      familyId: PARENT_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Family
  await assertSucceeds(
    parentDb.collection("families").doc(PARENT_UID).set({
      ownerId: PARENT_UID,
      familyName: "Famille Dupont",
      inviteCode: "EDU482",
      parentPinHash: PIN_HASH,
      memberUids: [PARENT_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Invite code
  await assertSucceeds(
    parentDb.collection("invite_codes").doc("EDU482").set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      familyName: "Famille Dupont",
      createdAt: now,
    })
  );

  // Child profile
  await assertSucceeds(
    parentDb.collection("families").doc(PARENT_UID).collection("children").doc("child_1").set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      memberUids: [PARENT_UID],
      name: "Léo",
      age: 9,
      gradeLevel: "CM1",
      avatarKey: "owl",
      pinHash: PIN_HASH,
      lessonsCount: 0,
      quizzesTaken: 0,
      totalCorrect: 0,
      totalQuestions: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Lesson
  await assertSucceeds(
    parentDb.collection("families").doc(PARENT_UID).collection("lessons").doc("lesson_1").set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      memberUids: [PARENT_UID],
      childId: "child_1",
      childName: "Léo",
      gradeLevel: "CM1",
      childAge: 9,
      subject: "Mathématiques",
      title: "Les fractions simples",
      sourceNotes: "Photo du cahier",
      summaryIntro: "Une fraction représente une part d'un tout.",
      keyPoints: ["Le numérateur est en haut", "Le dénominateur est en bas"],
      simpleExplanation: "Imagine une pizza coupée en 4 parts égales.",
      memoryTip: "Numérateur = Nuage (en haut)",
      photoAttached: true,
      quizCount: 1,
      attemptsCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Quiz
  await assertSucceeds(
    parentDb.collection("families").doc(PARENT_UID).collection("quizzes").doc("quiz_1").set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      memberUids: [PARENT_UID],
      lessonId: "lesson_1",
      childId: "child_1",
      subject: "Mathématiques",
      title: "Quiz sur les fractions",
      difficulty: "Facile",
      questions: [
        {
          prompt: "Que représente le chiffre du bas ?",
          options: ["Le total de parts", "Les parts mangées", "La taille", "Le prix"],
          correctIndex: 0,
          explanation: "Le dénominateur indique en combien de parts on coupe.",
        },
      ],
      attemptsCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // QuizAttempt
  await assertSucceeds(
    parentDb.collection("families").doc(PARENT_UID).collection("quiz_attempts").doc("att_1").set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      memberUids: [PARENT_UID],
      childId: "child_1",
      childName: "Léo",
      lessonId: "lesson_1",
      lessonTitle: "Les fractions simples",
      quizId: "quiz_1",
      subject: "Mathématiques",
      score: 1,
      totalQuestions: 1,
      percentage: 100,
      createdAt: now,
    })
  );

  // Query tests matching Kotlin repositories
  await assertSucceeds(
    parentDb.collection("families").where("memberUids", "array-contains", PARENT_UID).get()
  );
  await assertSucceeds(
    parentDb.collection("families").doc(PARENT_UID).collection("children").where("memberUids", "array-contains", PARENT_UID).get()
  );
  await assertSucceeds(
    parentDb.collection("families").doc(PARENT_UID).collection("lessons").where("memberUids", "array-contains", PARENT_UID).get()
  );
  await assertSucceeds(
    parentDb.collection("families").doc(PARENT_UID).collection("quizzes").where("memberUids", "array-contains", PARENT_UID).get()
  );
  await assertSucceeds(
    parentDb.collection("families").doc(PARENT_UID).collection("quiz_attempts").where("memberUids", "array-contains", PARENT_UID).get()
  );
});

test("Stranger cannot read, list, or tamper with another family's data", async () => {
  const parentDb = testEnv.authenticatedContext(PARENT_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    parentDb.collection("families").doc(PARENT_UID).set({
      ownerId: PARENT_UID,
      familyName: "Famille Dupont",
      inviteCode: "EDU482",
      parentPinHash: PIN_HASH,
      memberUids: [PARENT_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(strangerDb.collection("families").doc(PARENT_UID).get());
  await assertFails(strangerDb.collection("families").get());
  await assertFails(strangerDb.collection("invite_codes").get());
  await assertFails(
    strangerDb.collection("families").doc(PARENT_UID).collection("children").doc("bad").set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      memberUids: [STRANGER_UID, PARENT_UID],
      name: "Intruder",
      age: 10,
      gradeLevel: "CM2",
      avatarKey: "owl",
      pinHash: PIN_HASH,
      lessonsCount: 0,
      quizzesTaken: 0,
      totalCorrect: 0,
      totalQuestions: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
  // Reject @DocumentId 'id' ghost field
  await assertFails(
    parentDb.collection("families").doc(PARENT_UID).collection("children").doc("child_2").set({
      id: "child_2",
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      memberUids: [PARENT_UID],
      name: "Léo",
      age: 9,
      gradeLevel: "CM1",
      avatarKey: "owl",
      pinHash: PIN_HASH,
      lessonsCount: 0,
      quizzesTaken: 0,
      totalCorrect: 0,
      totalQuestions: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Child with own Google account can join via valid invite code without pre-reading family", async () => {
  const parentDb = testEnv.authenticatedContext(PARENT_UID).firestore();
  const childDb = testEnv.authenticatedContext(CHILD_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Owner reading non-existent own family doc does not throw PERMISSION_DENIED
  await assertSucceeds(parentDb.collection("families").doc(PARENT_UID).get());

  await assertSucceeds(
    parentDb.collection("families").doc(PARENT_UID).set({
      ownerId: PARENT_UID,
      familyName: "Famille Dupont",
      inviteCode: "EDU482",
      parentPinHash: PIN_HASH,
      memberUids: [PARENT_UID],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    parentDb.collection("invite_codes").doc("EDU482").set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      familyName: "Famille Dupont",
      createdAt: now,
    })
  );

  // Child looks up invite code
  await assertSucceeds(childDb.collection("invite_codes").doc("EDU482").get());
  // Child cannot read family BEFORE joining
  await assertFails(childDb.collection("families").doc(PARENT_UID).get());
  // Child joins family first via update
  await assertSucceeds(
    childDb.collection("families").doc(PARENT_UID).update({
      memberUids: [PARENT_UID, CHILD_UID],
      updatedAt: new Date(Date.now() - 500),
    })
  );
  // Child CAN read family AFTER joining
  await assertSucceeds(childDb.collection("families").doc(PARENT_UID).get());
  // Child creates their own profile
  await assertSucceeds(
    childDb.collection("families").doc(PARENT_UID).collection("children").doc("child_emma").set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      memberUids: [PARENT_UID, CHILD_UID],
      linkedUid: CHILD_UID,
      name: "Emma",
      age: 13,
      gradeLevel: "4ème",
      avatarKey: "fox",
      pinHash: PIN_HASH,
      lessonsCount: 0,
      quizzesTaken: 0,
      totalCorrect: 0,
      totalQuestions: 0,
      xp: 0,
      bestStreak: 0,
      badges: [],
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Quiz gamification fields (xp, bestStreak, badges, xpEarned, maxStreak) are validated properly", async () => {
  const parentDb = testEnv.authenticatedContext(PARENT_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    parentDb.collection("families").doc(PARENT_UID).set({
      ownerId: PARENT_UID,
      familyName: "Famille Dupont",
      inviteCode: "EDU482",
      parentPinHash: PIN_HASH,
      memberUids: [PARENT_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  const childRef = parentDb.collection("families").doc(PARENT_UID).collection("children").doc("child_gamified");
  // Legacy child profile without gamification fields still allowed and updatable with gamification fields
  await assertSucceeds(
    childRef.set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      memberUids: [PARENT_UID],
      name: "Léo",
      age: 10,
      gradeLevel: "CM2",
      avatarKey: "rocket",
      pinHash: PIN_HASH,
      lessonsCount: 1,
      quizzesTaken: 0,
      totalCorrect: 0,
      totalQuestions: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Updating child profile with XP, bestStreak, and badges succeeds
  await assertSucceeds(
    childRef.update({
      quizzesTaken: 1,
      totalCorrect: 4,
      totalQuestions: 4,
      xp: 122,
      bestStreak: 4,
      badges: ["first_quiz", "combo_3", "perfect_100"],
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Negative XP is rejected
  await assertFails(
    childRef.update({
      xp: -10,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // QuizAttempt with xpEarned and maxStreak succeeds
  await assertSucceeds(
    parentDb.collection("families").doc(PARENT_UID).collection("quiz_attempts").doc("att_gamified").set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      memberUids: [PARENT_UID],
      childId: "child_gamified",
      childName: "Léo",
      lessonId: "lesson_1",
      lessonTitle: "Les fractions",
      quizId: "quiz_1",
      subject: "Mathématiques",
      score: 4,
      totalQuestions: 4,
      percentage: 100,
      xpEarned: 122,
      maxStreak: 4,
      createdAt: now,
    })
  );

  // Negative xpEarned on QuizAttempt is rejected
  await assertFails(
    parentDb.collection("families").doc(PARENT_UID).collection("quiz_attempts").doc("att_bad_xp").set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      memberUids: [PARENT_UID],
      childId: "child_gamified",
      childName: "Léo",
      lessonId: "lesson_1",
      lessonTitle: "Les fractions",
      quizId: "quiz_1",
      subject: "Mathématiques",
      score: 4,
      totalQuestions: 4,
      percentage: 100,
      xpEarned: -5,
      maxStreak: 4,
      createdAt: now,
    })
  );
});

test("Lesson supports attachedFiles (PDF/documents) and enrichment updates", async () => {
  const parentDb = testEnv.authenticatedContext(PARENT_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    parentDb.collection("families").doc(PARENT_UID).set({
      ownerId: PARENT_UID,
      familyName: "Famille Dupont",
      inviteCode: "EDU482",
      parentPinHash: PIN_HASH,
      memberUids: [PARENT_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  const lessonRef = parentDb.collection("families").doc(PARENT_UID).collection("lessons").doc("lesson_pdf");
  await assertSucceeds(
    lessonRef.set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      memberUids: [PARENT_UID],
      childId: "child_1",
      childName: "Léo",
      gradeLevel: "CM1",
      childAge: 9,
      subject: "Sciences",
      title: "Le cycle de l'eau",
      sourceNotes: "Notes initiales",
      summaryIntro: "L'eau circule en boucle sur Terre.",
      keyPoints: ["Évaporation", "Condensation", "Précipitations"],
      simpleExplanation: "Le soleil chauffe l'eau qui monte en vapeur.",
      memoryTip: "E-C-P : Eau Chaude monte, Puis pleut !",
      photoAttached: false,
      attachedFiles: [
        { fileName: "cycle_eau.pdf", mimeType: "application/pdf", sizeBytes: 124500 }
      ],
      quizCount: 1,
      attemptsCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Enriching the selected course with an additional TXT/DOCX file and updated synthesis succeeds
  await assertSucceeds(
    lessonRef.update({
      sourceNotes: "Notes initiales\n[Document ajouté : fiche_exercices.txt]",
      summaryIntro: "Synthèse enrichie avec la fiche d'exercices.",
      keyPoints: ["Évaporation", "Condensation", "Précipitations", "Infiltration"],
      simpleExplanation: "Explication enrichie incluant le ruissellement et les nappes.",
      memoryTip: "E-C-P-I",
      attachedFiles: [
        { fileName: "cycle_eau.pdf", mimeType: "application/pdf", sizeBytes: 124500 },
        { fileName: "fiche_exercices.txt", mimeType: "text/plain", sizeBytes: 2400 }
      ],
      quizCount: 2,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Family chat messages allow parent and joined child, reject strangers, updates, spoofed senderUid, and empty text", async () => {
  const parentDb = testEnv.authenticatedContext(PARENT_UID).firestore();
  const childDb = testEnv.authenticatedContext(CHILD_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    parentDb.collection("families").doc(PARENT_UID).set({
      ownerId: PARENT_UID,
      familyName: "Famille Dupont",
      inviteCode: "EDU482",
      parentPinHash: PIN_HASH,
      memberUids: [PARENT_UID],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    parentDb.collection("invite_codes").doc("EDU482").set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      familyName: "Famille Dupont",
      createdAt: now,
    })
  );
  await assertSucceeds(
    childDb.collection("families").doc(PARENT_UID).update({
      memberUids: [PARENT_UID, CHILD_UID],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Parent sends a message in the family group channel
  const msg1Ref = parentDb.collection("families").doc(PARENT_UID).collection("family_messages").doc("msg_1");
  await assertSucceeds(
    msg1Ref.set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      memberUids: [PARENT_UID, CHILD_UID],
      channelId: "FAMILY",
      senderRole: "PARENT",
      senderName: "Maman",
      senderUid: PARENT_UID,
      text: "Bravo Léo pour tes révisions !",
      createdAt: now,
    })
  );

  // Child sends a reply in the family group channel
  const msg2Ref = childDb.collection("families").doc(PARENT_UID).collection("family_messages").doc("msg_2");
  await assertSucceeds(
    msg2Ref.set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      memberUids: [PARENT_UID, CHILD_UID],
      channelId: "FAMILY",
      senderRole: "CHILD",
      senderName: "Léo",
      senderUid: CHILD_UID,
      text: "Merci ! Je viens de finir mon quiz de maths.",
      createdAt: now,
    })
  );

  // Both parent and child can list family_messages using the exact Kotlin query filter
  await assertSucceeds(
    parentDb.collection("families").doc(PARENT_UID).collection("family_messages").where("memberUids", "array-contains", PARENT_UID).get()
  );
  await assertSucceeds(
    childDb.collection("families").doc(PARENT_UID).collection("family_messages").where("memberUids", "array-contains", CHILD_UID).get()
  );

  // Stranger cannot read, list unfiltered, or create messages
  await assertFails(
    strangerDb.collection("families").doc(PARENT_UID).collection("family_messages").doc("msg_1").get()
  );
  await assertFails(
    strangerDb.collection("families").doc(PARENT_UID).collection("family_messages").get()
  );
  await assertFails(
    strangerDb.collection("families").doc(PARENT_UID).collection("family_messages").doc("msg_bad").set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      memberUids: [PARENT_UID, STRANGER_UID],
      channelId: "FAMILY",
      senderRole: "PARENT",
      senderName: "Intrus",
      senderUid: STRANGER_UID,
      text: "Coucou",
      createdAt: now,
    })
  );

  // Spoofed senderUid is rejected
  await assertFails(
    childDb.collection("families").doc(PARENT_UID).collection("family_messages").doc("msg_spoof").set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      memberUids: [PARENT_UID, CHILD_UID],
      channelId: "FAMILY",
      senderRole: "PARENT",
      senderName: "Maman",
      senderUid: PARENT_UID,
      text: "Faux message",
      createdAt: now,
    })
  );

  // Empty text and message updates are rejected
  await assertFails(
    parentDb.collection("families").doc(PARENT_UID).collection("family_messages").doc("msg_empty").set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      memberUids: [PARENT_UID, CHILD_UID],
      channelId: "FAMILY",
      senderRole: "PARENT",
      senderName: "Maman",
      senderUid: PARENT_UID,
      text: "",
      createdAt: now,
    })
  );
  await assertFails(msg1Ref.update({ text: "Modifié" }));
  // Sender or family owner can delete a message
  await assertSucceeds(msg2Ref.delete());
});

test("Competency evaluation (documentType, acquiredSkills, weakPoints, targetedGaps, masteredSkills/weakSkills) is validated properly", async () => {
  const parentDb = testEnv.authenticatedContext(PARENT_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    parentDb.collection("families").doc(PARENT_UID).set({
      ownerId: PARENT_UID,
      familyName: "Famille Dupont",
      inviteCode: "EDU482",
      parentPinHash: PIN_HASH,
      memberUids: [PARENT_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  const childRef = parentDb.collection("families").doc(PARENT_UID).collection("children").doc("child_eval");
  await assertSucceeds(
    childRef.set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      memberUids: [PARENT_UID],
      name: "Lina",
      age: 9,
      gradeLevel: "CM1",
      avatarKey: "owl",
      pinHash: PIN_HASH,
      lessonsCount: 0,
      quizzesTaken: 0,
      totalCorrect: 0,
      totalQuestions: 0,
      acquiredSkills: ["Reconnaître le sujet"],
      weakPoints: ["Repérer tous les verbes conjugués"],
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    childRef.update({
      acquiredSkills: ["Reconnaître le sujet", "Vocabulaire anglais : la maison"],
      weakPoints: ["Accord sujet-verbe au pluriel"],
      updatedAt: new Date(Date.now() - 500),
    })
  );

  const lessonRef = parentDb.collection("families").doc(PARENT_UID).collection("lessons").doc("lesson_exo");
  await assertSucceeds(
    lessonRef.set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      memberUids: [PARENT_UID],
      childId: "child_eval",
      childName: "Lina",
      gradeLevel: "CM1",
      childAge: 9,
      subject: "Français",
      documentType: "Exercice",
      title: "Les verbes conjugués dans la phrase",
      sourceNotes: "Exercice du cahier",
      summaryIntro: "Savoir repérer chaque verbe conjugué dans une phrase.",
      keyPoints: ["Un verbe conjugué change selon la personne et le temps."],
      simpleExplanation: "Change le temps de la phrase pour trouver les mots qui varient.",
      memoryTip: "Mets 'Demain' au début de la phrase !",
      acquiredSkills: ["Identifier le verbe principal"],
      weakPoints: ["Repérer tous les verbes conjugués dans une phrase complexe"],
      evaluationSummary: "Lina trouve bien le premier verbe mais oublie parfois le second.",
      photoAttached: true,
      quizCount: 1,
      attemptsCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Invalid documentType is rejected
  await assertFails(
    parentDb.collection("families").doc(PARENT_UID).collection("lessons").doc("lesson_bad_type").set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      memberUids: [PARENT_UID],
      childId: "child_eval",
      childName: "Lina",
      gradeLevel: "CM1",
      childAge: 9,
      subject: "Français",
      documentType: "Invalide",
      title: "Test",
      sourceNotes: "",
      summaryIntro: "Intro",
      keyPoints: ["Point"],
      simpleExplanation: "Explication",
      memoryTip: "",
      photoAttached: false,
      quizCount: 0,
      attemptsCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Adaptive quiz with targetedGaps and FILL_BLANK question succeeds
  await assertSucceeds(
    parentDb.collection("families").doc(PARENT_UID).collection("quizzes").doc("quiz_adaptive").set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      memberUids: [PARENT_UID],
      lessonId: "lesson_exo",
      childId: "child_eval",
      subject: "Anglais",
      title: "Quiz sur mes lacunes en Anglais",
      difficulty: "Moyen",
      targetedGaps: ["Vocabulaire de la maison en anglais"],
      questions: [
        {
          questionType: "FILL_BLANK",
          prompt: "Comment écrit-on « canapé » en anglais ?",
          sentenceTemplate: "In the living room, I sit on the ____.",
          options: [],
          correctIndex: 0,
          acceptedAnswers: ["sofa", "couch"],
          guidingHint: "Ce mot anglais ressemble beaucoup à un mot que l'on utilise aussi en français pour un grand fauteuil !",
          secondHint: "Il commence par la lettre S et ne compte que quatre lettres.",
          explanation: "En anglais, « canapé » se dit « sofa » (ou « couch »).",
          skillTag: "Vocabulaire anglais : la maison",
        },
      ],
      attemptsCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // QuizAttempt with masteredSkills and weakSkills succeeds
  await assertSucceeds(
    parentDb.collection("families").doc(PARENT_UID).collection("quiz_attempts").doc("att_skills").set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      memberUids: [PARENT_UID],
      childId: "child_eval",
      childName: "Lina",
      lessonId: "lesson_exo",
      lessonTitle: "Les verbes conjugués",
      quizId: "quiz_adaptive",
      subject: "Français",
      score: 3,
      totalQuestions: 5,
      percentage: 60,
      xpEarned: 60,
      maxStreak: 2,
      masteredSkills: ["Identifier le sujet"],
      weakSkills: ["Repérer tous les verbes conjugués"],
      createdAt: now,
    })
  );
});

test("Weekly missed questions storage and Sunday 10-question review quiz rules are enforced", async () => {
  const parentDb = testEnv.authenticatedContext(PARENT_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    parentDb.collection("families").doc(PARENT_UID).set({
      ownerId: PARENT_UID,
      familyName: "Famille Dupont",
      inviteCode: "EDU482",
      parentPinHash: PIN_HASH,
      memberUids: [PARENT_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  const childRef = parentDb.collection("families").doc(PARENT_UID).collection("children").doc("child_sunday");
  await assertSucceeds(
    childRef.set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      memberUids: [PARENT_UID],
      name: "Lina",
      age: 9,
      gradeLevel: "CM1",
      avatarKey: "owl",
      pinHash: PIN_HASH,
      lessonsCount: 1,
      quizzesTaken: 1,
      totalCorrect: 3,
      totalQuestions: 5,
      weeklyMissedQuestions: [],
      lastSundayQuizWeekKey: null,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Storing first-try missed questions + lastSundayQuizWeekKey succeeds
  await assertSucceeds(
    childRef.update({
      weeklyMissedQuestions: [
        {
          subject: "Français",
          lessonId: "lesson_1",
          lessonTitle: "Les verbes conjugués",
          quizId: "quiz_1",
          skillTag: "Repérer les verbes conjugués",
          prompt: "Trouve tous les verbes dans la phrase.",
          weekKey: "2026-W47",
        },
        {
          subject: "Anglais",
          lessonId: "lesson_2",
          lessonTitle: "La maison",
          quizId: "quiz_2",
          skillTag: "Vocabulaire anglais : salon",
          prompt: "Comment dit-on canapé en anglais ?",
          weekKey: "2026-W47",
        },
      ],
      lastSundayQuizWeekKey: "2026-W47",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Exceeding 40 weeklyMissedQuestions is rejected
  const tooManyMissed = Array.from({ length: 41 }, (_, i) => ({
    subject: "Maths",
    prompt: `Question ${i}`,
    weekKey: "2026-W47",
  }));
  await assertFails(
    childRef.update({
      weeklyMissedQuestions: tooManyMissed,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Stranger cannot tamper with weeklyMissedQuestions
  await assertFails(
    strangerDb.collection("families").doc(PARENT_UID).collection("children").doc("child_sunday").update({
      weeklyMissedQuestions: [],
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Creating a 10-question Sunday weeklyReview quiz succeeds
  const tenQuestions = Array.from({ length: 10 }, (_, i) => ({
    questionType: i % 2 === 0 ? "FILL_BLANK" : "MCQ",
    prompt: `Question révision ${i + 1}`,
    options: ["A", "B", "C", "D"],
    correctIndex: 0,
    acceptedAnswers: ["réponse"],
    guidingHint: "Relis bien la règle de la semaine.",
    secondHint: "Procède étape par étape sans te presser.",
    explanation: "Explication pédagogique.",
    skillTag: "Thématique révisée",
  }));

  await assertSucceeds(
    parentDb.collection("families").doc(PARENT_UID).collection("quizzes").doc("quiz_sunday_10").set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      memberUids: [PARENT_UID],
      lessonId: "weekly_sunday_review",
      childId: "child_sunday",
      subject: "Révision Hebdo",
      title: "Quiz Révision du Dimanche",
      difficulty: "Moyen",
      weeklyReview: true,
      weekKey: "2026-W47",
      targetedGaps: ["Repérer les verbes conjugués", "Vocabulaire anglais : salon"],
      questions: tenQuestions,
      attemptsCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // 11 questions is rejected (cap at 10)
  await assertFails(
    parentDb.collection("families").doc(PARENT_UID).collection("quizzes").doc("quiz_sunday_11").set({
      familyId: PARENT_UID,
      ownerId: PARENT_UID,
      memberUids: [PARENT_UID],
      lessonId: "weekly_sunday_review",
      childId: "child_sunday",
      subject: "Révision Hebdo",
      title: "Trop long",
      difficulty: "Moyen",
      weeklyReview: true,
      weekKey: "2026-W47",
      questions: [...tenQuestions, tenQuestions[0]],
      attemptsCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
});




