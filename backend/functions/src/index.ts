import { onCall, HttpsError } from "firebase-functions/https";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

type CreateProjectRequest = {
  title: string;
  description?: string;
  propertyType?: string;
};

export const createProject = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Authentication required.");
  }

  const data = request.data as CreateProjectRequest;
  const title = (data.title || "").trim();

  if (!title) {
    throw new HttpsError("invalid-argument", "Project title is required.");
  }

  const now = admin.firestore.FieldValue.serverTimestamp();
  const ownerId = request.auth.uid;

  const projectRef = db.collection("projects").doc();
  await projectRef.set({
    ownerId,
    title,
    description: data.description ?? "",
    propertyType: data.propertyType ?? "apartment",
    status: "draft",
    createdAt: now,
    updatedAt: now
  });

  return {
    projectId: projectRef.id,
    message: "Project created successfully."
  };
});

type GenerateEstimateRequest = {
  projectId: string;
  roomId?: string;
  region?: "UAE" | "US";
  overlayCount?: number;
  overlays?: Array<{
    materialPreset: string;
    widthMeters?: number;
    heightMeters?: number;
  }>;
};

export const generateEstimate = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Authentication required.");
  }

  const data = request.data as GenerateEstimateRequest;
  if (!data.projectId) {
    throw new HttpsError("invalid-argument", "projectId is required.");
  }

  const ownerId = request.auth.uid;
  const region = data.region ?? "UAE";
  const overlays = data.overlays ?? [];
  const fallbackCount = Math.max(1, data.overlayCount ?? 1);
  const catalogSnapshot = await db.collection("materialCatalog").get();
  const catalogByPreset = new Map<string, { priceUae: number; priceUs: number }>();
  catalogSnapshot.docs.forEach((doc) => {
    const row = doc.data();
    const key = String(row.presetCode ?? doc.id);
    catalogByPreset.set(key, {
      priceUae: Number(row.priceUae ?? 120),
      priceUs: Number(row.priceUs ?? 40)
    });
  });

  const lineItems = (overlays.length > 0 ? overlays : new Array(fallbackCount).fill({ materialPreset: "default_paint" }))
    .map((overlay, index) => {
      const preset = overlay.materialPreset || "default_paint";
      const price = catalogByPreset.get(preset) ?? { priceUae: 120, priceUs: 40 };
      const unitRate = region === "UAE" ? price.priceUae : price.priceUs;
      const quantity = Math.max(1, Number(overlay.widthMeters ?? 1) * Number(overlay.heightMeters ?? 1));
      return {
        label: `Material estimate #${index + 1} (${preset})`,
        quantity,
        unitRate,
        lineTotal: quantity * unitRate
      };
    });

  const subtotal = lineItems.reduce((sum, row) => sum + row.lineTotal, 0);
  const taxRate = region === "UAE" ? 0.05 : 0.08;
  const tax = subtotal * taxRate;
  const total = subtotal + tax;
  const currency = region === "UAE" ? "AED" : "USD";

  const estimateRef = db.collection("estimates").doc();
  await estimateRef.set({
    projectId: data.projectId,
    roomId: data.roomId ?? "",
    versionId: "",
    ownerId,
    region,
    lineItems,
    subtotal,
    tax,
    total,
    currency,
    generatedBy: "system",
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  });

  return {
    estimateId: estimateRef.id,
    total,
    currency,
    message: "Estimate generated successfully."
  };
});

type CreateShareLinkRequest = {
  projectId: string;
  versionId?: string;
  expiresInDays?: number;
};

export const createShareLink = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Authentication required.");
  }

  const data = request.data as CreateShareLinkRequest;
  if (!data.projectId) {
    throw new HttpsError("invalid-argument", "projectId is required.");
  }

  const ownerId = request.auth.uid;
  const projectSnap = await db.collection("projects").doc(data.projectId).get();
  if (!projectSnap.exists || projectSnap.get("ownerId") !== ownerId) {
    throw new HttpsError("permission-denied", "Project ownership validation failed.");
  }

  const rateBucketRef = db.collection("shareRateLimits").doc(`${ownerId}_${data.projectId}`);
  const rateBucketSnap = await rateBucketRef.get();
  const minuteWindowStart = Date.now() - 60 * 1000;
  const recentTimestamps: number[] = (rateBucketSnap.get("timestamps") ?? [])
    .filter((ts: number) => typeof ts === "number" && ts >= minuteWindowStart);
  if (recentTimestamps.length >= 5) {
    throw new HttpsError("resource-exhausted", "Too many share-link requests. Please wait a minute.");
  }

  const expiresInDays = Math.max(1, Math.min(30, data.expiresInDays ?? 7));
  const now = Date.now();
  const expiresAt = new Date(now + expiresInDays * 24 * 60 * 60 * 1000);
  const tokenRaw = `${ownerId}:${data.projectId}:${now}`;
  const tokenHash = Buffer.from(tokenRaw).toString("base64url");

  const ref = db.collection("shareLinks").doc();
  await ref.set({
    projectId: data.projectId,
    versionId: data.versionId ?? "",
    ownerId,
    tokenHash,
    expiresAt: admin.firestore.Timestamp.fromDate(expiresAt),
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  });

  await rateBucketRef.set(
    {
      ownerId,
      projectId: data.projectId,
      timestamps: [...recentTimestamps, now],
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    },
    { merge: true }
  );

  const shareUrl = `https://arhomedesign.app/share/${tokenHash}`;
  return {
    shareId: ref.id,
    shareUrl,
    expiresAt: expiresAt.toISOString()
  };
});

type BuildSharePackageRequest = {
  projectId: string;
  roomId?: string;
};

export const buildSharePackage = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Authentication required.");
  }
  const data = request.data as BuildSharePackageRequest;
  if (!data.projectId) {
    throw new HttpsError("invalid-argument", "projectId is required.");
  }
  const ownerId = request.auth.uid;
  const projectSnap = await db.collection("projects").doc(data.projectId).get();
  if (!projectSnap.exists || projectSnap.get("ownerId") !== ownerId) {
    throw new HttpsError("permission-denied", "Project ownership validation failed.");
  }

  let latestVersionQuery = db.collection("designVersions")
    .where("ownerId", "==", ownerId)
    .where("projectId", "==", data.projectId)
    .orderBy("versionNumber", "desc")
    .limit(1);
  if (data.roomId) {
    latestVersionQuery = latestVersionQuery.where("roomId", "==", data.roomId);
  }

  let latestEstimateQuery = db.collection("estimates")
    .where("ownerId", "==", ownerId)
    .where("projectId", "==", data.projectId)
    .orderBy("createdAt", "desc")
    .limit(1);
  if (data.roomId) {
    latestEstimateQuery = latestEstimateQuery.where("roomId", "==", data.roomId);
  }

  const [versionSnap, estimateSnap, overlaySnap] = await Promise.all([
    latestVersionQuery.get(),
    latestEstimateQuery.get(),
    db.collection("overlayRegions")
      .where("ownerId", "==", ownerId)
      .where("projectId", "==", data.projectId)
      .get()
  ]);

  const latestVersion = versionSnap.docs[0]?.data() ?? null;
  const latestEstimate = estimateSnap.docs[0]?.data() ?? null;
  const overlays = overlaySnap.docs.map((doc) => doc.data());

  const payload = {
    projectId: data.projectId,
    roomId: data.roomId ?? "",
    generatedAt: new Date().toISOString(),
    latestVersion,
    latestEstimate,
    overlayCount: overlays.length,
    overlays
  };

  return payload;
});
