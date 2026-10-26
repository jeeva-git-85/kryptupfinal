import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import * as crypto from "crypto";

admin.initializeApp();
const db = admin.firestore();

const REGION = "asia-south1";

/**
 * Callable: requestBankLinkingOtp
 * Issues a cryptographically random 6-digit OTP for bank linking,
 * stores it in a server-only collection with a 5-minute expiry,
 * and returns it to the client so the demo UI can display the simulated SMS.
 */
export const requestBankLinkingOtp = functions
  .region(REGION)
  .https.onCall(async (_data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
    }

    const uid = context.auth.uid;
    const otp = crypto.randomInt(100000, 1000000).toString();
    const now = Date.now();
    const expiresAt = now + 5 * 60 * 1000; // 5 minutes validity

    await db.collection("bank_linking_sessions").doc(uid).set({
      otp,
      createdAt: now,
      expiresAt,
      used: false,
    });

    return {
      success: true,
      simulatedOtp: otp,
      message: `Simulated SMS: Your KryptUp verification code is ${otp}. Valid for 5 minutes.`,
    };
  });

/**
 * Callable: linkBankAccount
 * Server-side verified bank account linking. Requires the OTP issued by [requestBankLinkingOtp].
 * Atomically links the account and adds the ₹10,000 welcome deposit upon success.
 */
export const linkBankAccount = functions
  .region(REGION)
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
    }

    const uid = context.auth.uid;
    const { otp } = (data || {}) as { otp?: string };

    if (!otp || typeof otp !== "string" || otp.trim().length !== 6) {
      throw new functions.https.HttpsError("invalid-argument", "A valid 6-digit OTP is required.");
    }

    const sessionRef = db.collection("bank_linking_sessions").doc(uid);
    const userRef = db.collection("users").doc(uid);
    const referenceId = `BANK-LINK-${Date.now().toString(36).toUpperCase()}`;
    const timestamp = Date.now();
    const INITIAL_DEPOSIT_PAISE = 10_000 * 100; // ₹10,000 in integer paise

    return await db.runTransaction(async (tx) => {
      const sessionSnap = await tx.get(sessionRef);
      if (!sessionSnap.exists) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          "No active OTP request found. Please request a new OTP."
        );
      }

      const session = sessionSnap.data()!;
      if (session.used) {
        throw new functions.https.HttpsError("failed-precondition", "This OTP has already been used.");
      }

      if (Date.now() > session.expiresAt) {
        throw new functions.https.HttpsError("failed-precondition", "OTP has expired. Please request a new one.");
      }

      if (session.otp !== otp.trim()) {
        throw new functions.https.HttpsError("invalid-argument", "Invalid OTP entered. Please try again.");
      }

      const userSnap = await tx.get(userRef);
      if (!userSnap.exists) {
        throw new functions.https.HttpsError("not-found", "User profile not found.");
      }

      const isAlreadyLinked = userSnap.get("isBankLinked") ?? false;
      if (isAlreadyLinked) {
        throw new functions.https.HttpsError("already-exists", "Bank account is already linked to this profile.");
      }

      const currentBalanceInr: number = userSnap.get("fiatBalanceInr") ?? 0;
      const currentBalancePaise = Math.round(currentBalanceInr * 100);
      const newBalanceInr = (currentBalancePaise + INITIAL_DEPOSIT_PAISE) / 100;

      // Mark session used
      tx.update(sessionRef, { used: true });

      // Link bank and deposit bonus
      tx.update(userRef, {
        isBankLinked: true,
        fiatBalanceInr: newBalanceInr,
      });

      // Write immutable ledger entry
      tx.set(userRef.collection("transactions").doc(`tx_${referenceId}`), {
        type: "BANK_DEPOSIT",
        title: "Bank Account Linked & Initial Deposit",
        description: "Initial simulated bank balance added",
        amountDisplay: "+₹10,000.00 INR",
        amountValue: 10000.0,
        isDebit: false,
        timestamp,
        status: "CONFIRMED",
        category: "Banking",
        referenceId,
      });

      return {
        success: true,
        message: "Bank account successfully verified and linked! ₹10,000 demo funds credited.",
        referenceId,
      };
    });
  });

/**
 * Callable: sendUpiMoney
 * Robust P2P transfer with:
 *  - Idempotency key protection against network replay
 *  - Integer paise calculation (eliminates floating-point rounding bugs)
 *  - Ambiguity protection on UPI ID lookup
 *  - Atomic dual-ledger recording
 */
export const sendUpiMoney = functions
  .region(REGION)
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "You must be signed in to send money.");
    }

    const senderUid = context.auth.uid;
    const { recipientUpiId, amountInr, idempotencyKey } = data as {
      recipientUpiId: string;
      amountInr: number;
      idempotencyKey?: string;
    };

    if (!recipientUpiId || typeof recipientUpiId !== "string" || recipientUpiId.trim().length === 0) {
      throw new functions.https.HttpsError("invalid-argument", "Recipient UPI ID is required.");
    }

    if (typeof amountInr !== "number" || !isFinite(amountInr) || amountInr <= 0) {
      throw new functions.https.HttpsError("invalid-argument", "Transfer amount must be a positive finite number.");
    }

    // Convert amount to integer paise
    const amountPaise = Math.round(amountInr * 100);
    const MAX_TRANSFER_PAISE = 100_000 * 100; // ₹100,000 max

    if (!Number.isSafeInteger(amountPaise) || amountPaise <= 0) {
      throw new functions.https.HttpsError("invalid-argument", "Invalid transfer amount.");
    }

    if (amountPaise > MAX_TRANSFER_PAISE) {
      throw new functions.https.HttpsError("invalid-argument", "Single transfer limit is ₹1,00,000.");
    }

    const cleanUpiId = recipientUpiId.toLowerCase().trim();

    // Check idempotency if a key was provided
    if (idempotencyKey && typeof idempotencyKey === "string") {
      const existingKeyDoc = await db.collection("idempotency_keys").doc(`${senderUid}_${idempotencyKey}`).get();
      if (existingKeyDoc.exists) {
        const cached = existingKeyDoc.data()!;
        return {
          success: true,
          message: cached.message,
          referenceId: cached.referenceId,
          cached: true,
        };
      }
    }

    // Resolve recipient UPI ID
    const recipientQuery = await db.collection("users").where("upiId", "==", cleanUpiId).get();

    if (recipientQuery.empty) {
      throw new functions.https.HttpsError("not-found", `UPI ID '${cleanUpiId}' not found.`);
    }

    if (recipientQuery.size > 1) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        `Ambiguous UPI ID '${cleanUpiId}'. Multiple accounts matched. Contact support.`
      );
    }

    const recipientDoc = recipientQuery.docs[0];
    const recipientUid = recipientDoc.id;

    if (recipientUid === senderUid) {
      throw new functions.https.HttpsError("invalid-argument", "Cannot send money to your own UPI ID.");
    }

    const senderRef = db.collection("users").doc(senderUid);
    const recipientRef = db.collection("users").doc(recipientUid);
    const referenceId = `UPI-TX-${Date.now().toString(36).toUpperCase()}`;
    const timestamp = Date.now();

    return await db.runTransaction(async (tx) => {
      const senderSnap = await tx.get(senderRef);
      if (!senderSnap.exists) {
        throw new functions.https.HttpsError("not-found", "Sender account profile not found.");
      }

      const senderBalanceInr: number = senderSnap.get("fiatBalanceInr") ?? 0;
      const senderBalancePaise = Math.round(senderBalanceInr * 100);

      if (senderBalancePaise < amountPaise) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          `Insufficient balance. Available: ₹${(senderBalancePaise / 100).toFixed(2)}`
        );
      }

      const recipientSnap = await tx.get(recipientRef);
      if (!recipientSnap.exists) {
        throw new functions.https.HttpsError("not-found", "Recipient account profile not found.");
      }

      const recipientBalanceInr: number = recipientSnap.get("fiatBalanceInr") ?? 0;
      const recipientBalancePaise = Math.round(recipientBalanceInr * 100);

      // Perform integer paise additions and subtractions
      const newSenderBalanceInr = (senderBalancePaise - amountPaise) / 100;
      const newRecipientBalanceInr = (recipientBalancePaise + amountPaise) / 100;

      tx.update(senderRef, { fiatBalanceInr: newSenderBalanceInr });
      tx.update(recipientRef, { fiatBalanceInr: newRecipientBalanceInr });

      // Debit ledger entry
      tx.set(senderRef.collection("transactions").doc(`tx_${referenceId}`), {
        type: "UPI_SEND",
        title: `UPI Payment to ${cleanUpiId}`,
        description: "P2P Instant UPI Transfer",
        amountDisplay: `-₹${(amountPaise / 100).toFixed(2)} INR`,
        amountValue: amountPaise / 100,
        isDebit: true,
        timestamp,
        status: "CONFIRMED",
        category: "Transfer",
        referenceId,
      });

      // Credit ledger entry
      tx.set(recipientRef.collection("transactions").doc(`rx_${referenceId}`), {
        type: "UPI_RECEIVE",
        title: "Money received via UPI",
        description: "P2P Instant UPI Transfer",
        amountDisplay: `+₹${(amountPaise / 100).toFixed(2)} INR`,
        amountValue: amountPaise / 100,
        isDebit: false,
        timestamp,
        status: "CONFIRMED",
        category: "Transfer",
        referenceId,
      });

      // Store idempotency record if key was provided
      if (idempotencyKey && typeof idempotencyKey === "string") {
        tx.set(db.collection("idempotency_keys").doc(`${senderUid}_${idempotencyKey}`), {
          senderUid,
          recipientUpiId: cleanUpiId,
          amountInr: amountPaise / 100,
          referenceId,
          message: `Sent ₹${(amountPaise / 100).toFixed(2)} to ${cleanUpiId}`,
          createdAt: timestamp,
        });
      }

      return {
        success: true,
        message: `Sent ₹${(amountPaise / 100).toFixed(2)} to ${cleanUpiId}`,
        referenceId,
      };
    });
  });

/**
 * Callable: claimDailyReward
 * Secure server-side daily reward claim.
 * Validates the 24-hour streak server-side and increments points.
 */
export const claimDailyReward = functions
  .region(REGION)
  .https.onCall(async (_data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
    }

    const uid = context.auth.uid;
    const userRef = db.collection("users").doc(uid);
    const DAILY_REWARD_POINTS = 50;
    const ONE_DAY_MS = 24 * 60 * 60 * 1000;
    const now = Date.now();

    return await db.runTransaction(async (tx) => {
      const snap = await tx.get(userRef);
      if (!snap.exists) {
        throw new functions.https.HttpsError("not-found", "User profile not found.");
      }

      const lastClaim: number = snap.get("lastDailyClaim") ?? 0;
      if (now - lastClaim < ONE_DAY_MS) {
        const remainingHours = Math.ceil((ONE_DAY_MS - (now - lastClaim)) / (1000 * 60 * 60));
        throw new functions.https.HttpsError(
          "failed-precondition",
          `Daily reward already claimed. Next reward available in ${remainingHours} hour(s).`
        );
      }

      const currentPoints: number = snap.get("points") ?? 0;
      const newPoints = currentPoints + DAILY_REWARD_POINTS;

      tx.update(userRef, {
        points: newPoints,
        lastDailyClaim: now,
      });

      return {
        success: true,
        pointsAwarded: DAILY_REWARD_POINTS,
        totalPoints: newPoints,
        message: `Congratulations! +${DAILY_REWARD_POINTS} reward points claimed!`,
      };
    });
  });
